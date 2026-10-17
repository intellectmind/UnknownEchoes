package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** 世界共鸣祭坛的持久召唤事务，负责防重复召唤和服务器重启后的状态恢复。 */
public class WorldResonanceAltarBlockEntity extends BlockEntity {
    private static final int BOSS_RANGE_H = 48;
    private static final int BOSS_RANGE_V = 40;
    private static final long MISSING_BOSS_GRACE_TICKS = 100L;
    private static final long SETTLING_TICKS = 80L;
    private static final long RESONANCE_LOCK_TICKS = 14L;
    private static final int ALL_RESONANCES = (1 << EchoAbilityType.values().length) - 1;
    private static final int[] SAFE_EXIT_Y_OFFSETS = {0, 1, -1, 2, -2, 3, -3};
    private static final List<Vec3> LOCAL_RESONATORS = List.of(
            new Vec3(0.5D, 1.12D, -0.19D),
            new Vec3(1.25D, 1.12D, 0.31D),
            new Vec3(1.06D, 1.12D, 1.12D),
            new Vec3(-0.06D, 1.12D, 1.12D),
            new Vec3(-0.25D, 1.12D, 0.31D));

    private UUID arenaId = UUID.randomUUID();
    private TransactionState state = TransactionState.IDLE;
    @Nullable
    private UUID bossUuid;
    @Nullable
    private UUID summonerUuid;
    private boolean adminTestMode;
    /** 覆盖当前同步调谐、启动或完成召唤的调用，不写入祭坛事务存档。 */
    private boolean completingSummon;
    private boolean recoveringBosses;
    private long cooldownUntil;
    private long stateChangedAt;
    private long missingBossSince = -1L;
    private int resonanceMask;
    private int lastResonanceIndex = -1;
    private long lastResonanceAt = -1L;
    @Nullable
    private ServerBossEvent ritualEvent;
    @Nullable
    private List<Vec3> cachedPylonTops;
    @Nullable
    private Direction cachedPylonFacing;
    private boolean cachedPylonGrand;

    public WorldResonanceAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORLD_RESONANCE_ALTAR.get(), pos, state);
    }

    public UUID getArenaId() {
        return arenaId;
    }

    public boolean isRitualInProgress() {
        return state == TransactionState.STARTING;
    }

    public boolean isAdminTestMode() {
        return adminTestMode;
    }

    public boolean isFullyResonant() {
        return resonanceMask == ALL_RESONANCES;
    }

    public boolean hasResonance(EchoAbilityType ability) {
        return (resonanceMask & bit(ability)) != 0;
    }

    public static Vec3 localResonatorOffset(EchoAbilityType ability) {
        return LOCAL_RESONATORS.get(ability.ordinal());
    }

    public int getResonanceCount() {
        return Integer.bitCount(resonanceMask);
    }

    public int getLastResonanceIndex() {
        return lastResonanceIndex;
    }

    public long getLastResonanceAt() {
        return lastResonanceAt;
    }

    public long getStateChangedAt() {
        return stateChangedAt;
    }

    public boolean isResonanceLocked(long gameTime) {
        return lastResonanceAt >= 0L && gameTime >= lastResonanceAt
                && gameTime - lastResonanceAt < RESONANCE_LOCK_TICKS;
    }

    @Nullable
    public EchoAbilityType resonateNext(ServerLevel level) {
        if (completingSummon) {
            return null;
        }
        if (!isCurrentAltar(level)) {
            return null;
        }
        completingSummon = true;
        try {
            reconcile(level);
            long now = level.getGameTime();
            if (!isCurrentAltar(level) || state != TransactionState.IDLE || isFullyResonant() || isResonanceLocked(now)) {
                return null;
            }
            for (EchoAbilityType ability : EchoAbilityType.values()) {
                if (hasResonance(ability)) {
                    continue;
                }
                int index = ability.ordinal();
                resonanceMask |= bit(ability);
                lastResonanceIndex = index;
                lastResonanceAt = now;
                stateChangedAt = now;
                setChangedAndSync();
                if (isCurrentAltar(level) && state == TransactionState.IDLE) {
                    playResonanceFeedback(level, index);
                }
                return ability;
            }
            return null;
        } finally {
            completingSummon = false;
        }
    }

    public boolean isGrandArena() {
        return isGrandArena(getBlockState());
    }

    /** 返回五座主题回响柱的顶部中心，供正式 Boss 战的服务端表现层复用。 */
    public List<Vec3> resonancePylonTops() {
        BlockState state = getBlockState();
        Direction facing = facing(state);
        boolean grand = isGrandArena(state);
        if (cachedPylonTops == null || cachedPylonFacing != facing || cachedPylonGrand != grand) {
            int topOffset = WorldResonanceLayout.pylonTopOffset(grand);
            cachedPylonTops = WorldResonanceLayout.pylonPositions(worldPosition, facing, grand).stream()
                    .map(pos -> Vec3.atCenterOf(pos.above(topOffset)))
                    .toList();
            cachedPylonFacing = facing;
            cachedPylonGrand = grand;
        }
        return cachedPylonTops;
    }

    private Direction facing() {
        return facing(getBlockState());
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(WorldResonanceAltarBlock.FACING)
                ? state.getValue(WorldResonanceAltarBlock.FACING)
                : Direction.NORTH;
    }

    private static boolean isGrandArena(BlockState state) {
        return state.hasProperty(WorldResonanceAltarBlock.GRAND)
                && state.getValue(WorldResonanceAltarBlock.GRAND);
    }

    public boolean beginStarting(ServerLevel level, UUID summoner) {
        if (completingSummon) {
            return false;
        }
        return beginRitual(level, summoner, false);
    }

    /** 管理员测试入口：跳过正式能力门槛，但保留祭坛仪式、场地绑定和 Boss 事务。 */
    public boolean beginAdminTestStarting(ServerLevel level, UUID summoner) {
        if (completingSummon) {
            return false;
        }
        return beginRitual(level, summoner, true);
    }

    private boolean beginRitual(ServerLevel level, UUID summoner, boolean admin) {
        if (!isCurrentAltar(level)) {
            return false;
        }
        completingSummon = true;
        try {
            reconcile(level);
            if (!isCurrentAltar(level) || !isArenaChunksLoaded(level) || state != TransactionState.IDLE
                    || (!admin && !isFullyResonant())) {
                return false;
            }
            if (admin) {
                resonanceMask = ALL_RESONANCES;
                lastResonanceIndex = EchoAbilityType.values().length - 1;
                lastResonanceAt = level.getGameTime();
            }
            adminTestMode = admin;
            state = TransactionState.STARTING;
            bossUuid = null;
            summonerUuid = summoner;
            stateChangedAt = level.getGameTime();
            missingBossSince = -1L;
            setChangedAndSync();
            if (admin) {
                for (int index = 0; index < EchoAbilityType.values().length; index++) {
                    if (!isCurrentRitualStart(level, summoner)) {
                        return true;
                    }
                    playResonanceFeedback(level, index);
                }
            }
            if (isCurrentRitualStart(level, summoner)) {
                playRitualOpening(level);
                if (isCurrentRitualStart(level, summoner)) {
                    updateRitualBossBar(level, 0);
                }
            }
            return true;
        } finally {
            completingSummon = false;
        }
    }

    private boolean isCurrentRitualStart(ServerLevel level, UUID summoner) {
        var candidate = level.getPlayerByUUID(summoner);
        return state == TransactionState.STARTING && summoner.equals(summonerUuid)
                && candidate instanceof ServerPlayer player && isCurrentSummonContext(level, player);
    }

    public boolean isCurrentAltar(ServerLevel level) {
        if (isRemoved() || this.level != level || level.isOutsideBuildHeight(worldPosition)) {
            return false;
        }
        var chunk = level.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition) == this
                && chunk.getBlockState(worldPosition).getBlock() instanceof WorldResonanceAltarBlock;
    }

    @Nullable
    private BlockState getReadyAltarState(ServerLevel level) {
        return BossRechallengeAltarBlockEntity.getReadyAltarState(level, worldPosition);
    }

    public boolean beginSummonCompletion(ServerLevel level, ServerPlayer player) {
        if (completingSummon || state != TransactionState.STARTING || bossUuid != null
                || !isCurrentSummonContext(level, player)) {
            return false;
        }
        completingSummon = true;
        return true;
    }

    public void finishSummonCompletion() {
        completingSummon = false;
    }

    public boolean isCurrentSummonContext(ServerLevel level, ServerPlayer player) {
        return isCurrentAltar(level) && isArenaChunksLoaded(level)
                && isQualifiedNearby(player, WorldResonanceLayout.arenaRadius(isGrandArena()) + 8.0D);
    }

    public boolean markActive(WorldEcho boss) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (state == TransactionState.ACTIVE && bossUuid != null
                && bossUuid.equals(boss.getUUID())
                && boss.matchesAltar(arenaId, worldPosition)) {
            return true;
        }
        if (state != TransactionState.STARTING) {
            return false;
        }
        if (bossUuid == null || !bossUuid.equals(boss.getUUID())
                || !boss.matchesAltar(arenaId, worldPosition)) {
            return false;
        }
        state = TransactionState.ACTIVE;
        stateChangedAt = serverLevel.getGameTime();
        missingBossSince = -1L;
        clearRitualBossBar();
        setChangedAndSync();
        return true;
    }

    public boolean reserveStartingBoss(WorldEcho boss) {
        if (state != TransactionState.STARTING || bossUuid != null
                || !boss.bindToAltar(arenaId, worldPosition)) {
            return false;
        }
        bossUuid = boss.getUUID();
        setChangedAndSync();
        return true;
    }

    /** 绑定 Boss 加载时抢占精确 arena 所有权；同 arena 的后加载重复体必须退出。 */
    public boolean registerLoadedBoss(WorldEcho boss) {
        if (!(level instanceof ServerLevel serverLevel)
                || !boss.matchesAltar(arenaId, worldPosition)) {
            return false;
        }
        if (bossUuid == null || !bossUuid.equals(boss.getUUID())) {
            return false;
        }
        if (state == TransactionState.STARTING) {
            adoptBoss(serverLevel, boss);
            return true;
        }
        return state == TransactionState.ACTIVE;
    }

    public void abortStarting() {

        if (state != TransactionState.STARTING || bossUuid != null) {
            return;
        }
        state = TransactionState.IDLE;
        bossUuid = null;
        summonerUuid = null;
        adminTestMode = false;
        missingBossSince = -1L;
        stateChangedAt = level == null ? stateChangedAt : level.getGameTime();
        resetResonances();
        clearRitualBossBar();
        setChangedAndSync();
    }

    /**
     * 回滚一次具体 Boss 的召唤事务。实体加入世界时可能同步触发 registerLoadedBoss,
     * 所以失败时状态可能已是 ACTIVE;UUID 校验防止迟到的失败回调清理掉召唤。
     */
    public void abortSummon(UUID failedBossUuid) {
        if (failedBossUuid == null || bossUuid == null
                || !failedBossUuid.equals(bossUuid)
                || (state != TransactionState.STARTING && state != TransactionState.ACTIVE)) {
            return;
        }
        long now = level == null ? stateChangedAt : level.getGameTime();
        resetToIdle(now);
    }

    @Nullable
    public WorldEcho findActiveBoss(ServerLevel level) {
        if (!completingSummon) {
            reconcile(level);
        }
        return findLinkedBoss(level, false).boss();
    }

    public void onBossSettled(UUID settledArenaId, UUID settledBossUuid) {
        if (!(level instanceof ServerLevel serverLevel)
                || !ownsActiveBoss(settledArenaId, settledBossUuid)) {
            return;
        }
        state = TransactionState.SETTLING;
        stateChangedAt = serverLevel.getGameTime();
        missingBossSince = -1L;
        setChangedAndSync();
    }

    public boolean ownsActiveBoss(UUID activeArenaId, UUID activeBossUuid) {
        return state == TransactionState.ACTIVE
                && arenaId.equals(activeArenaId)
                && bossUuid != null
                && bossUuid.equals(activeBossUuid);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WorldResonanceAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (altar.completingSummon) {
            return;
        }
        if (altar.recoveringBosses) {
            return;
        }
        if (altar.state == TransactionState.IDLE) {
            return;
        }
        if (altar.state == TransactionState.STARTING) {
            altar.tickRitual(serverLevel);
            return;
        }
        long now = level.getGameTime();
        WorldEcho checkedBoss = null;
        BossLookup checkedLookup = null;
        boolean checkedActiveBoss = false;
        if (altar.state == TransactionState.ACTIVE && now % 5L == 0L) {
            checkedActiveBoss = true;
            checkedLookup = altar.findLinkedBoss(serverLevel, now % 20L == 0L);
            if (checkedLookup.interrupted()) {
                return;
            }
            checkedBoss = checkedLookup.boss();
            if (checkedBoss != null) {
                altar.enforceArenaBoundary(serverLevel, checkedBoss);
            }
        }
        if (now % 20L == 0L) {
            if (checkedActiveBoss) {
                altar.reconcile(serverLevel, checkedLookup);
            } else {
                altar.reconcile(serverLevel);
            }
        }
    }

    private void playRitualOpening(ServerLevel level) {
        MechanismFx.ritualOpening(level, Vec3.atCenterOf(worldPosition));
        level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_OPEN.get(),
                SoundSource.BLOCKS, 2.0F, 1.0F);
    }

    private void playResonanceFeedback(ServerLevel level, int index) {
        Vec3 center = Vec3.atLowerCornerOf(worldPosition).add(LOCAL_RESONATORS.get(index));
        MechanismFx.resonanceNode(level, center, index);
        switch (index) {
            case 0 -> level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_WIND.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            case 1 -> level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_TIDE.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            case 2 -> level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_SIGHT.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            case 3 -> level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_GRAVITY.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            case 4 -> level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_VOID.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            default -> { }
        }
    }

    private void tickRitual(ServerLevel level) {
        if (!isCurrentAltar(level)) {
            return;
        }
        long now = level.getGameTime();
        if (now < stateChangedAt) {
            stateChangedAt = now;
            setChangedAndSync();
        }
        int age = (int) Math.min(Integer.MAX_VALUE, now - stateChangedAt);
        updateRitualBossBar(level, age);
        renderRitual(level, age);

        if (age == 45) {
            level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_CONNECT.get(),
                    SoundSource.BLOCKS, 2.0F, 1.0F);
        } else if (age == 105) {
            level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_FOCUS.get(),
                    SoundSource.BLOCKS, 2.0F, 1.0F);
        } else if (age == 150) {
            level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_READY.get(),
                    SoundSource.BLOCKS, 2.0F, 1.0F);
        }

        if (age < WorldResonanceLayout.RITUAL_DURATION_TICKS) {
            return;
        }
        if (bossUuid != null) {
            BossLookup reservedLookup = findLinkedBoss(level, false);
            if (reservedLookup.interrupted()) {
                return;
            }
            WorldEcho reservedBoss = reservedLookup.boss();
            if (reservedBoss != null) {
                adoptBoss(level, reservedBoss);
            } else if (isArenaChunksLoaded(level) && elapsed(now, stateChangedAt,
                    WorldResonanceLayout.RITUAL_DURATION_TICKS + MISSING_BOSS_GRACE_TICKS)) {
                UnknownEchoes.LOGGER.warn(
                        "World resonance altar {} recovered a reserved summon without a live boss",
                        worldPosition);
                resetToIdle(now);
            }
            return;
        }
        ServerPlayer leader = findQualifiedRitualLeader(level);
        if (leader == null) {
            collapseRitual(level);
            abortStarting();
            return;
        }
        WorldResonanceAltarBlock.completeSummon(level, worldPosition, leader, this);
    }

    private void renderRitual(ServerLevel level, int age) {
        MechanismFx.emit(level, MechanismFx.ritualFrame(Vec3.atCenterOf(worldPosition),
                resonancePylonTops(), age, WorldResonanceLayout.RITUAL_DURATION_TICKS));
    }

    @Nullable
    private ServerPlayer findQualifiedRitualLeader(ServerLevel level) {
        double radius = WorldResonanceLayout.arenaRadius(isGrandArena()) + 8.0D;
        var summonerCandidate = summonerUuid == null ? null : level.getPlayerByUUID(summonerUuid);
        ServerPlayer summoner = summonerCandidate instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (isQualifiedNearby(summoner, radius)) {
            return summoner;
        }
        if (adminTestMode) {
            return null;
        }
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : playersInRange(level, radius)) {
            double distance = horizontalDistanceSqr(player.position());
            if (isQualifiedRitualPlayer(player) && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isQualifiedNearby(@Nullable ServerPlayer player, double radius) {
        return isWithinPlayerRange(player, radius) && isQualifiedRitualPlayer(player);
    }

    private boolean isQualifiedRitualPlayer(@Nullable ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && player.level() == level
                && (isAdminTestPlayer(player) || EchoPermission.canSummonWorldEcho(player));
    }

    private boolean isAdminTestPlayer(@Nullable ServerPlayer player) {
        return adminTestMode && player != null && summonerUuid != null
                && summonerUuid.equals(player.getUUID());
    }

    private boolean isWithinPlayerRange(@Nullable ServerPlayer player, double radius) {
        return player != null
                && Math.abs(player.getY() - worldPosition.getY()) <= BOSS_RANGE_V
                && horizontalDistanceSqr(player.position()) <= radius * radius;
    }

    private List<ServerPlayer> playersInRange(ServerLevel level, double radius) {
        AABB bounds = new AABB(worldPosition).inflate(radius, BOSS_RANGE_V, radius);
        return level.getEntitiesOfClass(ServerPlayer.class, bounds,
                player -> isWithinPlayerRange(player, radius));
    }

    private void collapseRitual(ServerLevel level) {
        MechanismFx.ritualCollapse(level, Vec3.atCenterOf(worldPosition));
        level.playSound(null, worldPosition, ModSounds.BLOCK_WORLD_RESONANCE_COLLAPSE.get(),
                SoundSource.BLOCKS, 1.5F, 1.0F);
    }

    private void updateRitualBossBar(ServerLevel level, int age) {
        ServerBossEvent event = ritualEvent();
        float progress = Math.max(0.0F, Math.min(1.0F,
                age / (float) WorldResonanceLayout.RITUAL_DURATION_TICKS));
        event.setProgress(progress);
        Component title = Component.translatable(ritualStageTranslationKey(age));
        if (!title.equals(event.getName())) {
            event.setName(title);
        }
        if (age == 0 || age % 5 == 0) {
            syncRitualViewers(level, event);
        }
    }

    private ServerBossEvent ritualEvent() {
        if (ritualEvent == null) {
            ritualEvent = new ServerBossEvent(
                    Component.translatable("gui.unknown_echoes.world_resonance.ritual.awakening"),
                    BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
            ritualEvent.setDarkenScreen(false);
            ritualEvent.setCreateWorldFog(false);
        }
        return ritualEvent;
    }

    private void syncRitualViewers(ServerLevel level, ServerBossEvent event) {
        double radius = WorldResonanceLayout.arenaRadius(isGrandArena()) + 8.0D;
        for (ServerPlayer viewer : List.copyOf(event.getPlayers())) {
            if (!isQualifiedNearby(viewer, radius)) {
                event.removePlayer(viewer);
            }
        }
        for (ServerPlayer player : playersInRange(level, radius)) {
            if (isQualifiedRitualPlayer(player)) {
                event.addPlayer(player);
            }
        }
    }

    private void clearRitualBossBar() {
        if (ritualEvent != null) {
            ritualEvent.removeAllPlayers();
        }
    }

    private static String ritualStageTranslationKey(int age) {
        if (age < 45) {
            return "gui.unknown_echoes.world_resonance.ritual.awakening";
        }
        if (age < 105) {
            return "gui.unknown_echoes.world_resonance.ritual.converging";
        }
        if (age < 150) {
            return "gui.unknown_echoes.world_resonance.ritual.folding";
        }
        return "gui.unknown_echoes.world_resonance.ritual.manifesting";
    }

    private void enforceArenaBoundary(ServerLevel level, WorldEcho boss) {
        double radius = WorldResonanceLayout.arenaRadius(isGrandArena());
        for (ServerPlayer player : playersInRange(level, radius)) {
            if (player.isSpectator()) {
                continue;
            }
            if (isQualifiedRitualPlayer(player)) {
                boss.registerParticipant(player);
                continue;
            }
            ejectUnqualifiedPlayer(level, player, radius);
        }
    }

    private void ejectUnqualifiedPlayer(ServerLevel level, ServerPlayer player, double radius) {
        double dx = player.getX() - (worldPosition.getX() + 0.5D);
        double dz = player.getZ() - (worldPosition.getZ() + 0.5D);
        Vec3 outward = new Vec3(dx, 0.0D, dz);
        if (outward.lengthSqr() < 1.0E-4D) {
            outward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (outward.lengthSqr() < 1.0E-4D) {
            outward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        outward = outward.normalize();
        double distance = radius + 3.0D;
        BlockPos horizontal = BlockPos.containing(
                worldPosition.getX() + 0.5D + outward.x * distance,
                worldPosition.getY(),
                worldPosition.getZ() + 0.5D + outward.z * distance);
        BlockPos exit = findSafeExit(level, player, horizontal);
        if (exit == null) {
            player.setDeltaMovement(outward.x * 1.2D, 0.35D, outward.z * 1.2D);
            player.hurtMarked = true;
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.boundary_reject"), true);
            return;
        }
        Vec3 origin = player.position();
        player.teleportTo(level, exit.getX() + 0.5D, exit.getY(), exit.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + 1.0D, origin.z, 36,
                0.45D, 0.8D, 0.45D, 0.12D);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.world_resonance.boundary_reject"), true);
    }

    @Nullable
    private BlockPos findSafeExit(ServerLevel level, ServerPlayer player, BlockPos horizontal) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int offset : SAFE_EXIT_Y_OFFSETS) {
                        BlockPos candidate = horizontal.offset(dx, 0, dz)
                                .atY(worldPosition.getY() + offset);
                        if (!level.hasChunkAt(candidate)) {
                            continue;
                        }
                        BlockPos floor = candidate.below();
                        Vec3 target = Vec3.atBottomCenterOf(candidate);
                        AABB moved = player.getBoundingBox().move(target.subtract(player.position()));
                        if (level.getWorldBorder().isWithinBounds(moved)
                                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                                && level.noCollision(player, moved)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private double horizontalDistanceSqr(Vec3 position) {
        double dx = position.x - (worldPosition.getX() + 0.5D);
        double dz = position.z - (worldPosition.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private void reconcile(ServerLevel level) {
        reconcile(level, findLinkedBoss(level, true));
    }

    private void reconcile(ServerLevel level, BossLookup lookup) {
        if (!isLookupCurrent(level, lookup)) {
            return;
        }
        WorldEcho boss = lookup.boss();
        if (boss != null) {
            if (state != TransactionState.STARTING && state != TransactionState.ACTIVE) {
                boss.discard();
                if (!isRecoveryCurrent(level, lookup.context())) {
                    return;
                }
            } else {
                adoptBoss(level, boss);
                return;
            }
        }

        long now = level.getGameTime();
        switch (state) {
            case STARTING -> {
                if (elapsed(now, stateChangedAt,
                        WorldResonanceLayout.RITUAL_DURATION_TICKS + MISSING_BOSS_GRACE_TICKS)) {
                    UnknownEchoes.LOGGER.warn(
                            "World resonance altar {} recovered a stalled summoning ritual",
                            worldPosition);
                    resetToIdle(now);
                }
            }
            case ACTIVE -> {

                if (!isArenaChunksLoaded(level)) {
                    missingBossSince = -1L;
                    return;
                }
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
                    cooldownUntil = safeDeadline(now,
                            ServerConfig.WORLD_ECHO_RECHALLENGE_COOLDOWN_TICKS.get());
                    stateChangedAt = now;
                    setChangedAndSync();
                }
            }
            case COOLDOWN -> {
                long maximum = safeDeadline(now,
                        ServerConfig.WORLD_ECHO_RECHALLENGE_COOLDOWN_TICKS.get());
                if (cooldownUntil > maximum) {
                    cooldownUntil = maximum;
                    setChangedAndSync();
                }
                if (now < stateChangedAt || now >= cooldownUntil) {
                    resetToIdle(now);
                }
            }
            case IDLE -> missingBossSince = -1L;
        }
    }

    private boolean isArenaChunksLoaded(ServerLevel level) {
        return BossRechallengeAltarBlockEntity.areArenaChunksReady(level, worldPosition, BOSS_RANGE_H);
    }

    private BossLookup findLinkedBoss(ServerLevel level, boolean allowAdoption) {
        if (recoveringBosses || !isCurrentAltar(level)) {
            return new BossLookup(null, null, true, false);
        }
        recoveringBosses = true;
        try {
            RecoveryContext context = captureRecoveryContext(level);
            if (allowAdoption && !context.areaLoaded()) {
                // 未应用的旧实体仍可能拥有场地；不能按暂时为空的索引清理归属或重复体。
                missingBossSince = -1L;
                return new BossLookup(null, context, true, false);
            }
            if (bossUuid != null) {
                Entity entity = level.getEntity(bossUuid);
                if (entity instanceof WorldEcho boss && isRecoveryEntity(level, boss)) {
                    if (boss.matchesAltar(arenaId, worldPosition)) {
                        return new BossLookup(boss, context, false, true);
                    } else if (allowAdoption && state == TransactionState.ACTIVE && boss.canBindToAltar()) {
                        boss.bindToAltar(arenaId, worldPosition);
                        return isRecoveryCurrent(level, context) && isRecoveryCandidate(level, boss)
                                ? new BossLookup(boss, context, false, true)
                                : new BossLookup(null, null, true, false);
                    } else if (!allowAdoption) {
                        return new BossLookup(null, context, false, false);
                    } else {
                        UnknownEchoes.LOGGER.warn(
                                "World resonance altar {} rejected boss {} with a mismatched arena binding",
                                worldPosition, bossUuid);
                        bossUuid = null;
                        context = captureRecoveryContext(level);
                        setChangedAndSync();
                        if (!isRecoveryCurrent(level, context)) {
                            return new BossLookup(null, null, true, false);
                        }
                    }
                }
            }
            if (!allowAdoption) {
                return new BossLookup(null, context, false, false);
            }

            AABB arena = new AABB(worldPosition).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H);
            List<WorldEcho> nearby = level.getEntitiesOfClass(WorldEcho.class, arena,
                    boss -> boss.isAlive() && !boss.isRemoved());
            if (!isRecoveryCurrent(level, context)) {
                return new BossLookup(null, null, true, false);
            }
            WorldEcho owner = null;
            double ownerDistance = Double.MAX_VALUE;
            boolean allowLegacyAdoption = bossUuid == null && state == TransactionState.ACTIVE;
            for (WorldEcho boss : nearby) {
                if (!isRecoveryCandidate(level, boss) || !arena.intersects(boss.getBoundingBox())) {
                    continue;
                }
                double distance = boss.distanceToSqr(
                        worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D);
                boolean currentOwner = bossUuid != null && bossUuid.equals(boss.getUUID());
                boolean selectedOwner = owner != null && bossUuid != null
                        && bossUuid.equals(owner.getUUID());
                if ((currentOwner && !selectedOwner)
                        || (allowLegacyAdoption && owner == null)
                        || (allowLegacyAdoption && distance < ownerDistance)) {
                    owner = boss;
                    ownerDistance = distance;
                }
            }
            for (WorldEcho boss : nearby) {
                if (!isRecoveryCurrent(level, context)
                        || owner != null && (!isRecoveryCandidate(level, owner)
                        || !arena.intersects(owner.getBoundingBox()))) {
                    return new BossLookup(null, null, true, false);
                }
                if (boss != owner && isRecoveryCandidate(level, boss)
                        && arena.intersects(boss.getBoundingBox())) {
                    boss.discard();
                }
            }
            if (!isRecoveryCurrent(level, context)
                    || owner != null && (!isRecoveryCandidate(level, owner)
                    || !arena.intersects(owner.getBoundingBox()))) {
                return new BossLookup(null, null, true, false);
            }
            return new BossLookup(owner, context, false, false);
        } finally {
            recoveringBosses = false;
        }
    }

    private RecoveryContext captureRecoveryContext(ServerLevel level) {
        return new RecoveryContext(arenaId, bossUuid, state, stateChangedAt, cooldownUntil,
                resonanceMask, getReadyAltarState(level), isArenaChunksLoaded(level));
    }

    private boolean isRecoveryCurrent(ServerLevel level, RecoveryContext context) {
        return isCurrentAltar(level)
                && getReadyAltarState(level) == context.altarState()
                && arenaId.equals(context.arenaId())
                && java.util.Objects.equals(bossUuid, context.bossUuid())
                && state == context.state() && stateChangedAt == context.stateChangedAt()
                && cooldownUntil == context.cooldownUntil() && resonanceMask == context.resonanceMask()
                && isArenaChunksLoaded(level) == context.areaLoaded();
    }

    private boolean isRecoveryEntity(ServerLevel level, WorldEcho boss) {
        return boss.level() == level && boss.isAlive() && level.getEntity(boss.getUUID()) == boss;
    }

    private boolean isRecoveryCandidate(ServerLevel level, WorldEcho boss) {
        return isRecoveryEntity(level, boss) && boss.matchesAltar(arenaId, worldPosition);
    }

    private boolean isLookupCurrent(ServerLevel level, BossLookup lookup) {
        return !lookup.interrupted() && isRecoveryCurrent(level, lookup.context())
                && (lookup.boss() == null || isRecoveryCandidate(level, lookup.boss())
                && (lookup.indexedOwner() || new AABB(worldPosition).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H)
                .intersects(lookup.boss().getBoundingBox())));
    }

    private void adoptBoss(ServerLevel level, WorldEcho boss) {
        boolean changed = state != TransactionState.ACTIVE || !boss.getUUID().equals(bossUuid)
                || resonanceMask != ALL_RESONANCES;
        state = TransactionState.ACTIVE;
        bossUuid = boss.getUUID();
        resonanceMask = ALL_RESONANCES;
        missingBossSince = -1L;
        clearRitualBossBar();
        if (changed) {
            stateChangedAt = level.getGameTime();
            setChangedAndSync();
        }
    }

    private void resetToIdle(long now) {
        state = TransactionState.IDLE;
        bossUuid = null;
        summonerUuid = null;
        adminTestMode = false;
        cooldownUntil = 0L;
        stateChangedAt = now;
        missingBossSince = -1L;
        resetResonances();
        clearRitualBossBar();
        setChangedAndSync();
    }

    private void resetResonances() {
        resonanceMask = 0;
        lastResonanceIndex = -1;
        lastResonanceAt = -1L;
    }

    private static int bit(EchoAbilityType ability) {
        return 1 << ability.ordinal();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setRemoved() {
        clearRitualBossBar();
        super.setRemoved();
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
        tag.putBoolean("AdminTestMode", adminTestMode);
        tag.putLong("CooldownUntil", cooldownUntil);
        tag.putLong("StateChangedAt", stateChangedAt);
        tag.putInt("ResonanceMask", resonanceMask);
        tag.putInt("LastResonanceIndex", lastResonanceIndex);
        tag.putLong("LastResonanceAt", lastResonanceAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        arenaId = tag.hasUUID("ArenaId") ? tag.getUUID("ArenaId") : UUID.randomUUID();
        state = readState(tag);
        bossUuid = tag.hasUUID("BossUuid") ? tag.getUUID("BossUuid") : null;
        summonerUuid = tag.hasUUID("SummonerUuid") ? tag.getUUID("SummonerUuid") : null;
        adminTestMode = tag.getBoolean("AdminTestMode");
        cooldownUntil = Math.max(0L, tag.getLong("CooldownUntil"));
        stateChangedAt = Math.max(0L, tag.getLong("StateChangedAt"));
        boolean hasResonanceData = tag.contains("ResonanceMask", Tag.TAG_INT);
        resonanceMask = hasResonanceData
                ? tag.getInt("ResonanceMask") & ALL_RESONANCES
                : state == TransactionState.IDLE ? 0 : ALL_RESONANCES;
        lastResonanceIndex = tag.contains("LastResonanceIndex", Tag.TAG_INT)
                ? tag.getInt("LastResonanceIndex") : -1;
        if (lastResonanceIndex < -1 || lastResonanceIndex >= EchoAbilityType.values().length) {
            lastResonanceIndex = -1;
        }
        lastResonanceAt = tag.contains("LastResonanceAt", Tag.TAG_LONG)
                ? tag.getLong("LastResonanceAt") : -1L;
        missingBossSince = -1L;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
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

    private record RecoveryContext(UUID arenaId, @Nullable UUID bossUuid, TransactionState state,
                                   long stateChangedAt, long cooldownUntil, int resonanceMask,
                                   BlockState altarState, boolean areaLoaded) {
    }

    private record BossLookup(@Nullable WorldEcho boss, @Nullable RecoveryContext context,
                              boolean interrupted, boolean indexedOwner) {
    }

    private enum TransactionState {
        IDLE,
        STARTING,
        ACTIVE,
        SETTLING,
        COOLDOWN
    }
}
