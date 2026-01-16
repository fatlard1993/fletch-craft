package justfatlard.fletch_craft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom recipe book widget for the fletching table that shows only fletching recipes.
 */
public class FletchingRecipeBookWidget {
	private static final int MAX_PANEL_WIDTH = 147;
	private static final int PANEL_HEIGHT = 166;
	private static final int CRAFTING_GUI_WIDTH = 176;
	private static final int ICON_SIZE = 18;
	private static final int PADDING = 5;

	private boolean open = true; // Start open by default for fletching table
	private int screenWidth;
	private int screenHeight;
	private MinecraftClient client;
	private CraftingScreenHandler handler;
	private int currentPanelX; // Updated during render for mouse click handling
	private int currentPanelY;
	private int currentPanelWidth; // Dynamic width based on available space
	private int currentCols; // Number of columns that fit

	private final List<SimpleRecipe> recipes = new ArrayList<>();
	private SimpleRecipe ghostRecipe = null;

	// Simple recipe representation for display
	public record SimpleRecipe(ItemStack result, Item[] ingredients, int width, int height) {}

	public void initialize(int screenWidth, int screenHeight, MinecraftClient client, CraftingScreenHandler handler) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.client = client;
		this.handler = handler;

		// Initialize recipes
		recipes.clear();
		// Arrows (3x3)
		recipes.add(new SimpleRecipe(new ItemStack(Items.ARROW, 16),
			new Item[]{Items.FLINT, Items.FLINT, Items.FLINT, Items.STICK, Items.STICK, Items.STICK, Items.FEATHER, Items.FEATHER, Items.FEATHER}, 3, 3));
		// Flint from gravel (2x2)
		recipes.add(new SimpleRecipe(new ItemStack(Items.FLINT, 1),
			new Item[]{Items.GRAVEL, Items.GRAVEL, Items.GRAVEL, Items.GRAVEL}, 2, 2));
		// Sticks from planks (1x1)
		recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 3),
			new Item[]{Items.OAK_PLANKS}, 1, 1));
		// Sticks from stripped logs
		recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 16),
			new Item[]{Items.STRIPPED_OAK_LOG}, 1, 1));
		// Strip oak log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_OAK_LOG, 1),
			new Item[]{Items.OAK_LOG}, 1, 1));
		// Strip birch log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_BIRCH_LOG, 1),
			new Item[]{Items.BIRCH_LOG}, 1, 1));
		// Strip spruce log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_SPRUCE_LOG, 1),
			new Item[]{Items.SPRUCE_LOG}, 1, 1));
		// Strip jungle log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_JUNGLE_LOG, 1),
			new Item[]{Items.JUNGLE_LOG}, 1, 1));
		// Strip acacia log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_ACACIA_LOG, 1),
			new Item[]{Items.ACACIA_LOG}, 1, 1));
		// Strip dark oak log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_DARK_OAK_LOG, 1),
			new Item[]{Items.DARK_OAK_LOG}, 1, 1));
		// Strip cherry log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_CHERRY_LOG, 1),
			new Item[]{Items.CHERRY_LOG}, 1, 1));
		// Strip mangrove log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_MANGROVE_LOG, 1),
			new Item[]{Items.MANGROVE_LOG}, 1, 1));
		// Strip pale oak log
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_PALE_OAK_LOG, 1),
			new Item[]{Items.PALE_OAK_LOG}, 1, 1));
		// Strip crimson stem
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_CRIMSON_STEM, 1),
			new Item[]{Items.CRIMSON_STEM}, 1, 1));
		// Strip warped stem
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_WARPED_STEM, 1),
			new Item[]{Items.WARPED_STEM}, 1, 1));
		// Strip bamboo block
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_BAMBOO_BLOCK, 1),
			new Item[]{Items.BAMBOO_BLOCK}, 1, 1));
		// Bamboo to sticks
		recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 2),
			new Item[]{Items.BAMBOO}, 1, 1));
		// Spectral arrows
		recipes.add(new SimpleRecipe(new ItemStack(Items.SPECTRAL_ARROW, 3),
			new Item[]{Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.ARROW, Items.ARROW, Items.ARROW}, 3, 2));
		// Wool to string (using white wool as representative)
		recipes.add(new SimpleRecipe(new ItemStack(Items.STRING, 9),
			new Item[]{Items.WHITE_WOOL}, 1, 1));
		// Target block
		recipes.add(new SimpleRecipe(new ItemStack(Items.TARGET, 1),
			new Item[]{Items.REDSTONE, Items.HAY_BLOCK}, 1, 2));
		// Bow (2x3: sticks left, strings right)
		recipes.add(new SimpleRecipe(new ItemStack(Items.BOW, 1),
			new Item[]{Items.STICK, Items.STRING, Items.STICK, Items.STRING, Items.STICK, Items.STRING}, 2, 3));
		// Crossbow (3x2 simplified)
		recipes.add(new SimpleRecipe(new ItemStack(Items.CROSSBOW, 1),
			new Item[]{Items.STICK, Items.IRON_NUGGET, Items.STICK, Items.STRING, Items.STICK, Items.STRING}, 3, 2));
	}

	public boolean isOpen() {
		return open;
	}

	public void toggleOpen() {
		this.open = !this.open;
	}

	public void render(DrawContext context, int mouseX, int mouseY, float delta, int guiX, int guiY) {
		if (!open) return;

		// Calculate available space to the left of the crafting GUI
		int availableWidth = guiX - 4; // Leave 4px gap

		// Calculate how many columns fit (minimum 2, respect available space)
		int cols = Math.max(2, Math.min(7, (availableWidth - PADDING * 2) / ICON_SIZE));
		int panelWidth = Math.min(MAX_PANEL_WIDTH, cols * ICON_SIZE + PADDING * 2);

		// Position panel to the left of the crafting GUI
		int panelX = guiX - panelWidth - 2;
		if (panelX < 2) panelX = 2;

		int panelY = guiY;
		this.currentPanelX = panelX;
		this.currentPanelY = panelY;
		this.currentPanelWidth = panelWidth;
		this.currentCols = cols;

		context.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xCC000000);

		// Draw border
		int borderColor = 0xFF555555;
		context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, borderColor); // top
		context.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + panelWidth, panelY + PANEL_HEIGHT, borderColor); // bottom
		context.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, borderColor); // left
		context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, borderColor); // right

		// Draw title
		var tr = client.textRenderer;
		context.drawTextWithBackground(tr, Text.literal("Fletching Recipes"), panelX + 10, panelY + 6, 100, 0xFFFFFFFF);

		// Draw recipe icons in a grid using dynamic columns
		int rows = (PANEL_HEIGHT - 25) / ICON_SIZE; // Calculate rows that fit
		int startX = panelX + PADDING;
		int startY = panelY + 20;

		for (int i = 0; i < Math.min(recipes.size(), cols * rows); i++) {
			int col = i % cols;
			int row = i / cols;
			int iconX = startX + col * ICON_SIZE;
			int iconY = startY + row * ICON_SIZE;

			SimpleRecipe recipe = recipes.get(i);
			ItemStack result = recipe.result();

			// Draw slot background
			boolean hovered = mouseX >= iconX && mouseX < iconX + ICON_SIZE &&
							  mouseY >= iconY && mouseY < iconY + ICON_SIZE;
			context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, hovered ? 0x80FFFFFF : 0x40FFFFFF);

			// Draw item
			context.drawItem(result, iconX + 1, iconY + 1);

			// Draw count if more than 1
			if (result.getCount() > 1) {
				context.drawStackOverlay(tr, result, iconX + 1, iconY + 1);
			}

			// Draw tooltip on hover
			if (hovered) {
				context.drawTooltip(tr, result.getName(), mouseX, mouseY);
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, CraftingScreenHandler handler, MinecraftClient client) {
		if (!open || currentCols == 0) return false;

		// Use stored panel position from last render
		int startX = currentPanelX + PADDING;
		int startY = currentPanelY + 20;
		int rows = (PANEL_HEIGHT - 25) / ICON_SIZE;

		if (button == 0) {
			for (int i = 0; i < Math.min(recipes.size(), currentCols * rows); i++) {
				int col = i % currentCols;
				int row = i / currentCols;
				int iconX = startX + col * ICON_SIZE;
				int iconY = startY + row * ICON_SIZE;

				if (mouseX >= iconX && mouseX < iconX + ICON_SIZE &&
					mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
					// Recipe clicked - populate the grid
					SimpleRecipe recipe = recipes.get(i);
					long windowHandle = client.getWindow().getHandle();
					boolean shiftHeld = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
						|| org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
					populateGrid(recipe, shiftHeld, handler, client);
					return true;
				}
			}
		}

		return false;
	}

	private void populateGrid(SimpleRecipe recipe, boolean fillMax, CraftingScreenHandler handler, MinecraftClient client) {
		Item[] ingredients = recipe.ingredients();
		int width = recipe.width();

		// Count how many of each ingredient we need per set
		Map<Item, Integer> neededPerSet = new HashMap<>();
		for (Item ingredient : ingredients) {
			if (ingredient != null && ingredient != Items.AIR) {
				neededPerSet.merge(ingredient, 1, Integer::sum);
			}
		}

		// Count available in inventory (in handler slots)
		Map<Item, Integer> available = new HashMap<>();
		for (int j = 10; j < handler.slots.size(); j++) {
			ItemStack stack = handler.slots.get(j).getStack();
			if (!stack.isEmpty() && neededPerSet.containsKey(stack.getItem())) {
				available.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}

		// Calculate how many complete sets we can make
		int maxSets = fillMax ? 64 : 1;
		for (var entry : neededPerSet.entrySet()) {
			int have = available.getOrDefault(entry.getKey(), 0);
			int need = entry.getValue();
			if (need > 0) {
				maxSets = Math.min(maxSets, have / need);
			}
		}

		if (maxSets <= 0) {
			// Clear grid and show ghost preview
			for (int k = 1; k <= 9; k++) {
				if (!handler.slots.get(k).getStack().isEmpty()) {
					client.interactionManager.clickSlot(
						handler.syncId, k, 0,
						SlotActionType.QUICK_MOVE,
						client.player
					);
				}
			}
			this.ghostRecipe = recipe;
			return;
		}

		this.ghostRecipe = null;

		// Check current grid state
		int currentMinInGrid = 64;
		boolean needsClear = false;
		for (int i = 0; i < ingredients.length && i < 9; i++) {
			Item ingredient = ingredients[i];
			if (ingredient == null || ingredient == Items.AIR) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1;

			ItemStack existing = handler.slots.get(gridSlot).getStack();
			if (!existing.isEmpty()) {
				if (!existing.isOf(ingredient)) {
					needsClear = true;
					break;
				}
				currentMinInGrid = Math.min(currentMinInGrid, existing.getCount());
			} else {
				currentMinInGrid = 0;
			}
		}

		// Clear grid if needed
		if (needsClear) {
			for (int k = 1; k <= 9; k++) {
				if (!handler.slots.get(k).getStack().isEmpty()) {
					client.interactionManager.clickSlot(
						handler.syncId, k, 0,
						SlotActionType.QUICK_MOVE,
						client.player
					);
				}
			}
			currentMinInGrid = 0;
		}

		// Limit sets
		int canAdd = Math.min(maxSets, 64 - currentMinInGrid);
		if (canAdd <= 0) return;

		// Transfer items
		for (int i = 0; i < ingredients.length && i < 9; i++) {
			Item ingredient = ingredients[i];
			if (ingredient == null || ingredient == Items.AIR) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1;

			int needed = canAdd;

			for (int j = 10; j < handler.slots.size() && needed > 0; j++) {
				ItemStack sourceStack = handler.slots.get(j).getStack();
				if (!sourceStack.isEmpty() && sourceStack.isOf(ingredient)) {
					int transfer = Math.min(needed, sourceStack.getCount());

					client.interactionManager.clickSlot(
						handler.syncId, j, 0,
						SlotActionType.PICKUP,
						client.player
					);

					for (int t = 0; t < transfer; t++) {
						client.interactionManager.clickSlot(
							handler.syncId, gridSlot, 1,
							SlotActionType.PICKUP,
							client.player
						);
					}

					client.interactionManager.clickSlot(
						handler.syncId, j, 0,
						SlotActionType.PICKUP,
						client.player
					);

					needed -= transfer;
				}
			}
		}
	}
}
