package cn.kurt6.unknown_echoes.client.gui;

import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleLogic;
import cn.kurt6.unknown_echoes.client.ClientStudyCache;
import cn.kurt6.unknown_echoes.network.StudyActionPayload;
import cn.kurt6.unknown_echoes.network.StudySyncPayload;
import cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog;
import cn.kurt6.unknown_echoes.study.WorkshopStudySession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WorkshopStudyScreen extends Screen {
    private record Line(FormattedCharSequence text, int y, int color) { }
    private StudySyncPayload state;
    private final List<Line> lines = new ArrayList<>();
    private int scroll, contentHeight, selected = -1;
    private boolean waiting, reverse, serverClosed, claiming;
    private Component claimFeedback;

    private WorkshopStudyScreen(StudySyncPayload state) {
        super(Component.translatable("study.unknown_echoes.title"));
        this.state = state;
    }

    public static void accept(StudySyncPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientStudyCache.update(payload.earnedMask(), payload.claimedMask());
        if (payload.details().isEmpty()) {
            if (minecraft.screen instanceof WorkshopStudyScreen screen && screen.state.session().equals(payload.session())) {
                screen.serverClosed = true;
                minecraft.setScreen(null);
            }
            return;
        }
        if (payload.details().size() != 5 || !WorkshopStudyCatalog.valid(payload.details().get(0), payload.details().get(1))
                || !WonderPuzzleLogic.valid(payload.details().get(0), payload.board().stream().mapToInt(Integer::intValue).toArray())) return;
        if (minecraft.screen instanceof WorkshopStudyScreen screen && screen.state.session().equals(payload.session())) {
            if (payload.revision() < screen.state.revision()) return;
            if (screen.claiming) {
                screen.claimFeedback = Component.translatable(payload.claimedMask() == screen.state.claimedMask()
                        ? "study.unknown_echoes.claim_retry" : "study.unknown_echoes.claim_done");
                screen.claiming = false;
            }
            screen.state = payload;
            screen.waiting = false;
            screen.selected = -1;
            screen.rebuild();
        } else if (payload.details().get(4) == 1) minecraft.setScreen(new WorkshopStudyScreen(payload));
    }

    private int panelWidth() { return Math.max(80, Math.min(440, width - 16)); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return 30; }
    private int bottom() { return Math.max(top() + 20, height - 28); }
    private int maxScroll() { return Math.max(0, contentHeight - (bottom() - top())); }
    private int theme() { return state.details().getFirst(); }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        lines.clear();
        buildContent();
        int clamped = Mth.clamp(scroll, 0, maxScroll());
        if (scroll != clamped) {
            scroll = clamped;
            clearWidgets();
            lines.clear();
            buildContent();
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 40, height - 24, 80, 20).build());
    }

    private int text(Component value, int y, int color) {
        for (var line : font.split(value, panelWidth() - 8)) {
            lines.add(new Line(line, y, color));
            y += 11;
        }
        return y + 4;
    }

    private void button(Component label, int x, int y, int w, boolean enabled, Runnable action) {
        int screenY = top() + y - scroll;
        Button button = Button.builder(label, ignored -> action.run()).bounds(x, screenY, w, 20).build();
        button.active = enabled && !waiting;
        button.visible = screenY >= top() && screenY + 20 <= bottom();
        addRenderableWidget(button);
    }

    private void buildContent() {
        int theme = theme();
        int[] board = state.board().stream().mapToInt(Integer::intValue).toArray();
        int mode = WonderPuzzleLogic.mode(theme);
        int y = text(Component.translatable("study.unknown_echoes.heading",
                Component.translatable("structure.unknown_echoes." + WorkshopStudyCatalog.IDS.get(theme)),
                state.details().get(1) + 1), 0, 0xEBD79B);
        y = text(Component.translatable("study.unknown_echoes.rule." + mode), y, 0xCDD9DC);
        int[] target = WonderPuzzleLogic.target(theme);
        Component goal = switch (mode) {
            case WonderPuzzleLogic.WATER -> Component.translatable("study.unknown_echoes.water_goal", Arrays.toString(target));
            case WonderPuzzleLogic.OPTICS -> Component.translatable("study.unknown_echoes.optics_goal", receiver(target[0]), receiver(target[1]));
            case WonderPuzzleLogic.BALANCE -> Component.translatable("study.unknown_echoes.balance_goal", target[0]);
            default -> Component.translatable("study.unknown_echoes.gears_goal");
        };
        y = text(goal, y, 0x90DCC9);
        y = text(Component.translatable("study.unknown_echoes.moves", state.details().get(2), state.details().get(3),
                state.details().get(3) + 2), y, 0xEBD79B);
        boolean solved = WonderPuzzleLogic.solved(theme, board);
        if (solved) y = text(Component.translatable("study.unknown_echoes.solved"), y, 0x80EEAD);
        if (mode == WonderPuzzleLogic.WATER) {
            y = text(selected < 0 ? Component.translatable("study.unknown_echoes.pick_source")
                    : Component.translatable("study.unknown_echoes.pick_target", selected + 1), y, 0xCDD9DC);
        } else {
            y = text(Component.translatable("study.unknown_echoes.turn_hint"), y, 0xCDD9DC);
            button(Component.translatable(reverse ? "study.unknown_echoes.reverse" : "study.unknown_echoes.forward"),
                    left(), y, panelWidth(), !solved, () -> { reverse = !reverse; rebuild(); });
            y += 24;
        }
        int columns = mode == WonderPuzzleLogic.OPTICS || mode == WonderPuzzleLogic.WATER ? 3 : 2;
        int cellWidth = (panelWidth() - (columns - 1) * 4) / columns;
        boolean[] lit = mode == WonderPuzzleLogic.OPTICS ? WonderPuzzleLogic.illuminated(board) : new boolean[board.length];
        for (int i = 0; i < board.length; i++) {
            int index = i;
            String value = switch (mode) {
                case WonderPuzzleLogic.WATER -> board[i] + "/" + WonderPuzzleLogic.capacities(theme)[i];
                case WonderPuzzleLogic.OPTICS -> (board[i] == 0 ? "·" : board[i] == 1 ? "/" : "\\") + (lit[i] ? " *" : "");
                case WonderPuzzleLogic.BALANCE -> WonderPuzzleLogic.weights(theme)[i] + " × " + new int[]{-2,-1,1,2}[board[i]];
                default -> Integer.toString(board[i]);
            };
            Component label = Component.literal((i == selected ? "> " : "") + (i + 1) + ": " + value);
            button(label, left() + (i % columns) * (cellWidth + 4), y + (i / columns) * 24, cellWidth, !solved, () -> {
                if (mode == WonderPuzzleLogic.WATER) {
                    if (selected == index) { selected = -1; rebuild(); }
                    else if (selected < 0) { selected = index; rebuild(); }
                    else send(WorkshopStudySession.MOVE, selected, index);
                } else send(WorkshopStudySession.MOVE, index, reverse || hasShiftDown() ? 1 : 0);
            });
        }
        y += ((board.length + columns - 1) / columns) * 24 + 3;
        if (mode == WonderPuzzleLogic.OPTICS) {
            y = text(Component.translatable("study.unknown_echoes.optics_status", receiver(WonderPuzzleLogic.trace(board, 0).exit()),
                    receiver(WonderPuzzleLogic.trace(board, 2).exit()), WonderPuzzleLogic.illuminatedCount(board)), y, 0x90DCC9);
        } else if (mode == WonderPuzzleLogic.BALANCE) {
            int[] moments = WonderPuzzleLogic.moments(theme, board);
            y = text(Component.translatable("study.unknown_echoes.balance_status", moments[0], moments[1]), y, 0x90DCC9);
        }
        int half = (panelWidth() - 4) / 2;
        button(Component.translatable("study.unknown_echoes.reset"), left(), y, half, true,
                () -> send(WorkshopStudySession.RESET, 0, 0));
        button(Component.translatable("study.unknown_echoes.next"), left() + half + 4, y, half, true,
                () -> send(WorkshopStudySession.NEXT, 0, 0));
        y += 27;
        y = text(Component.translatable("study.unknown_echoes.rewards_hint"), y, 0xCDD9DC);
        for (int tier = 0; tier < 3; tier++) {
            var reward = WorkshopStudyCatalog.reward(theme, tier);
            int bit = WorkshopStudyCatalog.bit(theme, tier);
            String status = (state.claimedMask() & bit) != 0 ? "claimed" : (state.earnedMask() & bit) != 0 ? "ready" : "locked";
            y = text(Component.translatable("study.unknown_echoes.reward_line",
                    Component.translatable("study.unknown_echoes.tier." + tier),
                    BuiltInRegistries.ITEM.get(UnknownEchoes.id(reward.item())).getDescription(), reward.count(),
                    Component.translatable("study.unknown_echoes." + status)), y, 0xCDD9DC);
        }
        if (minecraft != null && minecraft.player != null) {
            var particle = WorkshopStudyCatalog.particleReward(minecraft.player.getUUID(), theme);
            y = text(Component.translatable("study.unknown_echoes.particle_reward",
                    BuiltInRegistries.ITEM.get(UnknownEchoes.id(particle.item())).getDescription()), y, 0xCDD9DC);
        }
        button(Component.translatable("study.unknown_echoes.claim"), left(), y, panelWidth(),
                (state.earnedMask() & ~state.claimedMask() & WorkshopStudyCatalog.themeMask(theme)) != 0,
                () -> send(WorkshopStudySession.CLAIM, 0, 0));
        y += 24;
        if (claimFeedback != null) y = text(claimFeedback, y, 0xEBD79B);
        contentHeight = y;
    }

    private Component receiver(int exit) {
        if (exit < 0) return Component.translatable("study.unknown_echoes.loop");
        return Component.translatable("study.unknown_echoes.receiver", Component.translatable("study.unknown_echoes.side." + exit / 3), exit % 3 + 1);
    }

    private void send(int operation, int first, int second) {
        if (waiting) return;
        waiting = true;
        claiming = operation == WorkshopStudySession.CLAIM;
        claimFeedback = null;
        PacketDistributor.sendToServer(new StudyActionPayload(state.session(), state.revision(), operation, first, second));
        rebuild();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xEE101B24);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xEBD79B);
        graphics.enableScissor(left(), top(), left() + panelWidth(), bottom());
        for (Line line : lines) graphics.drawString(font, line.text(), left() + 2, top() + line.y() - scroll, line.color(), false);
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (maxScroll() > 0) {
            int travel = bottom() - top() - 10;
            int y = top() + travel * scroll / maxScroll();
            graphics.fill(left() + panelWidth() + 2, top(), left() + panelWidth() + 4, bottom(), 0xFF394B57);
            graphics.fill(left() + panelWidth() + 2, y, left() + panelWidth() + 4, y + 10, 0xFF90DCC9);
        }
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Mth.clamp(scroll - (int) (vertical * 24), 0, maxScroll());
        rebuild();
        return true;
    }

    @Override public void removed() {
        if (!serverClosed && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new StudyActionPayload(state.session(), state.revision(), WorkshopStudySession.CLOSE, 0, 0));
        }
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }
}
