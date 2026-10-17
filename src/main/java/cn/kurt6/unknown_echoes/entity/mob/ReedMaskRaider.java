package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class ReedMaskRaider extends HumanoidRaider {
    public ReedMaskRaider(EntityType<? extends Monster> type, Level level) {
        super(type, level, "reed_mask_raider", 7, 15);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.REED_MASK_RAIDER_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.REED_MASK_RAIDER_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.REED_MASK_RAIDER_DEATH.get(); }
}
