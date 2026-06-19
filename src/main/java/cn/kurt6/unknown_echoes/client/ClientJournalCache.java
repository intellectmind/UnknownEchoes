package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.JournalSyncPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户端探索日志缓存:书本 UI 的数据源(服务端 JournalSyncPayload 同步)。
 * 纯展示;真实进度永远以服务端 Data Attachment 为准。
 */
public class ClientJournalCache {
    private static final Set<String> STRUCTURES = new HashSet<>();
    private static final Set<String> BIOMES = new HashSet<>();
    private static final Set<String> MOBS = new HashSet<>();
    private static final Set<String> BOSSES = new HashSet<>();
    private static final List<Integer> PAGES = new ArrayList<>();
    private static int totalStructures;
    private static int totalBiomes;
    private static int totalMobs;
    private static int totalBosses;
    private static int knownPages;

    public static void update(JournalSyncPayload payload) {
        STRUCTURES.clear();
        STRUCTURES.addAll(payload.structures());
        BIOMES.clear();
        BIOMES.addAll(payload.biomes());
        MOBS.clear();
        MOBS.addAll(payload.mobs());
        BOSSES.clear();
        BOSSES.addAll(payload.bosses());
        PAGES.clear();
        PAGES.addAll(payload.pages());
        PAGES.sort(Integer::compareTo);
        List<Integer> totals = payload.totals();
        totalStructures = totals.size() > 0 ? totals.get(0) : 0;
        totalBiomes = totals.size() > 1 ? totals.get(1) : 0;
        totalMobs = totals.size() > 2 ? totals.get(2) : 0;
        totalBosses = totals.size() > 3 ? totals.get(3) : 0;
        knownPages = totals.size() > 4 ? totals.get(4) : 0;
    }

    public static Set<String> structures() {
        return STRUCTURES;
    }

    public static Set<String> biomes() {
        return BIOMES;
    }

    public static Set<String> mobs() {
        return MOBS;
    }

    public static Set<String> bosses() {
        return BOSSES;
    }

    public static List<Integer> pages() {
        return PAGES;
    }

    public static int totalStructures() {
        return totalStructures;
    }

    public static int totalBiomes() {
        return totalBiomes;
    }

    public static int totalMobs() {
        return totalMobs;
    }

    public static int totalBosses() {
        return totalBosses;
    }

    public static int knownPages() {
        return knownPages;
    }

    public static void clear() {
        STRUCTURES.clear();
        BIOMES.clear();
        MOBS.clear();
        BOSSES.clear();
        PAGES.clear();
        totalStructures = totalBiomes = totalMobs = totalBosses = knownPages = 0;
    }
}
