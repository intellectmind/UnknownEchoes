package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.ClueMapSearch;
import cn.kurt6.unknown_echoes.journal.ClueLocator;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
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
import javax.annotation.Nullable;

/** 线索地图（迷途旅者出售）：右键后定位附近已注册探索点并写入个人日志，之后由风暴罗盘指向或在日志中查看。 */
public class ClueMapItem extends Item {

    private static final int LOCATE_COOLDOWN_TICKS = 20;

    /** 无组件时的回声境域普通探索池(只含非 Boss 普通结构)。 */
    private static final List<String> REALM_POOL = List.of(
            "small_echo_ruin", "rune_puzzle_room", "silent_hut", "wind_eroded_tower",
            "tide_lighthouse_reef", "mirror_dust_cloister", "broken_archive");

    /** 主世界普通探索池:不指向入口信标等关键进度结构。 */
    private static final List<String> OVERWORLD_POOL = List.of(
            "small_echo_ruin", "overworld_lost_camp");

    /** 深层回响普通探索池:不指向首领场地或维度解锁节点。 */
    private static final List<String> DEEP_POOL = List.of(
            "folded_waystation", "inverted_archive_tower", "hushed_refuge",
            "gravity_shard_spire", "void_vein_chamber", "riftworn_memorial_court",
            "palimpsest_ecology_dome", "interlayer_expedition_ring");

    public ClueMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (ClueMapSearch.isSearching(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.searching"), true);
            return InteractionResultHolder.consume(stack);
        }
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.consume(stack);
        }
        serverPlayer.getCooldowns().addCooldown(this, LOCATE_COOLDOWN_TICKS);

        String bound = stack.get(ModDataComponents.CLUE_STRUCTURE.get());
        ResourceLocation target = bound == null ? null : parseBoundTarget(bound);
        ClueLocator.LocateOutcome outcome;
        if (bound != null) {
            outcome = target == null
                    ? new ClueLocator.LocateOutcome(ClueLocator.LocateResult.NOT_FOUND, null)
                    : new ClueLocator.LocateOutcome(
                            ClueLocator.locateAndAddClueResult(serverPlayer, target), target);
        } else {
            outcome = ClueLocator.locateAndAddAnyClue(serverPlayer,
                    resolvePool(serverPlayer), serverPlayer.getRandom());
        }
        if (outcome.result() == ClueLocator.LocateResult.NOT_FOUND) {
            List<ResourceLocation> candidates = bound == null
                    ? resolvePool(serverPlayer)
                    : target == null ? List.of() : List.of(target);
            ItemStack requestedMap = stack.copyWithCount(1);
            ClueMapSearch.RequestResult request = ClueMapSearch.request(serverPlayer, candidates, bound == null ? null : target,
                    (owner, clue) -> completeGeneratedLocate(owner, requestedMap, clue));
            if (request == ClueMapSearch.RequestResult.STARTED
                    || request == ClueMapSearch.RequestResult.ALREADY_SEARCHING) {
                if (ClueMapSearch.isSearching(serverPlayer)) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.clue_map.searching"), true);
                }
                // 已加载区块的搜索可能同步完成，回调也可能已经替换手持物。
                return InteractionResultHolder.consume(player.getItemInHand(hand));
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.unavailable"), true);
            return InteractionResultHolder.consume(stack);
        }
        completeLocate(serverPlayer, stack, bound, outcome);
        // 最后一张重复地图返还的拓印纸可能进入当前手持槽。
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    private static void completeLocate(ServerPlayer serverPlayer, ItemStack stack,
                                       String bound, ClueLocator.LocateOutcome outcome) {
        Level level = serverPlayer.level();
        if (outcome.result() == ClueLocator.LocateResult.ADDED) {
            // 新线索已写入，先完成支付，避免声音回调再次回收同一张地图。
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.unknown_echoes.clue_map.read",
                            Component.translatable("structure.unknown_echoes."
                                    + outcome.structureId().getPath())));
            level.playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (outcome.result() == ClueLocator.LocateResult.ALREADY_KNOWN && bound != null) {
            if (serverPlayer.getAbilities().instabuild) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.clue_map.known"), true);
                return;
            }
            stack.shrink(1);
            ItemStack refund = new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 2);
            if (!serverPlayer.getInventory().add(refund)) {
                serverPlayer.drop(refund, false);
            }
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.recovered"));
            level.playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.7F);
        } else if (outcome.result() == ClueLocator.LocateResult.ALREADY_KNOWN) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.known"), true);
        } else {

            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.clue_map.too_far"), true);
        }
    }

    /** 先确认背包仍有同组件地图，再把生成结果写入个人日志并结算。 */
    private static void completeGeneratedLocate(ServerPlayer serverPlayer,
                                                ItemStack requestedMap, @Nullable ExplorationClue clue) {
        if (clue == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.search_failed"), true);
            return;
        }
        ItemStack payment = findRequestedMap(serverPlayer, requestedMap);
        if (payment.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.clue_map.cancelled"), true);
            return;
        }
        boolean known = JournalManager.getClues(serverPlayer).stream()
                .anyMatch(existing -> existing.dimension().equals(clue.dimension())
                        && existing.structureId().equals(clue.structureId()));
        boolean added = !known && JournalManager.addClue(serverPlayer, clue);
        if (added) {
            JournalManager.syncJournal(serverPlayer);
        }
        completeLocate(serverPlayer, payment, requestedMap.get(ModDataComponents.CLUE_STRUCTURE.get()),
                new ClueLocator.LocateOutcome(added ? ClueLocator.LocateResult.ADDED
                        : ClueLocator.LocateResult.ALREADY_KNOWN, ResourceLocation.parse(clue.structureId())));
    }

    private static ItemStack findRequestedMap(ServerPlayer player, ItemStack requestedMap) {
        // 同组件地图允许拆分、合并或换槽，绑定目标和自定义名称不同的地图不能代扣。
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, requestedMap)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private ResourceLocation parseBoundTarget(String bound) {
        return ResourceLocation.tryParse(bound.contains(":") ? bound
                : UnknownEchoes.MODID + ":" + bound);
    }

    private List<ResourceLocation> resolvePool(ServerPlayer player) {
        ResourceLocation dimensionId = player.level().dimension().location();
        List<String> pool;
        if (dimensionId.equals(EchoPermission.ECHO_REALM_ID)) {
            pool = REALM_POOL;
        } else if (dimensionId.equals(EchoPermission.DEEP_ECHOES_ID)) {
            pool = DEEP_POOL;
        } else if (dimensionId.equals(Level.OVERWORLD.location())) {
            pool = OVERWORLD_POOL;
        } else {
            return List.of();
        }
        return pool.stream().map(UnknownEchoes::id).toList();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String bound = stack.get(ModDataComponents.CLUE_STRUCTURE.get());
        ResourceLocation target = bound == null ? null : parseBoundTarget(bound);
        if (target != null) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.clue_map.target",
                            Component.translatable("structure.unknown_echoes." + target.getPath()))
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.clue_map.hint")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }
}
