package cn.kurt6.unknown_echoes.study;

import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleLogic;
import java.util.Arrays;
import java.util.UUID;

public final class WorkshopStudySession {
    public static final int MOVE = 0, RESET = 1, NEXT = 2, CLAIM = 3, CLOSE = 4;
    private final UUID id = UUID.randomUUID();
    private final int theme;
    private int preset;
    private int revision;
    private int moves;
    private int[] board;
    private long lastAction = Long.MIN_VALUE;
    private long lastRequest = Long.MIN_VALUE;

    public WorkshopStudySession(int theme, int preset) {
        if (!WorkshopStudyCatalog.valid(theme, preset)) throw new IllegalArgumentException("Unknown study board");
        this.theme = theme;
        this.preset = preset;
        board = WorkshopStudyCatalog.initial(theme, preset);
    }

    public UUID id() { return id; }
    public int theme() { return theme; }
    public int preset() { return preset; }
    public int revision() { return revision; }
    public int moves() { return moves; }
    public int[] board() { return board.clone(); }
    public boolean solved() { return WonderPuzzleLogic.solved(theme, board); }

    public int earnedMask() {
        if (!solved()) return 0;
        int optimal = WorkshopStudyCatalog.optimal(theme, preset);
        int tiers = moves <= optimal ? 7 : moves <= optimal + 2 ? 3 : 1;
        return tiers << (theme * 3);
    }

    /** 过期版本也需要回包纠正，但同一 tick 只校验和同步一次。 */
    public boolean allowRequest(long tick) {
        if (tick == lastRequest) return false;
        lastRequest = tick;
        return true;
    }

    public boolean accept(UUID nonce, int expectedRevision, long tick) {
        if (!id.equals(nonce) || revision != expectedRevision || tick == lastAction) return false;
        lastAction = tick;
        return true;
    }

    public boolean apply(int operation, int first, int second) {
        if (revision >= Integer.MAX_VALUE - 1) return false;
        if (operation == RESET || operation == NEXT) {
            if (operation == NEXT) preset = (preset + 1) % WorkshopStudyCatalog.PRESETS;
            board = WorkshopStudyCatalog.initial(theme, preset);
            moves = 0;
        } else if (operation == MOVE && !solved() && moves < 10000) {
            if (WonderPuzzleLogic.mode(theme) != WonderPuzzleLogic.WATER && second != 0 && second != 1) return false;
            int[] next = WonderPuzzleLogic.mode(theme) == WonderPuzzleLogic.WATER
                    ? WonderPuzzleLogic.pour(theme, board, first, second)
                    : WonderPuzzleLogic.turn(theme, board, first, second == 1);
            if (Arrays.equals(board, next)) return false;
            board = next;
            moves++;
        } else return false;
        revision++;
        return true;
    }
}
