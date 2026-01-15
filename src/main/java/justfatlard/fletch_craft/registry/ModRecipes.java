package justfatlard.fletch_craft.registry;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.recipe.FletchingRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModRecipes {
	public static void register() {
		FletchCraft.LOGGER.info("Registering fletching recipe type and serializer");

		// Register the recipe serializer
		Registry.register(
			Registries.RECIPE_SERIALIZER,
			FletchCraft.id("fletching"),
			FletchingRecipeSerializer.INSTANCE
		);
	}
}
