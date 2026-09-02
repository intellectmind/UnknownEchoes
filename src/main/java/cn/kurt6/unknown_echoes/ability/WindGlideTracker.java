package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.network.DoubleJumpPayload;
import cn.kurt6.unknown_echoes.network.GlideDenyPayload;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 服务端追踪风之能力的空中状态，防止伪造请求绕过冷却与摔落保护。 */
public class WindGlideTracker {

    /** 服务端 onGround 标记滞后时,用明显上升速度识别真实起飞。 */
    private static final double TAKEOFF_VERTICAL_GRACE_SPEED = 0.05D;
    /** 服务端滑翔时长在客户端公式(40+20×研究)之上的网络余量(tick)。 */
    private static final int GLIDE_DURATION_TOLERANCE = 20;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static class State {
        /** 连续在地面(含水中/攀爬/载具/飞行)的 tick 数;0 = 空中。 */
        int groundedTicks = 100;
        boolean doubleJumped;
        boolean gliding;
        int glideTicksRemaining;
        int landingCooldown;
        long lastGlideHoldSoundTick = -10L;
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    /** 服务器停止时清理瞬态飞行状态,避免单机世界切换串状态。 */
    public static void clearAll() {
        STATES.clear();
    }

    /** 每个服务端玩家 tick 调用(CommonEvents):维护离地计数、滑翔倒计时与落地冷却。 */
    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || !EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)) {
            STATES.remove(player.getUUID());
            return;
        }
        State s = state(player);
        boolean grounded = isGroundedForWind(player);
        if (grounded) {
            s.groundedTicks = Math.min(s.groundedTicks + 1, 100);
            if (s.gliding) {

                s.gliding = false;
                int cooldown = ServerConfig.WIND_GLIDE_LANDING_COOLDOWN_TICKS.get();
                if (EchoArmorSets.hasListenerAbilityBonus(player)) {
                    cooldown = cooldown * 7 / 10;
                }
                if (hasWindWalkerAbilityBonus(player)) {
                    cooldown = cooldown * 4 / 5;
                }
                if (EchoArmorSets.hasResonatorAbilityBonus(player)) {
                    cooldown = cooldown * 9 / 10;
                }
                s.landingCooldown = cooldown;
            }
            s.doubleJumped = false;
            s.glideTicksRemaining = 0;
        } else {
            s.groundedTicks = 0;
            if (s.gliding && --s.glideTicksRemaining <= 0) {
                s.gliding = false;
            }
        }
        if (s.landingCooldown > 0) {
            s.landingCooldown--;
        }
    }

    /** DoubleJumpPayload 服务端入口:能力校验之外补状态/能量/冷却校验。 */
    public static void handle(ServerPlayer player, byte mode) {
        if (player.isSpectator() || !EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)) {
            return;
        }
        State s = state(player);
        switch (mode) {
            case DoubleJumpPayload.MODE_JUMP -> {

                if (!isAirborneForWindAction(player) || s.doubleJumped) {
                    return;
                }
                s.doubleJumped = true;
                player.fallDistance = 0.0F;
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            case DoubleJumpPayload.MODE_GLIDE_START -> {
                if (JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO) < 1) {
                    deny(player, "research");
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "glide_research");
                    return;
                }
                if (s.gliding) {
                    return;
                }

                if (!s.doubleJumped || !isAirborneForWindAction(player)) {
                    deny(player, "state");
                    return;
                }
                if (s.landingCooldown > 0) {
                    deny(player, "cooldown");
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "glide_cooldown");
                    return;
                }
                int cost = windGlideEnergyCost(player);

                if (cost > 0 && !ArtifactManager.hasEnoughEnergy(player, cost)) {
                    deny(player, "energy");
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "glide_energy");
                    return;
                }
                if (cost > 0 && !ArtifactManager.spendEnergy(player, cost)) {
                    deny(player, "energy");
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "glide_energy");
                    return;
                }
                s.gliding = true;
                s.lastGlideHoldSoundTick = player.level().getGameTime();

                s.glideTicksRemaining = windGlideDuration(player)
                        + (EchoArmorSets.hasListenerAbilityBonus(player) ? 20 : 0)
                        + (EchoArmorSets.hasResonatorAbilityBonus(player) ? 10 : 0)
                        + GLIDE_DURATION_TOLERANCE;
                player.fallDistance = 0.0F;
                player.level().playSound(null, player.blockPosition(),
                        ModSounds.WIND_GLIDE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            case DoubleJumpPayload.MODE_GLIDE_HOLD -> {

                if (!s.gliding) {
                    return;
                }
                player.fallDistance = 0.0F;

                long now = player.level().getGameTime();
                if (s.lastGlideHoldSoundTick > now || now - s.lastGlideHoldSoundTick >= 10L) {
                    s.lastGlideHoldSoundTick = now;
                    player.level().playSound(null, player.blockPosition(),
                            ModSounds.WIND_GLIDE.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
                }
            }
            default -> {
            }
        }
    }

    /** 服务端是否认可该玩家正在滑翔(风流平台等机关复用)。 */
    public static boolean isGliding(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.gliding;
    }

    private static void deny(ServerPlayer player, String reason) {
        PacketDistributor.sendToPlayer(player, new GlideDenyPayload(reason));
    }

    private static boolean isAirborneForWindAction(ServerPlayer player) {
        if (player.isInWater() || player.isInLava()
                || player.getAbilities().flying || player.onClimbable() || player.isPassenger()) {
            return false;
        }
        return !player.onGround() || player.getDeltaMovement().y > TAKEOFF_VERTICAL_GRACE_SPEED;
    }

    private static boolean isGroundedForWind(ServerPlayer player) {
        return player.onGround() || player.isInWater() || player.isInLava()
                || player.getAbilities().flying || player.onClimbable() || player.isPassenger();
    }

    private static int windGlideDuration(ServerPlayer player) {
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO);
        return 40 + (research >= 2 ? 20 : 0);
    }

    private static int windGlideEnergyCost(ServerPlayer player) {
        int cost = ServerConfig.WIND_GLIDE_ENERGY_COST.get();
        if (JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO) >= 4) {
            cost = Math.round(cost * 0.85F);
        }
        if (hasWindWalkerAbilityBonus(player)) {
            cost = Math.round(cost * 0.85F);
        }
        return cost;
    }

    private static boolean hasWindWalkerAbilityBonus(ServerPlayer player) {
        return EchoArmorSets.windWalkerPieces(player) >= 4
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO);
    }
}
