package justfatlard.fletch_craft.screen;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FletchingScreen extends HandledScreen<FletchingScreenHandler> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/crafting_table.png");
	private static final ButtonTextures RECIPE_BOOK_BUTTON_TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("recipe_book/button"),
		Identifier.ofVanilla("recipe_book/button_highlighted")
	);

	private boolean recipeBookOpen = false;
	private TexturedButtonWidget recipeBookButton;
	private List<SimpleRecipe> recipes = new ArrayList<>();
	private int scrollOffset = 0;
	private SimpleRecipe ghostRecipe = null; // Recipe to show as ghost preview

	// Simple recipe representation for display
	private record SimpleRecipe(ItemStack result, Item[] ingredients, int width, int height) {}

	public FletchingScreen(FletchingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		this.titleX = 29;
		this.titleY = 6;

		// Add recipe book toggle button
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;

		this.recipeBookButton = this.addDrawableChild(new TexturedButtonWidget(
			x + 5, y + 34, 20, 18,
			RECIPE_BOOK_BUTTON_TEXTURES,
			button -> toggleRecipeBook()
		));

		// Hardcode known fletching recipes
		this.recipes.clear();
		// Arrows (3x3)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.ARROW, 16),
			new Item[]{Items.FLINT, Items.FLINT, Items.FLINT, Items.STICK, Items.STICK, Items.STICK, Items.FEATHER, Items.FEATHER, Items.FEATHER}, 3, 3));
		// Flint from gravel (2x2)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.FLINT, 1),
			new Item[]{Items.GRAVEL, Items.GRAVEL, Items.GRAVEL, Items.GRAVEL}, 2, 2));
		// Sticks from planks (1x1)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 3),
			new Item[]{Items.OAK_PLANKS}, 1, 1));
		// Sticks from stripped logs
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 16),
			new Item[]{Items.STRIPPED_OAK_LOG}, 1, 1));
		// Strip oak log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_OAK_LOG, 1),
			new Item[]{Items.OAK_LOG}, 1, 1));
		// Strip birch log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_BIRCH_LOG, 1),
			new Item[]{Items.BIRCH_LOG}, 1, 1));
		// Strip spruce log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_SPRUCE_LOG, 1),
			new Item[]{Items.SPRUCE_LOG}, 1, 1));
		// Strip jungle log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_JUNGLE_LOG, 1),
			new Item[]{Items.JUNGLE_LOG}, 1, 1));
		// Strip acacia log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_ACACIA_LOG, 1),
			new Item[]{Items.ACACIA_LOG}, 1, 1));
		// Strip dark oak log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_DARK_OAK_LOG, 1),
			new Item[]{Items.DARK_OAK_LOG}, 1, 1));
		// Strip cherry log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_CHERRY_LOG, 1),
			new Item[]{Items.CHERRY_LOG}, 1, 1));
		// Strip mangrove log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_MANGROVE_LOG, 1),
			new Item[]{Items.MANGROVE_LOG}, 1, 1));
		// Strip pale oak log
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_PALE_OAK_LOG, 1),
			new Item[]{Items.PALE_OAK_LOG}, 1, 1));
		// Strip crimson stem
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_CRIMSON_STEM, 1),
			new Item[]{Items.CRIMSON_STEM}, 1, 1));
		// Strip warped stem
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_WARPED_STEM, 1),
			new Item[]{Items.WARPED_STEM}, 1, 1));
		// Strip bamboo block
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRIPPED_BAMBOO_BLOCK, 1),
			new Item[]{Items.BAMBOO_BLOCK}, 1, 1));
		// Bamboo to sticks
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STICK, 2),
			new Item[]{Items.BAMBOO}, 1, 1));
		// Spectral arrows
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.SPECTRAL_ARROW, 3),
			new Item[]{Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.GLOWSTONE_DUST, Items.ARROW, Items.ARROW, Items.ARROW}, 3, 2));
		// Wool to string (using white wool as representative)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.STRING, 9),
			new Item[]{Items.WHITE_WOOL}, 1, 1));
		// Target block
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.TARGET, 1),
			new Item[]{Items.REDSTONE, Items.HAY_BLOCK}, 1, 2));
		// Bow (2x3: sticks left, strings right)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.BOW, 1),
			new Item[]{Items.STICK, Items.STRING, Items.STICK, Items.STRING, Items.STICK, Items.STRING}, 2, 3));
		// Crossbow (3x2 simplified)
		this.recipes.add(new SimpleRecipe(new ItemStack(Items.CROSSBOW, 1),
			new Item[]{Items.STICK, Items.IRON_NUGGET, Items.STICK, Items.STRING, Items.STICK, Items.STRING}, 3, 2));
	}

	private void toggleRecipeBook() {
		this.recipeBookOpen = !this.recipeBookOpen;
		if (this.recipeBookOpen) {
			// Shift the main GUI to the right when recipe book is open
			this.x = (this.width - this.backgroundWidth) / 2 + 77;
		} else {
			this.x = (this.width - this.backgroundWidth) / 2;
		}
		// Reposition the button
		this.recipeBookButton.setX(this.x + 5);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		if (this.recipeBookOpen) {
			// Draw recipe book panel background
			int panelX = this.x - 147;
			int panelY = this.y;
			context.fill(panelX, panelY, panelX + 147, panelY + this.backgroundHeight, 0xCC000000);
			// Draw border manually (top, bottom, left, right)
			int borderColor = 0xFF555555;
			context.fill(panelX, panelY, panelX + 147, panelY + 1, borderColor); // top
			context.fill(panelX, panelY + this.backgroundHeight - 1, panelX + 147, panelY + this.backgroundHeight, borderColor); // bottom
			context.fill(panelX, panelY, panelX + 1, panelY + this.backgroundHeight, borderColor); // left
			context.fill(panelX + 146, panelY, panelX + 147, panelY + this.backgroundHeight, borderColor); // right

			// Draw title
			var tr = this.client.textRenderer;
			context.drawTextWithBackground(tr, Text.literal("Recipes"), panelX + 10, panelY + 6, 100, 0xFFFFFFFF);

			// Draw recipe icons in a grid (7 columns x 8 rows)
			int iconSize = 18;
			int cols = 7;
			int rows = 8;
			int startX = panelX + 5;
			int startY = panelY + 20;

			for (int i = 0; i < Math.min(recipes.size(), cols * rows); i++) {
				int col = i % cols;
				int row = i / cols;
				int iconX = startX + col * iconSize;
				int iconY = startY + row * iconSize;

				SimpleRecipe recipe = recipes.get(i + scrollOffset);
				ItemStack result = recipe.result();

				// Draw slot background
				boolean hovered = mouseX >= iconX && mouseX < iconX + iconSize &&
								  mouseY >= iconY && mouseY < iconY + iconSize;
				context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, hovered ? 0x80FFFFFF : 0x40FFFFFF);

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

		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean hasShift) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		if (this.recipeBookOpen && button == 0) {
			int panelX = this.x - 147;
			int panelY = this.y;
			int iconSize = 18;
			int cols = 7;
			int rows = 8;
			int startX = panelX + 5;
			int startY = panelY + 20;

			for (int i = 0; i < Math.min(recipes.size(), cols * rows); i++) {
				int col = i % cols;
				int row = i / cols;
				int iconX = startX + col * iconSize;
				int iconY = startY + row * iconSize;

				if (mouseX >= iconX && mouseX < iconX + iconSize &&
					mouseY >= iconY && mouseY < iconY + iconSize) {
					// Recipe clicked - populate the grid
					// Shift-click fills max, regular click adds one set
					SimpleRecipe recipe = recipes.get(i + scrollOffset);
					long handle = this.client.getWindow().getHandle();
					boolean shiftHeld = org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
						|| org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
					populateGrid(recipe, shiftHeld);
					return true;
				}
			}
		}
		return super.mouseClicked(click, hasShift);
	}

	private void populateGrid(SimpleRecipe recipe, boolean fillMax) {
		// Get recipe ingredients
		Item[] ingredients = recipe.ingredients();
		int width = recipe.width();
		int height = recipe.height();

		// First, count how many of each ingredient we need per set
		java.util.Map<Item, Integer> neededPerSet = new java.util.HashMap<>();
		for (Item ingredient : ingredients) {
			if (ingredient != null && ingredient != Items.AIR) {
				neededPerSet.merge(ingredient, 1, Integer::sum);
			}
		}

		// Count available in inventory (in handler slots)
		java.util.Map<Item, Integer> available = new java.util.HashMap<>();
		for (int j = 10; j < this.handler.slots.size(); j++) { // slots 10+ are player inventory
			ItemStack stack = this.handler.slots.get(j).getStack();
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
			// Clear grid first before showing ghost
			for (int k = 1; k <= 9; k++) {
				if (!this.handler.slots.get(k).getStack().isEmpty()) {
					this.client.interactionManager.clickSlot(
						this.handler.syncId, k, 0,
						net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
						this.client.player
					);
				}
			}
			// Show ghost preview when no ingredients available
			this.ghostRecipe = recipe;
			return;
		}

		// Clear ghost when we can craft
		this.ghostRecipe = null;

		// Check current grid state
		int currentMinInGrid = 64;
		boolean needsClear = false;
		for (int i = 0; i < ingredients.length && i < 9; i++) {
			Item ingredient = ingredients[i];
			if (ingredient == null || ingredient == Items.AIR) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1; // +1 because slot 0 is output

			ItemStack existing = this.handler.slots.get(gridSlot).getStack();
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

		// Clear grid if needed (different recipe)
		if (needsClear) {
			for (int k = 1; k <= 9; k++) {
				if (!this.handler.slots.get(k).getStack().isEmpty()) {
					// Quick move to return to inventory
					this.client.interactionManager.clickSlot(
						this.handler.syncId, k, 0,
						net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
						this.client.player
					);
				}
			}
			currentMinInGrid = 0;
		}

		// Limit sets to not exceed stack size
		int canAdd = Math.min(maxSets, 64 - currentMinInGrid);
		if (canAdd <= 0) return;

		// Now transfer items using proper slot clicks
		for (int i = 0; i < ingredients.length && i < 9; i++) {
			Item ingredient = ingredients[i];
			if (ingredient == null || ingredient == Items.AIR) continue;

			int recipeX = i % width;
			int recipeY = i / width;
			int gridSlot = recipeX + recipeY * 3 + 1; // +1 because slot 0 is output

			int needed = canAdd;

			// Find matching items in player inventory slots and transfer
			for (int j = 10; j < this.handler.slots.size() && needed > 0; j++) {
				ItemStack sourceStack = this.handler.slots.get(j).getStack();
				if (!sourceStack.isEmpty() && sourceStack.isOf(ingredient)) {
					int transfer = Math.min(needed, sourceStack.getCount());

					// Pick up from source (left click)
					this.client.interactionManager.clickSlot(
						this.handler.syncId, j, 0,
						net.minecraft.screen.slot.SlotActionType.PICKUP,
						this.client.player
					);

					// Right-click to place one at a time
					for (int t = 0; t < transfer; t++) {
						this.client.interactionManager.clickSlot(
							this.handler.syncId, gridSlot, 1, // right-click places 1
							net.minecraft.screen.slot.SlotActionType.PICKUP,
							this.client.player
						);
					}

					// Always put cursor back in source slot (left click puts down whatever is held)
					this.client.interactionManager.clickSlot(
						this.handler.syncId, j, 0,
						net.minecraft.screen.slot.SlotActionType.PICKUP,
						this.client.player
					);

					needed -= transfer;
				}
			}
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

		// Draw ghost recipe preview (clear ghost if player manually adds items)
		if (this.ghostRecipe != null) {
			for (int slot = 1; slot <= 9; slot++) {
				if (!this.handler.slots.get(slot).getStack().isEmpty()) {
					this.ghostRecipe = null;
					break;
				}
			}
		}

		if (this.ghostRecipe != null) {
			Item[] ingredients = this.ghostRecipe.ingredients();
			int width = this.ghostRecipe.width();

			for (int i = 0; i < ingredients.length && i < 9; i++) {
				Item ingredient = ingredients[i];
				if (ingredient == null || ingredient == Items.AIR) continue;

				int recipeX = i % width;
				int recipeY = i / width;
				int gridSlot = recipeX + recipeY * 3 + 1;

				// Only show ghost if slot is empty
				if (this.handler.slots.get(gridSlot).getStack().isEmpty()) {
					// Calculate slot position (crafting grid starts at 30,17 with 18px spacing)
					int slotX = this.x + 30 + recipeX * 18;
					int slotY = this.y + 17 + recipeY * 18;

					// Draw semi-transparent item with gray overlay
					context.drawItem(new ItemStack(ingredient), slotX, slotY);
					// Draw semi-transparent overlay to fade the item (ghost effect)
					context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80C6C6C6);
				}
			}
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
		context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
	}
}
