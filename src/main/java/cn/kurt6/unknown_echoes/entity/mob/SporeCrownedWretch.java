package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.ai.SporeVolleyGoal;
import cn.kurt6.unknown_echoes.entity.projectile.SporePellet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class SporeCrownedWretch extends HumanoidRaider {
    public SporeCrownedWretch(EntityType<? extends Monster> type, Level level) {
        super(type, level, "spore_crowned_wretch", 14, 46);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.SPORE_CROWNED_WRETCH_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.SPORE_CROWNED_WRETCH_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.SPORE_CROWNED_WRETCH_DEATH.get(); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SporeVolleyGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean validSporeTarget(LivingEntity target) {
        return !this.level().isClientSide && this.isAlive() && !this.isNoAi()
                && target != null && target.isAlive() && target.level() == this.level()
                && !this.isAlliedTo(target) && this.canAttack(target) && this.distanceToSqr(target) <= 400.0D
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    public void releaseSpore(LivingEntity target) {
        if (target != this.getTarget() || !this.validSporeTarget(target)
                || this.distanceToSqr(target) > 100.0D || !this.hasLineOfSight(target)) {
            return;
        }
        var pellet = new SporePellet(this.level(), this);
        pellet.setPos(this.getX(), this.getY() + 1.1D, this.getZ());
        pellet.shoot(target.getX() - pellet.getX(), target.getY(0.5D) - pellet.getY(),
                target.getZ() - pellet.getZ(), 0.8F, 2.0F);
        this.level().addFreshEntity(pellet);
    }
}
