package cn.kurt6.unknown_echoes.world;

import cn.kurt6.unknown_echoes.entity.boss.BossParticleTheme;
import cn.kurt6.unknown_echoes.fx.BossLifecycleFx;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.tide.TideCoreAltarBlock;
import cn.kurt6.unknown_echoes.block.truesight.TrueSightAltarBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.entity.boss.RechallengeBoundBoss;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModStructures;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 三座能力祭坛的世界级复战状态；每个维度独立持久化活动 Boss 与共享冷却。 */
public final class MainBossRechallengeData extends SavedData {
    public static final int INITIAL_RECOVERY_INTERVAL_TICKS = 40;
    private static final String DATA_NAME = "unknown_echoes_main_boss_rechallenge";
    private static final SavedData.Factory<MainBossRechallengeData> FACTORY =
            new SavedData.Factory<>(MainBossRechallengeData::new, MainBossRechallengeData::load);

    private final Map<Long, Entry> entries = new HashMap<>();

    public static void trySummon(ServerPlayer player, BlockPos altarPos, BossKind kind) {
        ServerLevel level = player.serverLevel();
        ArenaContext arena = findArena(level, altarPos, kind);
        if (arena == null || !isAreaLoaded(level, arena.bounds())) {
            reject(player, altarPos, kind, "failed");
            return;
        }

        MainBossRechallengeData data = get(level);
        Entry entry = data.entry(altarPos, kind);
        if (entry.spawning) {
            return;
        }
        entry.spawning = true;
        try {
            RecoveryResult recovery = data.findActiveBoss(level, arena, altarPos, kind, entry);
            if (recovery.interrupted()) {
                return;
            }
            Mob activeBoss = recovery.boss();
            if (activeBoss != null) {
                if (activeBoss instanceof RechallengeBoundBoss boundBoss) {
                    boundBoss.bindRechallengeArena(altarPos, arena.spawnAnchor());
                }
                data.setActive(entry, activeBoss.getUUID());
                reject(player, altarPos, kind, "active");
                return;
            }
            if (entry.activeBossUuid != null) {
                // 场地方块与实体加载均完成后，才可把仍查不到的 UUID 视作失效。
                data.clearStaleActiveForRetry(entry);
            }

            long remaining = data.remainingCooldown(level, entry);
            if (remaining > 0L) {
                reject(player, altarPos, kind, "cooldown", (remaining + 19L) / 20L);
                return;
            }

            BlockState altarState = getReadyAltarState(level, altarPos);
            if (altarState == null || !kind.isAltar(altarState) || player.level() != level || player.isSpectator()) {
                reject(player, altarPos, kind, "failed");
                return;
            }
            Mob boss = createBoss(level, kind);
            if (boss == null) {
                reject(player, altarPos, kind, "failed");
                return;
            }
            boolean reserved = false;
            boolean spawned = false;
            try {
                BlockPos spawnPos = findSpawnPos(level, boss, arena.spawnAnchor(), arena.bounds());
                if (spawnPos == null) {
                    reject(player, altarPos, kind, "failed");
                    return;
                }
                boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        player.getYRot() + 180.0F, 0.0F);
                boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null);
                boss.setPersistenceRequired();
                ((RechallengeBoundBoss) boss).bindRechallengeArena(altarPos, arena.spawnAnchor());
                if (!isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)
                        || !EntitySpawnSafety.isLoadedAndCollisionFree(level, boss)
                        || !isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)) {
                    reject(player, altarPos, kind, "failed");
                    return;
                }
                data.setActive(entry, boss.getUUID());
                reserved = true;
                if (!level.addFreshEntity(boss)) {
                    reject(player, altarPos, kind, "failed");
                    return;
                }
                if (!boss.isAlive() || level.getEntity(boss.getUUID()) != boss
                        || !isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)
                        || data.entries.get(altarPos.asLong()) != entry
                        || !boss.getUUID().equals(entry.activeBossUuid) || player.level() != level || player.isSpectator()) {
                    reject(player, altarPos, kind, "failed");
                    return;
                }
                spawned = true;
                BossLifecycleFx.summon(level, boss.position(), boss.getBbWidth(), boss.getBbHeight(), switch (kind) {
                    case FORGOTTEN_COLOSSUS -> BossParticleTheme.COLOSSUS.body();
                    case ABYSS_WATCHER -> BossParticleTheme.ABYSS.body();
                    case MIRROR_GUARDIAN -> BossParticleTheme.MIRROR.body();
                });
                level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.HOSTILE, 1.8F, 0.75F);
            } finally {
                if (!spawned) {
                    try {
                        if (reserved) {
                            data.clearActiveIfOwned(entry, boss.getUUID());
                        }
                    } finally {
                        // 先回滚本次归属，再销毁实体；外层保护持续覆盖移除回调。
                        boss.discard();
                    }
                }
            }
        } finally {
            entry.spawning = false;
        }
    }

    /** 进入已生成的主线场地时补救旧结构中漏掉的首次 Boss。 */
    public static void ensureInitialBossForPlayer(ServerPlayer player, BossKind kind) {
        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        StructureStart start = LoadedStructureLookup.loadedStructureStartAt(
                level, player.blockPosition(), kind::matchesStructure);
        if (start == null) {
            return;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (!piece.getBoundingBox().isInside(player.blockPosition())) {
                continue;
            }
            BlockPos altarPos = transform(piece, kind.altarX, kind.altarY, kind.altarZ);
            BlockState altarState = getReadyAltarState(level, altarPos);
            if (altarState == null || !kind.isAltar(altarState)) {
                continue;
            }
            ensureInitialBoss(level, start, altarPos, kind,
                    transform(piece, kind.bossX, kind.bossY, kind.bossZ),
                    piece.getBoundingBox());
            return;
        }
    }

    private static void ensureInitialBoss(ServerLevel level, StructureStart start,
                                           BlockPos altarPos, BossKind kind,
                                           BlockPos spawnAnchor, BoundingBox structureBounds) {
        if (!isAreaLoaded(level, structureBounds)) {
            return;
        }
        MainBossRechallengeData data = get(level);
        Entry entry = data.entries.get(altarPos.asLong());
        if (entry == null) {
            entry = data.entry(altarPos, kind);
        }
        if (entry.spawning) {
            return;
        }
        entry.spawning = true;
        try {
            long now = level.getGameTime();
            if (entry.lastInitialRecoveryTick != Long.MIN_VALUE && now >= entry.lastInitialRecoveryTick
                    && now - entry.lastInitialRecoveryTick < INITIAL_RECOVERY_INTERVAL_TICKS) {
                return;
            }
            entry.lastInitialRecoveryTick = now;

            ArenaContext arena = new ArenaContext(start, spawnAnchor, structureBounds);
            RecoveryResult recovery = data.findActiveBoss(level, arena, altarPos, kind, entry);
            if (recovery.interrupted()) {
                return;
            }
            Mob activeBoss = recovery.boss();
            if (activeBoss != null) {
                data.setActive(entry, activeBoss.getUUID());
                return;
            }
            // 不能在这里判定 active UUID 失效:Boss 离开包围盒或区块不可查询时 findActiveBoss
            // 也返回 null,清掉绑定会让活着的 Boss 自我 discard 并再补一只。
            if (entry.activeBossUuid != null || entry.ownershipInitialized) {
                return;
            }

            BlockState altarState = getReadyAltarState(level, altarPos);
            if (altarState == null || !kind.isAltar(altarState)) {
                return;
            }
            Mob boss = createBoss(level, kind);
            if (boss == null) {
                return;
            }
            boolean reserved = false;
            boolean spawned = false;
            try {
                BlockPos spawnPos = findSpawnPos(level, boss, spawnAnchor, arena.bounds());
                if (spawnPos == null) {
                    return;
                }
                boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        0.0F, 0.0F);
                boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null);
                boss.setPersistenceRequired();
                if (!(boss instanceof RechallengeBoundBoss boundBoss)) {
                    return;
                }
                boundBoss.bindRechallengeArena(altarPos, spawnAnchor);
                if (!isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)
                        || !EntitySpawnSafety.isLoadedAndCollisionFree(level, boss)
                        || !isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)) {
                    return;
                }
                data.setActive(entry, boss.getUUID());
                reserved = true;
                if (!level.addFreshEntity(boss)) {
                    return;
                }
                if (!boss.isAlive() || level.getEntity(boss.getUUID()) != boss
                        || !isAreaLoaded(level, arena.bounds()) || !isCurrentAltar(level, altarPos, altarState)
                        || data.entries.get(altarPos.asLong()) != entry
                        || !boss.getUUID().equals(entry.activeBossUuid)) {
                    return;
                }
                spawned = true;
                BossLifecycleFx.summon(level, boss.position(), boss.getBbWidth(), boss.getBbHeight(), switch (kind) {
                    case FORGOTTEN_COLOSSUS -> BossParticleTheme.COLOSSUS.body();
                    case ABYSS_WATCHER -> BossParticleTheme.ABYSS.body();
                    case MIRROR_GUARDIAN -> BossParticleTheme.MIRROR.body();
                });
            } finally {
                if (!spawned) {
                    try {
                        if (reserved) {
                            data.rollbackInitialSpawn(entry, boss.getUUID());
                        }
                    } finally {
                        // 先回滚本次归属，再销毁实体；外层保护持续覆盖移除回调。
                        boss.discard();
                    }
                }
            }
        } finally {
            entry.spawning = false;
        }
    }

    public static void onBossDefeated(ServerLevel level, BlockPos altarPos, BossKind kind,
                                      UUID defeatedBossUuid) {
        MainBossRechallengeData data = get(level);
        Entry entry = data.entry(altarPos, kind);
        if (!defeatedBossUuid.equals(entry.activeBossUuid)) {
            return;
        }
        entry.activeBossUuid = null;
        entry.ownershipInitialized = true;
        long duration = Math.max(0L, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get());
        entry.cooldownUntil = duration == 0L ? 0L : safeDeadline(level.getGameTime(), duration);
        data.setDirty();
    }

    public static boolean hasActiveBinding(ServerLevel level, BlockPos altarPos,
                                            BossKind kind, UUID bossUuid) {
        Entry entry = get(level).entries.get(altarPos.asLong());
        return entry != null && entry.kind == kind && bossUuid.equals(entry.activeBossUuid);
    }

    /** 实体真正加入活动世界后登记，覆盖结构初次生成与区块重载两条路径。 */
    public static boolean onBossAdded(ServerLevel level, Entity boss) {
        if (!(boss instanceof RechallengeBoundBoss boundBoss)) {
            return true;
        }
        BlockPos altarPos = boundBoss.rechallengeAltar();
        BossKind kind = boundBoss.rechallengeKind();
        if (altarPos == null || !kind.isRealBoss(boss)) {
            return true;
        }
        MainBossRechallengeData data = get(level);
        Entry entry = data.entries.get(altarPos.asLong());
        if (entry == null) {
            entry = data.entry(altarPos, kind);
            data.setActive(entry, boss.getUUID());
            return true;
        }
        if (entry.kind != kind) {
            return false;
        }
        if (entry.activeBossUuid == null && !entry.ownershipInitialized) {
            // 结构初次生成可能先创建了空记录,此时仍允许结构里的首只 Boss 认领祭坛。
            data.setActive(entry, boss.getUUID());
            return true;
        }
        return boss.getUUID().equals(entry.activeBossUuid);
    }

    /** 仅在实体被真正销毁且 UUID 仍匹配时解锁；区块卸载不得清除活动状态。 */
    public static void onBossDestroyed(ServerLevel level, Entity boss) {
        if (!(boss instanceof RechallengeBoundBoss boundBoss)) {
            return;
        }
        BlockPos altarPos = boundBoss.rechallengeAltar();
        BossKind kind = boundBoss.rechallengeKind();
        if (altarPos == null || !kind.isRealBoss(boss)) {
            return;
        }
        MainBossRechallengeData data = get(level);
        Entry entry = data.entries.get(altarPos.asLong());
        if (entry != null && entry.kind == kind && boss.getUUID().equals(entry.activeBossUuid)) {
            entry.activeBossUuid = null;
            entry.ownershipInitialized = true;
            data.setDirty();
        }
    }

    private static MainBossRechallengeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private Entry entry(BlockPos altarPos, BossKind kind) {
        long key = altarPos.asLong();
        Entry entry = entries.get(key);
        if (entry == null || entry.kind != kind) {
            entry = new Entry(kind);
            entries.put(key, entry);
            setDirty();
        }
        return entry;
    }

    private void setActive(Entry entry, UUID uuid) {
        if (!uuid.equals(entry.activeBossUuid) || !entry.ownershipInitialized) {
            entry.activeBossUuid = uuid;
            entry.ownershipInitialized = true;
            setDirty();
        }
    }

    private void clearActiveIfOwned(Entry entry, UUID uuid) {
        if (uuid.equals(entry.activeBossUuid)) {
            entry.activeBossUuid = null;
            entry.ownershipInitialized = true;
            setDirty();
        }
    }

    private void rollbackInitialSpawn(Entry entry, UUID uuid) {
        if (uuid.equals(entry.activeBossUuid)) {
            entry.activeBossUuid = null;
            entry.ownershipInitialized = false;
            setDirty();
        }
    }

    /**
     * 只清失效的 active UUID,不动 ownershipInitialized:一起清会把"已击败/已认领"退化成
     * "首次生成未完成",于是给一只仍活着但暂时查不到的 Boss 之外再补一只。
     */
    private void clearStaleActiveForRetry(Entry entry) {
        if (entry.activeBossUuid != null) {
            entry.activeBossUuid = null;
            setDirty();
        }
    }

    private long remainingCooldown(ServerLevel level, Entry entry) {
        long now = level.getGameTime();
        long maximum = safeDeadline(now,
                Math.max(0L, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get()));
        if (entry.cooldownUntil > maximum) {
            entry.cooldownUntil = maximum;
            setDirty();
        }
        if (now < entry.cooldownUntil) {
            return entry.cooldownUntil - now;
        }
        if (entry.cooldownUntil != 0L) {
            entry.cooldownUntil = 0L;
            setDirty();
        }
        return 0L;
    }

    @Nullable
    private static ArenaContext findArena(ServerLevel level, BlockPos altarPos, BossKind kind) {
        BlockState altarState = getReadyAltarState(level, altarPos);
        if (altarState == null || !kind.isAltar(altarState)) {
            return null;
        }
        StructureStart start = LoadedStructureLookup.loadedStructureStartAt(level, altarPos, kind::matchesStructure);
        if (start == null) {
            return null;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (!piece.getBoundingBox().isInside(altarPos)) {
                continue;
            }
            BlockPos expectedAltar = transform(piece, kind.altarX, kind.altarY, kind.altarZ);
            if (expectedAltar.equals(altarPos)) {
                return new ArenaContext(start, transform(piece, kind.bossX, kind.bossY, kind.bossZ),
                        piece.getBoundingBox());
            }
        }
        return null;
    }

    private RecoveryResult findActiveBoss(ServerLevel level, ArenaContext arena, BlockPos altarPos,
                                          BossKind kind, Entry entry) {
        if (entry.kind != kind) {
            return new RecoveryResult(null, true);
        }
        BlockState altarState = getReadyAltarState(level, altarPos);
        if (altarState == null || !kind.isAltar(altarState)) {
            return new RecoveryResult(null, true);
        }
        RecoveryContext context = new RecoveryContext(entry, entry.activeBossUuid,
                entry.ownershipInitialized, entry.cooldownUntil, altarState);
        if (!isRecoveryCurrent(level, arena, altarPos, context)) {
            return new RecoveryResult(null, true);
        }

        Mob owner = null;
        boolean ownerFromIndex = false;
        if (context.activeUuid() != null && level.getEntity(context.activeUuid()) instanceof Mob knownBoss
                && isRecoveryCandidate(level, altarPos, kind, knownBoss)) {
            // An indexed owner may have left the original structure bounds while still owning this arena.
            owner = knownBoss;
            ownerFromIndex = true;
        }
        BoundingBox box = arena.bounds();
        AABB bounds = new AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1.0D, box.maxY() + 1.0D, box.maxZ() + 1.0D);
        var candidates = level.getEntities((Entity) null, bounds,
                entity -> kind.isRealBoss(entity) && hasExactBinding(entity, altarPos));
        if (!isRecoveryCurrent(level, arena, altarPos, context)) {
            return new RecoveryResult(null, true);
        }
        for (Entity entity : candidates) {
            if (entity instanceof Mob boss && isRecoveryCandidate(level, altarPos, kind, boss)
                    && bounds.intersects(boss.getBoundingBox())) {
                if (owner == null && (context.activeUuid() != null && context.activeUuid().equals(boss.getUUID())
                        || context.activeUuid() == null && !context.ownershipInitialized())) {
                    owner = boss;
                }
            }
        }
        for (Entity entity : candidates) {
            // Removing a duplicate can synchronously change the remaining candidates or the arena owner.
            if (!isRecoveryCurrent(level, arena, altarPos, context)
                    || owner != null && (!isRecoveryCandidate(level, altarPos, kind, owner)
                    || !ownerFromIndex && !bounds.intersects(owner.getBoundingBox()))) {
                return new RecoveryResult(null, true);
            }
            if (entity instanceof Mob boss && boss != owner
                    && isRecoveryCandidate(level, altarPos, kind, boss)
                    && bounds.intersects(boss.getBoundingBox())) {
                boss.discard();
            }
        }
        if (!isRecoveryCurrent(level, arena, altarPos, context)
                || owner != null && (!isRecoveryCandidate(level, altarPos, kind, owner)
                || !ownerFromIndex && !bounds.intersects(owner.getBoundingBox()))) {
            return new RecoveryResult(null, true);
        }
        return new RecoveryResult(owner, false);
    }

    private boolean isRecoveryCurrent(ServerLevel level, ArenaContext arena, BlockPos altarPos,
                                      RecoveryContext context) {
        return isAreaLoaded(level, arena.bounds()) && isCurrentAltar(level, altarPos, context.altarState())
                && entries.get(altarPos.asLong()) == context.entry()
                && java.util.Objects.equals(context.entry().activeBossUuid, context.activeUuid())
                && context.entry().ownershipInitialized == context.ownershipInitialized()
                && context.entry().cooldownUntil == context.cooldownUntil();
    }

    private static boolean isRecoveryCandidate(ServerLevel level, BlockPos altarPos, BossKind kind, Mob boss) {
        return boss.level() == level && boss.isAlive()
                && level.getEntity(boss.getUUID()) == boss
                && kind.isRealBoss(boss) && hasExactBinding(boss, altarPos);
    }

    private static boolean hasExactBinding(Entity entity, BlockPos altarPos) {
        return entity instanceof RechallengeBoundBoss boundBoss
                && altarPos.equals(boundBoss.rechallengeAltar());
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, Mob boss, BlockPos anchor, BoundingBox arena) {
        int[][] offsets = {
                {0, 0}, {0, 4}, {4, 0}, {0, -4}, {-4, 0},
                {4, 4}, {4, -4}, {-4, 4}, {-4, -4}, {0, 7}, {7, 0}, {0, -7}, {-7, 0}
        };
        int[] verticalOffsets = {0, 1, 2, -1};
        for (int[] offset : offsets) {
            for (int dy : verticalOffsets) {
                BlockPos candidate = anchor.offset(offset[0], dy, offset[1]);
                if (!arena.isInside(candidate)) {
                    continue;
                }
                boss.moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                        0.0F, 0.0F);
                if (EntitySpawnSafety.isLoadedAndCollisionFree(level, boss)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isAreaLoaded(ServerLevel level, BoundingBox box) {
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null
                        || !level.areEntitiesLoaded(ChunkPos.asLong(chunkX, chunkZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    private static BlockState getReadyAltarState(ServerLevel level, BlockPos altarPos) {
        if (level.isOutsideBuildHeight(altarPos)) {
            return null;
        }
        var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(altarPos);
    }

    private static boolean isCurrentAltar(ServerLevel level, BlockPos altarPos, BlockState state) {
        return getReadyAltarState(level, altarPos) == state;
    }

    private static BlockPos transform(StructurePiece piece, int localX, int localY, int localZ) {
        BoundingBox box = piece.getBoundingBox();
        Direction direction = piece.getOrientation();
        if (direction == null) {
            return new BlockPos(localX, localY, localZ);
        }
        int worldX = switch (direction) {
            case NORTH, SOUTH -> box.minX() + localX;
            case WEST -> box.maxX() - localZ;
            case EAST -> box.minX() + localZ;
            default -> localX;
        };
        int worldZ = switch (direction) {
            case NORTH -> box.maxZ() - localZ;
            case SOUTH -> box.minZ() + localZ;
            case WEST, EAST -> box.minZ() + localX;
            default -> localZ;
        };
        return new BlockPos(worldX, box.minY() + localY, worldZ);
    }

    @Nullable
    private static Mob createBoss(ServerLevel level, BossKind kind) {
        return switch (kind) {
            case FORGOTTEN_COLOSSUS -> ModEntities.FORGOTTEN_COLOSSUS.get().create(level);
            case ABYSS_WATCHER -> ModEntities.ABYSS_WATCHER.get().create(level);
            case MIRROR_GUARDIAN -> ModEntities.MIRROR_GUARDIAN.get().create(level);
        };
    }

    private static void reject(ServerPlayer player, BlockPos altarPos, BossKind kind,
                               String suffix, Object... args) {
        player.serverLevel().playSound(null, altarPos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, suffix.equals("cooldown") ? 0.7F : 0.5F);
        player.displayClientMessage(Component.translatable(kind.messagePrefix + suffix, args), true);
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    private static MainBossRechallengeData load(CompoundTag tag, HolderLookup.Provider registries) {
        MainBossRechallengeData data = new MainBossRechallengeData();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BossKind kind = BossKind.byName(entryTag.getString("Kind"));
            if (kind == null || !entryTag.contains("Altar", Tag.TAG_LONG)) {
                continue;
            }
            Entry entry = new Entry(kind);
            if (entryTag.hasUUID("ActiveBoss")) {
                entry.activeBossUuid = entryTag.getUUID("ActiveBoss");
            }
            entry.cooldownUntil = Math.max(0L, entryTag.getLong("CooldownUntil"));
            entry.ownershipInitialized = entryTag.getBoolean("OwnershipInitialized")
                    || entry.activeBossUuid != null || entry.cooldownUntil > 0L
                    || !entryTag.contains("OwnershipInitialized", Tag.TAG_BYTE);
            data.entries.put(entryTag.getLong("Altar"), entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> mapEntry : entries.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Altar", mapEntry.getKey());
            entryTag.putString("Kind", mapEntry.getValue().kind.serializedName);
            if (mapEntry.getValue().activeBossUuid != null) {
                entryTag.putUUID("ActiveBoss", mapEntry.getValue().activeBossUuid);
            }
            entryTag.putLong("CooldownUntil", mapEntry.getValue().cooldownUntil);
            entryTag.putBoolean("OwnershipInitialized", mapEntry.getValue().ownershipInitialized);
            list.add(entryTag);
        }
        tag.put("Entries", list);
        return tag;
    }

    public enum BossKind {
        FORGOTTEN_COLOSSUS("forgotten_colossus", "message.unknown_echoes.colossus_rechallenge.",
                36, 2, 28, 36, 2, 36),
        ABYSS_WATCHER("abyss_watcher", "message.unknown_echoes.watcher_rechallenge.",
                32, 2, 32, 32, 4, 48),
        MIRROR_GUARDIAN("mirror_guardian", "message.unknown_echoes.guardian_rechallenge.",
                24, 2, 45, 24, 3, 30);

        private final String serializedName;
        private final String messagePrefix;
        private final int altarX;
        private final int altarY;
        private final int altarZ;
        private final int bossX;
        private final int bossY;
        private final int bossZ;

        BossKind(String serializedName, String messagePrefix,
                 int altarX, int altarY, int altarZ, int bossX, int bossY, int bossZ) {
            this.serializedName = serializedName;
            this.messagePrefix = messagePrefix;
            this.altarX = altarX;
            this.altarY = altarY;
            this.altarZ = altarZ;
            this.bossX = bossX;
            this.bossY = bossY;
            this.bossZ = bossZ;
        }

        private boolean matchesStructure(Structure structure) {
            return switch (this) {
                case FORGOTTEN_COLOSSUS -> structure.type() == ModStructures.MEMORY_PILLAR_COURTYARD.get();
                case ABYSS_WATCHER -> structure.type() == ModStructures.SUNKEN_TEMPLE.get();
                case MIRROR_GUARDIAN -> structure.type() == ModStructures.MIRROR_TEMPLE.get();
            };
        }

        private boolean isAltar(BlockState state) {
            return switch (this) {
                case FORGOTTEN_COLOSSUS -> state.getBlock() instanceof RewardAltarBlock;
                case ABYSS_WATCHER -> state.getBlock() instanceof TideCoreAltarBlock;
                case MIRROR_GUARDIAN -> state.getBlock() instanceof TrueSightAltarBlock;
            };
        }

        public boolean isAltarState(BlockState state) {
            return isAltar(state);
        }

        public boolean isRealBossEntity(Entity entity) {
            return isRealBoss(entity);
        }

        private boolean isRealBoss(Entity entity) {
            return switch (this) {
                case FORGOTTEN_COLOSSUS -> entity instanceof ForgottenColossus;
                case ABYSS_WATCHER -> entity instanceof AbyssWatcher watcher && !watcher.isClone();
                case MIRROR_GUARDIAN -> entity instanceof MirrorGuardian guardian && !guardian.isIllusion();
            };
        }

        @Nullable
        private static BossKind byName(String name) {
            for (BossKind kind : values()) {
                if (kind.serializedName.equals(name)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static final class Entry {
        private final BossKind kind;
        @Nullable
        private UUID activeBossUuid;
        private long cooldownUntil;
        private boolean ownershipInitialized;
        // 只保护当前同步调用，不写入存档；首次补生与主动复战共用。
        private boolean spawning;
        private long lastInitialRecoveryTick = Long.MIN_VALUE;

        private Entry(BossKind kind) {
            this.kind = kind;
        }
    }

    private record RecoveryContext(Entry entry, @Nullable UUID activeUuid, boolean ownershipInitialized,
                                   long cooldownUntil, BlockState altarState) {
    }

    private record RecoveryResult(@Nullable Mob boss, boolean interrupted) {
    }

    private record ArenaContext(StructureStart start, BlockPos spawnAnchor, BoundingBox bounds) {
    }
}
