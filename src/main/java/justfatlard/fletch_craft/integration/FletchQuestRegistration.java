package justfatlard.fletch_craft.integration;

import java.util.Random;
import justfatlard.fletch_craft.quest.FlintFromGravelQuest;
import justfatlard.village_quests.api.QuestRegistry;
import justfatlard.village_quests.quest.VillagerQuest;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Offers the flint lesson, from the only villager who would know it.
 *
 * <p>Names village-quests types directly, so it must only be loaded behind the
 * isModLoaded guard in the entry point.
 */
public final class FletchQuestRegistration {
	private FletchQuestRegistration() {}

	private static final float OFFER_CHANCE = 0.14F;

	public static void register() {
		QuestRegistry.registerProfessionQuest("fletcher", FletchQuestRegistration::offer);
	}

	private static VillagerQuest offer(Villager villager, String villagerName, int reputation, Random random) {
		// Early on purpose: this is a trade secret worth having before the part of
		// the game where flint stops mattering.
		if (reputation < 5) return null;
		if (random.nextFloat() > OFFER_CHANCE) return null;

		return new FlintFromGravelQuest(villagerName, villager.getUUID());
	}
}
