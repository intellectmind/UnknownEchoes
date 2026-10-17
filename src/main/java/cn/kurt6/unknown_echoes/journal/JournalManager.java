package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.registry.ModSounds;
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
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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

/** 探索日志读写入口。所有记录都在服务端发生;首次记录用 actionbar 含蓄提示。 */
public class JournalManager {
    public static final int KNOWN_PAGES = AncientPageCatalog.TOTAL;
    private static final String ALL_PAGES_REWARD_KEY = "journal:all_pages_reward";
    private static final String ALL_DEEP_BIOMES_KEY = "journal:all_deep_echoes_biomes";

    /** 研究等级上限；满级研究可联动神器隐藏词条。 */
    public static final int MAX_RESEARCH_LEVEL = EchoResearchManager.MAX_RESEARCH_LEVEL;

    public static final TagKey<EntityType<?>> BESTIARY_MOBS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("bestiary_mobs"));
    public static final TagKey<EntityType<?>> ECHO_BOSSES_TAG =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_bosses"));

    public static ExplorationJournalData getData(ServerPlayer player) {
        return player.getData(ModAttachments.EXPLORATION_JOURNAL);
    }

    public static void recordStructure(ServerPlayer player, ResourceLocation structureId) {
        recordStructure(player, structureId, player.blockPosition());
    }

    public static void recordStructure(ServerPlayer player, ResourceLocation structureId, net.minecraft.core.BlockPos pos) {
        if (structureId == null) return;
        var registry = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        BoundingBox bounds = LoadedStructureLookup.loadedStructureBoundsAt(player.serverLevel(), pos,
                structure -> structureId.equals(registry.getKey(structure)));
        recordStructureSite(player, structureId, bounds == null ? pos : bounds.getCenter(), bounds);
    }

    public static void recordStructureSite(ServerPlayer player, ResourceLocation structureId,
                                           BlockPos anchor, BoundingBox bounds) {
        if (structureId == null) {
            return;
        }
        ExplorationJournalData data = getData(player);
        boolean firstDiscovery = data.addStructure(structureId.toString());
        boolean newSite = data.visitStructure(structureId.toString(),
                player.level().dimension().location().toString(), anchor, bounds);
        if (firstDiscovery) {
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.refillEnergy(player,
                    cn.kurt6.unknown_echoes.config.ServerConfig.ENERGY_REFILL_STRUCTURE.get());
            notebookFlip(player);
            ModAdvancements.award(player, ModAdvancements.FIRST_RUIN);
            if (getData(player).getStructures().size() >= 5) {
                ModAdvancements.award(player, ModAdvancements.EXPLORER_5);
            }
            if (getData(player).getStructures().size() >= 20) {
                ModAdvancements.award(player, ModAdvancements.EXPLORER_20);
            }
            ModAdvancements.awardStructureDiscovery(player, structureId.toString(), getData(player).getStructures());
            ModAdvancements.checkDeepExplorationAdvancements(player,
                    getData(player).getStructures(), getData(player).getBiomes());
        }
        if (firstDiscovery || newSite) {
            syncJournal(player);
        }
    }

    // 写入与容量淘汰统一由个人日志处理。
    public static boolean addClue(ServerPlayer player, ExplorationClue clue) {
        return getData(player).addClue(clue);
    }

    public static void selectDestination(ServerPlayer player, String destination) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        ExplorationJournalData data = getData(player);
        if (data.setDestination(destination)) {
            ExplorationClue selected = data.getDestination();
            player.displayClientMessage(selected == null
                    ? Component.translatable("message.unknown_echoes.journal.destination_cleared")
                    : Component.translatable("journal.unknown_echoes.destination.current",
                            JournalNavigation.structureName(selected)), true);
        } else if (!"".equals(destination)
                && (data.getDestination() == null || !data.getDestination().sameSite(ExplorationClue.decode(destination)))) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.journal.destination_unavailable"), true);
        }
        syncJournal(player);
    }

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
        if (entityTypeId != null && getData(player).addMob(entityTypeId.toString())) {
            notebookFlip(player);
            ModAdvancements.checkBestiaryAdvancements(player);
            syncJournal(player);
        }
    }

    public static void recordBiome(ServerPlayer player, ResourceLocation biomeId) {
        if (biomeId == null) {
            return;
        }
        boolean firstDiscovery = getData(player).addBiome(biomeId.toString());
        // 已知群系也要补写完成记录，容量不足时下次扫描仍可重试。
        if (DeepEchoesBiomeCatalog.hasVisitedAll(getData(player).getBiomes())
                && !EchoAbilityManager.hasActivatedMechanism(player, ALL_DEEP_BIOMES_KEY)) {
            if (EchoAbilityManager.activateMechanism(player, ALL_DEEP_BIOMES_KEY)) {
                EchoAbilityManager.syncToClient(player);
            }
        }
        if (firstDiscovery) {
            notebookFlip(player);
            if (EchoRealmBiomeCatalog.hasVisitedAll(getData(player).getBiomes())) {
                ModAdvancements.award(player, ModAdvancements.ALL_BIOMES);
            }
            ModAdvancements.checkDeepExplorationAdvancements(player,
                    getData(player).getStructures(), getData(player).getBiomes());
            syncJournal(player);
        }
    }

    public static void recordPage(ServerPlayer player, int pageId) {
        if (!AncientPageCatalog.isReleasePage(pageId)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.ancient_page.unrecognized"), true);
            return;
        }
        if (getData(player).addPage(pageId)) {
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.refillEnergy(player,
                    cn.kurt6.unknown_echoes.config.ServerConfig.ENERGY_REFILL_PAGE.get());
            EchoAbilityManager.syncToClient(player);
            ModAdvancements.award(player, ModAdvancements.FIRST_PAGE);
            int releasePages = AncientPageCatalog.countReleasePages(getData(player).getPages());
            if (releasePages >= 10) {
                ModAdvancements.award(player, ModAdvancements.PAGES_10);
            }
            if (releasePages >= KNOWN_PAGES) {
                retryAllPagesReward(player);
            }
            int firstDimensionPages =
                    AncientPageCatalog.countFirstDimensionPages(getData(player).getPages());
            if (firstDimensionPages >= 48) {
                ModAdvancements.award(player, ModAdvancements.PAGES_48);
            }
            if (firstDimensionPages >= AncientPageCatalog.FIRST_DIMENSION_TOTAL) {
                ModAdvancements.award(player, ModAdvancements.ALL_FIRST_DIMENSION_PAGES);
            }
            ModAdvancements.checkResearchAdvancements(player);
            syncJournal(player);
        }
    }

    /** 全页成就按阅读事实授予；背包腾出空间后再补领物品奖励。 */
    public static boolean retryAllPagesReward(ServerPlayer player) {
        int releasePages = AncientPageCatalog.countReleasePages(getData(player).getPages());
        if (releasePages < KNOWN_PAGES) {
            return false;
        }
        ModAdvancements.award(player, ModAdvancements.ALL_PAGES);
        return claimAllPagesReward(player);
    }

    private static boolean claimAllPagesReward(ServerPlayer player) {
        if (EchoAbilityManager.hasActivatedMechanism(player, ALL_PAGES_REWARD_KEY)) {
            return false;
        }
        List<ItemStack> rewards = List.of(
                new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 12),
                new ItemStack(ModItems.ECHO_MARK.get(), 8));
        if (!RewardCapacity.canFitAll(player, rewards)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.pages.need_space"), true);
            return false;
        }
        if (!EchoAbilityManager.activateMechanisms(player, () -> {
            for (ItemStack reward : rewards) {
                player.getInventory().add(reward);
            }
        }, ALL_PAGES_REWARD_KEY)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return false;
        }
        EchoAbilityManager.syncToClient(player);
        player.playNotifySound(ModSounds.FEEDBACK_PAGES_COMPLETE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 同步个人日志到客户端(书本 UI 数据源)。登录与每次新记录后调用。 */
    public static void syncJournal(ServerPlayer player) {
        ExplorationJournalData journal = getData(player);
        var abilityData = EchoAbilityManager.getData(player);
        var registries = player.server.registryAccess();
        int totalStructures = (int) registries.registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)).count();
        int totalBiomes = EchoRealmBiomeCatalog.REQUIRED_COUNT + DeepEchoesBiomeCatalog.REQUIRED_COUNT;
        int totalMobs = countTagUnion(BESTIARY_MOBS_TAG, ECHO_BOSSES_TAG);
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
                List.copyOf(journal.getClues()),
                List.of(totalStructures, totalBiomes, totalMobs, totalBosses, KNOWN_PAGES),
                journal.getEncodedDestination()));
        JourneyManager.sync(player);
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
        if (AncientPageCatalog.countReleasePages(journal.getPages()) >= KNOWN_PAGES) {
            claimAllPagesReward(player);
        }
        var abilityData = EchoAbilityManager.getData(player);
        var registries = player.server.registryAccess();

        long totalStructures = registries.registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)).count();
        long totalBiomes = EchoRealmBiomeCatalog.REQUIRED_COUNT + DeepEchoesBiomeCatalog.REQUIRED_COUNT;
        int totalMobs = countTagUnion(BESTIARY_MOBS_TAG, ECHO_BOSSES_TAG);
        int totalBosses = countTag(ECHO_BOSSES_TAG);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("journal.unknown_echoes.header")
                .withStyle(ChatFormatting.GOLD));
        lines.add(line("journal.unknown_echoes.structures", journal.getStructures().size(), totalStructures));
        lines.add(line("journal.unknown_echoes.biomes", journal.getBiomes().size(), totalBiomes));
        lines.add(line("journal.unknown_echoes.mobs", journal.getMobs().size(), totalMobs));
        lines.add(line("journal.unknown_echoes.bosses", abilityData.getDefeatedBosses().size(), totalBosses));
        lines.add(line("journal.unknown_echoes.abilities",
                abilityData.getUnlockedAbilities().size(), EchoAbilityType.values().length));
        lines.add(line("journal.unknown_echoes.pages",
                AncientPageCatalog.countReleasePages(journal.getPages()), KNOWN_PAGES));

        for (EchoResearchLine line : EchoResearchLine.values()) {
            int level = EchoResearchManager.getResearchLevel(player, line);
            if (EchoResearchManager.isUnlocked(player, line) || level > 0) {
                lines.add(Component.translatable("journal.unknown_echoes.research",
                        Component.translatable("research.unknown_echoes." + line.getId()),
                        level, MAX_RESEARCH_LEVEL)
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        for (JournalRevisit revisit : JournalRevisit.values()) {
            if (revisit.available(journal.getStructures(), ability -> EchoAbilityManager.hasAbility(player, ability))) {
                lines.add(Component.translatable(revisit.hintKey()).withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        lines.addAll(EchoCompletionManager.buildIndexFeedback(player));
        return lines;
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

    @SafeVarargs
    private static int countTagUnion(TagKey<EntityType<?>>... tags) {
        java.util.Set<EntityType<?>> types = new java.util.HashSet<>();
        for (TagKey<EntityType<?>> tag : tags) {
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getTagOrEmpty(tag)) {
                types.add(holder.value());
            }
        }
        return types.size();
    }
}
