package cn.kurt6.unknown_echoes.trade;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.journal.AncientPageCatalog;
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
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record EchoTrade(List<Cost> costs, ResourceLocation resultItem, int resultCount,
                        int minStage, int maxUses, String requiredAbility,
                        int resultPageId, String resultClueStructure) {

    public static final int MAX_TRADES_PER_TABLE = 64;
    private static final int MAX_COSTS = 16;
    private static final int MAX_ITEM_COUNT = 4096;
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_CLUE_LENGTH = 256;
    private static final int MAX_ENCODED_LENGTH = 4096;
    private static final int MAX_STAGE = 64;
    private static final int MAX_USES = 100_000;

    public record Cost(ResourceLocation item, int count) {}

    public static EchoTrade fromJson(JsonObject json) {
        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        EchoTrade trade = new EchoTrade(
                parseCosts(json),
                parseResourceId(GsonHelper.getAsString(result, "item"), "trade result item"),
                GsonHelper.getAsInt(result, "count", 1),
                GsonHelper.getAsInt(json, "min_stage", 0),
                GsonHelper.getAsInt(json, "max_uses", -1),
                GsonHelper.getAsString(json, "required_ability", ""),
                parsePageId(result),
                parseClueStructure(result));
        trade.validate();
        return trade;
    }

    private void validate() {
        if (costs == null || resultClueStructure == null || requiredAbility == null
                || costs.isEmpty() || costs.size() > MAX_COSTS) {
            throw new IllegalArgumentException("trade costs must contain 1-16 entries");
        }
        for (Cost cost : costs) {
            if (cost.item() == null || cost.count() <= 0 || cost.count() > MAX_ITEM_COUNT
                    || BuiltInRegistries.ITEM.getOptional(cost.item()).orElse(Items.AIR) == Items.AIR) {
                throw new IllegalArgumentException("trade cost must reference a real item with a positive count");
            }
        }
        if (resultItem == null || resultCount <= 0 || resultCount > MAX_ITEM_COUNT
                || BuiltInRegistries.ITEM.getOptional(resultItem).orElse(Items.AIR) == Items.AIR) {
            throw new IllegalArgumentException("trade result must reference a real item with a positive count");
        }
        if (minStage < 0 || minStage > MAX_STAGE || maxUses < -1 || maxUses > MAX_USES) {
            throw new IllegalArgumentException("trade stage must be non-negative and uses must be at least -1");
        }
        if (resultPageId != 0 && !AncientPageCatalog.isReleasePage(resultPageId)) {
            throw new IllegalArgumentException("trade result page is unknown");
        }
        if (!resultClueStructure.isBlank()
                && (resultClueStructure.length() > MAX_CLUE_LENGTH
                || ResourceLocation.tryParse(resultClueStructure) == null)) {
            throw new IllegalArgumentException("trade clue structure must be a valid resource id");
        }
        if (!requiredAbility.isBlank() && EchoAbilityType.byId(requiredAbility) == null) {
            throw new IllegalArgumentException("trade required ability is unknown");
        }
        if (encodeForClient(Integer.MAX_VALUE).length() > MAX_ENCODED_LENGTH) {
            throw new IllegalArgumentException("trade display row is too long");
        }
    }

    private static List<Cost> parseCosts(JsonObject json) {
        List<Cost> parsed = new ArrayList<>();
        if (json.has("costs")) {
            JsonArray costs = GsonHelper.getAsJsonArray(json, "costs");
            for (JsonElement element : costs) {
                if (parsed.size() >= MAX_COSTS) {
                    throw new IllegalArgumentException("trade costs must contain 1-16 entries");
                }
                parsed.add(parseCost(GsonHelper.convertToJsonObject(element, "trade cost")));
            }
        } else {
            parsed.add(parseCost(GsonHelper.getAsJsonObject(json, "cost")));
        }
        return List.copyOf(parsed);
    }

    private static Cost parseCost(JsonObject cost) {
        return new Cost(parseResourceId(GsonHelper.getAsString(cost, "item"), "trade cost item"),
                GsonHelper.getAsInt(cost, "count", 1));
    }

    private static ResourceLocation parseResourceId(String value, String field) {
        if (value.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(field + " is too long");
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return id;
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
        return BuiltInRegistries.ITEM.getOptional(cost.item()).orElse(Items.AIR);
    }

    public ItemStack makeResult() {
        ItemStack stack = new ItemStack(
                BuiltInRegistries.ITEM.getOptional(resultItem).orElse(Items.AIR), resultCount);
        if (resultPageId > 0) {
            stack.set(ModDataComponents.PAGE_ID.get(), resultPageId);
        }
        if (!resultClueStructure.isBlank()) {
            stack.set(ModDataComponents.CLUE_STRUCTURE.get(), resultClueStructure);
        }
        return stack;
    }

    /** 按实际货款与产物识别库存；调序、阶段门槛或限购数量变更不抹掉成交记录。 */
    public String stockKey() {
        Map<String, Integer> normalizedCosts = new TreeMap<>();
        for (Cost cost : costs) {
            normalizedCosts.merge(cost.item().toString(), cost.count(), Integer::sum);
        }
        List<String> encodedCosts = new ArrayList<>();
        normalizedCosts.forEach((item, count) -> encodedCosts.add(item + "@" + count));
        return String.join(";", encodedCosts) + "|" + resultItem + "@" + resultCount
                + "#" + resultPageId + "~" + resultClueStructure;
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
