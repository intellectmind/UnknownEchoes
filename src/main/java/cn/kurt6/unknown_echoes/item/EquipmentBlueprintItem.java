package cn.kurt6.unknown_echoes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 装备图纸:第一维度武器的配方线索(V0.5C 只发线索,武器本体 V0.6 落地)。
 * 非关键奖励,可正常进宝箱/掉落(设计文档 11.4:只有普通材料与非关键图纸可进掉落表)。
 * 图纸不解锁任何权限,只展示材料清单与出处提示。
 */
public class EquipmentBlueprintItem extends Item {
    /** 对应武器的 lang 子键(wind_spear / tide_crossbow / true_sight_blade)。 */
    private final String weaponKey;

    public EquipmentBlueprintItem(String weaponKey, Properties properties) {
        super(properties);
        this.weaponKey = weaponKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.blueprint.header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.blueprint." + weaponKey + ".materials")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.blueprint." + weaponKey + ".hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
