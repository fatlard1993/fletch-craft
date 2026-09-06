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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
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
    /** What each open table last showed in its result slot; see {@link #afterSlotClick}. */
    private static final Map<UUID, ItemStack> shownResult = new ConcurrentHashMap<>();

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
        // Every slot click reports the whole container, one call per slot; slot 0 is the one
        // pass where the picture is complete and consistent, so the work happens there and the
        // other nine are ignored.
        PandoricalApi.screens().onSlotChange(SCREEN_TYPE, (player, slotIndex, stack) -> {
            if (slotIndex == 0) afterSlotClick(player);
        });

        // A recipe book asking for a recipe to be laid out on this bench.
        PandoricalApi.screens().onPlaceRecipe(SCREEN_TYPE, FletchCraft::placeRecipe);

        // Container removed: return items
        PandoricalApi.screens().onContainerRemoved(SCREEN_TYPE, player -> {
            shownResult.remove(player.getUUID());
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

        // Slot 0 = result, slots 1-9 = crafting grid. The result slot hands items out and
        // never takes them, said the way vanilla's ResultSlot says it: refuse the placement,
        // allow the pickup.
        SimpleContainer container = new SimpleContainer(10) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot != 0;
            }
        };
        craftingContainers.put(player.getUUID(), container);

        // A bench, and nothing else on it - and the bench is vanilla's own.
        //
        // This used to carry its own recipe list down the right-hand side: a grid of buttons
        // that drew, and answered a click, and told you almost nothing - two letters of a
        // result's name, which for a table whose output is mostly arrows read "St St St St".
        // Browsing recipes is a solved problem and not this mod's to solve twice.
        //
        // What is left is drawn on the crafting table's own GUI texture at the crafting table's
        // own coordinates, so the arrow, the big result frame, the panel bevel and every slot
        // are vanilla's to the pixel rather than an impression of them. The grids below only
        // place the menu's slots over the frames already in that picture, which is what
        // SLOT_STYLE "none" is for.
        ScreenBuilder builder = new ScreenBuilder(SCREEN_TYPE)
            .size(VANILLA_W, VANILLA_H)
            .title("Fletching Table")
            .container(10, true)
            // What this bench is, so a recipe book can offer its recipes. The screen draws no
            // browser of its own; saying what it crafts is the whole of its part in that.
            .recipeStation(Identifier.fromNamespaceAndPath(MOD_ID, "fletching").toString());

        builder.sprite("bg", 0, 0, VANILLA_W, VANILLA_H, Map.of(
            ComponentType.PROP_TEXTURE, "minecraft:textures/gui/container/crafting_table.png",
            ComponentType.PROP_TEXTURE_WIDTH, "256",
            ComponentType.PROP_TEXTURE_HEIGHT, "256",
            ComponentType.PROP_TEXTURE_U, "0",
            ComponentType.PROP_TEXTURE_V, "0"));

        // Where vanilla puts its own two labels
        builder.text("title", 28, 6, Map.of("text", "Fletching Table", "color", LABEL_COLOR));
        builder.text("inv_label", 8, 72, Map.of("text", "Inventory", "color", LABEL_COLOR));

        // Vanilla slot coordinates are the ITEM's corner; a grid's are the FRAME's, one pixel
        // out from it, which is why every number here is vanilla's less one.
        // These three draw their own frames, and it costs nothing: Pandorical's beveled slot is
        // vanilla's slot, to the same three colours, landing exactly on the ones already in the
        // picture. Drawing them is invisible where it works and a plain slot grid where the
        // client is older than SLOT_STYLE "none", which beats a client that old rendering the
        // whole grid as one flat grey block.
        builder.inventoryGrid("craft_grid", 29, 16, 3, 3, 1);
        builder.inventoryGrid("player_inv", 7, 83, 3, 9, 10);
        builder.inventoryGrid("hotbar", 7, 141, 1, 9, 37);

        // The result is the one that cannot: vanilla frames it at 26x26 and an 18x18 slot drawn
        // inside that reads as a box in a box, so here the backdrop's frame is the only one.
        builder.inventoryGrid("result_slot", 123, 34, 1, 1, 0, BARE_SLOTS);

        // No read-only slots. The result is guarded the way vanilla guards its own - the
        // container refuses to have anything placed in slot 0 - which leaves it free to be
        // picked up. Marking it read-only here would refuse the pickup too, which is what the
        // "Take" button underneath used to be for.
        PandoricalApi.screens().openContainer(player, builder.build(), container, Set.of());
    }

    /** Three by three, the box the recipes are read into and laid out in. */
    private static final int GRID_SIDE = 3;

    /** The crafting table's own panel size, because that is the picture this screen is drawn on. */
    private static final int VANILLA_W = 176;
    private static final int VANILLA_H = 166;

    /** Vanilla's label grey. */
    private static final String LABEL_COLOR = "#404040";

    /** The backdrop already has every slot frame in it; the grids only place the menu's slots. */
    private static final Map<String, String> BARE_SLOTS =
        Map.of(ComponentType.PROP_SLOT_STYLE, "none");


    /**
     * What one click on this screen means, worked out after the fact.
     *
     * <p>There is no event for "the player took the result", only that every slot is reported
     * once a click has landed. But the result slot is written by nothing except
     * {@link #updateResultFromContainer}, which runs at the end of every click - so between
     * clicks it always holds the result of the grid as it stands. An empty result slot where
     * there was one a moment ago can therefore only mean the player lifted it off, and that is
     * when the ingredients are spent.
     *
     * <p>Remembering what was shown is what makes that test honest: placing the last ingredient
     * of a recipe also leaves an empty result slot beside a grid that matches, and consuming
     * there would eat the ingredients the instant they were laid down.
     */
    private static void afterSlotClick(ServerPlayer player) {
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (container == null) return;

        ItemStack was = shownResult.getOrDefault(player.getUUID(), ItemStack.EMPTY);
        if (!was.isEmpty() && container.getItem(0).isEmpty()) {
            consumeIngredients(container);
        }

        updateResultFromContainer(container, player);
        shownResult.put(player.getUUID(), container.getItem(0).copy());
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
    }

    /**
     * Lay a recipe out in the grid from what the player is carrying.
     *
     * <p>The work a crafting table gets from vanilla for free. {@code ServerPlaceRecipe} only
     * serves a {@code RecipeBookMenu}, so a station has to do its own: empty the grid back to the
     * player, then take one of each ingredient out of their pack and put it where the recipe
     * says. Anything already on the bench goes back first, so picking a second recipe replaces
     * the first instead of refusing to fit beside it.
     *
     * <p>Ingredients are indexed {@code column + row * width} over the recipe's own box, the same
     * reading {@link FletchingRecipe#matches} uses, and the box is laid into the top-left of the
     * three by three - which is where the player would have put it.
     */
    private static void placeRecipe(ServerPlayer player, RecipeHolder<?> holder, boolean useMaxItems) {
        SimpleContainer container = craftingContainers.get(player.getUUID());
        if (container == null) return;
        if (!(holder.value() instanceof FletchingRecipe recipe)) return;

        returnGrid(player, container);

        List<Ingredient> ingredients = recipe.getIngredients();
        int width = Math.max(1, recipe.getWidth());

        for (int i = 0; i < ingredients.size(); i++) {
            int column = i % width;
            int row = i / width;
            if (column >= GRID_SIDE || row >= GRID_SIDE) continue;

            ItemStack taken = takeOneMatching(player, ingredients.get(i));
            if (taken.isEmpty()) continue;
            container.setItem(1 + row * GRID_SIDE + column, taken);
        }

        // A grid that was filled by hand ends its click in afterSlotClick; this one has to say
        // the same thing itself, or the result slot stays empty until something else is clicked
        updateResultFromContainer(container, player);
        shownResult.put(player.getUUID(), container.getItem(0).copy());
        if (player.containerMenu != null) player.containerMenu.broadcastChanges();
    }

    /** Empty the crafting grid back into the player's pack. */
    private static void returnGrid(ServerPlayer player, SimpleContainer container) {
        for (int slot = 1; slot <= GRID_SIDE * GRID_SIDE; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false, Prediction.SERVER_ONLY);
            }
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Take a single item matching this ingredient out of the player's pack.
     *
     * <p>The hotbar included, and the worn gear not: {@code INVENTORY_SIZE} is where the part a
     * player can rummage through ends, and taking a helmet off somebody's head to craft with is
     * not what picking a recipe means.
     */
    private static ItemStack takeOneMatching(ServerPlayer player, Ingredient ingredient) {
        var pack = player.getInventory();
        for (int slot = 0; slot < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = pack.getItem(slot);
            if (stack.isEmpty() || !ingredient.test(stack)) continue;
            return stack.split(1);
        }
        return ItemStack.EMPTY;
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
