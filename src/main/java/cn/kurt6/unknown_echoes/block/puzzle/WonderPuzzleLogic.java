package cn.kurt6.unknown_echoes.block.puzzle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WonderPuzzleLogic {
    public static final int WATER = 0;
    public static final int OPTICS = 1;
    public static final int BALANCE = 2;
    public static final int GEARS = 3;
    private static final int[] ARMS = {-2, -1, 1, 2};
    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DZ = {0, 1, 0, -1};

    private WonderPuzzleLogic() { }

    public static int mode(int theme) {
        return switch (theme) {
            case 0, 1 -> WATER;
            case 2, 4 -> OPTICS;
            case 3, 5 -> BALANCE;
            default -> GEARS;
        };
    }

    public static int[] capacities(int theme) {
        return theme == 0 ? new int[]{3, 5, 8} : new int[]{2, 5, 7};
    }

    public static int[] weights(int theme) {
        return theme == 3 ? new int[]{1, 3, 5, 7} : new int[]{1, 2, 3, 4};
    }

    public static int[] target(int theme) {
        return switch (theme) {
            case 0 -> new int[]{0, 4, 4};
            case 1 -> new int[]{1, 0, 6};
            case 2 -> new int[]{10, 1};
            case 3 -> new int[]{14, 14};
            case 4 -> new int[]{4, 1};
            case 5 -> new int[]{8, 8};
            default -> new int[]{0, 0, 0, 0};
        };
    }

    public static int[] initial(int theme) {
        return switch (mode(theme)) {
            case WATER -> new int[]{0, 0, capacities(theme)[2]};
            case OPTICS -> new int[9];
            case BALANCE -> new int[]{0, 0, 0, 0};
            default -> new int[]{3, 5, 2, 6};
        };
    }

    public static boolean valid(int theme, int[] state) {
        if (theme < 0 || theme > 6 || state.length != initial(theme).length) return false;
        for (int i = 0; i < state.length; i++) {
            int max = switch (mode(theme)) {
                case WATER -> capacities(theme)[i];
                case OPTICS -> 2;
                case BALANCE -> 3;
                default -> 7;
            };
            if (state[i] < 0 || state[i] > max) return false;
        }
        return mode(theme) != WATER || Arrays.stream(state).sum() == capacities(theme)[2];
    }

    public static int[] turn(int theme, int[] state, int index, boolean reverse) {
        int[] next = state.clone();
        if (!valid(theme, state) || index < 0 || index >= next.length) return next;
        int step = reverse ? -1 : 1;
        switch (mode(theme)) {
            case OPTICS -> next[index] = Math.floorMod(next[index] + step, 3);
            case BALANCE -> next[index] = Math.floorMod(next[index] + step, 4);
            case GEARS -> {
                next[index] = Math.floorMod(next[index] + step, 8);
                next[(index + 1) % 4] = Math.floorMod(next[(index + 1) % 4] + step * 2, 8);
            }
            default -> { }
        }
        return next;
    }

    public static int[] pour(int theme, int[] state, int from, int to) {
        int[] next = state.clone();
        if (!valid(theme, state) || mode(theme) != WATER
                || from < 0 || to < 0 || from >= 3 || to >= 3 || from == to) return next;
        int amount = Math.min(next[from], capacities(theme)[to] - next[to]);
        next[from] -= amount;
        next[to] += amount;
        return next;
    }

    public static int[] moments(int theme, int[] state) {
        int left = 0;
        int right = 0;
        int[] weights = weights(theme);
        for (int i = 0; i < 4; i++) {
            int torque = weights[i] * ARMS[state[i]];
            if (torque < 0) left -= torque;
            else right += torque;
        }
        return new int[]{left, right};
    }

    public record Ray(int exit, List<int[]> path) { }

    // 局部棋盘为 3×3，东南西北四边接收口依次编号 0..11。
    public static Ray trace(int[] state, int row) {
        int x = -1;
        int z = row;
        int direction = 0;
        boolean[] seen = new boolean[36];
        List<int[]> path = new ArrayList<>();
        path.add(new int[]{x, z});
        for (int step = 0; step < 40; step++) {
            x += DX[direction];
            z += DZ[direction];
            path.add(new int[]{x, z});
            if (x < 0) return new Ray(6 + z, path);
            if (x > 2) return new Ray(z, path);
            if (z < 0) return new Ray(9 + x, path);
            if (z > 2) return new Ray(3 + x, path);
            int cell = z * 3 + x;
            int visit = cell * 4 + direction;
            if (seen[visit]) return new Ray(-1, path);
            seen[visit] = true;
            if (state[cell] == 1) direction = 3 - direction;
            else if (state[cell] == 2) direction ^= 1;
        }
        return new Ray(-1, path);
    }

    public static boolean solved(int theme, int[] state) {
        if (!valid(theme, state)) return false;
        if (mode(theme) == OPTICS && illuminatedCount(state) != 9) return false;
        int[] actual = switch (mode(theme)) {
            case OPTICS -> new int[]{trace(state, 0).exit(), trace(state, 2).exit()};
            case BALANCE -> moments(theme, state);
            default -> state;
        };
        return Arrays.equals(target(theme), actual);
    }

    public static boolean[] illuminated(int[] state) {
        boolean[] lit = new boolean[9];
        for (int row : new int[]{0, 2}) {
            for (int[] point : trace(state, row).path()) {
                if (point[0] >= 0 && point[0] < 3 && point[1] >= 0 && point[1] < 3) {
                    lit[point[1] * 3 + point[0]] = true;
                }
            }
        }
        return lit;
    }

    public static int illuminatedCount(int[] state) {
        int count = 0;
        for (boolean lit : illuminated(state)) if (lit) count++;
        return count;
    }
}
