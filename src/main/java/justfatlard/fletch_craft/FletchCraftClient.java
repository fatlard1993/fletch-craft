package justfatlard.fletch_craft;

import net.fabricmc.api.ClientModInitializer;

public class FletchCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FletchCraft.LOGGER.info("Fletch Craft client initialized - enhanced recipe book enabled!");
	}
}
