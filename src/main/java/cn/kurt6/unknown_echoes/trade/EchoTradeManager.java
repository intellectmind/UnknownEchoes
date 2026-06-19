package cn.kurt6.unknown_echoes.trade;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.entity.mob.LostTraveler;
import cn.kurt6.unknown_echoes.network.TradeListPayload;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 迷途旅者交易框架(V0.6D):交易表数据驱动(data/unknown_echoes/echo_trades/*.json,
 * 数据包可覆盖),成交校验全在服务端;客户端只收展示行,购买只发请求(5.8 边界)。
 * 库存(max_uses)按旅者实例在内存中记账:营地本就随时间更换,旧库存随旅者离开作废。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class EchoTradeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    /** 迷途旅者使用的交易表 id。 */
    public static final ResourceLocation LOST_TRAVELER_TABLE = UnknownEchoes.id("lost_traveler");
    /** 交易有效距离(服务端校验购买请求)。 */
    public static final double TRADE_RANGE = 6.0D;

    private static final Map<ResourceLocation, List<EchoTrade>> TABLES = new HashMap<>();
    /** 旅者实例 → (交易序号 → 已成交次数)。内存记账,随旅者离开/重启作废。 */
    private static final Map<UUID, Map<Integer, Integer>> USES = new HashMap<>();

    public EchoTradeManager() {
        super(GSON, "echo_trades");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EchoTradeManager());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        TABLES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), "echo trade table");
                List<EchoTrade> trades = new ArrayList<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(root, "trades")) {
                    trades.add(EchoTrade.fromJson(GsonHelper.convertToJsonObject(element, "trade")));
                }
                TABLES.put(entry.getKey(), List.copyOf(trades));
            } catch (Exception e) {
                UnknownEchoes.LOGGER.error("无法解析交易表 {}: {}", entry.getKey(), e.getMessage());
            }
        }
        UnknownEchoes.LOGGER.info("已加载 {} 张回响交易表", TABLES.size());
    }

    /** 玩家交易阶段 = 已解锁能力数(9.2:库存随能力数缓慢升级)。 */
    public static int getStage(ServerPlayer player) {
        return EchoAbilityManager.getData(player).getUnlockedAbilities().size();
    }

    /** 当前玩家可见的交易列表(按阶段过滤;顺序稳定,客户端按序号回传)。 */
    public static List<EchoTrade> getVisibleTrades(ServerPlayer player) {
        List<EchoTrade> all = TABLES.getOrDefault(LOST_TRAVELER_TABLE, List.of());
        int stage = getStage(player);
        List<EchoTrade> visible = new ArrayList<>();
        for (EchoTrade trade : all) {
            if (trade.isVisibleTo(player, stage)) {
                visible.add(trade);
            }
        }
        return visible;
    }

    /** 下发交易表并让客户端打开总览交易页(旅者右键入口)。 */
    public static void openTrades(ServerPlayer player, LostTraveler traveler) {
        List<EchoTrade> visible = getVisibleTrades(player);
        List<String> lines = new ArrayList<>();
        Map<Integer, Integer> used = USES.getOrDefault(traveler.getUUID(), Map.of());
        for (int i = 0; i < visible.size(); i++) {
            EchoTrade trade = visible.get(i);
            int usesLeft = trade.maxUses() < 0 ? -1
                    : Math.max(0, trade.maxUses() - used.getOrDefault(i, 0));
            lines.add(trade.encodeForClient(usesLeft));
        }
        PacketDistributor.sendToPlayer(player, new TradeListPayload(traveler.getId(), lines));
    }

    /** 购买请求(C2S):重建可见列表、校验距离/库存/货款后成交。 */
    public static void handleTradeRequest(ServerPlayer player, int entityId, int index) {
        if (!(player.level().getEntity(entityId) instanceof LostTraveler traveler)
                || !traveler.isAlive()
                || player.distanceToSqr(traveler) > TRADE_RANGE * TRADE_RANGE) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.too_far"), true);
            return;
        }
        List<EchoTrade> visible = getVisibleTrades(player);
        if (index < 0 || index >= visible.size()) {
            return;
        }
        EchoTrade trade = visible.get(index);
        Map<Integer, Integer> used = USES.computeIfAbsent(traveler.getUUID(), k -> new HashMap<>());
        if (trade.maxUses() >= 0 && used.getOrDefault(index, 0) >= trade.maxUses()) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.sold_out"), true);
            playNo(player, traveler);
            return;
        }
        for (EchoTrade.Cost cost : trade.costs()) {
            if (countItem(player, trade.getCostItemHolder(cost)) < cost.count()) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.trade.cant_afford"), true);
                playNo(player, traveler);
                return;
            }
        }
        ItemStack result = trade.makeResult();
        if (!canFitResultAfterCosts(player, trade, result)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.need_space"), true);
            playNo(player, traveler);
            return;
        }
        for (EchoTrade.Cost cost : trade.costs()) {
            consumeItem(player, trade.getCostItemHolder(cost), cost.count());
        }
        player.getInventory().add(result);
        used.merge(index, 1, Integer::sum);
        traveler.level().playSound(null, traveler.blockPosition(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.0F);
        // 成交后重发列表刷新库存显示
        openTrades(player, traveler);
    }

    private static void playNo(ServerPlayer player, LostTraveler traveler) {
        traveler.level().playSound(null, traveler.blockPosition(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
    }

    private static boolean canFitResultAfterCosts(ServerPlayer player, EchoTrade trade, ItemStack result) {
        List<ItemStack> mainSlots = copySlots(player.getInventory().items);
        List<ItemStack> offhandSlots = copySlots(player.getInventory().offhand);
        for (EchoTrade.Cost cost : trade.costs()) {
            int remaining = consumeFromSlots(mainSlots, trade.getCostItemHolder(cost), cost.count());
            consumeFromSlots(offhandSlots, trade.getCostItemHolder(cost), remaining);
        }
        return simulateInsert(mainSlots, result.copy());
    }

    private static List<ItemStack> copySlots(List<ItemStack> slots) {
        List<ItemStack> copied = new ArrayList<>();
        for (ItemStack slot : slots) {
            copied.add(slot.copy());
        }
        return copied;
    }

    private static int consumeFromSlots(List<ItemStack> slots, Item item, int amount) {
        for (ItemStack stack : slots) {
            if (amount <= 0) {
                return 0;
            }
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
        return amount;
    }

    private static boolean simulateInsert(List<ItemStack> slots, ItemStack stack) {
        int remaining = stack.getCount();
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int move = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    remaining -= move;
                }
            }
        }
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            if (slot.isEmpty()) {
                remaining -= stack.getMaxStackSize();
            }
        }
        return remaining <= 0;
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
