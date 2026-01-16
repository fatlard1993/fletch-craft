package justfatlard.fletch_craft.mixin;

import justfatlard.fletch_craft.client.FletchingRecipeBookWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
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

	@Unique
	private boolean wasMouseDown = false;

	public CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void fletchCraft$onInit(CraftingScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
		// Check if this is a fletching table by title
		String titleString = title.getString();
		this.isFletchingTable = titleString.toLowerCase().contains("fletching");
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void fletchCraft$onDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		// Initialize the widget on first render if needed
		if (this.isFletchingTable && this.fletchingRecipeBook == null) {
			this.fletchingRecipeBook = new FletchingRecipeBookWidget();
			this.fletchingRecipeBook.initialize(this.width, this.height, this.client, this.handler);
		}

		if (this.isFletchingTable && this.fletchingRecipeBook != null) {
			// Check for mouse click (poll-based approach)
			long windowHandle = this.client.getWindow().getHandle();
			boolean isMouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

			if (isMouseDown && !wasMouseDown) {
				// Mouse just clicked
				this.fletchingRecipeBook.mouseClicked(mouseX, mouseY, 0, this.handler, this.client);
			}
			wasMouseDown = isMouseDown;

			this.fletchingRecipeBook.render(context, mouseX, mouseY, delta, this.x, this.y);
		}
	}
}
