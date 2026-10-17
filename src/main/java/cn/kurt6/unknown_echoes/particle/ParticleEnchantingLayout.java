package cn.kurt6.unknown_echoes.particle;

public final class ParticleEnchantingLayout {
    public static final int WIDTH = 360, HEIGHT = 272;
    public static final int WEAPON_X = 50, WEAPON_Y = 49, BOTTLE_X = 118, BOTTLE_Y = 49;
    public static final int INVENTORY_X = 16, INVENTORY_Y = 176, HOTBAR_Y = 234;
    public static final int CONTROL_WIDTH = 72, CONTROL_HEIGHT = 18, SOURCE_Y = 188;
    private ParticleEnchantingLayout() { }

    public static float scale(int width, int height) {
        return Math.max(.1F, Math.min(1, Math.min(width / (float) (WIDTH + 8), height / (float) (HEIGHT + 8))));
    }
    public static int kindX(int ordinal) { return 198 + ordinal % 2 * 78; }
    public static int kindY(int ordinal) { return 44 + ordinal / 2 * 27; }
    public static int operationX(int index) { return 198 + index % 2 * 78; }
    public static int operationY(int index) { return 212 + index / 2 * 22; }
}
