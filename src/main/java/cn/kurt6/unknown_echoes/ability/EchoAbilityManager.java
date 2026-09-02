package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.network.SyncAbilityPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 服务端能力数据读写入口。所有关键进度必须经由本类写入玩家数据并同步客户端。 */
public class EchoAbilityManager {

    public static EchoAbilityData getData(Player player) {
        return player.getData(ModAttachments.ECHO_ABILITY_DATA);
    }

    public static boolean hasAbility(Player player, EchoAbilityType ability) {
        return getData(player).hasAbility(ability);
    }

    public static boolean unlockAbility(ServerPlayer player, EchoAbilityType ability) {
        if (hasAbility(player, ability)) {
            return true;
        }
        if (getData(player).unlockAbility(ability)) {
            syncToClient(player);
            ModAdvancements.awardAbility(player, ability.getId());
            ModAdvancements.checkResearchAdvancements(player);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return hasAbility(player, ability);
    }

    public static boolean hasActivatedBeacon(Player player, ResourceLocation beaconId) {
        return getData(player).getActivatedBeacons().contains(beaconId.toString());
    }

    public static boolean activateBeacon(ServerPlayer player, ResourceLocation beaconId) {
        if (hasActivatedBeacon(player, beaconId)) {
            return true;
        }
        if (getData(player).activateBeacon(beaconId.toString())) {
            syncToClient(player);
        }
        return hasActivatedBeacon(player, beaconId);
    }

    public static boolean hasDefeatedBoss(Player player, ResourceLocation bossId) {
        return getData(player).getDefeatedBosses().contains(bossId.toString());
    }

    public static boolean markBossDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (hasDefeatedBoss(player, bossId)) {
            return true;
        }
        if (getData(player).markBossDefeated(bossId.toString())) {
            syncToClient(player);
            onBossFirstRecorded(player, bossId);
        }
        return hasDefeatedBoss(player, bossId);
    }

    public static boolean markBossDefeatedWithMechanisms(ServerPlayer player, ResourceLocation bossId,
                                                         String... mechanismKeys) {
        EchoAbilityData data = getData(player);
        boolean bossChanged = !hasDefeatedBoss(player, bossId);
        boolean changed = bossChanged;
        for (String key : mechanismKeys) {
            changed |= !hasActivatedMechanism(player, key);
        }
        if (!data.markBossDefeatedWithMechanisms(bossId.toString(), Arrays.asList(mechanismKeys))) {
            return false;
        }
        if (changed) {
            syncToClient(player);
        }
        if (bossChanged) {
            onBossFirstRecorded(player, bossId);
        }
        return true;
    }

    private static void onBossFirstRecorded(ServerPlayer player, ResourceLocation bossId) {
        ModAdvancements.awardBossDefeat(player, bossId.getPath());
        ModAdvancements.checkGuardianAdvancements(player);

        if (bossId.getPath().equals("storm_weaver")) {
            addResearchPoints(player, EchoAbilityType.WIND_ECHO, 1);
        }
    }

    public static boolean unlockDimension(ServerPlayer player, ResourceLocation dimensionId) {
        if (hasUnlockedDimension(player, dimensionId)) {
            return true;
        }
        if (getData(player).unlockDimension(dimensionId.toString())) {
            syncToClient(player);
        }
        return hasUnlockedDimension(player, dimensionId);
    }

    public static boolean hasUnlockedDimension(Player player, ResourceLocation dimensionId) {
        return getData(player).getUnlockedDimensions().contains(dimensionId.toString());
    }

    /** 个人机关激活记录( 潮汐符柱等):只在服务端判定,不参与客户端同步。 */
    public static boolean hasActivatedMechanism(Player player, String mechanismKey) {
        return getData(player).getActivatedMechanisms().contains(mechanismKey);
    }

    public static boolean activateMechanism(ServerPlayer player, String mechanismKey) {
        if (!getData(player).activateMechanism(mechanismKey)) {
            return false;
        }
        ModAdvancements.checkMechanismAdvancements(player);
        return true;
    }

    public static boolean activateMechanisms(ServerPlayer player, String... mechanismKeys) {
        if (!getData(player).activateMechanisms(Arrays.asList(mechanismKeys))) {
            return false;
        }
        ModAdvancements.checkMechanismAdvancements(player);
        return true;
    }

    public static boolean activateBeaconRoute(ServerPlayer player, ResourceLocation beaconId,
                                              ResourceLocation dimensionId, String... mechanismKeys) {
        EchoAbilityData data = getData(player);
        boolean changed = !hasActivatedBeacon(player, beaconId)
                || !hasUnlockedDimension(player, dimensionId);
        for (String key : mechanismKeys) {
            changed |= !hasActivatedMechanism(player, key);
        }
        if (!data.activateBeaconRoute(beaconId.toString(), dimensionId.toString(),
                Arrays.asList(mechanismKeys))) {
            return false;
        }
        if (changed) {
            syncToClient(player);
        }
        return true;
    }

    public static void deactivateMechanism(ServerPlayer player, String mechanismKey) {
        getData(player).getActivatedMechanisms().remove(mechanismKey);
    }

    /** 研究点写入:拓片阅读/回访点交互/守护者首杀统一走这里。 */
    public static void addResearchPoints(ServerPlayer player, EchoAbilityType ability, int amount) {
        EchoResearchLine line = EchoResearchLine.byAbility(ability);
        if (line == null) {
            getData(player).addResearchPoints(ability, amount);
            syncToClient(player);
            return;
        }
        EchoResearchManager.addResearchPoints(player, line, amount);
    }

    /** 记录最近一次能力使用失败的提示；仅用于界面展示，不写入存档。 */
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

    public static void revokeBossDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (getData(player).getDefeatedBosses().remove(bossId.toString())) {
            syncToClient(player);
        }
    }

    public static void revokeBeacon(ServerPlayer player, ResourceLocation beaconId) {
        if (getData(player).getActivatedBeacons().remove(beaconId.toString())) {
            syncToClient(player);
        }
    }

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
        research.add("@puzzle_score:" + EchoCompletionManager.puzzleCompletionScore(player));
        for (EchoAbilityType type : EchoAbilityType.values()) {

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
        DeepEchoAbilityHandler.syncCooldowns(player);
        ModAdvancements.checkMainlineAdvancements(player);

        RecipeUnlocks.checkAndUnlock(player);
    }
}
