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
	// Opens as ScreenHandlerType.CRAFTING so vanilla clients see a crafting screen.
	// The client mixin detects fletching tables by matching this title key.
	private static final Text TITLE = Text.translatable(FletchCraft.TITLE_KEY);

	private static final int RESULT_SLOT = 0;
	private static final int GRID_START = 1;
	private static final int GRID_SIZE = 9;

	private final SimpleInventory craftingInventory = new SimpleInventory(9);
	private final ServerWorld serverWorld;
	private ItemStack resultStack = ItemStack.EMPTY;
	private RecipeEntry<FletchingRecipe> currentRecipe = null;

	public FletchingGui(ServerPlayerEntity player, ServerWorld world) {
		super(ScreenHandlerType.CRAFTING, player, false);
		this.setTitle(TITLE);
		this.serverWorld = world;

		this.craftingInventory.addListener(this);

		for (int i = 0; i < 9; i++) {
			this.setSlotRedirect(GRID_START + i, new Slot(craftingInventory, i, 0, 0));
		}

		updateResult();
	}

	@Override
	public void onInventoryChanged(Inventory inventory) {
		updateResult();
	}

	@Override
	public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
		return true;
	}

	@Override
	public boolean onClick(int index, ClickType type, SlotActionType action, GuiElementInterface element) {
		if (index == RESULT_SLOT) {
			if (!resultStack.isEmpty() && currentRecipe != null) {
				handleResultTake(action, type);
			}
			return true;
		}

		if (index >= GRID_START && index < GRID_START + GRID_SIZE) {
			return false;
		}

		return super.onClick(index, type, action, element);
	}

	private void handleResultTake(SlotActionType action, ClickType clickType) {
		if (currentRecipe == null || resultStack.isEmpty()) {
			return;
		}

		ItemStack cursor = this.screenHandler.getCursorStack();

		if (action == SlotActionType.PICKUP) {
			if (cursor.isEmpty()) {
				this.screenHandler.setCursorStack(resultStack.copy());
				consumeIngredients();
			} else if (ItemStack.areItemsAndComponentsEqual(cursor, resultStack) &&
					   cursor.getCount() + resultStack.getCount() <= cursor.getMaxCount()) {
				cursor.increment(resultStack.getCount());
				consumeIngredients();
			}
		} else if (action == SlotActionType.QUICK_MOVE) {
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
		} else if (action == SlotActionType.THROW) {
			ItemStack crafted = resultStack.copy();
			this.player.dropItem(crafted, false);
			consumeIngredients();
		} else if (action == SlotActionType.SWAP && clickType.numKey) {
			int hotbarSlot = clickType.value;
			ItemStack hotbarStack = this.player.getInventory().getStack(hotbarSlot);
			if (hotbarStack.isEmpty()) {
				this.player.getInventory().setStack(hotbarSlot, resultStack.copy());
				consumeIngredients();
			} else if (ItemStack.areItemsAndComponentsEqual(hotbarStack, resultStack) &&
					   hotbarStack.getCount() + resultStack.getCount() <= hotbarStack.getMaxCount()) {
				hotbarStack.increment(resultStack.getCount());
				consumeIngredients();
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

		FletchingRecipe recipe = currentRecipe.value();
		int width = recipe.getWidth();
		int height = recipe.getHeight();
		int maxCrafts = Integer.MAX_VALUE;

		for (int i = 0; i < recipe.getIngredients().size(); i++) {
			if (recipe.getIngredients().get(i).isEmpty()) continue;
			int gridX = i % width;
			int gridY = i / width;
			int slotIndex = gridX + gridY * 3;
			ItemStack stack = craftingInventory.getStack(slotIndex);
			if (stack.isEmpty()) return 0;
			maxCrafts = Math.min(maxCrafts, stack.getCount());
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
		this.craftingInventory.removeListener(this);

		ItemStack cursorStack = this.screenHandler.getCursorStack();
		if (!cursorStack.isEmpty()) {
			this.player.dropItem(cursorStack, false);
			this.screenHandler.setCursorStack(ItemStack.EMPTY);
		}

		for (int i = 0; i < 9; i++) {
			ItemStack stack = craftingInventory.getStack(i);
			if (!stack.isEmpty()) {
				this.player.dropItem(stack, false);
			}
		}
		craftingInventory.clear();
		super.onClose();
	}
}
