package justfatlard.fletch_craft.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ThrowingKnifeItem extends Item {
    private static final int COOLDOWN_TICKS = 8;
    private static final float VELOCITY = 1.8F;
    private static final float INACCURACY = 0.5F;

    private final float damage;

    public ThrowingKnifeItem(float damage, Properties settings) {
        super(settings);
        this.damage = damage;
    }

    public float damage() { return damage; }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
            SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F,
            1.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide()) {
            ThrowingKnifeEntity knife = new ThrowingKnifeEntity(world, user, stack);
            knife.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, VELOCITY, INACCURACY);
            world.addFreshEntity(knife);
        }

        user.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        user.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, user);

        return InteractionResult.SUCCESS;
    }
}
