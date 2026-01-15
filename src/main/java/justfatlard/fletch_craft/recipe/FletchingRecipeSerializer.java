package justfatlard.fletch_craft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;

import java.util.List;

public class FletchingRecipeSerializer implements RecipeSerializer<FletchingRecipe> {
	public static final FletchingRecipeSerializer INSTANCE = new FletchingRecipeSerializer();

	private static final MapCodec<FletchingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.STRING.optionalFieldOf("group", "").forGetter(FletchingRecipe::getGroup),
			Codec.INT.fieldOf("width").forGetter(FletchingRecipe::getWidth),
			Codec.INT.fieldOf("height").forGetter(FletchingRecipe::getHeight),
			Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(FletchingRecipe::getIngredients),
			ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(FletchingRecipe::getResult)
		).apply(instance, FletchingRecipe::new)
	);

	private static final PacketCodec<RegistryByteBuf, FletchingRecipe> PACKET_CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, FletchingRecipe::getGroup,
		PacketCodecs.VAR_INT, FletchingRecipe::getWidth,
		PacketCodecs.VAR_INT, FletchingRecipe::getHeight,
		Ingredient.PACKET_CODEC.collect(PacketCodecs.toList()), FletchingRecipe::getIngredients,
		ItemStack.PACKET_CODEC, FletchingRecipe::getResult,
		FletchingRecipe::new
	);

	@Override
	public MapCodec<FletchingRecipe> codec() {
		return CODEC;
	}

	@Override
	public PacketCodec<RegistryByteBuf, FletchingRecipe> packetCodec() {
		return PACKET_CODEC;
	}
}
