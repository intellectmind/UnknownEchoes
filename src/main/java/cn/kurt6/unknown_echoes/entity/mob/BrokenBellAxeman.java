package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class BrokenBellAxeman extends HumanoidRaider {
    public BrokenBellAxeman(EntityType<? extends Monster> type, Level level) {
        super(type, level, "broken_bell_axeman", 14, 24);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 38.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.BROKEN_BELL_AXEMAN_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.BROKEN_BELL_AXEMAN_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.BROKEN_BELL_AXEMAN_DEATH.get(); }
}
