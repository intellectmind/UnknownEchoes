package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.ai.AerialBeamGoal;
import cn.kurt6.unknown_echoes.entity.ai.AerialBeamStrike;
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

public final class ShardwingSkate extends AerialHunter {
    public ShardwingSkate(EntityType<? extends Monster> type, Level level) {
        super(type, level, "shardwing_skate");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AerialBeamGoal(this));
        this.goalSelector.addGoal(4, new AerialWanderGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void warnBeam(Vec3 start, Vec3 end) {
        AerialBeamStrike.particles(this, start, end, false);
    }

    public void fireBeam(LivingEntity target, Vec3 start, Vec3 end) {
        AerialBeamStrike.release(this, target, start, end);
        this.playSound(ModSounds.SHARDWING_SKATE_BEAM.get(), 0.75F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SHARDWING_SKATE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SHARDWING_SKATE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHARDWING_SKATE_DEATH.get();
    }

    @Override
    public void signalSwoop() {
        super.signalSwoop();
        this.playSound(ModSounds.SHARDWING_SKATE_CHARGE.get(), 0.8F, 1.0F);
    }
}
