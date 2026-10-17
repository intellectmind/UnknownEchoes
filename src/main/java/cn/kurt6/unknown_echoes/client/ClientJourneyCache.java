package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.journal.JourneyCatalog;
import cn.kurt6.unknown_echoes.network.JourneySyncPayload;

import java.util.List;

public final class ClientJourneyCache {
    private static List<Integer> values = List.of();
    private static int claimedMask;

    private ClientJourneyCache() { }

    public static void update(JourneySyncPayload payload) {
        values = List.copyOf(payload.values());
        claimedMask = payload.claimedMask() & ((1 << JourneyCatalog.CHAPTERS.size()) - 1);
    }

    public static List<Integer> values() { return values; }
    public static int claimedCount() { return Integer.bitCount(claimedMask); }
    public static int readyCount() { return Integer.bitCount(JourneyCatalog.readyMask(values, claimedMask)); }
    public static boolean claimed(int index) { return (claimedMask & (1 << index)) != 0; }
    public static boolean ready(int index) {
        return JourneyCatalog.chapter(index) != null && !claimed(index) && JourneyCatalog.chapter(index).complete(values);
    }

    public static void clear() { values = List.of(); claimedMask = 0; }
}
