package cn.kurt6.unknown_echoes.trade;

import cn.kurt6.unknown_echoes.item.InventoryPayment;
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

/** 迷途旅者交易框架；交易表由数据包驱动，成交校验在服务端完成，客户端只展示并发送购买请求。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class EchoTradeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    public static final ResourceLocation LOST_TRAVELER_TABLE = UnknownEchoes.id("lost_traveler");
    /** 交易有效距离(服务端校验购买请求)。 */
    public static final double TRADE_RANGE = 6.0D;

    private static final Map<ResourceLocation, List<EchoTrade>> TABLES = new HashMap<>();
    /** 旅者实例 → (交易序号 → 已成交次数)。内存记账,随旅者离开/重启作废。 */
    private static final Map<UUID, Map<Integer, Integer>> USES = new HashMap<>();
    /** 玩家 → 上次下发列表的表内索引快照:点击按打开时看到的列表解析,阶段变化/重载不会错位买错行。 */
    private static final Map<UUID, TradeSession> SESSION_VIEW = new HashMap<>();

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
        USES.clear();
        SESSION_VIEW.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(entry.getValue(), "echo trade table");
                List<EchoTrade> trades = new ArrayList<>();
                var tradeElements = GsonHelper.getAsJsonArray(root, "trades");
                if (tradeElements.size() > EchoTrade.MAX_TRADES_PER_TABLE) {
                    throw new IllegalArgumentException("trade table contains too many trades");
                }
                for (JsonElement element : tradeElements) {
                    trades.add(EchoTrade.fromJson(GsonHelper.convertToJsonObject(element, "trade")));
                }
                TABLES.put(entry.getKey(), List.copyOf(trades));
            } catch (Exception e) {
                UnknownEchoes.LOGGER.error("无法解析交易表 {}: {}", entry.getKey(), e.getMessage());
            }
        }
        UnknownEchoes.LOGGER.info("已加载 {} 张回响交易表", TABLES.size());
    }

    /** 玩家交易阶段，等于已解锁能力数，用于逐步扩展交易库存。 */
    public static int getStage(ServerPlayer player) {
        return EchoAbilityManager.getData(player).getUnlockedAbilities().size();
    }

    /** 当前玩家可见的交易列表(按阶段过滤;顺序稳定,客户端按序号回传)。 */
    public static List<EchoTrade> getVisibleTrades(ServerPlayer player) {
        List<EchoTrade> visible = new ArrayList<>();
        for (IndexedTrade indexed : getVisibleTradeEntries(player)) {
            visible.add(indexed.trade());
        }
        return visible;
    }

    private static List<IndexedTrade> getVisibleTradeEntries(ServerPlayer player) {
        List<EchoTrade> all = TABLES.getOrDefault(LOST_TRAVELER_TABLE, List.of());
        int stage = getStage(player);
        List<IndexedTrade> visible = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < all.size(); tableIndex++) {
            EchoTrade trade = all.get(tableIndex);
            if (trade.isVisibleTo(player, stage)) {
                visible.add(new IndexedTrade(tableIndex, trade));
            }
        }
        return visible;
    }

    public static void openTrades(ServerPlayer player, LostTraveler traveler) {
        List<IndexedTrade> visible = getVisibleTradeEntries(player);
        List<String> lines = new ArrayList<>();
        List<Integer> view = new ArrayList<>();
        Map<Integer, Integer> used = USES.getOrDefault(traveler.getUUID(), Map.of());
        for (int i = 0; i < visible.size(); i++) {
            IndexedTrade indexed = visible.get(i);
            EchoTrade trade = indexed.trade();
            int usesLeft = trade.maxUses() < 0 ? -1
                    : Math.max(0, trade.maxUses() - used.getOrDefault(indexed.tableIndex(), 0));
            lines.add(trade.encodeForClient(usesLeft));
            view.add(indexed.tableIndex());
        }
        SESSION_VIEW.put(player.getUUID(), new TradeSession(traveler.getUUID(), List.copyOf(view)));
        PacketDistributor.sendToPlayer(player, new TradeListPayload(traveler.getId(), lines));
    }

    public static void handleTradeRequest(ServerPlayer player, int entityId, int index) {
        if (!(player.level().getEntity(entityId) instanceof LostTraveler traveler)
                || !traveler.isAlive()
                || player.distanceToSqr(traveler) > TRADE_RANGE * TRADE_RANGE) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.too_far"), true);
            return;
        }
        TradeSession session = SESSION_VIEW.get(player.getUUID());
        List<EchoTrade> all = TABLES.getOrDefault(LOST_TRAVELER_TABLE, List.of());
        if (session == null || !session.travelerId().equals(traveler.getUUID())
                || index < 0 || index >= session.view().size()) {
            return;
        }
        int tableIndex = session.view().get(index);
        if (tableIndex < 0 || tableIndex >= all.size()) {
            return;
        }
        EchoTrade trade = all.get(tableIndex);

        if (!trade.isVisibleTo(player, getStage(player))) {
            return;
        }
        Map<Integer, Integer> used = USES.computeIfAbsent(traveler.getUUID(), k -> new HashMap<>());
        if (trade.maxUses() >= 0 && used.getOrDefault(tableIndex, 0) >= trade.maxUses()) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.sold_out"), true);
            playNo(player, traveler);
            return;
        }
        if (!canAfford(player, trade)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.cant_afford"), true);
            playNo(player, traveler);
            return;
        }
        ItemStack result = trade.makeResult();
        if (!canFitResultAfterCosts(player, trade, result)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.need_space"), true);
            playNo(player, traveler);
            return;
        }
        List<ItemStack> originalMain = copySlots(player.getInventory().items);
        List<ItemStack> originalOffhand = copySlots(player.getInventory().offhand);
        for (EchoTrade.Cost cost : trade.costs()) {
            consumeItem(player, trade.getCostItemHolder(cost), cost.count());
        }
        if (!player.getInventory().add(result)) {
            restoreSlots(player.getInventory().items, originalMain);
            restoreSlots(player.getInventory().offhand, originalOffhand);
            player.getInventory().setChanged();
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.trade.need_space"), true);
            playNo(player, traveler);
            return;
        }
        used.merge(tableIndex, 1, Integer::sum);
        traveler.level().playSound(null, traveler.blockPosition(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.0F);

        openTrades(player, traveler);
    }

    public static void clearUses(UUID travelerId) {
        USES.remove(travelerId);
    }

    /** 服务器停止时清理旅者实例库存,避免跨世界保留旧实体 UUID。 */
    public static void clearAllUses() {
        USES.clear();
        SESSION_VIEW.clear();
    }

    public static void clearSessionView(UUID playerId) {
        SESSION_VIEW.remove(playerId);
    }

    private record IndexedTrade(int tableIndex, EchoTrade trade) {
    }

    private record TradeSession(UUID travelerId, List<Integer> view) {
    }

    private static void playNo(ServerPlayer player, LostTraveler traveler) {
        traveler.level().playSound(null, traveler.blockPosition(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
    }

    private static boolean canAfford(ServerPlayer player, EchoTrade trade) {
        Map<Item, Long> required = new HashMap<>();
        for (EchoTrade.Cost cost : trade.costs()) {
            if (cost.count() <= 0) {
                return false;
            }
            required.merge(trade.getCostItemHolder(cost), (long) cost.count(), Long::sum);
        }
        for (Map.Entry<Item, Long> entry : required.entrySet()) {
            if (countItem(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canFitResultAfterCosts(ServerPlayer player, EchoTrade trade, ItemStack result) {
        List<ItemStack> mainSlots = copySlots(player.getInventory().items);
        List<ItemStack> offhandSlots = copySlots(player.getInventory().offhand);
        for (EchoTrade.Cost cost : trade.costs()) {
            int remaining = consumeFromSlots(mainSlots, trade.getCostItemHolder(cost), cost.count());
            consumeFromSlots(offhandSlots, trade.getCostItemHolder(cost), remaining);
        }
        ItemStack remaining = result.copy();
        mergeIntoExisting(mainSlots, remaining);
        mergeIntoExisting(offhandSlots, remaining);
        return fillEmptySlots(mainSlots, remaining);
    }

    private static List<ItemStack> copySlots(List<ItemStack> slots) {
        List<ItemStack> copied = new ArrayList<>();
        for (ItemStack slot : slots) {
            copied.add(slot.copy());
        }
        return copied;
    }

    private static void restoreSlots(List<ItemStack> slots, List<ItemStack> snapshot) {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, snapshot.get(i));
        }
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

    private static void mergeIntoExisting(List<ItemStack> slots, ItemStack stack) {
        for (ItemStack slot : slots) {
            if (stack.isEmpty()) {
                return;
            }
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    stack.shrink(move);
                }
            }
        }
    }

    private static boolean fillEmptySlots(List<ItemStack> slots, ItemStack stack) {
        for (ItemStack slot : slots) {
            if (stack.isEmpty()) {
                return true;
            }
            if (slot.isEmpty()) {
                stack.shrink(Math.min(stack.getCount(), stack.getMaxStackSize()));
            }
        }
        return stack.isEmpty();
    }

    private static int countItem(ServerPlayer player, Item item) {
        return InventoryPayment.count(player, item);
    }


    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        InventoryPayment.consume(player, item, amount);
    }

}
