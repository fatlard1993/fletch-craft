package justfatlard.fletch_craft.item;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * One entity type for every knife: which stone is flying, and so how hard it hits, is read off
 * the thrown stack, which the client already has for drawing it.
 */
public class ThrowingKnifeEntity extends ThrowableItemProjectile {
    /** One knife in this many shatters on a hit; the rest drop where they struck, to be picked up. */
    private static final int SHATTER_ODDS = 5;

    public ThrowingKnifeEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    public ThrowingKnifeEntity(Level world, LivingEntity owner, ItemStack stack) {
        super(KnappedTools.THROWING_KNIFE, owner, world, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return KnappedTools.knife(Knapped.FLINT);
    }

    private float damage() {
        return this.getItem().getItem() instanceof ThrowingKnifeItem knife ? knife.damage() : 0.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        hit.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage());
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(this.level() instanceof ServerLevel level)) return;

        if (hit.getType() == HitResult.Type.ENTITY && this.random.nextInt(SHATTER_ODDS) == 0) {
            shatter(level);
        } else if (!(this.getOwner() instanceof Player player && player.hasInfiniteMaterials())) {
            this.spawnAtLocation(level, this.getItem().copyWithCount(1));
        }
        this.discard();
    }

    private void shatter(ServerLevel level) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem()),
            this.getX(), this.getY(), this.getZ(), 8, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
    }
}
