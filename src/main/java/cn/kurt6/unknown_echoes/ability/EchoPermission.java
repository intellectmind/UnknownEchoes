package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.deep.DeepRouteNodeBlock;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 统一权限检测。所有关键交互(风门、信标、奖励)都从这里判定, 保证飞行/传送/高伤害 Mod 无法绕过世界规则。 */
public class EchoPermission {
    public static final ResourceLocation RESONANCE_BEACON_ID = UnknownEchoes.id("resonance");
    public static final ResourceLocation FORGOTTEN_COLOSSUS_ID = UnknownEchoes.id("forgotten_colossus");
    public static final ResourceLocation ABYSS_WATCHER_ID = UnknownEchoes.id("abyss_watcher");
    public static final ResourceLocation MIRROR_GUARDIAN_ID = UnknownEchoes.id("mirror_guardian");
    public static final ResourceLocation VOID_PROPHET_ID = UnknownEchoes.id("void_prophet");
    public static final ResourceLocation RIFT_WARDEN_ID = UnknownEchoes.id("rift_warden");
    public static final ResourceLocation WORLD_ECHO_ID = UnknownEchoes.id("world_echo");
    public static final ResourceLocation ECHO_REALM_ID = UnknownEchoes.id("echo_realm");
    public static final ResourceLocation ABYSS_BEACON_ID = UnknownEchoes.id("abyss");
    public static final ResourceLocation DEEP_ECHOES_ID = UnknownEchoes.id("deep_echoes");
    public static final String GRAVITY_TRIAL_KEY = "deep:gravity_trial";
    public static final String VOID_ROUTE_READY_KEY = "deep:void_route_ready";
    public static final String VOID_TRIAL_KEY = "deep:void_trial";
    public static final String WORLD_ECHO_REWARD_READY_KEY = "ending:world_echo_reward_ready";
    public static final String WORLD_ECHO_COMPLETE_KEY = "ending:world_echo_complete";

    public static boolean canUseEchoMechanism(Player player, EchoAbilityType requiredAbility) {
        if (player.isSpectator()) {
            return false;
        }
        if (!ServerConfig.ABILITY_PERMISSION_REQUIRED.get()) {
            return true;
        }
        return EchoAbilityManager.hasAbility(player, requiredAbility);
    }

    /** 组合条件:需要同时拥有全部列出的能力(如 潮汐 + 真视机关)。 */
    public static boolean canUseEchoMechanism(Player player, EchoAbilityType... requiredAbilities) {
        if (player.isSpectator()) {
            return false;
        }
        if (!ServerConfig.ABILITY_PERMISSION_REQUIRED.get()) {
            return true;
        }
        for (EchoAbilityType ability : requiredAbilities) {
            if (!EchoAbilityManager.hasAbility(player, ability)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canOpenWindDoor(Player player) {
        return canUseEchoMechanism(player, EchoAbilityType.WIND_ECHO);
    }

    public static boolean canUseWindMechanism(Player player) {
        return canUseEchoMechanism(player, EchoAbilityType.WIND_ECHO);
    }

    public static boolean canUseTideMechanism(Player player) {
        return canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO);
    }

    public static boolean canEnterEchoRealm(Player player) {
        return !player.isSpectator()
                && EchoAbilityManager.hasActivatedBeacon(player, RESONANCE_BEACON_ID)
                && EchoAbilityManager.hasUnlockedDimension(player, ECHO_REALM_ID);
    }

    public static boolean canCalibrateAbyssBeacon(ServerPlayer player) {
        return !player.isSpectator()
                && (canEnterDeepEchoes(player)
                    || (EchoCompletionManager.hasMainlineCompletionRecord(player)
                        && EchoCompletionManager.hasCoreResonance(player)
                        && EchoCompletionManager.isComplete(player)));
    }

    public static boolean canEnterDeepEchoes(Player player) {
        return !player.isSpectator()
                && EchoAbilityManager.hasActivatedBeacon(player, ABYSS_BEACON_ID)
                && EchoAbilityManager.hasUnlockedDimension(player, DEEP_ECHOES_ID);
    }

    /** 虚空先知只要求前三种回响与校准大厅前置，绝不预先要求重力回响。 */
    public static boolean canStartVoidProphet(Player player) {
        return canEnterDeepEchoes(player)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)
                && DeepRouteNodeBlock.hasCompletedHall(player);
    }

    public static boolean canUseGravityMechanism(Player player) {
        return canUseEchoMechanism(player, EchoAbilityType.GRAVITY_ECHO);
    }

    public static boolean hasVoidRouteReady(Player player) {
        return EchoAbilityManager.hasActivatedMechanism(player, VOID_ROUTE_READY_KEY);
    }

    public static boolean canStartRiftWarden(Player player) {
        return canUseGravityMechanism(player) && hasVoidRouteReady(player);
    }

    public static boolean canSummonWorldEcho(Player player) {
        if (!canEnterDeepEchoes(player)) {
            return false;
        }
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            if (!EchoAbilityManager.hasAbility(player, ability)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canClaimWorldEchoEnding(ServerPlayer player) {
        return canSummonWorldEcho(player)
                && EchoAbilityManager.hasDefeatedBoss(player, WORLD_ECHO_ID)
                && EchoAbilityManager.hasActivatedMechanism(player, WORLD_ECHO_REWARD_READY_KEY);
    }

    public static boolean canClaimBossReward(ServerPlayer player, ResourceLocation bossId) {
        return !player.isSpectator()
                && EchoAbilityManager.hasDefeatedBoss(player, bossId);
    }
}
