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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 记录主线 Boss 祭坛的共享重战冷却，避免多人或重启后绕过限制。 */
public class BossRechallengeAltarBlockEntity extends BlockEntity {
    private long cooldownUntil;
    @Nullable
    private UUID activeBossUuid;
    private boolean ownershipInitialized;

    public BossRechallengeAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_RECHALLENGE_ALTAR.get(), pos, state);
    }

    public static BossRechallengeAltarBlockEntity getOrCreate(ServerLevel level, BlockPos pos,
                                                               BlockState state) {
        if (level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
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
        if (!level.hasChunkAt(pos)) {
            DeepBossRechallengeCleanupData.queueDefeated(level, pos, defeatedBossUuid);
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GravityCoreAltarBlock)
                && !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            altar.startCooldown(level.getGameTime(), defeatedBossUuid);
        } else {
            DeepBossRechallengeCleanupData.queueDefeated(level, pos, defeatedBossUuid);
        }
    }

    public static boolean onBossAdded(ServerLevel level, BlockPos pos, Entity boss) {
        if (!level.hasChunkAt(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (!isExpectedAltar(state, boss)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar)) {

            return true;
        }
        return altar.remainingCooldownTicks(level) <= 0L && altar.registerBoss(boss.getUUID());
    }

    public static BindingState bindingState(ServerLevel level, BlockPos pos, Entity boss) {
        if (!level.hasChunkAt(pos)) {
            return BindingState.UNAVAILABLE;
        }
        BlockState state = level.getBlockState(pos);
        if (!isExpectedAltar(state, boss)) {
            return BindingState.INVALID;
        }
        if (!(level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar)) {
            return BindingState.UNAVAILABLE;
        }
        return altar.remainingCooldownTicks(level) <= 0L && altar.registerBoss(boss.getUUID())
                ? BindingState.VALID : BindingState.INVALID;
    }

    private static boolean isExpectedAltar(BlockState state, Entity boss) {
        return boss instanceof RiftWarden && state.getBlock() instanceof VoidCoreAltarBlock
                || boss instanceof VoidProphet && state.getBlock() instanceof GravityCoreAltarBlock;
    }

    public enum BindingState {
        UNAVAILABLE,
        INVALID,
        VALID
    }

    public static void onBossDestroyed(ServerLevel level, BlockPos pos, UUID bossUuid) {
        if (!level.hasChunkAt(pos)) {
            DeepBossRechallengeCleanupData.queueDestroyed(level, pos, bossUuid);
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GravityCoreAltarBlock)
                && !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            altar.clearBossIfOwned(bossUuid);
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
