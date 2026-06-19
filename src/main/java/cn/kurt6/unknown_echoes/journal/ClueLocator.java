package cn.kurt6.unknown_echoes.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 线索定位器(V0.6D):在"读取线索"的那一刻做一次有界结构定位(同原版探险家地图),
 * 把结果写入玩家个人日志线索;此后罗盘只读线索,不再扫描(12.2:不做全图结构扫描)。
 * 定位是低频交互(观测站检索/线索地图使用)触发的一次性开销,半径有界。
 */
public class ClueLocator {

    /** 定位半径(区块)。 */
    private static final int SEARCH_RADIUS_CHUNKS = 48;

    /** 观测站检索可命中的回访遗迹池(回声境域内,值得被"检索残响"提到的结构)。 */
    private static final List<String> REALM_CLUE_POOL = List.of(
            "memory_pillar_courtyard", "sunken_temple", "mirror_temple", "echo_grand_archive",
            "echo_temple", "rune_puzzle_room",
            "silent_ring", "broken_archive", "tide_lighthouse_reef", "mirror_dust_cloister",
            "silent_altar", "wind_eroded_tower", "reflection_vault");

    /**
     * 定位指定结构并写入未踏入线索。返回 true 表示新线索落账(或附近已有同结构线索)。
     * 找不到(半径内无该结构)返回 false,调用方给含蓄提示。
     */
    public static boolean locateAndAddClue(ServerPlayer player, ResourceLocation structureId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder =
                registry.getHolder(ResourceKey.create(Registries.STRUCTURE, structureId));
        if (holder.isEmpty()) {
            return false;
        }
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(
                level, HolderSet.direct(holder.get()), player.blockPosition(),
                SEARCH_RADIUS_CHUNKS, false);
        if (found == null) {
            return false;
        }
        BlockPos pos = found.getFirst();
        // 已经踏入过的位置不再作为"未踏入"线索写入(addClue 内部会合并)
        JournalManager.addClue(player, new ExplorationClue(structureId.toString(),
                level.dimension().location().toString(), pos, false));
        JournalManager.syncJournal(player);
        return true;
    }

    /** 观测站"检索残响":从回访池里随机挑一个玩家尚无未踏入线索的结构定位。 */
    public static boolean addObservatoryClue(ServerPlayer player, RandomSource random) {
        List<String> candidates = new ArrayList<>(REALM_CLUE_POOL);
        // 排除已有未踏入线索的结构,避免检索重复
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (!clue.visited()) {
                ResourceLocation id = ResourceLocation.tryParse(clue.structureId());
                if (id != null) {
                    candidates.remove(id.getPath());
                }
            }
        }
        while (!candidates.isEmpty()) {
            String pick = candidates.remove(random.nextInt(candidates.size()));
            if (locateAndAddClue(player, cn.kurt6.unknown_echoes.UnknownEchoes.id(pick))) {
                return true;
            }
        }
        return false;
    }
}
