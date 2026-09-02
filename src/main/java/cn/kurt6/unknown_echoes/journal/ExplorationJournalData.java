package cn.kurt6.unknown_echoes.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplorationJournalData {
    public static final int CURRENT_DATA_VERSION = 2;
    public static final int MAX_CLUES = 64;
    private static final int MAX_DISCOVERIES = 1024;
    private static final int MAX_PAGES = 256;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_CLUE_LENGTH = 512;
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
                    .forGetter(d -> List.copyOf(d.clues))
    ).apply(instance, ExplorationJournalData::new));

    private int dataVersion;
    private final Set<String> structures = new HashSet<>();
    private final Set<String> mobs = new HashSet<>();
    private final Set<String> biomes = new HashSet<>();
    private final Set<Integer> pages = new HashSet<>();
    /** 探索线索编码数据；缺失数据按无线索处理。 */
    private final List<String> clues = new ArrayList<>();

    public ExplorationJournalData() {
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public ExplorationJournalData(int dataVersion, List<String> structures, List<String> mobs,
                                  List<String> biomes, List<Integer> pages, List<String> clues) {
        this.dataVersion = CURRENT_DATA_VERSION;
        copyResourceIds(structures, this.structures);
        copyResourceIds(mobs, this.mobs);
        copyResourceIds(biomes, this.biomes);
        for (Integer page : pages) {
            if (page != null && page > 0 && page <= 10000) {
                this.pages.add(page);
            }
        }
        int start = Math.max(0, clues.size() - MAX_CLUES);
        for (int i = start; i < clues.size(); i++) {
            String line = clues.get(i);
            ExplorationClue clue = ExplorationClue.decode(line);
            if (clue != null && ResourceLocation.tryParse(clue.structureId()) != null
                    && ResourceLocation.tryParse(clue.dimension()) != null) {
                this.clues.add(line);
            }
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

    private static boolean addResourceId(Set<String> target, String id, int maxEntries) {
        if (id == null || id.length() > MAX_ID_LENGTH
                || ResourceLocation.tryParse(id) == null
                || target.contains(id) || target.size() >= maxEntries) {
            return false;
        }
        return target.add(id);
    }
}
