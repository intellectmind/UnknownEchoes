package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.journal.ClueLocator;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 线索地图(V0.6D,迷途旅者出售):右键阅读后做一次有界定位,把"方位记忆"写入个人日志线索,
 * 之后由风暴罗盘指向或日志回看。只指向已注册普通探索点的模糊方向(16.4:不出售关键进度物)。
 * clue_structure 组件指定目标结构;无组件时按当前维度从普通探索池随机。
 */
public class ClueMapItem extends Item {

    /** 无组件时的回声境域普通探索池(只含非 Boss 普通结构)。 */
    private static final List<String> REALM_POOL = List.of(
            "small_echo_ruin", "rune_puzzle_room", "silent_hut", "wind_eroded_tower",
            "tide_lighthouse_reef", "mirror_dust_cloister", "broken_archive");

    /** 主世界普通探索池(入口遗迹与信标废墟)。 */
    private static final List<String> OVERWORLD_POOL = List.of(
            "small_echo_ruin", "resonance_beacon_structure");

    public ClueMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        ResourceLocation target = resolveTarget(serverPlayer, stack);
        boolean found = target != null && ClueLocator.locateAndAddClue(serverPlayer, target);
        if (found) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.unknown_echoes.clue_map.read",
                            Component.translatable("structure.unknown_echoes." + target.getPath())));
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.shrink(1);
        } else {
            // 半径内找不到:地图作废没收没道理,保留物品,提示换个地方再读
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.clue_map.too_far"), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    private ResourceLocation resolveTarget(ServerPlayer player, ItemStack stack) {
        String bound = stack.get(ModDataComponents.CLUE_STRUCTURE.get());
        if (bound != null) {
            return ResourceLocation.tryParse(bound.contains(":") ? bound
                    : UnknownEchoes.MODID + ":" + bound);
        }
        boolean inRealm = player.level().dimension().location()
                .equals(cn.kurt6.unknown_echoes.ability.EchoPermission.ECHO_REALM_ID);
        List<String> pool = inRealm ? REALM_POOL : OVERWORLD_POOL;
        return UnknownEchoes.id(pool.get(player.getRandom().nextInt(pool.size())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.clue_map.hint")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
