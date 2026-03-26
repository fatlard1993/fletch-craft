package justfatlard.fletch_craft.mixin;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.client.FletchingRecipeBookWidget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> {

	@Unique
	private FletchingRecipeBookWidget fletchingRecipeBook;

	@Unique
	private boolean isFletchingTable = false;

	public CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void fletchCraft$onInit(CraftingScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
		this.isFletchingTable = title.getContent() instanceof TranslatableTextContent translatable
			&& FletchCraft.TITLE_KEY.equals(translatable.getKey());

		if (this.isFletchingTable) {
			ScreenMouseEvents.allowMouseClick((Screen)(Object) this).register((screen, click) -> {
				if (this.fletchingRecipeBook != null && click.button() == 0) {
					if (this.fletchingRecipeBook.mouseClicked(click.x(), click.y(), click.button(), this.handler, this.client)) {
						return false;
					}
				}
				return true;
			});
		}
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void fletchCraft$onDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (!this.isFletchingTable) return;

		if (this.fletchingRecipeBook == null) {
			this.fletchingRecipeBook = new FletchingRecipeBookWidget();
			this.fletchingRecipeBook.initialize(this.client, this.handler);
		}

		this.fletchingRecipeBook.render(context, mouseX, mouseY, delta, this.x, this.y);
	}
}
