package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class BellNeedleWasp extends AerialHunter {
    public BellNeedleWasp(EntityType<? extends Monster> type, Level level) {
        super(type, level, "bell_needle_wasp");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BELL_NEEDLE_WASP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BELL_NEEDLE_WASP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BELL_NEEDLE_WASP_DEATH.get();
    }

    @Override
    public void signalSwoop() {
        super.signalSwoop();
        this.playSound(ModSounds.BELL_NEEDLE_WASP_SWOOP.get(), 0.8F, 1.0F);
    }
}
