package cn.kurt6.unknown_echoes.block.wind;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** 风流平台 BlockEntity:激活倒计时 + 上升气流柱(服务端权威)。 */
public class WindCurrentPlatformBlockEntity extends BlockEntity {

    /** 已加载的平台位置(服务端,按维度);BlockEntity 加载/移除时维护。 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED_PLATFORMS = new ConcurrentHashMap<>();
    /** 重叠气流去重:弱键不持有卸载实体，同一服务端 tick 最多施力一次。 */
    private static final Map<Entity, Long> LAST_FORCE_TICKS = new WeakHashMap<>();

    /** 每 tick 平滑积分气流推力，避免速度抖动。 */
    private static final double LIFT_ACCEL = 0.11D;
    private static final double LIFT_MAX_SPEED = 1.15D;
    /** 顺风推进:只限制沿视线方向的风力分量，不截断侧移或其他来源的动量。 */
    private static final double HORIZONTAL_DRAFT_ACCEL = 0.04D;
    private static final double HORIZONTAL_DRAFT_MAX_SPEED = 0.65D;
    /** 低于该水平速度视为"原地停留",只做垂直抬升,不叠加水平气流。 */
    private static final double HORIZONTAL_DRAFT_MIN_INPUT_SPEED_SQR = 0.01D * 0.01D;
    /** 离开气流后速度加成的保留时长(tick)。 */
    private static final int SPEED_BUFF_TICKS = 60;
    private static final int SPEED_BUFF_REFRESH_THRESHOLD_TICKS = 40;

    private int activeTicks = 0;
    private int stateRevision;
    private boolean ticking;
    private boolean interacting;

    public WindCurrentPlatformBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_CURRENT_PLATFORM.get(), pos, state);
    }

    void interact(ServerLevel level, BlockState state, ServerPlayer player) {
        int revision = stateRevision;
        if (interacting || !isCurrentInteraction(level, state, player, revision)) {
            return;
        }
        interacting = true;
        try {
            if (!EchoPermission.canUseWindMechanism(player)) {
                level.playSound(null, worldPosition, ModSounds.BLOCK_WIND_PLATFORM_LOCKED.get(),
                        SoundSource.BLOCKS, 0.55F, 1.0F);
                if (!isCurrentInteraction(level, state, player, revision)
                        || EchoPermission.canUseWindMechanism(player)) {
                    return;
                }
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.wind_platform.locked"), true);
                if (isCurrentInteraction(level, state, player, revision)
                        && !EchoPermission.canUseWindMechanism(player)) {
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "platform_locked");
                }
                return;
            }

            revision++;
            activate(ServerConfig.WIND_PLATFORM_ACTIVE_TICKS.get());
            if (!isCurrentInteraction(level, state, player, revision)
                    || !EchoPermission.canUseWindMechanism(player)) {
                return;
            }
            if (!state.getValue(WindCurrentPlatformBlock.ACTIVE)) {
                BlockState active = state.setValue(WindCurrentPlatformBlock.ACTIVE, Boolean.TRUE);
                if (level.setBlock(worldPosition, active, 3)
                        && isCurrentInteraction(level, active, player, revision)
                        && EchoPermission.canUseWindMechanism(player)) {
                    level.playSound(null, worldPosition, ModSounds.BLOCK_WIND_PLATFORM_ACTIVATE.get(),
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                }
            }
        } finally {
            interacting = false;
        }
    }

    private boolean isCurrentInteraction(ServerLevel level, BlockState state, ServerPlayer player, int revision) {
        return player.level() == level && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !player.hasDisconnected()
                && player.canInteractWithBlock(worldPosition, 1.0D)
                && isCurrentTick(level, state, revision);
    }

    public void activate(int ticks) {
        this.stateRevision++;
        this.activeTicks = Math.max(0, Math.min(Math.max(this.activeTicks, ticks),
                ServerConfig.WIND_PLATFORM_ACTIVE_TICKS.get()));
        this.setChanged();
    }

    /** 时间沙漏白名单重置入口：只清理临时气流,不改个人进度。 */
    public void deactivate() {
        int revision = ++this.stateRevision;
        this.activeTicks = 0;
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel && this.stateRevision == revision) {
            BlockState current = loadedState(serverLevel);
            if (current != null && current.getValue(WindCurrentPlatformBlock.ACTIVE)) {
                serverLevel.setBlock(this.worldPosition,
                        current.setValue(WindCurrentPlatformBlock.ACTIVE, Boolean.FALSE), 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WindCurrentPlatformBlockEntity platform) {
        if (!(level instanceof ServerLevel serverLevel) || platform.ticking
                || !platform.worldPosition.equals(pos) || platform.loadedState(serverLevel) != state) {
            return;
        }
        platform.ticking = true;
        try {
            tickCurrent(serverLevel, pos, state, platform);
        } finally {
            platform.ticking = false;
        }
    }

    private static void tickCurrent(ServerLevel level, BlockPos pos, BlockState state,
                                    WindCurrentPlatformBlockEntity platform) {
        int revision = platform.stateRevision;
        if (platform.activeTicks <= 0) {
            if (state.getValue(WindCurrentPlatformBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(WindCurrentPlatformBlock.ACTIVE, Boolean.FALSE), 3);
            }
            return;
        }
        platform.activeTicks--;
        if (platform.activeTicks == 0 || platform.activeTicks % 20 == 0) {
            platform.setChanged();
        }
        if (!platform.isCurrentTick(level, state, revision)) {
            return;
        }
        if (platform.activeTicks > 0 && !state.getValue(WindCurrentPlatformBlock.ACTIVE)) {
            state = state.setValue(WindCurrentPlatformBlock.ACTIVE, Boolean.TRUE);
            if (!level.setBlock(pos, state, 3)) {
                return;
            }
        }
        if (!platform.isCurrentTick(level, state, revision)) {
            return;
        }
        long gameTime = level.getGameTime();
        int height = ServerConfig.WIND_PLATFORM_LIFT_HEIGHT.get();
        AABB column = new AABB(pos.getX() - 0.75, pos.getY() + 0.2, pos.getZ() - 0.75,
                pos.getX() + 1.75, pos.getY() + height, pos.getZ() + 1.75);
        for (Entity entity : level.getEntities(null, column)) {
            if (!platform.isCurrentTick(level, state, revision)) {
                return;
            }
            if (!isWindTarget(level, column, entity)) {
                continue;
            }
            if (!shouldApplyForce(entity, gameTime)) {
                continue;
            }
            if (entity instanceof ServerPlayer player) {
                refreshSpeedBuff(player);
            }
            if (!platform.isCurrentTick(level, state, revision)) {
                return;
            }
            if (!isWindTarget(level, column, entity)) {
                continue;
            }
            Vec3 motion = entity.getDeltaMovement();
            Vec3 boosted = entity instanceof ServerPlayer player ? addHorizontalDraft(player, motion) : motion;
            if (boosted.y < LIFT_MAX_SPEED) {
                boosted = new Vec3(boosted.x, Math.min(LIFT_MAX_SPEED, boosted.y + LIFT_ACCEL), boosted.z);
            }
            entity.setDeltaMovement(boosted);
            entity.hasImpulse = true;
            entity.fallDistance = 0.0F;
            if (entity instanceof ServerPlayer player) {
                player.hurtMarked = true;
            }
        }

        if (platform.isCurrentTick(level, state, revision)) {
            MechanismFx.emit(level, ExplorationFx.windFrame(
                    Vec3.atBottomCenterOf(pos), height, gameTime, pos.asLong()));
        }
    }

    @Nullable
    private BlockState loadedState(ServerLevel serverLevel) {
        if (this.level != serverLevel || isRemoved()) {
            return null;
        }
        var chunk = serverLevel.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        if (chunk == null || chunk.getBlockEntities().get(worldPosition) != this) {
            return null;
        }
        BlockState current = chunk.getBlockState(worldPosition);
        return current.getBlock() instanceof WindCurrentPlatformBlock ? current : null;
    }

    private boolean isCurrentTick(ServerLevel serverLevel, BlockState state, int revision) {
        // 停用后重新激活可能恢复相同方块状态，旧 tick 仍须退出。
        return stateRevision == revision && loadedState(serverLevel) == state;
    }

    private static boolean isWindTarget(ServerLevel level, AABB column, Entity entity) {
        return entity.level() == level && entity.isAlive() && !entity.isRemoved()
                && !entity.isNoGravity() && !entity.isPassenger() && column.intersects(entity.getBoundingBox())
                && (!(entity instanceof ServerPlayer player)
                || (!player.isSpectator() && !player.getAbilities().flying && !player.hasDisconnected()));
    }

    private static Vec3 addHorizontalDraft(ServerPlayer player, Vec3 motion) {
        if (motion.x * motion.x + motion.z * motion.z < HORIZONTAL_DRAFT_MIN_INPUT_SPEED_SQR) {
            return motion;
        }
        Vec3 look = player.getLookAngle();
        double lookLengthSqr = look.x * look.x + look.z * look.z;
        if (lookLengthSqr < 1.0E-4D) {
            return motion;
        }
        double lookLength = Math.sqrt(lookLengthSqr);
        double draftX = look.x / lookLength;
        double draftZ = look.z / lookLength;
        double alongDraft = motion.x * draftX + motion.z * draftZ;
        if (alongDraft >= HORIZONTAL_DRAFT_MAX_SPEED) {
            return motion;
        }
        double addedDraft = Math.min(HORIZONTAL_DRAFT_ACCEL,
                HORIZONTAL_DRAFT_MAX_SPEED - alongDraft);
        return new Vec3(motion.x + draftX * addedDraft, motion.y,
                motion.z + draftZ * addedDraft);
    }

    private static boolean shouldApplyForce(Entity entity, long gameTime) {
        Long previous = LAST_FORCE_TICKS.put(entity, gameTime);
        return previous == null || previous.longValue() != gameTime;
    }

    private static void refreshSpeedBuff(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (current != null && (current.getAmplifier() > 0
                || current.getDuration() < 0
                || current.getDuration() > SPEED_BUFF_REFRESH_THRESHOLD_TICKS)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                SPEED_BUFF_TICKS, 0, true, false, true));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            LOADED_PLATFORMS.computeIfAbsent(this.level.dimension(),
                    key -> ConcurrentHashMap.newKeySet()).add(this.worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        unregister();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregister();
        super.onChunkUnloaded();
    }

    private void unregister() {
        if (this.level != null && !this.level.isClientSide) {
            Set<BlockPos> positions = LOADED_PLATFORMS.get(this.level.dimension());
            if (positions != null) {
                positions.remove(this.worldPosition);
                if (positions.isEmpty()) {
                    LOADED_PLATFORMS.remove(this.level.dimension(), positions);
                }
            }
        }
    }

    /** 服务器停止时清理运行态，避免跨世界保留旧维度坐标或施力记录。 */
    public static void clearLoadedPlatforms() {
        LOADED_PLATFORMS.clear();
        LAST_FORCE_TICKS.clear();
    }

    /** 最近的已加载风流平台(风暴罗盘"风流"词条用);无 → null。 */
    @Nullable
    public static BlockPos nearestPlatform(ServerLevel level, BlockPos origin) {
        Set<BlockPos> positions = LOADED_PLATFORMS.get(level.dimension());
        if (positions == null || positions.isEmpty()) {
            return null;
        }
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            double dist = pos.distSqr(origin);
            if (dist < nearestDist) {
                nearest = pos;
                nearestDist = dist;
            }
        }
        return nearest;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ActiveTicks", this.activeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stateRevision++;
        this.activeTicks = Math.max(0, Math.min(tag.getInt("ActiveTicks"),
                ServerConfig.WIND_PLATFORM_ACTIVE_TICKS.get()));
    }
}
