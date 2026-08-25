package justfatlard.fletch_craft;

import net.minecraft.util.Prediction;
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

    @Override
    public void onInitialize() {
		// Guarded class load: FletchQuestRegistration names village-quests types.
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("village-quests-justfatlard")) {
			justfatlard.fletch_craft.integration.FletchQuestRegistration.register();
		}

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

        // Container removed: return items
        PandoricalApi.screens().onContainerRemoved(SCREEN_TYPE, player -> {
            SimpleContainer container = craftingContainers.remove(player.getUUID());
            if (container != null) {
                for (int i = 1; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false, Prediction.SERVER_ONLY);
                    }
                }
                container.clearContent();
            }
        });

        PandoricalApi.screens().onClose(SCREEN_TYPE, player -> {});
    }

    @SuppressWarnings("unchecked")
    private static void openFletchingScreen(ServerPlayer player) {
        if (!PandoricalApi.hasCapability(player, "screens")) {
            player.sendSystemMessage(Component.literal("Fletching requires Pandorical.").withStyle(ChatFormatting.RED));
            return;
        }

        // Slot 0 = result (read-only), slots 1-9 = crafting grid
        SimpleContainer container = new SimpleContainer(10);
        craftingContainers.put(player.getUUID(), container);

        // A bench, and nothing else on it.
        //
        // This used to carry its own recipe list down the right-hand side: a grid of buttons
        // that drew, and answered a click, and told you almost nothing - two letters of a
        // result's name, which for a table whose output is mostly arrows read "St St St St".
        // Browsing recipes is a solved problem and not this mod's to solve twice, so the panel
        // is gone and the screen is what a fletching table always should have been, a crafting
        // bench that happens to sit in a fletching table.
        int width = MARGIN + 9 * CELL + MARGIN;
        int invY = TOP_Y + CRAFT_H + 12;
        int hotbarY = invY + 3 * CELL + 4;
        int height = hotbarY + CELL + MARGIN;
        int invX = MARGIN;

        ScreenBuilder builder = new ScreenBuilder(SCREEN_TYPE)
            .size(width, height)
            .title("Fletching Table")
            .container(10, true);

        builder.panel("bg", 0, 0, width, height, Map.of("border", "beveled"));
        builder.text("title", MARGIN, 6, Map.of("text", "Fletching Table", "color", "#404040"));

        // Crafting grid (3x3): slots 1-9
        builder.text("grid_label", MARGIN, TOP_Y + 4, Map.of("text", "Crafting", "color", "#404040"));
        builder.inventoryGrid("craft_grid", MARGIN, TOP_Y + 14, 3, 3, 1);

        // Arrow + result
        builder.sprite("arrow", MARGIN + 3 * CELL + 4, TOP_Y + 40, 14, 2, Map.of("color", "#373737"));
        builder.inventoryGrid("result_slot", MARGIN + 3 * CELL + 22, TOP_Y + 34, 1, 1, 0);
        // Wide enough for the word. At sixteen it read "Tak...", which is a button that has
        // spent its whole width telling you it has no width.
        builder.button("result_take", MARGIN + 3 * CELL + 18, TOP_Y + 54, 26, 12,
            Map.of("label", "Take"));

        // Player inventory
        builder.inventoryGrid("player_inv", invX, invY, 3, 9, 10);
        builder.inventoryGrid("hotbar", invX, hotbarY, 1, 9, 37);

        PandoricalApi.screens().openContainer(player, builder.build(), container, Set.of(0));
    }

    /** One slot, and the gap the vanilla screens leave around their edges. */
    private static final int CELL = 18;
    private static final int MARGIN = 8;

    /** Where the two columns start, below the screen's own title. */
    private static final int TOP_Y = 20;

    /** Three rows of slots plus the label above them. */
    private static final int CRAFT_W = 3 * CELL + 22 + CELL;
    private static final int CRAFT_H = 14 + 3 * CELL + 18;

    /** Room for the "Fletching Recipes" heading above the grid of them. */
    private static final int RECIPE_TITLE_H = 18;


    private static void handleResultTake(ServerPlayer player, boolean takeAll) {
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (container == null) return;
        ItemStack result = container.getItem(0);
        if (result.isEmpty()) return;

        if (takeAll) {
            for (int i = 0; i < 64; i++) {
                if (container.getItem(0).isEmpty()) break;
                ItemStack crafted = container.getItem(0).copy();
                if (!player.getInventory().add(crafted)) player.drop(crafted, false, Prediction.SERVER_ONLY);
                consumeIngredients(container);
                updateResultFromContainer(container, player);
            }
        } else {
            if (!player.getInventory().add(result.copy())) player.drop(result.copy(), false, Prediction.SERVER_ONLY);
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
