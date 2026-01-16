package justfatlard.fletch_craft.screen;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.recipe.FletchingRecipe;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FletchingGui extends SimpleGui implements InventoryChangedListener {
	private static final Text TITLE = Text.translatable("container.fletch_craft.fletching");

	// Slot indices for CRAFTING screen type
	// 0 = result, 1-9 = crafting grid (row by row)
	private static final int RESULT_SLOT = 0;
	private static final int GRID_START = 1;
	private static final int GRID_SIZE = 9;

	// Real inventory backing for the crafting grid
	private final SimpleInventory craftingInventory = new SimpleInventory(9);
	private final ServerWorld serverWorld;
	private ItemStack resultStack = ItemStack.EMPTY;
	private RecipeEntry<FletchingRecipe> currentRecipe = null;

	public FletchingGui(ServerPlayerEntity player, ServerWorld world) {
		super(ScreenHandlerType.CRAFTING, player, false);
		this.setTitle(TITLE);
		this.serverWorld = world;

		// Listen for inventory changes to update result
		this.craftingInventory.addListener(this);

		// Redirect crafting grid slots to our real inventory
		for (int i = 0; i < 9; i++) {
			this.setSlotRedirect(GRID_START + i, new Slot(craftingInventory, i, 0, 0));
		}

		// Initialize result display
		updateResult();
	}

	@Override
	public void onInventoryChanged(Inventory inventory) {
		// Called when items in the crafting inventory change
		updateResult();
	}

	@Override
	public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
		// Allow manipulation of redirected slots (the crafting grid)
		return true;
	}

	@Override
	public boolean onClick(int index, ClickType type, SlotActionType action, GuiElementInterface element) {
		// Handle result slot specially
		if (index == RESULT_SLOT) {
			if (!resultStack.isEmpty() && currentRecipe != null) {
				handleResultTake(action);
			}
			return true;
		}

		// Let redirected slots handle their own clicks
		if (index >= GRID_START && index < GRID_START + GRID_SIZE) {
			return false; // Let slot redirect handle it
		}

		// Let parent handle player inventory slots
		return super.onClick(index, type, action, element);
	}

	private void handleResultTake(SlotActionType action) {
		if (currentRecipe == null || resultStack.isEmpty()) {
			return;
		}

		ItemStack cursor = this.screenHandler.getCursorStack();

		if (action == SlotActionType.PICKUP) {
			// Single craft - left click
			if (cursor.isEmpty()) {
				this.screenHandler.setCursorStack(resultStack.copy());
				consumeIngredients();
			} else if (ItemStack.areItemsAndComponentsEqual(cursor, resultStack) &&
					   cursor.getCount() + resultStack.getCount() <= cursor.getMaxCount()) {
				cursor.increment(resultStack.getCount());
				consumeIngredients();
			}
		} else if (action == SlotActionType.QUICK_MOVE) {
			// Shift-click - craft as many as possible
			int maxCrafts = calculateMaxCrafts();
			for (int i = 0; i < maxCrafts; i++) {
				if (!canCraft()) break;

				ItemStack crafted = resultStack.copy();
				if (!this.player.getInventory().insertStack(crafted)) {
					this.player.dropItem(crafted, false);
				}
				consumeIngredients();
				updateResult();
			}
		}

		updateResult();
	}

	private boolean canCraft() {
		if (currentRecipe == null) return false;
		CraftingRecipeInput input = createRecipeInput();
		return currentRecipe.value().matches(input, this.serverWorld);
	}

	private int calculateMaxCrafts() {
		if (currentRecipe == null) return 0;

		int maxCrafts = Integer.MAX_VALUE;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = craftingInventory.getStack(i);
			if (!stack.isEmpty()) {
				maxCrafts = Math.min(maxCrafts, stack.getCount());
			}
		}
		return maxCrafts == Integer.MAX_VALUE ? 0 : maxCrafts;
	}

	private void consumeIngredients() {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = craftingInventory.getStack(i);
			if (!stack.isEmpty()) {
				stack.decrement(1);
				if (stack.isEmpty()) {
					craftingInventory.setStack(i, ItemStack.EMPTY);
				}
			}
		}
	}

	private void updateResult() {
		CraftingRecipeInput input = createRecipeInput();

		Optional<RecipeEntry<FletchingRecipe>> matchResult = this.serverWorld
			.getRecipeManager()
			.getFirstMatch(FletchCraft.FLETCHING_RECIPE_TYPE, input, this.serverWorld);

		if (matchResult.isPresent()) {
			currentRecipe = matchResult.get();
			resultStack = currentRecipe.value().craft(input, this.serverWorld.getRegistryManager());
		} else {
			currentRecipe = null;
			resultStack = ItemStack.EMPTY;
		}

		this.setSlot(RESULT_SLOT, resultStack.copy());
	}

	private CraftingRecipeInput createRecipeInput() {
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			items.add(craftingInventory.getStack(i).copy());
		}
		return CraftingRecipeInput.create(3, 3, items);
	}

	@Override
	public void onClose() {
		// Remove listener
		this.craftingInventory.removeListener(this);

		// Drop any items left in the crafting grid
		for (int i = 0; i < 9; i++) {
			ItemStack stack = craftingInventory.getStack(i);
			if (!stack.isEmpty()) {
				this.player.dropItem(stack, false);
			}
		}
		super.onClose();
	}
}
