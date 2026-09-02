package cn.kurt6.unknown_echoes.world.structure;

/**
 * 矩形建筑 helper 的半宽 / 半深(局部坐标格数):halfWidth 沿 X,halfDepth 沿 Z。
 *
 * 0 合法,表示该轴只有一格(贴墙直檐这类构件要用)。负数会让 minX > maxX,
 * 循环一次不跑、建筑整块消失且无报错,所以在构造器拦掉。
 */
record Footprint(int halfWidth, int halfDepth) {

    Footprint {
        if (halfWidth < 0 || halfDepth < 0) {
            throw new IllegalArgumentException(
                    "footprint half-extents must be >= 0, got " + halfWidth + "x" + halfDepth);
        }
    }

    static Footprint of(int halfWidth, int halfDepth) {
        return new Footprint(halfWidth, halfDepth);
    }

    /** 正方形足印:两轴同半径。 */
    static Footprint square(int half) {
        return new Footprint(half, half);
    }

    int minX(int centerX) {
        return centerX - halfWidth;
    }

    int maxX(int centerX) {
        return centerX + halfWidth;
    }

    int minZ(int centerZ) {
        return centerZ - halfDepth;
    }

    int maxZ(int centerZ) {
        return centerZ + halfDepth;
    }
}
