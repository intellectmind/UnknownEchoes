package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** 实体 tag 集中定义,避免能力/装备逻辑依赖某个武器类。 */
public final class ModEntityTags {
    public static final TagKey<EntityType<?>> ILLUSION_MOBS =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("illusion_mobs"));
    public static final TagKey<EntityType<?>> ECHO_REALM_HOSTILES =
            TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_realm_hostiles"));

    private ModEntityTags() {
    }
}
