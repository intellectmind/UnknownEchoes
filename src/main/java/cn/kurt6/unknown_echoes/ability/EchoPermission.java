package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 统一权限检测。所有关键交互(风门、信标、奖励)都从这里判定,
 * 保证飞行/传送/高伤害 Mod 无法绕过世界规则。
 */
public class EchoPermission {
    public static final ResourceLocation RESONANCE_BEACON_ID = UnknownEchoes.id("resonance");
    public static final ResourceLocation FORGOTTEN_COLOSSUS_ID = UnknownEchoes.id("forgotten_colossus");
    public static final ResourceLocation ABYSS_WATCHER_ID = UnknownEchoes.id("abyss_watcher");
    public static final ResourceLocation MIRROR_GUARDIAN_ID = UnknownEchoes.id("mirror_guardian");
    public static final ResourceLocation ECHO_REALM_ID = UnknownEchoes.id("echo_realm");

    /**
     * 通用回响机关权限判定(V0.2 起所有新机关统一走这里)。
     * 服务端校验玩家能力数据;ability_permission_required=false 时全部放行。
     */
    public static boolean canUseEchoMechanism(Player player, EchoAbilityType requiredAbility) {
        if (!ServerConfig.ABILITY_PERMISSION_REQUIRED.get()) {
            return true;
        }
        return EchoAbilityManager.hasAbility(player, requiredAbility);
    }

    /** 组合条件:需要同时拥有全部列出的能力(如 V0.5 潮汐 + 真视机关)。 */
    public static boolean canUseEchoMechanism(Player player, EchoAbilityType... requiredAbilities) {
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

    /** 潮汐机关(水下符文、深海信标、潮汐净化等)。 */
    public static boolean canUseTideMechanism(Player player) {
        return canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO);
    }

    public static boolean canEnterEchoRealm(Player player) {
        return EchoAbilityManager.hasActivatedBeacon(player, RESONANCE_BEACON_ID);
    }

    public static boolean canClaimBossReward(ServerPlayer player, ResourceLocation bossId) {
        return EchoAbilityManager.hasDefeatedBoss(player, bossId);
    }
}
