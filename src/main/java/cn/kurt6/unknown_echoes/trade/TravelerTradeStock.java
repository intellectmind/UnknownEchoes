package cn.kurt6.unknown_echoes.trade;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** 旅者自身持有的成交记录，随实体保存；重载交易表不补货。 */
public final class TravelerTradeStock {
    private final Map<String, Integer> uses = new HashMap<>();

    public int getUses(EchoTrade trade) {
        return uses.getOrDefault(trade.stockKey(), 0);
    }

    /** 仅在扣款和交付均成功后登记，无限库存交易无需记账。 */
    public void recordUse(EchoTrade trade) {
        int current = getUses(trade);
        if (trade.maxUses() >= 0 && current < trade.maxUses()) {
            uses.put(trade.stockKey(), current + 1);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        uses.forEach(tag::putInt);
        return tag;
    }

    public void load(CompoundTag tag) {
        uses.clear();
        for (String key : tag.getAllKeys()) {
            if (tag.contains(key, Tag.TAG_INT) && tag.getInt(key) > 0) {
                uses.put(key, tag.getInt(key));
            }
        }
    }
}
