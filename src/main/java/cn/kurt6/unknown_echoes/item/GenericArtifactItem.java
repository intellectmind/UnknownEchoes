package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 通用神器凭据物品。
 * 具体效果先落为服务端校验、能量/冷却和含蓄反馈, 按神器类型替换成专门 Item,但不能绕过 ArtifactManager。
 */
public class GenericArtifactItem extends Item implements ItemActiveSkill {
    private final ArtifactType type;

    public GenericArtifactItem(ArtifactType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /** 神器由「使用神器」或「物品主动技能」键触发；锚点绑定走「物品副功能」键。 */
    @Override
    public boolean activateItemSkill(ServerLevel level, ServerPlayer player,
                                     ItemStack stack, InteractionHand hand) {
        return activate(player, stack, hand, false);
    }

    @Override
    public boolean hasAlternateItemSkill() {
        return ArtifactEffectManager.hasBindMode(type);
    }

    @Override
    public boolean activateAlternateItemSkill(ServerLevel level, ServerPlayer player,
                                              ItemStack stack, InteractionHand hand) {
        return activate(player, stack, hand, true);
    }

    private boolean activate(ServerPlayer player, ItemStack stack, InteractionHand hand,
                             boolean bindInstead) {
        if (!ArtifactManager.isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return false;
        }
        if (!ArtifactManager.validateCredential(player, type, stack)) {
            actionbar(player, "message.unknown_echoes.artifact.not_yours");
            return false;
        }
        if (ArtifactManager.isOnCooldown(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.generic_cooldown");
            return false;
        }
        return ArtifactEffectManager.use(player, type, hand, bindInstead);
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        ArtifactTooltip.add(tooltip, type);
    }
}
