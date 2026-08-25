package justfatlard.fletch_craft.recipe;

import justfatlard.fletch_craft.FletchCraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class FletchingRecipe implements Recipe<CraftingInput> {
    private final List<Ingredient> ingredients;
    private final ItemStackTemplate resultTemplate;
    private final String group;
    private final int width;
    private final int height;

    public FletchingRecipe(String group, int width, int height, List<Ingredient> ingredients, ItemStackTemplate resultTemplate) {
        this.group = group;
        this.width = width;
        this.height = height;
        this.ingredients = Collections.unmodifiableList(ingredients);
        this.resultTemplate = resultTemplate;
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        if (input.width() < this.width || input.height() < this.height) return false;
        for (int ox = 0; ox <= input.width() - this.width; ox++) {
            for (int oy = 0; oy <= input.height() - this.height; oy++) {
                if (matchesExact(input, ox, oy)) return true;
            }
        }
        return false;
    }

    private boolean matchesExact(CraftingInput input, int ox, int oy) {
        for (int x = 0; x < input.width(); x++) {
            for (int y = 0; y < input.height(); y++) {
                int rx = x - ox, ry = y - oy;
                ItemStack stack = input.getItem(x + y * input.width());
                if (rx >= 0 && ry >= 0 && rx < width && ry < height) {
                    int idx = rx + ry * width;
                    if (idx < ingredients.size()) {
                        if (!ingredients.get(idx).test(stack)) return false;
                    } else if (!stack.isEmpty()) return false;
                } else if (!stack.isEmpty()) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.resultTemplate.create();
    }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer() {
        return FletchCraft.FLETCHING_SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<CraftingInput>> getType() {
        return FletchCraft.FLETCHING_RECIPE_TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (ingredients.isEmpty()) return PlacementInfo.NOT_PLACEABLE;
        return PlacementInfo.create(ingredients);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return FletchCraft.FLETCHING_CATEGORY;
    }

    @Override
    /**
     * The recipe as the client can see it.
     *
     * <p>This returned nothing, which is why the table grew a recipe browser of its own: with no
     * display, a fletching recipe does not exist as far as any client is concerned, and no book -
     * vanilla's or otherwise - could ever have listed one. Emitting a shaped display costs
     * nothing and is honest, because that is exactly what these are: a width, a height and an
     * ordered list of ingredients.
     *
     * <p>The client is never sent the recipe itself, only this. It needs no serializer, no recipe
     * type and no jar from this mod, which is the whole reason displays exist as a separate
     * thing. What keeps these out of an ordinary workbench's book is
     * {@link #recipeBookCategory()}, not their absence.
     */
    public List<RecipeDisplay> display() {
        List<SlotDisplay> slots = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            slots.add(ingredient.display());
        }

        return List.of(new ShapedCraftingRecipeDisplay(
            width, height, slots,
            new SlotDisplay.ItemStackSlotDisplay(resultTemplate),
            new SlotDisplay.ItemSlotDisplay(Items.FLETCHING_TABLE)));
    }

    @Override
    public String group() { return group; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public ItemStack getResult() { return resultTemplate.create(); }
    public ItemStackTemplate getResultTemplate() { return resultTemplate; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
