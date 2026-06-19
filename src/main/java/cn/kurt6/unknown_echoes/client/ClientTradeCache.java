package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.network.TradeListPayload;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端交易展示缓存(5.8:客户端只展示,购买只发请求)。
 * 服务端下发 TradeListPayload 时更新并打开总览交易页。
 */
public class ClientTradeCache {

    /** 单条交易展示行。usesLeft=-1 表示不限量。 */
    public record Entry(List<ItemStack> costs, ItemStack result, int usesLeft) {}

    private static int travelerEntityId = -1;
    private static final double CLIENT_TRADE_RANGE_SQR = 6.0D * 6.0D;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static void openFromServer(TradeListPayload payload) {
        travelerEntityId = payload.entityId();
        ENTRIES.clear();
        for (String line : payload.trades()) {
            try {
                ENTRIES.add(parseEntry(line));
            } catch (NumberFormatException ignored) {
            }
        }
        cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.openTrades();
    }

    private static Entry parseEntry(String line) {
        if (line.contains("|")) {
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                throw new NumberFormatException("bad trade row");
            }
            return new Entry(parseCosts(parts[0]), stackSpec(parts[1]), Integer.parseInt(parts[2]));
        }
        String[] parts = line.split(",");
        if (parts.length != 5) {
            throw new NumberFormatException("bad legacy trade row");
        }
        return new Entry(List.of(stack(parts[0], Integer.parseInt(parts[1]))),
                stack(parts[2], Integer.parseInt(parts[3])),
                Integer.parseInt(parts[4]));
    }

    private static List<ItemStack> parseCosts(String text) {
        List<ItemStack> costs = new ArrayList<>();
        for (String part : text.split(";")) {
            costs.add(stackSpec(part));
        }
        return List.copyOf(costs);
    }

    private static ItemStack stackSpec(String spec) {
        String[] parts = spec.split("@");
        if (parts.length != 2) {
            throw new NumberFormatException("bad stack spec");
        }
        String[] clueSplit = parts[1].split("~", 2);
        String[] countAndPage = clueSplit[0].split("#", 2);
        ItemStack stack = stack(parts[0], Integer.parseInt(countAndPage[0]));
        if (countAndPage.length == 2) {
            stack.set(ModDataComponents.PAGE_ID.get(), Integer.parseInt(countAndPage[1]));
        }
        if (clueSplit.length == 2 && !clueSplit[1].isBlank()) {
            stack.set(ModDataComponents.CLUE_STRUCTURE.get(), clueSplit[1]);
        }
        return stack;
    }

    private static ItemStack stack(String id, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(rl);
        return new ItemStack(item, Math.max(1, count));
    }

    public static int getTravelerEntityId() {
        return travelerEntityId;
    }

    public static List<Entry> getEntries() {
        return ENTRIES;
    }

    /** 是否存在进行中的交易会话(旅者还在跟前才有意义,展示层判断)。 */
    public static boolean hasSession() {
        clearIfTravelerUnavailable();
        return travelerEntityId >= 0 && !ENTRIES.isEmpty();
    }

    public static void clearSession() {
        travelerEntityId = -1;
        ENTRIES.clear();
    }

    private static void clearIfTravelerUnavailable() {
        if (travelerEntityId < 0 || ENTRIES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clearSession();
            return;
        }
        var traveler = minecraft.level.getEntity(travelerEntityId);
        if (traveler == null || !traveler.isAlive()
                || minecraft.player.distanceToSqr(traveler) > CLIENT_TRADE_RANGE_SQR) {
            clearSession();
        }
    }
}
