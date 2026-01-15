package justfatlard.fletch_craft.screen;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.recipe.FletchingRecipe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

public class FletchingResultSlot extends Slot {
	private final RecipeInputInventory craftingInventory;
	private final PlayerEntity player;
	private final ScreenHandlerContext context;
	private final FletchingScreenHandler handler;
	private int amount;

	public FletchingResultSlot(PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory inventory, int index, int x, int y, ScreenHandlerContext context, FletchingScreenHandler handler) {
		super(inventory, index, x, y);
		this.player = player;
		this.craftingInventory = craftingInventory;
		this.context = context;
		this.handler = handler;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack takeStack(int amount) {
		if (this.hasStack()) {
			this.amount += Math.min(amount, this.getStack().getCount());
		}
		return super.takeStack(amount);
	}

	@Override
	protected void onCrafted(ItemStack stack, int amount) {
		this.amount += amount;
		this.onCrafted(stack);
	}

	@Override
	protected void onCrafted(ItemStack stack) {
		if (this.amount > 0) {
			stack.onCraftByPlayer(this.player, this.amount);
		}
		this.amount = 0;
	}

	@Override
	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		this.onCrafted(stack);

		// Consume ingredients from the crafting grid
		this.context.run((world, pos) -> {
			if (!world.isClient()) {
				ServerWorld serverWorld = (ServerWorld) world;
				CraftingRecipeInput recipeInput = CraftingRecipeInput.create(3, 3,
					java.util.stream.IntStream.range(0, 9)
						.mapToObj(this.craftingInventory::getStack)
						.toList());

				Optional<RecipeEntry<FletchingRecipe>> optional = serverWorld.getRecipeManager()
					.getFirstMatch(FletchCraft.FLETCHING_RECIPE_TYPE, recipeInput, serverWorld);

				if (optional.isPresent()) {
					// Decrement each slot in the crafting grid by 1
					for (int i = 0; i < this.craftingInventory.size(); i++) {
						ItemStack inputStack = this.craftingInventory.getStack(i);
						if (!inputStack.isEmpty()) {
							inputStack.decrement(1);
							if (inputStack.isEmpty()) {
								this.craftingInventory.setStack(i, ItemStack.EMPTY);
							}
						}
					}
					// Trigger recipe re-check to show next result
					this.handler.onContentChanged(this.craftingInventory);
				}
			}
		});
	}
}
