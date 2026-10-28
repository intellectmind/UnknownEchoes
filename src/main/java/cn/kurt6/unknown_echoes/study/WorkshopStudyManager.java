package cn.kurt6.unknown_echoes.study;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleCoreBlockEntity;
import cn.kurt6.unknown_echoes.journal.RewardCapacity;
import cn.kurt6.unknown_echoes.network.StudyActionPayload;
import cn.kurt6.unknown_echoes.network.StudyCollectionPayload;
import cn.kurt6.unknown_echoes.network.StudySyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class WorkshopStudyManager {
    private static final class Entry {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final WonderPuzzleCoreBlockEntity core;
        private final WorkshopStudySession session;
        private StudyActionPayload pending;

        private Entry(ResourceKey<Level> dimension, BlockPos pos, WonderPuzzleCoreBlockEntity core,
                      WorkshopStudySession session) {
            this.dimension = dimension;
            this.pos = pos;
            this.core = core;
            this.session = session;
        }

        private ResourceKey<Level> dimension() { return dimension; }
        private BlockPos pos() { return pos; }
        private WonderPuzzleCoreBlockEntity core() { return core; }
        private WorkshopStudySession session() { return session; }
    }
    private static final Map<UUID, Entry> SESSIONS = new HashMap<>();

    private WorkshopStudyManager() { }

    public static void open(ServerPlayer player, WonderPuzzleCoreBlockEntity core, int theme) {
        if (!WorkshopStudyCatalog.valid(theme, 0)) return;
        var entry = new Entry(player.level().dimension(), core.getBlockPos().immutable(), core,
                new WorkshopStudySession(theme, 0));
        if (!valid(player, entry)) return;
        SESSIONS.put(player.getUUID(), entry);
        send(player, entry.session(), true);
    }

    private static boolean valid(ServerPlayer player, Entry entry) {
        return player.isAlive() && !player.isSpectator() && player.level().dimension().equals(entry.dimension())
                && player.distanceToSqr(entry.pos().getX() + 0.5, entry.pos().getY() + 0.5, entry.pos().getZ() + 0.5) <= 64
                && player.level().hasChunkAt(entry.pos()) && player.level().getBlockEntity(entry.pos()) == entry.core()
                && entry.core().studyAvailable(player, entry.session().theme());
    }

    private static int mask(ServerPlayer player, boolean claimed) {
        return WorkshopStudyCatalog.mask(EchoAbilityManager.getData(player).getActivatedMechanisms(), claimed);
    }

    public static void syncCollection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StudyCollectionPayload(mask(player, false), mask(player, true)));
    }

    private static void send(ServerPlayer player, WorkshopStudySession session, boolean opening) {
        PacketDistributor.sendToPlayer(player, new StudySyncPayload(session.id(), session.revision(),
                Arrays.stream(session.board()).boxed().toList(),
                List.of(session.theme(), session.preset(), session.moves(),
                        WorkshopStudyCatalog.optimal(session.theme(), session.preset()), opening ? 1 : 0),
                mask(player, false), mask(player, true)));
    }

    public static void action(ServerPlayer player, StudyActionPayload action) {
        Entry entry = SESSIONS.get(player.getUUID());
        if (entry == null || !entry.session().id().equals(action.session())) return;
        if (action.operation() == WorkshopStudySession.CLOSE) {
            close(player);
            return;
        }
        WorkshopStudySession session = entry.session();
        // 冻结世界不暂停研习交互，接纳与延迟队列须共用服务器处理 tick。
        long now = player.server.getTickCount();
        if (!session.allowRequest(now)) {
            if (entry.pending == null) entry.pending = action;
            return;
        }
        entry.pending = null;
        if (!valid(player, entry)) {
            close(player);
            return;
        }
        applyAndSend(player, entry, action, now);
    }

    private static void applyAndSend(ServerPlayer player, Entry entry, StudyActionPayload action, long now) {
        WorkshopStudySession session = entry.session();
        try {
            if (session.accept(action.session(), action.revision(), now)) {
                // 先保存已完成评价，再接受换题，避免容量暂满时静默丢失未保存的成绩。
                if (record(player, entry)) {
                    if (action.operation() == WorkshopStudySession.CLAIM) claim(player, entry);
                    else {
                        session.apply(action.operation(), action.first(), action.second());
                        record(player, entry);
                    }
                }
            }
        } finally {
            // 进度回调失败后也解除客户端等待；已关闭或替换的会话不能再回传旧棋盘。
            if (SESSIONS.get(player.getUUID()) == entry) send(player, session, false);
        }
    }

    private static boolean validateAfterCallback(ServerPlayer player, Entry entry) {
        if (SESSIONS.get(player.getUUID()) != entry) return false;
        if (!valid(player, entry)) {
            close(player);
            return false;
        }
        return true;
    }

    private static boolean record(ServerPlayer player, Entry entry) {
        WorkshopStudySession session = entry.session();
        int fresh = session.earnedMask() & ~mask(player, false);
        if (fresh == 0) return true;
        List<String> keys = new ArrayList<>();
        for (int tier = 0; tier < 3; tier++) {
            if ((fresh & WorkshopStudyCatalog.bit(session.theme(), tier)) != 0) {
                keys.add(WorkshopStudyCatalog.key(session.theme(), tier, false));
            }
        }
        boolean recorded;
        try {
            recorded = EchoAbilityManager.activateMechanisms(player, keys.toArray(String[]::new));
        } finally {
            validateAfterCallback(player, entry);
        }
        if (!recorded || SESSIONS.get(player.getUUID()) != entry) return false;
        syncCollection(player);
        return true;
    }

    private static void claim(ServerPlayer player, Entry entry) {
        int theme = entry.session().theme();
        int pending = mask(player, false) & ~mask(player, true) & WorkshopStudyCatalog.themeMask(theme);
        if (pending == 0) return;
        List<ItemStack> rewards = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (int tier = 0; tier < 3; tier++) {
            if ((pending & WorkshopStudyCatalog.bit(theme, tier)) == 0) continue;
            var reward = WorkshopStudyCatalog.reward(theme, tier);
            rewards.add(new ItemStack(BuiltInRegistries.ITEM.get(UnknownEchoes.id(reward.item())), reward.count()));
            if (tier == 2) {
                var particle = WorkshopStudyCatalog.particleReward(player.getUUID(), theme);
                rewards.add(new ItemStack(BuiltInRegistries.ITEM.get(UnknownEchoes.id(particle.item())), particle.count()));
            }
            keys.add(WorkshopStudyCatalog.key(theme, tier, true));
        }
        if (!RewardCapacity.canFitAll(player, rewards)) {
            player.displayClientMessage(Component.translatable("journey.unknown_echoes.need_space"), true);
            return;
        }
        try {
            if (!EchoAbilityManager.activateMechanisms(player, () -> {
                for (ItemStack reward : rewards) player.getInventory().add(reward);
            }, keys.toArray(String[]::new))) return;
        } finally {
            validateAfterCallback(player, entry);
        }
        if (SESSIONS.get(player.getUUID()) == entry) syncCollection(player);
    }

    private static void close(ServerPlayer player) {
        Entry entry = SESSIONS.remove(player.getUUID());
        if (entry != null) PacketDistributor.sendToPlayer(player, new StudySyncPayload(entry.session().id(),
                entry.session().revision(), List.of(), List.of(), mask(player, false), mask(player, true)));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entry entry = SESSIONS.get(player.getUUID());
        if (entry == null) return;
        if (!valid(player, entry)) close(player);
        else {
            long now = player.server.getTickCount();
            if (entry.pending != null && entry.session().allowRequest(now)) {
                StudyActionPayload action = entry.pending;
                entry.pending = null;
                applyAndSend(player, entry, action, now);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) close(player);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
}
