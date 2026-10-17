package cn.kurt6.unknown_echoes.study;

import java.util.List;
import java.util.Set;

public final class WorkshopStudyCatalog {
    public static final int THEMES = 7;
    public static final int PRESETS = 4;
    public static final int ALL = (1 << 21) - 1;
    public static final List<String> IDS = List.of("echo_world_tree", "eternal_echo_lighthouse", "mirror_sea",
            "inverted_mountains", "sky_rift", "silent_great_boat", "broken_bell_tower");
    private static final List<String> SUPPLIES = List.of("seeker_spore", "tide_breath_bottle", "revealing_powder",
            "wind_chime_bottle", "echo_decoy", "tide_breath_bottle", "chime_burst");
    private static final int[][][] STARTS = {
            {{3,3,2}, {0,5,3}, {3,0,5}, {0,3,5}},
            {{2,2,3}, {2,5,0}, {2,3,2}, {2,0,5}},
            {{0,1,0,0,0,0,2,0,0}, {1,1,1,2,0,0,2,0,2}, {2,1,0,2,0,0,2,2,0}, {1,1,1,0,1,0,2,0,0}},
            {{0,0,2,1}, {1,3,1,0}, {3,3,0,3}, {0,3,0,0}},
            {{1,1,0,1,0,2,1,2,2}, {1,0,1,0,1,0,0,0,0}, {1,0,1,1,2,1,0,2,0}, {1,2,1,2,2,1,1,2,1}},
            {{2,1,0,0}, {1,2,3,1}, {1,1,3,2}, {3,1,0,3}},
            {{3,4,0,3}, {5,7,0,1}, {5,3,7,3}, {1,0,7,0}}
    };
    private static final int[][] OPTIMAL = {{5,6,7,6}, {4,6,4,6}, {6,6,6,6}, {6,4,4,4},
            {6,6,7,6}, {4,4,4,4}, {12,9,10,10}};

    public record Reward(String item, int count) { }

    private WorkshopStudyCatalog() { }

    public static boolean valid(int theme, int preset) {
        return theme >= 0 && theme < THEMES && preset >= 0 && preset < PRESETS;
    }

    public static int[] initial(int theme, int preset) { return STARTS[theme][preset].clone(); }
    public static int optimal(int theme, int preset) { return OPTIMAL[theme][preset]; }
    public static int bit(int theme, int tier) { return 1 << (theme * 3 + tier); }
    public static int themeMask(int theme) { return 7 << (theme * 3); }

    public static String key(int theme, int tier, boolean claimed) {
        return "study:" + (claimed ? "claim:" : "earned:") + IDS.get(theme) + ":" + tier;
    }

    public static int mask(Set<String> records, boolean claimed) {
        int mask = 0;
        for (int theme = 0; theme < THEMES; theme++) {
            for (int tier = 0; tier < 3; tier++) if (records.contains(key(theme, tier, claimed))) mask |= bit(theme, tier);
        }
        return mask;
    }

    public static Reward reward(int theme, int tier) {
        return switch (tier) {
            case 0 -> new Reward("record_tracing_paper", 2);
            case 1 -> new Reward(SUPPLIES.get(theme), 2);
            case 2 -> new Reward("attuned_echo_gem", 1);
            default -> throw new IllegalArgumentException("Unknown study tier");
        };
    }
    public static Reward particleReward(java.util.UUID player, int theme) {
        int roll = new java.util.Random(player.getMostSignificantBits() ^ player.getLeastSignificantBits() ^ (theme * 0x9e3779b97f4a7c15L)).nextInt(14);
        String[] weighted = {"fire", "fire", "fire", "venom", "venom", "venom", "earth", "earth", "earth",
                "frost", "thunder", "void", "holy", "shadow"};
        return new Reward(weighted[roll] + "_particle_crystal", 1);
    }
}
