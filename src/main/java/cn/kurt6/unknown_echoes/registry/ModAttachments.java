package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityData;
import cn.kurt6.unknown_echoes.artifact.ArtifactData;
import cn.kurt6.unknown_echoes.journal.ExplorationJournalData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, UnknownEchoes.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EchoAbilityData>> ECHO_ABILITY_DATA =
            ATTACHMENT_TYPES.register("echo_ability_data",
                    () -> AttachmentType.builder(EchoAbilityData::new)
                            .serialize(EchoAbilityData.CODEC)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ExplorationJournalData>> EXPLORATION_JOURNAL =
            ATTACHMENT_TYPES.register("exploration_journal",
                    () -> AttachmentType.builder(ExplorationJournalData::new)
                            .serialize(ExplorationJournalData.CODEC)
                            .copyOnDeath()
                            .build());

    /** 神器与回响能量(V0.6D):神器本体是玩家数据,物品只是凭据(12.2)。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ArtifactData>> ARTIFACT_DATA =
            ATTACHMENT_TYPES.register("artifact_data",
                    () -> AttachmentType.builder(ArtifactData::new)
                            .serialize(ArtifactData.CODEC)
                            .copyOnDeath()
                            .build());
}
