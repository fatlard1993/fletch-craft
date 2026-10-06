package justfatlard.fletch_craft.gametest;

import justfatlard.pandorical.gametest.Pictures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The pictures for the readme and the mod page: the fletching table as a bench: three columns of flint, stick and feather, and twenty-four arrows out.
 */
public final class Showcase implements FabricClientGameTest {
	private static final long SEED = 20261005L;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = Pictures.world(context, SEED)) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			Pictures.stage(context, server, Pictures.MORNING);

			BlockPos at = server.computeOnServer(s -> {
				BlockPos spawn = connection.getServerPlayer().blockPosition();
				return Pictures.dryGround(s.overworld(), spawn.getX(), spawn.getZ(), 16);
			});
			if (at == null) throw new AssertionError("no dry ground near spawn");
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				level.setBlockAndUpdate(at, Blocks.FLETCHING_TABLE.defaultBlockState());
			});
			Pictures.carry(server, connection,
				new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_AXE), new ItemStack(Items.TORCH, 48),
				new ItemStack(Items.BREAD, 12), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.OAK_PLANKS, 30), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.STONE_SWORD));
			Pictures.open(context, server, connection, at);
			server.runOnServer(s -> {
				var menu = connection.getServerPlayer().containerMenu;
				for (int column = 0; column < 3; column++) {
					menu.getSlot(1 + column).set(new ItemStack(Items.FLINT));
					menu.getSlot(4 + column).set(new ItemStack(Items.STICK));
					menu.getSlot(7 + column).set(new ItemStack(Items.FEATHER));
				}
				// The bench works out what its grid makes after a click, which filling it from here is
				// not: so it is asked the way a click asks it.
				try {
					var after = justfatlard.fletch_craft.FletchCraft.class.getDeclaredMethod("afterSlotClick", net.minecraft.server.level.ServerPlayer.class);
					after.setAccessible(true);
					after.invoke(null, connection.getServerPlayer());
				} catch (ReflectiveOperationException e) {
					throw new AssertionError("could not ask the bench what the grid makes", e);
				}
				menu.broadcastChanges();
			});
			context.waitTicks(10);
			Pictures.shootScreen(context, connection, "arrow-bench");
		}
	}
}
