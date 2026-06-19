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
            category("ecology", 120, 121, 122, 123, 124, 125, 126, 127)
    );
    public static final int TOTAL = CATEGORIES.stream().mapToInt(Category::total).sum();

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
