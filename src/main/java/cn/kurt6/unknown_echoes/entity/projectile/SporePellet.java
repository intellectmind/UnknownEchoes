package cn.kurt6.unknown_echoes.entity.projectile;

import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SporePellet extends ThrowableItemProjectile {
    private int lifeTicks;

    public SporePellet(EntityType<? extends SporePellet> type, Level level) {
        super(type, level);
    }

    public SporePellet(Level level, LivingEntity owner) {
        super(ModEntities.SPORE_PELLET.get(), owner, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    private boolean validOwner() {
        return this.getOwner() instanceof Mob owner && owner.isAlive() && !owner.isNoAi()
                && owner.level() == this.level();
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (!this.validOwner() || this.lifeTicks >= 60) {
                this.discard();
                return;
            }
            this.lifeTicks++;
        }
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SporeAge", this.lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.lifeTicks = Math.clamp(compound.getInt("SporeAge"), 0, 60);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && this.validOwner()
                && target instanceof LivingEntity living && living.isAlive()
                && living.level() == this.level() && !this.getOwner().isAlliedTo(living)
                && ((Mob) this.getOwner()).canAttack(living)
                && (!(living instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && this.isAlive() && this.canHitEntity(result.getEntity())) {
            result.getEntity().hurt(this.damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), 3.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.MYCELIUM, this.getX(), this.getY(), this.getZ(), 8, .12, .12, .12, .01);
            this.discard();
        }
    }
}
