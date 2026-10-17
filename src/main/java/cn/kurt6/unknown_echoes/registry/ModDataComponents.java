package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    private static final int ARTIFACT_ID_MAX_LENGTH = 64;
    private static final int OWNER_UUID_MAX_LENGTH = 36;
    private static final int RESOURCE_ID_MAX_LENGTH = 256;
    private static final int ABILITY_ID_MAX_LENGTH = 64;

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, UnknownEchoes.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<cn.kurt6.unknown_echoes.particle.ParticleData>> PARTICLES =
            DATA_COMPONENT_TYPES.register("particles", () -> DataComponentType.<cn.kurt6.unknown_echoes.particle.ParticleData>builder()
                    .persistent(cn.kurt6.unknown_echoes.particle.ParticleData.CODEC)
                    .networkSynchronized(cn.kurt6.unknown_echoes.particle.ParticleData.STREAM_CODEC)
                    .cacheEncoding().build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PAGE_ID =
            DATA_COMPONENT_TYPES.register("page_id",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 999))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT_ID =
            DATA_COMPONENT_TYPES.register("artifact_id",
                    () -> stringComponent(ARTIFACT_ID_MAX_LENGTH));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CREDENTIAL_SERIAL =
            DATA_COMPONENT_TYPES.register("credential_serial",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT_OWNER =
            DATA_COMPONENT_TYPES.register("artifact_owner",
                    () -> stringComponent(OWNER_UUID_MAX_LENGTH));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CLUE_STRUCTURE =
            DATA_COMPONENT_TYPES.register("clue_structure",
                    () -> stringComponent(RESOURCE_ID_MAX_LENGTH));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOKEN_ABILITY =
            DATA_COMPONENT_TYPES.register("token_ability",
                    () -> stringComponent(ABILITY_ID_MAX_LENGTH));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TOKEN_OWNER =
            DATA_COMPONENT_TYPES.register("token_owner",
                    () -> stringComponent(OWNER_UUID_MAX_LENGTH));

    private static DataComponentType<String> stringComponent(int maxLength) {
        return DataComponentType.<String>builder()
                .persistent(Codec.sizeLimitedString(maxLength))
                .networkSynchronized(ByteBufCodecs.stringUtf8(maxLength))
                .build();
    }
}
