package justfatlard.fletch_craft;

import justfatlard.fletch_craft.network.FletchingRecipeSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

public class FletchCraftClient implements ClientModInitializer {
	private static volatile List<FletchingRecipeSyncPayload.RecipeData> syncedRecipes = List.of();

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(
			FletchingRecipeSyncPayload.ID,
			(payload, context) -> {
				List<FletchingRecipeSyncPayload.RecipeData> recipes = List.copyOf(payload.recipes());
				context.client().execute(() -> syncedRecipes = recipes);
			}
		);
	}

	public static List<FletchingRecipeSyncPayload.RecipeData> getSyncedRecipes() {
		return syncedRecipes;
	}
}
