package cn.kurt6.unknown_echoes.trade;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条数据驱动交易(V0.6D 迷途旅者):data/unknown_echoes/echo_trades/*.json。
 * min_stage = 玩家已解锁能力数门槛(9.2:库存随已解锁能力数缓慢升级);
 * 交易表只允许普通材料与线索物,关键进度物不进 JSON(16.4 边界,服务端不再二次发放)。
 */
public record EchoTrade(List<Cost> costs, ResourceLocation resultItem, int resultCount,
                        int minStage, int maxUses, String requiredAbility,
                        int resultPageId, String resultClueStructure) {

    public record Cost(ResourceLocation item, int count) {}

    public static EchoTrade fromJson(JsonObject json) {
        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        return new EchoTrade(
                parseCosts(json),
                ResourceLocation.parse(GsonHelper.getAsString(result, "item")),
                GsonHelper.getAsInt(result, "count", 1),
                GsonHelper.getAsInt(json, "min_stage", 0),
                GsonHelper.getAsInt(json, "max_uses", -1),
                GsonHelper.getAsString(json, "required_ability", ""),
                parsePageId(result),
                parseClueStructure(result));
    }

    private static List<Cost> parseCosts(JsonObject json) {
        List<Cost> parsed = new ArrayList<>();
        if (json.has("costs")) {
            JsonArray costs = GsonHelper.getAsJsonArray(json, "costs");
            for (JsonElement element : costs) {
                parsed.add(parseCost(GsonHelper.convertToJsonObject(element, "trade cost")));
            }
        } else {
            parsed.add(parseCost(GsonHelper.getAsJsonObject(json, "cost")));
        }
        return List.copyOf(parsed);
    }

    private static Cost parseCost(JsonObject cost) {
        return new Cost(ResourceLocation.parse(GsonHelper.getAsString(cost, "item")),
                GsonHelper.getAsInt(cost, "count", 1));
    }

    private static int parsePageId(JsonObject result) {
        if (!result.has("components")) {
            return 0;
        }
        JsonObject components = GsonHelper.getAsJsonObject(result, "components");
        return GsonHelper.getAsInt(components, "unknown_echoes:page_id", 0);
    }

    private static String parseClueStructure(JsonObject result) {
        if (!result.has("components")) {
            return "";
        }
        JsonObject components = GsonHelper.getAsJsonObject(result, "components");
        return GsonHelper.getAsString(components, "unknown_echoes:clue_structure", "");
    }

    public boolean isVisibleTo(ServerPlayer player, int stage) {
        if (minStage > stage) {
            return false;
        }
        if (requiredAbility.isBlank()) {
            return true;
        }
        EchoAbilityType ability = EchoAbilityType.byId(requiredAbility);
        return ability != null && EchoAbilityManager.hasAbility(player, ability);
    }

    public Item getCostItemHolder(Cost cost) {
        return BuiltInRegistries.ITEM.get(cost.item());
    }

    public ItemStack makeResult() {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(resultItem), resultCount);
        if (resultPageId > 0) {
            stack.set(ModDataComponents.PAGE_ID.get(), resultPageId);
        }
        if (!resultClueStructure.isBlank()) {
            stack.set(ModDataComponents.CLUE_STRUCTURE.get(), resultClueStructure);
        }
        return stack;
    }

    /** 同步给客户端的展示行(纯展示;成交校验全在服务端)。 */
    public String encodeForClient(int usesLeft) {
        List<String> encodedCosts = new ArrayList<>();
        for (Cost cost : costs) {
            encodedCosts.add(cost.item() + "@" + cost.count());
        }
        String pageSuffix = resultPageId > 0 ? "#" + resultPageId : "";
        String clueSuffix = resultClueStructure.isBlank() ? "" : "~" + resultClueStructure;
        return String.join(";", encodedCosts) + "|" + resultItem + "@" + resultCount
                + pageSuffix + clueSuffix + "|" + usesLeft;
    }
}
