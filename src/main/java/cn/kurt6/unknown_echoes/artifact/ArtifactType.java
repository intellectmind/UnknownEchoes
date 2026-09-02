package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/** 神器注册表。神器状态存于玩家数据，物品只作为凭据；升级与调谐统一写入玩家数据。 */
public enum ArtifactType {

    STORM_COMPASS("storm_compass", EchoAbilityType.WIND_ECHO,
            List.of("seek", "homeward"), "windflow",
            () -> ModItems.STORM_COMPASS.get(),
            60, 24),

    TIDE_LANTERN("tide_lantern", EchoAbilityType.TIDE_ECHO,
            List.of("rune", "record"), "stable_reflection",
            () -> ModItems.TIDE_LANTERN.get(),
            45, 18),

    ECHO_LENS("echo_lens", EchoAbilityType.TRUE_SIGHT_ECHO,
            List.of("inscription", "cache"), "archive_door",
            () -> ModItems.ECHO_LENS.get(),
            30, 18),

    TIME_HOURGLASS("time_hourglass", EchoAbilityType.GRAVITY_ECHO,
            List.of("shorthand", "breadth"), "folded_memory",
            () -> ModItems.TIME_HOURGLASS.get(),
            40, 32),

    DIMENSION_ANCHOR("dimension_anchor", EchoAbilityType.TIDE_ECHO,
            List.of("thrift", "dual_anchor"), "undertow_return",
            () -> ModItems.DIMENSION_ANCHOR.get(),
            90, 36),

    RIFT_ANCHOR("rift_anchor", EchoAbilityType.VOID_ECHO,
            List.of("lingering", "twin_spike"), "silent_fold",
            () -> ModItems.RIFT_ANCHOR.get(),
            120, 44);

    /** 等级上限；1 级为初始等级，后续等级开放调谐和词条强化。 */
    public static final int MAX_LEVEL = 3;
    /** 解锁隐藏词条所需的对应能力研究等级(十五章联动)。 */
    public static final int HIDDEN_WORD_RESEARCH_LEVEL = 4;

    private final String id;
    private final EchoAbilityType linkedAbility;
    private final List<String> tuningWords;
    private final String hiddenWord;
    private final Supplier<Item> credentialItem;
    private final int cooldownSeconds;
    private final int useEnergy;

    ArtifactType(String id, EchoAbilityType linkedAbility, List<String> tuningWords,
                 String hiddenWord, Supplier<Item> credentialItem, int cooldownSeconds, int useEnergy) {
        this.id = id;
        this.linkedAbility = linkedAbility;
        this.tuningWords = tuningWords;
        this.hiddenWord = hiddenWord;
        this.credentialItem = credentialItem;
        this.cooldownSeconds = cooldownSeconds;
        this.useEnergy = useEnergy;
    }

    public String getId() {
        return id;
    }

    /** 对应回响能力(研究 4 解锁隐藏词条;能力本身与神器互不解锁)。 */
    public EchoAbilityType getLinkedAbility() {
        return linkedAbility;
    }

    public List<String> getTuningWords() {
        return tuningWords;
    }

    public String getHiddenWord() {
        return hiddenWord;
    }

    /** 凭据物品，可掉落和拾取；序号不符时无法使用。 */
    public Item getCredentialItem() {
        return credentialItem.get();
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getUseEnergy() {
        return useEnergy;
    }

    public static ArtifactType byId(String id) {
        for (ArtifactType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
