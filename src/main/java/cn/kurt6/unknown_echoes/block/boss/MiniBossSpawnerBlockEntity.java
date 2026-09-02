package cn.kurt6.unknown_echoes.block.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mini Boss 场地计时器，玩家靠近时生成守护者，远离时不执行生成检查。 */
public class MiniBossSpawnerBlockEntity extends BlockEntity {

    private static final int TICK_INTERVAL = 40;
    private static final double ACTIVATION_RANGE = 28.0D;
    private static final double BOSS_RECOVERY_RANGE = 64.0D;
    private static final int MAX_RESET_POSITIONS = 256;
    /** 生成偏移与重置坐标的合法边界:写入端与 loadAdditional 共用,避免存档前后不一致。 */
    private static final int MIN_SPAWN_Y_OFFSET = -16;
    private static final int MAX_SPAWN_Y_OFFSET = 64;
    private static final int MAX_RESET_DISTANCE = 128;

    /** 要生成的守护者实体类型 ID(unknown_echoes:storm_weaver 等)。 */
    private ResourceLocation entityTypeId = null;
    /** 生成点相对本方块的垂直偏移(spawner 通常埋在地板下)。 */
    private int spawnYOffset = 2;
    /** 当前在场守护者;null = 尚未生成或已死亡。 */
    private UUID bossUuid = null;
    /** 重开时刻(gameTime);-1 = 未在计时。 */
    private long reopenAt = -1L;
    /** 是否已被击败过至少一次(重开关闭时用于一次性判定)。 */
    private boolean defeatedOnce = false;
    /** 当前等待从非死亡的异常消失中恢复;该状态不受“击败后不重开”配置限制。 */
    private boolean missingBossRecoveryPending = false;
    /** 重开时需要补回的机制方块(如共鸣烛)。 */
    private ResourceLocation resetBlockId = null;
    private final List<BlockPos> resetPositions = new ArrayList<>();

    /** 挑战门槛( 区域守护者):需要的能力,null = 无门槛。 */
    private EchoAbilityType requiredAbility = null;
    /** 门槛的替代记录:已击败该 Boss 也算满足(主线完成记录,如深渊观测者)。 */
    private ResourceLocation fallbackBossId = null;
    /** 未满足门槛时的含蓄提示 lang 键。 */
    private String lockedHintKey = null;
    private int lockedHintCooldown = 0;

    public MiniBossSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINIBOSS_SPAWNER.get(), pos, state);
    }

    /** 结构生成时配置:守护者类型与生成高度偏移。 */
    public void configure(ResourceLocation entityTypeId, int spawnYOffset) {
        this.entityTypeId = entityTypeId;
        this.spawnYOffset = clampSpawnYOffset(spawnYOffset);
        this.setChanged();
    }

    private static int clampSpawnYOffset(int spawnYOffset) {
        return Math.max(MIN_SPAWN_Y_OFFSET, Math.min(MAX_SPAWN_Y_OFFSET, spawnYOffset));
    }

    /** 结构生成时登记重开要补回的机制方块(绝对坐标)。 */
    public void setResetBlocks(ResourceLocation blockId, List<BlockPos> positions) {
        this.resetBlockId = blockId;
        this.resetPositions.clear();
        for (BlockPos position : positions) {
            if (this.resetPositions.size() >= MAX_RESET_POSITIONS) {
                break;
            }
            if (this.worldPosition.distManhattan(position) <= MAX_RESET_DISTANCE) {
                this.resetPositions.add(position.immutable());
            }
        }
        this.setChanged();
    }

    /**
     * 结构生成时登记挑战门槛,由服务端读能力数据判定,飞行与水下呼吸 Mod 绕不过。
     * 附近玩家持有 ability 或个人击败过 fallbackBossId 才生成守护者,否则只用 hintKey 给含蓄提示。
     */
    public void setChallengeGate(EchoAbilityType ability, ResourceLocation fallbackBossId, String hintKey) {
        this.requiredAbility = ability;
        this.fallbackBossId = fallbackBossId;
        this.lockedHintKey = hintKey;
        this.setChanged();
    }

    /** 守护者死亡回调:启动重开计时。认领必须凭 spawnerPos 绑定,否则别处的死亡会清掉本场地的锁。 */
    public void onBossDefeated(UUID defeatedBossUuid) {
        if (this.bossUuid == null && this.level instanceof ServerLevel serverLevel) {
            this.recoverLoadedBoss(serverLevel);
        }
        if (this.bossUuid != null) {
            if (!this.bossUuid.equals(defeatedBossUuid)) {
                return;
            }
        } else if (!this.canAdoptUnownedBoss()
                || !this.isBoundToThisSpawner(defeatedBossUuid)) {
            return;
        }
        this.bossUuid = null;
        this.defeatedOnce = true;
        this.missingBossRecoveryPending = false;
        if (this.level != null) {
            this.reopenAt = safeDeadline(this.level.getGameTime(),
                    ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
        }
        this.setChanged();
    }

    /** 区块重载时只补回空缺所有者，重复实体不能覆盖已登记 UUID。 */
    public boolean onBossAdded(UUID addedBossUuid) {
        if (this.bossUuid == null) {
            if (!this.canAdoptUnownedBoss()) {
                return false;
            }
            this.bossUuid = addedBossUuid;
            this.missingBossRecoveryPending = false;
            this.setChanged();
        }
        return addedBossUuid.equals(this.bossUuid);
    }

    /** 绑定实体真正销毁时进入异常恢复；区块卸载不会调用此入口。 */
    public void onBossDestroyed(UUID destroyedBossUuid) {
        if (!destroyedBossUuid.equals(this.bossUuid)) {
            return;
        }
        this.bossUuid = null;
        this.missingBossRecoveryPending = true;
        if (this.level != null) {
            this.reopenAt = safeDeadline(this.level.getGameTime(),
                    ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
        }
        this.setChanged();
    }

    public static BindingState bindingState(ServerLevel level, BlockPos pos, MiniBossEntity boss) {
        if (!level.hasChunkAt(pos)) {
            return BindingState.UNAVAILABLE;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof MiniBossSpawnerBlock)) {
            return BindingState.INVALID;
        }
        if (!(level.getBlockEntity(pos) instanceof MiniBossSpawnerBlockEntity spawner)) {

            return BindingState.UNAVAILABLE;
        }
        return spawner.matchesConfiguredType(boss) && spawner.onBossAdded(boss.getUUID())
                ? BindingState.VALID : BindingState.INVALID;
    }

    /** 实体销毁事件可能早于刷怪器方块实体加载；在方块仍存在时按需恢复清理状态。 */
    public static void onBossDestroyed(ServerLevel level, BlockPos pos, UUID bossUuid) {
        if (!level.hasChunkAt(pos)) {
            MiniBossSpawnerCleanupData.queueDestroyed(level, pos, bossUuid);
            return;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof MiniBossSpawnerBlock)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof MiniBossSpawnerBlockEntity spawner) {
            spawner.onBossDestroyed(bossUuid);
        } else {

            MiniBossSpawnerCleanupData.queueDestroyed(level, pos, bossUuid);
        }
    }

    /** 死亡结算可能早于刷怪器方块实体恢复；按需恢复后启动正常重开计时。 */
    public static void onBossDefeated(ServerLevel level, BlockPos pos, UUID bossUuid) {
        if (!level.hasChunkAt(pos)) {
            MiniBossSpawnerCleanupData.queueDefeated(level, pos, bossUuid);
            return;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof MiniBossSpawnerBlock)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof MiniBossSpawnerBlockEntity spawner) {
            spawner.onBossDefeated(bossUuid);
        } else {
            MiniBossSpawnerCleanupData.queueDefeated(level, pos, bossUuid);
        }
    }

    private boolean matchesConfiguredType(MiniBossEntity boss) {
        return this.entityTypeId != null && this.entityTypeId.equals(
                BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
    }

    public enum BindingState {
        UNAVAILABLE,
        INVALID,
        VALID
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  MiniBossSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel serverLevel) || spawner.entityTypeId == null) {
            return;
        }
        long now = level.getGameTime();
        if (Math.floorMod(Math.floorMod(now, (long) TICK_INTERVAL)
                        + Math.floorMod(pos.asLong(), (long) TICK_INTERVAL),
                        (long) TICK_INTERVAL) != 0L) {
            return;
        }

        if (spawner.bossUuid != null) {
            return;
        }

        if (spawner.defeatedOnce || spawner.missingBossRecoveryPending) {
            if (spawner.defeatedOnce && !spawner.missingBossRecoveryPending
                    && !ServerConfig.MINIBOSS_ARENA_REOPEN.get()) {
                return;
            }
            if (spawner.reopenAt < 0) {
                spawner.reopenAt = safeDeadline(now,
                        ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
                spawner.setChanged();
                return;
            }
            long maximum = safeDeadline(now,
                    ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
            if (spawner.reopenAt > maximum) {
                spawner.reopenAt = maximum;
                spawner.setChanged();
            }
            if (now < spawner.reopenAt) {
                return;
            }
        }
        if (spawner.requiredAbility == null) {
            if (nearbyPlayers(serverLevel, pos).isEmpty()) {
                return;
            }
        } else if (!spawner.checkChallengeGate(serverLevel, pos)) {
            return;
        }
        if (!spawner.areResetChunksLoaded(serverLevel)) {
            return;
        }
        if (spawner.recoverLoadedBoss(serverLevel)) {
            return;
        }
        spawner.restoreResetBlocks(serverLevel);
        spawner.spawnBoss(serverLevel);
    }

    /**
     * 实体与方块实体的加载回调没有顺序契约。
     * 生成前按精确绑定补扫一次，避免实体先加载时错过 onBossAdded，随后同一场地再生成一只。
     */
    private boolean recoverLoadedBoss(ServerLevel level) {
        List<MiniBossEntity> candidates = level.getEntitiesOfClass(
                MiniBossEntity.class, new AABB(this.worldPosition).inflate(BOSS_RECOVERY_RANGE),
                boss -> boss.isAlive() && !boss.isRemoved()
                        && this.worldPosition.equals(boss.spawnerPos())
                        && this.entityTypeId.equals(
                                BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())));
        if (candidates.isEmpty()) {
            return false;
        }
        if (!this.canAdoptUnownedBoss()) {
            for (MiniBossEntity candidate : candidates) {
                candidate.discard();
            }
            return false;
        }
        MiniBossEntity owner = candidates.getFirst();
        this.bossUuid = owner.getUUID();
        this.missingBossRecoveryPending = false;
        this.reopenAt = -1L;
        this.setChanged();
        for (int i = 1; i < candidates.size(); i++) {
            candidates.get(i).discard();
        }
        return true;
    }

    private boolean canAdoptUnownedBoss() {
        return !this.defeatedOnce && !this.missingBossRecoveryPending && this.reopenAt < 0L;
    }

    /** 该 UUID 是否绑定到本刷怪器。实体查不到时返回 false:未加载不构成绑定证据。 */
    private boolean isBoundToThisSpawner(UUID bossUuid) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getEntity(bossUuid) instanceof MiniBossEntity boss
                && this.worldPosition.equals(boss.spawnerPos())
                && this.matchesConfiguredType(boss);
    }

    /** 门槛检查:范围内任一玩家满足即放行;全员未满足时按冷却发含蓄提示。 */
    private boolean checkChallengeGate(ServerLevel level, BlockPos pos) {
        if (this.requiredAbility == null) {
            return true;
        }
        List<ServerPlayer> nearby = nearbyPlayers(level, pos);
        for (ServerPlayer player : nearby) {
            if (EchoAbilityManager.hasAbility(player, this.requiredAbility)
                    || (this.fallbackBossId != null
                    && EchoAbilityManager.hasDefeatedBoss(player, this.fallbackBossId))) {
                return true;
            }
        }
        if (this.lockedHintCooldown > 0) {
            this.lockedHintCooldown -= TICK_INTERVAL;
        } else if (this.lockedHintKey != null && !nearby.isEmpty()) {
            for (ServerPlayer player : nearby) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(this.lockedHintKey), true);
            }
            this.lockedHintCooldown = 200;
        }
        return false;
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, BlockPos pos) {
        double radiusSqr = ACTIVATION_RANGE * ACTIVATION_RANGE;
        return level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(pos).inflate(ACTIVATION_RANGE),
                player -> !player.isSpectator()
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                                pos.getZ() + 0.5D) <= radiusSqr);
    }

    /** 重开时补回被打碎的机制方块(共鸣烛等),只填空气位,不覆盖玩家建筑。 */
    private boolean areResetChunksLoaded(ServerLevel level) {
        for (BlockPos pos : this.resetPositions) {
            if (!level.hasChunkAt(pos)) {
                return false;
            }
        }
        return true;
    }

    private void restoreResetBlocks(ServerLevel level) {
        if (this.resetBlockId == null) {
            return;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(this.resetBlockId).orElse(null);
        if (block == null) {
            UnknownEchoes.LOGGER.warn("Mini boss spawner at {} references missing reset block {}",
                    this.worldPosition, this.resetBlockId);
            this.resetBlockId = null;
            this.setChanged();
            return;
        }
        for (BlockPos pos : this.resetPositions) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, block.defaultBlockState(), 3);
            }
        }
    }

    private void spawnBoss(ServerLevel level) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(this.entityTypeId).orElse(null);
        if (type == null) {
            UnknownEchoes.LOGGER.warn("Mini boss spawner at {} references missing entity {}",
                    this.worldPosition, this.entityTypeId);
            this.entityTypeId = null;
            this.setChanged();
            return;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof MiniBossEntity miniBoss)) {
            UnknownEchoes.LOGGER.error("Mini boss spawner at {} references non-mini-boss entity {}",
                    this.worldPosition, this.entityTypeId);
            this.entityTypeId = null;
            this.setChanged();
            return;
        }
        BlockPos spawnPos = findSpawnPosition(level, miniBoss);
        if (spawnPos == null) {
            entity.discard();
            return;
        }
        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        miniBoss.bindSpawner(this.worldPosition);
        miniBoss.setPersistenceRequired();
        miniBoss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.STRUCTURE, null);
        if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, entity)) {
            entity.discard();
            return;
        }
        UUID spawnedBossUuid = entity.getUUID();
        this.bossUuid = spawnedBossUuid;
        this.setChanged();
        if (!level.addFreshEntity(entity)) {
            entity.discard();
            if (spawnedBossUuid.equals(this.bossUuid)) {
                this.bossUuid = null;
                this.setChanged();
            }
            UnknownEchoes.LOGGER.warn("Mini boss spawner at {} failed to add entity {}",
                    this.worldPosition, this.entityTypeId);
            return;
        }
        this.missingBossRecoveryPending = false;
        this.reopenAt = -1L;
        this.setChanged();

        level.sendParticles(ParticleTypes.CLOUD,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                30, 0.8, 1.2, 0.8, 0.05);
        level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    /** 按中心、四向与上下偏移重试落点。水下 Boss 还要确认整根水柱,否则会出生在空气里。 */
    private BlockPos findSpawnPosition(ServerLevel level, MiniBossEntity boss) {
        int[][] offsets = {
                {0, 0}, {0, 3}, {3, 0}, {-3, 0}, {0, -3},
                {3, 3}, {3, -3}, {-3, 3}, {-3, -3},
                {0, 6}, {6, 0}, {-6, 0}, {0, -6}
        };
        int[] verticalOffsets = {0, 1, -1, 2, -2};
        for (int[] offset : offsets) {
            for (int dy : verticalOffsets) {
                BlockPos candidate = this.worldPosition.above(this.spawnYOffset)
                        .offset(offset[0], dy, offset[1]);
                boss.moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D,
                        0.0F, 0.0F);
                if (boss.requiresWaterSpawn() && !hasWaterBody(level, boss, candidate)) {
                    continue;
                }
                if (EntitySpawnSafety.isLoadedAndCollisionFree(level, boss)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean hasWaterBody(ServerLevel level, MiniBossEntity boss, BlockPos candidate) {
        int bodyHeight = Math.max(1, (int) Math.ceil(boss.getBbHeight()));
        for (int y = 0; y < bodyHeight; y++) {
            if (!level.getFluidState(candidate.above(y)).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            MiniBossSpawnerCleanupData.apply(serverLevel, worldPosition, this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.entityTypeId != null) {
            tag.putString("EntityType", this.entityTypeId.toString());
        }
        tag.putInt("SpawnYOffset", this.spawnYOffset);
        tag.putLong("ReopenAt", this.reopenAt);
        tag.putBoolean("DefeatedOnce", this.defeatedOnce);
        tag.putBoolean("MissingBossRecoveryPending", this.missingBossRecoveryPending);
        if (this.bossUuid != null) {
            tag.put("BossUuid", NbtUtils.createUUID(this.bossUuid));
        }
        if (this.resetBlockId != null) {
            tag.putString("ResetBlock", this.resetBlockId.toString());
        }
        if (this.requiredAbility != null) {
            tag.putString("RequiredAbility", this.requiredAbility.getId());
        }
        if (this.fallbackBossId != null) {
            tag.putString("FallbackBoss", this.fallbackBossId.toString());
        }
        if (this.lockedHintKey != null) {
            tag.putString("LockedHint", this.lockedHintKey);
        }
        ListTag positions = new ListTag();
        for (BlockPos pos : this.resetPositions) {
            positions.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("ResetPositions", positions);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.entityTypeId = readResourceLocation(tag, "EntityType");
        this.spawnYOffset = clampSpawnYOffset(
                tag.contains("SpawnYOffset") ? tag.getInt("SpawnYOffset") : 2);
        long savedReopenAt = tag.contains("ReopenAt") ? tag.getLong("ReopenAt") : -1L;
        this.reopenAt = savedReopenAt < 0L ? -1L : savedReopenAt;
        this.defeatedOnce = tag.getBoolean("DefeatedOnce");
        this.missingBossRecoveryPending = tag.getBoolean("MissingBossRecoveryPending");
        this.bossUuid = tag.hasUUID("BossUuid") ? tag.getUUID("BossUuid") : null;
        this.resetBlockId = readResourceLocation(tag, "ResetBlock");
        this.requiredAbility = tag.contains("RequiredAbility")
                ? EchoAbilityType.byId(tag.getString("RequiredAbility")) : null;
        this.fallbackBossId = readResourceLocation(tag, "FallbackBoss");
        String hint = tag.contains("LockedHint", Tag.TAG_STRING) ? tag.getString("LockedHint") : null;
        this.lockedHintKey = hint == null ? null : hint.substring(0, Math.min(256, hint.length()));
        this.resetPositions.clear();
        ListTag positions = tag.getList("ResetPositions", Tag.TAG_INT_ARRAY);
        int limit = Math.min(positions.size(), MAX_RESET_POSITIONS);
        for (int i = 0; i < limit; i++) {
            Tag entry = positions.get(i);
            if (!(entry instanceof IntArrayTag positionTag)) {
                continue;
            }
            int[] xyz = positionTag.getAsIntArray();
            if (xyz.length == 3) {
                BlockPos resetPos = new BlockPos(xyz[0], xyz[1], xyz[2]);
                if (this.worldPosition.distManhattan(resetPos) <= MAX_RESET_DISTANCE) {
                    this.resetPositions.add(resetPos);
                }
            }
        }
    }

    private static long safeDeadline(long now, long duration) {
        if (now < 0L) {
            now = 0L;
        }
        if (duration <= 0L || duration > Long.MAX_VALUE - now) {
            return duration <= 0L ? now : Long.MAX_VALUE;
        }
        return now + duration;
    }

    private static ResourceLocation readResourceLocation(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString(key)) : null;
    }
}
