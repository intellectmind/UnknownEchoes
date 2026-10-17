package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class MossplateHound extends HumanoidRaider {
    public MossplateHound(EntityType<? extends Monster> type, Level level) {
        super(type, level, "mossplate_hound", 8, 18);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.MOSSPLATE_HOUND_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.MOSSPLATE_HOUND_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.MOSSPLATE_HOUND_DEATH.get(); }
}
