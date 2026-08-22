package justfatlard.fletch_craft.integration;

import java.util.List;
import java.util.function.Predicate;
import justfatlard.village_quests.api.LessonApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A fletcher teaching that the table in their house is a workshop.
 *
 * <p>Registered with Village Quests when that mod is present. The fletching
 * table has sat in fletchers' houses for years doing nothing, so nobody
 * right-clicks one; every lesson here is a thing it does that the player has
 * been doing the long way or not at all.
 *
 * <p>Every ratio quoted was read off this mod's recipe JSON and vanilla's, not
 * from either README.
 *
 * <p>This class must only be touched behind a mod-loaded check. It refers to
 * Village Quests types directly, so loading it without that mod present throws.
 */
public final class FletchQuestRegistration {
	private FletchQuestRegistration() {}

	private static Predicate<ItemStack> atLeast(Item item, int count) {
		return stack -> stack.is(item) && stack.getCount() >= count;
	}

	/**
	 * Any stripped wood, by id rather than by listing them. Every wood type and
	 * the bamboo block all strip on the table, and the set grows with the game.
	 */
	private static boolean isStripped(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getPath().startsWith("stripped_");
	}

	public static void register() {
		LessonApi.register(new LessonApi.Craft(
			"fletch-craft:fletching",
			"fletcher",
			LessonApi.Policy.standard(),
			lessons(),
			new LessonApi.Openings(
				LessonApi.lines(
					"{former} is gone. They had you part-way through and I would rather not let it lapse. ",
					"You were learning off {former}, weren't you. I'd not have said anything while they were here. Since they aren't: ",
					"*pulls a stool over* {former}'s student. I know about where they got you to. "),
				LessonApi.lines(
					"Table's free when you want the next one.",
					"*nods at the table* Another whenever you like.",
					"There's one more waiting. No rush about it."),
				LessonApi.lines(
					"{former} is gone. Their table is still in there. Still nobody touching it.",
					"You'll have heard about {former}. They were showing you what the table does, weren't they."),
				LessonApi.lines(
					"*sees you glance at the table* You know what that is. Most people think it's furniture.",
					"You're carrying flint and you've no gravel on you. That says you know where flint comes from.",
					"{mentor} taught you. Course they did -- they start everyone on the gravel.")),
			new LessonApi.Hooks() {
				@Override
				public void onGraduate(ServerPlayer player, ServerLevel world, LessonApi.Teacher teacher) {
					teacher.give(new ItemStack(Items.FLETCHING_TABLE));
					teacher.says("Take a table of your own. I have watched too many people walk past mine.");
					teacher.laterInTheVillage("There is a fletching table out by the woodpile now that was not there last week, "
						+ "with a pile of gravel beside it and the gravel going down.", 0);
				}
			}));
	}

	private static List<LessonApi.Lesson> lessons() {
		return List.of(
			new LessonApi.Lesson(
				"I need flint and I am too old to smash gravel and pray. -- The table in my house. You have walked past it a hundred "
					+ "times. Right-click it. Four gravel in, three flint out, every time. Bring me twelve and I will say no more about it.",
				"bring {name} twelve flint -- four gravel makes three, on his table",
				"Four gravel, three flint. Every time. There is no other recipe for flint.",
				"*counts them* Good. Now understand what you just did, because it is the only reliable flint there is -- the game has no "
					+ "recipe for it at all. You break gravel and you hope, and one in ten gives you anything.",
				"And the table is not furniture. It is a bench with its own board and its own list of things it will make, and it has "
					+ "been sitting in every fletcher's house since before I was born with nobody laying a hand on it.",
				Items.FLINT, atLeast(Items.FLINT, 12), 6),

			new LessonApi.Lesson(
				"Next: bring me a stripped log. Any wood. And I want you to do it without an axe -- do not go buying one, do not go "
					+ "wearing one out. Put the log on the table.",
				"bring {name} a stripped log -- no axe",
				"The table strips wood. No axe, no edge worn down, no bark left over.",
				"*runs a thumb along it* There. The table does that. Put any log on it, bamboo too, and it comes back stripped -- no axe, "
					+ "nothing worn down, no waiting on a tool you would rather keep sharp.",
				"And while you have wood on the board: it makes sticks better than a bench does. A plank gives three sticks there against "
					+ "two at a crafting table. Half again, on the one thing you burn most of.",
				Items.STRIPPED_OAK_LOG, FletchQuestRegistration::isStripped, 6),

			new LessonApi.Lesson(
				"String now. Nine of it. And I do not want you out in the dark at it -- there is a shorter way and you are standing next "
					+ "to it every day.",
				"bring {name} nine string",
				"Wool is string. One block of it, on the table.",
				"*winds it round two fingers* Wool. A block of wool on the table comes apart into string. You have been going out after "
					+ "spiders in the dark for a thing the shepherd grows on the hoof and shears off twice a season.",
				"Which changes what a sheep is for. Everyone keeps them for the colour and the beds. They are a string farm that walks "
					+ "itself home at night.",
				Items.STRING, atLeast(Items.STRING, 9), 8),

			new LessonApi.Lesson(
				"Arrows. Thirty-two of them, and I want them made on the table, not at a bench. You will see the difference in the count "
					+ "before I have to explain it.",
				"bring {name} thirty-two arrows -- made on the table",
				"Three flint, three sticks, three feathers. Sixteen arrows, not twelve.",
				"*fans them out* Three of each and you get sixteen. At a bench those same three feathers get you twelve. A third more "
					+ "arrow out of the same bird.",
				"And feathers are the whole of it. Flint you now have as much of as you have gravel, sticks are wood, but every arrow in "
					+ "the world is waiting on a chicken. Anything that stretches a feather further is the only saving that matters.",
				Items.ARROW, atLeast(Items.ARROW, 32), 8),

			new LessonApi.Lesson(
				"Last one. A crossbow. Made on the table -- and before you go hunting a tripwire hook for it, do not. That is the point "
					+ "of the lesson and you will see it the moment you lay the pieces out.",
				"bring {name} a crossbow -- made on the table",
				"No tripwire hook. And a nugget of iron, not an ingot.",
				"*works the lever once* There it is. Three sticks, two string, and one nugget of iron. No tripwire hook at all -- and a "
					+ "hook is another ingot and a stick and a plank on its own, so you have saved that twice over.",
				"A bench wants a whole ingot and the hook besides. The table wants a ninth of one. -- And use the list down the side of "
					+ "the board while you are there: click a recipe and it lays the pieces out of your pack for you, hold shift and it "
					+ "lays out as many sets as you can afford. I watched a man fill that grid by hand for a year.",
				Items.CROSSBOW, stack -> stack.is(Items.CROSSBOW), 12));
	}
}
