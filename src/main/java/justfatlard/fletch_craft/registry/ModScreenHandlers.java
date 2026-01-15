package justfatlard.fletch_craft.registry;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.screen.FletchingScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ModScreenHandlers {
	public static final ScreenHandlerType<FletchingScreenHandler> FLETCHING_SCREEN_HANDLER =
		Registry.register(
			Registries.SCREEN_HANDLER,
			FletchCraft.id("fletching"),
			new ScreenHandlerType<>(FletchingScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
		);

	public static void register() {
		FletchCraft.LOGGER.info("Registering screen handlers");
	}
}
