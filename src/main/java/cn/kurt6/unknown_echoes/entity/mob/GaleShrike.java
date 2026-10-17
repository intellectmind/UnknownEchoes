package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class GaleShrike extends AerialHunter {
    public GaleShrike(EntityType<? extends Monster> type, Level level) {
        super(type, level, "gale_shrike");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.38D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GALE_SHRIKE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GALE_SHRIKE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GALE_SHRIKE_DEATH.get();
    }

    @Override
    public void signalSwoop() {
        super.signalSwoop();
        this.playSound(ModSounds.GALE_SHRIKE_SWOOP.get(), 0.8F, 1.0F);
    }
}
