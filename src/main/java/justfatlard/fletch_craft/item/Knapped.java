package justfatlard.fletch_craft.item;

import justfatlard.fletch_craft.FletchCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * A stone that takes an edge, and what an edge of it is worth.
 *
 * <p>Flint sits beside stone and obsidian beside iron, each a little faster and a little more
 * brittle than the tier it borrows its swing from: an edge knapped sharp wears out sooner.
 * Obsidian also takes enchantments as well as gold does.
 */
public enum Knapped {
    FLINT("flint",
        new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 96, 5.0F, 1.0F, 10, repairTag("flint")),
        7.0F, -3.2F, -1.0F, -2.0F, 4.0F),
    OBSIDIAN("obsidian",
        new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 200, 7.0F, 2.0F, 22, repairTag("obsidian")),
        6.0F, -3.1F, -2.0F, -1.0F, 6.0F);

    public static final float SHOVEL_DAMAGE = 1.5F;
    public static final float SHOVEL_SPEED = -3.0F;

    public final String name;
    public final ToolMaterial material;
    public final float axeDamage;
    public final float axeSpeed;
    public final float hoeDamage;
    public final float hoeSpeed;
    public final float knifeDamage;

    Knapped(String name, ToolMaterial material, float axeDamage, float axeSpeed,
            float hoeDamage, float hoeSpeed, float knifeDamage) {
        this.name = name;
        this.material = material;
        this.axeDamage = axeDamage;
        this.axeSpeed = axeSpeed;
        this.hoeDamage = hoeDamage;
        this.hoeSpeed = hoeSpeed;
        this.knifeDamage = knifeDamage;
    }

    private static TagKey<Item> repairTag(String stone) {
        return TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(FletchCraft.MOD_ID, stone + "_tool_materials"));
    }
}
