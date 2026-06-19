package cn.kurt6.unknown_echoes.block.wind;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 风流平台 BlockEntity:激活倒计时 + 上升气流柱(服务端权威)。
 * 气流对柱内全部实体生效(运动效果不查权限,5.2 边界);玩家速度经 hurtMarked 同步。
 * 已加载平台位置登记进静态注册表,供风暴罗盘"风流"词条指示最近平台
 * (只覆盖已加载区块,符合 12.3"不全图扫描未生成结构"的限制)。
 */
public class WindCurrentPlatformBlockEntity extends BlockEntity {

    /** 已加载的平台位置(服务端,按维度);BlockEntity 加载/移除时维护。 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED_PLATFORMS = new ConcurrentHashMap<>();

    /** 气流抬升:每次结算增加的竖直速度与上限。 */
    private static final double LIFT_ACCEL = 0.20D;
    private static final double LIFT_MAX_SPEED = 1.15D;
    /** 顺风推进:玩家看向方向的水平加速与速度上限;潜行时只保留竖直抬升。 */
    private static final double HORIZONTAL_DRAFT_ACCEL = 0.075D;
    private static final double HORIZONTAL_DRAFT_MAX_SPEED = 0.65D;
    /** 离开气流后仍短暂保留的速度感。 */
    private static final int SPEED_BUFF_TICKS = 60;
    private static final int SPEED_BUFF_REFRESH_THRESHOLD_TICKS = 40;

    private int activeTicks = 0;

    public WindCurrentPlatformBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_CURRENT_PLATFORM.get(), pos, state);
    }

    public void activate(int ticks) {
        this.activeTicks = Math.max(this.activeTicks, ticks);
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  WindCurrentPlatformBlockEntity platform) {
        if (platform.activeTicks <= 0) {
            if (state.getValue(WindCurrentPlatformBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(WindCurrentPlatformBlock.ACTIVE, Boolean.FALSE), 3);
            }
            return;
        }
        platform.activeTicks--;
        if (platform.activeTicks == 0) {
            platform.setChanged();
        }
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 2 != 0) {
            return;
        }
        int height = ServerConfig.WIND_PLATFORM_LIFT_HEIGHT.get();
        AABB column = new AABB(pos.getX() - 0.75, pos.getY() + 0.2, pos.getZ() - 0.75,
                pos.getX() + 1.75, pos.getY() + height, pos.getZ() + 1.75);
        for (Entity entity : serverLevel.getEntities(null, column)) {
            Vec3 motion = entity.getDeltaMovement();
            Vec3 boosted = motion;
            if (entity instanceof ServerPlayer player) {
                boosted = addHorizontalDraft(player, boosted);
                refreshSpeedBuff(player);
            }
            if (boosted.y < LIFT_MAX_SPEED) {
                boosted = new Vec3(boosted.x, Math.min(LIFT_MAX_SPEED, boosted.y + LIFT_ACCEL), boosted.z);
            }
            entity.setDeltaMovement(boosted);
            entity.fallDistance = 0.0F;
            if (entity instanceof ServerPlayer player) {
                player.hurtMarked = true; // 服务端速度变更同步到客户端
            }
        }
        // 螺旋风粒子(10.5 风流平台启动演出);随机相位避免多平台同步旋转的机械感
        if (level.getGameTime() % 4 == 0) {
            double phase = (level.getGameTime() / 4.0 + pos.hashCode() % 17) * 0.7;
            for (int i = 0; i < 3; i++) {
                double y = pos.getY() + 0.5 + (level.getGameTime() / 4.0 * 1.3 + i * height / 3.0) % height;
                double angle = phase + i * Math.PI * 2 / 3;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 + Math.cos(angle) * 0.7, y, pos.getZ() + 0.5 + Math.sin(angle) * 0.7,
                        1, 0.05, 0.1, 0.05, 0.01);
            }
        }
    }

    private static Vec3 addHorizontalDraft(ServerPlayer player, Vec3 motion) {
        if (player.isShiftKeyDown()) {
            return motion;
        }
        double currentSpeedSqr = motion.x * motion.x + motion.z * motion.z;
        double maxSpeedSqr = HORIZONTAL_DRAFT_MAX_SPEED * HORIZONTAL_DRAFT_MAX_SPEED;
        if (currentSpeedSqr >= maxSpeedSqr) {
            return motion;
        }
        Vec3 look = player.getLookAngle();
        double lookLengthSqr = look.x * look.x + look.z * look.z;
        if (lookLengthSqr < 1.0E-4D) {
            return motion;
        }
        double lookLength = Math.sqrt(lookLengthSqr);
        double nextX = motion.x + look.x / lookLength * HORIZONTAL_DRAFT_ACCEL;
        double nextZ = motion.z + look.z / lookLength * HORIZONTAL_DRAFT_ACCEL;
        double nextSpeed = Math.sqrt(nextX * nextX + nextZ * nextZ);
        if (nextSpeed > HORIZONTAL_DRAFT_MAX_SPEED) {
            double scale = HORIZONTAL_DRAFT_MAX_SPEED / nextSpeed;
            nextX *= scale;
            nextZ *= scale;
        }
        return new Vec3(nextX, motion.y, nextZ);
    }

    private static void refreshSpeedBuff(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (current != null && (current.getAmplifier() > 0
                || current.getDuration() > SPEED_BUFF_REFRESH_THRESHOLD_TICKS)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                SPEED_BUFF_TICKS, 0, true, false, true));
    }

    // ---- 罗盘用平台注册表(只登记已加载区块) ----

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
            }
        }
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
        this.activeTicks = tag.getInt("ActiveTicks");
    }
}
