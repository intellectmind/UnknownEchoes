package cn.kurt6.unknown_echoes.world;

public final class RealmTimeCycle {
    public static final long REALM_PERIOD_TICKS = 24000L;
    public static final long DEEP_PERIOD_TICKS = 36000L;

    private RealmTimeCycle() { }

    public static double dusk(long dayTime, boolean deep) {
        long period = deep ? DEEP_PERIOD_TICKS : REALM_PERIOD_TICKS;
        double phase = Math.floorMod(dayTime, period) / (double) period;
        return (1.0D + Math.cos(phase * Math.PI * 2.0D)) * 0.5D;
    }

    public static double visualTime(long dayTime, boolean deep) {
        double amplitude = deep ? 3500.0D : 5000.0D;
        return 18000.0D - amplitude * Math.cos(
                Math.floorMod(dayTime, deep ? DEEP_PERIOD_TICKS : REALM_PERIOD_TICKS)
                        * Math.PI * 2.0D / (deep ? DEEP_PERIOD_TICKS : REALM_PERIOD_TICKS));
    }

    // 服务端天光与客户端日月使用同一角度，任何时刻都不能经过正午。
    public static float angle(long dayTime, boolean deep) {
        double fraction = visualTime(dayTime, deep) / 24000.0D - 0.25D;
        double eased = 0.5D - Math.cos(fraction * Math.PI) * 0.5D;
        return (float) ((fraction * 2.0D + eased) / 3.0D);
    }

    public static float ambientLift(long dayTime, boolean deep, int blockLight) {
        float unlit = 1.0F - Math.clamp(blockLight, 0, 15) / 15.0F;
        return (float) dusk(dayTime, deep) * (deep ? 0.045F : 0.025F) * unlit * unlit;
    }
}
