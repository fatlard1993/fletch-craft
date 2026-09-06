package justfatlard.fletch_craft.mixin;

import justfatlard.fletch_craft.recipe.FletchingRecipe;
import justfatlard.pandorical.screen.PandoricalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fletching recipe asked for at a workbench is refused, with a word, rather than crashing.
 *
 * <p>A recipe book that lists every recipe it knows will offer these at a crafting table, and
 * vanilla's placement answers by casting whatever it was handed to a crafting recipe, which
 * this is not: the cast threw, the packet died half-handled, and the player saw nothing happen
 * and items out of place. The bench that can fill these is a Pandorical one, and it is filled
 * by its own route; anywhere else, the honest answer is where to go.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlaceInVanillaMenuMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
	private void fletchCraft$onlyAtTheTable(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
		if (player.containerMenu instanceof PandoricalMenu) return;
		MinecraftServer server = player.level().getServer();
		if (server == null) return;
		RecipeManager.ServerDisplayInfo info = server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
		if (info == null || !(info.parent().value() instanceof FletchingRecipe)) return;
		player.sendSystemMessage(Component.literal("That is made at a fletching table.").withStyle(ChatFormatting.RED), true);
		ci.cancel();
	}
}
