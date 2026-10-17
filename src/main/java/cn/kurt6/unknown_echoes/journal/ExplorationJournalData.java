package cn.kurt6.unknown_echoes.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplorationJournalData {
    public static final int CURRENT_DATA_VERSION = 3;
    public static final int MAX_CLUES = 64;
    private static final int MAX_DISCOVERIES = 1024;
    private static final int MAX_PAGES = 256;
    private static final int MAX_ID_LENGTH = 256;
    public static final int MAX_CLUE_LENGTH = 512;
    private static final Codec<String> ID_CODEC = Codec.sizeLimitedString(MAX_ID_LENGTH);

    public static final Codec<ExplorationJournalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            ID_CODEC.listOf(0, MAX_DISCOVERIES).optionalFieldOf("structures", List.of())
                    .forGetter(d -> List.copyOf(d.structures)),
            ID_CODEC.listOf(0, MAX_DISCOVERIES).optionalFieldOf("mobs", List.of())
                    .forGetter(d -> List.copyOf(d.mobs)),
            ID_CODEC.listOf(0, MAX_DISCOVERIES).optionalFieldOf("biomes", List.of())
                    .forGetter(d -> List.copyOf(d.biomes)),
            Codec.INT.listOf(0, MAX_PAGES).optionalFieldOf("pages", List.of())
                    .forGetter(d -> List.copyOf(d.pages)),
            Codec.sizeLimitedString(MAX_CLUE_LENGTH).listOf(0, MAX_PAGES).optionalFieldOf("clues", List.of())
                    .forGetter(d -> List.copyOf(d.clues)),
            Codec.sizeLimitedString(MAX_CLUE_LENGTH).optionalFieldOf("destination", "")
                    .forGetter(ExplorationJournalData::getEncodedDestination)
    ).apply(instance, ExplorationJournalData::new));

    private int dataVersion;
    private final Set<String> structures = new HashSet<>();
    private final Set<String> mobs = new HashSet<>();
    private final Set<String> biomes = new HashSet<>();
    private final Set<Integer> pages = new HashSet<>();
    /** 探索线索编码数据；缺失数据按无线索处理。 */
    private final List<String> clues = new ArrayList<>();
    private String destination = "";

    public ExplorationJournalData() {
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public ExplorationJournalData(int dataVersion, List<String> structures, List<String> mobs,
                                  List<String> biomes, List<Integer> pages, List<String> clues,
                                  String destination) {
        this.dataVersion = CURRENT_DATA_VERSION;
        copyResourceIds(structures, this.structures);
        copyResourceIds(mobs, this.mobs);
        copyResourceIds(biomes, this.biomes);
        for (Integer page : pages) {
            if (page != null && page > 0 && page <= 10000) {
                this.pages.add(page);
            }
        }
        for (String line : clues) {
            if (canStoreClue(line)) {
                this.clues.add(line);
            }
        }
        setDestination(destination);
        while (this.clues.size() > MAX_CLUES) {
            evictOldClue();
        }
    }

    private static void copyResourceIds(List<String> source, Set<String> target) {
        for (String id : source) {
            if (ResourceLocation.tryParse(id) != null) {
                target.add(id);
            }
        }
    }

    public Set<String> getStructures() {
        return structures;
    }

    /** 记录结构发现，保持运行时写入与附件 Codec 上限一致。 */
    public boolean addStructure(String structureId) {
        return addResourceId(structures, structureId, MAX_DISCOVERIES);
    }

    public Set<String> getMobs() {
        return mobs;
    }

    public boolean addMob(String mobId) {
        return addResourceId(mobs, mobId, MAX_DISCOVERIES);
    }

    public Set<String> getBiomes() {
        return biomes;
    }

    public boolean addBiome(String biomeId) {
        return addResourceId(biomes, biomeId, MAX_DISCOVERIES);
    }

    public Set<Integer> getPages() {
        return pages;
    }

    public boolean addPage(int pageId) {
        if (pageId <= 0 || pageId > 10000 || pages.contains(pageId)
                || pages.size() >= MAX_PAGES) {
            return false;
        }
        return pages.add(pageId);
    }

    /** 探索线索原始行(编码格式见 ExplorationClue);读写统一走 JournalManager。 */
    public List<String> getClues() {
        return clues;
    }

    /** 线索编码必须满足附件单条字符串上限，避免运行时数据在保存时失败。 */
    public boolean canStoreClue(String encodedClue) {
        if (encodedClue == null || encodedClue.length() > MAX_CLUE_LENGTH) {
            return false;
        }
        ExplorationClue clue = ExplorationClue.decode(encodedClue);
        return clue != null && ResourceLocation.tryParse(clue.structureId()) != null
                && ResourceLocation.tryParse(clue.dimension()) != null;
    }

    public ExplorationClue getDestination() {
        ExplorationClue selected = ExplorationClue.decode(destination);
        if (selected != null) {
            for (String line : clues) {
                ExplorationClue clue = ExplorationClue.decode(line);
                if (selected.sameSite(clue)) {
                    return clue;
                }
            }
        }
        return null;
    }

    public String getEncodedDestination() {
        ExplorationClue clue = getDestination();
        return clue == null ? "" : clue.encode();
    }

    // 只能选择个人日志中的现有地点，空字符串用于取消。
    public boolean setDestination(String encodedClue) {
        if ("".equals(encodedClue)) {
            boolean changed = !destination.isEmpty();
            destination = "";
            return changed;
        }
        if (!canStoreClue(encodedClue)) {
            return false;
        }
        ExplorationClue requested = ExplorationClue.decode(encodedClue);
        for (String line : clues) {
            ExplorationClue clue = ExplorationClue.decode(line);
            if (requested.sameSite(clue)) {
                boolean changed = !clue.sameSite(getDestination());
                destination = clue.encode();
                return changed;
            }
        }
        return false;
    }

    // 大遗迹按实际范围合并旧入口记录；保留选中地点的身份与坐标。
    public boolean visitStructure(String structureId, String dimension,
                                  BlockPos anchor, BoundingBox bounds) {
        ExplorationClue incoming = new ExplorationClue(structureId, dimension, anchor, true);
        if (!canStoreClue(incoming.encode())) return false;
        List<Integer> matches = new ArrayList<>();
        ExplorationClue retained = null;
        ExplorationClue selected = getDestination();
        for (int i = 0; i < clues.size(); i++) {
            ExplorationClue clue = ExplorationClue.decode(clues.get(i));
            if (clue != null && clue.structureId().equals(structureId) && clue.dimension().equals(dimension)
                    && ((bounds != null && bounds.isInside(clue.pos())) || clue.pos().distSqr(anchor) <= 96 * 96)) {
                matches.add(i);
                if (retained == null || clue.sameSite(selected)) retained = clue;
            }
        }
        if (retained == null) return addClue(incoming);
        boolean changed = !retained.visited() || matches.size() > 1;
        clues.set(matches.getFirst(), retained.asVisited().encode());
        for (int i = matches.size() - 1; i > 0; i--) clues.remove((int) matches.get(i));
        return changed;
    }

    // 同结构 96 格内合并；容量满时保留新线索和选中的目的地。
    public boolean addClue(ExplorationClue clue) {
        String encoded = clue == null ? null : clue.encode();
        if (!canStoreClue(encoded)) {
            return false;
        }
        for (int i = 0; i < clues.size(); i++) {
            ExplorationClue existing = ExplorationClue.decode(clues.get(i));
            if (existing != null && existing.structureId().equals(clue.structureId())
                    && existing.dimension().equals(clue.dimension())
                    && existing.pos().distSqr(clue.pos()) <= 96 * 96) {
                if (clue.visited() && !existing.visited()) {
                    clues.set(i, existing.asVisited().encode());
                    return true;
                }
                return false;
            }
        }
        while (clues.size() >= MAX_CLUES) {
            evictOldClue();
        }
        clues.add(encoded);
        return true;
    }

    private void evictOldClue() {
        ExplorationClue selected = getDestination();
        int fallback = -1;
        for (int i = 0; i < clues.size(); i++) {
            ExplorationClue clue = ExplorationClue.decode(clues.get(i));
            if (selected != null && selected.sameSite(clue)) {
                continue;
            }
            if (clue == null || clue.visited()) {
                clues.remove(i);
                return;
            }
            if (fallback < 0) {
                fallback = i;
            }
        }
        clues.remove(fallback >= 0 ? fallback : 0);
    }

    private static boolean addResourceId(Set<String> target, String id, int maxEntries) {
        if (id == null || id.length() > MAX_ID_LENGTH
                || ResourceLocation.tryParse(id) == null
                || target.contains(id) || target.size() >= maxEntries) {
            return false;
        }
        return target.add(id);
    }
}
