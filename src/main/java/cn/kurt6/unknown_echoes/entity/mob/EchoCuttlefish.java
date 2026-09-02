package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 回声墨鱼:以短促脉冲和腕足摆动在镜湖礁区游弋的水生动物。 */
public class EchoCuttlefish extends AbstractSchoolingFish implements GeoEntity {
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_cuttlefish.swim");
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_cuttlefish.idle");
    private static final RawAnimation STARTLE_ANIM =
            RawAnimation.begin().then("animation.echo_cuttlefish.startle", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public EchoCuttlefish(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSchoolingFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.48D);
    }

    @Override
    public int getMaxSchoolSize() {
        return 3;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.ECHO_CUTTLEFISH_BUCKET.get());
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.ECHO_CUTTLEFISH_FLOP.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ECHO_CUTTLEFISH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ECHO_CUTTLEFISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ECHO_CUTTLEFISH_DEATH.get();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.triggerAnim("reaction_controller", "startle");
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3, state ->
                state.setAndContinue(this.isInWater() && state.isMoving() ? SWIM_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "reaction_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("startle", STARTLE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
