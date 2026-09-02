package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 折页虫:折叠回廊中受惊会迅速收拢甲页的被动小型生物。 */
public class FoldpageBug extends PathfinderMob implements GeoEntity {
    private static final int GUIDE_TICKS = 160;
    private static final int GUIDE_RADIUS = 24;
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.foldpage_bug.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.foldpage_bug.walk");
    private static final RawAnimation REACT_ANIM =
            RawAnimation.begin().then("animation.foldpage_bug.react", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int guideTicks;
    private BlockPos guideTarget;

    public FoldpageBug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    /** 空手轻触会让折页虫沿附近路线节点投出一小段折痕光路。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(this.level() instanceof ServerLevel serverLevel) || player.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (this.guideTicks > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.foldpage_bug.guide_busy"), true);
            return InteractionResult.CONSUME;
        }
        this.guideTarget = findNearestRouteTarget(serverLevel);
        this.guideTicks = GUIDE_TICKS;
        this.triggerAnim("reaction_controller", "react");
        serverLevel.playSound(null, this.blockPosition(), ModSounds.FOLDPAGE_BUG_GUIDE.get(),
                SoundSource.NEUTRAL, 0.7F, 1.0F);
        if (this.guideTarget == null) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.foldpage_bug.no_route"), true);
        } else {
            renderGuidePath(serverLevel);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * 躲避排在 PanicGoal 之后、游荡之前:被打到的反应优先,但要在敌怪贴上来之前就走开。
     * 引导任务(guideTicks)靠 aiStep 每 20 tick 重发 moveTo,躲避结束后会自己接回来。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.55D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 8.0D, 1.15D, 1.5D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.triggerAnim("reaction_controller", "react");
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF,
                    this.getX(), this.getY() + 0.25D, this.getZ(),
                    5, 0.22D, 0.12D, 0.22D, 0.01D);
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.guideTicks <= 0) {
            return;
        }
        if (--this.guideTicks <= 0) {
            this.guideTarget = null;
            return;
        }
        if (this.guideTarget == null) {
            return;
        }
        if (!this.level().isLoaded(this.guideTarget)
                || !isGuideTarget(this.level(), this.guideTarget)) {
            this.guideTarget = null;
            return;
        }
        if (this.tickCount % 20 == 0) {
            this.getNavigation().moveTo(this.guideTarget.getX() + 0.5D,
                    this.guideTarget.getY(), this.guideTarget.getZ() + 0.5D, 0.9D);
        }
        if (this.tickCount % 10 == 0) {
            renderGuidePath((ServerLevel) this.level());
        }
    }

    private BlockPos findNearestRouteTarget(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int dx = -GUIDE_RADIUS; dx <= GUIDE_RADIUS; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -GUIDE_RADIUS; dz <= GUIDE_RADIUS; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(candidate) || !isGuideTarget(level, candidate)) {
                        continue;
                    }
                    double distance = candidate.distSqr(origin);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = candidate.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static boolean isGuideTarget(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(ModBlocks.DEEP_ROUTE_NODE.get())
                || state.is(ModBlocks.DEEP_CALIBRATION_NODE.get())
                || state.is(ModBlocks.GRAVITY_FOLD_DOOR.get());
    }

    private void renderGuidePath(ServerLevel level) {
        if (this.guideTarget == null) {
            return;
        }
        Vec3 start = this.position().add(0.0D, 0.35D, 0.0D);
        Vec3 target = Vec3.atCenterOf(this.guideTarget).add(0.0D, 0.45D, 0.0D);
        Vec3 delta = target.subtract(start);
        double length = Math.min(delta.length(), 16.0D);
        if (length < 0.1D) {
            return;
        }
        Vec3 step = delta.normalize();
        for (double distance = 0.0D; distance <= length; distance += 1.0D) {
            Vec3 point = start.add(step.scale(distance));
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    1, 0.04D, 0.04D, 0.04D, 0.0D);
        }
        level.sendParticles(ParticleTypes.GLOW, target.x, target.y, target.z,
                3, 0.18D, 0.18D, 0.18D, 0.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.FOLDPAGE_BUG_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FOLDPAGE_BUG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FOLDPAGE_BUG_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "reaction_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("react", REACT_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
