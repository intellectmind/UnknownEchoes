package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 共鸣信物:核心机关完成后由机关发放的仪式凭据。 */
public class ResonanceTokenItem extends Item {

    private final EchoAbilityType ability;

    public ResonanceTokenItem(EchoAbilityType ability, Properties properties) {
        super(properties);
        this.ability = ability;
    }

    public EchoAbilityType getAbility() {
        return ability;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.resonance_token." + ability.getId())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.resonance_token.bound")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.resonance_token.use")
                .withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
