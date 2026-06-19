package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 通用神器凭据物品。具体效果先落为服务端校验、能量/冷却和含蓄反馈,
 * 后续可按神器类型替换成专门 Item,但不能绕过 ArtifactManager。
 */
public class GenericArtifactItem extends Item {
    private final ArtifactType type;

    public GenericArtifactItem(ArtifactType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (!ArtifactManager.isEnabled(type)) {
            actionbar(serverPlayer, "message.unknown_echoes.artifact.disabled");
            return InteractionResultHolder.consume(stack);
        }
        if (!ArtifactManager.validateCredential(serverPlayer, type, stack)) {
            actionbar(serverPlayer, "message.unknown_echoes.artifact.not_yours");
            return InteractionResultHolder.consume(stack);
        }
        if (ArtifactManager.isOnCooldown(serverPlayer, type)) {
            actionbar(serverPlayer, "message.unknown_echoes.artifact.generic_cooldown");
            return InteractionResultHolder.consume(stack);
        }
        if (!ArtifactEffectManager.use(serverPlayer, type)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        if (serial != null) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.artifact.serial", serial)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.unknown_echoes.artifact." + type.getId() + ".hint")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
