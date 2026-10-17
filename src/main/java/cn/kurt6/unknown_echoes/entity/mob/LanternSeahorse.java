package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.entity.EntityBodyCollision;
import net.minecraft.world.phys.AABB;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 灯囊海马:在礁间直立游动、可用水桶捕捉的小型鱼类。 */
public class LanternSeahorse extends EchoSchoolingFish implements GeoEntity {
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.lantern_seahorse.swim");
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.lantern_seahorse.idle");
    private static final RawAnimation STARTLE_ANIM =
            RawAnimation.begin().then("animation.lantern_seahorse.startle", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public LanternSeahorse(EntityType<? extends EchoSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSchoolingFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    @Override
    public int getMaxSchoolSize() {
        return 2;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.LANTERN_SEAHORSE_FLOP.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LANTERN_SEAHORSE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LANTERN_SEAHORSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LANTERN_SEAHORSE_DEATH.get();
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

    @Override
    public boolean isInWall() {
        return EntityBodyCollision.isInWall(this);
    }

    @Override
    protected AABB makeBoundingBox() {
        return EntityHitboxProfiles.make(this);
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        EntityHitboxProfiles.refresh(this);
    }

    /** 身体朝向可能由原版控制器直接更新，tick 结束后同步长方形判定。 */
    @Override
    public void tick() {
        super.tick();
        EntityHitboxProfiles.refresh(this);
    }

    /** 控制器可直接改变身体朝向，移动碰撞必须先读取当前长宽。 */
    @Override
    public void move(MoverType type, Vec3 movement) {
        EntityHitboxProfiles.refresh(this);
        super.move(type, movement);
    }
}
