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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PAGE_ID =
            DATA_COMPONENT_TYPES.register("page_id",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 999))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT_ID =
            DATA_COMPONENT_TYPES.register("artifact_id",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CREDENTIAL_SERIAL =
            DATA_COMPONENT_TYPES.register("credential_serial",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT_OWNER =
            DATA_COMPONENT_TYPES.register("artifact_owner",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CLUE_STRUCTURE =
            DATA_COMPONENT_TYPES.register("clue_structure",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOKEN_ABILITY =
            DATA_COMPONENT_TYPES.register("token_ability",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOKEN_OWNER =
            DATA_COMPONENT_TYPES.register("token_owner",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
}
