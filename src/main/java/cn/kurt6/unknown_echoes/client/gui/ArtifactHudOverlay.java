package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.client.ClientArtifactCache;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArtifactHudOverlay {
    private static final int PANEL_WIDTH = 112;
    private static final int BAR_WIDTH = 72;
    private static final int BAR_HEIGHT = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int COL_PANEL_TOP = 0x8806101A;
    private static final int COL_PANEL_BOTTOM = 0xB802050A;
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
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !shouldRender(mc.player)) {
            return;
        }
        String held = heldArtifactId(mc.player);
        boolean renderAll = held == null && energyNeedsAttention();
        int rows = renderAll ? claimedCount() : 1;
        int height = 15 + rows * ROW_HEIGHT;
        int x = 8;
        int y = graphics.guiHeight() - height - 34;

        renderPanel(graphics, x, y, height);
        renderEnergy(graphics, mc, x, y);
        int rowY = y + 15;
        for (int i = 0; i < ARTIFACT_IDS.length; i++) {
            if (ClientArtifactCache.isClaimed(ARTIFACT_IDS[i])
                    && (renderAll || ARTIFACT_IDS[i].equals(held))) {
                renderArtifactRow(graphics, mc, ARTIFACT_IDS[i], ICONS[i], x, rowY);
                rowY += ROW_HEIGHT;
            }
        }
    }

    private static void renderPanel(GuiGraphics graphics, int x, int y, int height) {
        graphics.fillGradient(x - 4, y - 5, x + PANEL_WIDTH + 4, y + height + 3,
                COL_PANEL_TOP, COL_PANEL_BOTTOM);
        graphics.fillGradient(x - 1, y - 2, x + PANEL_WIDTH + 1, y + height,
                0x44223948, 0x7703050A);
        graphics.renderOutline(x - 4, y - 5, PANEL_WIDTH + 8, height + 8, COL_EDGE);
        graphics.renderOutline(x - 1, y - 2, PANEL_WIDTH + 2, height + 2, COL_EDGE_FAINT);
        graphics.fill(x + 7, y - 1, x + 35, y, COL_GOLD);
        graphics.fill(x + 39, y - 1, x + PANEL_WIDTH - 8, y, COL_ACCENT_DIM);
        drawCornerMarks(graphics, x - 4, y - 5, x + PANEL_WIDTH + 4, y + height + 3, COL_EDGE);
    }

    private static void renderEnergy(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int energy = Math.max(0, ClientArtifactCache.getEnergy());
        int max = Math.max(1, ClientArtifactCache.getMaxEnergy());
        int fill = Mth.clamp(energy * BAR_WIDTH / max, 0, BAR_WIDTH);
        long time = mc.level == null ? 0L : mc.level.getGameTime();
        boolean lowFlash = energy * 5 < max && (time / 10L) % 2L == 0L;
        graphics.drawString(mc.font,
                Component.translatable("hud.unknown_echoes.artifact.energy", energy, max),
                x + 2, y + 1, lowFlash ? 0xFFFF9E8A : 0xFFE2F9FF, false);
        int barX = x + 37;
        int barY = y + 4;
        graphics.fillGradient(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                0xAA08121A, 0xDD020408);
        graphics.renderOutline(barX - 1, barY - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2,
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
        graphics.fillGradient(x, y - 2, x + PANEL_WIDTH - 3, y + 16, 0x33101824, 0x1103050A);
        graphics.fill(x + 2, y - 1, x + 3, y + 15, cooldown > 0 ? COL_GOLD : COL_ACCENT_DIM);
        graphics.renderOutline(x + 7, y - 1, 18, 18, COL_EDGE_FAINT);
        graphics.renderItem(icon, x + 8, y);
        Component name = Component.translatable("artifact.unknown_echoes." + id);
        graphics.drawString(mc.font,
                Component.translatable("hud.unknown_echoes.artifact.level", name, entry.level()),
                x + 30, y - 1, COL_TEXT, false);
        Component state = cooldown > 0
                ? Component.translatable("hud.unknown_echoes.artifact.cooldown", cooldown)
                : Component.translatable("hud.unknown_echoes.artifact.ready");
        graphics.drawString(mc.font, state, x + 30, y + 8,
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

    private static boolean shouldRender(Player player) {
        return hasClaimedArtifact() && (energyNeedsAttention() || heldArtifactId(player) != null);
    }

    private static boolean energyNeedsAttention() {
        int energy = ClientArtifactCache.getEnergy();
        return energy >= 0 && energy < Math.max(1, ClientArtifactCache.getMaxEnergy());
    }

    private static String heldArtifactId(Player player) {
        for (String id : ARTIFACT_IDS) {
            if (ClientArtifactCache.isClaimed(id)
                    && (isHeldArtifact(player.getMainHandItem(), id)
                    || isHeldArtifact(player.getOffhandItem(), id))) {
                return id;
            }
        }
        return null;
    }

    private static boolean isHeldArtifact(ItemStack stack, String id) {
        for (int i = 0; i < ARTIFACT_IDS.length; i++) {
            if (ARTIFACT_IDS[i].equals(id) && stack.is(ICONS[i].getItem())) {
                return true;
            }
        }
        return false;
    }
}
