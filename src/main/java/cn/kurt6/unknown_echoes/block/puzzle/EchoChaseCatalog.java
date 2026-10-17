package cn.kurt6.unknown_echoes.block.puzzle;

import java.util.List;
import java.util.Set;

public final class EchoChaseCatalog {
    public static final int THEMES = 7;
    public static final int ALL = (1 << THEMES) - 1;
    public static final List<String> IDS = List.of("echo_world_tree", "eternal_echo_lighthouse", "mirror_sea",
            "inverted_mountains", "sky_rift", "silent_great_boat", "broken_bell_tower");
    private static final List<String> SUPPLIES = List.of("seeker_spore", "echo_decoy", "revealing_powder",
            "wind_chime_bottle", "echo_decoy", "tide_breath_bottle", "chime_burst");

    public record Reward(String item, int count) { }
    private EchoChaseCatalog() { }

    public static boolean valid(int theme) { return theme >= 0 && theme < THEMES; }
    public static int bit(int theme) { return 1 << theme; }
    public static String key(int theme, boolean claimed) {
        return "chase:" + (claimed ? "claim:" : "earned:") + IDS.get(theme);
    }
    public static String reservation(int theme) {
        return "chase:slot:" + IDS.get(theme);
    }
    public static int mask(Set<String> records, boolean claimed) {
        int result = 0;
        for (int theme = 0; theme < THEMES; theme++) {
            if (records.contains(key(theme, claimed))) result |= bit(theme);
        }
        return result;
    }
    public static String status(int theme, int earned, int claimed) {
        return (earned & bit(theme)) == 0 ? "incomplete" : (claimed & bit(theme)) == 0 ? "pending" : "completed";
    }
    public static List<Reward> rewards(int theme) {
        if (!valid(theme)) throw new IllegalArgumentException("Unknown chase theme");
        return List.of(new Reward(SUPPLIES.get(theme), 2), new Reward("attuned_echo_gem", 1));
    }
}
