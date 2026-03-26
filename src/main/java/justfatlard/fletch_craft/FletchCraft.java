package justfatlard.fletch_craft;

import justfatlard.fletch_craft.network.FletchingRecipeSyncPayload;
import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.fletch_craft.recipe.FletchingRecipeSerializer;
import justfatlard.fletch_craft.screen.FletchingGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FletchCraft implements ModInitializer {
	public static final String MOD_ID = "fletch_craft";
	public static final String TITLE_KEY = "container.fletch_craft.fletching";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

	@Override
	public void onInitialize() {
		Registry.register(
			Registries.RECIPE_SERIALIZER,
			Identifier.of(MOD_ID, "fletching"),
			FletchingRecipeSerializer.INSTANCE
		);

		PayloadTypeRegistry.playS2C().register(
			FletchingRecipeSyncPayload.ID,
			FletchingRecipeSyncPayload.CODEC
		);

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player.shouldCancelInteraction() && !player.getStackInHand(hand).isEmpty()) {
				return ActionResult.PASS;
			}

			if (world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.FLETCHING_TABLE)) {
				if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
					FletchingGui gui = new FletchingGui(serverPlayer, (ServerWorld) world);
					gui.open();
					sendRecipeSync(serverPlayer, (ServerWorld) world);
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		LOGGER.info("Fletch Craft loaded - Fletching table is now functional!");
	}

	@SuppressWarnings("unchecked")
	static void sendRecipeSync(ServerPlayerEntity player, ServerWorld world) {
		List<RecipeEntry<FletchingRecipe>> recipes = world.getRecipeManager().values().stream()
			.filter(entry -> entry.value().getType() == FLETCHING_RECIPE_TYPE)
			.map(entry -> (RecipeEntry<FletchingRecipe>) (RecipeEntry<? extends Recipe<?>>) entry)
			.toList();

		if (recipes.isEmpty()) {
			LOGGER.warn("No fletching recipes found - check datapack loading");
		}

		List<FletchingRecipeSyncPayload.RecipeData> recipeDataList = new ArrayList<>();
		for (RecipeEntry<FletchingRecipe> entry : recipes) {
			FletchingRecipe recipe = entry.value();
			recipeDataList.add(new FletchingRecipeSyncPayload.RecipeData(
				recipe.getResult().copy(),
				recipe.getIngredients(),
				recipe.getWidth(),
				recipe.getHeight()
			));
		}

		ServerPlayNetworking.send(player, new FletchingRecipeSyncPayload(recipeDataList));
	}
}
