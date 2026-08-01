package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.client.ClientAbilityCooldownCache;
import cn.kurt6.unknown_echoes.client.ClientArtifactCache;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** 左下角无文字状态环：能量恢复、重力回响冷却与虚空回响冷却。 */
public final class EchoStatusHudOverlay {
    private static final int SEGMENTS = 40;
    private static final int RADIUS = 13;
    private static final int SPACING = 34;
    private static final int[] RING_X = new int[SEGMENTS];
    private static final int[] RING_Y = new int[SEGMENTS];
    private static final int RING_BACKGROUND = 0x8A13212A;
    private static final int ENERGY_COLOR = 0xFF58DDF2;
    private static final int GRAVITY_COLOR = 0xFFE1AE5B;
    private static final int VOID_COLOR = 0xFFD06DE7;
    private static final ItemStack ENERGY_ICON = new ItemStack(ModItems.ECHO_SHARD.get());
    private static final ItemStack GRAVITY_ICON = new ItemStack(ModItems.GRAVITY_CORE_FRAGMENT.get());
    private static final ItemStack VOID_ICON = new ItemStack(ModItems.VOID_RIFT_SHARD.get());

    static {
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = -Math.PI / 2.0D + Math.PI * 2.0D * i / SEGMENTS;
            RING_X[i] = (int) Math.round(Math.cos(angle) * RADIUS);
            RING_Y[i] = (int) Math.round(Math.sin(angle) * RADIUS);
        }
    }

    private EchoStatusHudOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        int x = 22;
        int y = graphics.guiHeight() - 49;
        int energy = ClientArtifactCache.getEnergy();
        int maxEnergy = Math.max(1, ClientArtifactCache.getMaxEnergy());
        if (energy >= 0 && ClientArtifactCache.isEnergyRegenerating()) {
            drawRing(graphics, x, y, (float) energy / maxEnergy, ENERGY_COLOR, ENERGY_ICON);
            y -= SPACING;
        }
        if (ClientAbilityCooldownCache.hasGravityCooldown()) {
            drawRing(graphics, x, y, ClientAbilityCooldownCache.getGravityProgress(),
                    GRAVITY_COLOR, GRAVITY_ICON);
            y -= SPACING;
        }
        if (ClientAbilityCooldownCache.hasVoidCooldown()) {
            drawRing(graphics, x, y, ClientAbilityCooldownCache.getVoidProgress(),
                    VOID_COLOR, VOID_ICON);
        }
    }

    private static void drawRing(GuiGraphics graphics, int centerX, int centerY,
                                 float progress, int color, ItemStack icon) {
        for (int i = 0; i < SEGMENTS; i++) {
            int x = centerX + RING_X[i];
            int y = centerY + RING_Y[i];
            graphics.fill(x - 1, y - 1, x + 1, y + 1, RING_BACKGROUND);
        }
        int filled = Mth.clamp((int) Math.ceil(progress * SEGMENTS), 0, SEGMENTS);
        for (int i = 0; i < filled; i++) {
            int x = centerX + RING_X[i];
            int y = centerY + RING_Y[i];
            graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
        graphics.renderItem(icon, centerX - 8, centerY - 8);
    }
}
