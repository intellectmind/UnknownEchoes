package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.network.SyncAbilityPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端能力数据读写入口。所有关键进度必须经由本类写入玩家数据并同步客户端。
 */
public class EchoAbilityManager {

    public static EchoAbilityData getData(Player player) {
        return player.getData(ModAttachments.ECHO_ABILITY_DATA);
    }

    public static boolean hasAbility(Player player, EchoAbilityType ability) {
        return getData(player).hasAbility(ability);
    }

    public static void unlockAbility(ServerPlayer player, EchoAbilityType ability) {
        if (getData(player).unlockAbility(ability)) {
            syncToClient(player);
            ModAdvancements.awardAbility(player, ability.getId());
            if (ability == EchoAbilityType.WIND_ECHO) {
                player.sendSystemMessage(Component.translatable("message.unknown_echoes.ability.wind_unlocked"));
            } else if (ability == EchoAbilityType.TIDE_ECHO) {
                player.sendSystemMessage(Component.translatable("message.unknown_echoes.ability.tide_unlocked"));
            } else {
                player.sendSystemMessage(Component.translatable("message.unknown_echoes.ability.unlocked",
                        Component.translatable("ability.unknown_echoes." + ability.getId())));
            }
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    public static boolean hasActivatedBeacon(Player player, ResourceLocation beaconId) {
        return getData(player).getActivatedBeacons().contains(beaconId.toString());
    }

    public static void activateBeacon(ServerPlayer player, ResourceLocation beaconId) {
        if (getData(player).getActivatedBeacons().add(beaconId.toString())) {
            syncToClient(player);
        }
    }

    public static boolean hasDefeatedBoss(Player player, ResourceLocation bossId) {
        return getData(player).getDefeatedBosses().contains(bossId.toString());
    }

    public static void markBossDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (getData(player).getDefeatedBosses().add(bossId.toString())) {
            syncToClient(player);
            ModAdvancements.awardBossDefeat(player, bossId.getPath());
            // 守护者首杀研究点(V0.6B:研究进度来源之一;重复击败只走结算的普通奖励/拓片)
            if (bossId.getPath().equals("storm_weaver")) {
                addResearchPoints(player, EchoAbilityType.WIND_ECHO, 1);
            }
        }
    }

    public static void unlockDimension(ServerPlayer player, ResourceLocation dimensionId) {
        if (getData(player).getUnlockedDimensions().add(dimensionId.toString())) {
            syncToClient(player);
        }
    }

    public static boolean hasUnlockedDimension(Player player, ResourceLocation dimensionId) {
        return getData(player).getUnlockedDimensions().contains(dimensionId.toString());
    }

    /** 个人机关激活记录(V0.5 潮汐符柱等):只在服务端判定,不参与客户端同步。 */
    public static boolean hasActivatedMechanism(Player player, String mechanismKey) {
        return getData(player).getActivatedMechanisms().contains(mechanismKey);
    }

    public static void activateMechanism(ServerPlayer player, String mechanismKey) {
        getData(player).getActivatedMechanisms().add(mechanismKey);
    }

    public static void deactivateMechanism(ServerPlayer player, String mechanismKey) {
        getData(player).getActivatedMechanisms().remove(mechanismKey);
    }

    /** 研究点写入(V0.6B):拓片阅读/回访点交互/守护者首杀统一走这里。
     *  研究只影响表现与强化数值,永不授予 EchoPermission 权限(design-principles #8)。 */
    public static void addResearchPoints(ServerPlayer player, EchoAbilityType ability, int amount) {
        EchoResearchLine line = EchoResearchLine.byAbility(ability);
        if (line == null) {
            getData(player).addResearchPoints(ability, amount);
            syncToClient(player);
            return;
        }
        EchoResearchManager.addResearchPoints(player, line, amount);
    }

    /** 记录最近一次能力使用失败的含蓄提示(V0.6E,5.7 能力面板;瞬态,不入存档)。
     *  failureKey 为 lang 键后缀,面板显示 "overview.unknown_echoes.failure.<failureKey>"。 */
    public static void recordFailure(ServerPlayer player, EchoAbilityType ability, String failureKey) {
        if (!failureKey.equals(getData(player).getLastFailure(ability))) {
            getData(player).setLastFailure(ability, failureKey);
            syncToClient(player);
        }
    }

    public static void revokeAbility(ServerPlayer player, EchoAbilityType ability) {
        if (getData(player).getUnlockedAbilities().remove(ability.getId())) {
            syncToClient(player);
        }
    }

    // ---- V0.6D 管理员命令入口(/echoes admin):只走既有数据写入口,不绕过同步 ----

    /** 撤销 Boss 击败记录(多人服修档用)。 */
    public static void revokeBossDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (getData(player).getDefeatedBosses().remove(bossId.toString())) {
            syncToClient(player);
        }
    }

    /** 撤销信标激活记录。 */
    public static void revokeBeacon(ServerPlayer player, ResourceLocation beaconId) {
        if (getData(player).getActivatedBeacons().remove(beaconId.toString())) {
            syncToClient(player);
        }
    }

    /** 撤销维度解锁记录。 */
    public static void revokeDimension(ServerPlayer player, ResourceLocation dimensionId) {
        if (getData(player).getUnlockedDimensions().remove(dimensionId.toString())) {
            syncToClient(player);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        EchoAbilityData data = getData(player);
        List<String> research = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (EchoResearchLine line : EchoResearchLine.values()) {
            int level = EchoResearchManager.getResearchLevel(player, line);
            if (level > 0) {
                research.add(line.getLegacyResearchKey() + ":" + level);
            }
        }
        for (EchoAbilityType type : EchoAbilityType.values()) {
            // 凭据与誓记页(5.8/V0.6E):只同步"该能力核心机关是否完成"这一布尔事实,
            // 不泄露机关坐标等隐藏数据(红线 #9)
            if (ResonanceTokenManager.hasRitualRecord(player, type)) {
                tokens.add(type.getId());
            }
            String failure = data.getLastFailure(type);
            if (!failure.isEmpty()) {
                failures.add(type.getId() + ":" + failure);
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncAbilityPayload(
                List.copyOf(data.getUnlockedAbilities()),
                List.copyOf(data.getActivatedBeacons()),
                List.copyOf(data.getUnlockedDimensions()),
                research, tokens, failures));
        // 配方解锁链(V0.6B):能力/研究变化都会经过这里,顺带核对一次武器配方解锁
        RecipeUnlocks.checkAndUnlock(player);
    }
}
