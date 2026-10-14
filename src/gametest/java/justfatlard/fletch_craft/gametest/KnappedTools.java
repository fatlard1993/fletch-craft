package justfatlard.fletch_craft.gametest;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.fletch_craft.item.Knapped;
import justfatlard.fletch_craft.item.ThrowingKnifeEntity;
import justfatlard.pandorical.gametest.Smoke;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The flint and obsidian kit: each piece comes off the bench from its shape, mirrored shapes
 * included, a shape with its empty cells filled does not, and a thrown knife hurts what it hits.
 */
public final class KnappedTools implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Smoke.full()) return;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();

			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				Item stick = Items.STICK;
				for (Knapped stone : Knapped.values()) {
					Item x = stone == Knapped.FLINT ? Items.FLINT : Items.OBSIDIAN;
					String name = stone.name;
					expect(level, name + "_axe", 3, x, x, null, x, stick, null, null, stick, null);
					expect(level, name + "_axe", 3, x, x, null, stick, x, null, stick, null, null);
					expect(level, name + "_shovel", 3, null, x, null, null, stick, null, null, stick, null);
					expect(level, name + "_hoe", 3, x, x, null, null, stick, null, null, stick, null);
					expect(level, name + "_hoe", 3, x, x, null, stick, null, null, stick, null, null);
					expect(level, name + "_throwing_knife", 3, x, null, null, stick, null, null, null, null, null);
					expect(level, null, 3, x, x, null, x, stick, null, x, stick, null);
				}
			});

			AtomicReference<Zombie> target = new AtomicReference<>();
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				var player = connection.getServerPlayer();
				Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				zombie.setNoAi(true);
				zombie.setPos(player.getX() + 3, player.getY(), player.getZ());
				level.addFreshEntity(zombie);
				target.set(zombie);

				ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player,
					new ItemStack(item("obsidian_throwing_knife")));
				knife.setPos(player.getX() + 1.5, zombie.getY() + 1.0, player.getZ());
				knife.shoot(1, 0, 0, 1.5F, 0);
				level.addFreshEntity(knife);
			});
			server.waitFor(s -> target.get().getHealth() < target.get().getMaxHealth(), 40);
		}
	}

	private static Item item(String name) {
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FletchCraft.MOD_ID, name));
	}

	/** The nine cells row by row, {@code null} for empty; {@code result} null means nothing matches. */
	private static void expect(ServerLevel level, String result, int side, Item... cells) {
		List<ItemStack> grid = new ArrayList<>();
		for (Item cell : cells) grid.add(cell == null ? ItemStack.EMPTY : new ItemStack(cell));
		CraftingInput input = CraftingInput.of(side, side, grid);
		var match = level.recipeAccess().getRecipeFor(FletchCraft.FLETCHING_RECIPE_TYPE, input, level);
		Item got = match.map(holder -> holder.value().assemble(input).getItem()).orElse(null);
		Item want = result == null ? null : item(result);
		if (got != want) throw new AssertionError("expected " + result + " from " + grid + ", got " + got);
	}
}
