package cn.kurt6.unknown_echoes.journal;

import java.util.List;

public final class JourneyCatalog {
    public enum Metric {
        STRUCTURES, PAGES, BIOMES, MOBS, PUZZLES, PUZZLE_KINDS,
        ABILITIES, REVISITS, WONDERS, WORKSHOPS, DEEP_BIOMES, DEEP_STRUCTURES, DEEP_PAGES
    }

    public record Goal(Metric metric, int target) {
        public int current(List<Integer> values) {
            return metric.ordinal() < values.size() ? Math.max(0, values.get(metric.ordinal())) : 0;
        }

        public boolean complete(List<Integer> values) { return current(values) >= target; }
    }

    public record Reward(String item, int count) { }

    public record Chapter(String id, List<Goal> goals, List<Reward> rewards) {
        public boolean complete(List<Integer> values) {
            return goals.stream().allMatch(goal -> goal.complete(values));
        }

        public String claimKey() { return "journey:claim:" + id; }
        public String noticeKey() { return "journey:notice:" + id; }
    }

    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("first_steps", List.of(new Goal(Metric.STRUCTURES, 2), new Goal(Metric.PAGES, 3)),
                    List.of(new Reward("echo_bandage", 2), new Reward("seeker_spore", 2),
                            new Reward("faint_echo_candy", 2))),
            new Chapter("living_land", List.of(new Goal(Metric.BIOMES, 4), new Goal(Metric.MOBS, 6)),
                    List.of(new Reward("wind_chime_bottle", 3), new Reward("tide_breath_bottle", 3),
                            new Reward("echo_decoy", 3))),
            new Chapter("puzzle_reader", List.of(new Goal(Metric.PUZZLES, 3), new Goal(Metric.PUZZLE_KINDS, 2)),
                    List.of(new Reward("revealing_powder", 3), new Reward("chime_burst", 3),
                            new Reward("clue_map", 1))),
            new Chapter("returning_echoes", List.of(new Goal(Metric.ABILITIES, 1), new Goal(Metric.REVISITS, 3)),
                    List.of(new Reward("echo_metal_ingot", 3), new Reward("echo_candy", 3),
                            new Reward("record_tracing_paper", 4))),
            new Chapter("wonder_wayfarer", List.of(new Goal(Metric.WONDERS, 3), new Goal(Metric.WORKSHOPS, 1)),
                    List.of(new Reward("attuned_echo_gem", 2), new Reward("chime_burst", 4),
                            new Reward("clue_map", 2))),
            new Chapter("deep_expedition", List.of(new Goal(Metric.DEEP_BIOMES, 4),
                    new Goal(Metric.DEEP_STRUCTURES, 4), new Goal(Metric.DEEP_PAGES, 8)),
                    List.of(new Reward("deep_alloy_ingot", 2), new Reward("condensed_echo_candy", 2),
                            new Reward("seeker_spore", 4)))
    );

    private JourneyCatalog() { }

    public static Chapter chapter(int index) {
        return index >= 0 && index < CHAPTERS.size() ? CHAPTERS.get(index) : null;
    }

    public static int readyMask(List<Integer> values, int claimedMask) {
        int result = 0;
        for (int index = 0; index < CHAPTERS.size(); index++) {
            if ((claimedMask & (1 << index)) == 0 && CHAPTERS.get(index).complete(values)) result |= 1 << index;
        }
        return result;
    }
}
