package cn.kurt6.unknown_echoes.advancement;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactData;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.block.deep.DeepRouteNodeBlock;
import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
import cn.kurt6.unknown_echoes.journal.ArchiveTrialProgress;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/** 成就授予入口；成就只读取服务端关键进度，不反向授予权限或能力。 */
public class ModAdvancements {

    public static final String FIRST_BEACON = "first_beacon";
    public static final String ENTER_ECHO_REALM = "enter_echo_realm";
    public static final String FIRST_PAGE = "first_page";
    public static final String PAGES_10 = "pages_10";
    public static final String ALL_PAGES = "all_pages";
    public static final String FIRST_RUIN = "first_ruin";
    public static final String EXPLORER_5 = "explorer_5";
    public static final String ALL_BIOMES = "all_biomes";
    public static final String GRAND_ARCHIVE = "grand_archive";
    public static final String ECHO_REALM_COMPLETE = "echo_realm_complete";
    public static final String ECHO_REALM_GRADUATION = "echo_realm_graduation";
    public static final String ENTER_DEEP_ECHOES = "enter_deep_echoes";
    public static final String ALL_DEEP_BIOMES = "all_deep_biomes";
    public static final String DEEP_MICRO_SITES = "deep_micro_sites";
    public static final String DEEP_MONUMENTAL_SITES = "deep_monumental_sites";
    public static final String WORLD_ECHO_COMPLETE = "world_echo_complete";
    public static final String ARCHIVE_TRIALS = "archive_trials";
    public static final String FRACTURE_HALL = "fracture_hall";
    public static final String SPACE_ANCHORS = "space_anchors";
    public static final String ECHO_INDEX_UNLOCKED = "echo_index_unlocked";
    public static final String INDEX_EXPANSION_1 = "index_expansion_1";
    public static final String INDEX_EXPANSION_2 = "index_expansion_2";
    public static final String EXPLORER_20 = "explorer_20";
    public static final String PAGES_48 = "pages_48";
    public static final String ALL_FIRST_DIMENSION_PAGES = "all_first_dimension_pages";
    public static final String FIRST_CREATURE = "first_creature";
    public static final String BESTIARY_15 = "bestiary_15";
    public static final String BESTIARY_ALL = "bestiary_all";
    public static final String FIRST_ARTIFACT = "first_artifact";
    public static final String ARTIFACT_TUNED = "artifact_tuned";
    public static final String ARTIFACT_MAXED = "artifact_maxed";
    public static final String ALL_ARTIFACTS = "all_artifacts";
    public static final String FIRST_WORLD_EVENT = "first_world_event";
    public static final String WORLD_EVENTS_8 = "world_events_8";
    public static final String PUZZLE_ADEPT = "puzzle_adept";
    public static final String FIRST_GUARDIAN = "first_guardian";
    public static final String GUARDIANS_6 = "guardians_6";
    public static final String ALL_GUARDIANS = "all_guardians";
    private static final String ENTERED_ECHO_REALM_KEY = "advancement:entered_echo_realm_via_beacon";
    private static final String ENTERED_DEEP_ECHOES_KEY = "advancement:entered_deep_echoes_via_beacon";

    /** 第一维度守护者(与 EchoCompletionManager.MINI_BOSSES 同源,完满度要求 6 名)。 */
    private static final List<String> REALM_GUARDIANS = List.of(
            "storm_weaver",
            "tide_lantern_keeper",
            "mirror_dust_butler",
            "silent_priest",
            "crystal_songkeeper",
            "broken_bell_keeper",
            "dream_bloom_keeper",
            "lost_recorder_chief");
    private static final int GUARDIANS_REQUIRED = 6;
    private static final int BESTIARY_MILESTONE = 15;
    private static final int WORLD_EVENT_MILESTONE = 8;
    private static final int PUZZLE_MILESTONE = 10;
    /** 与 EchoCompletionManager.PUZZLE_PREFIXES 同源的机关键前缀。 */
    private static final List<String> PUZZLE_PREFIXES = List.of(
            "resonance_ritual:", "tide_pillar:", "mirror_sigil:",
            "research_revisit:", "world_event:");
    private static final String WORLD_EVENT_PREFIX = "world_event:";
    private static final int EXPLORER_MILESTONE = 20;
    private static final int PAGES_MILESTONE = 48;

    private static final Set<String> WORLD_WONDERS = Set.of(
            "unknown_echoes:echo_world_tree",
            "unknown_echoes:eternal_echo_lighthouse",
            "unknown_echoes:mirror_sea",
            "unknown_echoes:inverted_mountains",
            "unknown_echoes:sky_rift",
            "unknown_echoes:silent_great_boat",
            "unknown_echoes:broken_bell_tower");
    private static final Set<String> DEEP_MICRO_SITE_IDS = Set.of(
            "unknown_echoes:folded_waystation",
            "unknown_echoes:gravity_shard_spire",
            "unknown_echoes:hushed_refuge");
    private static final Set<String> DEEP_MONUMENTAL_SITE_IDS = Set.of(
            "unknown_echoes:resonance_foundry",
            "unknown_echoes:gravity_crown_observatory",
            "unknown_echoes:voidscar_bastion",
            "unknown_echoes:shattered_codex_vault",
            "unknown_echoes:weightless_orrery_court",
            "unknown_echoes:hushed_cathedral",
            "unknown_echoes:riftglass_citadel",
            "unknown_echoes:folded_celestial_archive",
            "unknown_echoes:inverted_pendulum_palace",
            "unknown_echoes:riftworn_memorial_court");

    public static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements()
                .get(UnknownEchoes.id(path));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }

    public static void awardAbility(ServerPlayer player, String abilityId) {
        award(player, "ability_" + abilityId);
    }

    public static void awardBossDefeat(ServerPlayer player, String bossPath) {
        award(player, "defeat_" + bossPath);
    }

    public static void checkMainlineAdvancements(ServerPlayer player) {
        if (EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID)) {
            award(player, FIRST_BEACON);
        }
        if (EchoAbilityManager.hasActivatedMechanism(player, ENTERED_ECHO_REALM_KEY)) {
            award(player, ENTER_ECHO_REALM);
        }
        awardBossIfDefeated(player, EchoPermission.FORGOTTEN_COLOSSUS_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.WIND_ECHO);
        awardBossIfDefeated(player, EchoPermission.ABYSS_WATCHER_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.TIDE_ECHO);
        awardBossIfDefeated(player, EchoPermission.MIRROR_GUARDIAN_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.TRUE_SIGHT_ECHO);
        if (EchoCompletionManager.hasArchiveTerminalRecord(player)) {
            award(player, GRAND_ARCHIVE);
        }
        if (ArchiveTrialProgress.hasCompletedAll(player)) {
            award(player, ARCHIVE_TRIALS);
        }
        if (EchoCompletionManager.hasMainlineCompletionRecord(player)) {
            award(player, ECHO_REALM_GRADUATION);
        }
        if (EchoAbilityManager.hasActivatedMechanism(player, ENTERED_DEEP_ECHOES_KEY)) {
            award(player, ENTER_DEEP_ECHOES);
        }
        checkDeepRouteAdvancements(player);
        checkArtifactAdvancements(player);
        awardBossIfDefeated(player, EchoPermission.VOID_PROPHET_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.GRAVITY_ECHO);
        awardBossIfDefeated(player, EchoPermission.RIFT_WARDEN_ID);
        awardAbilityIfUnlocked(player, EchoAbilityType.VOID_ECHO);
        awardBossIfDefeated(player, EchoPermission.WORLD_ECHO_ID);
        if (EchoAbilityManager.hasActivatedMechanism(player,
                EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            award(player, WORLD_ECHO_COMPLETE);
        }
    }

    private static void awardBossIfDefeated(ServerPlayer player, ResourceLocation bossId) {
        if (EchoAbilityManager.hasDefeatedBoss(player, bossId)) {
            awardBossDefeat(player, bossId.getPath());
        }
    }

    private static void awardAbilityIfUnlocked(ServerPlayer player, EchoAbilityType ability) {
        if (EchoAbilityManager.hasAbility(player, ability)) {
            awardAbility(player, ability.getId());
        }
    }

    /** 只由残响信标在传送成功后调用，其他 Mod 或管理员传送不计入。 */
    public static void recordEchoRealmEntry(ServerPlayer player) {
        if (player.isSpectator()
                || !player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)
                || !EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.RESONANCE_BEACON_ID)
                || !EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.ECHO_REALM_ID)) {
            return;
        }
        if (EchoAbilityManager.activateMechanism(player, ENTERED_ECHO_REALM_KEY)) {
            award(player, ENTER_ECHO_REALM);
        }
    }

    /** 只由校准完成的深渊信标在传送成功后调用，测试传送不计入。 */
    public static void recordDeepEchoesEntry(ServerPlayer player) {
        if (player.isSpectator()
                || !player.level().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID)
                || !EchoPermission.canEnterDeepEchoes(player)
                || !EchoAbilityManager.hasActivatedBeacon(player, EchoPermission.ABYSS_BEACON_ID)
                || !EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.DEEP_ECHOES_ID)) {
            return;
        }
        if (EchoAbilityManager.activateMechanism(player, ENTERED_DEEP_ECHOES_KEY)) {
            award(player, ENTER_DEEP_ECHOES);
        }
    }

    public static void checkJournalAdvancements(ServerPlayer player) {
        AdvancementHolder root = player.server.getAdvancements().get(UnknownEchoes.id("root"));
        if (root != null && player.getAdvancements().getOrStartProgress(root).isDone()) {
            award(player, "exploration_root");
            award(player, "equipment_root");
        }
        checkInventoryMilestones(player);
        var journal = JournalManager.getData(player);
        int pageCount = AncientPageCatalog.countReleasePages(journal.getPages());
        if (pageCount >= 1) {
            award(player, FIRST_PAGE);
        }
        if (pageCount >= 10) {
            award(player, PAGES_10);
        }
        if (pageCount >= JournalManager.KNOWN_PAGES) {
            award(player, ALL_PAGES);
        }
        int firstDimensionPages = AncientPageCatalog.countFirstDimensionPages(journal.getPages());
        if (firstDimensionPages >= PAGES_MILESTONE) {
            award(player, PAGES_48);
        }
        if (firstDimensionPages >= AncientPageCatalog.FIRST_DIMENSION_TOTAL) {
            award(player, ALL_FIRST_DIMENSION_PAGES);
        }

        Set<String> structures = journal.getStructures();
        if (!structures.isEmpty()) {
            award(player, FIRST_RUIN);
        }
        if (structures.size() >= 5) {
            award(player, EXPLORER_5);
        }
        if (structures.size() >= EXPLORER_MILESTONE) {
            award(player, EXPLORER_20);
        }
        for (String structureId : structures) {
            awardStructureDiscovery(player, structureId, structures);
        }
        if (EchoRealmBiomeCatalog.hasVisitedAll(journal.getBiomes())) {
            award(player, ALL_BIOMES);
        }
        checkDeepExplorationAdvancements(player, structures, journal.getBiomes());
        checkResearchAdvancements(player);
        checkBestiaryAdvancements(player);
        checkGuardianAdvancements(player);
        checkMechanismAdvancements(player);
        checkIndexAdvancements(player);
        if (EchoCompletionManager.hasClaimedT7Reward(player)) {
            award(player, ECHO_REALM_COMPLETE);
        }
    }

    /** 图鉴成就:只读已记录的生物集合,分母取图鉴 Tag 的实际大小。 */
    public static void checkBestiaryAdvancements(ServerPlayer player) {
        int recorded = JournalManager.getData(player).getMobs().size();
        if (recorded >= 1) {
            award(player, FIRST_CREATURE);
        }
        if (recorded >= BESTIARY_MILESTONE) {
            award(player, BESTIARY_15);
        }
        int total = bestiaryTotal(player);
        if (total > 0 && recorded >= total) {
            award(player, BESTIARY_ALL);
        }
    }

    private static int bestiaryTotal(ServerPlayer player) {
        var tag = player.server.registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getTag(JournalManager.BESTIARY_MOBS_TAG);
        return tag.map(holders -> holders.size()).orElse(0);
    }

    public static void checkGuardianAdvancements(ServerPlayer player) {
        int defeated = 0;
        for (String path : REALM_GUARDIANS) {
            if (EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id(path))) {
                defeated++;
                awardBossDefeat(player, path);
            }
        }
        if (defeated >= 1) {
            award(player, FIRST_GUARDIAN);
        }
        if (defeated >= GUARDIANS_REQUIRED) {
            award(player, GUARDIANS_6);
        }
        if (defeated >= REALM_GUARDIANS.size()) {
            award(player, ALL_GUARDIANS);
        }
    }

    /** 异象与机关成就:只读个人机关记录,前缀与完满度计分同源。 */
    public static void checkMechanismAdvancements(ServerPlayer player) {
        var mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        int events = 0;
        int puzzles = 0;
        for (String key : mechanisms) {
            if (key.startsWith(WORLD_EVENT_PREFIX)) {
                events++;
            }
            for (String prefix : PUZZLE_PREFIXES) {
                if (key.startsWith(prefix)) {
                    puzzles++;
                    break;
                }
            }

            if (events >= WORLD_EVENT_MILESTONE && puzzles >= PUZZLE_MILESTONE) {
                break;
            }
        }
        if (events >= 1) {
            award(player, FIRST_WORLD_EVENT);
        }
        if (events >= WORLD_EVENT_MILESTONE) {
            award(player, WORLD_EVENTS_8);
        }
        if (puzzles >= PUZZLE_MILESTONE) {
            award(player, PUZZLE_ADEPT);
        }
        for (String id : AdvancementMilestones.activities(mechanisms)) {
            award(player, id);
        }
    }

    public static void checkJourneyAdvancements(ServerPlayer player, List<Integer> values) {
        if (player.isSpectator()) return;
        for (String id : AdvancementMilestones.journeys(values)) {
            award(player, id);
        }
    }

    public static void checkParticleAdvancements(ServerPlayer player, ItemStack stack) {
        if (player.isSpectator() || !ParticleRules.eligible(stack)) return;
        var data = ParticleRules.data(stack);
        for (String id : AdvancementMilestones.particles(data.tier(), data.counts())) {
            award(player, id);
        }
    }

    public static void checkInventoryMilestones(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            checkParticleAdvancements(player, player.getInventory().getItem(slot));
        }
    }

    public static void checkIndexAdvancements(ServerPlayer player) {
        if (EchoCompletionManager.hasEchoIndexUnlocked(player)) {
            award(player, ECHO_INDEX_UNLOCKED);
        }
        if (EchoCompletionManager.hasIndexExpansionOne(player)) {
            award(player, INDEX_EXPANSION_1);
        }
        if (EchoCompletionManager.hasIndexExpansionTwo(player)) {
            award(player, INDEX_EXPANSION_2);
        }
    }

    public static void checkArtifactAdvancements(ServerPlayer player) {
        ArtifactData data = ArtifactManager.getData(player);
        int claimed = 0;
        int maxLevel = 0;
        for (ArtifactType type : ArtifactType.values()) {
            if (!data.isClaimed(type)) {
                continue;
            }
            claimed++;
            maxLevel = Math.max(maxLevel, data.getLevel(type));
            award(player, "artifact_" + type.getId());
        }
        if (claimed >= 1) {
            award(player, FIRST_ARTIFACT);
        }
        if (claimed >= ArtifactType.values().length) {
            award(player, ALL_ARTIFACTS);
        }
        if (maxLevel >= 2) {
            award(player, ARTIFACT_TUNED);
        }
        if (maxLevel >= ArtifactType.MAX_LEVEL) {
            award(player, ARTIFACT_MAXED);
        }
    }

    public static void checkDeepRouteAdvancements(ServerPlayer player) {
        if (DeepRouteNodeBlock.hasCompletedHall(player)) {
            award(player, FRACTURE_HALL);
        }
        if (EchoPermission.hasVoidRouteReady(player)) {
            award(player, SPACE_ANCHORS);
        }
    }

    public static void awardStructureDiscovery(ServerPlayer player, String structureId, Set<String> discovered) {
        switch (structureId) {
            case "unknown_echoes:echo_world_tree" -> award(player, "world_tree");
            case "unknown_echoes:mirror_sea" -> award(player, "mirror_sea");
            case "unknown_echoes:sky_rift" -> award(player, "sky_rift");
            case "unknown_echoes:silent_great_boat" -> award(player, "silent_ark");
            case "unknown_echoes:broken_bell_tower" -> award(player, "broken_bell_tower");
            case "unknown_echoes:eternal_echo_lighthouse" -> award(player, "eternal_lighthouse");
            case "unknown_echoes:inverted_mountains" -> award(player, "inverted_mountains");
            default -> {
            }
        }
        long foundWonders = WORLD_WONDERS.stream().filter(discovered::contains).count();
        if (foundWonders >= 5) {
            award(player, "all_wonders");
        }
        if (foundWonders == WORLD_WONDERS.size()) {
            award(player, "all_seven_wonders");
        }
    }

    public static void checkDeepExplorationAdvancements(ServerPlayer player,
                                                        Set<String> discoveredStructures,
                                                        Set<String> visitedBiomes) {
        if (!EchoPermission.canEnterDeepEchoes(player)) {
            return;
        }
        if (DeepEchoesBiomeCatalog.hasVisitedAll(visitedBiomes)) {
            award(player, ALL_DEEP_BIOMES);
            if (discoveredStructures.containsAll(DEEP_MICRO_SITE_IDS)) {
                award(player, DEEP_MICRO_SITES);
            }
            if (discoveredStructures.containsAll(DEEP_MONUMENTAL_SITE_IDS)) {
                award(player, DEEP_MONUMENTAL_SITES);
            }
        }
    }

    /** 研究成就:每条线各自一个满级成就,外加起步与两个汇总。研究不授予任何权限。 */
    public static void checkResearchAdvancements(ServerPlayer player) {
        boolean anyResearch = false;
        boolean allBase = true;
        boolean allLines = true;
        for (EchoResearchLine line : EchoResearchLine.values()) {
            int level = EchoResearchManager.getResearchLevel(player, line);
            boolean maxed = level >= EchoResearchManager.MAX_RESEARCH_LEVEL;
            anyResearch |= level >= 1;
            allLines &= maxed;
            if (maxed) {
                award(player, "research_" + line.getId());
            }
            if (line == EchoResearchLine.WIND
                    || line == EchoResearchLine.TIDE
                    || line == EchoResearchLine.TRUE_SIGHT) {
                allBase &= maxed;
            }
        }
        if (anyResearch) {
            award(player, "first_research");
        }
        if (allBase) {
            award(player, "all_base_research");
        }
        if (allLines) {
            award(player, "all_research");
        }
    }
}
