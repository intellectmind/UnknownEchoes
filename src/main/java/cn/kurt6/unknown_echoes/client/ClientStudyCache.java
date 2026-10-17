package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog;

public final class ClientStudyCache {
    private static int earned;
    private static int claimed;
    private ClientStudyCache() { }

    public static void update(int earnedMask, int claimedMask) {
        earned = earnedMask & WorkshopStudyCatalog.ALL;
        claimed = claimedMask & earned;
    }
    public static int rating(int theme) { return Integer.bitCount(earned & WorkshopStudyCatalog.themeMask(theme)); }
    public static int pending(int theme) { return Integer.bitCount(earned & ~claimed & WorkshopStudyCatalog.themeMask(theme)); }
    public static void clear() { earned = 0; claimed = 0; }
}
