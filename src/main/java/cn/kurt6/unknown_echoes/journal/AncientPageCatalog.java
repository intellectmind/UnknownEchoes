package cn.kurt6.unknown_echoes.journal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AncientPageCatalog {
    public static final List<Category> CATEGORIES = List.of(
            category("entry", 1, 2, 3, 4, 5, 6, 7, 8),
            category("wind", 17, 18, 19, 20, 21, 22, 23, 24),
            category("tide", 42, 43, 44, 45, 46, 47, 48, 49, 50, 51),
            category("true_sight", 56, 57, 58, 59, 60, 61, 62, 63, 64, 65),
            category("silent", 70, 71, 72, 73, 74, 75, 76, 77),
            category("crystal", 80, 81, 82, 83, 84, 85),
            category("dream", 86, 87, 88, 89, 90, 91),
            category("bell", 92, 93, 94, 95, 96, 97),
            category("endgame", 104, 105, 106, 107, 108, 109, 110, 111, 112, 113),
            category("ecology", 120, 121, 122, 123, 124, 125, 126, 127),
            category("deep", 128, 129, 130, 131, 132, 133, 134, 135,
                    136, 137, 138, 139, 140, 141, 142, 143,
                    144, 145, 146, 147, 148, 149, 150, 151,
                    152, 153, 154, 155, 156, 157, 158, 159,
                    160, 161, 162, 163, 164, 165, 166, 167,
                    168, 169, 170, 171, 172, 173, 174, 175,
                    176, 177, 178, 179, 180, 181, 182, 183,
                    184, 185, 186, 187, 188, 189, 190, 191,
                    192, 193, 194, 195, 196, 197, 198, 199,
                    200, 201, 202, 203, 204, 205, 206, 207)
    );
    public static final int TOTAL = CATEGORIES.stream().mapToInt(Category::total).sum();
    public static final int FIRST_DIMENSION_TOTAL = CATEGORIES.stream()
            .filter(category -> !category.id().equals("deep"))
            .mapToInt(Category::total)
            .sum();

    private static final Set<Integer> RELEASE_IDS = releaseIds();

    private AncientPageCatalog() {
    }

    public static boolean isReleasePage(int pageId) {
        return RELEASE_IDS.contains(pageId);
    }

    public static int countReleasePages(Collection<Integer> pages) {
        int count = 0;
        for (int pageId : pages) {
            if (isReleasePage(pageId)) {
                count++;
            }
        }
        return count;
    }

    public static int countFirstDimensionPages(Collection<Integer> pages) {
        int count = 0;
        for (Category category : CATEGORIES) {
            if (!category.id().equals("deep")) {
                count += category.read(pages);
            }
        }
        return count;
    }

    private static Set<Integer> releaseIds() {
        Set<Integer> ids = new HashSet<>();
        for (Category category : CATEGORIES) {
            for (int id : category.pageIds()) {
                ids.add(id);
            }
        }
        return Set.copyOf(ids);
    }

    private static Category category(String id, int... pageIds) {
        return new Category(id, pageIds);
    }

    public record Category(String id, int[] pageIds) {
        public int total() {
            return pageIds.length;
        }

        public int read(Collection<Integer> pages) {
            int count = 0;
            for (int pageId : pageIds) {
                if (pages.contains(pageId)) {
                    count++;
                }
            }
            return count;
        }
    }
}
