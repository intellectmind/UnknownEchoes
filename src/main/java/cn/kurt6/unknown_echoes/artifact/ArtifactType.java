package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/**
 * 神器注册表(十二章 12.2/12.3)。所有神器共用同一框架:
 * 本体是玩家个人数据(ArtifactData),物品只是凭据;升级与调谐写玩家数据。
 * 新增神器 = 加一个枚举项 + 在 ArtifactManager.checkClaimRequirement 里补领取资格判定,
 * 不允许任何神器绕开框架在物品类里单写一套(12.2 技术边界)。
 */
public enum ArtifactType {

    /** 风暴罗盘:记述派检索终端,指向已获线索遗迹的大致方向。 */
    STORM_COMPASS("storm_compass", EchoAbilityType.WIND_ECHO,
            List.of("seek", "homeward"), "windflow",
            () -> ModItems.STORM_COMPASS.get(),
            120, 18),

    TIDE_LANTERN("tide_lantern", EchoAbilityType.TIDE_ECHO,
            List.of("rune", "record"), "stable_reflection",
            () -> ModItems.TIDE_LANTERN.get(),
            75, 8),

    ECHO_LENS("echo_lens", EchoAbilityType.TRUE_SIGHT_ECHO,
            List.of("inscription", "cache"), "archive_door",
            () -> ModItems.ECHO_LENS.get(),
            60, 12);

    /** 等级上限(12.4:1=领取即得,2=解锁调谐,3=词条增强)。 */
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

    /** 2 级起可调谐的公开词条(二选一)。 */
    public List<String> getTuningWords() {
        return tuningWords;
    }

    /** 隐藏词条(对应能力研究 4 级后出现在升级台选项中)。 */
    public String getHiddenWord() {
        return hiddenWord;
    }

    /** 凭据物品(可掉落可被捡走;序号不符无法使用,红线 #2)。 */
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
