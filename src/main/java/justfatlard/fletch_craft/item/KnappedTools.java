package justfatlard.fletch_craft.item;

import justfatlard.fletch_craft.FletchCraft;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Flint and obsidian tools and throwing knives. None of them has a workbench recipe: the only
 * recipes that make them are fletching ones, so the fletching table is the only place they come
 * from.
 */
public final class KnappedTools {
    private KnappedTools() {}

    private static final int KNIFE_STACK = 16;

    private static final ResourceKey<CreativeModeTab> TOOLS_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"));
    private static final ResourceKey<CreativeModeTab> COMBAT_TAB = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "combat"));

    public static final EntityType<ThrowingKnifeEntity> THROWING_KNIFE = EntityType.Builder
        .<ThrowingKnifeEntity>of(ThrowingKnifeEntity::new, MobCategory.MISC)
        .sized(0.25F, 0.25F)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, id("throwing_knife")));

    private static final Map<Knapped, Item> KNIVES = new EnumMap<>(Knapped.class);

    public static Item knife(Knapped stone) { return KNIVES.get(stone); }

    /** {@code Item.Properties::axe} and its siblings, which differ only in name. */
    @FunctionalInterface
    private interface Shape {
        Item.Properties apply(Item.Properties properties, ToolMaterial material, float damage, float speed);
    }

    public static void register() {
        List<Item> tools = new ArrayList<>();
        List<Item> knives = new ArrayList<>();

        for (Knapped stone : Knapped.values()) {
            tools.add(tool(stone, "axe", Item.Properties::axe, stone.axeDamage, stone.axeSpeed));
            tools.add(tool(stone, "shovel", Item.Properties::shovel, Knapped.SHOVEL_DAMAGE, Knapped.SHOVEL_SPEED));
            tools.add(tool(stone, "hoe", Item.Properties::hoe, stone.hoeDamage, stone.hoeSpeed));

            String name = stone.name + "_throwing_knife";
            if (PandoricalApi.isAvailable()) {
                PandoricalApi.content().registerItem(FletchCraft.MOD_ID + ":" + name, new ItemRegistration()
                    .model(FletchCraft.MOD_ID + ":item/" + name)
                    .maxStackSize(KNIFE_STACK));
            }
            Item knife = Registry.register(BuiltInRegistries.ITEM, itemKey(name),
                new ThrowingKnifeItem(stone.knifeDamage,
                    new Item.Properties().setId(itemKey(name)).stacksTo(KNIFE_STACK)));
            KNIVES.put(stone, knife);
            knives.add(knife);
        }

        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("throwing_knife"), THROWING_KNIFE);
        PandoricalApi.registerEntityRenderer(THROWING_KNIFE, "thrown_item");
        if (PandoricalApi.isAvailable()) PandoricalApi.content().registerModAssets(FletchCraft.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(entries -> tools.forEach(entries::accept));
        CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(entries -> knives.forEach(entries::accept));
    }

    private static Item tool(Knapped stone, String kind, Shape shape, float damage, float speed) {
        String name = stone.name + "_" + kind;
        // The kind tells the client which vanilla helper to rebuild it with; without it the
        // client's copy is a plain item and digs and swings at bare-hand speed.
        if (PandoricalApi.isAvailable()) {
            PandoricalApi.content().registerItem(FletchCraft.MOD_ID + ":" + name, new ItemRegistration()
                .model(FletchCraft.MOD_ID + ":item/" + name)
                .tool(kind, stone.material, damage, speed)
                .maxStackSize(1));
        }
        return Registry.register(BuiltInRegistries.ITEM, itemKey(name),
            new Item(shape.apply(new Item.Properties().setId(itemKey(name)), stone.material, damage, speed)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FletchCraft.MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, id(name));
    }
}
