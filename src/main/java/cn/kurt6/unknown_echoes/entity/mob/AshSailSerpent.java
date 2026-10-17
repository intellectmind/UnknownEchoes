package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class AshSailSerpent extends AerialHunter {
    public AshSailSerpent(EntityType<? extends Monster> type, Level level) {
        super(type, level, "ash_sail_serpent");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ASH_SAIL_SERPENT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ASH_SAIL_SERPENT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ASH_SAIL_SERPENT_DEATH.get();
    }

    @Override
    public void signalSwoop() {
        super.signalSwoop();
        this.playSound(ModSounds.ASH_SAIL_SERPENT_SWOOP.get(), 0.8F, 1.0F);
    }
}
