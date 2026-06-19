package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * V0.7A 材料经济分组。JSON 是数据源,这里给后续配方/交易/校验代码提供稳定入口。
 */
public final class EchoMaterialTags {
    public static final TagKey<Item> BASIC_MATERIALS = item("basic_materials");
    public static final TagKey<Item> REGION_MATERIALS = item("region_materials");
    public static final TagKey<Item> CURRENCIES = item("currencies");
    public static final TagKey<Item> RESEARCH_RUBBINGS = item("research_rubbings");
    public static final TagKey<Item> BLUEPRINTS = item("blueprints");
    public static final TagKey<Item> ENHANCEMENT_MATERIALS = item("enhancement_materials");
    public static final TagKey<Item> PERSONAL_CREDENTIALS = item("personal_credentials");

    public static final TagKey<Block> RESOURCE_BLOCKS = block("resource_blocks");
    public static final TagKey<Block> FUNCTION_PLACEHOLDERS = block("function_placeholders");

    private EchoMaterialTags() {
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, UnknownEchoes.id(path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, UnknownEchoes.id(path));
    }
}
