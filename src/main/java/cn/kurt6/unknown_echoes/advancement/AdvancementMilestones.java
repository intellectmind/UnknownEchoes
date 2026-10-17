package cn.kurt6.unknown_echoes.advancement;

import cn.kurt6.unknown_echoes.block.puzzle.EchoChaseCatalog;
import cn.kurt6.unknown_echoes.journal.JourneyCatalog;
import cn.kurt6.unknown_echoes.journal.JourneyPuzzleRecords;
import cn.kurt6.unknown_echoes.study.WorkshopStudyCatalog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AdvancementMilestones {
    private AdvancementMilestones() { }

    public static Set<String> activities(Set<String> records) {
        Set<String> earned = new LinkedHashSet<>();
        var puzzles = JourneyPuzzleRecords.count(records);
        add(earned, puzzles.sites() >= 1, "first_puzzle");
        add(earned, puzzles.sites() >= 10, "puzzle_adept");
        add(earned, puzzles.kinds() >= 8, "puzzle_variety");
        add(earned, puzzles.workshops() >= 1, "workshop_first");

        int study = WorkshopStudyCatalog.mask(records, false);
        int themes = 0;
        int precise = 0;
        for (int theme = 0; theme < WorkshopStudyCatalog.THEMES; theme++) {
            themes |= WorkshopStudyCatalog.bit(theme, 0);
            precise |= WorkshopStudyCatalog.bit(theme, 2);
        }
        add(earned, study != 0, "study_first");
        add(earned, (study & precise) != 0, "study_precision");
        add(earned, (study & themes) == themes, "study_all_themes");
        add(earned, study == WorkshopStudyCatalog.ALL, "study_mastery");

        int chase = EchoChaseCatalog.mask(records, false);
        add(earned, chase != 0, "chase_first");
        add(earned, Integer.bitCount(chase) >= 3, "chase_three");
        add(earned, chase == EchoChaseCatalog.ALL, "chase_all");
        return earned;
    }

    public static Set<String> journeys(List<Integer> values) {
        Set<String> earned = new LinkedHashSet<>();
        boolean complete = true;
        for (var chapter : JourneyCatalog.CHAPTERS) {
            boolean done = chapter.complete(values);
            add(earned, done, "journey_" + chapter.id());
            complete &= done;
        }
        add(earned, complete, "journey_complete");
        return earned;
    }

    public static Set<String> particles(int tier, List<Integer> counts) {
        Set<String> earned = new LinkedHashSet<>();
        int total = counts.stream().mapToInt(Integer::intValue).sum();
        int strongest = counts.stream().mapToInt(Integer::intValue).max().orElse(0);
        long kinds = counts.stream().filter(count -> count > 0).count();
        add(earned, tier >= 1, "particle_attuned");
        add(earned, total >= 1, "particle_infused");
        add(earned, tier >= 3, "particle_capacity");
        add(earned, strongest >= 10, "particle_resonance");
        add(earned, kinds >= 3, "particle_mixture");
        add(earned, tier >= 5, "particle_master");
        add(earned, tier >= 5 && total == 50, "particle_full");
        add(earned, tier >= 5 && strongest == 50, "particle_specialist");
        return earned;
    }

    private static void add(Set<String> earned, boolean complete, String id) {
        if (complete) earned.add(id);
    }
}
