package justfatlard.fletch_craft;

import justfatlard.fletch_craft.registry.ModScreenHandlers;
import justfatlard.fletch_craft.screen.FletchingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class FletchCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.FLETCHING_SCREEN_HANDLER, FletchingScreen::new);
	}
}
