package cn.kurt6.unknown_echoes.worldevent;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/** 三域低频环境活动的局部效果与表现；触发频率仍由统一世界事件框架负责。 */
public final class RealmAmbientEventManager {
    private static final int NIGHT_VISION_DURATION_TICKS =
            NightVisionSupport.MIN_SUSTAINED_DURATION_TICKS;

    public static final int EVENT_RADIUS = 32;

    private RealmAmbientEventManager() {
    }

    public static boolean supports(WorldEventType type) {
        return switch (type) {
            case METEOR_AFTERGLOW, ECHO_STORM, STRATA_AFTERSHOCK -> true;
            default -> false;
        };
    }

    public static void present(ServerLevel level, WorldEventType type, BlockPos center,
                               List<ServerPlayer> nearbyPlayers) {
        switch (type) {
            case METEOR_AFTERGLOW -> presentMeteorAfterglow(level, center, nearbyPlayers);
            case ECHO_STORM -> presentEchoStorm(level, center, nearbyPlayers);
            case STRATA_AFTERSHOCK -> presentStrataAftershock(level, center, nearbyPlayers);
            default -> {
            }
        }
    }

    /** 夜间星痕提供辨路窗口，不生成坠星点或矿物。 */
    private static void presentMeteorAfterglow(ServerLevel level, BlockPos center,
        List<ServerPlayer> nearbyPlayers) {
        for (ServerPlayer player : nearbyPlayers) {
            refreshNightVision(level, player);
            addEffectIfMissing(player, MobEffects.LUCK, 60);
        }

        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            RandomSource random = level.random;
            for (int streak = 0; streak < 6; streak++) {
                double x = center.getX() + random.nextGaussian() * 8.0D;
                double y = center.getY() + 5.0D + random.nextDouble() * 6.0D;
                double z = center.getZ() + random.nextGaussian() * 8.0D;
                for (int step = 0; step < 4; step++) {
                    level.sendParticles(step == 0
                                    ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                            x - step * 0.65D, y + step * 0.45D, z + step * 0.25D,
                            1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }
        if (Math.floorMod(level.getGameTime(), 180L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_METEOR_AFTERGLOW_PULSE.get(), 0.45F, 1.0F);
        }
    }

    /** 风暴只激化普通敌对生物，Boss 始终不受世界事件增益。 */
    private static void presentEchoStorm(ServerLevel level, BlockPos center, List<ServerPlayer> nearbyPlayers) {
        AABB area = new AABB(center).inflate(EVENT_RADIUS, 8.0D, EVENT_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity instanceof Enemy && entity.isAlive()
                        && !entity.getType().is(ModEntityTags.ECHO_BOSSES)
                        && !entity.getType().is(Tags.EntityTypes.BOSSES)
                        && withinEventRadius(entity, center))) {
            addEffectIfMissing(target, MobEffects.MOVEMENT_SPEED, 40);
            addEffectIfMissing(target, MobEffects.DAMAGE_BOOST, 40);
            addEffectIfMissing(target, MobEffects.GLOWING, 30);
        }

        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            level.sendParticles(ParticleTypes.CLOUD,
                    center.getX(), center.getY() + 7.0D, center.getZ(),
                    8, 12.0D, 1.0D, 12.0D, 0.04D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    center.getX(), center.getY() + 1.5D, center.getZ(),
                    10, 10.0D, 2.0D, 10.0D, 0.08D);
        }
        if (Math.floorMod(level.getGameTime(), 240L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_ECHO_STORM_PULSE.get(), 0.22F, 1.0F);
        }
    }

    /** 深层余震形成短时采掘与耐热窗口，不改变矿脉或地形。 */
    private static void presentStrataAftershock(ServerLevel level, BlockPos center,
                                                List<ServerPlayer> nearbyPlayers) {
        for (ServerPlayer player : nearbyPlayers) {
            addEffectIfMissing(player, MobEffects.DIG_SPEED, 60);
            addEffectIfMissing(player, MobEffects.FIRE_RESISTANCE, 60);
        }

        if (Math.floorMod(level.getGameTime(), 5L) == 0L) {
            level.sendParticles(ParticleTypes.ASH,
                    center.getX(), center.getY() + 4.0D, center.getZ(),
                    20, 12.0D, 3.0D, 12.0D, 0.015D);
            double offset = level.getGameTime() * 0.025D;
            for (int point = 0; point < 16; point++) {
                double angle = point * Math.PI / 8.0D + offset;
                double radius = 5.0D + Math.floorMod(point, 4) * 2.5D;
                level.sendParticles(point % 4 == 0
                                ? ParticleTypes.POOF : ParticleTypes.SCULK_SOUL,
                        center.getX() + Math.cos(angle) * radius,
                        center.getY() + 0.25D,
                        center.getZ() + Math.sin(angle) * radius,
                        1, 0.04D, 0.05D, 0.04D, 0.0D);
            }
        }
        if (Math.floorMod(level.getGameTime(), 140L) == 0L) {
            WorldEventSounds.pulse(nearbyPlayers, ModSounds.WORLD_EVENT_STRATA_AFTERSHOCK_PULSE.get(), 0.5F, 1.0F);
        }
    }

    /** 非夜视效果仅在缺失或即将过期时补发；夜视走独立阈值。 */
    private static void addEffectIfMissing(LivingEntity entity,
                                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current != null && (current.getAmplifier() > 0
                || current.getDuration() < 0
                || current.getDuration() > Math.max(1, duration / 2))) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
    }

    /** 事件表现每 tick 调用一次，夜视按 NightVisionSupport 的提前阈值补时长。 */
    private static void refreshNightVision(ServerLevel level, ServerPlayer player) {
        NightVisionSupport.sustain(player, NightVisionSupport.Source.AREA_EVENT,
                NIGHT_VISION_DURATION_TICKS, 1, true,
                level.getServer().overworld().getGameTime());
    }

    private static boolean withinEventRadius(LivingEntity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dz * dz <= EVENT_RADIUS * EVENT_RADIUS;
    }
}
