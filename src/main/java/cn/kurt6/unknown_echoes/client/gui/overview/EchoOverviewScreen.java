package cn.kurt6.unknown_echoes.client.gui.overview;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.client.ClientAbilityCache;
import cn.kurt6.unknown_echoes.client.ClientArtifactCache;
import cn.kurt6.unknown_echoes.client.ClientJournalCache;
import cn.kurt6.unknown_echoes.client.ClientJourneyCache;
import cn.kurt6.unknown_echoes.journal.JourneyCatalog;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
import cn.kurt6.unknown_echoes.journal.DeepEchoesProgressCatalog;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalNavigation;
import cn.kurt6.unknown_echoes.journal.JournalRevisit;
import cn.kurt6.unknown_echoes.network.JournalDestinationPayload;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import cn.kurt6.unknown_echoes.world.wonder.WorldWonderType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class EchoOverviewScreen extends Screen {

    private static final ResourceLocation ICONS =
            UnknownEchoes.id("textures/gui/system/module_icons.png");
    private static final ResourceLocation ABILITY_ICONS =
            UnknownEchoes.id("textures/gui/ability_icons.png");
    private static final ResourceLocation STAR_DISC =
            UnknownEchoes.id("textures/gui/system/star_disc.png");
    private static final ResourceLocation PAPER =
            UnknownEchoes.id("textures/gui/system/paper.png");
    private static final ResourceLocation OVERVIEW_BACKDROP =
            UnknownEchoes.id("textures/gui/system/overview_backdrop.png");
    private static final ResourceLocation BUTTONS =
            UnknownEchoes.id("textures/gui/system/ui_button_atlas.png");
    private static final int OVERVIEW_BACKDROP_SIZE = 512;
    private static final int ICON_SIZE = 32;
    private static final int MODULE_ICON_ATLAS_WIDTH = 320;
    private static final int ABILITY_ICON_ATLAS_WIDTH = 160;

    private static final int COL_BACKDROP_TOP = 0xE507111D;
    private static final int COL_BACKDROP_BOTTOM = 0xF303050A;
    private static final int COL_VOID = 0xFA020306;
    private static final int COL_PANEL = 0xC00A111A;
    private static final int COL_PANEL_DEEP = 0xE004080F;
    private static final int COL_PANEL_LIGHT = 0xD0142230;
    private static final int COL_PANEL_WASH = 0x681F3C4A;
    private static final int COL_CARD = 0xC0142230;
    private static final int COL_CARD_HOVER = 0xE01C3544;
    private static final int COL_EDGE = 0xFF35525E;
    private static final int COL_EDGE_SOFT = 0x99436A76;
    private static final int COL_EDGE_FAINT = 0x5535505A;
    private static final int COL_ACCENT = 0xFF74F4E5;
    private static final int COL_ACCENT_DIM = 0xFF38A59E;
    private static final int COL_ACCENT_GLOW = 0x665BE8D8;
    private static final int COL_ACCENT_HAZE = 0x2E5BE8D8;
    private static final int COL_GOLD = 0xFFE2C276;
    private static final int COL_GOLD_DIM = 0xFFA0834A;
    private static final int COL_GOLD_GLOW = 0x55E2C276;
    private static final int COL_TEXT = 0xFFEAF7F4;
    private static final int COL_DIM = 0xFF7C8D96;
    private static final int COL_SILHOUETTE = 0xFF3D4852;

    /** 模块页:顺序即共鸣脊线导航顺序。 */
    public enum ModulePage {
        OVERVIEW("overview"),
        ABILITY("ability"),
        JOURNAL("journal"),
        RESEARCH("research"),
        ARTIFACT("artifact"),
        TRADE("trade"),
        INDEX("index"),
        OPTIONS("options");

        final String id;
        private final Component title;

        ModulePage(String id) {
            this.id = id;
            this.title = Component.translatable("overview.unknown_echoes.page." + id);
        }

        Component title() {
            return this.title;
        }
    }

    private enum JournalChapter {
        SUMMARY, JOURNEY, CLUES, BIOMES, STRUCTURES, MOBS, BOSSES, ABILITIES, PAGES, REVISIT
    }

    private static final ModulePage[] MODULE_PAGES = ModulePage.values();
    private static final ModulePage[] OVERVIEW_PAGES = {
            ModulePage.ABILITY, ModulePage.JOURNAL, ModulePage.RESEARCH,
            ModulePage.ARTIFACT, ModulePage.TRADE, ModulePage.INDEX, ModulePage.OPTIONS
    };
    private static final JournalChapter[] JOURNAL_CHAPTERS = JournalChapter.values();
    private static final EchoAbilityType[] ABILITY_TYPES = EchoAbilityType.values();
    private static final ArtifactType[] ARTIFACT_TYPES = ArtifactType.values();
    private static final EchoAbilityType[] CREDENTIAL_ABILITIES = {
            EchoAbilityType.WIND_ECHO, EchoAbilityType.TIDE_ECHO, EchoAbilityType.TRUE_SIGHT_ECHO,
            EchoAbilityType.GRAVITY_ECHO, EchoAbilityType.VOID_ECHO
    };
    private static final Component[] ABILITY_NAMES = abilityComponents("ability.unknown_echoes.");
    private static final Component[] ABILITY_HINTS = abilityComponents("screen.unknown_echoes.ability_panel.hint.");
    private static final Component[] ARTIFACT_NAMES = artifactComponents("artifact.unknown_echoes.");
    private static final Component[] ARTIFACT_HINTS = artifactComponents("overview.unknown_echoes.artifact.hint.");
    private static final Component[] CREDENTIAL_TOKEN_NAMES = abilityComponents(
            "overview.unknown_echoes.credentials.token.");
    private static final Component NAV_TITLE_TOP =
            Component.translatable("overview.unknown_echoes.brand.top");
    private static final Component NAV_TITLE_BOTTOM =
            Component.translatable("overview.unknown_echoes.brand.bottom");
    private static final Component POPUP_CLOSE =
            Component.translatable("overview.unknown_echoes.popup.close");
    private static final Component ENERGY_POOL =
            Component.translatable("overview.unknown_echoes.energy.pool");
    private static final Component ABILITY_UNLOCKED =
            Component.translatable("screen.unknown_echoes.ability_panel.unlocked");
    private static final Component ABILITY_LOCKED =
            Component.translatable("screen.unknown_echoes.ability_panel.locked");
    private static final Component ARTIFACT_UNTUNED =
            Component.translatable("overview.unknown_echoes.artifact.untuned");
    private static final Component ARTIFACT_READY =
            Component.translatable("overview.unknown_echoes.artifact.ready");
    private static final Component ARTIFACT_UNCLAIMED =
            Component.translatable("overview.unknown_echoes.artifact.unclaimed");
    private static final Component SCREEN_TITLE =
            Component.translatable("overview.unknown_echoes.title");

    private static final String[] KNOWN_STRUCTURES = {
            "small_echo_ruin", "resonance_beacon_structure", "rune_puzzle_room",
            "memory_pillar_courtyard", "echo_temple", "sunken_temple", "mirror_temple",
            "reflection_vault", "silent_hut", "silent_ring",
            "sky_observatory", "broken_archive", "wind_eroded_tower",
            "tide_lighthouse_reef", "mirror_dust_cloister", "submerged_record_room",
            "abyss_beacon_structure", "folded_archive", "calibration_hall",
            "inverted_mountains", "inverted_chasm_bridge", "silent_abyss_anchor",
            "spatial_rift", "void_vein_chamber", "folded_waystation",
            "gravity_shard_spire", "hushed_refuge", "misfiled_catalog_well",
            "folded_scriptorium", "inverted_archive_tower", "suspended_survey_station",
            "fractured_anchor_station", "severed_archive_vault", "resonance_foundry",
            "gravity_crown_observatory", "voidscar_bastion", "shattered_codex_vault",
            "weightless_orrery_court", "hushed_cathedral", "riftglass_citadel",
            "folded_celestial_archive", "inverted_pendulum_palace", "riftworn_memorial_court",
            "world_resonance_hall", "far_echo_cartography_court", "chorus_confluence_court",
            "palimpsest_ecology_dome", "far_bell_wayfarer_harbor",
            "prismatic_canopy_conservatory", "interlayer_expedition_ring"
    };
    private static final List<String> KNOWN_BIOMES = java.util.stream.Stream.concat(
            EchoRealmBiomeCatalog.PATHS.stream(), DeepEchoesBiomeCatalog.PATHS.stream()).toList();
    private static final String[] KNOWN_MOBS = {
            "echo_wanderer", "silent_walker", "crystal_feather_bird", "dreaming_deer",
            "crystal_chime_spirit", "broken_bell_guard", "crystal_noise_wisp",
            "broken_bell_crow", "lost_recorder", "wind_sac_moth",
            "wind_eroded_sentinel", "echo_remnant", "water_shadow", "mirror_dust_decoy",
            "foldpage_bug", "anchor_shell_beast", "abyss_lurker", "void_wanderer"
    };
    private static final String[] MAIN_BOSSES = {
            "forgotten_colossus", "abyss_watcher", "mirror_guardian"
    };
    private static final String[] MINI_BOSSES = {
            "storm_weaver", "tide_lantern_keeper", "mirror_dust_butler", "silent_priest",
            "crystal_songkeeper", "broken_bell_keeper", "dream_bloom_keeper", "lost_recorder_chief"
    };
    private static final String[] JOURNAL_BOSSES = {
            "forgotten_colossus", "abyss_watcher", "mirror_guardian",
            "storm_weaver", "tide_lantern_keeper", "mirror_dust_butler", "silent_priest",
            "crystal_songkeeper", "broken_bell_keeper", "dream_bloom_keeper", "lost_recorder_chief",
            "void_prophet", "rift_warden", "world_echo", "inverted_gatekeeper", "ash_scribe"
    };

    private static final int NAV_WIDTH_WIDE = 118;
    private static final int NAV_WIDTH_MID = 98;
    private static final int NAV_WIDTH_COMPACT = 48;
    private static final int STATUS_HEIGHT = 16;
    private static final int HEADER_HEIGHT = 24;
    private static final int JOURNAL_CHAPTER_BUTTON_HEIGHT = 18;
    private static final int JOURNAL_CHAPTER_GAP = 5;
    /** 文本行页的行高。渲染、点击命中与滚轮共用，不要在调用点各写一份。 */
    private static final int LINE_ROW_HEIGHT = 14;
    private static final int JOURNAL_CHAPTER_WIDE_COLUMNS = 5;
    private static final int BUTTON_ATLAS_WIDTH = 96;
    private static final int BUTTON_ATLAS_HEIGHT = 60;
    private static final int BUTTON_SLICE_WIDTH = 96;
    private static final int BUTTON_SLICE_HEIGHT = 20;
    private static final int T7_REQUIRED_BIOMES = EchoCompletionManager.REQUIRED_BIOMES;
    private static final int T7_REQUIRED_PAGES = EchoCompletionManager.REQUIRED_PAGES;
    private static final int T7_REQUIRED_MINI_BOSSES = EchoCompletionManager.REQUIRED_MINI_BOSSES;
    private static final int T7_REQUIRED_SCORE = EchoCompletionManager.REQUIRED_SCORE;
    private static final int FOCUS_TICKS = 8;
    private static final String[] MODULE_INDICES = {"01", "02", "03", "04", "05", "06", "07"};

    private ModulePage page;
    private JournalChapter chapter = JournalChapter.SUMMARY;
    private int scroll = 0;
    private int focusTicks = 0;
    private int navFocus = -1;
    private final Component[] overviewStatusCache = new Component[MODULE_PAGES.length];
    private Component statusRegion = Component.empty();
    private Component statusEnergy = Component.empty();
    private boolean statusEnergyDormant = true;

    private final List<Line> lines = new ArrayList<>();
    private final List<ActionButton> actionButtons = new ArrayList<>();
    private final List<ClientOptionSlider> optionSliders = new ArrayList<>();
    private final Map<ActionButton, Component> optionButtonValues = new HashMap<>();
    private final List<OptionSection> optionSections = new ArrayList<>();
    private final List<ModConfigSpec.ConfigValue<?>> configurableOptions = new ArrayList<>();
    private int optionsContentHeight;
    private boolean clientConfigDirty;
    private final Map<ArtifactType, ItemStack> artifactIcons = new EnumMap<>(ArtifactType.class);
    private ReadingPopup popup = null;
    /** 像素滚动页的行与控件必须一起偏移。 */
    private int pageScroll = 0;
    /** 弹层文本分行缓存(换弹层才重排,不每帧 split)。 */
    private ReadingPopup popupLinesFor = null;
    private List<FormattedCharSequence> popupLines = List.of();
    /** tick 刷新,替代每帧全背包扫描。 */
    private final boolean[] credentialHolding = new boolean[CREDENTIAL_ABILITIES.length];

    public static void openAncientPage(int pageId) {
        EchoOverviewScreen screen = new EchoOverviewScreen(ModulePage.RESEARCH);
        screen.popup = new ReadingPopup(
                Component.translatable("page.unknown_echoes." + pageId + ".title"),
                Component.translatable("page.unknown_echoes." + pageId + ".text"));
        Minecraft.getInstance().setScreen(screen);
    }

    /** 台座上下文保留打开时的维度，避免旧界面在另一维度发起操作。 */
    private int stationMode = -1;
    private net.minecraft.core.BlockPos stationPos = null;
    private net.minecraft.resources.ResourceLocation stationDimension = null;
    private final Map<String, Long> artifactActionCooldowns = new HashMap<>();

    /** 记录台/升级台交互入口(OpenArtifactStationPayload):打开总览"神器"页并携带台座上下文。 */
    public static void openArtifactStation(int mode, net.minecraft.core.BlockPos pos,
                                          net.minecraft.resources.ResourceLocation dimension) {
        EchoOverviewScreen screen = new EchoOverviewScreen(ModulePage.ARTIFACT);
        screen.stationMode = mode;
        screen.stationPos = pos;
        screen.stationDimension = dimension;
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openTrades() {
        if (Minecraft.getInstance().screen instanceof EchoOverviewScreen open
                && open.page == ModulePage.TRADE) {
            open.rebuildKeepingScroll();
            return;
        }
        Minecraft.getInstance().setScreen(new EchoOverviewScreen(ModulePage.TRADE));
    }

    public EchoOverviewScreen(ModulePage initialPage) {
        super(SCREEN_TITLE);
        this.page = initialPage;
    }

    private int panelLeft() {
        return 6;
    }

    private int panelTop() {
        return 6;
    }

    private int panelRight() {
        return this.width - 6;
    }

    private int panelBottom() {
        return this.height - 6;
    }

    private boolean compactLayout() {
        return this.width < 520;
    }

    private boolean wideLayout() {
        return this.width >= 620;
    }

    private int navWidth() {
        if (compactLayout()) {
            return NAV_WIDTH_COMPACT;
        }
        return wideLayout() ? NAV_WIDTH_WIDE : NAV_WIDTH_MID;
    }

    private int contentLeft() {
        return panelLeft() + navWidth() + 6;
    }

    private int contentRight() {
        return panelRight() - 4;
    }

    private int contentWidth() {
        return contentRight() - contentLeft();
    }

    private int contentTop() {
        return panelTop() + HEADER_HEIGHT + 4;
    }

    private int contentBottom() {
        return panelBottom() - STATUS_HEIGHT - 4;
    }

    private int contentHeight() {
        return contentBottom() - contentTop();
    }

    private int journalChapterColumns() {
        return contentWidth() >= 360 ? JOURNAL_CHAPTER_WIDE_COLUMNS : 2;
    }

    private int journalContentTop() {
        int columns = journalChapterColumns();
        int rows = (JOURNAL_CHAPTERS.length + columns - 1) / columns;
        return contentTop() + rows * JOURNAL_CHAPTER_BUTTON_HEIGHT
                + (rows - 1) * JOURNAL_CHAPTER_GAP + 8;
    }

    /** 文本行页的首行 Y。渲染、点击与滚轮必须用同一坐标函数，否则命中行会错位。 */
    private int lineListTop() {
        return this.page == ModulePage.JOURNAL ? journalContentTop() : contentTop() + 8;
    }

    /** 可见行数,下限必须是 1:小 GUI 下可用高度会算成负值,渲染循环恒不成立,日志页整块空白。 */
    private int lineListVisibleRows() {
        return Math.max(1, (contentBottom() - 26 - lineListTop()) / LINE_ROW_HEIGHT);
    }

    /** 行滚动上限。滚轮与渲染都按它钳制，避免滚过尾行后出现空白页。 */
    private int lineScrollMax() {
        return Math.max(0, this.lines.size() - lineListVisibleRows());
    }

    @Override
    protected void init() {
        rebuildWithPageScroll(0);
    }

    private void switchPage(ModulePage target) {
        if (this.page == target) {
            return;
        }
        this.page = target;
        this.chapter = JournalChapter.SUMMARY;
        this.popup = null;
        this.focusTicks = Math.min(this.focusTicks, FOCUS_TICKS / 2);
        rebuild();
    }

    private void rebuild() {
        rebuildWithPageScroll(0);
    }

    private void rebuildKeepingScroll() {
        int previousLineScroll = this.scroll;
        rebuildWithPageScroll(Math.max(0, Math.min(this.pageScroll, pageScrollMax())));
        this.scroll = Mth.clamp(previousLineScroll, 0, lineScrollMax());
    }

    /** pageScroll 必须在 buildPage 之前设好:按钮坐标在构建期算入滚动偏移,否则与卡片错位。 */
    private void rebuildWithPageScroll(int targetScroll) {
        saveClientConfigIfChanged();
        this.setFocused(null);
        this.setDragging(false);
        this.clearWidgets();
        this.lines.clear();
        this.actionButtons.clear();
        this.optionSliders.clear();
        this.optionButtonValues.clear();
        this.optionSections.clear();
        this.configurableOptions.clear();
        this.scroll = 0;
        this.pageScroll = targetScroll;
        buildPage();
        refreshDynamicText();
    }

    /** 神器同步刷新已打开的神器与完成度页，并保留阅读位置。 */
    public static void onArtifactSync() {
        if (Minecraft.getInstance().screen instanceof EchoOverviewScreen open
                && (open.page == ModulePage.ARTIFACT || open.page == ModulePage.INDEX)) {
            open.rebuildKeepingScroll();
        }
    }

    /** 能力与日志同步刷新依赖进度的页面，包括研究提示与隐藏调谐词。 */
    public static void onJournalSync() {
        if (Minecraft.getInstance().screen instanceof EchoOverviewScreen open
                && (open.page == ModulePage.JOURNAL || open.page == ModulePage.RESEARCH
                        || open.page == ModulePage.ARTIFACT || open.page == ModulePage.INDEX)) {
            open.rebuildKeepingScroll();
        }
    }

    @Override
    public void tick() {
        if (this.focusTicks < FOCUS_TICKS) {
            this.focusTicks++;
        }
        refreshDynamicText();
    }

    private void refreshDynamicText() {
        if (this.page == ModulePage.OVERVIEW) {
            for (ModulePage entry : OVERVIEW_PAGES) {
                this.overviewStatusCache[entry.ordinal()] = overviewStatus(entry);
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component dimensionName;
        if (minecraft.level == null) {
            dimensionName = Component.translatable("overview.unknown_echoes.status.region_unknown");
        } else {
            ResourceLocation dimension = minecraft.level.dimension().location();
            dimensionName = Component.translatableWithFallback(
                    "dimension." + dimension.getNamespace() + "." + dimension.getPath(),
                    dimension.getPath());
        }
        this.statusRegion = Component.translatable("overview.unknown_echoes.status.region",
                dimensionName);
        int energy = ClientArtifactCache.getEnergy();
        this.statusEnergyDormant = energy < 0;
        this.statusEnergy = this.statusEnergyDormant
                ? Component.translatable("overview.unknown_echoes.status.energy_dormant")
                : Component.translatable("overview.unknown_echoes.status.energy",
                        energy, ClientArtifactCache.getMaxEnergy());
        if (this.page == ModulePage.ARTIFACT) {
            for (int i = 0; i < CREDENTIAL_ABILITIES.length; i++) {
                this.credentialHolding[i] = clientHasToken(CREDENTIAL_ABILITIES[i]);
            }
        }
    }

    private void buildPage() {
        switch (this.page) {
            case ABILITY -> buildAbilityPage();
            case JOURNAL -> buildJournalPage();
            case RESEARCH -> buildResearchPage();
            case ARTIFACT -> buildArtifactPage();
            case INDEX -> buildIndexPage();
            case OPTIONS -> buildOptionsPage();
            default -> {
            }
        }
    }

    private void buildIndexPage() {
        int score = visibleCompletionScore();
        addWrapped(Component.translatable("overview.unknown_echoes.index.header")
                .withStyle(ChatFormatting.GOLD), null);
        addWrapped(Component.translatable("overview.unknown_echoes.index.entry_requirement"), null);
        addWrapped(Component.empty(), null);
        addIndexLine("overview.unknown_echoes.index.abilities", coreAbilityCount(), 3, 3);
        addIndexLine("overview.unknown_echoes.index.main_bosses", mainBossCount(), MAIN_BOSSES.length,
                MAIN_BOSSES.length);
        addIndexLine("overview.unknown_echoes.index.biomes",
                EchoRealmBiomeCatalog.countKnown(ClientJournalCache.biomes()),
                EchoRealmBiomeCatalog.REQUIRED_COUNT, T7_REQUIRED_BIOMES);
        int firstDimensionPages = AncientPageCatalog.countFirstDimensionPages(ClientJournalCache.pages());
        addIndexLine("overview.unknown_echoes.index.pages",
                firstDimensionPages, AncientPageCatalog.FIRST_DIMENSION_TOTAL,
                T7_REQUIRED_PAGES);
        addIndexLine("overview.unknown_echoes.index.mini_bosses", miniBossCount(), MINI_BOSSES.length,
                T7_REQUIRED_MINI_BOSSES);
        addIndexLine("overview.unknown_echoes.index.archive_terminal",
                ClientAbilityCache.hasArchiveTerminalRecord() ? 1 : 0, 1, 1);
        addIndexLine("overview.unknown_echoes.index.archive_trials",
                ClientAbilityCache.hasCompletedArchiveTrials() ? 1 : 0, 1, 1);
        addIndexLine("overview.unknown_echoes.index.score", score, T7_REQUIRED_SCORE, T7_REQUIRED_SCORE);
        addWrapped(Component.translatable("overview.unknown_echoes.index.calibration")
                .withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.empty(), null);
        String backfillTarget = visibleBackfillTarget();
        if (!backfillTarget.equals("ready")) {
            addWrapped(Component.translatable("overview.unknown_echoes.index.target",
                    Component.translatable("overview.unknown_echoes.index.target." + backfillTarget))
                    .withStyle(ChatFormatting.AQUA), null);
            addWrapped(Component.empty(), null);
        }
        addWrapped(Component.translatable("overview.unknown_echoes.index.deep_header")
                .withStyle(ChatFormatting.GOLD), null);
        addIndexLine("overview.unknown_echoes.index.deep_abilities", deepAbilityCount(),
                DeepEchoesProgressCatalog.ABILITIES.size(), DeepEchoesProgressCatalog.ABILITIES.size());
        addIndexLine("overview.unknown_echoes.index.deep_bosses", deepBossCount(),
                DeepEchoesProgressCatalog.BOSS_IDS.size(), DeepEchoesProgressCatalog.BOSS_IDS.size());
        addIndexLine("overview.unknown_echoes.index.deep_biomes",
                DeepEchoesProgressCatalog.countKnown(ClientJournalCache.biomes(),
                        DeepEchoesProgressCatalog.BIOME_IDS),
                DeepEchoesProgressCatalog.BIOME_IDS.size(), DeepEchoesProgressCatalog.BIOME_IDS.size());
        addIndexLine("overview.unknown_echoes.index.deep_structures",
                DeepEchoesProgressCatalog.countKnown(ClientJournalCache.structures(),
                        DeepEchoesProgressCatalog.STRUCTURE_IDS),
                DeepEchoesProgressCatalog.STRUCTURE_IDS.size(),
                DeepEchoesProgressCatalog.STRUCTURE_IDS.size());
        addIndexLine("overview.unknown_echoes.index.deep_mobs",
                DeepEchoesProgressCatalog.countKnown(ClientJournalCache.mobs(),
                        DeepEchoesProgressCatalog.MOB_IDS),
                DeepEchoesProgressCatalog.MOB_IDS.size(), DeepEchoesProgressCatalog.MOB_IDS.size());
        addIndexLine("overview.unknown_echoes.index.deep_pages",
                DeepEchoesProgressCatalog.countPages(ClientJournalCache.pages()),
                DeepEchoesProgressCatalog.PAGE_COUNT, DeepEchoesProgressCatalog.PAGE_COUNT);
        int deepArtifactSteps = DeepEchoesProgressCatalog.ARTIFACTS.size()
                * (ArtifactType.MAX_LEVEL - 1);
        addIndexLine("overview.unknown_echoes.index.deep_artifacts", deepArtifactUpgradeSteps(),
                deepArtifactSteps, deepArtifactSteps);
    }

    private void addIndexLine(String key, int have, int total, int need) {
        boolean done = have >= need;
        addWrapped(Component.literal(done ? "✦ " : "◇ ")
                .withStyle(done ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY)
                .append(Component.translatable(key, have, total, need)
                        .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)), null);
    }

    /** 神器页:记录台=领取/复领;升级台=升级/调谐;台外只是誊录,按钮不可用。 */
    private void buildArtifactPage() {
        cn.kurt6.unknown_echoes.artifact.ArtifactType[] types = ARTIFACT_TYPES;
        for (int i = 0; i < types.length; i++) {
            cn.kurt6.unknown_echoes.artifact.ArtifactType type = types[i];
            int rowY = artifactRowY(i);
            boolean claimed = cn.kurt6.unknown_echoes.client.ClientArtifactCache.isClaimed(type.getId());
            if (this.stationMode == cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload.MODE_RECORD) {
                addActionButton(new ActionButton(contentRight() - 96, rowY + 6, 88, 18,
                        Component.translatable(claimed
                                ? "overview.unknown_echoes.artifact.reissue"
                                : "overview.unknown_echoes.artifact.claim"),
                        () -> true,
                        () -> sendArtifactAction(
                                cn.kurt6.unknown_echoes.network.ArtifactActionPayload.ACTION_CLAIM,
                                type, ""),
                        false));
            } else if (this.stationMode == cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload.MODE_TUNING) {
                var entry = cn.kurt6.unknown_echoes.client.ClientArtifactCache.get(type.getId());
                addActionButton(new ActionButton(contentRight() - 96, rowY + 6, 88, 18,
                        Component.translatable("overview.unknown_echoes.artifact.upgrade"),

                        () -> ClientArtifactCache.isClaimed(type.getId())
                                && artifactLevel(type) < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL,
                        () -> sendArtifactAction(
                                cn.kurt6.unknown_echoes.network.ArtifactActionPayload.ACTION_UPGRADE,
                                type, ""),
                        false));

                java.util.List<String> words = new ArrayList<>(type.getTuningWords());
                if (ClientAbilityCache.getResearchLevel(type.getLinkedAbility())
                        >= cn.kurt6.unknown_echoes.artifact.ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL) {
                    words.add(type.getHiddenWord());
                }
                String current = entry == null ? "" : entry.tuning();
                int wordGap = 6;
                int wordCount = Math.max(1, words.size());
                int wordWidth = Mth.clamp(
                        (contentWidth() - 12 - wordGap * (wordCount - 1)) / wordCount,
                        52, 112);
                int wordGroupWidth = wordCount * wordWidth + (wordCount - 1) * wordGap;
                int wx = contentLeft() + (contentWidth() - wordGroupWidth) / 2;
                for (String word : words) {
                    Component label = Component.translatable(
                            "artifact.unknown_echoes." + type.getId() + ".tuning." + word);
                    addActionButton(new ActionButton(wx, rowY + 50, wordWidth, 18, label,
                            () -> ClientArtifactCache.isClaimed(type.getId()) && artifactLevel(type) >= 2,
                            () -> sendArtifactAction(
                                    cn.kurt6.unknown_echoes.network.ArtifactActionPayload.ACTION_TUNE,
                                    type, word),
                            word.equals(current),
                            Component.translatable("overview.unknown_echoes.artifact."
                                    + type.getId() + ".tuning." + word)));
                    wx += wordWidth + wordGap;
                }
            }
        }
    }

    private static int artifactLevel(ArtifactType type) {
        var entry = ClientArtifactCache.get(type.getId());
        return entry == null ? 0 : entry.level();
    }

    private static Component artifactEnergyCost(ClientArtifactCache.Entry entry) {
        return Component.translatable("overview.unknown_echoes.artifact.energy_cost",
                formatEnergyCost(entry.energyCostTwentieths()));
    }

    private static void addArtifactLevelTip(List<Component> tooltip, ArtifactType type,
                                            int level, ChatFormatting formatting) {
        String key = "overview.unknown_echoes.artifact." + type.getId() + ".l" + level;
        tooltip.add(Component.translatable(key).withStyle(formatting));
        int detailCount = type == ArtifactType.ECHO_LENS ? 1
                : type == ArtifactType.TIDE_LANTERN ? switch (level) {
                    case 1 -> 2;
                    case 2 -> 3;
                    default -> 5;
                } : 0;
        for (int detail = 1; detail <= detailCount; detail++) {
            String suffix = detail == 1 ? ".detail" : ".detail" + detail;
            tooltip.add(Component.translatable(key + suffix).withStyle(formatting));
        }
    }

    private static void addArtifactLimitsTip(List<Component> tooltip, ArtifactType type) {
        if (type == ArtifactType.TIME_HOURGLASS) {
            tooltip.add(Component.translatable("overview.unknown_echoes.artifact.hourglass_limits")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (type == ArtifactType.DIMENSION_ANCHOR || type == ArtifactType.RIFT_ANCHOR) {
            tooltip.add(Component.translatable("overview.unknown_echoes.artifact.anchor_limits")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String formatEnergyCost(int twentieths) {
        int safe = Math.max(0, twentieths);
        int whole = safe / 20;
        int hundredths = safe % 20 * 5;
        if (hundredths == 0) {
            return Integer.toString(whole);
        }
        if (hundredths % 10 == 0) {
            return whole + "." + hundredths / 10;
        }
        return whole + "." + (hundredths < 10 ? "0" : "") + hundredths;
    }

    private int artifactRowY(int index) {
        return contentTop() + 8 + index * artifactCardStride() - this.pageScroll;
    }

    private int artifactCardHeight() {
        return this.stationMode == cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload.MODE_TUNING
                ? 72 : 44;
    }

    private int artifactCardStride() {
        return artifactCardHeight() + 10;
    }

    private void sendArtifactAction(int action, cn.kurt6.unknown_echoes.artifact.ArtifactType type,
                                    String word) {
        var level = Minecraft.getInstance().level;
        if (this.stationPos == null || level == null
                || !level.dimension().location().equals(this.stationDimension)) {
            return;
        }
        String key = action + ":" + type.getId() + ":" + word;
        long now = level.getGameTime();
        long nextAllowed = this.artifactActionCooldowns.getOrDefault(key, Long.MIN_VALUE);
        if (now < nextAllowed) {
            return;
        }
        this.artifactActionCooldowns.put(key, now + 10L);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new cn.kurt6.unknown_echoes.network.ArtifactActionPayload(
                        action, type.getId(), word, this.stationPos, this.stationDimension));
    }

    private void buildAbilityPage() {

        addActionButton(new ActionButton(contentLeft(), contentBottom() - 24, 152, 20,
                toggleLabel("overview.unknown_echoes.option.glide", ClientConfig.WIND_THIRD_JUMP_GLIDE.get()),
                () -> ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)
                        && ClientAbilityCache.getResearchLevel(EchoAbilityType.WIND_ECHO) >= 1,
                () -> {
                    ClientConfig.WIND_THIRD_JUMP_GLIDE.set(!ClientConfig.WIND_THIRD_JUMP_GLIDE.get());
                    this.clientConfigDirty = true;
                    rebuild();
                },
                true));
        addActionButton(new ActionButton(contentLeft() + 160, contentBottom() - 24, 112, 20,
                Component.translatable("overview.unknown_echoes.ability.to_journal"),
                () -> true,
                () -> switchPage(ModulePage.JOURNAL),
                false));
    }

    private void buildJournalPage() {

        int columns = journalChapterColumns();
        int buttonWidth = (contentWidth() - (columns - 1) * JOURNAL_CHAPTER_GAP) / columns;
        for (int i = 0; i < JOURNAL_CHAPTERS.length; i++) {
            JournalChapter c = JOURNAL_CHAPTERS[i];
            int column = i % columns;
            int row = i / columns;
            int x = contentLeft() + column * (buttonWidth + JOURNAL_CHAPTER_GAP);
            int y = contentTop() + row * (JOURNAL_CHAPTER_BUTTON_HEIGHT + JOURNAL_CHAPTER_GAP);
            int width = column == columns - 1 ? contentRight() - x : buttonWidth;
            Component label = Component.translatable("overview.unknown_echoes.journal." + c.name().toLowerCase());
            JournalChapter target = c;
            addActionButton(new ActionButton(x, y, width, JOURNAL_CHAPTER_BUTTON_HEIGHT, label,
                    () -> this.chapter != target,
                    () -> {
                        this.chapter = target;
                        rebuild();
                    },
                    this.chapter == c));
        }
        buildJournalLines();
    }

    private void buildJournalLines() {
        List<Component> content = new ArrayList<>();
        switch (this.chapter) {
            case JOURNEY -> buildJourneyLines();
            case SUMMARY -> {
                ExplorationClue destination = ClientJournalCache.destination();
                addWrapped((destination == null
                        ? Component.translatable("journal.unknown_echoes.destination.choose")
                        : Component.translatable("journal.unknown_echoes.destination.current", JournalNavigation.structureName(destination)))
                        .withStyle(ChatFormatting.AQUA), () -> {
                    this.chapter = JournalChapter.CLUES;
                    rebuild();
                });
                if (destination != null) {
                    addWrapped(clueLine(destination), null);
                }
                addWrapped(Component.empty(), null);
                addWrapped(Component.translatable("journey.unknown_echoes.summary", ClientJourneyCache.claimedCount(),
                        JourneyCatalog.CHAPTERS.size(), ClientJourneyCache.readyCount()).withStyle(ChatFormatting.GOLD), () -> {
                    this.chapter = JournalChapter.JOURNEY;
                    rebuild();
                });
                addWrapped(Component.empty(), null);
                content.add(count("journal.unknown_echoes.structures",
                        ClientJournalCache.structures().size(), ClientJournalCache.totalStructures()));
                content.add(count("journal.unknown_echoes.biomes",
                        ClientJournalCache.biomes().size(), ClientJournalCache.totalBiomes()));
                content.add(count("journal.unknown_echoes.mobs",
                        ClientJournalCache.mobs().size(), ClientJournalCache.totalMobs()));
                content.add(count("journal.unknown_echoes.bosses",
                        ClientJournalCache.bosses().size(), ClientJournalCache.totalBosses()));
                content.add(count("journal.unknown_echoes.pages",
                        ClientJournalCache.pages().size(), ClientJournalCache.knownPages()));
            }
            case CLUES -> {
                addWrapped(Component.translatable("journal.unknown_echoes.destination.hint")
                        .withStyle(ChatFormatting.GRAY), null);
                addWrapped(Component.empty(), null);
                ExplorationClue destination = ClientJournalCache.destination();
                List<ExplorationClue> clues = ClientJournalCache.clues().stream()
                        .sorted(Comparator.comparing((ExplorationClue clue) -> !clue.sameSite(destination))
                                .thenComparing(ExplorationClue::visited))
                        .toList();
                for (ExplorationClue clue : clues) {
                    boolean selected = clue.sameSite(destination);
                    addWrapped(clueLine(clue), null);
                    addWrapped(Component.translatable(selected
                                    ? "journal.unknown_echoes.destination.clear" : "journal.unknown_echoes.destination.set")
                            .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.GOLD), () ->
                            PacketDistributor.sendToServer(new JournalDestinationPayload(selected ? "" : clue.encode())));
                    addWrapped(Component.empty(), null);
                }
                if (clues.isEmpty()) {
                    content.add(Component.translatable("journal.unknown_echoes.clue.none")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            case BIOMES -> {
                for (String biome : KNOWN_BIOMES) {
                    addDiscoveredEntry(content, ClientJournalCache.biomes().contains(id(biome)),
                            Component.translatable("biome.unknown_echoes." + biome));
                }
                addNoRecordsIfEmpty(content);
            }
            case STRUCTURES -> {
                for (String structure : knownStructurePaths()) {
                    boolean discovered = ClientJournalCache.structures().contains(id(structure));
                    addDiscoveredEntry(content, discovered,
                            Component.translatable("structure.unknown_echoes." + structure));
                    addWonderHint(content, structure, discovered);
                }
                addNoRecordsIfEmpty(content);
            }
            case MOBS -> {
                for (String mob : KNOWN_MOBS) {
                    addDiscoveredEntry(content, ClientJournalCache.mobs().contains(id(mob)),
                            Component.translatable("entity.unknown_echoes." + mob));
                }
                addNoRecordsIfEmpty(content);
            }
            case BOSSES -> {
                for (String boss : JOURNAL_BOSSES) {
                    addDiscoveredEntry(content, ClientJournalCache.bosses().contains(id(boss)),
                            Component.translatable("entity.unknown_echoes." + boss));
                }
                addNoRecordsIfEmpty(content);
            }
            case ABILITIES -> {
                for (EchoAbilityType type : ABILITY_TYPES) {
                    boolean unlocked = ClientAbilityCache.hasAbility(type);
                    Component name = Component.translatable("ability.unknown_echoes." + type.getId());
                    if (unlocked) {
                        content.add(Component.literal("✦ ").withStyle(ChatFormatting.DARK_AQUA)
                                .append(name)
                                .append(Component.literal("  "
                                                + bar(ClientAbilityCache.getResearchLevel(type)))
                                        .withStyle(ChatFormatting.DARK_AQUA)));
                    } else {
                        content.add(Component.literal("◇ ").withStyle(ChatFormatting.GRAY)
                                .append(name.copy().withStyle(ChatFormatting.GRAY)));
                    }
                }
            }
            case PAGES -> {
                addPageCategories();
            }
            case REVISIT -> buildRevisitLines();
        }
        for (Component component : content) {
            addWrapped(component, null);
        }
    }

    private void buildRevisitLines() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) return;
        boolean any = false;
        for (JournalRevisit revisit : JournalRevisit.values()) {
            if (!revisit.available(ClientJournalCache.structures(), ClientAbilityCache::hasAbility)) continue;
            if (!any) {
                addWrapped(Component.translatable("journal.unknown_echoes.revisit.entry_hint")
                        .withStyle(ChatFormatting.GRAY), null);
            }
            any = true;
            addWrapped(Component.empty(), null);
            addWrapped(Component.translatable(revisit.hintKey()).withStyle(ChatFormatting.DARK_PURPLE), null);
            ExplorationClue route = revisit.visitedSite(ClientJournalCache.clues(), ClientJournalCache.destination(),
                    this.minecraft.level.dimension().location().toString(), this.minecraft.player.blockPosition());
            if (route == null) {
                addWrapped(Component.translatable("journal.unknown_echoes.revisit.unknown")
                        .withStyle(ChatFormatting.DARK_GRAY), null);
                continue;
            }
            addWrapped(clueLine(route), null);
            if (route.sameSite(ClientJournalCache.destination())) {
                addWrapped(Component.translatable("journal.unknown_echoes.route.selected")
                        .withStyle(ChatFormatting.AQUA), null);
            } else {
                addWrapped(Component.translatable("journal.unknown_echoes.destination.set")
                        .withStyle(ChatFormatting.AQUA), () -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new cn.kurt6.unknown_echoes.network.JournalDestinationPayload(route.encode())));
            }
        }
        if (!any) {
            addWrapped(Component.translatable("journal.unknown_echoes.book.no_revisit")
                    .withStyle(ChatFormatting.GRAY), null);
        }
    }

    private void buildJourneyLines() {
        addWrapped(Component.translatable("journey.unknown_echoes.intro").withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.translatable("journey.unknown_echoes.puzzle_hint").withStyle(ChatFormatting.DARK_AQUA), null);
        for (int index = 0; index < JourneyCatalog.CHAPTERS.size(); index++) {
            var entry = JourneyCatalog.CHAPTERS.get(index);
            boolean claimed = ClientJourneyCache.claimed(index);
            addWrapped(Component.empty(), null);
            addWrapped(Component.translatable("journey.unknown_echoes.chapter." + entry.id())
                    .withStyle(claimed ? ChatFormatting.DARK_AQUA : ChatFormatting.GOLD), null);
            addWrapped(Component.translatable("journey.unknown_echoes.hint." + entry.id())
                    .withStyle(ChatFormatting.GRAY), null);
            for (var goal : entry.goals()) {
                addWrapped(Component.translatable("journey.unknown_echoes.progress",
                        Component.translatable("journey.unknown_echoes.metric." + goal.metric().name().toLowerCase(java.util.Locale.ROOT)),
                        Math.min(goal.target(), goal.current(ClientJourneyCache.values())), goal.target())
                        .withStyle(goal.complete(ClientJourneyCache.values()) ? ChatFormatting.GREEN : ChatFormatting.GRAY), null);
            }
            var rewards = Component.translatable("journey.unknown_echoes.rewards");
            for (int i = 0; i < entry.rewards().size(); i++) {
                var reward = entry.rewards().get(i);
                if (i > 0) rewards.append(Component.translatable("journey.unknown_echoes.separator"));
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(UnknownEchoes.id(reward.item()));
                rewards.append(Component.translatable("journey.unknown_echoes.reward", item.getDescription(), reward.count()));
            }
            addWrapped(rewards.withStyle(ChatFormatting.GRAY), null);
            final int chapterIndex = index;
            if (claimed) {
                addWrapped(Component.translatable("journey.unknown_echoes.claimed").withStyle(ChatFormatting.DARK_AQUA), null);
            } else if (ClientJourneyCache.ready(index)) {
                addWrapped(Component.translatable("journey.unknown_echoes.claim").withStyle(ChatFormatting.AQUA), () -> {
                    if (ClientJourneyCache.ready(chapterIndex)) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new cn.kurt6.unknown_echoes.network.JourneyClaimPayload(chapterIndex));
                    }
                });
            } else {
                addWrapped(Component.translatable("journey.unknown_echoes.in_progress").withStyle(ChatFormatting.DARK_GRAY), null);
            }
        }
        buildStudyLines();
        buildChaseLines();
    }

    private void buildChaseLines() {
        addWrapped(Component.empty(), null);
        addWrapped(Component.translatable("chase.unknown_echoes.title").withStyle(ChatFormatting.GOLD), null);
        addWrapped(Component.translatable("chase.unknown_echoes.entry").withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.translatable("chase.unknown_echoes.collection_hint").withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.translatable("journal.unknown_echoes.route.entry_hint").withStyle(ChatFormatting.GRAY), null);
        for (int theme = 0; theme < cn.kurt6.unknown_echoes.block.puzzle.EchoChaseCatalog.THEMES; theme++) {
            addWrapped(Component.translatable("chase.unknown_echoes.collection",
                    Component.translatable("structure.unknown_echoes." + cn.kurt6.unknown_echoes.block.puzzle.EchoChaseCatalog.IDS.get(theme)),
                    Component.translatable("chase.unknown_echoes." + cn.kurt6.unknown_echoes.client.ClientChaseCache.status(theme)))
                    .withStyle(ChatFormatting.DARK_AQUA), null);
            if (!cn.kurt6.unknown_echoes.client.ClientChaseCache.completed(theme)
                    || cn.kurt6.unknown_echoes.client.ClientChaseCache.pending(theme)) {
                addWorkshopRoute(cn.kurt6.unknown_echoes.block.puzzle.EchoChaseCatalog.IDS.get(theme),
                        cn.kurt6.unknown_echoes.client.ClientChaseCache.pending(theme));
            }
        }
    }

    private void buildStudyLines() {
        addWrapped(Component.empty(), null);
        addWrapped(Component.translatable("study.unknown_echoes.title").withStyle(ChatFormatting.GOLD), null);
        addWrapped(Component.translatable("study.unknown_echoes.enter").withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.translatable("study.unknown_echoes.collection_hint").withStyle(ChatFormatting.GRAY), null);
        addWrapped(Component.translatable("journal.unknown_echoes.route.entry_hint").withStyle(ChatFormatting.GRAY), null);
        for (int theme = 0; theme < cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog.THEMES; theme++) {
            addWrapped(Component.translatable("study.unknown_echoes.collection",
                    Component.translatable("structure.unknown_echoes." + cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog.IDS.get(theme)),
                    cn.kurt6.unknown_echoes.client.ClientStudyCache.rating(theme),
                    cn.kurt6.unknown_echoes.client.ClientStudyCache.pending(theme)).withStyle(ChatFormatting.DARK_AQUA), null);
            if (cn.kurt6.unknown_echoes.client.ClientStudyCache.rating(theme) < 3
                    || cn.kurt6.unknown_echoes.client.ClientStudyCache.pending(theme) > 0) {
                addWorkshopRoute(cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog.IDS.get(theme),
                        cn.kurt6.unknown_echoes.client.ClientStudyCache.pending(theme) > 0);
            }
        }
    }

    private void addWorkshopRoute(String structurePath, boolean pending) {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) return;
        ExplorationClue route = JournalNavigation.knownSite(ClientJournalCache.clues(),
                UnknownEchoes.id(structurePath).toString(), this.minecraft.level.dimension().location().toString(),
                this.minecraft.player.blockPosition());
        if (route == null) {
            addWrapped(Component.translatable("journal.unknown_echoes.route.unknown").withStyle(ChatFormatting.DARK_GRAY), null);
            return;
        }
        addWrapped(clueLine(route), null);
        boolean selected = route.sameSite(ClientJournalCache.destination());
        String key = selected ? "selected" : pending ? "return" : "explore";
        addWrapped(Component.translatable("journal.unknown_echoes.route." + key).withStyle(ChatFormatting.AQUA),
                selected ? null : () -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new cn.kurt6.unknown_echoes.network.JournalDestinationPayload(route.encode())));
    }

    private List<String> knownStructurePaths() {
        LinkedHashSet<String> structures = new LinkedHashSet<>(List.of(KNOWN_STRUCTURES));
        if (this.minecraft != null && this.minecraft.level != null) {
            this.minecraft.level.registryAccess().registry(Registries.STRUCTURE)
                    .ifPresent(registry -> registry.keySet().stream()
                            .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID))
                            .map(ResourceLocation::getPath)
                            .sorted()
                            .forEach(structures::add));
        }
        return List.copyOf(structures);
    }

    private Component clueLine(ExplorationClue clue) {
        Component structureName = JournalNavigation.structureName(clue);
        if (clue.visited()) {
            structureName = Component.translatable("journal.unknown_echoes.clue.visited", structureName);
        }
        var marker = Component.literal(clue.visited() ? "◆ " : "◇ ")
                .withStyle(clue.sameSite(ClientJournalCache.destination()) ? ChatFormatting.AQUA
                        : clue.visited() ? ChatFormatting.DARK_AQUA : ChatFormatting.GOLD);

        ResourceLocation clueDimension = ResourceLocation.tryParse(clue.dimension());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || clueDimension == null
                || !minecraft.level.dimension().location().equals(clueDimension)) {
            return marker
                    .append(Component.translatable("journal.unknown_echoes.clue.other_dimension",
                                    structureName, JournalNavigation.dimensionName(clue.dimension()))
                            .withStyle(ChatFormatting.GRAY));
        }

        double dx = clue.pos().getX() + 0.5D - minecraft.player.getX();
        double dz = clue.pos().getZ() + 0.5D - minecraft.player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < JournalNavigation.NEARBY_RADIUS) {
            return marker.append(Component.translatable("journal.unknown_echoes.clue.nearby", structureName)
                    .withStyle(ChatFormatting.GRAY));
        }
        String direction = clueDirection(dx, dz);
        String distanceKey = distance < 200.0D ? "near" : distance < 800.0D ? "mid" : "far";
        return marker
                .append(Component.translatable("journal.unknown_echoes.clue.active",
                                structureName,
                                Component.translatable("direction.unknown_echoes." + direction),
                                Component.translatable("message.unknown_echoes.storm_compass.dist." + distanceKey))
                        .withStyle(ChatFormatting.GRAY));
    }

    private static String clueDirection(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0.0D) {
            angle += 360.0D;
        }
        String[] directions = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return directions[(int) Math.floor(((angle + 22.5D) % 360.0D) / 45.0D)];
    }

    private void buildResearchPage() {

        for (EchoResearchLine line : EchoResearchLine.values()) {
            boolean unlocked = !line.isAbilityGated()
                    ? line.getRequiredBiomes().isEmpty()
                    || line.getRequiredBiomes().stream().anyMatch(ClientJournalCache.biomes()::contains)
                    : ClientAbilityCache.hasAbility(line.getRequiredAbility());
            Component name = Component.translatable("research.unknown_echoes." + line.getId());
            int level = ClientAbilityCache.getResearchLevel(line);
            if (unlocked) {
                addWrapped(Component.literal("✦ ").withStyle(ChatFormatting.DARK_AQUA)
                        .append(name)
                        .append(Component.literal("  " + bar(level))
                                .withStyle(ChatFormatting.DARK_AQUA)), null);
            } else {
                addWrapped(Component.literal("◇ ").withStyle(ChatFormatting.GRAY)
                        .append(name.copy().withStyle(ChatFormatting.GRAY)), null);
            }
            addResearchHint(line, unlocked, level);
        }
        addWrapped(Component.empty(), null);
        addWrapped(Component.translatable("overview.unknown_echoes.research.pages_header")
                .withStyle(ChatFormatting.GOLD), null);
        addPageCategories();
    }

    private void addResearchHint(EchoResearchLine line, boolean unlocked, int level) {
        Component hint;
        if (!unlocked) {
            if (line.isAbilityGated()) {
                hint = Component.translatable("overview.unknown_echoes.research.locked.ability",
                        Component.translatable("ability.unknown_echoes." + line.getRequiredAbility().getId()));
            } else {
                hint = Component.translatable("overview.unknown_echoes.research.locked.biome",
                        firstRequiredBiomeName(line));
            }
        } else if (level >= 4) {
            hint = Component.translatable("overview.unknown_echoes.research.complete");
        } else {
            hint = Component.translatable("overview.unknown_echoes.research.next."
                    + line.getId() + "." + (level + 1), researchHintArgs(line, level + 1));
        }
        addWrapped(Component.literal("  ↳ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(hint.copy().withStyle(ChatFormatting.GRAY)), null);
    }

    /** 面板悬停提示:重力/虚空的基础冷却按当前配置填充,其余能力沿用静态文本。 */
    private static net.minecraft.network.chat.MutableComponent abilityHint(EchoAbilityType type) {
        if (type == EchoAbilityType.GRAVITY_ECHO) {
            return Component.translatable("screen.unknown_echoes.ability_panel.hint.gravity_echo",
                    Component.keybind("key.unknown_echoes.gravity_attune"),
                    cooldownSecondsText(ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS, 0));
        }
        if (type == EchoAbilityType.VOID_ECHO) {
            return Component.translatable("screen.unknown_echoes.ability_panel.hint.void_echo",
                    Component.keybind("key.unknown_echoes.void_step"),
                    cooldownSecondsText(ServerConfig.VOID_STEP_COOLDOWN_TICKS, 0));
        }
        return ABILITY_HINTS[type.ordinal()].copy();
    }

    /** 重力/虚空冷却提示实时读取同步后的服务器配置,与实际结算共用折减公式,不得写死数值。 */
    private static Object[] researchHintArgs(EchoResearchLine line, int nextLevel) {
        if (line == EchoResearchLine.GRAVITY) {
            return new Object[]{
                    gravityRadiusText(nextLevel),
                    gravityDurationSecondsText(nextLevel),
                    cooldownSecondsText(ServerConfig.GRAVITY_ACTIVE_COOLDOWN_TICKS, nextLevel)
            };
        }
        if (line == EchoResearchLine.VOID) {
            return new Object[]{cooldownSecondsText(
                    ServerConfig.VOID_STEP_COOLDOWN_TICKS, nextLevel)};
        }
        return new Object[0];
    }

    private static String gravityRadiusText(int research) {
        double base = ServerConfig.SPEC.isLoaded()
                ? ServerConfig.GRAVITY_ACTIVE_RADIUS.get()
                : ServerConfig.GRAVITY_ACTIVE_RADIUS.getDefault();
        double radius = DeepEchoAbilityHandler.gravityRadius(base, research);
        return radius == Math.rint(radius) ? String.valueOf((int) radius)
                : String.format(java.util.Locale.ROOT, "%.1f", radius);
    }

    private static String gravityDurationSecondsText(int research) {
        int base = ServerConfig.SPEC.isLoaded()
                ? ServerConfig.GRAVITY_ACTIVE_DURATION_TICKS.get()
                : ServerConfig.GRAVITY_ACTIVE_DURATION_TICKS.getDefault();
        int ticks = DeepEchoAbilityHandler.gravityDurationTicks(base, research);
        return ticks % 20 == 0 ? String.valueOf(ticks / 20)
                : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private static String cooldownSecondsText(net.neoforged.neoforge.common.ModConfigSpec.IntValue baseTicks,
                                              int research) {
        int base = ServerConfig.SPEC.isLoaded() ? baseTicks.get() : baseTicks.getDefault();
        long ticks = DeepEchoAbilityHandler.scaledCooldown(base, research);
        return ticks % 20L == 0L ? String.valueOf(ticks / 20L)
                : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private Component firstRequiredBiomeName(EchoResearchLine line) {
        if (line.getRequiredBiomes().isEmpty()) {
            return Component.translatable("overview.unknown_echoes.research.locked.place_unknown");
        }
        String id = line.getRequiredBiomes().iterator().next();
        int sep = id.indexOf(':');
        String path = sep >= 0 ? id.substring(sep + 1) : id;
        return Component.translatable("biome.unknown_echoes." + path);
    }

    private void addPageLine(int pageId) {
        Component title = Component.translatable("page.unknown_echoes." + pageId + ".title");
        addWrapped(Component.literal("✦ ").withStyle(ChatFormatting.DARK_GREEN)
                        .append(title.copy().withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.UNDERLINE)),
                () -> this.popup = new ReadingPopup(title,
                        Component.translatable("page.unknown_echoes." + pageId + ".text")));
    }

    private void addPageCategories() {
        boolean anyRead = false;
        for (AncientPageCatalog.Category category : AncientPageCatalog.CATEGORIES) {
            int read = category.read(ClientJournalCache.pages());
            addWrapped(Component.translatable("journal.unknown_echoes.pages.category",
                    Component.translatable("journal.unknown_echoes.pages.category." + category.id()),
                    read, category.total()).withStyle(read >= category.total()
                    ? ChatFormatting.GOLD : ChatFormatting.GRAY), null);
            for (int pageId : category.pageIds()) {
                if (ClientJournalCache.pages().contains(pageId)) {
                    anyRead = true;
                    addPageLine(pageId);
                }
            }
        }
        if (!anyRead) {
            addWrapped(Component.translatable("journal.unknown_echoes.book.no_pages")
                    .withStyle(ChatFormatting.GRAY), null);
        }
    }

    private void buildOptionsPage() {
        int y = addOptionSection(4, "overview.unknown_echoes.option.section.general");
        y = addToggle(y, "overview.unknown_echoes.option.glide", ClientConfig.WIND_THIRD_JUMP_GLIDE);
        y = addToggle(y, "overview.unknown_echoes.option.particles", ClientConfig.ENABLE_ECHO_PARTICLES);
        y = addToggle(y, "overview.unknown_echoes.option.ambient", ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS);
        y = addOptionSection(y + 8, "overview.unknown_echoes.option.section.boss");
        y = addToggle(y, "overview.unknown_echoes.option.boss_particles", ClientConfig.ENABLE_BOSS_PARTICLES);
        y = addSlider(y, "overview.unknown_echoes.option.boss_density", ClientConfig.BOSS_PARTICLE_DENSITY);
        y = addOptionSection(y + 8, "overview.unknown_echoes.option.section.disaster");
        y = addToggle(y, "overview.unknown_echoes.option.disaster_effects", ClientConfig.DISASTER_SCREEN_EFFECTS);
        y = addSlider(y, "overview.unknown_echoes.option.disaster_shake", ClientConfig.DISASTER_SCREEN_SHAKE);
        y = addSlider(y, "overview.unknown_echoes.option.disaster_density", ClientConfig.DISASTER_VISUAL_DENSITY);
        y = addSlider(y, "overview.unknown_echoes.option.disaster_volume", ClientConfig.DISASTER_SOUND_VOLUME);
        y += 8;
        addActionButton(new ActionButton(contentLeft(), optionRowY(y), optionControlWidth(), 20,
                Component.translatable("overview.unknown_echoes.option.reset"), () -> true,
                () -> {
                    this.configurableOptions.forEach(EchoOverviewScreen::resetOption);
                    this.clientConfigDirty = true;
                    rebuildKeepingScroll();
                }, false, Component.translatable("overview.unknown_echoes.option.reset.hint")));
        this.optionsContentHeight = y + 20;
    }

    private int optionsViewportTop() {
        return contentTop() + 22;
    }

    private int optionsViewportBottom() {
        return contentBottom() - 16;
    }

    private int optionRowY(int offset) {
        return optionsViewportTop() + offset - this.pageScroll;
    }

    private int optionControlWidth() {
        return Math.max(20, Math.min(360, contentWidth() - 16));
    }

    private int addOptionSection(int y, String key) {
        this.optionSections.add(new OptionSection(optionRowY(y), Component.translatable(key)));
        return y + 18;
    }

    private int addToggle(int y, String key, ModConfigSpec.BooleanValue value) {
        this.configurableOptions.add(value);
        ActionButton button = new ActionButton(contentLeft(), optionRowY(y), optionControlWidth(), 20,
                Component.translatable(key),
                () -> true,
                () -> {
                    value.set(!value.get());
                    this.clientConfigDirty = true;
                    rebuildKeepingScroll();
                },
                value.get(), Component.translatable(key + ".hint"));
        addActionButton(button);
        this.optionButtonValues.put(button, Component.translatable(value.get()
                ? "overview.unknown_echoes.option.on" : "overview.unknown_echoes.option.off"));
        return y + 24;
    }

    private int addSlider(int y, String key, ModConfigSpec.DoubleValue value) {
        this.configurableOptions.add(value);
        ClientOptionSlider slider = new ClientOptionSlider(optionRowY(y), key, value);
        this.optionSliders.add(slider);
        this.addWidget(slider);
        return y + 24;
    }

    private static <T> void resetOption(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }

    private void saveClientConfigIfChanged() {
        if (this.clientConfigDirty) {
            ClientConfig.SPEC.save();
            this.clientConfigDirty = false;
        }
    }

    @Override
    public void removed() {
        saveClientConfigIfChanged();
        super.removed();
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(
                on ? "overview.unknown_echoes.option.on" : "overview.unknown_echoes.option.off"));
    }

    private void addActionButton(ActionButton button) {
        this.actionButtons.add(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float focus = Mth.clamp((this.focusTicks + partialTick) / FOCUS_TICKS, 0.0F, 1.0F);
        renderBackdrop(graphics, focus);
        renderNav(graphics, mouseX, mouseY, focus);
        renderHeader(graphics, focus);
        renderContent(graphics, mouseX, mouseY, focus);
        renderStatusBar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderActionButtons(graphics, mouseX, mouseY);
        if (this.popup == null) {
            renderResponsiveTooltips(graphics, mouseX, mouseY);
        }
        if (this.popup != null) {
            renderPopup(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

    }

    private void renderBackdrop(GuiGraphics graphics, float focus) {
        int navWidth = navWidth();
        int alphaTop = (int) ((COL_BACKDROP_TOP >>> 24) * focus) << 24 | (COL_BACKDROP_TOP & 0xFFFFFF);
        int alphaBottom = (int) ((COL_BACKDROP_BOTTOM >>> 24) * focus) << 24 | (COL_BACKDROP_BOTTOM & 0xFFFFFF);
        graphics.fillGradient(0, 0, this.width, this.height, alphaTop, alphaBottom);
        graphics.fillGradient(0, 0, this.width, this.height, 0x11000000, 0x88000000);

        drawGlassPanel(graphics, panelLeft(), panelTop(), panelRight(), panelBottom(), COL_ACCENT_DIM, true);
        graphics.setColor(0.70F, 0.88F, 0.92F, 0.56F * focus);
        graphics.blit(OVERVIEW_BACKDROP, panelLeft() + 3, panelTop() + 3, 0, 0,
                panelRight() - panelLeft() - 6, panelBottom() - panelTop() - 6,
                OVERVIEW_BACKDROP_SIZE, OVERVIEW_BACKDROP_SIZE);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fillGradient(panelLeft() + 3, panelTop() + 3,
                panelRight() - 3, panelBottom() - 3, 0x22101824, 0x9903050A);
        graphics.fill(panelLeft() + 3, panelTop() + 3, panelRight() - 3, panelBottom() - 3, 0x7604070D);
        graphics.fillGradient(panelLeft() + 4, panelTop() + 4,
                panelLeft() + 92, panelBottom() - 4, 0x2538A59E, 0x00101824);
        graphics.fillGradient(panelRight() - 148, panelTop() + 4,
                panelRight() - 4, panelBottom() - 4, 0x20E2C276, 0x0003050A);
        drawSoftGrid(graphics, panelLeft() + navWidth + 12, panelTop() + 48,
                panelRight() - 16, panelBottom() - STATUS_HEIGHT - 10, 0x1138A59E);

        int cx = (panelLeft() + panelRight()) / 2;
        int cy = (panelTop() + panelBottom()) / 2;
        graphics.fillGradient(cx - 170, cy - 96, cx + 170, cy + 104, COL_ACCENT_HAZE, 0x00101824);
        graphics.fillGradient(cx - 90, panelTop() + 8, cx + 120, panelTop() + 34,
                0x18E2C276, 0x00101824);
        graphics.fillGradient(panelLeft() + 8, panelTop() + 10, panelRight() - 8, panelTop() + 46,
                0x2559D6C8, 0x00101824);
        for (int i = 0; i < 7; i++) {
            int y = panelTop() + 42 + i * 24;
            drawFineDivider(graphics, panelLeft() + 12, y, panelRight() - 12,
                    i % 2 == 0 ? 0x1A62E6D8 : 0x168D7440);
        }
        graphics.fill(panelLeft() + navWidth + 1, panelTop() + 2,
                panelLeft() + navWidth + 2, panelBottom() - 2, 0x772D8B86);

        int sweep = (int) ((panelRight() - panelLeft()) * focus);
        graphics.fill(panelLeft() + 3, panelTop() + 3, panelLeft() + 3 + sweep, panelTop() + 4, COL_ACCENT);
        graphics.fill(panelLeft() + 3, panelBottom() - 4,
                panelLeft() + 3 + sweep / 2, panelBottom() - 3, COL_GOLD_DIM);
        graphics.fillGradient(panelLeft() + 8, panelTop() + 6,
                panelLeft() + 8 + Math.max(0, sweep - 12), panelTop() + 8, 0x0062E6D8, COL_ACCENT_GLOW);
        for (int i = 0; i < 9; i++) {
            int px = panelLeft() + navWidth + 24 + i * 37;
            int py = panelTop() + 18 + (i * 29) % Math.max(24, panelBottom() - panelTop() - 44);
            graphics.fill(px, py, px + 1, py + 1, i % 3 == 0 ? COL_GOLD_DIM : COL_ACCENT_DIM);
        }
    }

    private void renderHeader(GuiGraphics graphics, float focus) {
        int x = contentLeft();
        int y = panelTop() + 6;
        graphics.fillGradient(x - 6, y - 5, contentRight() + 1, y + 19, 0x9A142330, 0x2203050A);
        graphics.fillGradient(x - 6, y - 5, x + 120, y + 19, 0x3438A59E, 0x00101824);
        graphics.fill(x - 6, y - 5, x - 3, y + 19, COL_GOLD_DIM);
        graphics.fillGradient(x - 2, y - 4, x + 22, y + 18, 0x6638A59E, 0xAA03050A);
        graphics.renderOutline(x - 2, y - 4, 24, 22, COL_EDGE_SOFT);
        drawCornerMarks(graphics, x - 2, y - 4, x + 22, y + 18, COL_GOLD_DIM);
        float iconShade = 0.72F + 0.28F * focus;
        graphics.setColor(iconShade, iconShade, iconShade, 1.0F);
        graphics.blit(ICONS, x + 1, y - 2, 18, 18,
                moduleIconU(this.page), 0.0F, ICON_SIZE, ICON_SIZE,
                MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(contentRight() - 48, y - 3, contentRight() - 46, y + 17, COL_ACCENT_DIM);
        graphics.fill(contentRight() - 38, y + 3, contentRight() - 22, y + 4, COL_EDGE_SOFT);
        graphics.fill(contentRight() - 34, y + 8, contentRight() - 16, y + 9, COL_GOLD_DIM);
        int textX = x + 27;
        if (focus >= 1.0F || (this.focusTicks * 3) % 2 == 0) {
            graphics.drawString(this.font, this.page.title(), textX, y, COL_GOLD, true);
        }
        drawFineDivider(graphics, textX, y + 13, contentRight(), COL_ACCENT_DIM);
        graphics.fill(textX, y + 14, textX + Math.max(24, contentWidth() / 5), y + 15, COL_ACCENT);
        drawRuneTick(graphics, contentRight() - 20, y + 4, COL_GOLD_DIM);
    }

    private void renderNav(GuiGraphics graphics, int mouseX, int mouseY, float focus) {
        int x = panelLeft();
        int top = panelTop();
        int navWidth = navWidth();
        boolean compact = compactLayout();
        graphics.fillGradient(x, top, x + navWidth, panelBottom(), 0xE0142431, 0xF004070C);
        graphics.fillGradient(x + 3, top + 4, x + navWidth - 4, panelBottom() - 4,
                0x44294755, 0x77000000);
        drawSoftGrid(graphics, x + 6, top + 34, x + navWidth - 8, panelBottom() - 10, 0x1038A59E);
        graphics.fill(x + navWidth, top, x + navWidth + 1, panelBottom(), COL_EDGE);
        graphics.fill(x + navWidth - 2, top + 6, x + navWidth - 1, panelBottom() - 8, COL_ACCENT_HAZE);
        graphics.fillGradient(x + 6, top + 5, x + navWidth - 8, top + 28, 0x2538A59E, 0x0003050A);
        if (compact) {
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.92F);
            graphics.blit(STAR_DISC, x + 11, top + 4, 26, 26,
                    0.0F, 0.0F, 96, 96, 96, 96);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            graphics.drawString(this.font, NAV_TITLE_TOP,
                    x + 7, top + 6, COL_GOLD_DIM, false);
            graphics.drawString(this.font, NAV_TITLE_BOTTOM,
                    x + 18, top + 16, COL_ACCENT_DIM, false);
            if (navWidth >= NAV_WIDTH_WIDE) {
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.72F);
                graphics.blit(STAR_DISC, x + navWidth - 30, top + 5, 22, 22,
                        0.0F, 0.0F, 96, 96, 96, 96);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        drawFineDivider(graphics, x + 7, top + 29, x + navWidth - 8, COL_EDGE_SOFT);
        for (int i = 0; i < MODULE_PAGES.length; i++) {
            ModulePage entry = MODULE_PAGES[i];
            int rowY = navRowY(i);
            boolean hovered = mouseX >= x && mouseX < x + navWidth
                    && mouseY >= rowY && mouseY < rowY + 20;
            boolean selected = entry == this.page;
            if (selected) {
                graphics.fillGradient(x + 1, rowY - 2, x + navWidth - 3, rowY + 21,
                        0x705BE8D8, 0x18101824);
                graphics.fillGradient(x + 6, rowY + 2, x + navWidth - (compact ? 6 : 18), rowY + 18,
                        0x30E2C276, 0x00101824);
                graphics.fill(x + 2, rowY + 1, x + 4, rowY + 18, COL_ACCENT);
                graphics.fill(x + navWidth - 6, rowY + 4, x + navWidth - 4, rowY + 14, COL_GOLD);
                graphics.renderOutline(x + 1, rowY - 2, navWidth - 4, 23, COL_EDGE_SOFT);
            } else if (hovered || this.navFocus == i) {
                graphics.fillGradient(x + 2, rowY, x + navWidth - 3, rowY + 20,
                        0x305BE8D8, 0x0A101824);
                graphics.fill(x + 3, rowY + 3, x + 4, rowY + 17, COL_ACCENT_DIM);
            }
            if (selected || hovered) {
                graphics.fillGradient(x + 5, rowY + 1, x + 30, rowY + 19,
                        selected ? 0x355BE8D8 : 0x2038A59E, 0x00101824);
            }
            int iconX = compact ? x + (navWidth - 20) / 2 : x + 7;
            graphics.blit(ICONS, iconX, rowY, 20, 20,
                    i * (float) ICON_SIZE, 0.0F, ICON_SIZE, ICON_SIZE,
                    MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
            int color = selected ? COL_TEXT : COL_DIM;
            if (!compact) {
                graphics.drawString(this.font, entry.title(), x + 34, rowY + 6, color, false);
            }
            if (!compact && (selected || hovered)) {
                drawRuneTick(graphics, x + navWidth - 16, rowY + 7, selected ? COL_GOLD : COL_ACCENT_DIM);
            }
        }
    }

    private int navRowY(int index) {
        return panelTop() + 36 + index * 20;
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float focus) {
        if (this.page == ModulePage.OVERVIEW) {
            renderOverviewDisc(graphics, mouseX, mouseY);
            return;
        }
        drawGlassPanel(graphics, contentLeft() - 3, contentTop() - 3,
                contentRight() + 1, contentBottom() + 1, COL_ACCENT_DIM, false);
        graphics.fillGradient(contentLeft(), contentTop(), contentRight() - 3, contentBottom() - 2,
                0x1F294755, 0x1103050A);
        graphics.fillGradient(contentLeft(), contentTop(), contentRight() - 3, contentTop() + 18,
                0x2D5BE8D8, 0x00101824);
        drawSoftGrid(graphics, contentLeft() + 5, contentTop() + 22,
                contentRight() - 10, contentBottom() - 30, 0x0C38A59E);

        if (this.page == ModulePage.ABILITY) {
            renderAbilityRows(graphics, mouseX, mouseY);
            return;
        }
        if (this.page == ModulePage.ARTIFACT) {
            renderArtifactPage(graphics, mouseX, mouseY);
            return;
        }
        if (this.page == ModulePage.TRADE) {
            renderTradePage(graphics, mouseX, mouseY);
            return;
        }
        if (this.page == ModulePage.OPTIONS) {
            renderOptionsPage(graphics, mouseX, mouseY, focus);
            return;
        }
        int top = lineListTop();
        int rowHeight = LINE_ROW_HEIGHT;
        int visible = lineListVisibleRows();
        int max = lineScrollMax();
        this.scroll = Mth.clamp(this.scroll, 0, max);
        int railX = contentLeft() + 5;
        graphics.fill(railX, top - 2, railX + 1, contentBottom() - 26, 0x5538A59E);
        int y = top;
        for (int i = this.scroll; i < Math.min(this.lines.size(), this.scroll + visible); i++) {
            Line lineEntry = this.lines.get(i);
            boolean hovered = lineEntry.action != null
                    && mouseX >= contentLeft() && mouseX < contentRight()
                    && mouseY >= y && mouseY < y + rowHeight;
            if ((i - this.scroll) % 2 == 0) {
                graphics.fillGradient(contentLeft() + 8, y - 2, contentRight() - 6, y + rowHeight - 1,
                        0x30142330, 0x0803050A);
            }
            if (hovered) {
                graphics.fillGradient(contentLeft() + 7, y - 2, contentRight() - 6, y + rowHeight - 1,
                        COL_ACCENT_GLOW, 0x20101824);
                graphics.fill(contentLeft() + 7, y - 2, contentLeft() + 9, y + rowHeight - 1, COL_ACCENT);
                graphics.fill(contentRight() - 8, y + 1, contentRight() - 6, y + rowHeight - 4, COL_GOLD_DIM);
            }
            if (this.font.width(lineEntry.text) > 0) {
                int nodeColor = lineEntry.action != null ? COL_GOLD_DIM : COL_ACCENT_DIM;
                graphics.fill(railX - 2, y + 4, railX + 3, y + 7, 0xAA071018);
                graphics.fill(railX - 1, y + 5, railX + 2, y + 6,
                        hovered ? COL_ACCENT : nodeColor);
            }
            graphics.drawString(this.font, lineEntry.text, contentLeft() + 12, y + 1,
                    hovered ? COL_ACCENT : COL_TEXT, false);
            y += rowHeight;
        }
        if (max > 0) {

            int barH = Math.max(8, (contentBottom() - top) * visible / this.lines.size());
            int barY = top + (contentBottom() - 26 - top - barH) * this.scroll / max;
            int trackX = contentRight() - 5;
            int trackBottom = contentBottom() - 26;
            graphics.fill(trackX - 1, top, trackX + 3, trackBottom, 0x66101824);
            graphics.fill(trackX, top + 2, trackX + 2, trackBottom - 2, 0x8835505A);
            for (int tickY = top + 6; tickY < trackBottom - 4; tickY += 16) {
                graphics.fill(trackX - 1, tickY, trackX + 3, tickY + 1, 0x5538A59E);
            }
            graphics.fillGradient(trackX - 1, barY, trackX + 3, barY + barH,
                    COL_GOLD_DIM, COL_ACCENT_DIM);
            graphics.fill(trackX, barY + 1, trackX + 2, barY + Math.max(2, barH - 1), COL_ACCENT);
        }
    }

    private void renderOptionsPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font,
                fitComponent(Component.translatable("overview.unknown_echoes.option.saved"), contentWidth() - 12),
                contentLeft() + 4, contentTop() + 6, COL_DIM, false);
        drawFineDivider(graphics, contentLeft() + 4, contentTop() + 17,
                contentRight() - 8, COL_EDGE_SOFT);
        int top = optionsViewportTop();
        int bottom = optionsViewportBottom();
        graphics.enableScissor(contentLeft(), top, contentRight(), bottom);
        for (OptionSection section : this.optionSections) {
            graphics.drawString(this.font, fitComponent(section.title, optionControlWidth()),
                    contentLeft() + 4, section.y + 2, COL_GOLD, false);
        }
        for (ClientOptionSlider slider : this.optionSliders) {
            slider.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();
        int max = pageScrollMax();
        if (max > 0) {
            int trackHeight = Math.max(1, bottom - top);
            int barHeight = Math.min(trackHeight,
                    Math.max(8, trackHeight * trackHeight / this.optionsContentHeight));
            int barY = top + (trackHeight - barHeight) * this.pageScroll / max;
            int x = contentRight() - 5;
            graphics.fill(x - 1, top, x + 3, bottom, 0x66101824);
            graphics.fillGradient(x - 1, barY, x + 3, barY + barHeight, COL_GOLD_DIM, COL_ACCENT_DIM);
            graphics.drawString(this.font,
                    fitComponent(Component.translatable("overview.unknown_echoes.option.scroll"), contentWidth() - 12),
                    contentLeft() + 4, contentBottom() - 11, COL_DIM, false);
        }
    }

    private void renderOverviewDisc(GuiGraphics graphics, int mouseX, int mouseY) {
        OverviewLayout layout = overviewLayout();
        int cy = (contentTop() + contentBottom()) / 2;
        drawGlassPanel(graphics, contentLeft(), contentTop(), contentRight(), contentBottom(), COL_ACCENT_DIM, false);
        graphics.fillGradient(contentLeft() + 6, contentTop() + 6, contentRight() - 6, contentBottom() - 6,
                0x44142330, 0x1703060A);
        if (layout.showDisc()) {
            int discLeft = contentLeft() + 8;
            int discRight = layout.startX() - 8;
            int discSize = Math.min(184, Math.max(88,
                    Math.min(discRight - discLeft - 20, contentHeight() - 44)));
            int discX = discLeft + Math.max(0, (discRight - discLeft - discSize) / 2);
            int discY = cy - discSize / 2 + 8;
            graphics.fillGradient(discLeft, contentTop() + 12, discRight, contentBottom() - 12,
                    0x24294755, 0x0503060A);
            graphics.fillGradient(discLeft + 6, cy - discSize / 2 - 8, discRight, cy + discSize / 2 + 8,
                    0x3A5BE8D8, 0x00101824);
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.88F);
            graphics.blit(STAR_DISC, discX, discY, discSize, discSize,
                    0.0F, 0.0F, 96, 96, 96, 96);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.renderOutline(discX + 14, discY + 14, discSize - 28, discSize - 28, 0x3359D6C8);
            graphics.renderOutline(discX + 24, discY + 24, discSize - 48, discSize - 48, 0x448D7440);
            drawCornerMarks(graphics, discLeft, contentTop() + 12, discRight, contentBottom() - 12,
                    COL_EDGE_FAINT);
            graphics.drawString(this.font, SCREEN_TITLE,
                    discLeft + 10, contentTop() + 18, COL_GOLD, true);
            if (discRight - discLeft >= 142) {
                drawFineDivider(graphics, discLeft + 10, contentTop() + 35, discRight - 10, COL_ACCENT_DIM);
            }
        } else {
            graphics.setColor(0.48F, 0.76F, 0.78F, 0.12F);
            graphics.blit(STAR_DISC, contentRight() - 78, contentBottom() - 78, 68, 68,
                    0.0F, 0.0F, 96, 96, 96, 96);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int cardsBottom = layout.startY() + layout.rows() * layout.cardH()
                + (layout.rows() - 1) * layout.gap();
        graphics.fillGradient(layout.startX() - 6, layout.startY() - 6,
                layout.startX() + layout.cols() * layout.cardW() + (layout.cols() - 1) * layout.gap() + 6,
                cardsBottom + 6,
                0x2C142330, 0x0803050A);
        drawCornerMarks(graphics, layout.startX() - 6, layout.startY() - 6,
                layout.startX() + layout.cols() * layout.cardW() + (layout.cols() - 1) * layout.gap() + 6,
                cardsBottom + 6,
                COL_EDGE_FAINT);
        for (int i = 0; i < OVERVIEW_PAGES.length; i++) {
            ModulePage entry = OVERVIEW_PAGES[i];
            int x = layout.startX() + (i % layout.cols()) * (layout.cardW() + layout.gap());
            int y = layout.startY() + (i / layout.cols()) * (layout.cardH() + layout.gap());
            boolean hovered = mouseX >= x && mouseX < x + layout.cardW()
                    && mouseY >= y && mouseY < y + layout.cardH();
            if (layout.showDisc()) {
                int connectorY = y + layout.cardH() / 2;
                graphics.fillGradient(contentLeft() + layout.discWidth() - 12, connectorY,
                        x, connectorY + 1, hovered ? 0x2259D6C8 : 0x0E8D7440, 0x00101824);
            }
            graphics.fillGradient(x, y, x + layout.cardW(), y + layout.cardH(),
                    hovered ? COL_CARD_HOVER : COL_CARD, 0xAA071018);
            graphics.fillGradient(x + 3, y + 2, x + layout.cardW() - 4, y + 13,
                    hovered ? 0x465BE8D8 : 0x243A5C67, 0x00101824);
            graphics.fill(x, y, x + 2, y + layout.cardH(),
                    hovered ? COL_ACCENT : COL_ACCENT_DIM);
            graphics.fill(x + 5, y + layout.cardH() - 2,
                    x + layout.cardW() - 7, y + layout.cardH() - 1,
                    hovered ? COL_ACCENT_DIM : COL_EDGE_FAINT);
            graphics.renderOutline(x, y, layout.cardW(), layout.cardH(),
                    hovered ? COL_ACCENT : COL_EDGE);
            drawCornerMarks(graphics, x, y, x + layout.cardW(), y + layout.cardH(),
                    hovered ? COL_ACCENT : COL_EDGE_SOFT);
            int iconSize = layout.cardW() < 118 || layout.cardH() < 40 ? 20 : 28;
            int iconX = x + 7;
            int iconY = y + (layout.cardH() - iconSize) / 2;
            graphics.fillGradient(x + 5, iconY - 2, x + iconSize + 10, iconY + iconSize + 2,
                    hovered ? 0x355BE8D8 : 0x18101824, 0x00101824);
            graphics.blit(ICONS, iconX, iconY, iconSize, iconSize,
                    moduleIconU(entry), 0.0F, ICON_SIZE, ICON_SIZE,
                    MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
            int textX = x + iconSize + 14;
            boolean showIndex = layout.cardW() >= 150;
            int textRight = x + layout.cardW() - (showIndex ? 22 : 5);
            graphics.enableScissor(textX, y + 2, textRight, y + layout.cardH() - 2);
            graphics.drawString(this.font, entry.title(), textX, y + 6, COL_TEXT, false);
            Component status = this.overviewStatusCache[entry.ordinal()];
            if (status == null) {
                status = overviewStatus(entry);
            }
            graphics.drawString(this.font, status, textX, y + Math.min(20, layout.cardH() - 12),
                    COL_DIM, false);
            graphics.disableScissor();
            if (showIndex) {
                String index = MODULE_INDICES[i];
                graphics.drawString(this.font, index, x + layout.cardW() - 18, y + 5,
                        hovered ? COL_GOLD : COL_EDGE, false);
            }
            if (hovered) {
                graphics.fill(textX, y + layout.cardH() - 5,
                        x + layout.cardW() - 8, y + layout.cardH() - 4, COL_ACCENT_DIM);
            }
        }
    }

    private void renderResponsiveTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (compactLayout() && mouseX >= panelLeft() && mouseX < panelLeft() + navWidth()) {
            for (int i = 0; i < MODULE_PAGES.length; i++) {
                int rowY = navRowY(i);
                if (mouseY >= rowY && mouseY < rowY + 20) {
                    graphics.renderComponentTooltip(this.font,
                            List.of(MODULE_PAGES[i].title()), mouseX, mouseY);
                    return;
                }
            }
        }
        if (this.page == ModulePage.OVERVIEW) {
            OverviewLayout layout = overviewLayout();
            if (layout.cardW() < 150) {
                ModulePage hovered = overviewCardAt(mouseX, mouseY);
                if (hovered != null) {
                    graphics.renderComponentTooltip(this.font,
                            List.of(hovered.title(), overviewStatus(hovered)), mouseX, mouseY);
                }
            }
        }
    }

    private OverviewLayout overviewLayout() {
        int innerLeft = contentLeft() + 8;
        int innerRight = contentRight() - 8;
        boolean showDisc = contentWidth() >= 340;
        int discWidth = showDisc ? Math.min(166, Math.max(112, contentWidth() / 3)) : 0;
        int gap = 6;
        int cardsLeft = innerLeft + (showDisc ? discWidth + 8 : 0);
        int availableWidth = Math.max(96, innerRight - cardsLeft);
        int cols = availableWidth >= 202 ? 2 : 1;
        int cardWidth = Math.min(164, Math.max(88,
                (availableWidth - (cols - 1) * gap) / cols));
        int totalWidth = cols * cardWidth + (cols - 1) * gap;
        int startX = innerRight - totalWidth;
        int rows = (OVERVIEW_PAGES.length + cols - 1) / cols;
        int availableHeight = Math.max(120, contentHeight() - 20);
        int cardHeight = Mth.clamp((availableHeight - (rows - 1) * gap) / rows, 30, 48);
        int totalHeight = rows * cardHeight + (rows - 1) * gap;
        int startY = contentTop() + Math.max(8, (contentHeight() - totalHeight) / 2);
        return new OverviewLayout(showDisc, discWidth, cols, rows,
                cardWidth, cardHeight, gap, startX, startY);
    }

    private Component overviewStatus(ModulePage entry) {
        return switch (entry) {
            case ABILITY -> {
                int unlocked = 0;
                for (EchoAbilityType type : ABILITY_TYPES) {
                    if (ClientAbilityCache.hasAbility(type)) {
                        unlocked++;
                    }
                }
                yield Component.translatable("overview.unknown_echoes.status.ability",
                        unlocked, ABILITY_TYPES.length);
            }
            case JOURNAL -> Component.translatable("overview.unknown_echoes.status.journal",
                    ClientJournalCache.structures().size(), ClientJournalCache.totalStructures());
            case RESEARCH -> Component.translatable("overview.unknown_echoes.status.research",
                    ClientJournalCache.pages().size(), ClientJournalCache.knownPages());
            case ARTIFACT -> {
                int claimed = 0;
                for (var type : ARTIFACT_TYPES) {
                    if (cn.kurt6.unknown_echoes.client.ClientArtifactCache.isClaimed(type.getId())) {
                        claimed++;
                    }
                }
                yield Component.translatable("overview.unknown_echoes.status.artifact",
                        claimed, ARTIFACT_TYPES.length);
            }
            case TRADE -> Component.empty();
            case INDEX -> Component.translatable("overview.unknown_echoes.status.index",
                    visibleCompletionScore(), T7_REQUIRED_SCORE);
            case OPTIONS -> Component.empty();
            case OVERVIEW -> Component.empty();
        };
    }

    private void renderAbilityRows(GuiGraphics graphics, int mouseX, int mouseY) {
        EchoAbilityType[] types = ABILITY_TYPES;
        int x = contentLeft() + 2;
        int rowAreaTop = contentTop() + 8;
        int rowsBottom = contentBottom() - 46;
        int rowAreaHeight = Math.max(1, rowsBottom - rowAreaTop);
        int cardHeight = Mth.clamp((rowAreaHeight - (types.length - 1) * 4)
                / Math.max(1, types.length), 21, 38);
        int rowGap = types.length <= 1 ? 0 : Mth.clamp(
                (rowAreaHeight - types.length * cardHeight) / (types.length - 1), 3, 12);
        int rowStep = cardHeight + rowGap;
        int blockHeight = types.length * cardHeight + (types.length - 1) * rowGap;
        int rowY = rowAreaTop + Math.max(0, (rowAreaHeight - blockHeight) / 2);
        java.util.List<Component> tooltip = null;
        for (int i = 0; i < types.length; i++) {
            EchoAbilityType type = types[i];
            boolean unlocked = ClientAbilityCache.hasAbility(type);
            boolean hovered = mouseX >= x && mouseX <= contentRight() - 4
                    && mouseY >= rowY && mouseY < rowY + cardHeight;
            graphics.fillGradient(x - 2, rowY - 1, contentRight() - 4, rowY + cardHeight,
                    hovered ? COL_CARD_HOVER : COL_CARD,
                    unlocked ? 0x55101824 : 0x4405070B);
            graphics.fillGradient(x + 4, rowY + 2, contentRight() - 14,
                    rowY + Math.min(12, cardHeight - 2),
                    unlocked ? 0x2259D6C8 : 0x113D4852, 0x00101824);
            graphics.fill(x - 2, rowY - 1, x + 1, rowY + cardHeight,
                    unlocked ? COL_ACCENT : COL_SILHOUETTE);
            graphics.renderOutline(x - 2, rowY - 1, contentRight() - x - 2, cardHeight + 1,
                    hovered ? COL_ACCENT : COL_EDGE_SOFT);
            if (hovered) {
                drawCornerMarks(graphics, x - 2, rowY - 1,
                        contentRight() - 4, rowY + cardHeight, COL_ACCENT);
            }
            int iconSize = Math.min(28, Math.max(16, cardHeight - 5));
            int iconY = rowY + (cardHeight - iconSize) / 2;
            float shadeF = unlocked ? 1.0F : 0.35F;
            graphics.setColor(shadeF, shadeF, shadeF, 1.0F);
            graphics.blit(ABILITY_ICONS, x + 7, iconY, iconSize, iconSize,
                    i * (float) ICON_SIZE, 0.0F, ICON_SIZE, ICON_SIZE,
                    ABILITY_ICON_ATLAS_WIDTH, ICON_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            Component name = ABILITY_NAMES[type.ordinal()];
            int textX = x + iconSize + 15;
            int pipX = contentRight() - 70;
            graphics.enableScissor(textX, rowY, pipX - 4, rowY + cardHeight);
            graphics.drawString(this.font, name, textX, rowY + 3,
                    unlocked ? COL_TEXT : COL_SILHOUETTE, true);
            Component status = unlocked ? ABILITY_UNLOCKED : ABILITY_LOCKED;
            if (cardHeight >= 22) {
                graphics.drawString(this.font, status, textX, rowY + 13,
                        unlocked ? COL_ACCENT : COL_SILHOUETTE, false);
            }

            String failure = ClientAbilityCache.getLastFailure(type);
            if (cardHeight >= 32 && unlocked && !failure.isEmpty()) {
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.failure." + failure)
                                .withStyle(ChatFormatting.ITALIC),
                        textX, rowY + 23, COL_DIM, false);
            }
            graphics.disableScissor();

            if (unlocked) {
                int research = ClientAbilityCache.getResearchLevel(type);
                int pipSize = cardHeight < 28 ? 7 : 10;
                int pipY = rowY + (cardHeight - pipSize) / 2;
                for (int pip = 0; pip < 4; pip++) {
                    int px = pipX + pip * 14;
                    drawInstrumentPip(graphics, px, pipY, pipSize, pipSize,
                            pip < research, COL_ACCENT, COL_ACCENT_DIM);
                }
            }
            if (hovered) {
                Component hint = abilityHint(type)
                        .withStyle(unlocked ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);
                tooltip = unlocked
                        ? java.util.List.of(Component.translatable("overview.unknown_echoes.ability.tip.research",
                                ClientAbilityCache.getResearchLevel(type)).withStyle(ChatFormatting.AQUA), hint)
                        : java.util.List.of(hint);
            }
            rowY += rowStep;
        }
        if (tooltip != null) {
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        int energyNow = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getEnergy();
        int max = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getMaxEnergy();
        int barY = contentBottom() - 38;
        int barX = contentLeft() + 2;
        int barW = contentRight() - 10 - barX;
        graphics.drawString(this.font,
                ENERGY_POOL,
                barX, barY - 11, COL_GOLD_DIM, false);
        graphics.fillGradient(barX - 2, barY - 2, barX + barW + 2, barY + 10, 0x66101824, 0xAA03050A);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF0B1118);
        graphics.renderOutline(barX - 1, barY - 1, barW + 2, 10, COL_EDGE);
        if (energyNow >= 0 && max > 0) {
            int fill = (int) ((long) barW * energyNow / max);
            graphics.fillGradient(barX, barY, barX + fill, barY + 8, COL_ACCENT, COL_ACCENT_DIM);
            if (fill > 2) {
                graphics.fill(barX + 1, barY + 1, barX + fill - 1, barY + 2, 0x885CFFF0);
            }
            for (int segment = 1; segment < 12; segment++) {
                int segmentX = barX + segment * barW / 12;
                graphics.fill(segmentX, barY + 1, segmentX + 1, barY + 7, 0xAA071018);
            }
            if (fill > 4) {
                graphics.fill(barX + fill - 2, barY + 1, barX + fill, barY + 7, COL_GOLD);
            }
            Component value = Component.translatable(
                    "overview.unknown_echoes.energy.value", energyNow, max);
            graphics.drawString(this.font, value,
                    barX + barW - this.font.width(value) - 2, barY - 11, COL_ACCENT_DIM, false);
        }
    }

    private void renderArtifactPage(GuiGraphics graphics, int mouseX, int mouseY) {
        var types = ARTIFACT_TYPES;
        java.util.List<Component> artifactHoverTip = null;
        graphics.enableScissor(contentLeft(), contentTop(), contentRight(), contentBottom() - 16);
        for (int i = 0; i < types.length; i++) {
            var type = types[i];
            int rowY = artifactRowY(i);
            var entry = cn.kurt6.unknown_echoes.client.ClientArtifactCache.get(type.getId());
            boolean claimed = entry != null;
            int cardBottom = rowY + artifactCardHeight();
            boolean hovered = mouseX >= contentLeft() && mouseX <= contentRight() - 4
                    && mouseY >= Math.max(rowY - 2, contentTop())
                    && mouseY < Math.min(cardBottom, contentBottom() - 16);
            graphics.fillGradient(contentLeft(), rowY - 2, contentRight() - 4, cardBottom,
                    hovered ? COL_CARD_HOVER : COL_CARD, claimed ? 0x55101824 : 0x4405070B);
            graphics.fillGradient(contentLeft() + 4, rowY + 1, contentRight() - 10, rowY + 18,
                    hovered ? COL_ACCENT_HAZE : (claimed ? COL_GOLD_GLOW : 0x113D4852), 0x00101824);
            graphics.fill(contentLeft(), rowY - 2, contentLeft() + 3, cardBottom,
                    hovered ? COL_ACCENT : (claimed ? COL_GOLD : COL_SILHOUETTE));
            graphics.renderOutline(contentLeft(), rowY - 2,
                    contentRight() - contentLeft() - 4, cardBottom - rowY + 2,
                    hovered ? COL_ACCENT_DIM : COL_EDGE_SOFT);
            if (claimed || hovered) {
                drawCornerMarks(graphics, contentLeft(), rowY - 2, contentRight() - 4, cardBottom,
                        hovered ? COL_ACCENT : COL_GOLD_DIM);
            }

            drawItemSocket(graphics, contentLeft() + 5, rowY + 3, 22,
                    claimed ? COL_GOLD_DIM : COL_EDGE_SOFT, hovered);
            graphics.renderItem(artifactIcons.computeIfAbsent(type,
                    key -> new ItemStack(key.getCredentialItem())), contentLeft() + 8, rowY + 6);
            graphics.drawString(this.font, MODULE_INDICES[i], contentLeft() + 9, rowY + 30,
                    hovered ? COL_ACCENT_DIM : (claimed ? COL_GOLD_DIM : COL_SILHOUETTE), false);
            Component name = ARTIFACT_NAMES[type.ordinal()];
            graphics.drawString(this.font, name, contentLeft() + 32, rowY + 2,
                    claimed ? COL_TEXT : COL_SILHOUETTE, true);
            if (claimed) {

                int pipX = contentLeft() + 32;
                for (int pip = 0; pip < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL; pip++) {
                    int px = pipX + pip * 12;
                    drawInstrumentPip(graphics, px, rowY + 14, 9, 7,
                            pip < entry.level(), COL_GOLD, COL_GOLD_DIM);
                }
                Component tuning = entry.tuning().isEmpty()
                        ? ARTIFACT_UNTUNED
                        : Component.translatable("artifact.unknown_echoes." + type.getId()
                                + ".tuning." + entry.tuning());
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.artifact.tuning_value", tuning),
                        contentLeft() + 32, rowY + 24, COL_ACCENT_DIM, false);
                int cooldown = cn.kurt6.unknown_echoes.client.ClientArtifactCache
                        .getCooldownSeconds(type.getId());
                Component state = cooldown > 0
                        ? Component.translatable("overview.unknown_echoes.artifact.cooldown", cooldown)
                        : ARTIFACT_READY;
                graphics.drawString(this.font, state, contentLeft() + 32, rowY + 34,
                        cooldown > 0 ? COL_DIM : COL_ACCENT, false);
                Component energyCost = artifactEnergyCost(entry);
                graphics.drawString(this.font, energyCost,
                        contentRight() - 10 - this.font.width(energyCost), rowY + 34,
                        COL_GOLD_DIM, false);
            } else {
                graphics.drawString(this.font,
                        ARTIFACT_UNCLAIMED,
                        contentLeft() + 32, rowY + 16, COL_SILHOUETTE, false);
                graphics.drawString(this.font,
                        ARTIFACT_HINTS[type.ordinal()],
                        contentLeft() + 32, rowY + 28, COL_DIM, false);
            }
            if (hovered) {
                java.util.List<Component> tip = new ArrayList<>();
                tip.add(name.copy().withStyle(claimed ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                if (claimed) {
                    int lvl = entry.level();
                    tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.current", lvl)
                            .withStyle(ChatFormatting.GOLD));
                    addArtifactLevelTip(tip, type, lvl, ChatFormatting.AQUA);
                    tip.add(artifactEnergyCost(entry).copy().withStyle(ChatFormatting.DARK_AQUA));
                    if (!entry.tuning().isEmpty()) {
                        tip.add(Component.translatable("overview.unknown_echoes.artifact."
                                        + type.getId() + ".tuning." + entry.tuning())
                                .withStyle(ChatFormatting.GRAY));
                    }
                    if (lvl < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL) {
                        tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.next", lvl + 1)
                                .withStyle(ChatFormatting.DARK_GRAY));
                        addArtifactLevelTip(tip, type, lvl + 1, ChatFormatting.GRAY);
                        tip.add(Component.translatable("tooltip.unknown_echoes.artifact."
                                        + type.getId() + ".upgrade_" + (lvl + 1))
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                    addArtifactLimitsTip(tip, type);
                } else {
                    tip.add(ARTIFACT_HINTS[type.ordinal()].copy().withStyle(ChatFormatting.DARK_GRAY));
                }
                artifactHoverTip = tip;
            }
        }

        int credY = artifactRowY(types.length) + 4;
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.credentials.header"),
                contentLeft() + 4, credY, COL_GOLD, true);
        graphics.fill(contentLeft() + 4, credY + 10, contentRight() - 8, credY + 11, COL_EDGE_SOFT);
        credY += 15;
        for (int credIndex = 0; credIndex < CREDENTIAL_ABILITIES.length; credIndex++) {
            EchoAbilityType ability = CREDENTIAL_ABILITIES[credIndex];
            if (credY > contentBottom() - 16) {
                break;
            }
            boolean fulfilled = ClientAbilityCache.hasAbility(ability);
            boolean ritual = ClientAbilityCache.hasRitualRecord(ability);
            boolean holding = this.credentialHolding[credIndex];
            String stateKey = fulfilled ? "fulfilled" : holding ? "holding"
                    : ritual ? "reclaimable" : "dormant";
            boolean lit = fulfilled || ritual || holding;
            Component line = Component.literal(lit ? "✦ " : "◇ ")
                    .withStyle(lit ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY)
                    .append(Component.translatable("overview.unknown_echoes.credentials.entry",
                            CREDENTIAL_TOKEN_NAMES[ability.ordinal()],
                            Component.translatable("overview.unknown_echoes.credentials." + stateKey)
                                    .withStyle(lit ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)));
            graphics.drawString(this.font, line, contentLeft() + 8, credY,
                    lit ? COL_TEXT : COL_SILHOUETTE, false);
            credY += 12;
        }
        graphics.disableScissor();
        if (this.stationMode < 0) {
            graphics.drawString(this.font,
                    Component.translatable("overview.unknown_echoes.artifact.no_station")
                            .withStyle(ChatFormatting.ITALIC),
                    contentLeft() + 4, contentBottom() - 12, COL_DIM, false);
        }
        if (artifactHoverTip != null) {
            graphics.renderComponentTooltip(this.font, artifactHoverTip, mouseX, mouseY);
        }
    }

    /** 客户端展示用:背包(含副手)里是否带着该能力的共鸣信物。真伪以祭坛服务端校验为准。 */
    private static boolean clientHasToken(EchoAbilityType ability) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (player.getOffhandItem().getItem()
                instanceof cn.kurt6.unknown_echoes.item.ResonanceTokenItem token
                && token.getAbility() == ability) {
            return true;
        }
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof cn.kurt6.unknown_echoes.item.ResonanceTokenItem token
                    && token.getAbility() == ability) {
                return true;
            }
        }
        return false;
    }

    private void renderTradePage(GuiGraphics graphics, int mouseX, int mouseY) {
        var cache = cn.kurt6.unknown_echoes.client.ClientTradeCache.getEntries();
        if (!cn.kurt6.unknown_echoes.client.ClientTradeCache.hasSession()) {
            Component hint = Component.translatable("overview.unknown_echoes.trade.no_session");
            int cx = (contentLeft() + contentRight()) / 2;
            int y = (contentTop() + contentBottom()) / 2 - 10;
            drawGlassPanel(graphics, cx - 88, y - 18, cx + 88, y + 30, COL_GOLD_DIM, false);
            for (FormattedCharSequence seq : this.font.split(hint, contentWidth() - 40)) {
                graphics.drawString(this.font, seq, cx - this.font.width(seq) / 2, y, COL_DIM, false);
                y += 11;
            }
            return;
        }
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.trade.header"),
                contentLeft() + 4, contentTop() + 6, COL_GOLD, true);
        drawFineDivider(graphics, contentLeft() + 4, contentTop() + 17,
                contentRight() - 8, COL_EDGE_SOFT);
        ItemStack hoveredStack = ItemStack.EMPTY;
        graphics.enableScissor(contentLeft(), contentTop() + 16, contentRight(), contentBottom() - 14);
        for (int i = 0; i < cache.size(); i++) {
            var entry = cache.get(i);
            int rowY = tradeRowY(i);
            if (rowY + 22 <= contentTop() + 16) {
                continue;
            }
            if (rowY > contentBottom() - 14) {
                break;
            }
            boolean soldOut = entry.usesLeft() == 0;
            boolean hovered = !soldOut && mouseX >= contentLeft() && mouseX < contentRight() - 4
                    && mouseY >= rowY && mouseY < rowY + 22;
            graphics.fillGradient(contentLeft(), rowY, contentRight() - 4, rowY + 22,
                    hovered ? COL_CARD_HOVER : COL_CARD, 0x55101824);
            graphics.fill(contentLeft(), rowY, contentLeft() + 2, rowY + 22,
                    soldOut ? COL_SILHOUETTE : hovered ? COL_ACCENT : COL_GOLD_DIM);
            graphics.fillGradient(contentLeft() + 4, rowY + 2, contentRight() - 12, rowY + 9,
                    hovered ? 0x2259D6C8 : 0x118D7440, 0x00101824);
            graphics.renderOutline(contentLeft(), rowY, contentRight() - contentLeft() - 4, 22,
                    hovered ? COL_ACCENT : COL_EDGE_SOFT);
            if (soldOut) {
                for (int scanX = contentLeft() + 8; scanX < contentRight() - 10; scanX += 14) {
                    graphics.fill(scanX, rowY + 3, scanX + 1, rowY + 19, 0x223D4852);
                    graphics.fill(scanX + 1, rowY + 3, scanX + 5, rowY + 4, 0x163D4852);
                }
            } else if (hovered) {
                drawCornerMarks(graphics, contentLeft(), rowY, contentRight() - 4, rowY + 22, COL_ACCENT);
                graphics.fill(contentLeft() + 8, rowY + 19, contentRight() - 12, rowY + 20,
                        COL_ACCENT_DIM);
            }
            float shade = soldOut ? 0.4F : 1.0F;
            graphics.setColor(shade, shade, shade, 1.0F);
            int costsX = contentLeft() + 7;
            int itemY = rowY + 3;
            renderTradeCosts(graphics, entry.costs(), costsX, itemY,
                    soldOut ? COL_EDGE_FAINT : COL_GOLD_DIM);
            drawExchangeBridge(graphics, contentLeft() + 48, rowY + 11,
                    soldOut ? COL_SILHOUETTE : (hovered ? COL_ACCENT : COL_GOLD_DIM));
            int resultX = contentLeft() + 64;
            drawItemSocket(graphics, resultX - 1, itemY - 1, 18,
                    soldOut ? COL_EDGE_FAINT : COL_ACCENT_DIM, hovered);
            graphics.renderItem(entry.result(), resultX, itemY);
            graphics.renderItemDecorations(this.font, entry.result(), resultX, itemY);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Component uses = entry.usesLeft() < 0
                    ? Component.translatable("overview.unknown_echoes.trade.unlimited")
                    : Component.translatable("overview.unknown_echoes.trade.uses", entry.usesLeft());
            int usesX = contentRight() - this.font.width(uses) - 10;
            int labelX = contentLeft() + 87;
            int labelWidth = Math.max(0, usesX - labelX - 8);
            Component label = fitComponent(entry.result().getHoverName(), labelWidth);
            graphics.drawString(this.font, label, labelX, rowY + 7,
                    soldOut ? COL_SILHOUETTE : COL_TEXT, false);
            graphics.drawString(this.font, uses,
                    usesX, rowY + 7,
                    soldOut ? COL_SILHOUETTE : COL_DIM, false);
            if (mouseY >= contentTop() + 16 && mouseY < contentBottom() - 14) {
                int shownCosts = Math.min(2, entry.costs().size());
                for (int costIndex = 0; costIndex < shownCosts; costIndex++) {
                    int costX = costsX + costIndex * 22;
                    if (mouseX >= costX && mouseX < costX + 16
                            && mouseY >= itemY && mouseY < itemY + 16) {
                        hoveredStack = entry.costs().get(costIndex);
                    }
                }
                if (mouseX >= resultX && mouseX < resultX + 16
                        && mouseY >= itemY && mouseY < itemY + 16) {
                    hoveredStack = entry.result();
                }
            }
        }
        graphics.disableScissor();
        if (!hoveredStack.isEmpty()) {
            graphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private int tradeRowY(int index) {
        return contentTop() + 22 + index * 24 - this.pageScroll;
    }

    private int pageScrollMax() {
        if (this.page == ModulePage.OPTIONS) {
            return Math.max(0, this.optionsContentHeight
                    - Math.max(1, optionsViewportBottom() - optionsViewportTop()));
        }
        if (this.page == ModulePage.TRADE) {
            if (!cn.kurt6.unknown_echoes.client.ClientTradeCache.hasSession()) {
                return 0;
            }
            int count = cn.kurt6.unknown_echoes.client.ClientTradeCache.getEntries().size();
            return Math.max(0, 22 + count * 24 + 16 - contentHeight());
        }
        if (this.page == ModulePage.ARTIFACT) {
            int credentials = 4 + 15 + CREDENTIAL_ABILITIES.length * 12 + 18;
            return Math.max(0,
                    8 + ARTIFACT_TYPES.length * artifactCardStride() + credentials - contentHeight());
        }
        return 0;
    }

    private void renderTradeCosts(GuiGraphics graphics, List<net.minecraft.world.item.ItemStack> costs,
                                  int x, int y, int accent) {
        int shown = Math.min(2, costs.size());
        for (int i = 0; i < shown; i++) {
            int itemX = x + i * 22;
            drawItemSocket(graphics, itemX - 1, y - 1, 18, accent, false);
            graphics.renderItem(costs.get(i), itemX, y);
            graphics.renderItemDecorations(this.font, costs.get(i), itemX, y);
            if (i == 0 && shown > 1) {
                graphics.fill(x + 18, y + 7, x + 21, y + 8, accent);
                graphics.fill(x + 19, y + 6, x + 20, y + 9, accent);
            }
        }
        if (costs.size() > 2) {
            for (int tick = 0; tick < 3; tick++) {
                graphics.fill(x + 42 + tick * 2, y + 7, x + 43 + tick * 2, y + 8, COL_DIM);
            }
        }
    }

    private void renderStatusBar(GuiGraphics graphics) {
        int navWidth = navWidth();
        int availableWidth = panelRight() - panelLeft() - navWidth;
        int y = panelBottom() - STATUS_HEIGHT;
        graphics.fillGradient(panelLeft() + navWidth + 1, y, panelRight(), panelBottom(),
                0xDD142330, 0xF003050A);
        graphics.fill(panelLeft() + navWidth + 1, y, panelRight(), y + 1, COL_EDGE_SOFT);
        graphics.fill(panelLeft() + navWidth + 1, y + 1,
                panelLeft() + navWidth + 80, y + 2, COL_ACCENT_DIM);
        graphics.fillGradient(panelRight() - 126, y + 1, panelRight() - 5, panelBottom() - 2,
                0x255BE8D8, 0x0003050A);
        graphics.fillGradient(contentLeft() - 2, y + 2, contentLeft() + 160, panelBottom() - 2,
                0x16E2C276, 0x00101824);
        if (availableWidth >= 250) {
            graphics.drawString(this.font, this.statusRegion, contentLeft(), y + 3, COL_DIM, false);
        }
        if (availableWidth >= 430) {
            graphics.drawString(this.font, this.statusEnergy,
                    (contentLeft() + panelRight()) / 2 - this.font.width(this.statusEnergy) / 2,
                    y + 3, this.statusEnergyDormant ? COL_SILHOUETTE : COL_ACCENT_DIM, false);
        }
    }

    private void renderPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        int w = 150;
        int h = 180;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        graphics.fillGradient(x - 14, y - 13, x + w + 14, y + h + 15,
                0x55142330, 0xEE000000);
        graphics.fillGradient(x - 10, y - 9, x + w + 10, y + h + 11,
                0xAA101824, 0xDD03050A);
        graphics.renderOutline(x - 9, y - 8, w + 18, h + 16, COL_EDGE_FAINT);
        graphics.fill(x - 8, y - 6, x - 6, y + h + 6, COL_GOLD_DIM);
        drawCornerMarks(graphics, x - 8, y - 8, x + w + 8, y + h + 8, COL_GOLD_DIM);
        graphics.blit(PAPER, x, y, 0, 0, w, h, w, h);
        graphics.setColor(0.43F, 0.34F, 0.18F, 0.10F);
        graphics.blit(STAR_DISC, x + 34, y + 70, 82, 82,
                0.0F, 0.0F, 96, 96, 96, 96);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(x + 9, y + 26, x + 10, y + h - 14, 0x226B5018);
        graphics.fill(x + w - 10, y + 26, x + w - 9, y + h - 14, 0x226B5018);
        graphics.drawCenteredString(this.font, this.popup.title.copy().withStyle(ChatFormatting.BOLD),
                x + w / 2, y + 10, 0xFF6B5018);
        drawFineDivider(graphics, x + 15, y + 23, x + w - 15, 0x886B5018);
        drawRuneTick(graphics, x + w - 24, y + 18, 0xAA80642A);
        if (this.popupLinesFor != this.popup) {
            this.popupLinesFor = this.popup;
            this.popupLines = this.font.split(this.popup.text, w - 24);
        }
        int textY = y + 30;
        for (FormattedCharSequence seq : this.popupLines) {
            if (textY > y + h - 16) {
                break;
            }
            graphics.drawString(this.font, seq, x + 12, textY, 0xFF3A372E, false);
            textY += 10;
        }
        for (int mark = -1; mark <= 1; mark++) {
            graphics.fill(x + w / 2 + mark * 4, y + h - 10,
                    x + w / 2 + mark * 4 + 1, y + h - 9, 0x886B5018);
        }
        int closeWidth = this.font.width(POPUP_CLOSE) + 18;
        int closeX = x + (w - closeWidth) / 2;
        graphics.fillGradient(closeX, y + h + 2, closeX + closeWidth, y + h + 15,
                0xCC101824, 0xEE03050A);
        graphics.renderOutline(closeX, y + h + 2, closeWidth, 13, COL_EDGE_SOFT);
        graphics.fill(closeX + 4, y + h + 12, closeX + closeWidth - 4, y + h + 13, COL_GOLD_DIM);
        graphics.drawCenteredString(this.font, POPUP_CLOSE, x + w / 2, y + h + 5, COL_DIM);
    }

    private void renderActionButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean clip = this.page == ModulePage.ARTIFACT || this.page == ModulePage.OPTIONS;
        List<Component> tooltip = null;
        if (clip) {
            graphics.enableScissor(contentLeft(), actionViewportTop(), contentRight(), actionViewportBottom());
        }
        for (ActionButton button : this.actionButtons) {
            boolean enabled = button.enabled.getAsBoolean();
            boolean hovered = this.popup == null && enabled && button.contains(mouseX, mouseY)
                    && isActionButtonClickable(button, mouseY);
            int state = button.active || hovered ? 1 : 0;
            if (button.active) {
                state = 2;
            }
            graphics.setColor(enabled ? 1.0F : 0.45F, enabled ? 1.0F : 0.45F, enabled ? 1.0F : 0.45F, 1.0F);
            graphics.blit(BUTTONS, button.x, button.y, button.width, button.height,
                    0.0F, state * (float) BUTTON_SLICE_HEIGHT,
                    BUTTON_SLICE_WIDTH, BUTTON_SLICE_HEIGHT,
                    BUTTON_ATLAS_WIDTH, BUTTON_ATLAS_HEIGHT);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.fillGradient(button.x + 2, button.y + 2, button.x + button.width - 2, button.y + 8,
                    enabled ? 0x2238A59E : 0x113D4852, 0x00101824);
            graphics.renderOutline(button.x, button.y, button.width, button.height,
                    !enabled ? COL_EDGE_FAINT : (button.active ? COL_GOLD_DIM : (hovered ? COL_ACCENT_DIM : COL_EDGE_SOFT)));
            int guideColor = !enabled ? COL_EDGE_FAINT
                    : (button.active ? COL_GOLD : (hovered ? COL_ACCENT : COL_EDGE_SOFT));
            int guideY = button.y + button.height / 2;
            graphics.fill(button.x + 4, guideY - 2, button.x + 5, guideY + 2, guideColor);
            graphics.fill(button.x + button.width - 5, guideY - 2,
                    button.x + button.width - 4, guideY + 2, guideColor);
            graphics.fill(button.x + button.width / 2 - 5, button.y + 2,
                    button.x + button.width / 2 + 5, button.y + 3,
                    enabled ? (button.active ? COL_GOLD_DIM : COL_ACCENT_DIM) : COL_EDGE_FAINT);
            if (enabled && (button.active || hovered)) {
                drawCornerMarks(graphics, button.x, button.y, button.x + button.width, button.y + button.height,
                        button.active ? COL_GOLD : COL_ACCENT);
                graphics.fill(button.x + 6, button.y + button.height - 3,
                        button.x + button.width - 6, button.y + button.height - 2,
                        button.active ? COL_GOLD_DIM : COL_ACCENT_DIM);
            }
            int color = !enabled ? COL_SILHOUETTE : (button.active ? COL_GOLD : (hovered ? COL_ACCENT : COL_TEXT));
            Component optionValue = this.optionButtonValues.get(button);
            if (optionValue != null) {
                int valueWidth = this.font.width(optionValue);
                graphics.drawString(this.font, fitComponent(button.label, button.width - valueWidth - 28),
                        button.x + 9, button.y + (button.height - 8) / 2, color, true);
                graphics.drawString(this.font, optionValue, button.x + button.width - valueWidth - 9,
                        button.y + (button.height - 8) / 2, color, true);
            } else {
                Component buttonLabel = fitComponent(button.label, Math.max(0, button.width - 18));
                graphics.drawCenteredString(this.font, buttonLabel,
                        button.x + button.width / 2, button.y + (button.height - 8) / 2, color);
            }
            if (hovered && button.tooltip != null) {
                tooltip = List.of(button.label.copy().withStyle(ChatFormatting.GOLD),
                        button.tooltip.copy().withStyle(ChatFormatting.GRAY));
            }
        }
        if (clip) {
            graphics.disableScissor();
        }
        if (this.page == ModulePage.OPTIONS && this.popup == null) {
            for (ClientOptionSlider slider : this.optionSliders) {
                if (slider.isMouseOver(mouseX, mouseY)) {
                    tooltip = List.of(slider.getMessage().copy().withStyle(ChatFormatting.GOLD),
                            slider.hint.copy().withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (tooltip != null) {
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.popup != null) {
            this.popup = null;
            return true;
        }
        for (ActionButton actionButton : this.actionButtons) {
            if (actionButton.enabled.getAsBoolean() && actionButton.contains(mouseX, mouseY)
                    && isActionButtonClickable(actionButton, mouseY)) {
                actionButton.action.run();
                return true;
            }
        }

        for (int i = 0; i < MODULE_PAGES.length; i++) {
            int rowY = navRowY(i);
            if (mouseX >= panelLeft() && mouseX < panelLeft() + navWidth()
                    && mouseY >= rowY && mouseY < rowY + 20) {
                switchPage(MODULE_PAGES[i]);
                return true;
            }
        }

        if (this.page == ModulePage.OVERVIEW) {
            ModulePage clicked = overviewCardAt(mouseX, mouseY);
            if (clicked != null) {
                switchPage(clicked);
                return true;
            }
        }

        if (this.page == ModulePage.TRADE
                && cn.kurt6.unknown_echoes.client.ClientTradeCache.hasSession()) {
            var entries = cn.kurt6.unknown_echoes.client.ClientTradeCache.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                int rowY = tradeRowY(i);
                if (rowY + 22 <= contentTop() + 16) {
                    continue;
                }
                if (rowY > contentBottom() - 14) {
                    break;
                }
                if (mouseX >= contentLeft() && mouseX < contentRight() - 4
                        && mouseY >= Math.max(rowY, contentTop() + 16)
                        && mouseY < Math.min(rowY + 22, contentBottom() - 14)) {
                    cn.kurt6.unknown_echoes.client.ClientTradeCache.requestTrade(i);
                    return true;
                }
            }
        }

        if (this.page != ModulePage.ABILITY && this.page != ModulePage.OVERVIEW) {
            int top = lineListTop();
            int rowHeight = LINE_ROW_HEIGHT;
            int visible = lineListVisibleRows();
            int index = this.scroll + (int) ((mouseY - top) / rowHeight);
            if (mouseX >= contentLeft() && mouseX < contentRight()
                    && mouseY >= top && index >= 0 && index < this.lines.size()
                    && index < this.scroll + visible) {
                Line lineEntry = this.lines.get(index);
                if (lineEntry.action != null) {
                    lineEntry.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ModulePage overviewCardAt(double mouseX, double mouseY) {
        OverviewLayout layout = overviewLayout();
        for (int i = 0; i < OVERVIEW_PAGES.length; i++) {
            int x = layout.startX() + (i % layout.cols()) * (layout.cardW() + layout.gap());
            int y = layout.startY() + (i / layout.cols()) * (layout.cardH() + layout.gap());
            if (mouseX >= x && mouseX < x + layout.cardW()
                    && mouseY >= y && mouseY < y + layout.cardH()) {
                return OVERVIEW_PAGES[i];
            }
        }
        return null;
    }

    private int actionViewportTop() {
        return this.page == ModulePage.OPTIONS ? optionsViewportTop() : contentTop();
    }

    private int actionViewportBottom() {
        return this.page == ModulePage.OPTIONS ? optionsViewportBottom() : contentBottom() - 16;
    }

    /** 滚动页的按钮只接受裁剪区内的点击。 */
    private boolean isActionButtonClickable(ActionButton button, double mouseY) {
        if (this.page != ModulePage.ARTIFACT && this.page != ModulePage.OPTIONS) {
            return true;
        }
        return mouseY >= actionViewportTop() && mouseY < actionViewportBottom()
                && button.y + button.height > actionViewportTop() && button.y < actionViewportBottom();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.popup == null
                && (this.page == ModulePage.ARTIFACT || this.page == ModulePage.TRADE
                    || this.page == ModulePage.OPTIONS)) {
            if (this.page == ModulePage.OPTIONS
                    && (mouseX < contentLeft() || mouseX >= contentRight()
                        || mouseY < optionsViewportTop() || mouseY >= optionsViewportBottom())) {
                return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
            }
            int max = pageScrollMax();
            if (max > 0) {
                int target = Mth.clamp(this.pageScroll - (int) Math.signum(deltaY) * 26, 0, max);
                if (target != this.pageScroll) {
                    this.pageScroll = target;
                    rebuildKeepingScroll();
                }
                return true;
            }
        }
        if (this.popup == null && !this.lines.isEmpty()) {

            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(deltaY) * 3,
                    0, lineScrollMax());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        saveClientConfigIfChanged();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.popup != null) {
                this.popup = null;
                return true;
            }
            if (this.page != ModulePage.OVERVIEW) {
                switchPage(ModulePage.OVERVIEW);
                return true;
            }
            onClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            if (this.page == ModulePage.OPTIONS) {
                this.setFocused(null);
            }
            int delta = keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1;
            this.navFocus = Math.floorMod(
                    (this.navFocus < 0 ? this.page.ordinal() : this.navFocus) + delta,
                    MODULE_PAGES.length);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.navFocus >= 0) {
            switchPage(MODULE_PAGES[this.navFocus]);
            this.navFocus = -1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawGlassPanel(GuiGraphics graphics, int left, int top, int right, int bottom,
                                       int accent, boolean active) {
        graphics.fillGradient(left, top, right, bottom, COL_PANEL_DEEP, COL_VOID);
        graphics.fillGradient(left + 1, top + 1, right - 1, bottom - 1, COL_PANEL_WASH, 0x7A05080F);
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x553A5C67);
        graphics.fill(left + 2, bottom - 3, right - 2, bottom - 2, 0x55101824);
        graphics.renderOutline(left, top, right - left, bottom - top, active ? accent : COL_EDGE_SOFT);
        graphics.renderOutline(left + 2, top + 2, right - left - 4, bottom - top - 4, COL_EDGE_FAINT);
        drawCornerMarks(graphics, left, top, right, bottom, active ? accent : COL_EDGE_SOFT);
    }

    private static void drawItemSocket(GuiGraphics graphics, int x, int y, int size,
                                       int accent, boolean focused) {
        graphics.fillGradient(x, y, x + size, y + size,
                focused ? 0xAA18313C : 0x88101824, 0xCC03050A);
        graphics.renderOutline(x, y, size, size, accent);
        int center = x + size / 2;
        graphics.fill(center - 2, y, center + 2, y + 1, accent);
        graphics.fill(center - 2, y + size - 1, center + 2, y + size, accent);
        graphics.fill(x, y + size / 2 - 2, x + 1, y + size / 2 + 2, accent);
        graphics.fill(x + size - 1, y + size / 2 - 2,
                x + size, y + size / 2 + 2, accent);
    }

    private static void drawExchangeBridge(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 12, y + 1, color);
        graphics.fill(x + 8, y - 2, x + 9, y + 3, color);
        graphics.fill(x + 9, y - 1, x + 11, y + 2, color);
        graphics.fill(x + 11, y, x + 13, y + 1, color);
    }

    private static void drawCornerMarks(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        int len = 9;
        graphics.fill(left - 1, top - 1, left + len, top, color);
        graphics.fill(left - 1, top - 1, left, top + len, color);
        graphics.fill(right - len, top - 1, right + 1, top, color);
        graphics.fill(right, top - 1, right + 1, top + len, color);
        graphics.fill(left - 1, bottom, left + len, bottom + 1, color);
        graphics.fill(left - 1, bottom - len, left, bottom + 1, color);
        graphics.fill(right - len, bottom, right + 1, bottom + 1, color);
        graphics.fill(right, bottom - len, right + 1, bottom + 1, color);
    }

    private static void drawSoftGrid(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        for (int x = left; x < right; x += 18) {
            graphics.fill(x, top, x + 1, bottom, color);
        }
        for (int y = top; y < bottom; y += 18) {
            graphics.fill(left, y, right, y + 1, color);
        }
    }

    private static void drawFineDivider(GuiGraphics graphics, int left, int y, int right, int color) {
        graphics.fillGradient(left, y, right, y + 1, 0x00101824, color);
        graphics.fillGradient((left + right) / 2, y, right, y + 1, color, 0x00101824);
    }

    private static void drawInstrumentPip(GuiGraphics graphics, int x, int y, int width, int height,
                                          boolean lit, int bright, int dim) {
        int right = x + width;
        int bottom = y + height;
        int edge = lit ? dim : COL_EDGE;
        int fillTop = lit ? bright : 0xFF1C2630;
        int fillBottom = lit ? dim : 0xFF090D12;
        graphics.fillGradient(x + 1, y + 1, right - 1, bottom - 1, fillTop, fillBottom);
        graphics.fill(x + 2, y, right - 2, y + 1, edge);
        graphics.fill(x + 2, bottom - 1, right - 2, bottom, edge);
        graphics.fill(x, y + 2, x + 1, bottom - 2, edge);
        graphics.fill(right - 1, y + 2, right, bottom - 2, edge);
        if (lit && width >= 7 && height >= 6) {
            int highlight = 0x66000000 | (bright & 0x00FFFFFF);
            graphics.fill(x + 2, y + 2, right - 2, y + 3, highlight);
            graphics.fill(right - 3, y + 2, right - 2, bottom - 2, dim);
        }
    }

    private static void drawRuneTick(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 1, x + 2, y + 5, color);
        graphics.fill(x + 2, y, x + 6, y + 1, color);
        graphics.fill(x + 5, y + 1, x + 7, y + 5, color);
    }

    private Component fitComponent(Component component, int maxWidth) {
        if (maxWidth <= 0) {
            return Component.empty();
        }
        if (this.font.width(component) <= maxWidth) {
            return component;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return Component.empty();
        }
        String prefix = this.font.plainSubstrByWidth(component.getString(), maxWidth - ellipsisWidth);
        return Component.literal(prefix + ellipsis).withStyle(component.getStyle());
    }

    private void addWrapped(Component component, Runnable action) {
        int width = contentRight() - contentLeft() - 16;
        List<FormattedCharSequence> split = this.font.split(component, width);
        if (split.isEmpty()) {
            this.lines.add(new Line(FormattedCharSequence.EMPTY, action));
            return;
        }
        for (FormattedCharSequence seq : split) {
            this.lines.add(new Line(seq, action));
            action = null;
        }
    }


    private static Component count(String key, int have, int total) {
        return Component.translatable(key, have, total);
    }

    private static void addDiscoveredEntry(List<Component> content, boolean discovered, Component name) {
        if (discovered) {
            content.add(Component.literal("✦ ").withStyle(ChatFormatting.DARK_AQUA).append(name));
        }
    }

    /**
     * 未发现的世界奇观显示一句环境提示,发现后由 addDiscoveredEntry 显示正式条目。
     * 提示键取自 WorldWonderType.getClueKey(),与清单 JSON 的 clue_key 同源。
     */
    private static void addWonderHint(List<Component> content, String structurePath,
                                      boolean discovered) {
        if (discovered) {
            return;
        }
        WorldWonderType wonder = WorldWonderType.byId(structurePath);
        if (wonder == null) {
            return;
        }
        content.add(Component.literal("◇ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(wonder.getClueKey())
                        .withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static void addNoRecordsIfEmpty(List<Component> content) {
        if (content.isEmpty()) {
            content.add(Component.translatable("journal.unknown_echoes.book.no_records")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static String bar(int level) {
        return "●".repeat(level) + "○".repeat(Math.max(0, 4 - level));
    }

    private static float moduleIconU(ModulePage page) {
        int maxIndex = MODULE_ICON_ATLAS_WIDTH / ICON_SIZE - 1;
        return Math.min(page.ordinal(), maxIndex) * (float) ICON_SIZE;
    }

    private static int coreAbilityCount() {
        int count = 0;
        if (ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)) {
            count++;
        }
        if (ClientAbilityCache.hasAbility(EchoAbilityType.TIDE_ECHO)) {
            count++;
        }
        if (ClientAbilityCache.hasAbility(EchoAbilityType.TRUE_SIGHT_ECHO)) {
            count++;
        }
        return count;
    }

    private static int mainBossCount() {
        int count = 0;
        for (String boss : MAIN_BOSSES) {
            if (ClientJournalCache.bosses().contains(id(boss))) {
                count++;
            }
        }
        return count;
    }

    private static int deepAbilityCount() {
        int count = 0;
        for (EchoAbilityType type : DeepEchoesProgressCatalog.ABILITIES) {
            if (ClientAbilityCache.hasAbility(type)) {
                count++;
            }
        }
        return count;
    }

    private static int deepBossCount() {
        return DeepEchoesProgressCatalog.countKnown(ClientJournalCache.bosses(),
                DeepEchoesProgressCatalog.BOSS_IDS);
    }

    private static int miniBossCount() {
        int count = 0;
        for (String boss : MINI_BOSSES) {
            if (ClientJournalCache.bosses().contains(id(boss))) {
                count++;
            }
        }
        return count;
    }

    private static int artifactUnlocks() {
        int count = 0;
        for (var type : ARTIFACT_TYPES) {
            if (cn.kurt6.unknown_echoes.client.ClientArtifactCache.isClaimed(type.getId())) {
                count++;
            }
        }
        return count;
    }

    private static int artifactUpgradeSteps() {
        int count = 0;
        for (var type : ARTIFACT_TYPES) {
            var entry = cn.kurt6.unknown_echoes.client.ClientArtifactCache.get(type.getId());
            count += entry == null ? 0 : Math.max(0, entry.level() - 1);
        }
        return count;
    }

    private static int deepArtifactUpgradeSteps() {
        int count = 0;
        for (ArtifactType type : DeepEchoesProgressCatalog.ARTIFACTS) {
            var entry = ClientArtifactCache.get(type.getId());
            count += entry == null ? 0 : Math.max(0, entry.level() - 1);
        }
        return count;
    }

    private static int visibleCompletionScore() {
        int score = 0;
        score += Math.min(EchoRealmBiomeCatalog.countKnown(ClientJournalCache.biomes()) * 3, 30);
        score += Math.min(ClientJournalCache.structures().size() * 2, 40);
        score += ClientAbilityCache.getPuzzleScore();
        score += Math.min(miniBossCount() * 6, 48);
        score += Math.min(AncientPageCatalog.countFirstDimensionPages(ClientJournalCache.pages()),
                AncientPageCatalog.FIRST_DIMENSION_TOTAL);
        score += Math.min(visibleResearchStages() * 5, 45);
        score += Math.min(artifactUnlocks() * 4, 12);
        score += Math.min(artifactUpgradeSteps() * 3, 18);
        return score;
    }

    private static int visibleResearchStages() {
        int sum = 0;
        sum += Math.min(3, ClientAbilityCache.getResearchLevel(EchoAbilityType.WIND_ECHO));
        sum += Math.min(3, ClientAbilityCache.getResearchLevel(EchoAbilityType.TIDE_ECHO));
        sum += Math.min(3, ClientAbilityCache.getResearchLevel(EchoAbilityType.TRUE_SIGHT_ECHO));
        return sum;
    }

    private static Component[] abilityComponents(String prefix) {
        Component[] components = new Component[ABILITY_TYPES.length];
        for (EchoAbilityType type : ABILITY_TYPES) {
            components[type.ordinal()] = Component.translatable(prefix + type.getId());
        }
        return components;
    }

    private static Component[] artifactComponents(String prefix) {
        Component[] components = new Component[ARTIFACT_TYPES.length];
        for (ArtifactType type : ARTIFACT_TYPES) {
            components[type.ordinal()] = Component.translatable(prefix + type.getId());
        }
        return components;
    }

    private static String visibleBackfillTarget() {
        if (AncientPageCatalog.countReleasePages(ClientJournalCache.pages())
                < AncientPageCatalog.TOTAL) {
            return "pages";
        }
        if (miniBossCount() < MINI_BOSSES.length) {
            return "mini_bosses";
        }
        if (artifactUpgradeSteps() < ARTIFACT_TYPES.length
                * (cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL - 1)) {
            return "artifacts";
        }
        if (EchoRealmBiomeCatalog.countKnown(ClientJournalCache.biomes())
                < EchoRealmBiomeCatalog.REQUIRED_COUNT) {
            return "biomes";
        }
        return "ready";
    }

    private static String id(String path) {
        return UnknownEchoes.id(path).toString();
    }

    private record Line(FormattedCharSequence text, Runnable action) {
    }

    private record OptionSection(int y, Component title) {
    }

    private final class ClientOptionSlider extends AbstractSliderButton {
        private final String key;
        private final ModConfigSpec.DoubleValue configValue;
        private final Component hint;

        private ClientOptionSlider(int y, String key, ModConfigSpec.DoubleValue configValue) {
            super(contentLeft(), y, optionControlWidth(), 20, Component.empty(), configValue.get());
            this.key = key;
            this.configValue = configValue;
            this.hint = Component.translatable(key + ".hint");
            this.visible = y + this.height > optionsViewportTop() && y < optionsViewportBottom();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("overview.unknown_echoes.option.percent",
                    Component.translatable(this.key), Math.round(this.configValue.get() * 100)));
        }

        @Override
        protected void applyValue() {
            double percent = Math.round(this.value * 100) / 100.0D;
            if (Double.compare(this.configValue.get(), percent) != 0) {
                this.configValue.set(percent);
                clientConfigDirty = true;
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return popup == null && mouseY >= optionsViewportTop() && mouseY < optionsViewportBottom()
                    && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return isMouseOver(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (focused) {
                navFocus = -1;
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocused() && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
                int step = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1;
                this.value = Mth.clamp((Math.round(this.value * 100) + step) / 100.0D, 0.0D, 1.0D);
                applyValue();
                updateMessage();
                saveClientConfigIfChanged();
                return true;
            }
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled) {
                saveClientConfigIfChanged();
            }
            return handled;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            int accent = hovered || isFocused() ? COL_ACCENT : COL_EDGE_SOFT;
            graphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height,
                    COL_CARD, COL_PANEL_DEEP);
            graphics.renderOutline(getX(), getY(), this.width, this.height, accent);
            int handleX = getX() + (int) (this.value * (this.width - 8));
            graphics.fill(getX() + 2, getY() + this.height - 4,
                    handleX + 4, getY() + this.height - 2, COL_ACCENT_DIM);
            graphics.fillGradient(handleX + 1, getY() + 2, handleX + 7, getY() + this.height - 2,
                    COL_GOLD_DIM, COL_ACCENT_DIM);
            graphics.renderOutline(handleX + 1, getY() + 2, 6, this.height - 4, accent);
            Component percent = Component.translatable("overview.unknown_echoes.option.percent_value",
                    Math.round(this.configValue.get() * 100));
            int valueWidth = font.width(percent);
            int color = hovered || isFocused() ? COL_ACCENT : COL_TEXT;
            graphics.drawString(font, fitComponent(Component.translatable(this.key), this.width - valueWidth - 28),
                    getX() + 9, getY() + (this.height - 8) / 2, color, true);
            graphics.drawString(font, percent, getX() + this.width - valueWidth - 9,
                    getY() + (this.height - 8) / 2, color, true);
        }
    }

    private record ActionButton(int x, int y, int width, int height, Component label,
                                BooleanSupplier enabled, Runnable action, boolean active,
                                Component tooltip) {
        private ActionButton(int x, int y, int width, int height, Component label,
                             BooleanSupplier enabled, Runnable action, boolean active) {
            this(x, y, width, height, label, enabled, action, active, null);
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record OverviewLayout(boolean showDisc, int discWidth, int cols, int rows,
                                  int cardW, int cardH, int gap, int startX, int startY) {
    }

    private record ReadingPopup(Component title, Component text) {
    }
}
