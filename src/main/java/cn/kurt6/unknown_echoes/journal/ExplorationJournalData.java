package cn.kurt6.unknown_echoes.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 玩家个人探索日志(Data Attachment 持久化,copyOnDeath)。
 * 自动记录:发现过的遗迹、击败过的回响生物、踏入过的回响群系、读过的远古残页。
 * Boss/能力/维度进度直接读 EchoAbilityData,不重复存。
 * 默认个人独立(设计红线 #3);团队共享留待配置系统扩展。
 */
public class ExplorationJournalData {
    public static final int CURRENT_DATA_VERSION = 2;

    public static final Codec<ExplorationJournalData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(d -> d.dataVersion),
            Codec.STRING.listOf().optionalFieldOf("structures", List.of()).forGetter(d -> List.copyOf(d.structures)),
            Codec.STRING.listOf().optionalFieldOf("mobs", List.of()).forGetter(d -> List.copyOf(d.mobs)),
            Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(d -> List.copyOf(d.biomes)),
            Codec.INT.listOf().optionalFieldOf("pages", List.of()).forGetter(d -> List.copyOf(d.pages)),
            Codec.STRING.listOf().optionalFieldOf("clues", List.of()).forGetter(d -> List.copyOf(d.clues))
    ).apply(instance, ExplorationJournalData::new));

    private int dataVersion;
    private final Set<String> structures = new HashSet<>();
    private final Set<String> mobs = new HashSet<>();
    private final Set<String> biomes = new HashSet<>();
    private final Set<Integer> pages = new HashSet<>();
    /** 探索线索(V0.6D,dataVersion 2):ExplorationClue 编码行,旧档缺失按无线索处理。
     *  有序保留写入顺序,超出上限时由 JournalManager 淘汰最旧的已踏入线索。 */
    private final List<String> clues = new ArrayList<>();

    public ExplorationJournalData() {
        this.dataVersion = CURRENT_DATA_VERSION;
    }

    public ExplorationJournalData(int dataVersion, List<String> structures, List<String> mobs,
                                  List<String> biomes, List<Integer> pages, List<String> clues) {
        this.dataVersion = CURRENT_DATA_VERSION;
        this.structures.addAll(structures);
        this.mobs.addAll(mobs);
        this.biomes.addAll(biomes);
        this.pages.addAll(pages);
        this.clues.addAll(clues);
    }

    public Set<String> getStructures() {
        return structures;
    }

    public Set<String> getMobs() {
        return mobs;
    }

    public Set<String> getBiomes() {
        return biomes;
    }

    public Set<Integer> getPages() {
        return pages;
    }

    /** 探索线索原始行(编码格式见 ExplorationClue);读写统一走 JournalManager。 */
    public List<String> getClues() {
        return clues;
    }
}
