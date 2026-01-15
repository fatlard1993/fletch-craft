package justfatlard.fletch_craft.recipe;

import justfatlard.fletch_craft.FletchCraft;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;

public class FletchingRecipe implements Recipe<CraftingRecipeInput> {
	public static final RecipeBookCategory FLETCHING_CATEGORY = new RecipeBookCategory();

	private final List<Ingredient> ingredients;
	private final ItemStack result;
	private final String group;
	private final int width;
	private final int height;

	public FletchingRecipe(String group, int width, int height, List<Ingredient> ingredients, ItemStack result) {
		this.group = group;
		this.width = width;
		this.height = height;
		this.ingredients = ingredients;
		this.result = result;
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() < this.width || input.getHeight() < this.height) {
			return false;
		}
		return matchesShifted(input);
	}

	private boolean matchesShifted(CraftingRecipeInput input) {
		for (int offsetX = 0; offsetX <= input.getWidth() - this.width; offsetX++) {
			for (int offsetY = 0; offsetY <= input.getHeight() - this.height; offsetY++) {
				if (matchesExact(input, offsetX, offsetY)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean matchesExact(CraftingRecipeInput input, int offsetX, int offsetY) {
		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				int recipeX = x - offsetX;
				int recipeY = y - offsetY;

				ItemStack stackInSlot = input.getStackInSlot(x + y * input.getWidth());

				if (recipeX >= 0 && recipeY >= 0 && recipeX < this.width && recipeY < this.height) {
					int index = recipeX + recipeY * this.width;
					if (index < this.ingredients.size()) {
						Ingredient ingredient = this.ingredients.get(index);
						if (!ingredient.test(stackInSlot)) {
							return false;
						}
					} else if (!stackInSlot.isEmpty()) {
						return false;
					}
				} else if (!stackInSlot.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
		return this.result.copy();
	}

	@Override
	public RecipeSerializer<FletchingRecipe> getSerializer() {
		return FletchingRecipeSerializer.INSTANCE;
	}

	@Override
	public RecipeType<FletchingRecipe> getType() {
		return FletchCraft.FLETCHING_RECIPE_TYPE;
	}

	@Override
	public IngredientPlacement getIngredientPlacement() {
		// Create ingredient placement for recipe book display
		if (this.ingredients.isEmpty()) {
			return IngredientPlacement.NONE;
		}
		return IngredientPlacement.forMultipleSlots(
			this.ingredients.stream()
				.map(java.util.Optional::of)
				.toList()
		);
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return FLETCHING_CATEGORY;
	}

	@Override
	public String getGroup() {
		return this.group;
	}

	public List<Ingredient> getIngredients() {
		return this.ingredients;
	}

	public ItemStack getResult() {
		return this.result;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return false;
	}
}
