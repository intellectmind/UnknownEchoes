package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.journal.RewardCapacity;
import cn.kurt6.unknown_echoes.network.ChaseCollectionPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class EchoChaseManager {
    public static final String MOB_TAG = "UnknownEchoesEchoChase";
    private record Entry(ServerLevel world, BlockPos pos, WonderTrialBlockEntity core, int theme,
                         EchoChaseSession session, Map<UUID, Mob> guards) { }
    private static final Map<UUID, Entry> SESSIONS = new HashMap<>();
    private EchoChaseManager() { }

    private static long now(ServerPlayer player) { return player.serverLevel().getServer().overworld().getGameTime(); }
    private static void tell(ServerPlayer player, String suffix, boolean actionbar, Object... args) {
        player.displayClientMessage(Component.translatable("chase.unknown_echoes." + suffix, args), actionbar);
    }
    private static boolean valid(ServerPlayer player, Entry entry) {
        return player.isAlive() && !player.isSpectator() && player.serverLevel() == entry.world()
                && entry.world().getDifficulty() != Difficulty.PEACEFUL
                && player.distanceToSqr(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5) <= 18 * 18
                && entry.world().hasChunkAt(entry.pos()) && entry.world().getBlockEntity(entry.pos()) == entry.core()
                && entry.core().chaseTheme() == entry.theme() && entry.core().chaseAvailable(player);
    }
    private static int mask(ServerPlayer player, boolean claimed) {
        return EchoChaseCatalog.mask(EchoAbilityManager.getData(player).getActivatedMechanisms(), claimed);
    }
    public static void syncCollection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ChaseCollectionPayload(mask(player, false), mask(player, true)));
    }

    public static void interact(ServerPlayer player, WonderTrialBlockEntity core) {
        BlockPos pos = core.getBlockPos();
        if (!player.isAlive() || player.isSpectator() || !core.chaseAvailable(player)
                || !player.serverLevel().hasChunkAt(pos) || player.serverLevel().getBlockEntity(pos) != core
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 64) return;
        Entry active = SESSIONS.get(player.getUUID());
        if (active != null && (!valid(player, active) || active.session().expired(now(player)))) {
            close(player.getUUID());
            active = SESSIONS.get(player.getUUID());
        }
        if (active != null) {
            if (active.session().complete()) finish(player, active);
            else tell(player, "active", true, active.session().wave() + 1, active.session().left(), active.session().secondsLeft(now(player)));
            return;
        }
        int theme = core.chaseTheme();
        if ((mask(player, false) & EchoChaseCatalog.bit(theme)) != 0) {
            claim(player, theme);
            collection(player, theme);
            return;
        }
        if (!player.isShiftKeyDown()) {
            tell(player, "entry", false);
            tell(player, "rule", false);
            collection(player, theme);
            return;
        }
        if (player.serverLevel().getDifficulty() == Difficulty.PEACEFUL) { tell(player, "peaceful", true); return; }
        for (Entry entry : SESSIONS.values()) {
            if (entry.world() == player.serverLevel() && entry.pos().equals(pos)) { tell(player, "occupied", true); return; }
        }
        Entry entry = new Entry(player.serverLevel(), pos.immutable(), core, theme,
                new EchoChaseSession(now(player)), new HashMap<>());
        if (!EchoAbilityManager.activateMechanisms(player,
                () -> SESSIONS.put(player.getUUID(), entry), EchoChaseCatalog.reservation(theme))) {
            tell(player, "capacity", true);
            return;
        }
        if (SESSIONS.get(player.getUUID()) != entry) return;
        EchoAbilityManager.syncToClient(player);
        if (SESSIONS.get(player.getUUID()) != entry) return;
        if (!valid(player, entry)) { close(player.getUUID()); return; }
        tell(player, "rule", false);
        tell(player, "started", false);
    }

    private static void collection(ServerPlayer player, int theme) {
        tell(player, "collection", false, Component.translatable("structure.unknown_echoes." + EchoChaseCatalog.IDS.get(theme)),
                Component.translatable("chase.unknown_echoes." + EchoChaseCatalog.status(theme, mask(player, false), mask(player, true))));
    }

    private static void claim(ServerPlayer player, int theme) {
        if (!player.isAlive() || player.isSpectator()) return;
        int pending = mask(player, false) & ~mask(player, true) & EchoChaseCatalog.bit(theme);
        if (pending == 0) return;
        List<ItemStack> rewards = new ArrayList<>();
        for (var reward : EchoChaseCatalog.rewards(theme)) {
            rewards.add(new ItemStack(BuiltInRegistries.ITEM.get(UnknownEchoes.id(reward.item())), reward.count()));
        }
        if (!RewardCapacity.canFitAll(player, rewards)) { tell(player, "need_space", true); return; }
        if (!EchoAbilityManager.activateMechanisms(player, () -> {
            for (ItemStack reward : rewards) player.getInventory().add(reward);
        }, EchoChaseCatalog.key(theme, true))) { tell(player, "capacity", true); return; }
        EchoAbilityManager.syncToClient(player);
        tell(player, "claimed", false);
    }

    private static void finish(ServerPlayer player, Entry entry) {
        if (SESSIONS.get(player.getUUID()) != entry) return;
        Map<String, String> replacements = Map.of(EchoChaseCatalog.reservation(entry.theme()), EchoChaseCatalog.key(entry.theme(), false));
        try {
            if (!EchoAbilityManager.completeReservedMechanisms(player,
                    () -> SESSIONS.remove(player.getUUID(), entry), replacements)) { tell(player, "capacity", true); return; }
        } finally {
            if (SESSIONS.get(player.getUUID()) != entry) discardGuards(entry);
        }
        EchoAbilityManager.syncToClient(player);
        tell(player, "cleared", false, (entry.session().elapsed(now(player)) + 19) / 20);
        claim(player, entry.theme());
    }

    private static void close(UUID owner) {
        Entry entry = SESSIONS.remove(owner);
        if (entry != null) discardGuards(entry);
    }

    private static void discardGuards(Entry entry) {
        for (Mob mob : List.copyOf(entry.guards().values())) mob.discard();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entry entry = SESSIONS.get(player.getUUID());
        if (entry == null) return;
        long time = now(player);
        if (!valid(player, entry) || entry.session().expired(time)) {
            close(player.getUUID());
            if (!SESSIONS.containsKey(player.getUUID())) tell(player, "ended", false);
            return;
        }
        if (entry.session().complete()) { finish(player, entry); return; }
        for (Mob mob : entry.guards().values()) {
            if (mob.isRemoved() || !entry.world().hasChunkAt(mob.blockPosition())) {
                close(player.getUUID());
                if (!SESSIONS.containsKey(player.getUUID())) tell(player, "ended", false);
                return;
            }
        }
        int wave = entry.session().wave();
        var impact = entry.session().resolve(time);
        if (impact != null) {
            pulse(entry.world(), impact, 10);
            if (!canContinueTick(player, entry, wave)) return;
            if (impact.contains(player.getX(), player.getY(), player.getZ())) {
                player.hurt(player.damageSources().generic(), 8.0F);
                if (!canContinueTick(player, entry, wave)) return;
                tell(player, "hit", true);
            }
        }
        if (!canContinueTick(player, entry, wave)) return;
        var warning = entry.session().warn(time, player.getX(), player.getY(), player.getZ());
        if (warning != null) {
            pulse(entry.world(), warning, EchoChaseSession.WARNING_TICKS);
            if (!canContinueTick(player, entry, wave)) return;
            entry.world().playSound(null, BlockPos.containing(warning.x(), warning.y(), warning.z()), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.7F, 1.6F);
            if (!canContinueTick(player, entry, wave)) return;
            tell(player, "warning", true);
            if (!canContinueTick(player, entry, wave)) return;
        }
        if (time % 40 == 0 && entry.session().strike() == null) {
            tell(player, entry.session().resting(time) ? "rest" : "active", true,
                    entry.session().wave() + 1, entry.session().left(), entry.session().secondsLeft(time));
        }
        if (time % EchoChaseSession.SPAWN_INTERVAL_TICKS == 0 && entry.session().canSpawn(time)) spawn(player, entry, time);
    }

    private static boolean canContinueTick(ServerPlayer player, Entry entry, int wave) {
        return SESSIONS.get(player.getUUID()) == entry && entry.session().wave() == wave && !entry.session().complete()
                && valid(player, entry) && !entry.session().expired(now(player));
    }

    private static void pulse(ServerLevel world, EchoChaseSession.Strike strike, int duration) {
        CombatFx.emit(world, CombatFxPayload.Shape.RING, new Vec3(strike.x(), strike.y() + 0.06, strike.z()), Vec3.ZERO,
                EchoChaseSession.RADIUS, 0, 0, 0xFFCE70, 0xEA7458, duration, false);
    }

    private static void spawn(ServerPlayer player, Entry entry, long time) {
        if (SESSIONS.get(player.getUUID()) != entry || !valid(player, entry) || entry.session().expired(now(player))) return;
        int wave = entry.session().wave();
        Mob mob = entry.core().createGuard(entry.world(), wave, entry.session().spawned());
        if (mob == null) return;
        boolean reserved = false;
        boolean added = false;
        try {
            if (!entry.core().positionGuard(entry.world(), mob)) {
                tell(player, "space", true);
                return;
            }
            mob.finalizeSpawn(entry.world(), entry.world().getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
            if (mob instanceof Zombie zombie) zombie.setBaby(false);
            for (EquipmentSlot slot : EquipmentSlot.values()) mob.setItemSlot(slot, ItemStack.EMPTY);
            if (!EntitySpawnSafety.isLoadedAndCollisionFree(entry.world(), mob) && !entry.core().positionGuard(entry.world(), mob)) {
                tell(player, "space", true);
                return;
            }
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (health == null || attack == null) return;
            health.setBaseValue(entry.session().health());
            attack.setBaseValue(entry.session().attack());
            mob.setHealth(mob.getMaxHealth());
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            mob.restrictTo(entry.pos(), 10);
            mob.setTarget(player);
            mob.setCustomName(Component.translatable("chase.unknown_echoes.guard", wave + 1));
            CompoundTag marker = new CompoundTag();
            marker.putUUID("Owner", player.getUUID());
            marker.putUUID("Run", entry.session().id());
            mob.getPersistentData().put(MOB_TAG, marker);
            if (SESSIONS.get(player.getUUID()) != entry || entry.session().wave() != wave || !mob.isAlive()
                    || !valid(player, entry) || entry.session().expired(now(player))) return;
            // 同步添加回调可能立刻击败守卫；名册先登记，失败只回滚当前会话仍存活的预留。
            if (!entry.session().reserve(mob.getUUID(), time)) return;
            reserved = true;
            entry.guards().put(mob.getUUID(), mob);
            added = entry.world().addFreshEntity(mob);
        } finally {
            // 初始化也可能抛错；清理只释放本次已登记且仍存活的预留。
            if (!added) {
                if (reserved) {
                    if (entry.session().cancel(entry.session().id(), mob.getUUID())) entry.guards().remove(mob.getUUID());
                }
                mob.discard();
            } else if (SESSIONS.get(player.getUUID()) != entry || !entry.session().remaining().contains(mob.getUUID())) {
                mob.discard();
            }
        }
    }

    private static Entry owned(Mob mob) {
        CompoundTag marker = mob.getPersistentData().getCompound(MOB_TAG);
        if (!marker.hasUUID("Owner") || !marker.hasUUID("Run")) return null;
        Entry entry = SESSIONS.get(marker.getUUID("Owner"));
        return entry != null && entry.world() == mob.level() && entry.session().id().equals(marker.getUUID("Run"))
                && entry.guards().get(mob.getUUID()) == mob && entry.session().remaining().contains(mob.getUUID()) ? entry : null;
    }

    // 掉落事件位于可取消的死亡判定之后，取消死亡不会推进挑战。
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)
                || !mob.getPersistentData().contains(MOB_TAG)) return;
        event.getDrops().clear();
        event.setCanceled(true);
        Entry entry = owned(mob);
        if (entry != null) {
            ServerPlayer owner = entry.world().getServer().getPlayerList().getPlayer(mob.getPersistentData().getCompound(MOB_TAG).getUUID("Owner"));
            if (owner != null && valid(owner, entry) && entry.session().defeated(entry.session().id(), mob.getUUID(), now(owner))) {
                entry.guards().remove(mob.getUUID());
                if (entry.session().complete()) finish(owner, entry);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void experience(LivingExperienceDropEvent event) {
        if (event.getEntity().getPersistentData().contains(MOB_TAG)) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void guardTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)
                || !mob.getPersistentData().contains(MOB_TAG)) return;
        Entry entry = owned(mob);
        if (entry == null) { mob.discard(); return; }
        if (mob.isAlive() && mob.tickCount % 20 == 0 && entry.world().hasChunkAt(entry.pos())
                && mob.distanceToSqr(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5) > 20 * 20) {
            entry.core().positionGuard(entry.world(), mob);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { close(event.getEntity().getUUID()); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { close(event.getEntity().getUUID()); }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
}
