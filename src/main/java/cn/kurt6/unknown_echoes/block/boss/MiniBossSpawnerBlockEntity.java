package cn.kurt6.unknown_echoes.block.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mini Boss 场地计时器逻辑:
 * - 首次生成:有玩家靠近(ACTIVATION_RANGE)时生成守护者——无人区域零开销(10.4.1 实现边界)。
 * - 场地重开:守护者死亡回调 {@link #onBossDefeated()} 启动计时;到点且有玩家靠近时
 *   重置机制方块(共鸣烛等)并重新生成。重开可整体关闭(MINIBOSS_ARENA_REOPEN)。
 * - 重置方块表:结构生成时写入(如四角共鸣烛位置),重开时补齐被打碎的。
 * 注意低频 tick(2 秒一次),且只做廉价检查。
 */
public class MiniBossSpawnerBlockEntity extends BlockEntity {

    private static final int TICK_INTERVAL = 40;
    private static final double ACTIVATION_RANGE = 28.0D;
    private static final int MAX_RESET_POSITIONS = 256;
    private static final int MISSING_BOSS_CONFIRMATIONS = 3;

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
    /** 重开时需要补回的机制方块(如共鸣烛)。 */
    private ResourceLocation resetBlockId = null;
    private final List<BlockPos> resetPositions = new ArrayList<>();

    /** 挑战门槛(V0.6C 区域守护者):需要的能力,null = 无门槛。 */
    private EchoAbilityType requiredAbility = null;
    /** 门槛的替代记录:已击败该 Boss 也算满足(主线完成记录,如深渊观测者)。 */
    private ResourceLocation fallbackBossId = null;
    /** 未满足门槛时的含蓄提示 lang 键。 */
    private String lockedHintKey = null;
    private int lockedHintCooldown = 0;
    private int missingBossChecks = 0;

    public MiniBossSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINIBOSS_SPAWNER.get(), pos, state);
    }

    /** 结构生成时配置:守护者类型与生成高度偏移。 */
    public void configure(ResourceLocation entityTypeId, int spawnYOffset) {
        this.entityTypeId = entityTypeId;
        this.spawnYOffset = spawnYOffset;
        this.setChanged();
    }

    /** 结构生成时登记重开要补回的机制方块(绝对坐标)。 */
    public void setResetBlocks(ResourceLocation blockId, List<BlockPos> positions) {
        this.resetBlockId = blockId;
        this.resetPositions.clear();
        int limit = Math.min(positions.size(), MAX_RESET_POSITIONS);
        for (int i = 0; i < limit; i++) {
            this.resetPositions.add(positions.get(i).immutable());
        }
        this.setChanged();
    }

    /**
     * 结构生成时登记挑战门槛(红线 #1:服务端检测能力数据,飞行/水下呼吸 Mod 绕不过):
     * 附近玩家持有 ability、或个人击败过 fallbackBossId 时才生成守护者;
     * 否则只用 hintKey 给含蓄提示。
     */
    public void setChallengeGate(EchoAbilityType ability, ResourceLocation fallbackBossId, String hintKey) {
        this.requiredAbility = ability;
        this.fallbackBossId = fallbackBossId;
        this.lockedHintKey = hintKey;
        this.setChanged();
    }

    /** 守护者死亡回调(MiniBossEntity.die):启动重开计时。 */
    public void onBossDefeated() {
        this.bossUuid = null;
        this.missingBossChecks = 0;
        this.defeatedOnce = true;
        if (this.level != null) {
            this.reopenAt = safeDeadline(this.level.getGameTime(),
                    ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
        }
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  MiniBossSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel serverLevel)
                || Math.floorMod(level.getGameTime() + pos.asLong(), TICK_INTERVAL) != 0L
                || spawner.entityTypeId == null) {
            return;
        }
        // 在场守护者存活检查(掉出世界等异常消失 → 兜底走重开计时)
        if (spawner.bossUuid != null) {
            Entity boss = serverLevel.getEntity(spawner.bossUuid);
            if (boss != null && boss.isAlive()) {
                spawner.missingBossChecks = 0;
                return;
            }
            if (boss == null) {
                if (serverLevel.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, ACTIVATION_RANGE, false) == null) {
                    spawner.missingBossChecks = 0;
                    return;
                }
                if (++spawner.missingBossChecks < MISSING_BOSS_CONFIRMATIONS) {
                    return;
                }
            }
            spawner.missingBossChecks = 0;
            spawner.bossUuid = null;
            spawner.defeatedOnce = true;
            if (spawner.reopenAt < 0) {
                spawner.reopenAt = safeDeadline(level.getGameTime(),
                        ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
            }
            spawner.setChanged();
            return;
        }
        // 击败后:重开关闭 = 一次性场地;开启则等计时到点
        if (spawner.defeatedOnce) {
            if (!ServerConfig.MINIBOSS_ARENA_REOPEN.get()) {
                return;
            }
            if (spawner.reopenAt < 0) {
                spawner.reopenAt = safeDeadline(level.getGameTime(),
                        ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
                spawner.setChanged();
                return;
            }
            long maximum = safeDeadline(level.getGameTime(),
                    ServerConfig.MINIBOSS_ARENA_REOPEN_TICKS.get());
            if (spawner.reopenAt > maximum) {
                spawner.reopenAt = maximum;
                spawner.setChanged();
            }
            if (level.getGameTime() < spawner.reopenAt) {
                return;
            }
        }
        if (spawner.requiredAbility == null) {
            // 无门槛时只做一次最近玩家查询
            if (serverLevel.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ACTIVATION_RANGE, false) == null) {
                return;
            }
        } else if (!spawner.checkChallengeGate(serverLevel, pos)) {
            return;
        }
        spawner.restoreResetBlocks(serverLevel);
        spawner.spawnBoss(serverLevel);
    }

    /** 门槛检查:范围内任一玩家满足即放行;全员未满足时按冷却发含蓄提示。 */
    private boolean checkChallengeGate(ServerLevel level, BlockPos pos) {
        if (this.requiredAbility == null) {
            return true;
        }
        boolean collectHints = this.lockedHintCooldown <= 0 && this.lockedHintKey != null;
        List<ServerPlayer> nearby = null;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    <= ACTIVATION_RANGE * ACTIVATION_RANGE) {
                if (EchoAbilityManager.hasAbility(player, this.requiredAbility)
                        || (this.fallbackBossId != null
                        && EchoAbilityManager.hasDefeatedBoss(player, this.fallbackBossId))) {
                    return true;
                }
                if (collectHints) {
                    if (nearby == null) {
                        nearby = new ArrayList<>();
                    }
                    nearby.add(player);
                }
            }
        }
        if (this.lockedHintCooldown > 0) {
            this.lockedHintCooldown -= TICK_INTERVAL;
        } else if (nearby != null && !nearby.isEmpty()) {
            for (ServerPlayer player : nearby) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(this.lockedHintKey), true);
            }
            this.lockedHintCooldown = 200;
        }
        return false;
    }

    /** 重开时补回被打碎的机制方块(共鸣烛等),只填空气位,不覆盖玩家建筑。 */
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
        if (entity == null) {
            UnknownEchoes.LOGGER.error("Mini boss spawner at {} failed to create entity {}",
                    this.worldPosition, this.entityTypeId);
            return;
        }
        BlockPos spawnPos = this.worldPosition.above(this.spawnYOffset);
        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof MiniBossEntity miniBoss) {
            miniBoss.bindSpawner(this.worldPosition);
            miniBoss.setPersistenceRequired();
            miniBoss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.STRUCTURE, null);
        }
        if (!level.addFreshEntity(entity)) {
            UnknownEchoes.LOGGER.warn("Mini boss spawner at {} failed to add entity {}",
                    this.worldPosition, this.entityTypeId);
            return;
        }
        this.bossUuid = entity.getUUID();
        this.missingBossChecks = 0;
        this.reopenAt = -1L;
        this.setChanged();
        // 出场演出:守护者自下而上凝聚
        level.sendParticles(ParticleTypes.CLOUD,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                30, 0.8, 1.2, 0.8, 0.05);
        level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 0.7F);
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
        this.spawnYOffset = Math.max(-16, Math.min(64,
                tag.contains("SpawnYOffset") ? tag.getInt("SpawnYOffset") : 2));
        long savedReopenAt = tag.contains("ReopenAt") ? tag.getLong("ReopenAt") : -1L;
        this.reopenAt = savedReopenAt < 0L ? -1L : savedReopenAt;
        this.defeatedOnce = tag.getBoolean("DefeatedOnce");
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
            int[] xyz = ((IntArrayTag) entry).getAsIntArray();
            if (xyz.length == 3) {
                BlockPos resetPos = new BlockPos(xyz[0], xyz[1], xyz[2]);
                if (this.worldPosition.distManhattan(resetPos) <= 128) {
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
