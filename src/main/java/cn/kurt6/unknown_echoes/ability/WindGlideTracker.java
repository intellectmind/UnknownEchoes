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

/**
 * 风之空中状态服务端追踪(V0.6E,5.2 缺口):
 * 旧版 DoubleJumpPayload 只校验能力持有,伪造滑翔包可无限重置摔落距离。
 * 现在服务端逐 tick 追踪离地/二段跳/滑翔状态——未离地或未二段跳的滑翔包直接忽略;
 * 滑翔开始消耗回响能量并受落地冷却限制(数值走 ServerConfig),拒绝时通知客户端取消滑翔。
 * 全部状态为瞬态体验数据,不入存档;客户端的滑翔手感预测照旧,服务端只是权威闸门。
 */
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
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    /** 每个服务端玩家 tick 调用(CommonEvents):维护离地计数、滑翔倒计时与落地冷却。 */
    public static void tick(ServerPlayer player) {
        State s = state(player);
        boolean grounded = isGroundedForWind(player);
        if (grounded) {
            s.groundedTicks = Math.min(s.groundedTicks + 1, 100);
            if (s.gliding) {
                // 滑翔落地 → 进入冷却(5.2:落地后冷却,数值走 ServerConfig)
                // 聆听者 4 件:能力冷却小幅降低(11.3,×0.7)
                s.gliding = false;
                int cooldown = ServerConfig.WIND_GLIDE_LANDING_COOLDOWN_TICKS.get();
                if (EchoArmorSets.hasListenerAbilityBonus(player)) {
                    cooldown = cooldown * 7 / 10;
                }
                if (hasWindWalkerAbilityBonus(player)) {
                    cooldown = cooldown * 4 / 5;
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
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)) {
            return;
        }
        State s = state(player);
        switch (mode) {
            case DoubleJumpPayload.MODE_JUMP -> {
                // 未离地或本段空中已二段跳:忽略——不重置摔落,不播音效。
                // 同时检查玩家当前状态,避免 tracker tick/网络顺序滞后时把真实起飞误判为在地面。
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
                // 状态校验:必须已二段跳且仍在空中;当前 onGround 滞后时用上升速度识别真实起飞。
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
                if (cost > 0 && ArtifactManager.getEnergy(player) < cost) {
                    deny(player, "energy");
                    EchoAbilityManager.recordFailure(player, EchoAbilityType.WIND_ECHO, "glide_energy");
                    return;
                }
                if (cost > 0) {
                    ArtifactManager.spendEnergy(player, cost);
                }
                s.gliding = true;
                // 聆听者 4 件:能力持续时间小幅延长(11.3,+20 tick);客户端公式同步加成
                s.glideTicksRemaining = windGlideDuration(player)
                        + (EchoArmorSets.hasListenerAbilityBonus(player) ? 20 : 0)
                        + GLIDE_DURATION_TOLERANCE;
                player.fallDistance = 0.0F;
                player.level().playSound(null, player.blockPosition(),
                        ModSounds.WIND_GLIDE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            case DoubleJumpPayload.MODE_GLIDE_HOLD -> {
                // 只有服务端确认的滑翔才持续重置摔落;伪造保持包在这里被掐断
                if (!s.gliding) {
                    return;
                }
                player.fallDistance = 0.0F;
                // 滑翔保持包每 10 tick 一次,低音量风声变体连成持续气流感
                player.level().playSound(null, player.blockPosition(),
                        ModSounds.WIND_GLIDE.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
            }
            default -> {
            }
        }
    }

    /** 服务端是否认可该玩家正在滑翔(风流平台等机关复用)。 */
    public static boolean isGliding(ServerPlayer player) {
        return state(player).gliding;
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
