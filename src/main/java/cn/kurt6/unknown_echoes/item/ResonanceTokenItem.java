package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 共鸣信物(V0.6E,5.1.1):核心机关完成后由机关发放的仪式凭据。
 * 物品不承载任何权威进度——能力判定永远是服务端双记录(机关完成 + Boss 击败);
 * 凭据归属同神器序号方案(TOKEN_ABILITY + CREDENTIAL_SERIAL),他人/旧信物在祭坛不被认可。
 * 丢失不卡进度:回到机关核心处免费复领(ResonanceTokenManager.reissueIfMissing)。
 * tooltip 只做短提示,完整说明在总览"凭据与誓记"页(5.8)。
 */
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
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
