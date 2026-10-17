package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.ai.AlertPackGoal;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/** 锚壳兽:只在受击后反击,并会在空间锚与稳定器附近安静休眠。 */
public class AnchorShellBeast extends PathfinderMob implements GeoEntity {
    private static final int RESONANCE_COOLDOWN_TICKS = 200;
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.anchor_shell_beast.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.anchor_shell_beast.walk");
    private static final RawAnimation PULSE_ANIM =
            RawAnimation.begin().then("animation.anchor_shell_beast.pulse", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int pulseCooldown;
    private int resonanceCooldown;
    private boolean nearAnchor;
    private BlockPos cachedAnchor;

    public AnchorShellBeast(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    /** 仍然只反击不主动索敌:AlertPackGoal 只在自己已有目标后拉群,不会让它先动手。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AnchorRestGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.65D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new AlertPackGoal(this, 10.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.pulseCooldown > 0) {
            this.pulseCooldown--;
        }
        if (this.resonanceCooldown > 0) {
            this.resonanceCooldown--;
        }
        if (!this.level().isClientSide && this.getTarget() == null && this.tickCount % 20 == 0) {
            this.nearAnchor = this.scanForAnchor();
        }
        if (!this.level().isClientSide && this.nearAnchor && this.getTarget() == null) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.2D, movement.y, movement.z * 0.2D);
        }
    }

    /** 消耗普通空间锚片，换取一次不影响进度的局部稳定脉冲。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.SPACE_ANCHOR_SHARD.get())) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(this.level() instanceof ServerLevel serverLevel) || !this.canResonate(serverLevel, player)) {
            return InteractionResult.CONSUME;
        }
        this.nearAnchor = this.scanForAnchor();
        if (!this.nearAnchor) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.anchor_shell_beast.no_anchor"), true);
            return InteractionResult.CONSUME;
        }
        if (this.resonanceCooldown > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.anchor_shell_beast.not_ready"), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.resonanceCooldown = RESONANCE_COOLDOWN_TICKS;
        Vec3 pulseCenter = this.position();
        AABB pulseBounds = this.getBoundingBox().inflate(6.0D);
        this.triggerAnim("pulse_controller", "pulse");
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pulseCenter.x, pulseCenter.y + 0.55D, pulseCenter.z,
                48, 1.4D, 0.45D, 1.4D, 0.08D);
        serverLevel.playSound(null, BlockPos.containing(pulseCenter), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.NEUTRAL, 1.0F, 0.9F);
        if (!this.canResonate(serverLevel, player)) {
            return InteractionResult.CONSUME;
        }
        for (Player nearby : serverLevel.getEntitiesOfClass(Player.class,
                pulseBounds, target -> canReceiveResonance(serverLevel, pulseCenter, target))) {
            if (!this.canResonate(serverLevel, player)) {
                return InteractionResult.CONSUME;
            }
            if (!canReceiveResonance(serverLevel, pulseCenter, nearby)) {
                continue;
            }
            nearby.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                    120, 0, true, false, true));
            if (!this.canResonate(serverLevel, player)) {
                return InteractionResult.CONSUME;
            }
            if (!canReceiveResonance(serverLevel, pulseCenter, nearby)) {
                continue;
            }
            nearby.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    60, 0, true, false, true));
        }
        return InteractionResult.CONSUME;
    }

    private boolean canResonate(ServerLevel level, Player player) {
        return this.level() == level && this.isAlive() && player.level() == level
                && player.isAlive() && !player.isSpectator()
                && player.canInteractWithEntity(this, 1.0D);
    }

    private boolean canReceiveResonance(ServerLevel level, Vec3 center, Player target) {
        return target.level() == level && target.isAlive() && !target.isSpectator()
                && target.distanceToSqr(center) <= 6.0D * 6.0D;
    }

    private boolean scanForAnchor() {
        if (!(this.level() instanceof ServerLevel source)) {
            this.cachedAnchor = null;
            return false;
        }
        BlockPos center = this.blockPosition();
        int minChunkX = (center.getX() - 5) >> 4;
        int minChunkZ = (center.getZ() - 5) >> 4;
        int cachedIndex = -1;
        LevelChunk cachedChunk = null;
        if (this.cachedAnchor != null
                && Math.abs(this.cachedAnchor.getX() - center.getX()) <= 5
                && Math.abs(this.cachedAnchor.getY() - center.getY()) <= 2
                && Math.abs(this.cachedAnchor.getZ() - center.getZ()) <= 5
                && !source.isOutsideBuildHeight(this.cachedAnchor)) {
            int chunkX = this.cachedAnchor.getX() >> 4;
            int chunkZ = this.cachedAnchor.getZ() >> 4;
            cachedIndex = (chunkX - minChunkX) + 2 * (chunkZ - minChunkZ);
            cachedChunk = source.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (cachedChunk != null) {
                BlockState state = cachedChunk.getBlockState(this.cachedAnchor);
                if (state.is(ModBlocks.SPACE_ANCHOR.get()) || state.is(ModBlocks.VOID_STABILIZER.get())) {
                    return true;
                }
            }
        }
        this.cachedAnchor = null;
        int minY = Math.max(center.getY() - 2, source.getMinBuildHeight());
        int maxY = Math.min(center.getY() + 2, source.getMaxBuildHeight() - 1);
        if (minY > maxY) {
            return false;
        }
        LevelChunk[] chunks = new LevelChunk[4];
        int checkedChunks = 0;
        if (cachedIndex >= 0) {
            chunks[cachedIndex] = cachedChunk;
            checkedChunks |= 1 << cachedIndex;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                new BlockPos(center.getX() - 5, minY, center.getZ() - 5),
                new BlockPos(center.getX() + 5, maxY, center.getZ() + 5))) {
            // 11×11 格的水平范围最多跨四个区块，同次查询只检查各区块一次。
            int index = ((pos.getX() >> 4) - minChunkX) + 2 * ((pos.getZ() >> 4) - minChunkZ);
            int bit = 1 << index;
            if ((checkedChunks & bit) == 0) {
                checkedChunks |= bit;
                chunks[index] = source.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            }
            LevelChunk chunk = chunks[index];
            if (chunk == null) {
                continue;
            }
            BlockState state = chunk.getBlockState(pos);
            if (state.is(ModBlocks.SPACE_ANCHOR.get()) || state.is(ModBlocks.VOID_STABILIZER.get())) {
                this.cachedAnchor = pos.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.pulseCooldown == 0) {
            this.pulseCooldown = 100;
            this.releaseAnchorPulse((ServerLevel) this.level());
        }
        return hurt;
    }

    private void releaseAnchorPulse(ServerLevel level) {
        this.triggerAnim("pulse_controller", "pulse");
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 0.55D, this.getZ(),
                42, 1.5D, 0.45D, 1.5D, 0.08D);
        level.playSound(null, this.blockPosition(), ModSounds.ANCHOR_SHELL_BEAST_PULSE.get(),
                SoundSource.NEUTRAL, 1.2F, 1.0F);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.5D), entity -> entity != this && entity.isAlive())) {
            Vec3 offset = entity.position().subtract(this.position());
            double distanceSqr = offset.lengthSqr();
            if (distanceSqr < 0.1D * 0.1D || distanceSqr > 4.5D * 4.5D) {
                continue;
            }
            double distance = Math.sqrt(distanceSqr);
            double force = distance <= 2.5D ? 0.85D : -0.42D;
            Vec3 impulse = offset.normalize().scale(force);
            entity.push(impulse.x, 0.2D, impulse.z);
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ResonanceCooldown", this.resonanceCooldown);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.resonanceCooldown = Math.max(0,
                Math.min(tag.getInt("ResonanceCooldown"), RESONANCE_COOLDOWN_TICKS));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ANCHOR_SHELL_BEAST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ANCHOR_SHELL_BEAST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ANCHOR_SHELL_BEAST_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
        controllers.add(new AnimationController<>(this, "pulse_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("pulse", PULSE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private class AnchorRestGoal extends Goal {
        private AnchorRestGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return AnchorShellBeast.this.nearAnchor && AnchorShellBeast.this.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            AnchorShellBeast.this.getNavigation().stop();
        }
    }
}
