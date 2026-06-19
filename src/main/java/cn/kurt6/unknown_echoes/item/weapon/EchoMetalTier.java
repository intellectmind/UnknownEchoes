package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 回响金属材质:正式武器/工具的统一材质等级。
 * 定位高于下界合金一档(终局回响装备):耐久 3250(>合金 2031),挖掘速度 10.0(>合金 9.0),
 * 工具攻击加成 5.0(>合金 4.0),附魔亲和 20(回响材料"易于共鸣"),挖掘等级等同合金(可挖一切)。
 * 近战武器面板伤害仍由 WeaponAttributes 直接声明(不吃材质加成);此处加成只影响工具类。
 * 修复材料为回响金属锭。
 */
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
