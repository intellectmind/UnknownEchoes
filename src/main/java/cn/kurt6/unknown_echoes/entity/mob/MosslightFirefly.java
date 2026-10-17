package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MosslightFirefly extends AerialAnimal {
    public MosslightFirefly(EntityType<? extends Animal> type, Level level) {
        super(type, level, "mosslight_firefly");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.21D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SUGAR) || stack.is(Items.GLOW_BERRIES);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MOSSLIGHT_FIREFLY.get().create(level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 40 == 0) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.3D, this.getZ(), 1, 0.12D, 0.06D, 0.12D, 0.0D);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MOSSLIGHT_FIREFLY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOSSLIGHT_FIREFLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOSSLIGHT_FIREFLY_DEATH.get();
    }
}
