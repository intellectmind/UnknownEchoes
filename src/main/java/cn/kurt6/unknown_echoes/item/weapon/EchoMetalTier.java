package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** 回响金属材质:正式武器/工具的统一材质等级。 */
public final class EchoMetalTier implements Tier {

    public static final EchoMetalTier INSTANCE = new EchoMetalTier();

    private EchoMetalTier() {
    }

    @Override
    public int getUses() {
        return 3250;
    }

    @Override
    public float getSpeed() {
        return 10.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 5.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.ECHO_METAL_INGOT.get());
    }
}
