package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** 深层合金工具材质：后期采掘档位，仍不具备任何机关权限。 */
public final class DeepAlloyTier implements Tier {
    public static final DeepAlloyTier INSTANCE = new DeepAlloyTier();

    private DeepAlloyTier() {
    }

    @Override
    public int getUses() {
        return 4200;
    }

    @Override
    public float getSpeed() {
        return 12.5F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 6.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.DEEP_ALLOY_INGOT.get());
    }
}
