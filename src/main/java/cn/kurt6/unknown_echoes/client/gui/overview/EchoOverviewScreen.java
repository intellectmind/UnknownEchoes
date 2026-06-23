package cn.kurt6.unknown_echoes.client.gui.overview;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.client.ClientAbilityCache;
import cn.kurt6.unknown_echoes.client.ClientJournalCache;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 回响总览(EchoOverviewScreen,5.8 系统模块 UI 统一入口壳):
 * 所有系统模块共用这一个界面——K/J/物品右键只是不同的初始页参数。
 * 布局(5.8.0):左侧"共鸣脊线"纵向导航 / 中心内容区(总览页为"残响星盘")/
 * 右侧"回声旁注"(选中页的进度与下一条线索)/ 底部"同步状态条"。
 * 已落地页:总览 / 能力 / 探索日志 / 残页与研究 / 神器 / 迷途交易 / 回声索引 / 选项。
 * 边界:本界面不承担任何解锁/授予逻辑;数据全部来自服务端同步缓存(红线 #6/#9);
 * 客户端开关只改 ClientConfig 表现项。ESC 逐层返回:弹层 → 页面 → 总览 → 关闭。
 */
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

    // ---- 5.8 色板:镜湖暗蓝 + 回响青线 + 氧化金记忆墨迹 ----
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

    /** 模块页:顺序即共鸣脊线导航顺序(5.8.0)。 */
    public enum ModulePage {
        OVERVIEW("overview", true),
        ABILITY("ability", true),
        JOURNAL("journal", true),
        RESEARCH("research", true),
        ARTIFACT("artifact", true),
        TRADE("trade", true),
        INDEX("index", true),
        OPTIONS("options", true);

        final String id;
        /** false = 暂未苏醒的模块入口。 */
        final boolean awake;

        ModulePage(String id, boolean awake) {
            this.id = id;
            this.awake = awake;
        }

        Component title() {
            return Component.translatable("overview.unknown_echoes.page." + this.id);
        }
    }

    /** 探索日志章节(原 JournalBookScreen 章节迁入,5.8"回响档案")。 */
    private enum JournalChapter {
        SUMMARY, BIOMES, STRUCTURES, MOBS, ABILITIES, PAGES, REVISIT
    }

    private static final String[] KNOWN_STRUCTURES = {
            "small_echo_ruin", "resonance_beacon_structure", "rune_puzzle_room",
            "memory_pillar_courtyard", "echo_temple", "sunken_temple", "mirror_temple",
            "reflection_vault", "silent_hut", "silent_ring",
            "sky_observatory", "broken_archive", "wind_eroded_tower",
            "tide_lighthouse_reef", "mirror_dust_cloister", "submerged_record_room"
    };
    private static final List<String> KNOWN_BIOMES = EchoRealmBiomeCatalog.PATHS;
    private static final String[] KNOWN_MOBS = {
            "echo_wanderer", "silent_walker", "crystal_feather_bird"
    };
    /** 守护者记录页:主线 Boss 在前,区域守护者(Mini Boss)在后。 */
    private static final String[] MAIN_BOSSES = {
            "forgotten_colossus", "abyss_watcher", "mirror_guardian"
    };
    private static final String[] MINI_BOSSES = {
            "storm_weaver", "tide_lantern_keeper", "mirror_dust_butler", "silent_priest",
            "crystal_songkeeper", "broken_bell_keeper", "dream_bloom_keeper", "lost_recorder_chief"
    };

    private static final int NAV_WIDTH = 118;
    private static final int NOTE_WIDTH = 108;
    private static final int STATUS_HEIGHT = 16;
    private static final int HEADER_HEIGHT = 24;
    private static final int BUTTON_ATLAS_WIDTH = 96;
    private static final int BUTTON_ATLAS_HEIGHT = 60;
    private static final int BUTTON_SLICE_WIDTH = 96;
    private static final int BUTTON_SLICE_HEIGHT = 20;
    private static final int T7_REQUIRED_BIOMES = 8;
    private static final int T7_REQUIRED_PAGES = 48;
    private static final int T7_REQUIRED_MINI_BOSSES = 6;
    private static final int T7_REQUIRED_SCORE = 150;
    private static final int INDEX_EXPANSION_ONE_PAGES = 60;
    private static final int INDEX_EXPANSION_TWO_PAGES = 72;
    private static final int INDEX_EXPANSION_TWO_SCORE = 160;
    /** 回响聚焦动效时长(tick,5.8 交互主张:短暂扫亮,不做花哨长动画)。 */
    private static final int FOCUS_TICKS = 8;

    private ModulePage page;
    private JournalChapter chapter = JournalChapter.SUMMARY;
    private int scroll = 0;
    private int focusTicks = 0;
    private int navFocus = -1;

    /** 内容区缓存行(滚动列表;每行可带点击动作)。 */
    private final List<Line> lines = new ArrayList<>();
    /** 自绘按钮/页签/开关交互区。 */
    private final List<ActionButton> actionButtons = new ArrayList<>();
    /** 阅读弹层:null = 未打开。 */
    private ReadingPopup popup = null;

    /** 残页右键直接打开阅读弹层(AncientPageItem 客户端分支调用)。 */
    public static void openAncientPage(int pageId) {
        EchoOverviewScreen screen = new EchoOverviewScreen(ModulePage.RESEARCH);
        screen.popup = new ReadingPopup(
                Component.translatable("page.unknown_echoes." + pageId + ".title"),
                Component.translatable("page.unknown_echoes." + pageId + ".text"));
        Minecraft.getInstance().setScreen(screen);
    }

    /** 神器台座上下文:仅在由记录台/升级台打开时非空,按钮操作回传该坐标供服务端校验。 */
    private int stationMode = -1;
    private net.minecraft.core.BlockPos stationPos = null;
    private final Map<String, Long> artifactActionCooldowns = new HashMap<>();

    /** 记录台/升级台交互入口(OpenArtifactStationPayload):打开总览"神器"页并携带台座上下文。 */
    public static void openArtifactStation(int mode, net.minecraft.core.BlockPos pos) {
        EchoOverviewScreen screen = new EchoOverviewScreen(ModulePage.ARTIFACT);
        screen.stationMode = mode;
        screen.stationPos = pos;
        Minecraft.getInstance().setScreen(screen);
    }

    /** 迷途旅者交易入口(TradeListPayload):打开总览"迷途交易"页(已打开则刷新)。 */
    public static void openTrades() {
        if (Minecraft.getInstance().screen instanceof EchoOverviewScreen open
                && open.page == ModulePage.TRADE) {
            open.rebuild();
            return;
        }
        Minecraft.getInstance().setScreen(new EchoOverviewScreen(ModulePage.TRADE));
    }

    public EchoOverviewScreen(ModulePage initialPage) {
        super(Component.translatable("overview.unknown_echoes.title"));
        this.page = initialPage;
    }

    // ---- 布局 ----

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

    private int contentLeft() {
        return panelLeft() + NAV_WIDTH + 6;
    }

    private int contentRight() {
        return this.page == ModulePage.OVERVIEW ? panelRight() - 4 : panelRight() - NOTE_WIDTH - 6;
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

    @Override
    protected void init() {
        this.clearWidgets();
        this.lines.clear();
        this.actionButtons.clear();
        this.scroll = 0;
        buildPage();
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
        this.clearWidgets();
        this.lines.clear();
        this.actionButtons.clear();
        this.scroll = 0;
        buildPage();
    }

    @Override
    public void tick() {
        if (this.focusTicks < FOCUS_TICKS) {
            this.focusTicks++;
        }
    }

    // ---- 页面构建 ----

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
        addWrapped(Component.empty(), null);
        addIndexLine("overview.unknown_echoes.index.abilities", coreAbilityCount(), 3, 3);
        addIndexLine("overview.unknown_echoes.index.main_bosses", mainBossCount(), MAIN_BOSSES.length,
                MAIN_BOSSES.length);
        addIndexLine("overview.unknown_echoes.index.biomes",
                EchoRealmBiomeCatalog.countKnown(ClientJournalCache.biomes()),
                EchoRealmBiomeCatalog.REQUIRED_COUNT, T7_REQUIRED_BIOMES);
        addIndexLine("overview.unknown_echoes.index.pages",
                ClientJournalCache.pages().size(), Math.max(ClientJournalCache.knownPages(), 80),
                T7_REQUIRED_PAGES);
        addIndexLine("overview.unknown_echoes.index.mini_bosses", miniBossCount(), MINI_BOSSES.length,
                T7_REQUIRED_MINI_BOSSES);
        addIndexLine("overview.unknown_echoes.index.score", score, T7_REQUIRED_SCORE, T7_REQUIRED_SCORE);
        addWrapped(Component.empty(), null);
        addWrapped(Component.translatable("overview.unknown_echoes.index.target",
                Component.translatable("overview.unknown_echoes.index.target." + visibleBackfillTarget()))
                .withStyle(ChatFormatting.AQUA), null);
        addWrapped(Component.empty(), null);
        addWrapped(Component.translatable("overview.unknown_echoes.index.deep_header")
                .withStyle(ChatFormatting.GOLD), null);
        addIndexLine("overview.unknown_echoes.index.deep_pages", ClientJournalCache.pages().size(),
                Math.max(ClientJournalCache.knownPages(), 80), INDEX_EXPANSION_TWO_PAGES);
        addIndexLine("overview.unknown_echoes.index.deep_score", score,
                INDEX_EXPANSION_TWO_SCORE, INDEX_EXPANSION_TWO_SCORE);
        addIndexLine("overview.unknown_echoes.index.deep_artifacts", artifactUpgradeSteps(),
                cn.kurt6.unknown_echoes.artifact.ArtifactType.values().length
                        * (cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL - 1),
                cn.kurt6.unknown_echoes.artifact.ArtifactType.values().length
                        * (cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL - 1));
    }

    private void addIndexLine(String key, int have, int total, int need) {
        boolean done = have >= need;
        addWrapped(Component.literal(done ? "✦ " : "◇ ")
                .withStyle(done ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY)
                .append(Component.translatable(key, have, total, need)
                        .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)), null);
    }

    /** 神器页(V0.6D,5.8):记录台=领取/复领;升级台=升级/调谐;台外只是誊录,按钮不可用。 */
    private void buildArtifactPage() {
        cn.kurt6.unknown_echoes.artifact.ArtifactType[] types =
                cn.kurt6.unknown_echoes.artifact.ArtifactType.values();
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
                int level = entry == null ? 0 : entry.level();
                addActionButton(new ActionButton(contentRight() - 96, rowY + 6, 88, 18,
                        Component.translatable("overview.unknown_echoes.artifact.upgrade"),
                        () -> claimed && level < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL,
                        () -> sendArtifactAction(
                                cn.kurt6.unknown_echoes.network.ArtifactActionPayload.ACTION_UPGRADE,
                                type, ""),
                        false));
                // 调谐词条(2 级解锁;研究 4 追加隐藏词条;选择只发请求,服务端校验)
                java.util.List<String> words = new ArrayList<>(type.getTuningWords());
                if (ClientAbilityCache.getResearchLevel(type.getLinkedAbility())
                        >= cn.kurt6.unknown_echoes.artifact.ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL) {
                    words.add(type.getHiddenWord());
                }
                String current = entry == null ? "" : entry.tuning();
                int wx = contentLeft() + 6;
                for (String word : words) {
                    Component label = Component.translatable(
                            "artifact.unknown_echoes." + type.getId() + ".tuning." + word);
                    int w = Math.max(64, this.font.width(label) + 18);
                    addActionButton(new ActionButton(wx, rowY + 50, w, 18, label,
                            () -> claimed && level >= 2,
                            () -> sendArtifactAction(
                                    cn.kurt6.unknown_echoes.network.ArtifactActionPayload.ACTION_TUNE,
                                    type, word),
                            word.equals(current)));
                    wx += w + 6;
                }
            }
        }
    }

    private int artifactRowY(int index) {
        return contentTop() + 8 + index * 82;
    }

    private void sendArtifactAction(int action, cn.kurt6.unknown_echoes.artifact.ArtifactType type,
                                    String word) {
        if (this.stationPos == null) {
            return;
        }
        String key = action + ":" + type.getId() + ":" + word;
        long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        long nextAllowed = this.artifactActionCooldowns.getOrDefault(key, Long.MIN_VALUE);
        if (now < nextAllowed) {
            return;
        }
        this.artifactActionCooldowns.put(key, now + 10L);
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new cn.kurt6.unknown_echoes.network.ArtifactActionPayload(
                        action, type.getId(), word, this.stationPos));
    }

    private void buildAbilityPage() {
        // 滑翔开关(自动触发开关,5.8 能力页;同一开关也出现在"选项"抽屉)
        addActionButton(new ActionButton(contentLeft(), contentBottom() - 24, 152, 20,
                toggleLabel("overview.unknown_echoes.option.glide", ClientConfig.WIND_THIRD_JUMP_GLIDE.get()),
                () -> ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)
                        && ClientAbilityCache.getResearchLevel(EchoAbilityType.WIND_ECHO) >= 1,
                () -> {
                    ClientConfig.WIND_THIRD_JUMP_GLIDE.set(!ClientConfig.WIND_THIRD_JUMP_GLIDE.get());
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
        // 章节标签:横向一排小按钮(5.8:模块切换横向滑移感)
        int x = contentLeft();
        for (JournalChapter c : JournalChapter.values()) {
            Component label = Component.translatable("overview.unknown_echoes.journal." + c.name().toLowerCase());
            int w = this.font.width(label) + 16;
            if (x + w > contentRight()) {
                break;
            }
            JournalChapter target = c;
            addActionButton(new ActionButton(x, contentTop(), w, 18, label,
                    () -> this.chapter != target,
                    () -> {
                        this.chapter = target;
                        rebuild();
                    },
                    this.chapter == c));
            x += w + 4;
        }
        buildJournalLines();
    }

    private void buildJournalLines() {
        List<Component> content = new ArrayList<>();
        switch (this.chapter) {
            case SUMMARY -> {
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
            case BIOMES -> {
                for (String biome : KNOWN_BIOMES) {
                    content.add(discoveredEntry(ClientJournalCache.biomes().contains(id(biome)),
                            Component.translatable("biome.unknown_echoes." + biome)));
                }
            }
            case STRUCTURES -> {
                for (String structure : KNOWN_STRUCTURES) {
                    content.add(discoveredEntry(ClientJournalCache.structures().contains(id(structure)),
                            Component.translatable("structure.unknown_echoes." + structure)));
                }
            }
            case MOBS -> {
                for (String mob : KNOWN_MOBS) {
                    content.add(discoveredEntry(ClientJournalCache.mobs().contains(id(mob)),
                            Component.translatable("entity.unknown_echoes." + mob)));
                }
            }
            case ABILITIES -> {
                for (EchoAbilityType type : EchoAbilityType.values()) {
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
                content.add(Component.translatable("journal.unknown_echoes.book.research_note")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            case PAGES -> {
                addPageCategories();
            }
            case REVISIT -> {
                boolean any = false;
                any |= addRevisit(content, EchoAbilityType.TIDE_ECHO, "sunken_temple",
                        "journal.unknown_echoes.revisit.tide_runes");
                any |= addRevisit(content, EchoAbilityType.TRUE_SIGHT_ECHO, "mirror_temple",
                        "journal.unknown_echoes.revisit.hidden_room");
                any |= addRevisit(content, EchoAbilityType.WIND_ECHO, "sky_observatory",
                        "journal.unknown_echoes.revisit.observatory");
                any |= addRevisitMechanism(content, "clue:tide_lighthouse",
                        "journal.unknown_echoes.revisit.tide_lighthouse");
                any |= addRevisitMechanism(content, "clue:mirror_hidden_room",
                        "journal.unknown_echoes.revisit.mirror_cloister");
                if (!any) {
                    content.add(Component.translatable("journal.unknown_echoes.book.no_revisit")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
        for (Component component : content) {
            addWrapped(component, null);
        }
    }

    private void buildResearchPage() {
        // 上半:研究进度概览;下半:残页集(点击打开阅读弹层)
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
                    + line.getId() + "." + (level + 1));
        }
        addWrapped(Component.literal("  ↳ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(hint.copy().withStyle(ChatFormatting.GRAY)), null);
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
        // 客户端表现项开关(5.8:配置抽屉只管客户端;服务端 ServerConfig 走文件/整合包)
        int y = contentTop() + 4;
        y = addToggle(y, "overview.unknown_echoes.option.glide", ClientConfig.WIND_THIRD_JUMP_GLIDE);
        y = addToggle(y, "overview.unknown_echoes.option.unlock_message",
                ClientConfig.SHOW_ABILITY_UNLOCK_MESSAGE);
        y = addToggle(y, "overview.unknown_echoes.option.particles", ClientConfig.ENABLE_ECHO_PARTICLES);
        addToggle(y, "overview.unknown_echoes.option.ambient", ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS);
    }

    private int addToggle(int y, String key, ModConfigSpec.BooleanValue value) {
        addActionButton(new ActionButton(contentLeft(), y, 190, 20,
                toggleLabel(key, value.get()),
                () -> true,
                () -> {
                    value.set(!value.get());
                    rebuild();
                },
                value.get()));
        return y + 24;
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(
                on ? "overview.unknown_echoes.option.on" : "overview.unknown_echoes.option.off"));
    }

    private void addActionButton(ActionButton button) {
        this.actionButtons.add(button);
    }

    // ---- 渲染 ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float focus = Mth.clamp((this.focusTicks + partialTick) / FOCUS_TICKS, 0.0F, 1.0F);
        renderBackdrop(graphics, focus);
        renderNav(graphics, mouseX, mouseY, focus);
        renderHeader(graphics, focus);
        renderContent(graphics, mouseX, mouseY, focus);
        if (this.page != ModulePage.OVERVIEW) {
            renderSideNote(graphics);
        }
        renderStatusBar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderActionButtons(graphics, mouseX, mouseY);
        if (this.popup != null) {
            renderPopup(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景在 render() 自绘(回响聚焦动效需要控制透明度),这里只压暗世界
    }

    private void renderBackdrop(GuiGraphics graphics, float focus) {
        int alphaTop = (int) ((COL_BACKDROP_TOP >>> 24) * focus) << 24 | (COL_BACKDROP_TOP & 0xFFFFFF);
        int alphaBottom = (int) ((COL_BACKDROP_BOTTOM >>> 24) * focus) << 24 | (COL_BACKDROP_BOTTOM & 0xFFFFFF);
        graphics.fillGradient(0, 0, this.width, this.height, alphaTop, alphaBottom);
        graphics.fillGradient(0, 0, this.width, this.height, 0x11000000, 0x88000000);

        // 主面板:外层暗玻璃 + 内层石质雾面,让背景纹理保留层次但不抢文字。
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
        drawSoftGrid(graphics, panelLeft() + NAV_WIDTH + 12, panelTop() + 48,
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
        graphics.fill(panelLeft() + NAV_WIDTH + 1, panelTop() + 2,
                panelLeft() + NAV_WIDTH + 2, panelBottom() - 2, 0x772D8B86);

        // 回响聚焦:顶部青线随动效扫亮
        int sweep = (int) ((panelRight() - panelLeft()) * focus);
        graphics.fill(panelLeft() + 3, panelTop() + 3, panelLeft() + 3 + sweep, panelTop() + 4, COL_ACCENT);
        graphics.fill(panelLeft() + 3, panelBottom() - 4,
                panelLeft() + 3 + sweep / 2, panelBottom() - 3, COL_GOLD_DIM);
        graphics.fillGradient(panelLeft() + 8, panelTop() + 6,
                panelLeft() + 8 + Math.max(0, sweep - 12), panelTop() + 8, 0x0062E6D8, COL_ACCENT_GLOW);
        for (int i = 0; i < 9; i++) {
            int px = panelLeft() + NAV_WIDTH + 24 + i * 37;
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
        graphics.fill(contentRight() - 48, y - 3, contentRight() - 46, y + 17, COL_ACCENT_DIM);
        graphics.fill(contentRight() - 38, y + 3, contentRight() - 22, y + 4, COL_EDGE_SOFT);
        graphics.fill(contentRight() - 34, y + 8, contentRight() - 16, y + 9, COL_GOLD_DIM);
        if (focus >= 1.0F || (this.focusTicks * 3) % 2 == 0) {
            graphics.drawString(this.font, this.page.title(), x, y, COL_GOLD, true);
        }
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.subtitle." + this.page.id)
                        .withStyle(ChatFormatting.ITALIC),
                x + this.font.width(this.page.title()) + 8, y, COL_DIM, false);
        drawFineDivider(graphics, x, y + 13, contentRight(), COL_ACCENT_DIM);
        graphics.fill(x, y + 14, x + Math.max(24, contentWidth() / 5), y + 15, COL_ACCENT);
        drawRuneTick(graphics, contentRight() - 20, y + 4, COL_GOLD_DIM);
    }

    private void renderNav(GuiGraphics graphics, int mouseX, int mouseY, float focus) {
        int x = panelLeft();
        int top = panelTop();
        graphics.fillGradient(x, top, x + NAV_WIDTH, panelBottom(), 0xE0142431, 0xF004070C);
        graphics.fillGradient(x + 3, top + 4, x + NAV_WIDTH - 4, panelBottom() - 4,
                0x44294755, 0x77000000);
        drawSoftGrid(graphics, x + 6, top + 34, x + NAV_WIDTH - 8, panelBottom() - 10, 0x1038A59E);
        graphics.fill(x + NAV_WIDTH, top, x + NAV_WIDTH + 1, panelBottom(), COL_EDGE);
        graphics.fill(x + NAV_WIDTH - 2, top + 6, x + NAV_WIDTH - 1, panelBottom() - 8, COL_ACCENT_HAZE);
        graphics.fillGradient(x + 6, top + 5, x + NAV_WIDTH - 8, top + 28, 0x2538A59E, 0x0003050A);
        graphics.drawString(this.font, Component.literal("UNKNOWN"),
                x + 7, top + 6, COL_GOLD_DIM, false);
        graphics.drawString(this.font, Component.literal("ECHOES"),
                x + 18, top + 16, COL_ACCENT_DIM, false);
        drawFineDivider(graphics, x + 7, top + 29, x + NAV_WIDTH - 8, COL_EDGE_SOFT);
        ModulePage[] pages = ModulePage.values();
        for (int i = 0; i < pages.length; i++) {
            ModulePage entry = pages[i];
            int rowY = navRowY(i);
            boolean hovered = mouseX >= x && mouseX < x + NAV_WIDTH
                    && mouseY >= rowY && mouseY < rowY + 20;
            boolean selected = entry == this.page;
            if (selected) {
                graphics.fillGradient(x + 1, rowY - 2, x + NAV_WIDTH - 3, rowY + 21,
                        0x705BE8D8, 0x18101824);
                graphics.fillGradient(x + 6, rowY + 2, x + NAV_WIDTH - 18, rowY + 18,
                        0x30E2C276, 0x00101824);
                graphics.fill(x + 2, rowY + 1, x + 4, rowY + 18, COL_ACCENT);
                graphics.fill(x + NAV_WIDTH - 6, rowY + 4, x + NAV_WIDTH - 4, rowY + 14, COL_GOLD);
                graphics.renderOutline(x + 1, rowY - 2, NAV_WIDTH - 4, 23, COL_EDGE_SOFT);
            } else if (hovered || this.navFocus == i) {
                graphics.fillGradient(x + 2, rowY, x + NAV_WIDTH - 3, rowY + 20,
                        0x305BE8D8, 0x0A101824);
                graphics.fill(x + 3, rowY + 3, x + 4, rowY + 17, COL_ACCENT_DIM);
            }
            // 图标:未苏醒模块压暗成剪影
            float shadeF = entry.awake ? 1.0F : 0.35F;
            if (selected || hovered) {
                graphics.fillGradient(x + 5, rowY + 1, x + 30, rowY + 19,
                        selected ? 0x355BE8D8 : 0x2038A59E, 0x00101824);
            }
            graphics.setColor(shadeF, shadeF, shadeF, 1.0F);
            graphics.blit(ICONS, x + 7, rowY, 20, 20,
                    i * (float) ICON_SIZE, 0.0F, ICON_SIZE, ICON_SIZE,
                    MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int color = selected ? COL_TEXT : (entry.awake ? COL_DIM : COL_SILHOUETTE);
            graphics.drawString(this.font, entry.title(), x + 34, rowY + 6, color, false);
            if (selected || hovered) {
                drawRuneTick(graphics, x + NAV_WIDTH - 16, rowY + 7, selected ? COL_GOLD : COL_ACCENT_DIM);
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
        if (!this.page.awake) {
            renderSilhouettePage(graphics);
            return;
        }
        // 滚动文本行(日志/研究/守护者页;能力页为自绘行)
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
        int top = this.page == ModulePage.JOURNAL ? contentTop() + 24 : contentTop() + 8;
        int rowHeight = 14;
        int visible = (contentBottom() - 26 - top) / rowHeight;
        int max = Math.max(0, this.lines.size() - visible);
        this.scroll = Mth.clamp(this.scroll, 0, max);
        int y = top;
        for (int i = this.scroll; i < Math.min(this.lines.size(), this.scroll + visible); i++) {
            Line lineEntry = this.lines.get(i);
            boolean hovered = lineEntry.action != null
                    && mouseX >= contentLeft() && mouseX < contentRight()
                    && mouseY >= y && mouseY < y + rowHeight;
            if ((i - this.scroll) % 2 == 0) {
                graphics.fillGradient(contentLeft(), y - 2, contentRight() - 5, y + rowHeight - 1,
                        0x30142330, 0x0803050A);
            }
            if (hovered) {
                graphics.fillGradient(contentLeft(), y - 2, contentRight() - 5, y + rowHeight - 1,
                        COL_ACCENT_GLOW, 0x20101824);
                graphics.fill(contentLeft(), y - 2, contentLeft() + 2, y + rowHeight - 1, COL_ACCENT);
                graphics.fill(contentRight() - 8, y + 1, contentRight() - 6, y + rowHeight - 4, COL_GOLD_DIM);
            }
            graphics.drawString(this.font, lineEntry.text, contentLeft() + 6, y + 1,
                    hovered ? COL_ACCENT : COL_TEXT, false);
            y += rowHeight;
        }
        if (max > 0) {
            // 滚动指示
            int barH = Math.max(8, (contentBottom() - top) * visible / this.lines.size());
            int barY = top + (contentBottom() - 26 - top - barH) * this.scroll / max;
            graphics.fill(contentRight() - 4, top, contentRight() - 1,
                    contentBottom() - 26, 0x66101824);
            graphics.fill(contentRight() - 3, barY, contentRight() - 1, barY + barH, COL_ACCENT_DIM);
        }
    }

    /** 总览页:残响星盘——可用模块大入口 + 未解锁剪影(5.8.0)。 */
    private void renderOverviewDisc(GuiGraphics graphics, int mouseX, int mouseY) {
        int cx = (contentLeft() + contentRight()) / 2;
        int cy = (contentTop() + contentBottom()) / 2;
        drawGlassPanel(graphics, contentLeft(), contentTop(), contentRight(), contentBottom(), COL_ACCENT_DIM, false);
        graphics.fillGradient(contentLeft() + 6, contentTop() + 6, contentRight() - 6, contentBottom() - 6,
                0x44142330, 0x1703060A);
        graphics.fillGradient(contentLeft() + 10, contentTop() + 12, contentLeft() + 166, contentBottom() - 12,
                0x24294755, 0x0503060A);
        graphics.fillGradient(contentLeft() + 16, cy - 84, contentLeft() + 168, cy + 84,
                0x3A5BE8D8, 0x00101824);
        graphics.fillGradient(contentLeft() + 32, cy - 54, contentLeft() + 138, cy + 58,
                0x18E2C276, 0x00101824);
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.88F);
        graphics.blit(STAR_DISC, contentLeft() + 14, cy - 66, 132, 132,
                0.0F, 0.0F, 96, 96, 96, 96);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.renderOutline(contentLeft() + 28, cy - 52, 104, 104, 0x3359D6C8);
        graphics.renderOutline(contentLeft() + 40, cy - 40, 80, 80, 0x448D7440);
        graphics.renderOutline(contentLeft() + 52, cy - 28, 56, 56, 0x5538A59E);
        drawCornerMarks(graphics, contentLeft() + 12, contentTop() + 12,
                contentLeft() + 166, contentBottom() - 12, COL_EDGE_FAINT);
        graphics.drawString(this.font, Component.translatable("overview.unknown_echoes.title"),
                contentLeft() + 28, contentTop() + 18, COL_GOLD, true);
        graphics.drawString(this.font, Component.translatable("overview.unknown_echoes.subtitle.overview"),
                contentLeft() + 28, contentTop() + 31, COL_DIM, false);
        drawFineDivider(graphics, contentLeft() + 30, contentTop() + 44, contentLeft() + 150, COL_ACCENT_DIM);
        drawRuneTick(graphics, contentLeft() + 137, contentTop() + 31, COL_GOLD_DIM);

        ModulePage[] entries = {ModulePage.ABILITY, ModulePage.JOURNAL, ModulePage.RESEARCH,
                ModulePage.ARTIFACT, ModulePage.TRADE, ModulePage.INDEX, ModulePage.OPTIONS};
        int cols = 2;
        int cardW = Math.min(138, Math.max(100, (contentWidth() - 190) / 2));
        int cardH = 42;
        int gap = 8;
        int startX = Math.max(contentLeft() + 174, contentRight() - (cols * cardW + gap) - 12);
        int startY = contentTop() + 18;
        graphics.fillGradient(startX - 8, startY - 8,
                startX + cols * cardW + gap + 8, Math.min(contentBottom() - 10, startY + 4 * (cardH + gap) + 4),
                0x2C142330, 0x0803050A);
        drawCornerMarks(graphics, startX - 8, startY - 8,
                startX + cols * cardW + gap + 8, Math.min(contentBottom() - 10, startY + 4 * (cardH + gap) + 4),
                COL_EDGE_FAINT);
        for (int i = 0; i < entries.length; i++) {
            ModulePage entry = entries[i];
            int x = startX + (i % cols) * (cardW + gap);
            int y = startY + (i / cols) * (cardH + gap);
            boolean hovered = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
            int connectorY = y + cardH / 2;
            graphics.fillGradient(contentLeft() + 128, connectorY, x, connectorY + 1,
                    hovered ? 0x2259D6C8 : 0x0E8D7440, 0x00101824);
            graphics.fillGradient(x, y, x + cardW, y + cardH,
                    hovered ? COL_CARD_HOVER : COL_CARD,
                    entry.awake ? 0xAA071018 : 0xAA05070B);
            graphics.fillGradient(x + 3, y + 2, x + cardW - 4, y + 14,
                    hovered ? 0x465BE8D8 : 0x243A5C67, 0x00101824);
            graphics.fill(x, y, x + 2, y + cardH,
                    hovered ? COL_ACCENT : (entry.awake ? COL_ACCENT_DIM : 0xFF18222A));
            graphics.fill(x + 5, y + cardH - 2, x + cardW - 7, y + cardH - 1,
                    hovered ? COL_ACCENT_DIM : COL_EDGE_FAINT);
            graphics.renderOutline(x, y, cardW, cardH,
                    hovered ? COL_ACCENT : (entry.awake ? COL_EDGE : 0xFF161E26));
            drawCornerMarks(graphics, x, y, x + cardW, y + cardH,
                    hovered ? COL_ACCENT : (entry.awake ? COL_EDGE_SOFT : 0x55161E26));
            float shadeF = entry.awake ? 1.0F : 0.3F;
            graphics.fillGradient(x + 6, y + 5, x + 36, y + 35,
                    hovered ? 0x355BE8D8 : 0x18101824, 0x00101824);
            graphics.setColor(shadeF, shadeF, shadeF, 1.0F);
            graphics.blit(ICONS, x + 8, y + 7, 28, 28,
                    moduleIconU(entry), 0.0F, ICON_SIZE, ICON_SIZE,
                    MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.drawString(this.font, entry.title(), x + 42, y + 7,
                    entry.awake ? COL_TEXT : COL_SILHOUETTE, false);
            Component status = overviewStatus(entry);
            graphics.drawString(this.font, status, x + 42, y + 21,
                    entry.awake ? COL_DIM : COL_SILHOUETTE, false);
            String index = String.format("%02d", i + 1);
            graphics.drawString(this.font, index, x + cardW - 18, y + 5,
                    hovered ? COL_GOLD : COL_EDGE, false);
            if (hovered) {
                graphics.fill(x + 42, y + cardH - 5, x + cardW - 18, y + cardH - 4, COL_ACCENT_DIM);
            }
        }
    }

    /** 总览卡片的一行状态(右侧旁注的精简版)。 */
    private Component overviewStatus(ModulePage entry) {
        return switch (entry) {
            case ABILITY -> {
                int unlocked = 0;
                for (EchoAbilityType type : EchoAbilityType.values()) {
                    if (ClientAbilityCache.hasAbility(type)) {
                        unlocked++;
                    }
                }
                yield Component.translatable("overview.unknown_echoes.status.ability",
                        unlocked, EchoAbilityType.values().length);
            }
            case JOURNAL -> Component.translatable("overview.unknown_echoes.status.journal",
                    ClientJournalCache.structures().size(), ClientJournalCache.totalStructures());
            case RESEARCH -> Component.translatable("overview.unknown_echoes.status.research",
                    ClientJournalCache.pages().size(), ClientJournalCache.knownPages());
            case ARTIFACT -> {
                int claimed = 0;
                for (var type : cn.kurt6.unknown_echoes.artifact.ArtifactType.values()) {
                    if (cn.kurt6.unknown_echoes.client.ClientArtifactCache.isClaimed(type.getId())) {
                        claimed++;
                    }
                }
                yield Component.translatable("overview.unknown_echoes.status.artifact",
                        claimed, cn.kurt6.unknown_echoes.artifact.ArtifactType.values().length);
            }
            case TRADE -> Component.translatable(
                    cn.kurt6.unknown_echoes.client.ClientTradeCache.hasSession()
                            ? "overview.unknown_echoes.status.trade_open"
                            : "overview.unknown_echoes.status.trade_idle");
            case INDEX -> Component.translatable("overview.unknown_echoes.status.index",
                    visibleCompletionScore(), T7_REQUIRED_SCORE);
            case OPTIONS -> Component.translatable("overview.unknown_echoes.status.options");
            default -> Component.translatable("overview.unknown_echoes.status.dormant");
        };
    }

    /** 剪影占位页:V0.6D/E 落地的模块先给含蓄文案,不开独立界面(5.8)。 */
    private void renderSilhouettePage(GuiGraphics graphics) {
        int cx = (contentLeft() + contentRight()) / 2;
        int cy = (contentTop() + contentBottom()) / 2;
        graphics.setColor(0.25F, 0.28F, 0.32F, 1.0F);
        graphics.blit(ICONS, cx - 24, cy - 40, 48, 48,
                moduleIconU(this.page), 0.0F, ICON_SIZE, ICON_SIZE,
                MODULE_ICON_ATLAS_WIDTH, ICON_SIZE);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        Component hint = Component.translatable("overview.unknown_echoes.dormant." + this.page.id);
        List<FormattedCharSequence> split = this.font.split(hint, contentRight() - contentLeft() - 40);
        int y = cy - split.size() * 5;
        for (FormattedCharSequence seq : split) {
            int w = this.font.width(seq);
            graphics.drawString(this.font, seq, cx - w / 2, y, COL_DIM, false);
            y += 11;
        }
    }

    /** 能力页:图标行 + 解锁状态 + 研究刻度 + 悬停提示(原 AbilityPanelScreen 迁入)。 */
    private void renderAbilityRows(GuiGraphics graphics, int mouseX, int mouseY) {
        EchoAbilityType[] types = EchoAbilityType.values();
        int x = contentLeft() + 2;
        int rowY = contentTop() + 8;
        java.util.List<Component> tooltip = null;
        for (int i = 0; i < types.length; i++) {
            EchoAbilityType type = types[i];
            boolean unlocked = ClientAbilityCache.hasAbility(type);
            boolean hovered = mouseX >= x && mouseX <= contentRight() - 4
                    && mouseY >= rowY && mouseY < rowY + 34;
            graphics.fillGradient(x - 2, rowY - 1, contentRight() - 4, rowY + 33,
                    hovered ? COL_CARD_HOVER : COL_CARD,
                    unlocked ? 0x55101824 : 0x4405070B);
            graphics.fillGradient(x + 4, rowY + 2, contentRight() - 14, rowY + 12,
                    unlocked ? 0x2259D6C8 : 0x113D4852, 0x00101824);
            graphics.fill(x - 2, rowY - 1, x + 1, rowY + 33,
                    unlocked ? COL_ACCENT : COL_SILHOUETTE);
            graphics.renderOutline(x - 2, rowY - 1, contentRight() - x - 2, 34,
                    hovered ? COL_ACCENT : COL_EDGE_SOFT);
            if (hovered) {
                drawCornerMarks(graphics, x - 2, rowY - 1, contentRight() - 4, rowY + 33, COL_ACCENT);
            }
            float shadeF = unlocked ? 1.0F : 0.35F;
            graphics.setColor(shadeF, shadeF, shadeF, 1.0F);
            graphics.blit(ABILITY_ICONS, x + 7, rowY + 3, 28, 28,
                    i * (float) ICON_SIZE, 0.0F, ICON_SIZE, ICON_SIZE,
                    ABILITY_ICON_ATLAS_WIDTH, ICON_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            Component name = Component.translatable("ability.unknown_echoes." + type.getId());
            graphics.drawString(this.font, name, x + 44, rowY + 4,
                    unlocked ? COL_TEXT : COL_SILHOUETTE, true);
            Component status = Component.translatable(unlocked
                    ? "screen.unknown_echoes.ability_panel.unlocked"
                    : "screen.unknown_echoes.ability_panel.locked");
            graphics.drawString(this.font, status, x + 44, rowY + 14,
                    unlocked ? COL_ACCENT : COL_SILHOUETTE, false);
            // 最近失败提示(V0.6E,5.7:含蓄文案,服务端同步;无记录则不占行)
            String failure = ClientAbilityCache.getLastFailure(type);
            if (unlocked && !failure.isEmpty()) {
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.failure." + failure)
                                .withStyle(ChatFormatting.ITALIC),
                        x + 44, rowY + 24, COL_DIM, false);
            }

            int pipX = contentRight() - 70;
            if (unlocked) {
                int research = ClientAbilityCache.getResearchLevel(type);
                for (int pip = 0; pip < 4; pip++) {
                    int color = pip < research ? COL_ACCENT : 0xFF1C2630;
                    int px = pipX + pip * 14;
                    graphics.fillGradient(px, rowY + 10, px + 10, rowY + 20,
                            color, pip < research ? COL_ACCENT_DIM : 0xFF0B1118);
                    graphics.renderOutline(px, rowY + 10, 10, 10, pip < research ? COL_ACCENT_DIM : COL_EDGE);
                }
            }
            if (hovered) {
                Component hint = Component.translatable("screen.unknown_echoes.ability_panel.hint." + type.getId())
                        .withStyle(unlocked ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);
                tooltip = unlocked
                        ? java.util.List.of(Component.translatable("overview.unknown_echoes.ability.tip.research",
                                ClientAbilityCache.getResearchLevel(type)).withStyle(ChatFormatting.AQUA), hint)
                        : java.util.List.of(hint);
            }
            rowY += 38;
        }
        if (tooltip != null) {
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
        // 能量栏位(V0.6D,12.2:能量显示在能力管理面板;不做常驻 HUD)
        int energyNow = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getEnergy();
        int max = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getMaxEnergy();
        int barY = contentBottom() - 38;
        int barX = contentLeft() + 2;
        int barW = contentRight() - 10 - barX;
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.energy.pool"),
                barX, barY - 11, COL_GOLD_DIM, false);
        graphics.fillGradient(barX - 2, barY - 2, barX + barW + 2, barY + 10, 0x66101824, 0xAA03050A);
        graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF0B1118);
        graphics.renderOutline(barX - 1, barY - 1, barW + 2, 10, COL_EDGE);
        if (energyNow >= 0 && max > 0) {
            int fill = (int) ((long) barW * energyNow / max);
            graphics.fillGradient(barX, barY, barX + fill, barY + 8, COL_ACCENT, COL_ACCENT_DIM);
            if (fill > 4) {
                graphics.fill(barX + fill - 2, barY + 1, barX + fill, barY + 7, COL_GOLD);
            }
            String text = energyNow + " / " + max;
            graphics.drawString(this.font, text,
                    barX + barW - this.font.width(text) - 2, barY - 11, COL_ACCENT_DIM, false);
        }
    }

    /** 神器页(V0.6D):凭据卡片——等级刻度/调谐词条/冷却;数据全部来自 ArtifactSyncPayload。 */
    private void renderArtifactPage(GuiGraphics graphics, int mouseX, int mouseY) {
        var types = cn.kurt6.unknown_echoes.artifact.ArtifactType.values();
        java.util.List<Component> artifactHoverTip = null;
        for (int i = 0; i < types.length; i++) {
            var type = types[i];
            int rowY = artifactRowY(i);
            var entry = cn.kurt6.unknown_echoes.client.ClientArtifactCache.get(type.getId());
            boolean claimed = entry != null;
            int cardBottom = rowY + (this.stationMode
                    == cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload.MODE_TUNING ? 72 : 44);
            graphics.fillGradient(contentLeft(), rowY - 2, contentRight() - 4, cardBottom,
                    COL_CARD, claimed ? 0x55101824 : 0x4405070B);
            graphics.fillGradient(contentLeft() + 4, rowY + 1, contentRight() - 10, rowY + 18,
                    claimed ? COL_GOLD_GLOW : 0x113D4852, 0x00101824);
            graphics.fill(contentLeft(), rowY - 2, contentLeft() + 3, cardBottom,
                    claimed ? COL_GOLD : COL_SILHOUETTE);
            graphics.renderOutline(contentLeft(), rowY - 2,
                    contentRight() - contentLeft() - 4, cardBottom - rowY + 2, COL_EDGE_SOFT);
            if (claimed) {
                drawCornerMarks(graphics, contentLeft(), rowY - 2, contentRight() - 4, cardBottom, COL_GOLD_DIM);
            }
            // 凭据物品作图标(物品只是凭据,图标恰如其分)
            graphics.fillGradient(contentLeft() + 5, rowY + 3, contentLeft() + 27, rowY + 25,
                    0x77101824, 0xAA03050A);
            graphics.renderOutline(contentLeft() + 5, rowY + 3, 22, 22,
                    claimed ? COL_GOLD_DIM : COL_EDGE_SOFT);
            graphics.renderItem(new net.minecraft.world.item.ItemStack(
                    type.getCredentialItem()), contentLeft() + 8, rowY + 6);
            Component name = Component.translatable("artifact.unknown_echoes." + type.getId());
            graphics.drawString(this.font, name, contentLeft() + 32, rowY + 2,
                    claimed ? COL_TEXT : COL_SILHOUETTE, true);
            if (claimed) {
                // 等级刻度(3 级)
                int pipX = contentLeft() + 32;
                for (int pip = 0; pip < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL; pip++) {
                    int color = pip < entry.level() ? COL_GOLD : 0xFF1C2630;
                    int px = pipX + pip * 12;
                    graphics.fillGradient(px, rowY + 14, px + 8, rowY + 20,
                            color, pip < entry.level() ? COL_GOLD_DIM : 0xFF0B1118);
                    graphics.renderOutline(px, rowY + 14, 8, 6, pip < entry.level() ? COL_GOLD_DIM : COL_EDGE);
                }
                Component tuning = entry.tuning().isEmpty()
                        ? Component.translatable("overview.unknown_echoes.artifact.untuned")
                        : Component.translatable("artifact.unknown_echoes." + type.getId()
                                + ".tuning." + entry.tuning());
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.artifact.tuning_label")
                                .append(": ").append(tuning),
                        contentLeft() + 32, rowY + 24, COL_ACCENT_DIM, false);
                int cooldown = cn.kurt6.unknown_echoes.client.ClientArtifactCache
                        .getCooldownSeconds(type.getId());
                Component state = cooldown > 0
                        ? Component.translatable("overview.unknown_echoes.artifact.cooldown", cooldown)
                        : Component.translatable("overview.unknown_echoes.artifact.ready");
                graphics.drawString(this.font, state, contentLeft() + 32, rowY + 34,
                        cooldown > 0 ? COL_DIM : COL_ACCENT, false);
            } else {
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.artifact.unclaimed"),
                        contentLeft() + 32, rowY + 16, COL_SILHOUETTE, false);
                graphics.drawString(this.font,
                        Component.translatable("overview.unknown_echoes.artifact.hint." + type.getId()),
                        contentLeft() + 32, rowY + 28, COL_DIM, false);
            }
            if (mouseX >= contentLeft() && mouseX <= contentRight() - 4
                    && mouseY >= rowY - 2 && mouseY < cardBottom) {
                java.util.List<Component> tip = new ArrayList<>();
                tip.add(name.copy().withStyle(claimed ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                if (claimed) {
                    int lvl = entry.level();
                    tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.current", lvl)
                            .withStyle(ChatFormatting.GOLD));
                    tip.add(Component.translatable("overview.unknown_echoes.artifact." + type.getId() + ".l" + lvl)
                            .withStyle(ChatFormatting.AQUA));
                    if (lvl < cn.kurt6.unknown_echoes.artifact.ArtifactType.MAX_LEVEL) {
                        tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.next", lvl + 1)
                                .withStyle(ChatFormatting.DARK_GRAY));
                        tip.add(Component.translatable("overview.unknown_echoes.artifact." + type.getId() + ".l" + (lvl + 1))
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.maxed")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                } else {
                    tip.add(Component.translatable("overview.unknown_echoes.artifact.tip.locked")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                artifactHoverTip = tip;
            }
        }
        // 台座上下文提示(底部一行)
        String contextKey = switch (this.stationMode) {
            case 0 -> "overview.unknown_echoes.artifact.at_record";
            case 1 -> "overview.unknown_echoes.artifact.at_tuning";
            default -> "overview.unknown_echoes.artifact.no_station";
        };
        graphics.drawString(this.font, Component.translatable(contextKey)
                        .withStyle(ChatFormatting.ITALIC),
                contentLeft() + 4, contentBottom() - 12, COL_DIM, false);

        // ---- 凭据与誓记(V0.6E,5.8):共鸣信物与神器凭据的状态登记 ----
        int credY = artifactRowY(types.length) + 4;
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.credentials.header"),
                contentLeft() + 4, credY, COL_GOLD, true);
        graphics.fill(contentLeft() + 4, credY + 10, contentRight() - 8, credY + 11, COL_EDGE_SOFT);
        credY += 15;
        for (EchoAbilityType ability : new EchoAbilityType[]{EchoAbilityType.WIND_ECHO,
                EchoAbilityType.TIDE_ECHO, EchoAbilityType.TRUE_SIGHT_ECHO}) {
            if (credY > contentBottom() - 26) {
                break;
            }
            boolean fulfilled = ClientAbilityCache.hasAbility(ability);
            boolean ritual = ClientAbilityCache.hasRitualRecord(ability);
            boolean holding = clientHasToken(ability);
            String stateKey = fulfilled ? "fulfilled" : holding ? "holding"
                    : ritual ? "reclaimable" : "dormant";
            boolean lit = fulfilled || ritual || holding;
            Component line = Component.literal(lit ? "✦ " : "◇ ")
                    .withStyle(lit ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY)
                    .append(Component.translatable("overview.unknown_echoes.credentials.token."
                            + ability.getId()))
                    .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable("overview.unknown_echoes.credentials." + stateKey)
                            .withStyle(lit ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
            graphics.drawString(this.font, line, contentLeft() + 8, credY,
                    lit ? COL_TEXT : COL_SILHOUETTE, false);
            credY += 12;
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

    /** 迷途交易页(V0.6D,5.8):旅者摊卷——行点击即购买请求,成交由服务端校验。 */
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
        for (int i = 0; i < cache.size(); i++) {
            var entry = cache.get(i);
            int rowY = tradeRowY(i);
            if (rowY + 22 > contentBottom() - 14) {
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
            float shade = soldOut ? 0.4F : 1.0F;
            graphics.setColor(shade, shade, shade, 1.0F);
            renderTradeCosts(graphics, entry.costs(), contentLeft() + 6, rowY + 3);
            graphics.drawString(this.font, "→", contentLeft() + 48, rowY + 7,
                    soldOut ? COL_SILHOUETTE : COL_GOLD_DIM, false);
            graphics.renderItem(entry.result(), contentLeft() + 62, rowY + 3);
            graphics.renderItemDecorations(this.font, entry.result(), contentLeft() + 62, rowY + 3);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Component label = entry.result().getHoverName();
            graphics.drawString(this.font, label, contentLeft() + 86, rowY + 7,
                    soldOut ? COL_SILHOUETTE : COL_TEXT, false);
            Component uses = entry.usesLeft() < 0
                    ? Component.translatable("overview.unknown_echoes.trade.unlimited")
                    : Component.translatable("overview.unknown_echoes.trade.uses", entry.usesLeft());
            graphics.drawString(this.font, uses,
                    contentRight() - this.font.width(uses) - 10, rowY + 7,
                    soldOut ? COL_SILHOUETTE : COL_DIM, false);
        }
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.trade.footer")
                        .withStyle(ChatFormatting.ITALIC),
                contentLeft() + 4, contentBottom() - 12, COL_DIM, false);
    }

    private int tradeRowY(int index) {
        return contentTop() + 22 + index * 24;
    }

    private void renderTradeCosts(GuiGraphics graphics, List<net.minecraft.world.item.ItemStack> costs,
                                  int x, int y) {
        int shown = Math.min(2, costs.size());
        for (int i = 0; i < shown; i++) {
            int itemX = x + i * 19;
            graphics.renderItem(costs.get(i), itemX, y);
            graphics.renderItemDecorations(this.font, costs.get(i), itemX, y);
            if (i == 0 && shown > 1) {
                graphics.drawString(this.font, "+", x + 16, y + 6, COL_GOLD_DIM, false);
            }
        }
        if (costs.size() > 2) {
            graphics.drawString(this.font, "...", x + 36, y + 6, COL_DIM, false);
        }
    }

    /** 右侧"回声旁注":选中页的最近进度与下一条线索(5.8.0)。 */
    private void renderSideNote(GuiGraphics graphics) {
        int x = panelRight() - NOTE_WIDTH - 2;
        int top = contentTop();
        drawGlassPanel(graphics, x, top, panelRight() - 2, contentBottom(), COL_GOLD_DIM, false);
        graphics.fillGradient(x + 3, top + 3, panelRight() - 5, contentBottom() - 3,
                0x68142330, 0xA005080D);
        graphics.fillGradient(x + 4, top + 4, panelRight() - 8, top + 36,
                0x2EE2C276, 0x00101824);
        drawSoftGrid(graphics, x + 6, top + 18, panelRight() - 9, contentBottom() - 8, 0x0D38A59E);
        graphics.fill(x, top, x + 2, contentBottom(), COL_GOLD_DIM);
        int textX = x + 5;
        int y = top + 5;
        graphics.drawString(this.font,
                Component.translatable("overview.unknown_echoes.note.header"), textX, y, COL_GOLD, false);
        drawFineDivider(graphics, textX, y + 10, panelRight() - 8, COL_EDGE_SOFT);
        y += 13;
        Component note = Component.translatable("overview.unknown_echoes.note." + this.page.id);
        for (FormattedCharSequence seq : this.font.split(note, NOTE_WIDTH - 10)) {
            graphics.drawString(this.font, seq, textX, y, COL_DIM, false);
            y += 10;
        }
        y += 4;
        Component next = nextClue();
        if (next != null) {
            int boxTop = y - 2;
            int boxBottom = Math.min(contentBottom() - 7, boxTop + 52);
            graphics.fillGradient(textX - 2, boxTop, panelRight() - 8, boxBottom,
                    0x26294755, 0x1103050A);
            graphics.renderOutline(textX - 2, boxTop, panelRight() - textX - 6,
                    boxBottom - boxTop, COL_EDGE_FAINT);
            graphics.drawString(this.font,
                    Component.translatable("overview.unknown_echoes.note.next"), textX, y, COL_GOLD, false);
            y += 12;
            for (FormattedCharSequence seq : this.font.split(next, NOTE_WIDTH - 10)) {
                graphics.drawString(this.font, seq, textX, y, COL_ACCENT_DIM, false);
                y += 10;
            }
        }
    }

    /** 下一条可执行线索:按已同步进度给含蓄指引,不泄露隐藏答案(红线 #9)。 */
    private Component nextClue() {
        boolean wind = ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO);
        boolean tide = ClientAbilityCache.hasAbility(EchoAbilityType.TIDE_ECHO);
        boolean sight = ClientAbilityCache.hasAbility(EchoAbilityType.TRUE_SIGHT_ECHO);
        if (!wind && !tide && !sight) {
            return Component.translatable("overview.unknown_echoes.clue.start");
        }
        if (tide && !ClientJournalCache.structures().contains(id("tide_lighthouse_reef"))) {
            return Component.translatable("overview.unknown_echoes.clue.lighthouse");
        }
        if (sight && !ClientJournalCache.structures().contains(id("mirror_dust_cloister"))) {
            return Component.translatable("overview.unknown_echoes.clue.cloister");
        }
        if (!tide) {
            return Component.translatable("overview.unknown_echoes.clue.tide");
        }
        if (!sight) {
            return Component.translatable("overview.unknown_echoes.clue.sight");
        }
        return Component.translatable("overview.unknown_echoes.clue.research");
    }

    /** 底部"同步状态条":区域 / 能量摘要(V0.6D 前为占位)/ 同步状态(5.8.0)。 */
    private void renderStatusBar(GuiGraphics graphics) {
        int y = panelBottom() - STATUS_HEIGHT;
        graphics.fillGradient(panelLeft() + NAV_WIDTH + 1, y, panelRight(), panelBottom(),
                0xDD142330, 0xF003050A);
        graphics.fill(panelLeft() + NAV_WIDTH + 1, y, panelRight(), y + 1, COL_EDGE_SOFT);
        graphics.fill(panelLeft() + NAV_WIDTH + 1, y + 1,
                panelLeft() + NAV_WIDTH + 80, y + 2, COL_ACCENT_DIM);
        graphics.fillGradient(panelRight() - 126, y + 1, panelRight() - 5, panelBottom() - 2,
                0x255BE8D8, 0x0003050A);
        graphics.fillGradient(contentLeft() - 2, y + 2, contentLeft() + 160, panelBottom() - 2,
                0x16E2C276, 0x00101824);
        Minecraft mc = Minecraft.getInstance();
        String dimension = mc.level != null
                ? mc.level.dimension().location().getPath() : "unknown";
        Component left = Component.translatable("overview.unknown_echoes.status.region",
                Component.translatableWithFallback("dimension.unknown_echoes." + dimension, dimension));
        graphics.drawString(this.font, left, contentLeft(), y + 3, COL_DIM, false);
        int energyNow = cn.kurt6.unknown_echoes.client.ClientArtifactCache.getEnergy();
        Component energy = energyNow < 0
                ? Component.translatable("overview.unknown_echoes.status.energy_dormant")
                : Component.translatable("overview.unknown_echoes.status.energy",
                        energyNow, cn.kurt6.unknown_echoes.client.ClientArtifactCache.getMaxEnergy());
        graphics.drawString(this.font, energy,
                (panelLeft() + panelRight()) / 2 - this.font.width(energy) / 2 + NAV_WIDTH / 2,
                y + 3, energyNow < 0 ? COL_SILHOUETTE : COL_ACCENT_DIM, false);
        boolean synced = !ClientJournalCache.structures().isEmpty()
                || !ClientJournalCache.pages().isEmpty()
                || ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)
                || ClientAbilityCache.hasAbility(EchoAbilityType.TIDE_ECHO);
        Component sync = Component.translatable(synced
                ? "overview.unknown_echoes.status.synced"
                : "overview.unknown_echoes.status.unsynced");
        graphics.drawString(this.font, sync,
                panelRight() - this.font.width(sync) - 6, y + 3,
                synced ? COL_ACCENT_DIM : COL_SILHOUETTE, false);
    }

    /** 阅读弹层:残页/拓片的单页书面阅读(5.8 残页与研究)。 */
    private void renderPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        int w = 150;
        int h = 180;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        graphics.fillGradient(x - 10, y - 10, x + w + 10, y + h + 12, 0xAA03050A, 0xDD000000);
        drawCornerMarks(graphics, x - 8, y - 8, x + w + 8, y + h + 8, COL_GOLD_DIM);
        graphics.blit(PAPER, x, y, 0, 0, w, h, w, h);
        graphics.drawCenteredString(this.font, this.popup.title.copy().withStyle(ChatFormatting.BOLD),
                x + w / 2, y + 10, 0xFF6B5018);
        List<FormattedCharSequence> split = this.font.split(this.popup.text, w - 24);
        int textY = y + 30;
        for (FormattedCharSequence seq : split) {
            if (textY > y + h - 16) {
                break;
            }
            graphics.drawString(this.font, seq, x + 12, textY, 0xFF3A372E, false);
            textY += 10;
        }
        Component close = Component.translatable("overview.unknown_echoes.popup.close");
        graphics.drawCenteredString(this.font, close, x + w / 2, y + h + 6, COL_DIM);
    }

    /** 自绘按钮:统一替换原版灰按钮,保留 Minecraft GUI 的轻量输入模型。 */
    private void renderActionButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ActionButton button : this.actionButtons) {
            boolean enabled = button.enabled.getAsBoolean();
            boolean hovered = enabled && button.contains(mouseX, mouseY);
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
            if (enabled && (button.active || hovered)) {
                drawCornerMarks(graphics, button.x, button.y, button.x + button.width, button.y + button.height,
                        button.active ? COL_GOLD : COL_ACCENT);
                graphics.fill(button.x + 6, button.y + button.height - 3,
                        button.x + button.width - 6, button.y + button.height - 2,
                        button.active ? COL_GOLD_DIM : COL_ACCENT_DIM);
            }
            int color = !enabled ? COL_SILHOUETTE : (button.active ? COL_GOLD : (hovered ? COL_ACCENT : COL_TEXT));
            graphics.drawCenteredString(this.font, button.label,
                    button.x + button.width / 2, button.y + (button.height - 8) / 2, color);
        }
    }

    // ---- 输入 ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.popup != null) {
            this.popup = null;
            return true;
        }
        for (ActionButton actionButton : this.actionButtons) {
            if (actionButton.enabled.getAsBoolean() && actionButton.contains(mouseX, mouseY)) {
                actionButton.action.run();
                return true;
            }
        }
        // 共鸣脊线导航
        ModulePage[] pages = ModulePage.values();
        for (int i = 0; i < pages.length; i++) {
            int rowY = navRowY(i);
            if (mouseX >= panelLeft() && mouseX < panelLeft() + NAV_WIDTH
                    && mouseY >= rowY && mouseY < rowY + 20) {
                switchPage(pages[i]);
                return true;
            }
        }
        // 总览星盘卡片
        if (this.page == ModulePage.OVERVIEW) {
            ModulePage clicked = overviewCardAt(mouseX, mouseY);
            if (clicked != null) {
                switchPage(clicked);
                return true;
            }
        }
        // 迷途交易:行点击=购买请求(服务端校验成交,5.8 边界)
        if (this.page == ModulePage.TRADE
                && cn.kurt6.unknown_echoes.client.ClientTradeCache.hasSession()) {
            var entries = cn.kurt6.unknown_echoes.client.ClientTradeCache.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                int rowY = tradeRowY(i);
                if (rowY + 22 > contentBottom() - 14) {
                    break;
                }
                if (mouseX >= contentLeft() && mouseX < contentRight() - 4
                        && mouseY >= rowY && mouseY < rowY + 22) {
                    if (entries.get(i).usesLeft() != 0) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new cn.kurt6.unknown_echoes.network.TradeRequestPayload(
                                        cn.kurt6.unknown_echoes.client.ClientTradeCache.getTravelerEntityId(), i));
                    }
                    return true;
                }
            }
        }
        // 内容区可点击行(残页阅读)
        if (this.page.awake && this.page != ModulePage.ABILITY && this.page != ModulePage.OVERVIEW) {
            int top = this.page == ModulePage.JOURNAL ? contentTop() + 24 : contentTop() + 8;
            int rowHeight = 14;
            int visible = (contentBottom() - 26 - top) / rowHeight;
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
        int cx = (contentLeft() + contentRight()) / 2;
        int cy = (contentTop() + contentBottom()) / 2;
        ModulePage[] entries = {ModulePage.ABILITY, ModulePage.JOURNAL, ModulePage.RESEARCH,
                ModulePage.ARTIFACT, ModulePage.TRADE, ModulePage.INDEX, ModulePage.OPTIONS};
        int cols = 2;
        int cardW = Math.min(138, Math.max(100, (contentWidth() - 190) / 2));
        int cardH = 42;
        int gap = 8;
        int startX = Math.max(contentLeft() + 174, contentRight() - (cols * cardW + gap) - 12);
        int startY = contentTop() + 18;
        for (int i = 0; i < entries.length; i++) {
            int x = startX + (i % cols) * (cardW + gap);
            int y = startY + (i / cols) * (cardH + gap);
            if (mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH) {
                return entries[i];
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.popup == null && !this.lines.isEmpty()) {
            this.scroll = Math.max(0, this.scroll - (int) Math.signum(deltaY) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC 逐层返回:弹层 → 页面 → 总览 → 关闭(5.8 交互主张)
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
        // 键盘焦点:上下键在共鸣脊线移动,回车进入(手柄/键盘可用)
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            int delta = keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1;
            this.navFocus = Math.floorMod(
                    (this.navFocus < 0 ? this.page.ordinal() : this.navFocus) + delta,
                    ModulePage.values().length);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.navFocus >= 0) {
            switchPage(ModulePage.values()[this.navFocus]);
            this.navFocus = -1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 工具 ----

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

    private static void drawRuneTick(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 1, x + 2, y + 5, color);
        graphics.fill(x + 2, y, x + 6, y + 1, color);
        graphics.fill(x + 5, y + 1, x + 7, y + 5, color);
    }

    private void addWrapped(Component component, Runnable action) {
        int width = contentRight() - contentLeft() - 8;
        List<FormattedCharSequence> split = this.font.split(component, width);
        if (split.isEmpty()) {
            this.lines.add(new Line(FormattedCharSequence.EMPTY, action));
            return;
        }
        for (FormattedCharSequence seq : split) {
            this.lines.add(new Line(seq, action));
            action = null; // 多行时只有首行可点击,避免误触
        }
    }

    private boolean addRevisit(List<Component> content, EchoAbilityType ability,
                               String structure, String key) {
        if (ClientAbilityCache.hasAbility(ability)
                && ClientJournalCache.structures().contains(id(structure))) {
            content.add(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
            return true;
        }
        return false;
    }

    /** 个人机制记录驱动的回访线索(V0.6C Mini Boss 首杀写入)——记录不同步则不显示。 */
    private boolean addRevisitMechanism(List<Component> content, String mechanismKey, String langKey) {
        // 机制记录不在 Journal 同步范围;以结构发现作为近似条件(线索本身是含蓄文案)
        String structure = mechanismKey.contains("lighthouse") ? "tide_lighthouse_reef" : "mirror_dust_cloister";
        if (ClientJournalCache.structures().contains(id(structure))) {
            content.add(Component.translatable(langKey).withStyle(ChatFormatting.DARK_PURPLE));
            return true;
        }
        return false;
    }

    private static Component count(String key, int have, int total) {
        return Component.translatable(key, have, total);
    }

    private static Component discoveredEntry(boolean discovered, Component name) {
        if (discovered) {
            return Component.literal("✦ ").withStyle(ChatFormatting.DARK_AQUA).append(name);
        }
        return Component.literal("◇ ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("journal.unknown_echoes.book.unknown")
                        .withStyle(ChatFormatting.GRAY));
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
        for (var type : cn.kurt6.unknown_echoes.artifact.ArtifactType.values()) {
            if (cn.kurt6.unknown_echoes.client.ClientArtifactCache.isClaimed(type.getId())) {
                count++;
            }
        }
        return count;
    }

    private static int artifactUpgradeSteps() {
        int count = 0;
        for (var type : cn.kurt6.unknown_echoes.artifact.ArtifactType.values()) {
            var entry = cn.kurt6.unknown_echoes.client.ClientArtifactCache.get(type.getId());
            count += entry == null ? 0 : Math.max(0, entry.level() - 1);
        }
        return count;
    }

    private static int visibleCompletionScore() {
        int score = 0;
        score += Math.min(EchoRealmBiomeCatalog.countKnown(ClientJournalCache.biomes()) * 3, 30);
        score += Math.min(ClientJournalCache.structures().size() * 2, 40);
        score += Math.min(miniBossCount() * 6, 48);
        score += Math.min(ClientJournalCache.pages().size(), 80);
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

    private static String visibleBackfillTarget() {
        if (ClientJournalCache.pages().size() < Math.max(T7_REQUIRED_PAGES, INDEX_EXPANSION_ONE_PAGES)) {
            return "pages";
        }
        if (miniBossCount() < MINI_BOSSES.length) {
            return "mini_bosses";
        }
        if (artifactUpgradeSteps() < cn.kurt6.unknown_echoes.artifact.ArtifactType.values().length
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

    private record ActionButton(int x, int y, int width, int height, Component label,
                                BooleanSupplier enabled, Runnable action, boolean active) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    private record ReadingPopup(Component title, Component text) {
    }
}
