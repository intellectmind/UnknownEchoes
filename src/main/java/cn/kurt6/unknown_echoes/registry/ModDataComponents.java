package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, UnknownEchoes.MODID);

    /** 远古残页页码:决定残页显示哪段记录(lang 键 page.unknown_echoes.<id>.*)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PAGE_ID =
            DATA_COMPONENT_TYPES.register("page_id",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 999))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /** 神器凭据所属神器 id(V0.6D 12.2:物品只是凭据,功能数据一律读玩家数据)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT_ID =
            DATA_COMPONENT_TYPES.register("artifact_id",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /** 神器凭据序号:与玩家 ArtifactData 比对,序号不符(旧凭据/他人凭据)不可用。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CREDENTIAL_SERIAL =
            DATA_COMPONENT_TYPES.register("credential_serial",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /** 线索地图绑定的结构 id(迷途旅者出售;使用时一次性定位写入个人日志线索)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CLUE_STRUCTURE =
            DATA_COMPONENT_TYPES.register("clue_structure",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /** 共鸣信物所属能力 id(V0.6E 5.1.1:信物只是仪式凭据,序号复用 CREDENTIAL_SERIAL)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOKEN_ABILITY =
            DATA_COMPONENT_TYPES.register("token_ability",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
}
