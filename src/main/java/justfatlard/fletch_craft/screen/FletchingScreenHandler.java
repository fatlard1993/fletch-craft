package justfatlard.fletch_craft.screen;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.fletch_craft.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class FletchingScreenHandler extends ScreenHandler {
	private final RecipeInputInventory input = new CraftingInventory(this, 3, 3);
	private final CraftingResultInventory result = new CraftingResultInventory();
	private final ScreenHandlerContext context;
	private final PlayerEntity player;

	public FletchingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public FletchingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ModScreenHandlers.FLETCHING_SCREEN_HANDLER, syncId);
		this.context = context;
		this.player = playerInventory.player;

		// Result slot (index 0)
		this.addSlot(new FletchingResultSlot(playerInventory.player, this.input, this.result, 0, 124, 35, context, this));

		// Crafting grid 3x3 (indices 1-9)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				this.addSlot(new Slot(this.input, col + row * 3, 30 + col * 18, 17 + row * 18));
			}
		}

		// Player inventory (indices 10-36)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Player hotbar (indices 37-45)
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		this.context.run((world, pos) -> {
			updateResult(this, world, this.player, this.input, this.result);
		});
	}

	protected static void updateResult(ScreenHandler handler, World world, PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory) {
		if (world.isClient()) {
			return;
		}

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		ServerWorld serverWorld = (ServerWorld) world;

		// Create CraftingRecipeInput from inventory
		CraftingRecipeInput recipeInput = createInput(craftingInventory);

		// Find matching recipe using ServerRecipeManager
		ItemStack resultStack = ItemStack.EMPTY;
		ServerRecipeManager recipeManager = serverWorld.getRecipeManager();
		var matchResult = recipeManager.getFirstMatch(FletchCraft.FLETCHING_RECIPE_TYPE, recipeInput, serverWorld);
		if (matchResult.isPresent()) {
			FletchingRecipe recipe = matchResult.get().value();
			resultStack = recipe.craft(recipeInput, world.getRegistryManager());
		}

		resultInventory.setStack(0, resultStack);
		serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, resultStack));
	}

	private static CraftingRecipeInput createInput(RecipeInputInventory inventory) {
		return CraftingRecipeInput.create(3, 3, java.util.stream.IntStream.range(0, 9)
			.mapToObj(inventory::getStack)
			.toList());
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.context.run((world, pos) -> {
			this.dropInventory(player, this.input);
		});
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, net.minecraft.block.Blocks.FLETCHING_TABLE);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);

		if (slot != null && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();
			newStack = originalStack.copy();

			if (slotIndex == 0) {
				// Result slot - move to player inventory
				originalStack.getItem().onCraftByPlayer(originalStack, player);

				if (!this.insertItem(originalStack, 10, 46, true)) {
					return ItemStack.EMPTY;
				}

				slot.onQuickTransfer(originalStack, newStack);
			} else if (slotIndex >= 10 && slotIndex < 46) {
				// Player inventory - move to crafting grid
				if (!this.insertItem(originalStack, 1, 10, false)) {
					if (slotIndex < 37) {
						// Main inventory to hotbar
						if (!this.insertItem(originalStack, 37, 46, false)) {
							return ItemStack.EMPTY;
						}
					} else {
						// Hotbar to main inventory
						if (!this.insertItem(originalStack, 10, 37, false)) {
							return ItemStack.EMPTY;
						}
					}
				}
			} else if (!this.insertItem(originalStack, 10, 46, false)) {
				// Crafting grid to player inventory
				return ItemStack.EMPTY;
			}

			if (originalStack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}

			if (originalStack.getCount() == newStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTakeItem(player, originalStack);

			if (slotIndex == 0) {
				player.dropItem(originalStack, false);
			}
		}

		return newStack;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != this.result && super.canInsertIntoSlot(stack, slot);
	}

	public RecipeInputInventory getInput() {
		return this.input;
	}
}
