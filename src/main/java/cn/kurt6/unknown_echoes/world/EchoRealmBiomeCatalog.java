package cn.kurt6.unknown_echoes.world;

import cn.kurt6.unknown_echoes.UnknownEchoes;

import java.util.List;
import java.util.Set;

public final class EchoRealmBiomeCatalog {
    public static final List<String> PATHS = List.of(
            "echo_forest",
            "mirror_lake",
            "tide_lighthouse_reef",
            "mirror_dust_cloister",
            "silent_swamp",
            "floating_isles",
            "echo_cliffs",
            "crystal_song_grove",
            "dreaming_meadow",
            "broken_bell_wastes"
    );
    public static final List<String> IDS = PATHS.stream()
            .map(path -> UnknownEchoes.id(path).toString())
            .toList();
    public static final int REQUIRED_COUNT = IDS.size();

    private EchoRealmBiomeCatalog() {
    }

    public static boolean hasVisitedAll(Set<String> visited) {
        return visited.containsAll(IDS);
    }

    public static int countKnown(Set<String> visited) {
        int count = 0;
        for (String id : IDS) {
            if (visited.contains(id)) {
                count++;
            }
        }
        return count;
    }
}
