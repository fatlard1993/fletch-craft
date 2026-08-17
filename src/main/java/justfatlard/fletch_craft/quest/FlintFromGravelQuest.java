package justfatlard.fletch_craft.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.quest.VillagerQuest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The one thing the fletching table does that nothing else can.
 *
 * <p>Vanilla has no recipe for flint. You break gravel and hope, and most people
 * have made peace with that. The fletching table turns four gravel into three
 * flint every single time, and there is nothing in the game that would ever tell
 * you so: the table has sat in fletchers' houses for years doing nothing at all,
 * so nobody thinks to right-click it.
 *
 * <p>Which makes the fletcher the right person to say it, and makes saying it
 * the whole quest. The flint is only the receipt.
 */
public class FlintFromGravelQuest extends VillagerQuest {
	private static final int NEEDED = 12;

	public FlintFromGravelQuest(String requesterName, UUID villagerUuid) {
		super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 6);
	}

	@Override
	public String getDescription() {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		String[] lines = {
			this.requesterName + ": \"I need flint and I am too old to go smashing gravel and praying. "
				+ "Use my table - four gravel in, three flint out, every time. It has always done that. Nobody ever asks.\"",
			this.requesterName + ": \"You have seen the table in my house? Everyone thinks it is furniture. "
				+ "Put four gravel on it. It gives back three flint. Bring me twelve and I will say no more about it.\"",
			this.requesterName + ": \"There is a trick to flint that my trade keeps badly. The table does it. "
				+ "Four gravel, three flint, no praying involved.\""
		};
		return lines[rng.nextInt(lines.length)];
	}

	@Override
	public String getObjective() {
		return "bring " + this.requesterName + " " + NEEDED
			+ " flint - a fletching table turns 4 gravel into 3 flint, which is the only reliable way there is";
	}

	@Override
	public Item getSubmissionItem() {
		return Items.FLINT;
	}

	@Override
	public int getSubmissionAmount() {
		return NEEDED;
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		int found = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(Items.FLINT)) found += stack.getCount();
		}
		return found >= NEEDED;
	}

	@Override
	public void onComplete(ServerPlayer player) {
		int remaining = NEEDED;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (remaining <= 0) break;
			if (!stack.is(Items.FLINT)) continue;

			int taken = Math.min(remaining, stack.getCount());
			stack.shrink(taken);
			remaining -= taken;
		}
	}
}
