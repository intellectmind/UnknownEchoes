package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.network.JournalSyncPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientJournalCache {
    private static final Set<String> STRUCTURES = new HashSet<>();
    private static final Set<String> BIOMES = new HashSet<>();
    private static final Set<String> MOBS = new HashSet<>();
    private static final Set<String> BOSSES = new HashSet<>();
    private static final List<Integer> PAGES = new ArrayList<>();
    private static final List<ExplorationClue> CLUES = new ArrayList<>();
    private static final Set<String> STRUCTURES_VIEW = Collections.unmodifiableSet(STRUCTURES);
    private static final Set<String> BIOMES_VIEW = Collections.unmodifiableSet(BIOMES);
    private static final Set<String> MOBS_VIEW = Collections.unmodifiableSet(MOBS);
    private static final Set<String> BOSSES_VIEW = Collections.unmodifiableSet(BOSSES);
    private static final List<Integer> PAGES_VIEW = Collections.unmodifiableList(PAGES);
    private static final List<ExplorationClue> CLUES_VIEW = Collections.unmodifiableList(CLUES);
    private static int totalStructures;
    private static int totalBiomes;
    private static int totalMobs;
    private static int totalBosses;
    private static int knownPages;
    private static ExplorationClue destination;

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
        CLUES.clear();
        for (String encoded : payload.clues()) {
            ExplorationClue clue = ExplorationClue.decode(encoded);
            if (clue != null) {
                CLUES.add(clue);
            }
        }
        ExplorationClue selected = ExplorationClue.decode(payload.destination());
        destination = selected == null ? null : CLUES.stream().filter(selected::sameSite).findFirst().orElse(null);
        List<Integer> totals = payload.totals();
        totalStructures = totals.size() > 0 ? totals.get(0) : 0;
        totalBiomes = totals.size() > 1 ? totals.get(1) : 0;
        totalMobs = totals.size() > 2 ? totals.get(2) : 0;
        totalBosses = totals.size() > 3 ? totals.get(3) : 0;
        knownPages = totals.size() > 4 ? totals.get(4) : 0;
    }

    public static Set<String> structures() {
        return STRUCTURES_VIEW;
    }

    public static Set<String> biomes() {
        return BIOMES_VIEW;
    }

    public static Set<String> mobs() {
        return MOBS_VIEW;
    }

    public static Set<String> bosses() {
        return BOSSES_VIEW;
    }

    public static List<Integer> pages() {
        return PAGES_VIEW;
    }

    public static List<ExplorationClue> clues() {
        return CLUES_VIEW;
    }

    public static ExplorationClue destination() {
        return destination;
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
        CLUES.clear();
        destination = null;
        totalStructures = totalBiomes = totalMobs = totalBosses = knownPages = 0;
    }
}
