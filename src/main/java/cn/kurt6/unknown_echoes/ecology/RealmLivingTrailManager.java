package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.item.RealmFieldLensItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 维护谐生观测镜发起的短时活体寻迹，不持有全局实体引用。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class RealmLivingTrailManager {
    public static final int TRAIL_DURATION_TICKS = 400;
    public static final int REPATH_INTERVAL_TICKS = 10;
    public static final double ARRIVAL_DISTANCE_SQR = 6.25D;

    private static final double OBSERVER_NOTICE_RADIUS_SQR = 48.0D * 48.0D;
    /** 寻迹生物的快路索引。getPersistentData() 首次调用即分配 CompoundTag,不能对每只动物都调。 */
    private static final Set<UUID> ACTIVE_TRAIL_MOBS = ConcurrentHashMap.newKeySet();
    private static final String END_KEY = "ue_living_trail_end";
    private static final String TARGET_X_KEY = "ue_living_trail_x";
    private static final String TARGET_Y_KEY = "ue_living_trail_y";
    private static final String TARGET_Z_KEY = "ue_living_trail_z";
    private static final String MODE_KEY = "ue_living_trail_mode";
    private static final String OBSERVER_KEY = "ue_living_trail_observer";

    private RealmLivingTrailManager() {
    }

    public static void startTrail(ServerLevel level, ServerPlayer observer,
                                  PathfinderMob mob, BlockPos target,
                                  RealmHabitatChimeBlock.Mode mode) {
        ACTIVE_TRAIL_MOBS.add(mob.getUUID());
        CompoundTag data = mob.getPersistentData();
        data.putLong(END_KEY, safeDeadline(level.getGameTime(), TRAIL_DURATION_TICKS));
        data.putInt(TARGET_X_KEY, target.getX());
        data.putInt(TARGET_Y_KEY, target.getY());
        data.putInt(TARGET_Z_KEY, target.getZ());
        data.putInt(MODE_KEY, mode.ordinal());
        data.putUUID(OBSERVER_KEY, observer.getUUID());
    }

    public static boolean hasActiveTrail(PathfinderMob mob) {
        return ACTIVE_TRAIL_MOBS.contains(mob.getUUID())
                && mob.getPersistentData().contains(END_KEY, Tag.TAG_LONG);
    }

    /** 带寻迹 NBT 的实体回到世界时补登记,否则快路索引会永久跳过它们。先过维度与类型判断再读 NBT。 */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        RealmHabitatChimeBlock.Mode mode = RealmHabitatChimeBlock.modeForDimension(level);
        if (mode == null || !RealmHabitatChimeBlock.matchesMode(mob, mode)) {
            return;
        }
        if (mob.getPersistentData().contains(END_KEY, Tag.TAG_LONG)) {
            ACTIVE_TRAIL_MOBS.add(mob.getUUID());
        }
    }

    /** 服务器停止时清空运行态索引，权威 NBT 不动。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_TRAIL_MOBS.clear();
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (ACTIVE_TRAIL_MOBS.isEmpty()
                || !(event.getEntity() instanceof PathfinderMob mob)
                || !(mob.level() instanceof ServerLevel level)) {
            return;
        }

        long phase = Math.floorMod((long) mob.tickCount, (long) REPATH_INTERVAL_TICKS)
                + Math.floorMod((long) mob.getId(), (long) REPATH_INTERVAL_TICKS);
        if (Math.floorMod(phase, (long) REPATH_INTERVAL_TICKS) != 0L) {
            return;
        }
        if (!ACTIVE_TRAIL_MOBS.contains(mob.getUUID())) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(END_KEY, Tag.TAG_LONG)) {

            ACTIVE_TRAIL_MOBS.remove(mob.getUUID());
            return;
        }

        long now = level.getGameTime();
        BlockPos target = new BlockPos(data.getInt(TARGET_X_KEY),
                data.getInt(TARGET_Y_KEY), data.getInt(TARGET_Z_KEY));
        RealmHabitatChimeBlock.Mode mode = readMode(data);
        long end = data.getLong(END_KEY);
        long maximumEnd = safeDeadline(now, TRAIL_DURATION_TICKS);
        if (end > maximumEnd) {
            end = maximumEnd;
            data.putLong(END_KEY, end);
        }
        if (now >= end) {
            notifyObserver(level, mob, data,
                    "message.unknown_echoes.realm_field_lens.trail_faded");
            mob.getNavigation().stop();
            clearTrail(mob);
            return;
        }
        if (mode == null || RealmHabitatChimeBlock.modeForDimension(level) != mode
                || mob.getTarget() != null || mob.isLeashed()
                || mob.isPassenger() || mob.isVehicle()) {
            mob.getNavigation().stop();
            clearTrail(mob);
            return;
        }
        if (!level.hasChunkAt(target)
                || !RealmFieldLensItem.isForageTarget(level.getBlockState(target), mode)) {
            mob.getNavigation().stop();
            clearTrail(mob);
            return;
        }
        if (mob.distanceToSqr(Vec3.atCenterOf(target)) <= ARRIVAL_DISTANCE_SQR) {
            mob.getNavigation().stop();
            presentArrival(level, mob, target, mode);
            clearTrail(mob);
            return;
        }
        mob.getNavigation().moveTo(target.getX() + 0.5D,
                target.getY(), target.getZ() + 0.5D,
                RealmFieldLensItem.trailSpeed(mode));
    }

    private static RealmHabitatChimeBlock.Mode readMode(CompoundTag data) {
        int ordinal = data.getInt(MODE_KEY);
        RealmHabitatChimeBlock.Mode[] modes = RealmHabitatChimeBlock.Mode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : null;
    }

    private static void notifyObserver(ServerLevel level, PathfinderMob mob,
                                       CompoundTag data, String translationKey) {
        if (!data.hasUUID(OBSERVER_KEY)) {
            return;
        }
        ServerPlayer observer = level.getServer().getPlayerList()
                .getPlayer(data.getUUID(OBSERVER_KEY));
        if (observer != null && observer.level() == level
                && observer.distanceToSqr(mob) <= OBSERVER_NOTICE_RADIUS_SQR) {
            observer.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private static void presentArrival(ServerLevel level, PathfinderMob mob,
                                       BlockPos target, RealmHabitatChimeBlock.Mode mode) {
        var particle = switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
        level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D,
                mob.getZ(), 18, 0.45D, 0.25D, 0.45D, 0.025D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX() + 0.5D, target.getY() + 0.6D, target.getZ() + 0.5D,
                12, 0.3D, 0.2D, 0.3D, 0.02D);
        level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 0.7F,
                mode == RealmHabitatChimeBlock.Mode.INTERLAYER ? 0.75F : 1.55F);
    }

    private static void clearTrail(PathfinderMob mob) {
        ACTIVE_TRAIL_MOBS.remove(mob.getUUID());
        CompoundTag data = mob.getPersistentData();
        data.remove(END_KEY);
        data.remove(TARGET_X_KEY);
        data.remove(TARGET_Y_KEY);
        data.remove(TARGET_Z_KEY);
        data.remove(MODE_KEY);
        data.remove(OBSERVER_KEY);
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }
}
