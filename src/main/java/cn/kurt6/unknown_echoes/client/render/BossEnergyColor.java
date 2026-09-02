package cn.kurt6.unknown_echoes.client.render;

/**
 * 为 additive energy_swirl 统一处理能量层颜色。
 *
 * energy_swirl 使用 ONE, ONE 混合，alpha 只用于纹理裁剪，不能拿来控制亮度。
 * 这里把攻击包络映射到 RGB 强度，并把主题色的最高通道抬到可见下限，保留色相。
 */
final class BossEnergyColor {
    private static final int MIN_VALUE = 235;

    private BossEnergyColor() {
    }

    static int withIntensity(int rgb, float intensity) {
        int lifted = liftValue(rgb);
        float strength = Math.max(0.0F, Math.min(1.0F, intensity));
        int red = Math.round(((lifted >> 16) & 0xFF) * strength);
        int green = Math.round(((lifted >> 8) & 0xFF) * strength);
        int blue = Math.round((lifted & 0xFF) * strength);
        // 保持 alpha 足够通过 energy_swirl 的 discard，实际亮度完全由 RGB 控制。
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int liftValue(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int peak = Math.max(red, Math.max(green, blue));
        if (peak <= 0 || peak >= MIN_VALUE) {
            return rgb & 0xFFFFFF;
        }
        float scale = (float) MIN_VALUE / peak;
        red = Math.min(255, Math.round(red * scale));
        green = Math.min(255, Math.round(green * scale));
        blue = Math.min(255, Math.round(blue * scale));
        return (red << 16) | (green << 8) | blue;
    }
}
