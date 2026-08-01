package cn.kurt6.unknown_echoes.world;

import cn.kurt6.unknown_echoes.UnknownEchoes;

import java.util.List;
import java.util.Set;

/** 第二维度已实现群系目录，与第一维度 T7 完成度分母保持分离。 */
public final class DeepEchoesBiomeCatalog {
    public static final List<String> PATHS = List.of(
            "folded_corridors",
            "shattered_archive",
            "inverted_chasm",
            "weightless_grotto",
            "silent_abyss",
            "voidscar_hollows"
    );
    public static final List<String> IDS = PATHS.stream()
            .map(path -> UnknownEchoes.id(path).toString())
            .toList();
    public static final int REQUIRED_COUNT = IDS.size();

    private DeepEchoesBiomeCatalog() {
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
