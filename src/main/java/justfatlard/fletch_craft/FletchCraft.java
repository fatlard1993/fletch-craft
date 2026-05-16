package justfatlard.fletch_craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.pandorical.api.ComponentBuilder;
import justfatlard.pandorical.api.ComponentType;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.api.ScreenBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FletchCraft implements ModInitializer {
    public static final String MOD_ID = "fletch_craft";
    public static final String SCREEN_TYPE = "fletch-craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Recipe type
    public static final RecipeType<FletchingRecipe> FLETCHING_RECIPE_TYPE = Registry.register(
        BuiltInRegistries.RECIPE_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "fletching"),
        new RecipeType<>() {
            @Override public String toString() { return MOD_ID + ":fletching"; }
        }
    );

    // Recipe book category
    public static final RecipeBookCategory FLETCHING_CATEGORY = new RecipeBookCategory();

    // RecipeSerializer is a record in 26.1
    private static final MapCodec<FletchingRecipe> RECIPE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(FletchingRecipe::group),
        Codec.INT.fieldOf("width").forGetter(FletchingRecipe::getWidth),
        Codec.INT.fieldOf("height").forGetter(FletchingRecipe::getHeight),
        Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(FletchingRecipe::getIngredients),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(FletchingRecipe::getResultTemplate)
    ).apply(i, FletchingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> RECIPE_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, FletchingRecipe::group,
        ByteBufCodecs.VAR_INT, FletchingRecipe::getWidth,
        ByteBufCodecs.VAR_INT, FletchingRecipe::getHeight,
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), FletchingRecipe::getIngredients,
        ItemStackTemplate.STREAM_CODEC, FletchingRecipe::getResultTemplate,
        FletchingRecipe::new
    );

    public static final RecipeSerializer<FletchingRecipe> FLETCHING_SERIALIZER = new RecipeSerializer<>(RECIPE_CODEC, RECIPE_STREAM_CODEC);

    // Per-player crafting state
    private static final Map<UUID, SimpleContainer> craftingContainers = new ConcurrentHashMap<>();
    private static final Map<UUID, List<RecipeHolder<FletchingRecipe>>> cachedRecipes = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(MOD_ID, "fletching"), FLETCHING_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY,
            Identifier.fromNamespaceAndPath(MOD_ID, "fletching"), FLETCHING_CATEGORY);

        registerBlockInteraction();
        registerScreenHandlers();

        LOGGER.info("Fletch Craft loaded - Fletching table is now functional!");
    }

    private void registerBlockInteraction() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.isShiftKeyDown() && !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
            if (world.getBlockState(hitResult.getBlockPos()).is(Blocks.FLETCHING_TABLE)) {
                if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    openFletchingScreen(serverPlayer);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private void registerScreenHandlers() {
        // Recipe click
        PandoricalApi.screens().onActionFallback(SCREEN_TYPE, (player, data) -> {
            String componentId = data.get("_componentId");
            if (componentId != null && componentId.startsWith("recipe_")) {
                try {
                    int idx = Integer.parseInt(componentId.substring(7));
                    handleRecipeClick(player, idx, player.isShiftKeyDown());
                } catch (NumberFormatException ignored) {}
            }
        });

        // Slot changes → update result
        PandoricalApi.screens().onSlotChange(SCREEN_TYPE, (player, slotIndex, stack) -> {
            if (slotIndex >= 1 && slotIndex <= 9) updateResult(player);
        });

        // Result take
        PandoricalApi.screens().onAction(SCREEN_TYPE, "result_take", (player, data) -> {
            handleResultTake(player, false);
        });
        PandoricalApi.screens().onAction(SCREEN_TYPE, "result_take_all", (player, data) -> {
            handleResultTake(player, true);
        });

        // Container removed — return items
        PandoricalApi.screens().onContainerRemoved(SCREEN_TYPE, player -> {
            SimpleContainer container = craftingContainers.remove(player.getUUID());
            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
                    }
                }
                container.clearContent();
            }
            cachedRecipes.remove(player.getUUID());
        });

        PandoricalApi.screens().onClose(SCREEN_TYPE, player -> {});
    }

    @SuppressWarnings("unchecked")
    private static void openFletchingScreen(ServerPlayer player) {
        if (!PandoricalApi.hasCapability(player, "screens")) {
            player.sendSystemMessage(Component.literal("Fletching requires Pandorical.").withStyle(ChatFormatting.RED));
            return;
        }

        ServerLevel world = (ServerLevel) player.level();
        RecipeManager recipeManager = world.recipeAccess();

        // Get all fletching recipes
        List<RecipeHolder<FletchingRecipe>> recipes = recipeManager.getRecipes().stream()
            .filter(h -> h.value() instanceof FletchingRecipe)
            .map(h -> (RecipeHolder<FletchingRecipe>) (RecipeHolder<?>) h)
            .toList();
        cachedRecipes.put(player.getUUID(), recipes);

        // Slot 0 = result (read-only), slots 1-9 = crafting grid
        SimpleContainer container = new SimpleContainer(10);
        craftingContainers.put(player.getUUID(), container);

        ScreenBuilder builder = new ScreenBuilder(SCREEN_TYPE)
            .size(280, 180)
            .title("Fletching Table")
            .container(10, true);

        builder.panel("bg", 0, 0, 280, 180, Map.of("border", "beveled"));
        builder.text("title", 8, 6, Map.of("text", "Fletching Table", "color", "#404040"));

        // Crafting grid (3x3) — slots 1-9
        builder.text("grid_label", 8, 18, Map.of("text", "Crafting", "color", "#404040"));
        builder.inventoryGrid("craft_grid", 8, 28, 3, 3, 1);

        // Arrow + result
        builder.sprite("arrow", 66, 44, 14, 2, Map.of("color", "#373737"));
        builder.inventoryGrid("result_slot", 84, 38, 1, 1, 0);
        builder.button("result_take", 84, 58, 16, 12, Map.of("label", "Take"));

        // Recipe browser
        builder.panel("recipe_panel", 110, 6, 164, 168,
            Map.of("background", "#CC000000", "border", "flat", "border_color", "#555555"));
        builder.text("recipe_title", 116, 10, Map.of("text", "Fletching Recipes", "color", "#FFFFFF"));

        int cols = 8;
        for (int i = 0; i < Math.min(recipes.size(), cols * 8); i++) {
            RecipeHolder<FletchingRecipe> recipe = recipes.get(i);
            String label = recipe.value().getResult().getHoverName().getString();
            int col = i % cols, row = i / cols;
            builder.button("recipe_" + i, 116 + col * 18, 24 + row * 18, 18, 18,
                Map.of("label", label.length() > 2 ? label.substring(0, 2) : label));
        }

        // Player inventory
        builder.inventoryGrid("player_inv", 8, 100, 3, 9, 10);
        builder.inventoryGrid("hotbar", 8, 158, 1, 9, 37);

        PandoricalApi.screens().openContainer(player, builder.build(), container, Set.of(0));
    }

    private static void handleRecipeClick(ServerPlayer player, int recipeIndex, boolean fillMax) {
        List<RecipeHolder<FletchingRecipe>> recipes = cachedRecipes.get(player.getUUID());
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (recipes == null || container == null || recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        FletchingRecipe recipe = recipes.get(recipeIndex).value();

        // Return current grid items to player
        for (int i = 1; i <= 9; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
                container.setItem(i, ItemStack.EMPTY);
            }
        }

        // Find matching items and calculate max sets
        Map<net.minecraft.world.item.Item, Integer> needed = new LinkedHashMap<>();
        Map<Integer, net.minecraft.world.item.Item> slotToItem = new HashMap<>();

        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);
            if (ingredient.isEmpty()) continue;
            int gridSlot = 1 + (i % recipe.getWidth()) + (i / recipe.getWidth()) * 3;

            net.minecraft.world.item.Item found = null;
            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                ItemStack invStack = player.getInventory().getItem(j);
                if (!invStack.isEmpty() && ingredient.test(invStack)) { found = invStack.getItem(); break; }
            }
            if (found == null) return;

            needed.merge(found, 1, Integer::sum);
            slotToItem.put(gridSlot, found);
        }

        int maxSets = fillMax ? 64 : 1;
        for (var entry : needed.entrySet()) {
            int available = 0;
            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                if (player.getInventory().getItem(j).is(entry.getKey())) available += player.getInventory().getItem(j).getCount();
            }
            if (entry.getValue() > 0) maxSets = Math.min(maxSets, available / entry.getValue());
        }
        if (maxSets <= 0) return;

        // Take items from inventory and place into grid
        for (var entry : slotToItem.entrySet()) {
            int toPlace = maxSets;
            for (int j = 0; j < player.getInventory().getContainerSize() && toPlace > 0; j++) {
                ItemStack invStack = player.getInventory().getItem(j);
                if (invStack.is(entry.getValue())) {
                    int take = Math.min(toPlace, invStack.getCount());
                    invStack.shrink(take);
                    toPlace -= take;
                }
            }
            container.setItem(entry.getKey(), new ItemStack(entry.getValue(), maxSets));
        }

        updateResult(player);
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
    }

    private static void handleResultTake(ServerPlayer player, boolean takeAll) {
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (container == null) return;
        ItemStack result = container.getItem(0);
        if (result.isEmpty()) return;

        if (takeAll) {
            for (int i = 0; i < 64; i++) {
                if (container.getItem(0).isEmpty()) break;
                ItemStack crafted = container.getItem(0).copy();
                if (!player.getInventory().add(crafted)) player.drop(crafted, false);
                consumeIngredients(container);
                updateResultFromContainer(container, player);
            }
        } else {
            if (!player.getInventory().add(result.copy())) player.drop(result.copy(), false);
            consumeIngredients(container);
            updateResultFromContainer(container, player);
        }

        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
    }

    private static void updateResult(ServerPlayer player) {
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (container == null) return;
        updateResultFromContainer(container, player);
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
    }

    private static void updateResultFromContainer(SimpleContainer container, ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 1; i <= 9; i++) items.add(container.getItem(i).copy());
        CraftingInput input = CraftingInput.of(3, 3, items);

        ServerLevel world = (ServerLevel) player.level();
        Optional<RecipeHolder<FletchingRecipe>> match = world.recipeAccess()
            .getRecipeFor(FLETCHING_RECIPE_TYPE, input, world);

        container.setItem(0, match.isPresent() ? match.get().value().assemble(input) : ItemStack.EMPTY);
    }

    private static void consumeIngredients(SimpleContainer container) {
        for (int i = 1; i <= 9; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
