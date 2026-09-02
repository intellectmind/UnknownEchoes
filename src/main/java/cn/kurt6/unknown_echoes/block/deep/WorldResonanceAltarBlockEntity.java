package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.sounds.SoundEvents;
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
    private static final List<ParticleOptions> RITUAL_PARTICLES = List.of(
            ParticleTypes.CLOUD,
            ParticleTypes.SPLASH,
            ParticleTypes.ENCHANT,
            ParticleTypes.PORTAL,
            ParticleTypes.REVERSE_PORTAL);
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
        reconcile(level);
        long now = level.getGameTime();
        if (state != TransactionState.IDLE || isFullyResonant() || isResonanceLocked(now)) {
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
            playResonanceFeedback(level, index);
            return ability;
        }
        return null;
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
        reconcile(level);
        if (state != TransactionState.IDLE || !isFullyResonant()) {
            return false;
        }
        state = TransactionState.STARTING;
        bossUuid = null;
        summonerUuid = summoner;
        stateChangedAt = level.getGameTime();
        missingBossSince = -1L;
        setChangedAndSync();
        playRitualOpening(level);
        updateRitualBossBar(level, 0);
        return true;
    }

    /** 管理员测试入口：跳过正式能力门槛，但保留祭坛仪式、场地绑定和 Boss 事务。 */
    public boolean beginAdminTestStarting(ServerLevel level, UUID summoner) {
        reconcile(level);
        if (state != TransactionState.IDLE) {
            return false;
        }
        resonanceMask = ALL_RESONANCES;
        lastResonanceIndex = EchoAbilityType.values().length - 1;
        lastResonanceAt = level.getGameTime();
        adminTestMode = true;
        for (int index = 0; index < EchoAbilityType.values().length; index++) {
            playResonanceFeedback(level, index);
        }
        if (beginStarting(level, summoner)) {
            return true;
        }
        adminTestMode = false;
        resetResonances();
        setChangedAndSync();
        return false;
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
        reconcile(level);
        return findLinkedBoss(level, false);
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

        if (altar.state == TransactionState.IDLE) {
            return;
        }
        if (altar.state == TransactionState.STARTING) {
            altar.tickRitual(serverLevel);
            return;
        }
        long now = level.getGameTime();
        WorldEcho checkedBoss = null;
        boolean checkedActiveBoss = false;
        if (altar.state == TransactionState.ACTIVE && now % 5L == 0L) {
            checkedActiveBoss = true;
            checkedBoss = altar.findLinkedBoss(serverLevel, now % 20L == 0L);
            if (checkedBoss != null) {
                altar.enforceArenaBoundary(serverLevel, checkedBoss);
            }
        }
        if (now % 20L == 0L) {
            if (checkedActiveBoss) {
                altar.reconcile(serverLevel, checkedBoss);
            } else {
                altar.reconcile(serverLevel);
            }
        }
    }

    private void playRitualOpening(ServerLevel level) {
        Vec3 core = Vec3.atCenterOf(worldPosition).add(0.0D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z,
                2, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z,
                90, 1.8D, 1.4D, 1.8D, 0.16D);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 2.4F, 0.55F);
    }

    private void playResonanceFeedback(ServerLevel level, int index) {
        Vec3 local = LOCAL_RESONATORS.get(index);
        double x = worldPosition.getX() + local.x;
        double y = worldPosition.getY() + local.y;
        double z = worldPosition.getZ() + local.z;
        ParticleOptions particle = RITUAL_PARTICLES.get(index);
        level.sendParticles(particle, x, y, z,
                42, 0.22D, 0.42D, 0.22D, 0.045D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.18D, z,
                18, 0.12D, 0.28D, 0.12D, 0.025D);
        var sound = switch (index) {
            case 0 -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case 1 -> SoundEvents.CONDUIT_ACTIVATE;
            case 2 -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case 3 -> SoundEvents.RESPAWN_ANCHOR_CHARGE;
            default -> SoundEvents.END_PORTAL_FRAME_FILL;
        };
        float pitch = 0.72F + index * 0.12F;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.7F, pitch);
    }

    private void tickRitual(ServerLevel level) {
        long now = level.getGameTime();
        if (now < stateChangedAt) {
            stateChangedAt = now;
            setChangedAndSync();
        }
        int age = (int) Math.min(Integer.MAX_VALUE, now - stateChangedAt);
        updateRitualBossBar(level, age);
        renderRitual(level, age);

        if (age == 45) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE,
                    SoundSource.BLOCKS, 2.5F, 0.72F);
        } else if (age == 105) {
            level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.BLOCKS, 2.8F, 0.48F);
        } else if (age == 150) {
            Vec3 core = Vec3.atCenterOf(worldPosition).add(0.0D, 7.0D, 0.0D);
            level.sendParticles(ParticleTypes.FLASH, core.x, core.y, core.z,
                    3, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, worldPosition, SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE, 3.0F, 0.7F);
        }

        if (age < WorldResonanceLayout.RITUAL_DURATION_TICKS) {
            return;
        }
        if (bossUuid != null) {
            WorldEcho reservedBoss = findLinkedBoss(level, false);
            if (reservedBoss != null) {
                adoptBoss(level, reservedBoss);
            } else if (elapsed(now, stateChangedAt,
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
        Vec3 core = Vec3.atCenterOf(worldPosition).add(0.0D,
                age < 105 ? 4.5D : 7.0D, 0.0D);
        if ((age & 1) == 0) {
            List<Vec3> pylons = resonancePylonTops();
            for (int index = 0; index < pylons.size(); index++) {
                Vec3 source = pylons.get(index);
                double reach = Math.min(1.0D, Math.max(0.08D, (age - index * 6) / 58.0D));
                ParticleOptions particle = RITUAL_PARTICLES.get(index);
                for (int sample = 0; sample < 7; sample++) {
                    double t = level.random.nextDouble() * reach;
                    Vec3 point = source.lerp(core, t);
                    level.sendParticles(particle, point.x, point.y, point.z,
                            1, 0.035D, 0.035D, 0.035D, 0.005D);
                }
                level.sendParticles(particle, source.x, source.y, source.z,
                        age >= 45 ? 5 : 2, 0.35D, 0.45D, 0.35D, 0.025D);
            }
        }

        if (age >= 45) {
            double spin = age * 0.12D;
            double height = Math.min(14.0D, 2.0D + (age - 45) * 0.12D);
            for (int arm = 0; arm < 5; arm++) {
                double angle = spin + arm * Math.PI * 0.4D;
                double radius = 2.5D + Math.sin(age * 0.08D + arm) * 0.8D;
                level.sendParticles(RITUAL_PARTICLES.get(arm),
                        core.x + Math.cos(angle) * radius,
                        worldPosition.getY() + 1.0D + height * (arm + 1) / 5.0D,
                        core.z + Math.sin(angle) * radius,
                        2, 0.08D, 0.15D, 0.08D, 0.01D);
            }
        }
        if (age >= 90 && age % 4 == 0) {
            double radius = 4.0D + (age - 90) % 36 * 0.32D;
            ritualRing(level, core, radius, age >= 145
                    ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT);
        }
        if (age >= 135) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    core.x, core.y, core.z, 18,
                    2.2D, 4.5D, 2.2D, 0.18D);
        }
    }

    private static void ritualRing(ServerLevel level, Vec3 center, double radius,
                                   ParticleOptions particle) {
        for (int step = 0; step < 48; step++) {
            double angle = step * Math.PI * 2.0D / 48.0D;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * radius,
                    center.y + Math.sin(angle * 2.0D) * 0.35D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
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
        Vec3 core = Vec3.atCenterOf(worldPosition).add(0.0D, 3.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, core.x, core.y, core.z,
                80, 1.5D, 1.6D, 1.5D, 0.08D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, core.x, core.y, core.z,
                60, 1.2D, 1.2D, 1.2D, 0.15D);
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS, 2.0F, 0.55F);
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
            ritualEvent.setDarkenScreen(true);
            ritualEvent.setCreateWorldFog(true);
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

    private void reconcile(ServerLevel level, @Nullable WorldEcho boss) {
        if (boss != null) {
            if (state != TransactionState.STARTING && state != TransactionState.ACTIVE) {
                boss.discard();
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
        return LoadedAreaCheck.horizontalRadiusLoaded(level, worldPosition, BOSS_RANGE_H);
    }

    @Nullable
    private WorldEcho findLinkedBoss(ServerLevel level, boolean allowAdoption) {
        if (bossUuid != null) {
            Entity entity = level.getEntity(bossUuid);
            if (entity instanceof WorldEcho boss && boss.isAlive() && !boss.isRemoved()) {
                if (boss.matchesAltar(arenaId, worldPosition)) {

                    return boss;
                }
                else if (allowAdoption && state == TransactionState.ACTIVE && boss.canBindToAltar()) {
                    boss.bindToAltar(arenaId, worldPosition);
                    return boss;
                }
                else if (!allowAdoption) {
                    return null;
                }
                else {
                    UnknownEchoes.LOGGER.warn(
                            "World resonance altar {} rejected boss {} with a mismatched arena binding",
                            worldPosition, bossUuid);
                    bossUuid = null;
                    setChangedAndSync();
                }
            }
        }

        if (!allowAdoption) {
            return null;
        }

        AABB arena = new AABB(worldPosition).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H);
        List<WorldEcho> nearby = level.getEntitiesOfClass(WorldEcho.class, arena,
                boss -> boss.isAlive() && !boss.isRemoved());

        WorldEcho owner = null;
        double ownerDistance = Double.MAX_VALUE;
        boolean allowLegacyAdoption = bossUuid == null && state == TransactionState.ACTIVE;
        for (WorldEcho boss : nearby) {
            if (!boss.matchesAltar(arenaId, worldPosition)) {
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
            if (boss != owner && boss.matchesAltar(arenaId, worldPosition)) {
                boss.discard();
            }
        }
        return owner;
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

    private enum TransactionState {
        IDLE,
        STARTING,
        ACTIVE,
        SETTLING,
        COOLDOWN
    }
}
