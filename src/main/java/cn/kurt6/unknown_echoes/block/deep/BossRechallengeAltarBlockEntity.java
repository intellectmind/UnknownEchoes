package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 记录主线 Boss 祭坛的共享重战冷却，避免多人或重启后绕过限制。 */
public class BossRechallengeAltarBlockEntity extends BlockEntity {
    private long cooldownUntil;
    @Nullable
    private UUID activeBossUuid;
    private boolean ownershipInitialized;
    private boolean recoveringBosses;

    public BossRechallengeAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_RECHALLENGE_ALTAR.get(), pos, state);
    }

    @Nullable
    public static BossRechallengeAltarBlockEntity getOrCreate(ServerLevel level, BlockPos pos,
                                                               BlockState state) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || level.isOutsideBuildHeight(pos) || chunk.getBlockState(pos) != state) {
            return null;
        }
        if (chunk.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            DeepBossRechallengeCleanupData.apply(level, pos, altar);
            return altar;
        }
        BossRechallengeAltarBlockEntity altar = new BossRechallengeAltarBlockEntity(pos, state);
        level.setBlockEntity(altar);
        DeepBossRechallengeCleanupData.apply(level, pos, altar);
        return altar;
    }

    /** Boss 死亡后从其绑定的祭坛开始冷却；祭坛未加载时持久化通知。 */
    public static void startCooldown(ServerLevel level, BlockPos pos, UUID defeatedBossUuid) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            DeepBossRechallengeCleanupData.queueDefeated(level, pos, defeatedBossUuid);
            return;
        }
        BlockState state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof GravityCoreAltarBlock)
                && !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        if (chunk.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            DeepBossRechallengeCleanupData.queueDefeated(level, pos, defeatedBossUuid);
            DeepBossRechallengeCleanupData.apply(level, pos, altar);
        } else {
            DeepBossRechallengeCleanupData.queueDefeated(level, pos, defeatedBossUuid);
        }
    }

    public static boolean onBossAdded(ServerLevel level, BlockPos pos, Entity boss) {
        return bindingState(level, pos, boss) != BindingState.INVALID;
    }

    public static BindingState bindingState(ServerLevel level, BlockPos pos, Entity boss) {
        if (!isBindingCandidate(level, pos, boss)) {
            return BindingState.INTERRUPTED;
        }
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return BindingState.UNAVAILABLE;
        }
        BlockState state = chunk.getBlockState(pos);
        if (!isExpectedAltar(state, boss)) {
            return BindingState.INVALID;
        }
        if (!(chunk.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar)) {
            return BindingState.UNAVAILABLE;
        }
        if (!altar.isCurrentAltar(level, state) || altar.recoveringBosses) {
            return BindingState.INTERRUPTED;
        }
        // 登记和扫描共用保护；冷却归零、首次认领及其补偿都可能同步通知邻块。
        altar.recoveringBosses = true;
        try {
            UUID previousOwner = altar.activeBossUuid;
            boolean previousHistory = altar.ownershipInitialized;
            long now = level.getGameTime();
            long expectedCooldown = Math.min(altar.cooldownUntil, safeDeadline(now,
                    Math.max(0L, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get())));
            expectedCooldown = now < expectedCooldown ? expectedCooldown : 0L;
            long remaining = altar.remainingCooldownTicks(level);
            if (bindingResult(level, pos, state, altar, boss, previousOwner,
                    previousHistory, expectedCooldown) == BindingState.INTERRUPTED) {
                return BindingState.INTERRUPTED;
            }
            if (remaining > 0L) {
                return BindingState.INVALID;
            }
            boolean newClaim = altar.canAdoptUnownedBoss();
            try {
                boolean registered = altar.registerBoss(boss.getUUID());
                return bindingResult(level, pos, state, altar, boss,
                        registered ? boss.getUUID() : previousOwner,
                        previousHistory || registered, expectedCooldown);
            } finally {
                if (newClaim && altar.ownsBoss(boss.getUUID()) && altar.isCurrentAltar(level, state)
                        && shouldReleaseBindingClaim(level, pos, boss)) {
                    altar.clearBossIfOwned(boss.getUUID());
                }
            }
        } finally {
            altar.recoveringBosses = false;
        }
    }

    private static boolean isBindingCandidate(ServerLevel level, BlockPos pos, Entity boss) {
        return boss.level() == level && boss.isAlive() && !boss.isRemoved()
                && level.getEntity(boss.getUUID()) == boss
                && boss instanceof cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss boundBoss
                && !boundBoss.isSettlingDeath() && pos.equals(boundBoss.deepRechallengeAltar());
    }

    private static BindingState bindingResult(ServerLevel level, BlockPos pos, BlockState state,
                                             BossRechallengeAltarBlockEntity altar, Entity boss,
                                             @Nullable UUID expectedOwner, boolean expectedHistory,
                                             long expectedCooldown) {
        if (!altar.isCurrentAltar(level, state) || !isBindingCandidate(level, pos, boss)
                || !java.util.Objects.equals(altar.activeBossUuid, expectedOwner)
                || altar.ownershipInitialized != expectedHistory || altar.cooldownUntil != expectedCooldown) {
            return BindingState.INTERRUPTED;
        }
        return altar.ownsBoss(boss.getUUID()) ? BindingState.VALID : BindingState.INVALID;
    }

    private static boolean shouldReleaseBindingClaim(ServerLevel level, BlockPos pos, Entity boss) {
        if (boss instanceof cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss boundBoss) {
            // 死亡调用仍持有场地收尾权；卸载或不可见索引也不能当成已销毁。
            if (boundBoss.isSettlingDeath() || boss.isRemoved()
                    && (boss.getRemovalReason() == null || !boss.getRemovalReason().shouldDestroy())) {
                return false;
            }
            Entity indexed = level.getEntity(boss.getUUID());
            return boss.level() != level || !pos.equals(boundBoss.deepRechallengeAltar())
                    || !boss.isAlive() || indexed != null && indexed != boss;
        }
        return false;
    }

    private static boolean isExpectedAltar(BlockState state, Entity boss) {
        return boss instanceof RiftWarden && state.getBlock() instanceof VoidCoreAltarBlock
                || boss instanceof VoidProphet && state.getBlock() instanceof GravityCoreAltarBlock;
    }

    public enum BindingState {
        INTERRUPTED,
        UNAVAILABLE,
        INVALID,
        VALID
    }

    public static void onBossDestroyed(ServerLevel level, BlockPos pos, UUID bossUuid) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            DeepBossRechallengeCleanupData.queueDestroyed(level, pos, bossUuid);
            return;
        }
        BlockState state = chunk.getBlockState(pos);
        if (!(state.getBlock() instanceof GravityCoreAltarBlock)
                && !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        if (chunk.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            DeepBossRechallengeCleanupData.queueDestroyed(level, pos, bossUuid);
            DeepBossRechallengeCleanupData.apply(level, pos, altar);
        } else {

            DeepBossRechallengeCleanupData.queueDestroyed(level, pos, bossUuid);
        }
    }

    @Nullable
    public Entity findActiveBoss(ServerLevel level) {
        if (activeBossUuid == null) {
            return null;
        }
        Entity boss = level.getEntity(activeBossUuid);
        return boss instanceof cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss boundBoss
                && worldPosition.equals(boundBoss.deepRechallengeAltar())
                && boss.isAlive() && !boss.isRemoved() ? boss : null;
    }

    public boolean hasActiveBoss() {
        return activeBossUuid != null;
    }

    public boolean ownsBoss(UUID bossUuid) {
        return bossUuid.equals(activeBossUuid);
    }

    public boolean canAdoptUnownedBoss() {
        return activeBossUuid == null && !ownershipInitialized;
    }

    public boolean isCurrentAltar(ServerLevel level, BlockState state) {
        if (isRemoved() || this.level != level) {
            return false;
        }
        var chunk = level.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition) == this
                && chunk.getBlockState(worldPosition) == state;
    }

    public boolean isRecoveryInProgress() {
        return recoveringBosses;
    }

    /** 清理回调会改变后续候选和祭坛；中断的扫描不能被当成空场继续生成。 */
    public RecoveryResult recoverBosses(ServerLevel level, BlockState state,
                                        Class<? extends Entity> bossType, int range, int rangeV) {
        if (recoveringBosses || !isCurrentAltar(level, state)) {
            return new RecoveryResult(null, true);
        }
        recoveringBosses = true;
        try {
            RecoveryContext context = new RecoveryContext(activeBossUuid, ownershipInitialized, cooldownUntil);
            AABB bounds = new AABB(worldPosition).inflate(range, rangeV, range);
            if (!isRecoveryCurrent(level, state, range, context)) {
                return new RecoveryResult(null, true);
            }
            var recovered = level.getEntitiesOfClass(bossType, bounds,
                    boss -> isRecoveryCandidate(level, state, bounds, boss));
            if (!isRecoveryCurrent(level, state, range, context)) {
                return new RecoveryResult(null, true);
            }
            Entity owner = null;
            for (Entity boss : recovered) {
                if (isRecoveryCandidate(level, state, bounds, boss)
                        && (ownsBoss(boss.getUUID()) || owner == null && canAdoptUnownedBoss())) {
                    owner = boss;
                }
            }
            for (Entity boss : recovered) {
                if (!isRecoveryCurrent(level, state, range, context)
                        || owner != null && !isRecoveryCandidate(level, state, bounds, owner)) {
                    return new RecoveryResult(null, true);
                }
                if (boss != owner && isRecoveryCandidate(level, state, bounds, boss)) {
                    boss.discard();
                }
            }
            // 最后一条移除通知也可能使本次恢复失效。
            if (!isRecoveryCurrent(level, state, range, context)
                    || owner != null && !isRecoveryCandidate(level, state, bounds, owner)) {
                return new RecoveryResult(null, true);
            }
            if (owner == null) {
                return new RecoveryResult(null, false);
            }
            boolean newClaim = !ownsBoss(owner.getUUID());
            try {
                replaceActiveBoss(owner.getUUID());
                if (!isCurrentAltar(level, state) || !owner.getUUID().equals(activeBossUuid)
                        || cooldownUntil != context.cooldownUntil()
                        || !areArenaChunksReady(level, worldPosition, range)
                        || !isRecoveryCandidate(level, state, bounds, owner)) {
                    return new RecoveryResult(null, true);
                }
                return new RecoveryResult(owner, false);
            } finally {
                // 只补偿本次失效的新认领；卸载时保留 UUID，真实击败冷却和后续归属不受影响。
                if (newClaim && owner.getUUID().equals(activeBossUuid)
                        && isCurrentAltar(level, state)
                        && areArenaChunksReady(level, worldPosition, range)
                        && !isRecoveryCandidate(level, state, bounds, owner)) {
                    clearBossIfOwned(owner.getUUID());
                }
            }
        } finally {
            recoveringBosses = false;
        }
    }

    private boolean isRecoveryCurrent(ServerLevel level, BlockState state, int range,
                                      RecoveryContext context) {
        return isCurrentAltar(level, state)
                && areArenaChunksReady(level, worldPosition, range)
                && java.util.Objects.equals(activeBossUuid, context.activeUuid())
                && ownershipInitialized == context.ownershipInitialized()
                && cooldownUntil == context.cooldownUntil();
    }

    private boolean isRecoveryCandidate(ServerLevel level, BlockState state, AABB bounds, Entity boss) {
        return boss.level() == level && boss.isAlive() && !boss.isRemoved()
                && level.getEntity(boss.getUUID()) == boss && isExpectedAltar(state, boss)
                && boss instanceof cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss boundBoss
                && worldPosition.equals(boundBoss.deepRechallengeAltar())
                && bounds.intersects(boss.getBoundingBox());
    }

    public static boolean areArenaChunksReady(ServerLevel level, BlockPos center, int radius) {
        int safeRadius = Math.max(0, radius);
        int minX = (center.getX() - safeRadius) >> 4;
        int maxX = (center.getX() + safeRadius) >> 4;
        int minZ = (center.getZ() - safeRadius) >> 4;
        int maxZ = (center.getZ() + safeRadius) >> 4;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null
                        || !level.areEntitiesLoaded(ChunkPos.asLong(chunkX, chunkZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    public static BlockState getReadyAltarState(ServerLevel level, BlockPos altarPos) {
        if (level.isOutsideBuildHeight(altarPos)) {
            return null;
        }
        var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(altarPos);
    }

    private record RecoveryContext(@Nullable UUID activeUuid, boolean ownershipInitialized, long cooldownUntil) {}

    public record RecoveryResult(@Nullable Entity boss, boolean interrupted) {}

    public void replaceActiveBoss(UUID bossUuid) {
        if (!bossUuid.equals(activeBossUuid)) {
            activeBossUuid = bossUuid;
            ownershipInitialized = true;
            setChanged();
        }
    }

    public boolean registerBoss(UUID bossUuid) {
        if (activeBossUuid == null) {
            if (ownershipInitialized) {
                return false;
            }
            activeBossUuid = bossUuid;
            ownershipInitialized = true;
            setChanged();
        }
        return bossUuid.equals(activeBossUuid);
    }

    public boolean reserveBoss(UUID bossUuid) {
        if (activeBossUuid != null) {
            return bossUuid.equals(activeBossUuid);
        }
        activeBossUuid = bossUuid;
        ownershipInitialized = true;
        setChanged();
        return true;
    }

    public void releaseBossReservation(UUID bossUuid) {
        clearBossIfOwned(bossUuid);
    }

    public long remainingCooldownTicks(ServerLevel level) {
        long now = level.getGameTime();
        long maximum = safeDeadline(now,
                Math.max(0L, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get()));
        if (cooldownUntil > maximum) {
            cooldownUntil = maximum;
            setChanged();
        }
        if (now < cooldownUntil) {
            return cooldownUntil - now;
        }
        if (cooldownUntil != 0L) {
            cooldownUntil = 0L;
            setChanged();
        }
        return 0L;
    }

    void startCooldown(long now, UUID defeatedBossUuid) {
        if (activeBossUuid == null && !ownershipInitialized) {
            activeBossUuid = defeatedBossUuid;
            ownershipInitialized = true;
        }
        if (!defeatedBossUuid.equals(activeBossUuid)) {
            return;
        }
        activeBossUuid = null;
        long duration = Math.max(0, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get());
        cooldownUntil = duration == 0L ? 0L : safeDeadline(now, duration);
        setChanged();
    }

    void clearBossIfOwned(UUID bossUuid) {
        if (bossUuid.equals(activeBossUuid)) {
            activeBossUuid = null;
            setChanged();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            DeepBossRechallengeCleanupData.apply(serverLevel, worldPosition, this);
        }
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("CooldownUntil", cooldownUntil);
        if (activeBossUuid != null) {
            tag.putUUID("ActiveBoss", activeBossUuid);
        }
        tag.putBoolean("OwnershipInitialized", ownershipInitialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cooldownUntil = Math.max(0L, tag.getLong("CooldownUntil"));
        activeBossUuid = tag.hasUUID("ActiveBoss") ? tag.getUUID("ActiveBoss") : null;
        ownershipInitialized = tag.getBoolean("OwnershipInitialized")
                || activeBossUuid != null || cooldownUntil > 0L;
    }
}
