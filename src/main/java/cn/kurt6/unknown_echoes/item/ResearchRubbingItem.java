package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * 研究拓片:守护者结算与谜题奖励箱产出的研究材料。
 * V0.7D 起:拓片右键阅读写入基础能力研究线(+1 研究点)并消耗。
 * 风/潮汐/真视未获得基础能力时不消耗。
 * 物品本身可掉落可交易——强化材料明确允许进掉落表(11.1/13.2)。
 */
public class ResearchRubbingItem extends Item {

    /** 拓片种类:wind / tide / true_sight。 */
    private final String kind;

    public ResearchRubbingItem(String kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public String getKind() {
        return this.kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            EchoResearchLine line = EchoResearchLine.byId(this.kind);
            if (line == null) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.research_rubbing.unresponsive"), true);
            } else if (!EchoResearchManager.isUnlocked(serverPlayer, line)) {
                serverPlayer.displayClientMessage(Component.translatable(
                        line.isAbilityGated()
                                ? "message.unknown_echoes.research_rubbing.unresponsive"
                                : "message.unknown_echoes.research_rubbing.undiscovered"), true);
            } else {
                int currentLevel = EchoResearchManager.getResearchLevel(serverPlayer, line);
                if (currentLevel >= EchoResearchManager.MAX_RESEARCH_LEVEL) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.research_rubbing.saturated"), true);
                    return InteractionResultHolder.consume(stack);
                }
                int nextLevel = currentLevel + 1;
                MaterialCost missing = firstMissingCost(serverPlayer, line, nextLevel);
                if (!serverPlayer.getAbilities().instabuild && missing != null) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.research_rubbing.need_material",
                            new ItemStack(missing.item()).getHoverName(), missing.count()), true);
                    return InteractionResultHolder.consume(stack);
                }
                consumeCosts(serverPlayer, line, nextLevel);
                EchoResearchManager.addResearchPoints(serverPlayer, line, 1);
                stack.shrink(1);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.research_rubbing.absorbed",
                        Component.translatable("research.unknown_echoes." + line.getId()),
                        EchoResearchManager.getResearchLevel(serverPlayer, line)), true);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.research_rubbing." + this.kind)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.research_rubbing.cost_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private record MaterialCost(Item item, int count) {}

    private static MaterialCost cost(Item item, int count) {
        return new MaterialCost(item, count);
    }

    private static MaterialCost firstMissingCost(ServerPlayer player, EchoResearchLine line, int nextLevel) {
        for (MaterialCost cost : upgradeCosts(line, nextLevel)) {
            if (countItem(player, cost.item()) < cost.count()) {
                return cost;
            }
        }
        return null;
    }

    private static void consumeCosts(ServerPlayer player, EchoResearchLine line, int nextLevel) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (MaterialCost cost : upgradeCosts(line, nextLevel)) {
            consumeItem(player, cost.item(), cost.count());
        }
    }

    /**
     * 拓片阅读按"即将提升到的等级"扣媒材;古代残页仍作为稀有探索理解,不在这里扣。
     */
    private static List<MaterialCost> upgradeCosts(EchoResearchLine line, int nextLevel) {
        return switch (line) {
            case WIND -> switch (nextLevel) {
                case 1 -> List.of(cost(ModItems.WIND_RUNE_SHARD.get(), 2),
                        cost(ModItems.ECHO_DUST.get(), 1));
                case 2 -> List.of(cost(ModItems.SKY_QUARTZ.get(), 2),
                        cost(ModItems.CRYSTAL_FEATHER.get(), 1),
                        cost(ModItems.WIND_RUNE_SHARD.get(), 2));
                case 3 -> List.of(cost(ModItems.WIND_ENHANCE_CORE.get(), 1),
                        cost(ModItems.WINDWOVEN_THREAD.get(), 2),
                        cost(ModItems.SKY_QUARTZ.get(), 2));
                case 4 -> List.of(cost(ModItems.STORM_COMPASS_PART.get(), 1),
                        cost(ModItems.WIND_ENHANCE_CORE.get(), 2),
                        cost(ModItems.OATHBOUND_ALLOY.get(), 1));
                default -> List.of();
            };
            case TIDE -> switch (nextLevel) {
                case 1 -> List.of(cost(ModItems.TIDE_RUNE_FRAGMENT.get(), 2),
                        cost(ModItems.TIDE_PEARL.get(), 1));
                case 2 -> List.of(cost(ModItems.MIRROR_LAKE_SHARD.get(), 2),
                        cost(ModItems.TIDE_SALT.get(), 2),
                        cost(ModItems.WATCHER_EYE_SHARD.get(), 1));
                case 3 -> List.of(cost(ModItems.TIDE_UPGRADE_CORE.get(), 1),
                        cost(ModItems.MIRROR_TIDE_ALLOY_PLATE.get(), 2),
                        cost(ModItems.TIDE_CORE.get(), 1));
                case 4 -> List.of(cost(ModItems.TIDE_UPGRADE_CORE.get(), 2),
                        cost(ModItems.WATCHER_EYE_SHARD.get(), 2),
                        cost(ModItems.OATHBOUND_ALLOY.get(), 1));
                default -> List.of();
            };
            case TRUE_SIGHT -> switch (nextLevel) {
                case 1 -> List.of(cost(ModItems.ILLUSION_DUST.get(), 2),
                        cost(ModItems.POLISHED_MIRROR_DUST.get(), 1));
                case 2 -> List.of(cost(ModItems.MIRROR_LAKE_SHARD.get(), 2),
                        cost(ModItems.DECOY_SHARD.get(), 1),
                        cost(ModItems.MIRROR_SCALE.get(), 1));
                case 3 -> List.of(cost(ModItems.TRUE_SIGHT_UPGRADE_CORE.get(), 1),
                        cost(ModItems.ILLUSION_PRISM.get(), 2),
                        cost(ModItems.TRUE_SIGHT_CORE.get(), 1));
                case 4 -> List.of(cost(ModItems.TRUE_SIGHT_UPGRADE_CORE.get(), 2),
                        cost(ModItems.DECOY_SHARD.get(), 2),
                        cost(ModItems.OATHBOUND_ALLOY.get(), 1));
                default -> List.of();
            };
        };
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
    }
}
