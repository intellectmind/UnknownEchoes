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
import cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class WindDriftJelly extends AerialAnimal {
    private Vec3 windDirection = Vec3.ZERO;
    public WindDriftJelly(EntityType<? extends Animal> type, Level level) {
        super(type, level, "wind_drift_jelly");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FLYING_SPEED, 0.19D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.DREAM_NECTAR.get());
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.WIND_DRIFT_JELLY.get().create(level);
    }

    @Override
    protected Vec3 preferredFlightDirection() {
        return this.windDirection;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.tickCount % 20 != 0) {
            return;
        }
        if (this.tickCount % 100 == 0 && this.level() instanceof ServerLevel serverLevel) {
            BlockPos platform = WindCurrentPlatformBlockEntity.nearestPlatform(serverLevel, this.blockPosition());
            this.windDirection = platform != null && platform.distSqr(this.blockPosition()) <= 48.0D * 48.0D
                    && serverLevel.hasChunkAt(platform)
                    && serverLevel.getBlockState(platform).is(ModBlocks.WIND_CURRENT_PLATFORM.get())
                    ? Vec3.atCenterOf(platform).subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize()
                    : Vec3.ZERO;
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.1D),
                player -> player.isAlive() && !player.isSpectator())) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false), this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WIND_DRIFT_JELLY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WIND_DRIFT_JELLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WIND_DRIFT_JELLY_DEATH.get();
    }
}
