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
    private static final int BAR_INCREMENT = 24;
    private static final int SEGMENT_WIDTH = 18;

    private EchoBossBarOverlay() {
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
        long time = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        boolean dangerPulse = progress <= 0.25F && (time / 5L) % 2L == 0L;

        renderAura(graphics, x, y, theme, dangerPulse);
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
                    dangerPulse ? 0xFFFFE0B0 : theme.tickColor);
        }
        renderSegments(graphics, x, y, theme, fillWidth);
        if (dangerPulse) {
            graphics.renderOutline(x + 6, y + 6, TEXTURE_WIDTH - 12, TEXTURE_HEIGHT - 8, 0xAAFF6F5A);
        }
        renderText(graphics, Minecraft.getInstance().font, bossEvent.getName(), x, y, progress, theme);
    }

    private static void renderAura(GuiGraphics graphics, int x, int y, BossBarTheme theme, boolean dangerPulse) {
        graphics.fillGradient(x - 18, y + 4, x + TEXTURE_WIDTH + 18, y + TEXTURE_HEIGHT - 3,
                dangerPulse ? 0x44FF6F5A : theme.auraColor, 0x00000000);
        graphics.fillGradient(x + 18, y + TEXTURE_HEIGHT - 5, x + TEXTURE_WIDTH - 18, y + TEXTURE_HEIGHT - 3,
                0x00FFFFFF, theme.glintColor);
        graphics.renderOutline(x + 8, y + 7, TEXTURE_WIDTH - 16, TEXTURE_HEIGHT - 10,
                dangerPulse ? 0xFFFF9E8A : theme.frameColor);
    }

    private static void renderSegments(GuiGraphics graphics, int x, int y, BossBarTheme theme, int fillWidth) {
        int barLeft = x + PROGRESS_X;
        int barY = y + PROGRESS_Y;
        for (int offset = SEGMENT_WIDTH; offset < PROGRESS_WIDTH; offset += SEGMENT_WIDTH) {
            int tickX = barLeft + offset;
            boolean lit = offset <= fillWidth;
            graphics.fill(tickX, barY - 1, tickX + 1, barY + PROGRESS_HEIGHT + 1,
                    lit ? theme.tickColor : 0x55263A47);
        }
    }

    private static void renderText(GuiGraphics graphics, Font font, Component name,
                                   int x, int y, float progress, BossBarTheme theme) {
        graphics.drawCenteredString(font, name, graphics.guiWidth() / 2, y + 3, theme.titleColor);
        String percent = Math.round(progress * 100.0F) + "%";
        int textX = x + TEXTURE_WIDTH - font.width(percent) - 15;
        graphics.drawString(font, percent, textX, y + 14, theme.percentColor, false);
    }

    private enum BossBarTheme {
        // 自绘 BossBar 只覆盖5大主线 Boss;
        // Mini Boss 一律使用原版 BossBar——主线战斗的仪式感与可选战斗的轻量感由此区分,
        COLOSSUS("entity.unknown_echoes.forgotten_colossus", BossEvent.BossBarColor.PURPLE,
                "遗忘巨像", "Forgotten Colossus", "forgotten_colossus", 0xFFF8EBC7, 0xFFEFE6FF,
                0x449873FF, 0xFF8D7440, 0x88F8EBC7, 0xAAD8B76A),
        ABYSS("entity.unknown_echoes.abyss_watcher", BossEvent.BossBarColor.BLUE,
                "深渊观测者", "Abyss Watcher", "abyss_watcher", 0xFFE0FDFF, 0xFFE6FBFF,
                0x4438B8FF, 0xFF2D8B86, 0x88E0FDFF, 0xAA62E6D8),
        MIRROR("entity.unknown_echoes.mirror_guardian", BossEvent.BossBarColor.WHITE,
                "镜像守护者", "Mirror Guardian", "mirror_guardian", 0xFFFFFFFF, 0xFFF0F7FF,
                0x44D6F4FF, 0xFFBFC8D6, 0x99FFFFFF, 0xAAF0F7FF);

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
            String key = translationKey(event.getName());
            for (BossBarTheme theme : values()) {
                if (theme.translationKey.equals(key)) {
                    return theme;
                }
            }
            return matchLocalizedName(event);
        }

        private static BossBarTheme matchLocalizedName(LerpingBossEvent event) {
            String name = event.getName().getString();
            for (BossBarTheme theme : values()) {
                if (theme.color == event.getColor()
                        && (theme.zhName.equals(name) || theme.enName.equals(name))) {
                    return theme;
                }
            }
            return null;
        }

        private static String translationKey(Component component) {
            ComponentContents contents = component.getContents();
            if (contents instanceof TranslatableContents translatable) {
                return translatable.getKey();
            }
            return "";
        }
    }
}
