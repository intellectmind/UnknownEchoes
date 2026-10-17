package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** 按实际世界 tick 播放信标动画；同一信标只保留当前序列。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class AbyssBeaconEffects {
    private static final int MAX_ACTIVE_PER_LEVEL = 64;
    private static final Map<ServerLevel, LinkedHashMap<BlockPos, Playback>> ACTIVE = new WeakHashMap<>();

    private AbyssBeaconEffects() { }

    public static void playCalibration(ServerLevel level, BlockPos pos,
                                       EchoAbilityType ability, ParticleOptions primary) {
        start(level, pos, new Playback(level.getGameTime() + 1, ability, primary));
    }

    public static void playActivation(ServerLevel level, BlockPos pos) {
        start(level, pos, new Playback(level.getGameTime() + 1, null, null));
    }

    private static void start(ServerLevel level, BlockPos pos, Playback playback) {
        if (!canPlayFx(level, pos)) return;
        LinkedHashMap<BlockPos, Playback> animations = ACTIVE.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        animations.remove(pos);
        if (animations.size() >= MAX_ACTIVE_PER_LEVEL) animations.pollFirstEntry();
        animations.put(pos.immutable(), playback);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LinkedHashMap<BlockPos, Playback> animations = ACTIVE.get(level);
        if (animations == null) return;
        long now = level.getGameTime();
        for (var entry : Map.copyOf(animations).entrySet()) {
            BlockPos pos = entry.getKey();
            Playback playback = entry.getValue();
            if (animations.get(pos) != playback) continue;
            int spacing = playback.ability == null ? 2 : 1;
            int lastFrame = playback.ability == null ? 30 : 22;
            long age = now - playback.startTime;
            if (now < playback.startTime - 1 || age > lastFrame * spacing) {
                animations.remove(pos, playback);
                continue;
            }
            if (age < 0 || age % spacing != 0) continue;
            if (!canPlayFx(level, pos)) {
                animations.remove(pos, playback);
                continue;
            }
            int frame = (int) age / spacing;
            if (frame <= playback.lastFrame) continue;
            playback.lastFrame = frame;
            if (playback.ability == null) activationFrame(level, pos, frame);
            else calibrationFrame(level, pos, playback.ability, playback.primary, frame);
            if (frame == lastFrame) animations.remove(pos, playback);
        }
        if (animations.isEmpty() && ACTIVE.get(level) == animations) ACTIVE.remove(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ACTIVE.remove(level);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); }

    private static final class Playback {
        private final long startTime;
        private final EchoAbilityType ability;
        private final ParticleOptions primary;
        private int lastFrame = -1;

        private Playback(long startTime, EchoAbilityType ability, ParticleOptions primary) {
            this.startTime = startTime;
            this.ability = ability;
            this.primary = primary;
        }
    }

    private static void calibrationFrame(ServerLevel level, BlockPos pos,
                                         EchoAbilityType ability, ParticleOptions primary, int frameIndex) {
        ParticleOptions accent = switch (ability) {
            case WIND_ECHO -> ParticleTypes.END_ROD;
            case TIDE_ECHO -> ParticleTypes.NAUTILUS;
            case TRUE_SIGHT_ECHO -> ParticleTypes.GLOW;
            case GRAVITY_ECHO -> ParticleTypes.ELECTRIC_SPARK;
            case VOID_ECHO -> ParticleTypes.REVERSE_PORTAL;
        };
        double progress = frameIndex / 22.0D;
        double radius = 1.35D - progress * 0.95D;
        double height = 0.25D + progress * 3.4D;
        double direction = ability == EchoAbilityType.TIDE_ECHO ? -1.0D : 1.0D;
        double phase = frameIndex * 0.42D * direction;
        for (int arm = 0; arm < 5; arm++) {
            double angle = phase + arm * Math.PI * 2.0D / 5.0D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
            level.sendParticles(primary, x, pos.getY() + height, z,
                    2, 0.035D, 0.035D, 0.035D, 0.015D);
            if ((frameIndex + arm) % 3 == 0) {
                level.sendParticles(accent, x, pos.getY() + height + 0.12D, z,
                        1, 0.02D, 0.03D, 0.02D, 0.01D);
            }
        }
        if (frameIndex % 5 == 0) {
            emitRing(level, pos, primary, 0.7D + progress * 1.5D,
                    0.35D + progress * 1.2D, 20);
        }
    }

    private static void activationFrame(ServerLevel level, BlockPos pos, int frameIndex) {
        double progress = frameIndex / 30.0D;
        double pulse = Math.sin(progress * Math.PI);
        double radius = 0.55D + pulse * 2.5D;
        double y = 0.45D + progress * 3.8D;
        double phase = frameIndex * 0.28D;
        for (int arm = 0; arm < 5; arm++) {
            double angle = phase + arm * Math.PI * 2.0D / 5.0D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
            ParticleOptions echoParticle = switch (arm) {
                case 0 -> ParticleTypes.CLOUD;
                case 1 -> ParticleTypes.SPLASH;
                case 2 -> ParticleTypes.ENCHANT;
                case 3 -> ParticleTypes.ELECTRIC_SPARK;
                default -> ParticleTypes.REVERSE_PORTAL;
            };
            level.sendParticles(echoParticle, x, pos.getY() + y, z,
                    2, 0.025D, 0.04D, 0.025D, 0.01D);
            level.sendParticles(ParticleTypes.END_ROD, x, pos.getY() + y + 0.08D, z,
                    1, 0.015D, 0.025D, 0.015D, 0.005D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D - Math.cos(angle) * radius * 0.65D,
                    pos.getY() + y * 0.82D,
                    pos.getZ() + 0.5D - Math.sin(angle) * radius * 0.65D,
                    1, 0.025D, 0.04D, 0.025D, 0.02D);
        }
        if (frameIndex % 6 == 0) {
            emitRing(level, pos, ParticleTypes.ENCHANT,
                    0.8D + progress * 2.6D, 0.3D + progress * 0.9D, 28);
        }
        if (frameIndex == 12) {
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.BLOCKS, 1.4F, 0.7F);
        } else if (frameIndex == 24) {
            level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.BLOCKS, 1.8F, 1.1F);
        } else if (frameIndex == 30) {
            double x = pos.getX() + 0.5D;
            double yTop = pos.getY() + 3.6D;
            double z = pos.getZ() + 0.5D;
            level.sendParticles(ParticleTypes.FLASH, x, yTop, z,
                    3, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, x, yTop, z,
                    110, 1.8D, 2.1D, 1.8D, 0.22D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, yTop, z,
                    140, 2.1D, 2.3D, 2.1D, 0.3D);
        }
    }

    private static boolean canPlayFx(ServerLevel level, BlockPos pos) {
        return AbyssBeaconBlock.isBeaconPresent(level, pos);
    }

    private static void emitRing(ServerLevel level, BlockPos pos, ParticleOptions particle,
                                 double radius, double height, int points) {
        for (int point = 0; point < points; point++) {
            double angle = point * Math.PI * 2.0D / points;
            level.sendParticles(particle,
                    pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + height,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius,
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }
}
