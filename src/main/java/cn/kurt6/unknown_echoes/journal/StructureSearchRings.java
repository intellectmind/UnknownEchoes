package cn.kurt6.unknown_echoes.journal;

/** 可暂停的方形环周界游标，按行遍历边界而不扫描环内格。 */
final class StructureSearchRings {
    static final int CHECKS_PER_STEP = 128;

    private final int targetCount;
    private int ring;
    private int point;
    private int targetIndex;

    StructureSearchRings(int targetCount) {
        if (targetCount <= 0) throw new IllegalArgumentException("No search targets");
        this.targetCount = targetCount;
    }

    int ring() { return ring; }
    int targetIndex() { return targetIndex; }
    boolean atRingStart() { return point == 0 && targetIndex == 0; }

    int offsetX() {
        if (ring == 0) return 0;
        if (point <= 2 * ring) return point - ring;
        int side = point - (2 * ring + 1);
        if (side < 4 * ring - 2) return side % 2 == 0 ? -ring : ring;
        return side - (4 * ring - 2) - ring;
    }

    int offsetZ() {
        if (ring == 0) return 0;
        if (point <= 2 * ring) return -ring;
        int side = point - (2 * ring + 1);
        return side < 4 * ring - 2 ? -ring + 1 + side / 2 : ring;
    }

    void advance() {
        if (++targetIndex < targetCount) return;
        targetIndex = 0;
        if (++point < (ring == 0 ? 1 : 8 * ring)) return;
        point = 0;
        ring++;
    }
}
