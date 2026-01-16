package justfatlard.fletch_craft;

import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.fletch_craft.registry.ModRecipes;
import justfatlard.fletch_craft.screen.FletchingGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FletchCraft implements ModInitializer {
	public static final String MOD_ID = "fletch_craft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

		// Register block use callback for fletching table
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.FLETCHING_TABLE)) {
				if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
					FletchingGui gui = new FletchingGui(serverPlayer, (ServerWorld) world);
					gui.open();
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		LOGGER.info("Fletch Craft loaded - Fletching table is now functional (server-side only)!");
	}
}
