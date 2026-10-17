package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.block.puzzle.EchoChaseCatalog;

public final class ClientChaseCache {
    private static int earned;
    private static int claimed;
    private ClientChaseCache() { }
    public static void update(int earnedMask, int claimedMask) {
        earned = earnedMask & EchoChaseCatalog.ALL;
        claimed = claimedMask & earned;
    }
    public static boolean completed(int theme) { return (earned & EchoChaseCatalog.bit(theme)) != 0; }
    public static boolean pending(int theme) { return (earned & ~claimed & EchoChaseCatalog.bit(theme)) != 0; }
    public static String status(int theme) { return EchoChaseCatalog.status(theme, earned, claimed); }
    public static void clear() { earned = 0; claimed = 0; }
}
