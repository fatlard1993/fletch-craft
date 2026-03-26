package justfatlard.fletch_craft.network;

import justfatlard.fletch_craft.FletchCraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record FletchingRecipeSyncPayload(List<RecipeData> recipes) implements CustomPayload {
	private static final int MAX_RECIPES = 1024;
	private static final int MAX_INGREDIENTS = 9;

	public static final Id<FletchingRecipeSyncPayload> ID =
		new Id<>(Identifier.of(FletchCraft.MOD_ID, "recipe_sync"));

	public record RecipeData(ItemStack result, List<Ingredient> ingredients, int width, int height) {}

	public static final PacketCodec<RegistryByteBuf, FletchingRecipeSyncPayload> CODEC =
		new PacketCodec<>() {
			@Override
			public FletchingRecipeSyncPayload decode(RegistryByteBuf buf) {
				int count = buf.readVarInt();
				if (count < 0 || count > MAX_RECIPES) {
					throw new IllegalArgumentException("Recipe count out of bounds: " + count);
				}
				List<RecipeData> recipes = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					ItemStack result = ItemStack.PACKET_CODEC.decode(buf);
					int ingredientCount = buf.readVarInt();
					if (ingredientCount < 0 || ingredientCount > MAX_INGREDIENTS) {
						throw new IllegalArgumentException("Ingredient count out of bounds: " + ingredientCount);
					}
					List<Ingredient> ingredients = new ArrayList<>(ingredientCount);
					for (int j = 0; j < ingredientCount; j++) {
						ingredients.add(Ingredient.PACKET_CODEC.decode(buf));
					}
					int width = buf.readVarInt();
					int height = buf.readVarInt();
					recipes.add(new RecipeData(result, ingredients, width, height));
				}
				return new FletchingRecipeSyncPayload(recipes);
			}

			@Override
			public void encode(RegistryByteBuf buf, FletchingRecipeSyncPayload value) {
				buf.writeVarInt(value.recipes.size());
				for (RecipeData recipe : value.recipes) {
					ItemStack.PACKET_CODEC.encode(buf, recipe.result);
					buf.writeVarInt(recipe.ingredients.size());
					for (Ingredient ingredient : recipe.ingredients) {
						Ingredient.PACKET_CODEC.encode(buf, ingredient);
					}
					buf.writeVarInt(recipe.width);
					buf.writeVarInt(recipe.height);
				}
			}
		};

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
