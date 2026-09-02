package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class EchoBossBarOverlay {
    private static final int TEXTURE_WIDTH = 240;
    private static final int TEXTURE_HEIGHT = 30;
    private static final int PROGRESS_WIDTH = 212;
    private static final int PROGRESS_HEIGHT = 6;
    private static final int PROGRESS_X = 14;
    private static final int PROGRESS_Y = 17;
    private static final int CAST_Y = 26;
    private static final int CAST_HEIGHT = 3;
    private static final int BAR_INCREMENT = 24;
    private static final int SEGMENT_WIDTH = 18;
    private static final String[] PERCENT_LABELS = createPercentLabels();
    private static final String[] WORLD_ECHO_PHASE_KEYS = {
            "gui.unknown_echoes.world_echo.phase.wind",
            "gui.unknown_echoes.world_echo.phase.tide",
            "gui.unknown_echoes.world_echo.phase.true_sight",
            "gui.unknown_echoes.world_echo.phase.gravity",
            "gui.unknown_echoes.world_echo.phase.void"
    };
    private static final int[] WORLD_ECHO_PHASE_COLORS = {
            0xFFBDEFFF,
            0xFF62D4E8,
            0xFFE7C9FF,
            0xFFFFD66F,
            0xFFC08AFF
    };
    private static final java.util.Map<LerpingBossEvent, CastVisualState> WORLD_ECHO_CAST_STATES =
            new java.util.IdentityHashMap<>();

    private EchoBossBarOverlay() {
    }

    public static void clearCaches() {
        WORLD_ECHO_CAST_STATES.clear();
        BossBarTheme.MATCH_CACHE.clear();
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossBarTheme theme = BossBarTheme.match(event.getBossEvent());
        if (theme == null) {
            return;
        }
        event.setCanceled(true);
        event.setIncrement(BAR_INCREMENT);
        render(event.getGuiGraphics(), event.getBossEvent(), theme, event.getY());
    }

    private static void render(GuiGraphics graphics, LerpingBossEvent bossEvent,
                               BossBarTheme theme, int vanillaY) {
        int x = graphics.guiWidth() / 2 - TEXTURE_WIDTH / 2;
        int y = Math.max(2, vanillaY - 11);
        float progress = Mth.clamp(bossEvent.getProgress(), 0.0F, 1.0F);
        int fillWidth = Mth.ceil(PROGRESS_WIDTH * progress);
        Minecraft minecraft = Minecraft.getInstance();
        long time = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        boolean dangerPulse = theme != BossBarTheme.WORLD_ECHO_RITUAL
                && progress <= 0.25F && (time / 5L) % 2L == 0L;
        int accentColor = theme == BossBarTheme.WORLD_ECHO
                ? WORLD_ECHO_PHASE_COLORS[worldEchoPhase(progress)]
                : theme == BossBarTheme.WORLD_ECHO_RITUAL
                ? WORLD_ECHO_PHASE_COLORS[ritualEcho(progress)] : theme.tickColor;
        WorldEchoCast cast = theme == BossBarTheme.WORLD_ECHO
                ? WorldEchoCast.match(bossEvent.getName()) : null;
        float castProgress = updateCastProgress(bossEvent, cast, time);

        renderAura(graphics, x, y, theme, dangerPulse, accentColor);
        graphics.blit(theme.background, x, y, 0.0F, 0.0F,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.fillGradient(x + PROGRESS_X - 2, y + PROGRESS_Y - 2,
                x + PROGRESS_X + PROGRESS_WIDTH + 2, y + PROGRESS_Y + PROGRESS_HEIGHT + 2,
                0x7703050A, 0xAA000000);
        if (fillWidth > 0) {
            graphics.blit(theme.progress, x + PROGRESS_X, y + PROGRESS_Y, 0.0F, 0.0F,
                    fillWidth, PROGRESS_HEIGHT, PROGRESS_WIDTH, PROGRESS_HEIGHT);
            graphics.fillGradient(x + PROGRESS_X, y + PROGRESS_Y,
                    x + PROGRESS_X + fillWidth, y + PROGRESS_Y + 2,
                    theme.glintColor, 0x00FFFFFF);
            graphics.fill(x + PROGRESS_X + fillWidth - 1, y + PROGRESS_Y - 1,
                    x + PROGRESS_X + fillWidth + 1, y + PROGRESS_Y + PROGRESS_HEIGHT + 1,
                    dangerPulse ? 0xFFFFE0B0 : accentColor);
        }
        renderSegments(graphics, x, y, theme, fillWidth, accentColor, progress);
        if (cast != null) {
            renderCastTimeline(graphics, x, y, cast, castProgress, accentColor, time);
        }
        if (dangerPulse) {
            graphics.renderOutline(x + 6, y + 6, TEXTURE_WIDTH - 12, TEXTURE_HEIGHT - 8, 0xAAFF6F5A);
        }
        renderText(graphics, minecraft.font, bossEvent.getName(), x, y,
                progress, theme, accentColor);
    }

    private static float updateCastProgress(LerpingBossEvent bossEvent,
                                            WorldEchoCast cast, long gameTime) {
        if (cast == null) {
            WORLD_ECHO_CAST_STATES.remove(bossEvent);
            return 0.0F;
        }
        if (WORLD_ECHO_CAST_STATES.size() > 16) {
            WORLD_ECHO_CAST_STATES.clear();
        }
        CastVisualState state = WORLD_ECHO_CAST_STATES.get(bossEvent);
        if (state == null || state.cast() != cast || gameTime < state.startedAt()) {
            state = new CastVisualState(cast, gameTime);
            WORLD_ECHO_CAST_STATES.put(bossEvent, state);
        }
        return Mth.clamp((gameTime - state.startedAt()) / (float) cast.durationTicks(), 0.0F, 1.0F);
    }

    private static void renderCastTimeline(GuiGraphics graphics, int x, int y,
                                           WorldEchoCast cast, float progress,
                                           int accentColor, long gameTime) {
        int left = x + PROGRESS_X;
        int top = y + CAST_Y;
        int filled = Mth.clamp(Mth.ceil(PROGRESS_WIDTH * progress), 0, PROGRESS_WIDTH);
        graphics.fill(left, top, left + PROGRESS_WIDTH, top + CAST_HEIGHT, 0xCC03050A);
        if (filled > 0) {
            graphics.fill(left, top, left + filled, top + CAST_HEIGHT, accentColor);
            graphics.fill(left + filled - 1, top - 1, left + filled + 1,
                    top + CAST_HEIGHT + 1, 0xFFFFF1C7);
        }
        int elapsedTicks = Mth.floor(progress * cast.durationTicks());
        for (int impactTick : cast.impactTicks()) {
            int markerX = left + Mth.clamp(Math.round(
                    PROGRESS_WIDTH * impactTick / (float) cast.durationTicks()), 1, PROGRESS_WIDTH - 1);
            boolean imminent = Math.abs(impactTick - elapsedTicks) <= 2 && (gameTime & 1L) == 0L;
            int markerColor = imminent ? 0xFFFFFFFF
                    : elapsedTicks >= impactTick ? 0xAA6A7185 : 0xFFFFD989;
            graphics.fill(markerX - 1, top - 1, markerX + 1,
                    top + CAST_HEIGHT + 1, markerColor);
        }
    }

    private static void renderAura(GuiGraphics graphics, int x, int y, BossBarTheme theme,
                                   boolean dangerPulse, int accentColor) {
        graphics.fillGradient(x - 18, y + 4, x + TEXTURE_WIDTH + 18, y + TEXTURE_HEIGHT - 3,
                dangerPulse ? 0x44FF6F5A : theme.auraColor, 0x00000000);
        graphics.fillGradient(x + 18, y + TEXTURE_HEIGHT - 5, x + TEXTURE_WIDTH - 18, y + TEXTURE_HEIGHT - 3,
                0x00FFFFFF, theme.glintColor);
        if (theme != BossBarTheme.RIFT_WARDEN) {
            graphics.renderOutline(x + 8, y + 7, TEXTURE_WIDTH - 16, TEXTURE_HEIGHT - 10,
                    dangerPulse ? 0xFFFF9E8A
                            : theme == BossBarTheme.WORLD_ECHO
                            || theme == BossBarTheme.WORLD_ECHO_RITUAL
                            ? accentColor : theme.frameColor);
        }
    }

    private static void renderSegments(GuiGraphics graphics, int x, int y, BossBarTheme theme,
                                       int fillWidth, int accentColor, float progress) {
        int barLeft = x + PROGRESS_X;
        int barY = y + PROGRESS_Y;
        if (theme == BossBarTheme.WORLD_ECHO_RITUAL) {
            int activeEcho = ritualEcho(progress);
            for (int echo = 0; echo < 5; echo++) {
                int markerX = barLeft + Math.round(PROGRESS_WIDTH * (echo + 0.5F) / 5.0F);
                boolean lit = progress >= echo / 5.0F;
                graphics.fill(markerX - 2, barY - 2, markerX + 2,
                        barY + PROGRESS_HEIGHT + 2,
                        lit ? WORLD_ECHO_PHASE_COLORS[echo] : 0x66303A48);
                if (echo == activeEcho) {
                    graphics.renderOutline(markerX - 4, barY - 3, 8,
                            PROGRESS_HEIGHT + 6, WORLD_ECHO_PHASE_COLORS[echo]);
                }
            }
            return;
        }
        if (theme == BossBarTheme.WORLD_ECHO) {
            for (int threshold = 1; threshold < 5; threshold++) {
                int offset = Math.round(PROGRESS_WIDTH * threshold / 5.0F);
                int markerX = barLeft + offset;
                int nextPhase = 5 - threshold;
                graphics.fill(markerX - 1, barY - 2, markerX + 1,
                        barY + PROGRESS_HEIGHT + 2, WORLD_ECHO_PHASE_COLORS[nextPhase]);
            }
            int phase = worldEchoPhase(progress);
            float centerFraction = 0.9F - phase * 0.2F;
            int activeX = barLeft + Math.round(PROGRESS_WIDTH * centerFraction);
            graphics.renderOutline(activeX - 4, barY - 2, 8,
                    PROGRESS_HEIGHT + 4, accentColor);
            return;
        }
        for (int offset = SEGMENT_WIDTH; offset < PROGRESS_WIDTH; offset += SEGMENT_WIDTH) {
            int tickX = barLeft + offset;
            boolean lit = offset <= fillWidth;
            graphics.fill(tickX, barY - 1, tickX + 1, barY + PROGRESS_HEIGHT + 1,
                    lit ? theme.tickColor : 0x55263A47);
        }
    }

    private static void renderText(GuiGraphics graphics, Font font, Component name,
                                   int x, int y, float progress, BossBarTheme theme,
                                   int accentColor) {
        graphics.drawCenteredString(font, name, graphics.guiWidth() / 2, y + 3, theme.titleColor);
        if (theme == BossBarTheme.WORLD_ECHO) {
            Component phase = Component.translatable(WORLD_ECHO_PHASE_KEYS[worldEchoPhase(progress)]);
            graphics.drawString(font, phase, x + PROGRESS_X, y + 14, accentColor, true);
        } else if (theme == BossBarTheme.WORLD_ECHO_RITUAL) {
            Component channel = Component.translatable(
                    "gui.unknown_echoes.world_resonance.ritual.channel");
            graphics.drawString(font, channel, x + PROGRESS_X, y + 14, accentColor, true);
        }
        String percent = PERCENT_LABELS[Mth.clamp(Math.round(progress * 100.0F), 0, 100)];
        int textX = x + TEXTURE_WIDTH - font.width(percent) - 15;
        graphics.drawString(font, percent, textX, y + 14, theme.percentColor, false);
    }

    private static int worldEchoPhase(float progress) {
        if (progress > 0.80F) {
            return 0;
        }
        if (progress > 0.60F) {
            return 1;
        }
        if (progress > 0.40F) {
            return 2;
        }
        if (progress > 0.20F) {
            return 3;
        }
        return 4;
    }

    private static int ritualEcho(float progress) {
        return Mth.clamp(Mth.floor(progress * 5.0F), 0, 4);
    }

    private static String[] createPercentLabels() {
        String[] labels = new String[101];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = i + "%";
        }
        return labels;
    }

    private static String translationKey(Component component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return "";
    }

    private enum WorldResonanceRitualStage {
        AWAKENING("gui.unknown_echoes.world_resonance.ritual.awakening"),
        CONVERGING("gui.unknown_echoes.world_resonance.ritual.converging"),
        FOLDING("gui.unknown_echoes.world_resonance.ritual.folding"),
        MANIFESTING("gui.unknown_echoes.world_resonance.ritual.manifesting");

        private static final WorldResonanceRitualStage[] VALUES = values();
        private final String translationKey;

        WorldResonanceRitualStage(String translationKey) {
            this.translationKey = translationKey;
        }

        private static WorldResonanceRitualStage match(Component name) {
            String key = translationKey(name);
            for (WorldResonanceRitualStage stage : VALUES) {
                if (stage.translationKey.equals(key)) {
                    return stage;
                }
            }
            return null;
        }
    }

    private enum WorldEchoCast {
        MANIFESTING("gui.unknown_echoes.world_echo.cast.manifesting", 52, 52),
        SILENCING("gui.unknown_echoes.world_echo.cast.silencing", 80, 12, 24, 36, 48, 60, 72),
        WIND("gui.unknown_echoes.world_echo.cast.wind", 48, 34),
        TIDE("gui.unknown_echoes.world_echo.cast.tide", 40, 20),
        TRUE_SIGHT("gui.unknown_echoes.world_echo.cast.true_sight", 52, 22),
        GRAVITY("gui.unknown_echoes.world_echo.cast.gravity", 48, 28),
        VOID("gui.unknown_echoes.world_echo.cast.void", 56, 24),
        LANCE("gui.unknown_echoes.world_echo.cast.lance", 36, 14),
        SLAM("gui.unknown_echoes.world_echo.cast.slam", 44, 22),
        BARRAGE("gui.unknown_echoes.world_echo.cast.barrage", 36, 6, 18, 30),
        BEAM("gui.unknown_echoes.world_echo.cast.beam", 52, 34),
        FLAME("gui.unknown_echoes.world_echo.cast.flame", 44, 22),
        METEOR("gui.unknown_echoes.world_echo.cast.meteor", 56, 32),
        NOVA("gui.unknown_echoes.world_echo.cast.nova", 48, 14, 26, 38),
        SKYFALL("gui.unknown_echoes.world_echo.cast.skyfall", 48, 32),
        WING_STORM("gui.unknown_echoes.world_echo.cast.wing_storm", 40, 11, 17, 23, 29),
        PHASE("gui.unknown_echoes.world_echo.cast.phase", 48, 24);

        private static final WorldEchoCast[] VALUES = values();

        private final String translationKey;
        private final int durationTicks;
        private final int[] impactTicks;

        WorldEchoCast(String translationKey, int durationTicks, int... impactTicks) {
            this.translationKey = translationKey;
            this.durationTicks = durationTicks;
            this.impactTicks = impactTicks;
        }

        private static WorldEchoCast match(Component name) {
            String key = translationKey(name);
            for (WorldEchoCast cast : VALUES) {
                if (cast.translationKey.equals(key)) {
                    return cast;
                }
            }
            return null;
        }

        private int durationTicks() {
            return this.durationTicks;
        }

        private int[] impactTicks() {
            return this.impactTicks;
        }
    }

    private record CastVisualState(WorldEchoCast cast, long startedAt) {
    }

    private enum BossBarTheme {

        COLOSSUS("entity.unknown_echoes.forgotten_colossus", BossEvent.BossBarColor.PURPLE,
                "遗忘巨像", "Forgotten Colossus", "forgotten_colossus", 0xFFF8EBC7, 0xFFEFE6FF,
                0x449873FF, 0xFF8D7440, 0x88F8EBC7, 0xAAD8B76A),
        VOID_PROPHET("entity.unknown_echoes.void_prophet", BossEvent.BossBarColor.PURPLE,
                "虚空先知", "Void Prophet", "void_prophet", 0xFFE9F7FF, 0xFFF1E9FF,
                0x55582B8C, 0xFF5B3B88, 0xAA79E6FF, 0xFFC48BFF),
        RIFT_WARDEN("entity.unknown_echoes.rift_warden", BossEvent.BossBarColor.PURPLE,
                "裂隙看守者", "Rift Warden", "rift_warden", 0xFFF1F8FF, 0xFFD9F8FF,
                0x55304C62, 0xFF69B8C9, 0xAAEF63CE, 0xFFF05BCB),
        ABYSS("entity.unknown_echoes.abyss_watcher", BossEvent.BossBarColor.BLUE,
                "深渊观测者", "Abyss Watcher", "abyss_watcher", 0xFFE0FDFF, 0xFFE6FBFF,
                0x4438B8FF, 0xFF2D8B86, 0x88E0FDFF, 0xAA62E6D8),
        WORLD_ECHO_RITUAL("gui.unknown_echoes.world_resonance.ritual.awakening",
                BossEvent.BossBarColor.WHITE,
                "五重共鸣 | 回响柱苏醒", "Fivefold Resonance | Pillars Awaken", "world_echo",
                0xFFFFF4D6, 0xFFDDFBFF, 0x556837B7, 0xFFD7AF52, 0xAA86EAFF, 0xFFF2D58A),
        WORLD_ECHO("entity.unknown_echoes.world_echo", BossEvent.BossBarColor.WHITE,
                "世界回响体", "World Echo", "world_echo", 0xFFFFF4D6, 0xFFDDFBFF,
                0x556837B7, 0xFFD7AF52, 0xAA86EAFF, 0xFFF2D58A),
        MIRROR("entity.unknown_echoes.mirror_guardian", BossEvent.BossBarColor.WHITE,
                "镜像守护者", "Mirror Guardian", "mirror_guardian", 0xFFFFFFFF, 0xFFF0F7FF,
                0x44D6F4FF, 0xFFBFC8D6, 0x99FFFFFF, 0xAAF0F7FF);

        private static final BossBarTheme[] VALUES = values();
        /** 逐帧 match 按事件实例缓存,避免每帧 getString 解析;上限触发时整体重解析一次。 */
        private static final java.util.Map<LerpingBossEvent, java.util.Optional<BossBarTheme>> MATCH_CACHE =
                new java.util.IdentityHashMap<>();

        private final String translationKey;
        private final BossEvent.BossBarColor color;
        private final String zhName;
        private final String enName;
        private final ResourceLocation background;
        private final ResourceLocation progress;
        private final int titleColor;
        private final int percentColor;
        private final int auraColor;
        private final int frameColor;
        private final int glintColor;
        private final int tickColor;

        BossBarTheme(String translationKey, BossEvent.BossBarColor color,
                     String zhName, String enName, String textureName,
                     int titleColor, int percentColor,
                     int auraColor, int frameColor, int glintColor, int tickColor) {
            this.translationKey = translationKey;
            this.color = color;
            this.zhName = zhName;
            this.enName = enName;
            this.background = UnknownEchoes.id("textures/gui/bossbar/" + textureName + "_background.png");
            this.progress = UnknownEchoes.id("textures/gui/bossbar/" + textureName + "_progress.png");
            this.titleColor = titleColor;
            this.percentColor = percentColor;
            this.auraColor = auraColor;
            this.frameColor = frameColor;
            this.glintColor = glintColor;
            this.tickColor = tickColor;
        }

        private static BossBarTheme match(LerpingBossEvent event) {
            if (MATCH_CACHE.size() > 16) {
                MATCH_CACHE.clear();
            }
            if (WorldResonanceRitualStage.match(event.getName()) != null) {
                MATCH_CACHE.put(event, java.util.Optional.of(WORLD_ECHO_RITUAL));
                return WORLD_ECHO_RITUAL;
            }
            if (WorldEchoCast.match(event.getName()) != null) {
                MATCH_CACHE.put(event, java.util.Optional.of(WORLD_ECHO));
                return WORLD_ECHO;
            }
            return MATCH_CACHE.computeIfAbsent(event, e -> {
                String key = EchoBossBarOverlay.translationKey(e.getName());
                for (BossBarTheme theme : VALUES) {
                    if (theme.translationKey.equals(key)) {
                        return java.util.Optional.of(theme);
                    }
                }
                return java.util.Optional.ofNullable(matchLocalizedName(e));
            }).orElse(null);
        }

        private static BossBarTheme matchLocalizedName(LerpingBossEvent event) {
            String name = event.getName().getString();
            for (BossBarTheme theme : VALUES) {
                if (theme.color == event.getColor()
                        && (theme.zhName.equals(name) || theme.enName.equals(name))) {
                    return theme;
                }
            }
            return null;
        }

    }
}
