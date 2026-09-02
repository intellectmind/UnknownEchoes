package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** 荧光兔:回响森林与镜湖附近的小型被动生物(生命 6)。 */
public class GlowRabbit extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.glow_rabbit.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.glow_rabbit.walk");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public GlowRabbit(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    /** FleeThreatGoal 按 Enemy 接口判定,比 AvoidEntityGoal 的 Monster.class 多覆盖恶魂/幻翼/史莱姆。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.2D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 10.0D, 2.0D, 2.2D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2D,
                stack -> stack.is(Items.CARROT) || stack.is(Items.DANDELION) || stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.DANDELION) || stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.GLOW_RABBIT.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GLOW_RABBIT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GLOW_RABBIT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GLOW_RABBIT_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
