package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.particle.ParticleData;
import cn.kurt6.unknown_echoes.particle.ParticleEnchantingLayout;
import cn.kurt6.unknown_echoes.particle.ParticleEnchantingMenu;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleInfo;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.particle.ParticleTooltipPage;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ParticleEnchantingScreen extends AbstractContainerScreen<ParticleEnchantingMenu> {
    private static final ResourceLocation BACKGROUND = UnknownEchoes.id("textures/gui/particle_enchanting.png");
    private static final int TEXT = 0xD9E6ED, DIM = 0x91A7B5, GOLD = 0xDBC08D, TEAL = 0x8DDACF;
    private ParticleKind selected = ParticleKind.FIRE;
    private boolean core, confirmClear;
    private ItemStack lastWeapon = ItemStack.EMPTY;
    private float uiScale = 1;
    private ArtButton detailOwner;
    private List<Component> detailSource;
    private int detailPage, detailPages = 1;
    private final List<ArtButton> operations = new ArrayList<>();
    private final List<ArtButton> kindButtons = new ArrayList<>();

    public ParticleEnchantingScreen(ParticleEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ParticleEnchantingLayout.WIDTH; imageHeight = ParticleEnchantingLayout.HEIGHT;
        inventoryLabelY = 164;
    }
    private static MutableComponent text(String key, Object... args) { return Component.translatable("particle.unknown_echoes." + key, args); }
    private int action(int index) { return switch (index) { case 0 -> 0; case 1 -> selected.ordinal() + (core ? 9 : 1); case 2 -> 17 + selected.ordinal(); default -> 25; }; }

    @Override protected void init() {
        uiScale = ParticleEnchantingLayout.scale(width, height);
        width = (int) Math.ceil(width / uiScale); height = (int) Math.ceil(height / uiScale);
        super.init(); operations.clear(); kindButtons.clear(); confirmClear = false;
        for (ParticleKind kind : ParticleKind.values()) {
            kindButtons.add(addRenderableWidget(new ArtButton(leftPos + ParticleEnchantingLayout.kindX(kind.ordinal()), topPos + ParticleEnchantingLayout.kindY(kind.ordinal()),
                    72, 24, Component.translatable(kind.key()), button -> { selected = kind; confirmClear = false; refresh(); },
                    () -> selected == kind, new ItemStack(kind.material()), kind)));
        }
        ArtButton crystalButton = addRenderableWidget(new ArtButton(leftPos + 198, topPos + ParticleEnchantingLayout.SOURCE_Y,
                ParticleEnchantingLayout.CONTROL_WIDTH, ParticleEnchantingLayout.CONTROL_HEIGHT, text("source_crystal"), button -> { core = false; refresh(); },
                () -> !core, ItemStack.EMPTY, null));
        crystalButton.description = List.of(text("crystal"));
        ArtButton coreButton = addRenderableWidget(new ArtButton(leftPos + 276, topPos + ParticleEnchantingLayout.SOURCE_Y,
                ParticleEnchantingLayout.CONTROL_WIDTH, ParticleEnchantingLayout.CONTROL_HEIGHT, text("source_core"), button -> { core = true; refresh(); },
                () -> core, ItemStack.EMPTY, null));
        coreButton.description = List.of(text("core"));
        String[] labels = {"upgrade", "inject", "remove", "clear_short"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            ArtButton button = new ArtButton(leftPos + ParticleEnchantingLayout.operationX(i), topPos + ParticleEnchantingLayout.operationY(i),
                    ParticleEnchantingLayout.CONTROL_WIDTH, ParticleEnchantingLayout.CONTROL_HEIGHT, text(labels[i]), ignored -> {
                if (index == 3 && !confirmClear) { confirmClear = true; refresh(); return; }
                confirmClear = false;
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action(index));
                refresh();
            }, () -> index == 1 || index == 3 && confirmClear, ItemStack.EMPTY, null);
            operations.add(addRenderableWidget(button));
        }
        refresh();
    }

    // 提示按界面 tick 更新，输入分派前再刷新，避免使用过时的条件或清空确认。
    @Override protected void containerTick() { super.containerTick(); refresh(); }

    private void refresh() {
        if (minecraft == null || minecraft.player == null) return;
        boolean weaponChanged = !ItemStack.isSameItemSameComponents(lastWeapon, menu.weapon());
        if (weaponChanged) {
            lastWeapon = menu.weapon().copy(); confirmClear = false;
        }
        ParticleData data = ParticleRules.data(menu.weapon());
        for (ArtButton button : kindButtons) if (weaponChanged || button.description == null) button.description = ParticleInfo.describe(data, button.kind);
        for (int i = 0; i < operations.size(); i++) {
            ArtButton button = operations.get(i);
            int action = action(i), reason = menu.rejection(minecraft.player, action);
            button.active = reason == 0;
            if (i == 3) button.setMessage(text(confirmClear ? "confirm_clear" : "clear_short"));
            List<Component> description = new ArrayList<>();
            description.add(button.getMessage());
            description.add(text(i == 3 && confirmClear ? "clear_confirm_hint" : "cost"));
            for (var cost : menu.costs(action).entrySet()) description.add(text("material_cost", cost.getKey().getDescription(), cost.getValue(), menu.count(cost.getKey())));
            if (i == 0 && data.tier() < 5) description.add(text("upgrade_preview", ParticleRules.level(data.tier() + 1), data.tier() + 1, ParticleRules.capacity(data.tier() + 1)));
            if (i == 1) {
                description.add(text("inject_preview", ParticleRules.successPercent(data.count(selected)), ParticleRules.injectionCost(data.count(selected))));
                description.add(text("xp_stock", ParticleEnchantingMenu.experience(minecraft.player)));
                if (data.total() < data.capacity()) description.add(text("inject_result", data.count(selected), data.count(selected) + 1,
                        String.format(java.util.Locale.ROOT, "%.1f", (data.total() + 1) * ParticleRules.DAMAGE_PER_PARTICLE)));
            }
            if (i >= 2) description.add(text("refund", i == 2 ? ParticleRules.singleRefund(data.count(selected)) : ParticleRules.clearRefund(data)));
            if (reason != 0) description.add(text("result." + reason));
            button.description = description;
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, imageWidth, imageHeight, 0, 0, 720, 544, 720, 544);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF52636C);
            graphics.fill(x, y, x + 16, y + 16, 0xFF0D1720);
            graphics.fill(x, y, x + 16, y + 1, 0xFF080F16);
        }
        if (menu.weapon().isEmpty()) {
            graphics.setColor(.42F, .48F, .52F, .65F);
            graphics.renderItem(new ItemStack(ModItems.ECHO_BROADSWORD.get()), leftPos + ParticleEnchantingLayout.WEAPON_X, topPos + ParticleEnchantingLayout.WEAPON_Y);
            graphics.setColor(1, 1, 1, 1);
        }
        ParticleData data = ParticleRules.data(menu.weapon());
        int capacity = data.capacity();
        for (int i = 0; i < 50; i++) {
            int x = leftPos + 16 + i * 3, y = topPos + 115;
            int color = i < data.total() ? 0xFF000000 | kindAt(data, i).color : i < capacity ? 0xFF354B5C : 0xFF162532;
            graphics.fill(x, y, x + 2, y + 5, color);
        }
        if (minecraft != null && minecraft.player != null) {
            ItemStack material = new ItemStack(core ? ModItems.PARTICLE_CORE.get() : selected.material());
            graphics.renderItem(material, leftPos + 198, topPos + 158);
        }
    }

    private static ParticleKind kindAt(ParticleData data, int index) {
        for (ParticleKind kind : ParticleKind.values()) { index -= data.count(kind); if (index < 0) return kind; }
        return ParticleKind.FIRE;
    }

    private void line(GuiGraphics graphics, Component component, int x, int y, int maxWidth, int color) {
        String value = component.getString();
        if (font.width(value) > maxWidth) value = font.plainSubstrByWidth(value, maxWidth - font.width("…")) + "…";
        graphics.drawString(font, value, x, y, color, false);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        line(graphics, title, 14, 10, 240, GOLD);
        line(graphics, text("choose_particle"), 198, 31, 150, DIM);
        graphics.drawCenteredString(font, text("weapon_slot"), 58, 32, DIM);
        graphics.drawCenteredString(font, text("bottle_slot"), 126, 32, DIM);
        ParticleData data = ParticleRules.data(menu.weapon());
        line(graphics, menu.weapon().isEmpty() ? text("place_weapon") : menu.weapon().getHoverName(), 16, 82, 160, TEXT);
        line(graphics, text("capacity", data.tier(), data.total(), data.capacity()), 16, 99, 160, TEAL);
        line(graphics, text("damage", String.format(java.util.Locale.ROOT, "%.1f", data.total() * ParticleRules.DAMAGE_PER_PARTICLE)), 16, 126, 160, TEXT);
        line(graphics, text("selected", Component.translatable(selected.key()), data.count(selected)), 16, 138, 160, selected.color);
        line(graphics, text("resonance", ParticleRules.resonance(data.count(selected))), 16, 150, 160, DIM);
        line(graphics, playerInventoryTitle, 16, inventoryLabelY, 160, DIM);
        int held = menu.count(core ? ModItems.PARTICLE_CORE.get() : selected.material());
        line(graphics, text("stock", held), 220, 161, 128, held > 0 ? TEXT : 0xE58E7A);
        line(graphics, text("inject_brief", ParticleRules.injectionCost(data.count(selected)), ParticleRules.successPercent(data.count(selected))), 198, 176, 150, GOLD);
        Component status = text(confirmClear ? "clear_confirm_hint" : menu.result() == 0 ? "hint" : "result." + menu.result());
        line(graphics, status, 14, 259, 334, menu.result() == 9 ? 0xE58E7A : DIM);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose(); graphics.pose().scale(uiScale, uiScale, 1);
        int x = (int) (mouseX / uiScale), y = (int) (mouseY / uiScale);
        super.render(graphics, x, y, partialTick);
        graphics.pose().popPose();
        // 工具提示使用窗口坐标，GuiGraphics 的边界不随面板矩阵缩放。
        boolean keyboard = minecraft != null && minecraft.getLastInputType().isKeyboard();
        ArtButton button = tooltipButton(keyboard);
        if (button != null) {
            int tipX = keyboard ? Math.round((button.getX() + button.getWidth() / 2F) * uiScale) : mouseX;
            int tipY = keyboard ? Math.round((button.getY() + button.getHeight() / 2F) * uiScale) : mouseY;
            if (button.kind != null) renderDetail(graphics, button, tipX, tipY);
            else {
                detailOwner = null;
                graphics.renderComponentTooltip(font, button.description, tipX, tipY);
            }
            return;
        }
        detailOwner = null;
        if (x >= leftPos + 14 && x < leftPos + 348 && y >= topPos + 257 && y < topPos + 271)
            graphics.renderComponentTooltip(font, List.of(text(confirmClear ? "clear_confirm_hint" : menu.result() == 0 ? "hint" : "result." + menu.result())), mouseX, mouseY);
        else renderTooltip(graphics, mouseX, mouseY);
    }

    private ArtButton tooltipButton(boolean keyboard) {
        if (keyboard) return getFocused() instanceof ArtButton button && button.visible && button.description != null ? button : null;
        for (var child : children()) {
            // isMouseOver 会排除禁用按钮，但禁用原因仍需允许悬停查看。
            if (child instanceof ArtButton button && button.visible && button.isHovered() && button.description != null) return button;
        }
        return null;
    }

    private void renderDetail(GuiGraphics graphics, ArtButton button, int x, int y) {
        if (detailOwner != button || detailSource != button.description) detailPage = 0;
        detailOwner = button; detailSource = button.description;
        int maxWidth = ParticleTooltipPage.width(graphics.guiWidth());
        List<FormattedCharSequence> title = font.split(button.description.getFirst(), maxWidth);
        List<FormattedCharSequence> body = new ArrayList<>();
        for (Component line : button.description.subList(1, button.description.size())) body.addAll(font.split(line, maxWidth));
        int footerRows = font.split(text("detail_page", 999, 999), maxWidth).size();
        var page = ParticleTooltipPage.of(body.size(), graphics.guiHeight(), title.size(), footerRows, detailPage);
        detailPage = page.index(); detailPages = page.count();
        List<FormattedCharSequence> visible = new ArrayList<>(title);
        visible.addAll(body.subList(page.start(), page.end()));
        if (detailPages > 1) visible.addAll(font.split(text("detail_page", detailPage + 1, detailPages).withStyle(net.minecraft.ChatFormatting.DARK_GRAY), maxWidth));
        graphics.renderTooltip(font, visible, x, y);
    }

    @Override public boolean mouseClicked(double x, double y, int button) { refresh(); return super.mouseClicked(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x / uiScale, y / uiScale); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (detailOwner != null && detailOwner.visible && detailOwner.isMouseOver(x / uiScale, y / uiScale) && vertical != 0 && detailPages > 1) {
            detailPage = Math.max(0, Math.min(detailPages - 1, detailPage + (vertical < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(x / uiScale, y / uiScale, horizontal, vertical);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        refresh();
        if ((key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN) && detailOwner != null
                && detailOwner.visible && getFocused() == detailOwner && detailPages > 1) {
            detailPage = Math.max(0, Math.min(detailPages - 1, detailPage + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1)));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private final class ArtButton extends Button {
        private final BooleanSupplier selectedState;
        private final ItemStack icon;
        private final ParticleKind kind;
        private List<Component> description;
        private ArtButton(int x, int y, int width, int height, Component message, OnPress press,
                          BooleanSupplier selectedState, ItemStack icon, ParticleKind kind) {
            super(x, y, width, height, message, press, DEFAULT_NARRATION);
            this.selectedState = selectedState; this.icon = icon; this.kind = kind;
            if (kind == null) description = List.of(message);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = selectedState.getAsBoolean(), hover = isHoveredOrFocused();
            int edge = !active ? 0xFF35444F : chosen ? 0xFFC3A575 : hover ? 0xFF71BEB9 : 0xFF405665;
            graphics.fillGradient(getX(), getY(), getX() + width, getY() + height, chosen ? 0xFF33494E : 0xFF223341, 0xFF111E28);
            graphics.renderOutline(getX(), getY(), width, height, edge);
            if (chosen) graphics.fill(getX() + 1, getY() + 1, getX() + 3, getY() + height - 1, edge);
            if (icon.isEmpty()) {
                line(graphics, getMessage(), getX() + 6, getY() + (height - font.lineHeight) / 2, width - 12, active ? chosen ? GOLD : TEXT : 0x758795);
            } else {
                graphics.renderItem(icon, getX() + 5, getY() + 4);
                line(graphics, getMessage(), getX() + 25, getY() + 3, width - 28, kind.color);
                line(graphics, text("kind_count", ParticleRules.data(menu.weapon()).count(kind)), getX() + 25, getY() + 13, width - 29, DIM);
            }
        }
    }
}
