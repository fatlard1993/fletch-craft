package justfatlard.fletch_craft;

import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.fletch_craft.registry.ModRecipes;
import justfatlard.fletch_craft.registry.ModScreenHandlers;
import justfatlard.fletch_craft.screen.FletchingScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FletchCraft implements ModInitializer {
	public static final String MOD_ID = "fletch_craft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Text TITLE = Text.translatable("container.fletch_craft.fletching");

	// Recipe type for fletching recipes - registered with our mod namespace
	public static final RecipeType<FletchingRecipe> FLETCHING_RECIPE_TYPE = Registry.register(
		Registries.RECIPE_TYPE,
		Identifier.of(MOD_ID, "fletching"),
		new RecipeType<FletchingRecipe>() {
			@Override
			public String toString() {
				return MOD_ID + ":fletching";
			}
		}
	);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModRecipes.register();
		ModScreenHandlers.register();

		// Register block use callback for fletching table
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.FLETCHING_TABLE)) {
				if (!world.isClient()) {
					player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
						(syncId, playerInventory, playerEntity) -> new FletchingScreenHandler(
							syncId,
							playerInventory,
							ScreenHandlerContext.create(world, hitResult.getBlockPos())
						),
						TITLE
					));
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		LOGGER.info("Fletch Craft loaded - Fletching table is now functional!");
	}
}
