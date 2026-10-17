package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
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

/** 研究拓片:守护者结算与谜题奖励箱产出的研究材料。 */
public class ResearchRubbingItem extends Item {

    /** 拓片种类:wind / tide / true_sight / gravity / void。 */
    private final String kind;

    /** 单次阅读可推进的研究等级数(研究拓片=1,誊清后的残篇页=2);逐级校验并扣除媒材。 */
    private final int points;

    public ResearchRubbingItem(String kind, Properties properties) {
        this(kind, 1, properties);
    }

    public ResearchRubbingItem(String kind, int points, Properties properties) {
        super(properties);
        this.kind = kind;
        this.points = Math.max(1, points);
    }

    public String getKind() {
        return this.kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(this)) {
            return InteractionResultHolder.fail(stack);
        }
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
                    recycleSaturated(serverPlayer, stack);
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
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
                EchoAbilityManager.getData(serverPlayer).addResearchPoints(line, 1);
                int appliedPoints = 1;

                for (int extra = 1; extra < this.points; extra++) {
                    int reached = EchoResearchManager.getResearchLevel(serverPlayer, line);
                    if (reached >= EchoResearchManager.MAX_RESEARCH_LEVEL) {
                        break;
                    }
                    int next = reached + 1;
                    if (!serverPlayer.getAbilities().instabuild
                            && firstMissingCost(serverPlayer, line, next) != null) {
                        break;
                    }
                    consumeCosts(serverPlayer, line, next);
                    EchoAbilityManager.getData(serverPlayer).addResearchPoints(line, 1);
                    appliedPoints++;
                }
                int preservedPoints = 0;
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                    preservedPoints = this.points - appliedPoints;
                    if (preservedPoints > 0) {
                        ItemStack preserved = new ItemStack(baseRubbing(line), preservedPoints);
                        if (!serverPlayer.getInventory().add(preserved)) {
                            serverPlayer.drop(preserved, false);
                        }
                    }
                }
                EchoResearchManager.completeResearchUpdate(serverPlayer);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
                Component researchName = Component.translatable(
                        "research.unknown_echoes." + line.getId());
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.research_rubbing.absorbed",
                        researchName, EchoResearchManager.getResearchLevel(serverPlayer, line)), true);
            }
        }
        // 退还的剩余研究价值可能已占用原手持槽，按结算后的栈完成使用。
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.research_rubbing." + this.kind,
                        Component.keybind("key.use"))
                .withStyle(ChatFormatting.GRAY));
        if (this.points > 1) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.research_rubbing.condensed")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.unknown_echoes.research_rubbing.preserved")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
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

    /** 拓片阅读按"即将提升到的等级"扣媒材;古代残页仍作为稀有探索理解,不在这里扣。 */
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
                        cost(ModItems.BLACK_TIDE_THREAD.get(), 1));
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
                        cost(ModItems.BROKEN_REFLECTION.get(), 1));
                case 4 -> List.of(cost(ModItems.TRUE_SIGHT_UPGRADE_CORE.get(), 2),
                        cost(ModItems.DECOY_SHARD.get(), 2),
                        cost(ModItems.OATHBOUND_ALLOY.get(), 1));
                default -> List.of();
            };
            case GRAVITY -> switch (nextLevel) {
                case 1 -> List.of(cost(ModItems.GRAVITY_CORE_FRAGMENT.get(), 1),
                        cost(ModItems.DEEP_ORE_SCRAP.get(), 2));
                case 2 -> List.of(cost(ModItems.GRAVITY_CORE_FRAGMENT.get(), 1),
                        cost(ModItems.SPACE_ANCHOR_SHARD.get(), 2),
                        cost(ModItems.DEEP_ORE_SCRAP.get(), 2));
                case 3 -> List.of(cost(ModItems.GRAVITY_CORE_FRAGMENT.get(), 2),
                        cost(ModItems.DEEP_ALLOY_INGOT.get(), 1),
                        cost(ModItems.SPACE_ANCHOR_SHARD.get(), 2));
                case 4 -> List.of(cost(ModItems.GRAVITY_CORE_FRAGMENT.get(), 2),
                        cost(ModItems.DEEP_ALLOY_INGOT.get(), 2),
                        cost(ModItems.VOID_RIFT_SHARD.get(), 1));
                default -> List.of();
            };
            case VOID -> switch (nextLevel) {
                case 1 -> List.of(cost(ModItems.VOID_RIFT_SHARD.get(), 1),
                        cost(ModItems.SPACE_ANCHOR_SHARD.get(), 2));
                case 2 -> List.of(cost(ModItems.VOID_RIFT_SHARD.get(), 1),
                        cost(ModItems.DEEP_ORE_SCRAP.get(), 2),
                        cost(ModItems.SPACE_ANCHOR_SHARD.get(), 2));
                case 3 -> List.of(cost(ModItems.VOID_RIFT_SHARD.get(), 2),
                        cost(ModItems.DEEP_ALLOY_INGOT.get(), 1),
                        cost(ModItems.SPACE_ANCHOR_SHARD.get(), 2));
                case 4 -> List.of(cost(ModItems.VOID_RIFT_SHARD.get(), 2),
                        cost(ModItems.DEEP_ALLOY_INGOT.get(), 2),
                        cost(ModItems.GRAVITY_CORE_FRAGMENT.get(), 1));
                default -> List.of();
            };
        };
    }

    private static Item baseRubbing(EchoResearchLine line) {
        return switch (line) {
            case WIND -> ModItems.WIND_RESEARCH_RUBBING.get();
            case TIDE -> ModItems.TIDE_RESEARCH_RUBBING.get();
            case TRUE_SIGHT -> ModItems.TRUE_SIGHT_RESEARCH_RUBBING.get();
            case GRAVITY -> ModItems.GRAVITY_RESEARCH_RUBBING.get();
            case VOID -> ModItems.VOID_RESEARCH_RUBBING.get();
        };
    }

    private void recycleSaturated(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.research_rubbing.saturated"), true);
            return;
        }
        stack.shrink(1);
        ItemStack recycled = new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), this.points);
        if (!player.getInventory().add(recycled)) {
            player.drop(recycled, false);
        }
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.research_rubbing.recycled"), true);
    }

    private static int countItem(ServerPlayer player, Item item) {
        return InventoryPayment.count(player, item);
    }


    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        InventoryPayment.consume(player, item, amount);
    }

}
