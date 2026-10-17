package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BooleanSupplier;

/** 局部重力场运行状态。每三秒切换方向，十二秒后自动关闭。 */
public class GravityFieldCoreBlockEntity extends BlockEntity {
    private static final int ACTIVE_TICKS = 240;
    private static final int PHASE_TICKS = 60;
    private static final double FIELD_RADIUS = 8.0D;
    private static final double FIELD_HEIGHT = 6.0D;
    /** 每两 tick 施加一次，必须覆盖同期原版重力才会形成可见的反转上浮。 */
    private static final double FULL_ACCELERATION = 0.19D;
    /** 重力回响只削弱场强；仍需高于两 tick 的原版重力，避免玩家看起来完全不受影响。 */
    private static final double ATTUNED_ACCELERATION = 0.17D;

    private int remainingTicks;
    private int phaseTicks;
    private boolean handlingInteraction;
    private boolean changingState;
    private boolean ticking;
    private int stateRevision;

    public GravityFieldCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_FIELD_CORE.get(), pos, state);
    }

    boolean tryBeginInteraction() {
        if (handlingInteraction || changingState) {
            return false;
        }
        handlingInteraction = true;
        return true;
    }

    void endInteraction() {
        handlingInteraction = false;
    }

    boolean isCurrentState(BlockState state) {
        return level instanceof ServerLevel serverLevel && getBlockState() == state
                && getIfReady(serverLevel, worldPosition) == this;
    }

    @Nullable
    static GravityFieldCoreBlockEntity getIfReady(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return null;
        }
        BlockState state = chunk.getBlockState(pos);
        // 只接受已登记的实体，避免过期交互通过查询创建新的核心。
        if (!(state.getBlock() instanceof GravityFieldCoreBlock)
                || !(chunk.getBlockEntities().get(pos) instanceof GravityFieldCoreBlockEntity core)
                || core.isRemoved() || core.getLevel() != level || !core.getBlockPos().equals(pos)
                || core.getBlockState() != state) {
            return null;
        }
        return core;
    }

    public void activate() {
        setActive(true, () -> true);
    }

    public void deactivate() {
        setActive(false, () -> true);
    }

    boolean setActive(boolean active, BooleanSupplier current) {
        if (changingState || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState original = getBlockState();
        if (!isCurrentState(original) || !current.getAsBoolean()) {
            return false;
        }
        BlockState updated = original.setValue(GravityFieldCoreBlock.ACTIVE, active)
                .setValue(GravityFieldCoreBlock.INVERTED, active);
        if (updated == original) {
            return false;
        }
        int previousRemaining = remainingTicks;
        int previousPhase = phaseTicks;
        changingState = true;
        stateRevision++;
        boolean committed = false;
        try {
            // 状态写入会通知邻块；回调观察到新状态时，计时器必须已经与之匹配。
            remainingTicks = active ? ACTIVE_TICKS : 0;
            phaseTicks = 0;
            if (!serverLevel.setBlock(worldPosition, updated, 3)
                    || level != serverLevel || !isCurrentState(updated)) {
                return false;
            }
            committed = true;
            setChanged();
            if (level == serverLevel && isCurrentState(updated) && current.getAsBoolean()) {
                serverLevel.playSound(null, worldPosition, active
                                ? ModSounds.BLOCK_GRAVITY_FIELD_ACTIVATE.get()
                                : ModSounds.BLOCK_GRAVITY_FIELD_DEACTIVATE.get(),
                        SoundSource.BLOCKS, 0.9F, 1.0F);
            }
            return true;
        } finally {
            // 只恢复仍属于原现场的失败写入，不覆盖回调已经替换或成功写入的状态。
            try {
                if (!committed && level == serverLevel && isCurrentState(original)) {
                    remainingTicks = previousRemaining;
                    phaseTicks = previousPhase;
                }
            } finally {
                changingState = false;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  GravityFieldCoreBlockEntity core) {
        if (!(level instanceof ServerLevel serverLevel) || core.level != serverLevel
                || !core.worldPosition.equals(pos) || core.ticking || core.changingState
                || core.handlingInteraction || !core.isCurrentState(state)) {
            return;
        }
        core.ticking = true;
        try {
            if (!state.getValue(GravityFieldCoreBlock.ACTIVE)) {
                if (core.remainingTicks > 0 || core.phaseTicks > 0) {
                    core.remainingTicks = 0;
                    core.phaseTicks = 0;
                    core.setChanged();
                }
                return;
            }
            if (core.remainingTicks <= 0) {
                core.deactivate();
                return;
            }

            int revision = core.stateRevision;
            core.remainingTicks--;
            core.phaseTicks++;
            if (core.phaseTicks >= PHASE_TICKS) {
                BlockState updated = state.cycle(GravityFieldCoreBlock.INVERTED);
                if (!core.changePhase(serverLevel, state, updated, revision)) {
                    return;
                }
                state = updated;
                serverLevel.playSound(null, pos, state.getValue(GravityFieldCoreBlock.INVERTED)
                                ? ModSounds.BLOCK_GRAVITY_FIELD_RISE.get()
                                : ModSounds.BLOCK_GRAVITY_FIELD_FALL.get(),
                        SoundSource.BLOCKS, 0.85F, 1.0F);
            }
            BlockState runningState = state;
            BooleanSupplier current = () -> core.isCurrentTick(serverLevel, runningState, revision);
            if (!current.getAsBoolean()) {
                return;
            }
            if (core.remainingTicks == 0) {
                core.deactivate();
                return;
            }
            if (core.remainingTicks % 20 == 0) {
                core.setChanged();
                if (!current.getAsBoolean()) {
                    return;
                }
                MechanismFx.gravityBoundary(serverLevel, Vec3.atCenterOf(pos), FIELD_RADIUS,
                        state.getValue(GravityFieldCoreBlock.INVERTED));
                if (!current.getAsBoolean()) {
                    return;
                }
                GravityFieldCoreBlock.emitAnchorGuidanceWhileCurrent(serverLevel, pos, current);
                if (!current.getAsBoolean()) {
                    return;
                }
            }
            long gameTime = serverLevel.getGameTime();
            if (Math.floorMod(gameTime, 2L) != 0L) {
                return;
            }

            boolean inverted = state.getValue(GravityFieldCoreBlock.INVERTED);
            AABB field = fieldBounds(pos);
            for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, field,
                    candidate -> isFieldTarget(serverLevel, pos, field, candidate))) {
                if (!current.getAsBoolean()) {
                    return;
                }
                if (!isFieldTarget(serverLevel, pos, field, entity)) {
                    continue;
                }
                boolean attuned = entity instanceof ServerPlayer player
                        && EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO);
                if (!current.getAsBoolean()) {
                    return;
                }
                if (!isFieldTarget(serverLevel, pos, field, entity)) {
                    continue;
                }
                double acceleration = attuned ? ATTUNED_ACCELERATION : FULL_ACCELERATION;
                Vec3 motion = entity.getDeltaMovement();
                if (!current.getAsBoolean()) {
                    return;
                }
                if (!isFieldTarget(serverLevel, pos, field, entity)) {
                    continue;
                }
                double nextY = inverted
                        ? Math.min(0.38D, motion.y + acceleration)
                        : Math.max(-0.55D, motion.y - acceleration);
                entity.setDeltaMovement(motion.x, nextY, motion.z);
                if (!current.getAsBoolean()) {
                    return;
                }
                if (!isFieldTarget(serverLevel, pos, field, entity)) {
                    continue;
                }
                entity.hasImpulse = true;
                if (entity instanceof LivingEntity living && (inverted || attuned)) {
                    living.fallDistance = 0.0F;
                }
                if (entity instanceof ServerPlayer player) {
                    player.hurtMarked = true;
                }
            }

            for (var cue : MechanismFx.gravityFrame(Vec3.atCenterOf(pos), FIELD_RADIUS, FIELD_HEIGHT, inverted, gameTime)) {
                if (!current.getAsBoolean()) {
                    return;
                }
                MechanismFx.emit(serverLevel, List.of(cue));
            }
        } finally {
            core.ticking = false;
        }
    }

    private boolean isCurrentTick(ServerLevel serverLevel, BlockState state, int revision) {
        // 关闭再激活可能回到同一方块状态；旧 tick 不能借此继续执行。
        return level == serverLevel && stateRevision == revision && isCurrentState(state);
    }

    private boolean changePhase(ServerLevel serverLevel, BlockState original, BlockState updated, int revision) {
        boolean committed = false;
        phaseTicks = 0;
        try {
            if (!serverLevel.setBlock(worldPosition, updated, 3) || !isCurrentTick(serverLevel, updated, revision)) {
                return false;
            }
            committed = true;
            return true;
        } finally {
            // 未提交的阶段下一 tick 重试，不能覆盖回调建立的新一轮计时。
            if (!committed && isCurrentTick(serverLevel, original, revision)) {
                phaseTicks = PHASE_TICKS - 1;
            }
        }
    }

    private static boolean isFieldTarget(ServerLevel level, BlockPos pos, AABB field, Entity entity) {
        return entity.level() == level && entity.isAlive() && !entity.isRemoved() && !entity.isNoGravity()
                && field.intersects(entity.getBoundingBox())
                && horizontalDistanceSqr(entity, pos) <= FIELD_RADIUS * FIELD_RADIUS
                && (entity instanceof LivingEntity || entity instanceof ItemEntity
                || entity instanceof ExperienceOrb || entity instanceof Projectile)
                && (!(entity instanceof ServerPlayer player)
                || (!player.isSpectator() && !player.getAbilities().flying && !player.hasDisconnected()));
    }

    private static double horizontalDistanceSqr(Entity entity, BlockPos pos) {
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static AABB fieldBounds(BlockPos pos) {
        return new AABB(pos.getX() + 0.5D - FIELD_RADIUS, pos.getY() - 2.0D,
                pos.getZ() + 0.5D - FIELD_RADIUS, pos.getX() + 0.5D + FIELD_RADIUS,
                pos.getY() + FIELD_HEIGHT, pos.getZ() + 0.5D + FIELD_RADIUS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putInt("PhaseTicks", phaseTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        remainingTicks = Math.max(0, Math.min(ACTIVE_TICKS, tag.getInt("RemainingTicks")));
        phaseTicks = Math.max(0, Math.min(PHASE_TICKS - 1, tag.getInt("PhaseTicks")));
    }
}
