package cn.kurt6.unknown_echoes.ecology;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.block.CommunalEchoTableBlock;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.block.RealmServicePlinthBlock;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 将共餐台、栖鸣台和服务台组成一次个人化的三域公共设施调谐。 */
public final class RealmFacilityCircuitManager {
    public static final int TABLE_LINK_RADIUS = 12;
    public static final int TABLE_VERTICAL_RADIUS = 8;
    public static final int CHIME_LINK_RADIUS = 24;
    public static final int CHIME_VERTICAL_RADIUS = 32;
    public static final int COOLDOWN_TICKS = 12000;
    public static final int SUPPORT_TICKS = 600;
    public static final int ENERGY_REFILL = 8;
    public static final int MAX_ACTIVE_CIRCUITS_PER_LEVEL = 32;

    private static final String COOLDOWN_KEY_PREFIX =
            "unknown_echoes.realm_facility_circuit.";
    private static final Map<ServerLevel, List<ActiveCircuit>> ACTIVE_CIRCUITS =
            new WeakHashMap<>();

    private RealmFacilityCircuitManager() {
    }

    /** 服务器停止时清空运行态联动:弱键只保证可回收,残留引用会把上个存档的坐标带进下一个存档。 */
    public static void clearAll() {
        ACTIVE_CIRCUITS.clear();
    }

    public static void arm(ServerLevel level, BlockPos tablePos, BlockPos chimePos,
                           CommunalEchoTableBlock.Mode feastMode) {
        RealmServicePlinthBlock.Mode mode = modeForFeast(feastMode);
        if (mode == null) {
            return;
        }

        long now = level.getServer().overworld().getGameTime();
        long maximumEnd = safeDeadline(now, RealmHabitatChimeBlock.ACTIVE_TICKS);
        List<ActiveCircuit> circuits = ACTIVE_CIRCUITS.computeIfAbsent(
                level, ignored -> new ArrayList<>());
        circuits.removeIf(circuit -> circuit.endGameTime() <= now
                || circuit.endGameTime() > maximumEnd
                || circuit.tablePos().equals(tablePos));
        if (circuits.size() >= MAX_ACTIVE_CIRCUITS_PER_LEVEL) {
            circuits.sort(Comparator.comparingLong(ActiveCircuit::endGameTime));
            circuits.remove(0);
        }
        circuits.add(new ActiveCircuit(tablePos.immutable(), chimePos.immutable(), mode,
                safeDeadline(now, RealmHabitatChimeBlock.ACTIVE_TICKS)));
    }

    public static Result tryTune(ServerLevel level, BlockPos plinthPos,
                                 ServerPlayer player, RealmServicePlinthBlock.Mode mode) {
        FacilityLink link = findLink(level, plinthPos, mode);
        if (link == null) {
            return Result.NOT_LINKED;
        }

        long now = level.getServer().overworld().getGameTime();
        String cooldownKey = cooldownKey(mode);
        long readyAt = player.getPersistentData().getLong(cooldownKey);
        long maximumReadyAt = safeDeadline(now, COOLDOWN_TICKS);
        if (readyAt > maximumReadyAt) {
            readyAt = maximumReadyAt;
            player.getPersistentData().putLong(cooldownKey, readyAt);
        }
        if (readyAt > now) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_service_plinth.circuit.resting"), true);
            return Result.COOLDOWN;
        }
        player.getPersistentData().remove(cooldownKey);

        applySupport(player, mode);
        player.getPersistentData().putLong(cooldownKey, maximumReadyAt);
        playCircuit(level, plinthPos, link, mode);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.realm_service_plinth.circuit.tuned."
                        + mode.getSerializedName()), true);
        return Result.TUNED;
    }

    public static void copyCooldowns(Player source, Player target) {
        long now = target.level() instanceof ServerLevel serverLevel
                ? serverLevel.getServer().overworld().getGameTime()
                : Math.max(0L, target.level().getGameTime());
        long maximumReadyAt = safeDeadline(now, COOLDOWN_TICKS);
        for (RealmServicePlinthBlock.Mode mode : RealmServicePlinthBlock.Mode.values()) {
            String key = cooldownKey(mode);
            if (source.getPersistentData().contains(key)) {
                long readyAt = source.getPersistentData().getLong(key);
                target.getPersistentData().putLong(key,
                        Math.min(readyAt, maximumReadyAt));
            } else {
                target.getPersistentData().remove(key);
            }
        }
    }

    private static FacilityLink findLink(ServerLevel level, BlockPos plinthPos,
                                         RealmServicePlinthBlock.Mode mode) {
        List<ActiveCircuit> circuits = ACTIVE_CIRCUITS.get(level);
        if (circuits == null || circuits.isEmpty()) {
            return null;
        }

        long now = level.getServer().overworld().getGameTime();
        long maximumEnd = safeDeadline(now, RealmHabitatChimeBlock.ACTIVE_TICKS);
        ActiveCircuit nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        Iterator<ActiveCircuit> iterator = circuits.iterator();
        while (iterator.hasNext()) {
            ActiveCircuit circuit = iterator.next();
            if (circuit.endGameTime() <= now || circuit.endGameTime() > maximumEnd) {
                iterator.remove();
                continue;
            }
            if (circuit.mode() != mode
                    || !within(circuit.tablePos(), plinthPos,
                    TABLE_LINK_RADIUS, TABLE_VERTICAL_RADIUS)
                    || !within(circuit.chimePos(), plinthPos,
                    CHIME_LINK_RADIUS, CHIME_VERTICAL_RADIUS)) {
                continue;
            }
            if (!isStillValid(level, circuit)) {
                iterator.remove();
                continue;
            }

            double distanceSqr = circuit.tablePos().distSqr(plinthPos)
                    + circuit.chimePos().distSqr(plinthPos);
            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = circuit;
            }
        }
        if (circuits.isEmpty()) {
            ACTIVE_CIRCUITS.remove(level);
        }
        return nearest == null ? null
                : new FacilityLink(nearest.tablePos(), nearest.chimePos());
    }

    private static boolean isStillValid(ServerLevel level, ActiveCircuit circuit) {
        if (!level.hasChunkAt(circuit.tablePos()) || !level.hasChunkAt(circuit.chimePos())) {
            return false;
        }
        BlockState tableState = level.getBlockState(circuit.tablePos());
        BlockState chimeState = level.getBlockState(circuit.chimePos());
        return tableState.getBlock() instanceof CommunalEchoTableBlock
                && tableState.getValue(CommunalEchoTableBlock.MODE) == tableMode(circuit.mode())
                && chimeState.getBlock() instanceof RealmHabitatChimeBlock
                && chimeState.getValue(RealmHabitatChimeBlock.MODE) == chimeMode(circuit.mode())
                && chimeState.getValue(RealmHabitatChimeBlock.ACTIVE);
    }

    private static boolean within(BlockPos candidate, BlockPos origin,
                                  int horizontalRadius, int verticalRadius) {
        long xOffset = (long) candidate.getX() - origin.getX();
        long yOffset = (long) candidate.getY() - origin.getY();
        long zOffset = (long) candidate.getZ() - origin.getZ();
        return Math.abs(yOffset) <= verticalRadius
                && xOffset * xOffset + zOffset * zOffset
                <= (long) horizontalRadius * horizontalRadius;
    }

    private static void applySupport(ServerPlayer player, RealmServicePlinthBlock.Mode mode) {
        switch (mode) {
            case WAYFARER -> {
                player.getFoodData().eat(4, 0.6F);
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, SUPPORT_TICKS, 0, true, false, true));
            }
            case CONFLUENCE -> {
                player.removeEffect(MobEffects.BLINDNESS);
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, SUPPORT_TICKS, 0, true, false, true));
                ArtifactManager.refillEnergy(player, ENERGY_REFILL);
            }
            case INTERLAYER -> {
                player.removeEffect(MobEffects.DARKNESS);
                player.fallDistance = 0.0F;
                player.heal(4.0F);
                FoodEffectSupport.applyAbsorption(player, SUPPORT_TICKS, 0, 4.0F);
            }
        }
    }

    private static void playCircuit(ServerLevel level, BlockPos plinthPos, FacilityLink link,
                                    RealmServicePlinthBlock.Mode mode) {
        ParticleOptions particle = particleFor(mode);
        sendLink(level, plinthPos, link.tablePos(), particle);
        sendLink(level, plinthPos, link.chimePos(), particle);
        level.sendParticles(particle,
                plinthPos.getX() + 0.5D, plinthPos.getY() + 1.2D,
                plinthPos.getZ() + 0.5D,
                48, 0.7D, 0.6D, 0.7D, 0.045D);
        level.playSound(null, plinthPos, switch (mode) {
            case WAYFARER -> SoundEvents.BELL_BLOCK;
            case CONFLUENCE -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case INTERLAYER -> SoundEvents.RESPAWN_ANCHOR_AMBIENT;
        }, SoundSource.BLOCKS, 1.0F, switch (mode) {
            case WAYFARER -> 1.25F;
            case CONFLUENCE -> 1.55F;
            case INTERLAYER -> 0.72F;
        });
    }

    private static void sendLink(ServerLevel level, BlockPos from, BlockPos to,
                                 ParticleOptions particle) {
        Vec3 start = Vec3.atCenterOf(from).add(0.0D, 0.65D, 0.0D);
        Vec3 end = Vec3.atCenterOf(to).add(0.0D, 0.55D, 0.0D);
        for (int step = 1; step <= 18; step++) {
            double progress = step / 18.0D;
            Vec3 point = start.lerp(end, progress);
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    private static CommunalEchoTableBlock.Mode tableMode(RealmServicePlinthBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> CommunalEchoTableBlock.Mode.FAR_BELL;
            case CONFLUENCE -> CommunalEchoTableBlock.Mode.CANOPY;
            case INTERLAYER -> CommunalEchoTableBlock.Mode.INTERLAYER;
        };
    }

    private static RealmServicePlinthBlock.Mode modeForFeast(
            CommunalEchoTableBlock.Mode mode) {
        return switch (mode) {
            case FAR_BELL -> RealmServicePlinthBlock.Mode.WAYFARER;
            case CANOPY -> RealmServicePlinthBlock.Mode.CONFLUENCE;
            case INTERLAYER -> RealmServicePlinthBlock.Mode.INTERLAYER;
            case IDLE -> null;
        };
    }

    private static RealmHabitatChimeBlock.Mode chimeMode(RealmServicePlinthBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> RealmHabitatChimeBlock.Mode.WAYFARER;
            case CONFLUENCE -> RealmHabitatChimeBlock.Mode.CONFLUENCE;
            case INTERLAYER -> RealmHabitatChimeBlock.Mode.INTERLAYER;
        };
    }

    private static ParticleOptions particleFor(RealmServicePlinthBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
    }

    private static String cooldownKey(RealmServicePlinthBlock.Mode mode) {
        return COOLDOWN_KEY_PREFIX + mode.getSerializedName() + ".ready_at";
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    public enum Result {
        TUNED,
        COOLDOWN,
        NOT_LINKED
    }

    private record FacilityLink(BlockPos tablePos, BlockPos chimePos) {
    }

    private record ActiveCircuit(BlockPos tablePos, BlockPos chimePos,
                                 RealmServicePlinthBlock.Mode mode, long endGameTime) {
    }
}
