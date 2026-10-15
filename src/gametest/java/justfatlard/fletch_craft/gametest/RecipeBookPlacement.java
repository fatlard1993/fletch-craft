package justfatlard.fletch_craft.gametest;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.pandorical.gametest.Pictures;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;

/**
 * Picking a recipe in the recipe book: one set without shift, as many as the pack affords with it,
 * capped at a full stack per cell, a grid already at its most left alone, and a pack short of one
 * whole set still laying out what it has.
 */
public final class RecipeBookPlacement implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Smoke.full()) return;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();

			BlockPos at = server.computeOnServer(s -> {
				BlockPos spawn = connection.getServerPlayer().blockPosition();
				return Pictures.dryGround(s.overworld(), spawn.getX(), spawn.getZ(), 16);
			});
			if (at == null) throw new AssertionError("no dry ground near spawn");
			server.runOnServer(s -> s.overworld().setBlockAndUpdate(at, Blocks.FLETCHING_TABLE.defaultBlockState()));
			Pictures.open(context, server, connection, at);

			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				AbstractContainerMenu menu = player.containerMenu;
				RecipeHolder<?> arrow = recipe(s, "arrow");

				// Ten of each: three whole sets of three, one of each left over
				carry(player, 10, 10, 10);
				place(player, arrow, true);
				expectGrid(menu, "shift with ten of each", 3);
				expectPack(player, "shift with ten of each", 1, 1, 1);

				// Already at its most: shift again changes nothing
				place(player, arrow, true);
				expectGrid(menu, "shift on a full grid", 3);
				expectPack(player, "shift on a full grid", 1, 1, 1);

				// More in the pack: the grid's own nine count toward the next shift
				give(player, 20, 20, 20);
				place(player, arrow, true);
				expectGrid(menu, "shift after topping up", 10);
				expectPack(player, "shift after topping up", 0, 0, 0);

				// No shift: everything back, one set out
				place(player, arrow, false);
				expectGrid(menu, "a click without shift", 1);
				expectPack(player, "a click without shift", 27, 27, 27);

				// More than a stack's worth: each cell stops at sixty-four
				clear(player, menu);
				carry(player, 200, 200, 200);
				place(player, arrow, true);
				expectGrid(menu, "shift with two hundred of each", 64);
				expectPack(player, "shift with two hundred of each", 8, 8, 8);

				// Short of one whole set: shift lays out what there is, as a plain click does
				clear(player, menu);
				carry(player, 2, 3, 3);
				place(player, arrow, true);
				int flint = 0;
				for (int slot = 1; slot <= 9; slot++) {
					ItemStack stack = menu.getSlot(slot).getItem();
					if (stack.is(Items.FLINT)) flint += stack.getCount();
					else if (!stack.isEmpty() && stack.getCount() != 1) {
						throw new AssertionError("short of a set: expected singles, got " + stack + " in slot " + slot);
					}
				}
				if (flint != 2) throw new AssertionError("short of a set: expected 2 flint laid out, got " + flint);
				if (!menu.getSlot(0).getItem().isEmpty()) {
					throw new AssertionError("short of a set: an incomplete grid shows " + menu.getSlot(0).getItem());
				}
			});
		}
	}

	private static RecipeHolder<?> recipe(MinecraftServer server, String name) {
		ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key =
			ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(FletchCraft.MOD_ID, name));
		return server.getRecipeManager().byKey(key).orElseThrow(() -> new AssertionError("no recipe " + key));
	}

	/** Asked the way the recipe book's packet asks it. */
	private static void place(ServerPlayer player, RecipeHolder<?> recipe, boolean shift) {
		try {
			Method place = FletchCraft.class.getDeclaredMethod("placeRecipe", ServerPlayer.class, RecipeHolder.class, boolean.class);
			place.setAccessible(true);
			place.invoke(null, player, recipe, shift);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("could not ask the bench to place a recipe", e);
		}
	}

	/** An empty pack, then flint, sticks and feathers in it, split into stacks no larger than allowed. */
	private static void carry(ServerPlayer player, int flint, int sticks, int feathers) {
		player.getInventory().clearContent();
		give(player, flint, sticks, feathers);
	}

	private static void give(ServerPlayer player, int flint, int sticks, int feathers) {
		add(player, Items.FLINT, flint);
		add(player, Items.STICK, sticks);
		add(player, Items.FEATHER, feathers);
	}

	private static void add(ServerPlayer player, Item item, int count) {
		Inventory pack = player.getInventory();
		while (count > 0) {
			// Fifty at a time, so the pack holds part stacks the way a real one does
			int n = Math.min(count, 50);
			int slot = pack.getFreeSlot();
			if (slot < 0) throw new AssertionError("pack full");
			pack.setItem(slot, new ItemStack(item, n));
			count -= n;
		}
	}

	private static void clear(ServerPlayer player, AbstractContainerMenu menu) {
		for (int slot = 0; slot <= 9; slot++) menu.getSlot(slot).set(ItemStack.EMPTY);
		player.getInventory().clearContent();
	}

	/** The arrow recipe's three rows, each cell holding {@code count}, and the result shown. */
	private static void expectGrid(AbstractContainerMenu menu, String when, int count) {
		Item[] rows = { Items.FLINT, Items.STICK, Items.FEATHER };
		for (int slot = 1; slot <= 9; slot++) {
			ItemStack stack = menu.getSlot(slot).getItem();
			Item want = rows[(slot - 1) / 3];
			if (!stack.is(want) || stack.getCount() != count) {
				throw new AssertionError(when + ": expected " + count + " " + want + " in slot " + slot + ", got " + stack);
			}
		}
		if (!menu.getSlot(0).getItem().is(Items.ARROW)) {
			throw new AssertionError(when + ": expected arrows in the result slot, got " + menu.getSlot(0).getItem());
		}
	}

	private static void expectPack(ServerPlayer player, String when, int flint, int sticks, int feathers) {
		Inventory pack = player.getInventory();
		int[] got = { pack.countItem(Items.FLINT), pack.countItem(Items.STICK), pack.countItem(Items.FEATHER) };
		if (got[0] != flint || got[1] != sticks || got[2] != feathers) {
			throw new AssertionError(when + ": expected " + flint + "/" + sticks + "/" + feathers
				+ " flint/sticks/feathers left in the pack, got " + got[0] + "/" + got[1] + "/" + got[2]);
		}
	}
}
