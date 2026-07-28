package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** 世界共鸣祭坛的持久召唤事务，负责防重复召唤和服务器重启后的状态恢复。 */
public class WorldResonanceAltarBlockEntity extends BlockEntity {
    private static final int BOSS_RANGE_H = 48;
    private static final int BOSS_RANGE_V = 20;
    private static final long MISSING_BOSS_GRACE_TICKS = 100L;
    private static final long SETTLING_TICKS = 40L;
    private static final long RECHALLENGE_COOLDOWN_TICKS = 200L;

    private UUID arenaId = UUID.randomUUID();
    private TransactionState state = TransactionState.IDLE;
    @Nullable
    private UUID bossUuid;
    @Nullable
    private UUID summonerUuid;
    private long cooldownUntil;
    private long stateChangedAt;
    private long missingBossSince = -1L;

    public WorldResonanceAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORLD_RESONANCE_ALTAR.get(), pos, state);
    }

    public UUID getArenaId() {
        return arenaId;
    }

    public boolean beginStarting(ServerLevel level, UUID summoner) {
        reconcile(level);
        if (state != TransactionState.IDLE) {
            return false;
        }
        state = TransactionState.STARTING;
        bossUuid = null;
        summonerUuid = summoner;
        stateChangedAt = level.getGameTime();
        missingBossSince = -1L;
        setChanged();
        return true;
    }

    public void markActive(WorldEcho boss) {
        if (!(level instanceof ServerLevel serverLevel) || state != TransactionState.STARTING) {
            return;
        }
        if (!boss.bindToAltar(arenaId, worldPosition)) {
            abortStarting();
            return;
        }
        state = TransactionState.ACTIVE;
        bossUuid = boss.getUUID();
        stateChangedAt = serverLevel.getGameTime();
        missingBossSince = -1L;
        setChanged();
    }

    public void abortStarting() {
        if (state != TransactionState.STARTING) {
            return;
        }
        state = TransactionState.IDLE;
        bossUuid = null;
        summonerUuid = null;
        missingBossSince = -1L;
        stateChangedAt = level == null ? stateChangedAt : level.getGameTime();
        setChanged();
    }

    @Nullable
    public WorldEcho findActiveBoss(ServerLevel level) {
        reconcile(level);
        return findLinkedBoss(level, false);
    }

    public void onBossSettled(UUID settledArenaId, UUID settledBossUuid) {
        if (!(level instanceof ServerLevel serverLevel)
                || !arenaId.equals(settledArenaId)
                || bossUuid == null
                || !bossUuid.equals(settledBossUuid)) {
            return;
        }
        state = TransactionState.SETTLING;
        stateChangedAt = serverLevel.getGameTime();
        missingBossSince = -1L;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WorldResonanceAltarBlockEntity altar) {
        long interval = altar.state == TransactionState.IDLE ? 200L : 20L;
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % interval == 0L) {
            altar.reconcile(serverLevel);
        }
    }

    private void reconcile(ServerLevel level) {
        WorldEcho boss = findLinkedBoss(level, true);
        if (boss != null) {
            adoptBoss(level, boss);
            return;
        }

        long now = level.getGameTime();
        switch (state) {
            case STARTING, ACTIVE -> {
                if (missingBossSince < 0L) {
                    missingBossSince = now;
                } else if (elapsed(now, missingBossSince, MISSING_BOSS_GRACE_TICKS)) {
                    UnknownEchoes.LOGGER.warn(
                            "World resonance altar {} recovered {} without a live boss",
                            worldPosition, state);
                    resetToIdle(now);
                }
            }
            case SETTLING -> {
                if (elapsed(now, stateChangedAt, SETTLING_TICKS)) {
                    state = TransactionState.COOLDOWN;
                    bossUuid = null;
                    summonerUuid = null;
                    cooldownUntil = safeDeadline(now, RECHALLENGE_COOLDOWN_TICKS);
                    stateChangedAt = now;
                    setChanged();
                }
            }
            case COOLDOWN -> {
                if (now < stateChangedAt || now >= cooldownUntil) {
                    resetToIdle(now);
                }
            }
            case IDLE -> missingBossSince = -1L;
        }
    }

    @Nullable
    private WorldEcho findLinkedBoss(ServerLevel level, boolean allowAdoption) {
        if (bossUuid != null) {
            Entity entity = level.getEntity(bossUuid);
            if (entity instanceof WorldEcho boss && boss.isAlive() && !boss.isRemoved()) {
                if (boss.matchesAltar(arenaId, worldPosition)) {
                    return boss;
                }
                if (allowAdoption && boss.canBindToAltar()) {
                    boss.bindToAltar(arenaId, worldPosition);
                    return boss;
                }
                UnknownEchoes.LOGGER.warn(
                        "World resonance altar {} rejected boss {} with a mismatched arena binding",
                        worldPosition, bossUuid);
                bossUuid = null;
                setChanged();
            }
        }

        AABB arena = new AABB(worldPosition).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H);
        List<WorldEcho> nearby = level.getEntitiesOfClass(WorldEcho.class, arena,
                boss -> boss.isAlive() && !boss.isRemoved());
        // 祭坛只需最近的可绑定 Boss；避免每次心跳都对整座场地的实体列表排序。
        WorldEcho nearestLinked = null;
        WorldEcho nearestBindable = null;
        double nearestLinkedDistance = Double.MAX_VALUE;
        double nearestBindableDistance = Double.MAX_VALUE;
        for (WorldEcho boss : nearby) {
            double distance = boss.distanceToSqr(
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D);
            if (boss.matchesAltar(arenaId, worldPosition)) {
                if (distance < nearestLinkedDistance) {
                    nearestLinkedDistance = distance;
                    nearestLinked = boss;
                }
            }
            if (allowAdoption && boss.canBindToAltar()) {
                if (distance < nearestBindableDistance) {
                    nearestBindableDistance = distance;
                    nearestBindable = boss;
                }
            }
        }
        if (nearestLinked != null) {
            return nearestLinked;
        }
        if (nearestBindable != null && nearestBindable.bindToAltar(arenaId, worldPosition)) {
            return nearestBindable;
        }
        return null;
    }

    private void adoptBoss(ServerLevel level, WorldEcho boss) {
        boolean changed = state != TransactionState.ACTIVE || !boss.getUUID().equals(bossUuid);
        state = TransactionState.ACTIVE;
        bossUuid = boss.getUUID();
        missingBossSince = -1L;
        if (changed) {
            stateChangedAt = level.getGameTime();
            setChanged();
        }
    }

    private void resetToIdle(long now) {
        state = TransactionState.IDLE;
        bossUuid = null;
        summonerUuid = null;
        cooldownUntil = 0L;
        stateChangedAt = now;
        missingBossSince = -1L;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("ArenaId", arenaId);
        tag.putString("State", state.name());
        if (bossUuid != null) {
            tag.putUUID("BossUuid", bossUuid);
        }
        if (summonerUuid != null) {
            tag.putUUID("SummonerUuid", summonerUuid);
        }
        tag.putLong("CooldownUntil", cooldownUntil);
        tag.putLong("StateChangedAt", stateChangedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        arenaId = tag.hasUUID("ArenaId") ? tag.getUUID("ArenaId") : UUID.randomUUID();
        state = readState(tag);
        bossUuid = tag.hasUUID("BossUuid") ? tag.getUUID("BossUuid") : null;
        summonerUuid = tag.hasUUID("SummonerUuid") ? tag.getUUID("SummonerUuid") : null;
        cooldownUntil = Math.max(0L, tag.getLong("CooldownUntil"));
        stateChangedAt = Math.max(0L, tag.getLong("StateChangedAt"));
        missingBossSince = -1L;
    }

    private static TransactionState readState(CompoundTag tag) {
        if (!tag.contains("State", Tag.TAG_STRING)) {
            return TransactionState.IDLE;
        }
        try {
            return TransactionState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException ignored) {
            return TransactionState.IDLE;
        }
    }

    private static boolean elapsed(long now, long start, long duration) {
        return now < start || now - start >= duration;
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    private enum TransactionState {
        IDLE,
        STARTING,
        ACTIVE,
        SETTLING,
        COOLDOWN
    }
}
