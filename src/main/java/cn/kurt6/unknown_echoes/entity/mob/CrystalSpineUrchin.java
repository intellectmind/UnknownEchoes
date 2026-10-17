package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.ai.AerialPulseGoal;
import cn.kurt6.unknown_echoes.entity.ai.AerialPulseStrike;
import cn.kurt6.unknown_echoes.entity.ai.AerialWanderGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class CrystalSpineUrchin extends AerialHunter {
    public CrystalSpineUrchin(EntityType<? extends Monster> type, Level level) {
        super(type, level, "crystal_spine_urchin");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.19D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AerialPulseGoal(this));
        this.goalSelector.addGoal(4, new AerialWanderGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void warnPulse(Vec3 center) {
        AerialPulseStrike.particles(this, center, false);
    }

    public void firePulse(LivingEntity target, Vec3 center) {
        AerialPulseStrike.release(this, target, center);
        this.playSound(ModSounds.CRYSTAL_SPINE_URCHIN_PULSE.get(), 0.75F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRYSTAL_SPINE_URCHIN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRYSTAL_SPINE_URCHIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRYSTAL_SPINE_URCHIN_DEATH.get();
    }

    @Override
    public void signalSwoop() {
        super.signalSwoop();
        this.playSound(ModSounds.CRYSTAL_SPINE_URCHIN_CHARGE.get(), 0.8F, 1.0F);
    }
}
