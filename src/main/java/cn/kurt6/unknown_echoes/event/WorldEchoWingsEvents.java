package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.network.WorldEchoFlightStatePayload;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 世界回响之翼的服务端飞行状态、装备校验与防护补偿。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class WorldEchoWingsEvents {
    private static final int FLIGHT_RESISTANCE_DURATION_TICKS = 60;
    private static final int FLIGHT_RESISTANCE_REFRESH_THRESHOLD_TICKS = 20;
    private static final int FLIGHT_RESISTANCE_AMPLIFIER = 1;
    private static final double TAKEOFF_VERTICAL_SPEED = 0.42D;
    public static final ResourceLocation FLIGHT_ATTRIBUTE_MODIFIER_ID =
            UnknownEchoes.id("world_echo_wings_flight");
    private static final Set<UUID> ACTIVE_FLIGHTS = new HashSet<>();
    private static final Set<UUID> VISIBLE_FLIGHTS = new HashSet<>();

    private WorldEchoWingsEvents() {
    }

    public static void handleFlightRequest(ServerPlayer player, boolean flying) {
        if (!flying) {
            stopFlight(player);
            return;
        }
        if (!canUseWings(player)) {
            stopFlight(player);
            PacketDistributor.sendToPlayer(player,
                    new WorldEchoFlightStatePayload(player.getUUID(), false));
            return;
        }
        if (!ACTIVE_FLIGHTS.add(player.getUUID())) {
            return;
        }

        setFlightAccess(player, true);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        var motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.max(motion.y, TAKEOFF_VERTICAL_SPEED), motion.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, 1.15F);
        syncVisibleState(player, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        boolean active = ACTIVE_FLIGHTS.contains(playerId);
        if (active && !canUseWings(player)) {
            stopFlight(player);
            active = false;
        }
        if (active) {
            setFlightAccess(player, true);
            if (!player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            player.fallDistance = 0.0F;
            if (player.tickCount % 10 == 0) {
                refreshFlightResistance(player);
            }
        }

        if (active || player.getAbilities().flying || VISIBLE_FLIGHTS.contains(playerId)) {
            syncVisibleState(player, hasUsableWings(player) && player.getAbilities().flying);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
                && hasUsableWings(player)) {
            event.setDamageMultiplier(0.0F);
            player.fallDistance = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
                && event.getTarget() instanceof ServerPlayer target) {
            PacketDistributor.sendToPlayer(observer,
                    new WorldEchoFlightStatePayload(target.getUUID(),
                            VISIBLE_FLIGHTS.contains(target.getUUID())));
        }
    }

    @SubscribeEvent
    public static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
                && event.getTarget() instanceof ServerPlayer target
                && VISIBLE_FLIGHTS.contains(target.getUUID())) {
            PacketDistributor.sendToPlayer(observer,
                    new WorldEchoFlightStatePayload(target.getUUID(), false));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopFlight(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            stopFlight(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer player) {
            stopFlight(player);
        } else {
            ACTIVE_FLIGHTS.remove(playerId);
        }
        VISIBLE_FLIGHTS.remove(playerId);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (ACTIVE_FLIGHTS.contains(player.getUUID()) || hasFlightAccess(player)) {
                stopFlight(player);
            }
        }
        VISIBLE_FLIGHTS.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_FLIGHTS.clear();
        VISIBLE_FLIGHTS.clear();
    }

    private static boolean canUseWings(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && hasUsableWings(player);
    }

    private static boolean hasUsableWings(Player player) {
        return equippedWings(player).is(ModItems.WORLD_ECHO_WINGS.get());
    }

    private static ItemStack equippedWings(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    private static void stopFlight(ServerPlayer player) {
        boolean wasManaged = ACTIVE_FLIGHTS.remove(player.getUUID()) || hasFlightAccess(player);
        setFlightAccess(player, false);
        if (wasManaged && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (wasManaged) {
            player.fallDistance = 0.0F;
        }
        syncVisibleState(player, hasUsableWings(player) && player.getAbilities().flying);
    }

    private static boolean hasFlightAccess(ServerPlayer player) {
        var attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return attribute != null && attribute.getModifier(FLIGHT_ATTRIBUTE_MODIFIER_ID) != null;
    }

    private static void setFlightAccess(ServerPlayer player, boolean enabled) {
        var attribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (attribute == null) {
            return;
        }
        if (enabled) {
            if (attribute.getModifier(FLIGHT_ATTRIBUTE_MODIFIER_ID) == null) {
                attribute.addTransientModifier(new AttributeModifier(FLIGHT_ATTRIBUTE_MODIFIER_ID,
                        1.0D, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            attribute.removeModifier(FLIGHT_ATTRIBUTE_MODIFIER_ID);
        }
    }

    private static void refreshFlightResistance(ServerPlayer player) {
        MobEffectInstance resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (resistance == null
                || resistance.getAmplifier() < FLIGHT_RESISTANCE_AMPLIFIER
                || (resistance.getAmplifier() == FLIGHT_RESISTANCE_AMPLIFIER
                && resistance.getDuration() > 0
                && resistance.getDuration() <= FLIGHT_RESISTANCE_REFRESH_THRESHOLD_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    FLIGHT_RESISTANCE_DURATION_TICKS, FLIGHT_RESISTANCE_AMPLIFIER,
                    true, false, true));
        }
    }

    private static void syncVisibleState(ServerPlayer player, boolean flying) {
        boolean changed = flying
                ? VISIBLE_FLIGHTS.add(player.getUUID())
                : VISIBLE_FLIGHTS.remove(player.getUUID());
        if (changed) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new WorldEchoFlightStatePayload(player.getUUID(), flying));
        }
    }
}
