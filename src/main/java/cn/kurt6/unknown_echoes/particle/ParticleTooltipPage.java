package cn.kurt6.unknown_echoes.particle;

public record ParticleTooltipPage(int index, int count, int start, int end) {
    public static int width(int windowWidth) { return Math.max(1, Math.min(320, windowWidth - 24)); }

    public static ParticleTooltipPage of(int lines, int windowHeight, int headerLines, int footerLines, int requested) {
        // 原版文本工具提示每行占 10 像素，另给边框和窗口边缘留空。
        int rows = Math.max(1, (windowHeight - 24) / 10 - headerLines - footerLines);
        int count = Math.max(1, (lines + rows - 1) / rows);
        int index = Math.max(0, Math.min(count - 1, requested));
        return new ParticleTooltipPage(index, count, index * rows, Math.min(lines, (index + 1) * rows));
    }
}
