package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.network.JournalSyncPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 探索日志读写入口。所有记录都在服务端发生;首次记录用 actionbar 含蓄提示。
 * 完成度分母尽量从注册表/Tag 动态取,新增内容不用改这里。
 */
public class JournalManager {
    /** 第一维度发行残页目标。 */
    public static final int KNOWN_PAGES = AncientPageCatalog.TOTAL;
    private static final String ALL_PAGES_REWARD_KEY = "journal:all_pages_reward";

    /** 研究等级上限(V0.6B:设定基线 4 级;等级 4 预留给神器隐藏词条联动)。 */
    public static final int MAX_RESEARCH_LEVEL = EchoResearchManager.MAX_RESEARCH_LEVEL;

    public static final TagKey<EntityType<?>> ECHO_MOBS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_mobs"));
    public static final TagKey<EntityType<?>> ECHO_BOSSES_TAG =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_bosses"));

    public static ExplorationJournalData getData(ServerPlayer player) {
        return player.getData(ModAttachments.EXPLORATION_JOURNAL);
    }

    /** 个人探索线索上限(超出时优先淘汰最旧的已踏入线索,防数据无界增长)。 */
    private static final int MAX_CLUES = 64;

    public static void recordStructure(ServerPlayer player, ResourceLocation structureId) {
        recordStructure(player, structureId, player.blockPosition());
    }

    public static void recordStructure(ServerPlayer player, ResourceLocation structureId, net.minecraft.core.BlockPos pos) {
        if (getData(player).getStructures().add(structureId.toString())) {
            notebookFlip(player);
            ModAdvancements.award(player, ModAdvancements.FIRST_RUIN);
            if (getData(player).getStructures().size() >= 5) {
                ModAdvancements.award(player, ModAdvancements.EXPLORER_5);
            }
            ModAdvancements.awardStructureDiscovery(player, structureId.toString(), getData(player).getStructures());
            // V0.6D:发现遗迹 = 罗盘"归途"线索落账 + 回响能量回填(回访点档位)
            addClue(player, new ExplorationClue(structureId.toString(),
                    player.level().dimension().location().toString(), pos, true));
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.refillEnergy(player,
                    cn.kurt6.unknown_echoes.config.ServerConfig.ENERGY_REFILL_STRUCTURE.get());
            syncJournal(player);
        } else {
            // 已发现过:若有同结构的未踏入线索且玩家确实到场,标记为已踏入(线索完成)
            markClueVisited(player, structureId.toString(), pos);
        }
    }

    /** 写入一条探索线索(V0.6D):同结构 96 格内已有线索时合并,不重复记。 */
    public static void addClue(ServerPlayer player, ExplorationClue clue) {
        var clues = getData(player).getClues();
        for (int i = 0; i < clues.size(); i++) {
            ExplorationClue existing = ExplorationClue.decode(clues.get(i));
            if (existing != null && existing.structureId().equals(clue.structureId())
                    && existing.dimension().equals(clue.dimension())
                    && existing.pos().distSqr(clue.pos()) <= 96 * 96) {
                if (clue.visited() && !existing.visited()) {
                    clues.set(i, existing.asVisited().encode());
                }
                return;
            }
        }
        clues.add(clue.encode());
        if (clues.size() > MAX_CLUES) {
            // 优先淘汰最旧的已踏入线索;全是未踏入时淘汰最旧一条
            int removeIdx = 0;
            for (int i = 0; i < clues.size(); i++) {
                ExplorationClue existing = ExplorationClue.decode(clues.get(i));
                if (existing == null || existing.visited()) {
                    removeIdx = i;
                    break;
                }
            }
            clues.remove(removeIdx);
        }
    }

    /** 玩家亲身抵达某结构时,把附近同结构的未踏入线索改为已踏入。 */
    private static void markClueVisited(ServerPlayer player, String structureId, net.minecraft.core.BlockPos pos) {
        var clues = getData(player).getClues();
        String dim = player.level().dimension().location().toString();
        for (int i = 0; i < clues.size(); i++) {
            ExplorationClue clue = ExplorationClue.decode(clues.get(i));
            if (clue != null && !clue.visited() && clue.structureId().equals(structureId)
                    && clue.dimension().equals(dim) && clue.pos().distSqr(pos) <= 128 * 128) {
                clues.set(i, clue.asVisited().encode());
            }
        }
    }

    /** 解析后的个人线索列表(罗盘/UI 用)。 */
    public static List<ExplorationClue> getClues(ServerPlayer player) {
        List<ExplorationClue> result = new ArrayList<>();
        for (String line : getData(player).getClues()) {
            ExplorationClue clue = ExplorationClue.decode(line);
            if (clue != null) {
                result.add(clue);
            }
        }
        return result;
    }

    public static void recordMob(ServerPlayer player, ResourceLocation entityTypeId) {
        if (getData(player).getMobs().add(entityTypeId.toString())) {
            notebookFlip(player);
            syncJournal(player);
        }
    }

    public static void recordBiome(ServerPlayer player, ResourceLocation biomeId) {
        if (getData(player).getBiomes().add(biomeId.toString())) {
            notebookFlip(player);
            if (EchoRealmBiomeCatalog.hasVisitedAll(getData(player).getBiomes())) {
                ModAdvancements.award(player, ModAdvancements.ALL_BIOMES);
            }
            syncJournal(player);
        }
    }

    public static void recordPage(ServerPlayer player, int pageId) {
        if (!AncientPageCatalog.isReleasePage(pageId)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.ancient_page.unrecognized"), true);
            return;
        }
        if (getData(player).getPages().add(pageId)) {
            notebookFlip(player);
            // 残页影响研究等级(表现层数据),读到新页后同步客户端
            EchoAbilityManager.syncToClient(player);
            ModAdvancements.award(player, ModAdvancements.FIRST_PAGE);
            int releasePages = AncientPageCatalog.countReleasePages(getData(player).getPages());
            if (releasePages >= 10) {
                ModAdvancements.award(player, ModAdvancements.PAGES_10);
            }
            if (releasePages >= KNOWN_PAGES) {
                claimAllPagesReward(player);
            }
            ModAdvancements.checkResearchAdvancements(player);
            // V0.6D:读到新残页一次性回填回响能量(12.2)
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.refillEnergy(player,
                    cn.kurt6.unknown_echoes.config.ServerConfig.ENERGY_REFILL_PAGE.get());
            syncJournal(player);
        }
    }

    private static void claimAllPagesReward(ServerPlayer player) {
        if (EchoAbilityManager.hasActivatedMechanism(player, ALL_PAGES_REWARD_KEY)) {
            return;
        }
        EchoAbilityManager.activateMechanism(player, ALL_PAGES_REWARD_KEY);
        giveOrDrop(player, new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 12));
        giveOrDrop(player, new ItemStack(ModItems.ECHO_MARK.get(), 8));
        ModAdvancements.award(player, ModAdvancements.ALL_PAGES);
        player.displayClientMessage(Component.translatable("message.unknown_echoes.pages.all_reward"), true);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** 同步个人日志到客户端(书本 UI 数据源)。登录与每次新记录后调用。 */
    public static void syncJournal(ServerPlayer player) {
        ExplorationJournalData journal = getData(player);
        var abilityData = EchoAbilityManager.getData(player);
        var registries = player.server.registryAccess();
        int totalStructures = (int) registries.registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)).count();
        int totalBiomes = EchoRealmBiomeCatalog.REQUIRED_COUNT;
        int totalMobs = countTag(ECHO_MOBS_TAG);
        int totalBosses = countTag(ECHO_BOSSES_TAG);
        PacketDistributor.sendToPlayer(player, new JournalSyncPayload(
                List.copyOf(journal.getStructures()),
                List.copyOf(journal.getBiomes()),
                List.copyOf(journal.getMobs()),
                List.copyOf(abilityData.getDefeatedBosses()),
                journal.getPages().stream()
                        .filter(AncientPageCatalog::isReleasePage)
                        .sorted()
                        .toList(),
                List.of(totalStructures, totalBiomes, totalMobs + totalBosses, totalBosses, KNOWN_PAGES)));
    }

    /** 能力研究等级兼容入口;实际由 EchoResearchManager 处理六研究线。 */
    public static int getResearchLevel(ServerPlayer player, EchoAbilityType ability) {
        return EchoResearchManager.getResearchLevel(player, ability);
    }

    private static void notebookFlip(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.journal.recorded"), true);
    }

    /** 汇总完成度(分母动态:结构/群系按本 Mod 命名空间注册表,生物按 Tag)。 */
    public static List<Component> buildSummary(ServerPlayer player) {
        ExplorationJournalData journal = getData(player);
        var abilityData = EchoAbilityManager.getData(player);
        var registries = player.server.registryAccess();

        long totalStructures = registries.registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)).count();
        long totalBiomes = EchoRealmBiomeCatalog.REQUIRED_COUNT;
        int totalMobs = countTag(ECHO_MOBS_TAG);
        int totalBosses = countTag(ECHO_BOSSES_TAG);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("journal.unknown_echoes.header")
                .withStyle(ChatFormatting.GOLD));
        lines.add(line("journal.unknown_echoes.structures", journal.getStructures().size(), totalStructures));
        lines.add(line("journal.unknown_echoes.biomes", journal.getBiomes().size(), totalBiomes));
        lines.add(line("journal.unknown_echoes.mobs", journal.getMobs().size(), totalMobs + totalBosses));
        lines.add(line("journal.unknown_echoes.bosses", abilityData.getDefeatedBosses().size(), totalBosses));
        lines.add(line("journal.unknown_echoes.abilities",
                abilityData.getUnlockedAbilities().size(), EchoAbilityType.values().length));
        lines.add(line("journal.unknown_echoes.pages",
                AncientPageCatalog.countReleasePages(journal.getPages()), KNOWN_PAGES));

        // 研究等级:六研究线。前三条需能力,后三条需发现对应区域。
        for (EchoResearchLine line : EchoResearchLine.values()) {
            int level = EchoResearchManager.getResearchLevel(player, line);
            if (EchoResearchManager.isUnlocked(player, line) || level > 0) {
                lines.add(Component.translatable("journal.unknown_echoes.research",
                        Component.translatable("research.unknown_echoes." + line.getId()),
                        level, MAX_RESEARCH_LEVEL)
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        // 回访提示:新能力 + 已发现的旧遗迹 → 含蓄提醒值得回去看看
        addRevisitHint(lines, player, journal, EchoAbilityType.TIDE_ECHO,
                "sunken_temple", "journal.unknown_echoes.revisit.tide_runes");
        addRevisitHint(lines, player, journal, EchoAbilityType.TRUE_SIGHT_ECHO,
                "mirror_temple", "journal.unknown_echoes.revisit.hidden_room");
        addRevisitHint(lines, player, journal, EchoAbilityType.WIND_ECHO,
                "sky_observatory", "journal.unknown_echoes.revisit.observatory");
        // 淹没记录室(V0.6E,5.3 强化方向:"在日志中提示已发现遗迹内的淹没记录室")
        addRevisitHint(lines, player, journal, EchoAbilityType.TIDE_ECHO,
                "submerged_record_room", "journal.unknown_echoes.revisit.record_room");
        lines.addAll(EchoCompletionManager.buildIndexFeedback(player));
        return lines;
    }

    /** 玩家拥有能力且发现过对应结构时,在日志末尾追加一条回访提示。 */
    private static void addRevisitHint(List<Component> lines, ServerPlayer player,
                                       ExplorationJournalData journal, EchoAbilityType ability,
                                       String structureName, String langKey) {
        if (EchoAbilityManager.hasAbility(player, ability)
                && journal.getStructures().contains(UnknownEchoes.id(structureName).toString())) {
            lines.add(Component.translatable(langKey).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static Component line(String key, long have, long total) {
        return Component.translatable(key, have, total).withStyle(ChatFormatting.GRAY);
    }

    private static int countTag(TagKey<EntityType<?>> tag) {
        int count = 0;
        for (var ignored : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getTagOrEmpty(tag)) {
            count++;
        }
        return count;
    }
}
