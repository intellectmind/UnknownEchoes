package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.client.ClientArtifactCache;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class ArtifactHudOverlay {
    private static final int PANEL_WIDTH = 164;
    private static final int BAR_WIDTH = 98;
    private static final int BAR_HEIGHT = 5;
    private static final int COL_PANEL_TOP = 0xAA06101A;
    private static final int COL_PANEL_BOTTOM = 0xD902050A;
    private static final int COL_EDGE = 0xAA2D8B86;
    private static final int COL_EDGE_FAINT = 0x55304E59;
    private static final int COL_ACCENT = 0xFF52D6FF;
    private static final int COL_ACCENT_DIM = 0xFF2D8B86;
    private static final int COL_GOLD = 0xFFD8B76A;
    private static final int COL_TEXT = 0xFFF2F6FF;
    private static final int COL_DIM = 0xFFCFE7FF;
    private static final String[] ARTIFACT_IDS = {
            ArtifactType.STORM_COMPASS.getId(),
            ArtifactType.TIDE_LANTERN.getId(),
            ArtifactType.ECHO_LENS.getId()
    };
    private static final ItemStack[] ICONS = {
            new ItemStack(ModItems.STORM_COMPASS.get()),
            new ItemStack(ModItems.TIDE_LANTERN.get()),
            new ItemStack(ModItems.ECHO_LENS.get())
    };

    private ArtifactHudOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !hasClaimedArtifact()) {
            return;
        }
        int rows = claimedCount();
        int height = 18 + rows * 26;
        int x = 8;
        int y = graphics.guiHeight() - height - 40;

        renderPanel(graphics, x, y, height);
        renderEnergy(graphics, mc, x, y);
        int rowY = y + 16;
        for (int i = 0; i < ARTIFACT_IDS.length; i++) {
            if (ClientArtifactCache.isClaimed(ARTIFACT_IDS[i])) {
                renderArtifactRow(graphics, mc, ARTIFACT_IDS[i], ICONS[i], x, rowY);
                rowY += 26;
            }
        }
    }

    private static void renderPanel(GuiGraphics graphics, int x, int y, int height) {
        graphics.fillGradient(x - 5, y - 7, x + PANEL_WIDTH + 5, y + height + 5,
                COL_PANEL_TOP, COL_PANEL_BOTTOM);
        graphics.fillGradient(x - 2, y - 4, x + PANEL_WIDTH + 2, y + height + 2,
                0x44223948, 0x7703050A);
        graphics.renderOutline(x - 5, y - 7, PANEL_WIDTH + 10, height + 12, COL_EDGE);
        graphics.renderOutline(x - 2, y - 4, PANEL_WIDTH + 4, height + 6, COL_EDGE_FAINT);
        graphics.fill(x + 7, y - 3, x + 48, y - 2, COL_GOLD);
        graphics.fill(x + 52, y - 3, x + PANEL_WIDTH - 9, y - 2, COL_ACCENT_DIM);
        drawCornerMarks(graphics, x - 5, y - 7, x + PANEL_WIDTH + 5, y + height + 5, COL_EDGE);
    }

    private static void renderEnergy(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int energy = Math.max(0, ClientArtifactCache.getEnergy());
        int max = Math.max(1, ClientArtifactCache.getMaxEnergy());
        int fill = Mth.clamp(energy * BAR_WIDTH / max, 0, BAR_WIDTH);
        long time = mc.level == null ? 0L : mc.level.getGameTime();
        boolean lowFlash = energy * 5 < max && (time / 10L) % 2L == 0L;
        graphics.drawString(mc.font,
                Component.translatable("hud.unknown_echoes.artifact.energy", energy, max),
                x, y, lowFlash ? 0xFFFF9E8A : 0xFFE2F9FF, true);
        int barX = x + 42;
        int barY = y + 3;
        graphics.fillGradient(barX - 2, barY - 2, barX + BAR_WIDTH + 2, barY + BAR_HEIGHT + 2,
                0xAA08121A, 0xDD020408);
        graphics.renderOutline(barX - 2, barY - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4,
                lowFlash ? 0xCCFF6F5A : COL_EDGE);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xAA102030);
        graphics.fillGradient(barX, barY, barX + fill, barY + BAR_HEIGHT,
                lowFlash ? 0xFFFF6F5A : COL_ACCENT, lowFlash ? 0xFFFFB48A : COL_ACCENT_DIM);
        if (fill > 3) {
            graphics.fill(barX + fill - 2, barY, barX + fill, barY + BAR_HEIGHT, COL_GOLD);
        }
    }

    private static void renderArtifactRow(GuiGraphics graphics, Minecraft mc,
                                          String id, ItemStack icon, int x, int y) {
        ClientArtifactCache.Entry entry = ClientArtifactCache.get(id);
        if (entry == null) {
            return;
        }
        int cooldown = ClientArtifactCache.getCooldownSeconds(id);
        graphics.fillGradient(x - 1, y - 3, x + PANEL_WIDTH - 4, y + 22, 0x33101824, 0x1103050A);
        graphics.fill(x + 2, y - 2, x + 3, y + 21, cooldown > 0 ? COL_GOLD : COL_ACCENT_DIM);
        graphics.renderOutline(x + 6, y - 3, 20, 20, COL_EDGE_FAINT);
        graphics.renderItem(icon, x, y - 2);
        Component name = Component.translatable("artifact.unknown_echoes." + id);
        graphics.drawString(mc.font,
                Component.translatable("hud.unknown_echoes.artifact.level", name, entry.level()),
                x + 28, y - 1, COL_TEXT, false);
        Component tuning = entry.tuning().isBlank()
                ? Component.translatable("hud.unknown_echoes.artifact.tuning.none")
                : Component.translatable("artifact.unknown_echoes." + id + ".tuning." + entry.tuning());
        graphics.drawString(mc.font,
                Component.translatable("hud.unknown_echoes.artifact.tuning", tuning),
                x + 28, y + 8, COL_DIM, false);
        Component state = cooldown > 0
                ? Component.translatable("hud.unknown_echoes.artifact.cooldown", cooldown)
                : Component.translatable("hud.unknown_echoes.artifact.ready");
        graphics.drawString(mc.font, state, x + 28, y + 17,
                cooldown > 0 ? 0xFFFFD28A : 0xFF8AF7C5, false);
    }

    private static void drawCornerMarks(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        int len = 7;
        graphics.fill(left - 1, top - 1, left + len, top, color);
        graphics.fill(left - 1, top - 1, left, top + len, color);
        graphics.fill(right - len, top - 1, right + 1, top, color);
        graphics.fill(right, top - 1, right + 1, top + len, color);
        graphics.fill(left - 1, bottom, left + len, bottom + 1, color);
        graphics.fill(left - 1, bottom - len, left, bottom + 1, color);
        graphics.fill(right - len, bottom, right + 1, bottom + 1, color);
        graphics.fill(right, bottom - len, right + 1, bottom + 1, color);
    }

    private static boolean hasClaimedArtifact() {
        for (String id : ARTIFACT_IDS) {
            if (ClientArtifactCache.isClaimed(id)) {
                return true;
            }
        }
        return false;
    }

    private static int claimedCount() {
        int count = 0;
        for (String id : ARTIFACT_IDS) {
            if (ClientArtifactCache.isClaimed(id)) {
                count++;
            }
        }
        return count;
    }
}
