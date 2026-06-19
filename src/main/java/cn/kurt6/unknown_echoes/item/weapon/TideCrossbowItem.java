package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 潮汐弩(V0.6B 三件武器之一,V0.6C 强化):
 * - 弹射物伤害约 9.5(基础伤害 3.0 × 离弦速度 ≈3.15;V3.2 上调,原设定基线 7)。
 * - 装填略快于原版(等效 20 tick,原版 25):释放结算时补 5 tick 已用时长,
 *   不复制私有装填逻辑;装填动画进度条按原版速度显示,提前松手即可完成装填。
 * - 潮爆 AOE、水中增伤、水下弹道无衰减与符文方向标记由 TideBoltEntity 实现。
 */
public class TideCrossbowItem extends CrossbowItem {

    private final double boltBaseDamage;
    private final int quickLoadTicks;

    public TideCrossbowItem(Properties properties) {
        this(3.0, 0, properties);
    }

    public TideCrossbowItem(double boltBaseDamage, int quickLoadTicks, Properties properties) {
        super(properties);
        this.boltBaseDamage = boltBaseDamage;
        this.quickLoadTicks = quickLoadTicks;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.skill")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft - quickLoadTicks);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon,
                                          ItemStack ammo, boolean isCrit) {
        if (!(ammo.getItem() instanceof ArrowItem)) {
            // 烟花火箭等非箭弹药走原版逻辑
            return super.createProjectile(level, shooter, weapon, ammo, isCrit);
        }
        TideBoltEntity bolt = new TideBoltEntity(level, shooter,
                ammo.copyWithCount(1), weapon.copyWithCount(1));
        bolt.setBaseDamage(boltBaseDamage);
        if (isCrit) {
            bolt.setCritArrow(true);
        }
        bolt.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        return bolt;
    }
}
