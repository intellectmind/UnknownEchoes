package cn.kurt6.unknown_echoes.journal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JourneyPuzzleRecords {
    private static final String PREFIX = "journey:puzzle:";
    private static final int PER_KIND_LIMIT = 8;
    private static final Set<String> KINDS = Set.of(
            "sequence", "ring", "chime", "pairs", "water", "optics", "balance", "gears");

    private static final Set<String> WORKSHOP_KINDS = Set.of("water", "optics", "balance", "gears");

    public record Record(String site, String kind) { }
    public record Counts(int sites, int kinds, int workshops) { }

    private JourneyPuzzleRecords() { }

    public static Optional<Record> decode(String key) {
        if (key == null || !key.startsWith(PREFIX)) return Optional.empty();
        String[] fields = key.substring(PREFIX.length()).split("\\|", -1);
        if (fields.length != 3 || !fields[0].matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
                || fields[0].length() > 256 || !KINDS.contains(fields[2])) return Optional.empty();
        try {
            if (!Long.toString(Long.parseLong(fields[1])).equals(fields[1])) return Optional.empty();
        }
        catch (NumberFormatException ignored) { return Optional.empty(); }
        return Optional.of(new Record(fields[0] + "|" + fields[1], fields[2]));
    }

    public static String nextKey(Collection<String> records, String dimension, long position, String kind) {
        String key = PREFIX + dimension + "|" + position + "|" + kind;
        Optional<Record> candidate = decode(key);
        if (candidate.isEmpty()) return null;
        int sameKind = 0;
        for (String existing : records) {
            Optional<Record> decoded = decode(existing);
            if (decoded.isEmpty()) continue;
            if (decoded.get().site().equals(candidate.get().site())) return null;
            if (decoded.get().kind().equals(kind)) sameKind++;
        }
        return sameKind >= PER_KIND_LIMIT ? null : key;
    }

    public static Map<String, String> sequenceCorrection(Collection<String> records, String dimension,
                                                         long position, String kind) {
        if (!"sequence".equals(kind)) return Map.of();
        String target = PREFIX + dimension + "|" + position + "|sequence";
        if (decode(target).isEmpty()) return Map.of();
        String source = PREFIX + dimension + "|" + position + "|ring";
        return records.contains(source) ? Map.of(source, target) : Map.of();
    }

    public static Counts count(Collection<String> records) {
        Set<String> sites = new HashSet<>();
        Set<String> kinds = new HashSet<>();
        Set<String> workshops = new HashSet<>();
        for (String key : records) {
            decode(key).ifPresent(record -> {
                sites.add(record.site());
                kinds.add(record.kind());
                if (WORKSHOP_KINDS.contains(record.kind())) {
                    workshops.add(record.site());
                }
            });
        }
        return new Counts(sites.size(), kinds.size(), workshops.size());
    }
}
