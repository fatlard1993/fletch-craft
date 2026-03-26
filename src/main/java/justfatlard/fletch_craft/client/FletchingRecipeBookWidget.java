package justfatlard.fletch_craft.client;

import justfatlard.fletch_craft.FletchCraftClient;
import justfatlard.fletch_craft.network.FletchingRecipeSyncPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FletchingRecipeBookWidget {
	private static final int MAX_PANEL_WIDTH = 147;
	private static final int PANEL_HEIGHT = 166;
	private static final int ICON_SIZE = 18;
	private static final int PADDING = 5;
	private static final int INVENTORY_START = 10;

	private boolean recipesLoaded = false;
	private MinecraftClient client;
	private CraftingScreenHandler handler;
	private int currentPanelX;
	private int currentPanelY;
	private int currentPanelWidth;
	private int currentCols;

	private final List<FletchingRecipeSyncPayload.RecipeData> recipes = new ArrayList<>();

	public void initialize(MinecraftClient client, CraftingScreenHandler handler) {
		this.client = client;
		this.handler = handler;

		recipes.clear();
		List<FletchingRecipeSyncPayload.RecipeData> synced = FletchCraftClient.getSyncedRecipes();
		recipes.addAll(synced);
		recipesLoaded = !synced.isEmpty();
	}

	public void render(DrawContext context, int mouseX, int mouseY, float delta, int guiX, int guiY) {
		if (!recipesLoaded) {
			List<FletchingRecipeSyncPayload.RecipeData> synced = FletchCraftClient.getSyncedRecipes();
			if (!synced.isEmpty()) {
				recipes.clear();
				recipes.addAll(synced);
				recipesLoaded = true;
			}
		}
		if (recipes.isEmpty()) return;

		int availableWidth = guiX - 4;
		int cols = Math.max(2, Math.min(7, (availableWidth - PADDING * 2) / ICON_SIZE));
		int panelWidth = Math.min(MAX_PANEL_WIDTH, cols * ICON_SIZE + PADDING * 2);

		int panelX = guiX - panelWidth - 2;
		if (panelX < 2) panelX = 2;

		int panelY = guiY;
		this.currentPanelX = panelX;
		this.currentPanelY = panelY;
		this.currentPanelWidth = panelWidth;
		this.currentCols = cols;

		context.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xCC000000);

		int borderColor = 0xFF555555;
		context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, borderColor);
		context.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + panelWidth, panelY + PANEL_HEIGHT, borderColor);
		context.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, borderColor);
		context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, borderColor);

		var tr = client.textRenderer;
		context.drawTextWithBackground(tr, Text.literal("Fletching Recipes"), panelX + 10, panelY + 6, 100, 0xFFFFFFFF);

		int rows = (PANEL_HEIGHT - 25) / ICON_SIZE;
		int startX = panelX + PADDING;
		int startY = panelY + 20;

		FletchingRecipeSyncPayload.RecipeData hoveredRecipe = null;
		int tooltipX = 0, tooltipY = 0;

		for (int i = 0; i < Math.min(recipes.size(), cols * rows); i++) {
			int col = i % cols;
			int row = i / cols;
			int iconX = startX + col * ICON_SIZE;
			int iconY = startY + row * ICON_SIZE;

			FletchingRecipeSyncPayload.RecipeData recipe = recipes.get(i);
			ItemStack result = recipe.result();

			boolean hovered = mouseX >= iconX && mouseX < iconX + ICON_SIZE &&
							  mouseY >= iconY && mouseY < iconY + ICON_SIZE;
			context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, hovered ? 0x80FFFFFF : 0x40FFFFFF);
			context.drawItem(result, iconX + 1, iconY + 1);

			if (result.getCount() > 1) {
				context.drawStackOverlay(tr, result, iconX + 1, iconY + 1);
			}

			if (hovered) {
				hoveredRecipe = recipe;
				tooltipX = mouseX;
				tooltipY = mouseY;
			}
		}

		if (hoveredRecipe != null) {
			context.drawTooltip(tr, hoveredRecipe.result().getName(), tooltipX, tooltipY);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, CraftingScreenHandler handler, MinecraftClient client) {
		if (currentCols == 0) return false;

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
					FletchingRecipeSyncPayload.RecipeData recipe = recipes.get(i);
					boolean shiftHeld = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
						|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
					populateGrid(recipe, shiftHeld, handler, client);
					return true;
				}
			}
		}

		return false;
	}

	private void populateGrid(FletchingRecipeSyncPayload.RecipeData recipe, boolean fillMax, CraftingScreenHandler handler, MinecraftClient client) {
		List<Ingredient> ingredients = recipe.ingredients();
		int width = recipe.width();

		Map<Item, Integer> neededPerSet = new LinkedHashMap<>();

		for (int i = 0; i < ingredients.size(); i++) {
			Ingredient ingredient = ingredients.get(i);
			if (ingredient.isEmpty()) continue;

			Item representative = findRepresentativeItem(ingredient, handler);
			if (representative == null) {
				clearGrid(handler, client);
				return;
			}
			neededPerSet.merge(representative, 1, Integer::sum);
		}

		Map<Item, Integer> available = new LinkedHashMap<>();
		for (int j = INVENTORY_START; j < handler.slots.size(); j++) {
			ItemStack stack = handler.slots.get(j).getStack();
			if (!stack.isEmpty() && neededPerSet.containsKey(stack.getItem())) {
				available.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}

		int maxSets = fillMax ? 64 : 1;
		for (var entry : neededPerSet.entrySet()) {
			int have = available.getOrDefault(entry.getKey(), 0);
			int need = entry.getValue();
			if (need > 0) {
				maxSets = Math.min(maxSets, have / need);
			}
		}

		if (maxSets <= 0) {
			clearGrid(handler, client);
			return;
		}

		int currentMinInGrid = 64;
		boolean needsClear = false;
		for (int i = 0; i < ingredients.size() && i < 9; i++) {
			Ingredient ingredient = ingredients.get(i);
			if (ingredient.isEmpty()) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1;

			ItemStack existing = handler.slots.get(gridSlot).getStack();
			if (!existing.isEmpty()) {
				if (!ingredient.test(existing)) {
					needsClear = true;
					break;
				}
				currentMinInGrid = Math.min(currentMinInGrid, existing.getCount());
			} else {
				currentMinInGrid = 0;
			}
		}

		if (needsClear) {
			clearGrid(handler, client);
			currentMinInGrid = 0;
		}

		int canAdd = Math.min(maxSets, 64 - currentMinInGrid);
		if (canAdd <= 0) return;

		for (int i = 0; i < ingredients.size() && i < 9; i++) {
			Ingredient ingredient = ingredients.get(i);
			if (ingredient.isEmpty()) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1;

			int needed = canAdd;

			for (int j = INVENTORY_START; j < handler.slots.size() && needed > 0; j++) {
				ItemStack sourceStack = handler.slots.get(j).getStack();
				if (!sourceStack.isEmpty() && ingredient.test(sourceStack)) {
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

	private Item findRepresentativeItem(Ingredient ingredient, CraftingScreenHandler handler) {
		for (int j = INVENTORY_START; j < handler.slots.size(); j++) {
			ItemStack stack = handler.slots.get(j).getStack();
			if (!stack.isEmpty() && ingredient.test(stack)) {
				return stack.getItem();
			}
		}
		return null;
	}

	private void clearGrid(CraftingScreenHandler handler, MinecraftClient client) {
		for (int k = 1; k <= 9; k++) {
			if (!handler.slots.get(k).getStack().isEmpty()) {
				client.interactionManager.clickSlot(
					handler.syncId, k, 0,
					SlotActionType.QUICK_MOVE,
					client.player
				);
			}
		}
	}
}
