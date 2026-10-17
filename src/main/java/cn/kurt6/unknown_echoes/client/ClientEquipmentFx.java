package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.fx.EquipmentWardGeometry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.CombatFxPayload;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientEquipmentFx {
    private static final Map<UUID, Long> NEXT_EMISSION = new HashMap<>();
    private static ClientLevel currentLevel;

    private ClientEquipmentFx() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (currentLevel != mc.level) {
            NEXT_EMISSION.clear();
            currentLevel = mc.level;
        }
        if (mc.level == null || mc.player == null || mc.isPaused() || !ClientConfig.ENABLE_ECHO_PARTICLES.get()
                || !mc.level.tickRateManager().runsNormally()
                || mc.level.getGameTime() % 4 != 0) {
            return;
        }
        long now = mc.level.getGameTime();
        NEXT_EMISSION.values().removeIf(next -> next <= now);
        if (now % 12 == 0 && mc.player.isInWater() && mc.player.isSwimming()
                && ClientAbilityCache.hasAbility(EchoAbilityType.TIDE_ECHO)) {
            wake(mc.player, 0x72E9E1, 0x2688BD, .22F);
        }
        NearestFxCandidates<Player> nearest = null;
        for (Player player : mc.level.players()) {
            if (player.hurtTime == 0 || !player.isAlive() || player.isSpectator() || player.isInvisible()
                    || player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()
                    || NEXT_EMISSION.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now
                    || quietWingFlight(player)) continue;
            double distance = player.distanceToSqr(mc.player);
            if (distance <= 32 * 32) {
                if (nearest == null) nearest = new NearestFxCandidates<>(16);
                nearest.add(player, distance);
            }
        }
        if (nearest == null) return;
        int shown = 0;
        while (shown < 8 && !nearest.isEmpty()) {
            Player player = nearest.removeNearest();
            Map<String, Integer> pieces = new HashMap<>();
            for (var stack : player.getArmorSlots()) {
                if (stack.getItem() instanceof ArmorItem) {
                    var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (id.getNamespace().equals(UnknownEchoes.MODID)) {
                        String set = id.getPath().replaceFirst("_(helmet|chestplate|leggings|boots)$", "");
                        pieces.merge(set, 1, Integer::sum);
                    }
                }
            }
            String set = pieces.entrySet().stream().filter(e -> e.getValue() >= 2)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey).findFirst().orElse("");
            int color = switch (set) {
                case "explorer" -> 0xC5DCAE;
                case "echo_traveler" -> 0x63CABD;
                case "listener" -> 0xB8B2E5;
                case "wind_walker" -> 0xAEF2D5;
                case "tide_stalker" -> 0x45BEEB;
                case "true_sight_shadow" -> 0xE4E8FF;
                case "silent_watch" -> 0xA783D1;
                case "echo_oath" -> 0xEFD69A;
                case "abyss_walker" -> 0x748BF4;
                case "resonator" -> 0xF7DCB6;
                default -> 0;
            };
            if (color == 0) {
                continue;
            }
            Shape shape = set.equals("wind_walker") || set.equals("tide_stalker") ? Shape.DOME : Shape.AEGIS;
            ClientCombatFx.accept(new CombatFxPayload(shape, Vec3.ZERO, Vec3.ZERO,
                    player.getBbWidth() * 0.5F + 0.3F, player.getBbHeight(), EquipmentWardGeometry.tag(set),
                    color, (color & 0xFEFEFE) >> 1, 12, false, player.getId()));
            NEXT_EMISSION.put(player.getUUID(), now + 12);
            shown++;
        }
    }

    public static void wind(Player player, boolean flight) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WORLD_ECHO_WINGS.get())) {
            return;
        }
        if (flight) {
            wake(player, 0xB5F4DC, 0x449EB0, .18F);
            return;
        }
        ClientCombatFx.accept(new CombatFxPayload(Shape.PETALS, player.position(), Vec3.ZERO,
                .55F, .7F, (float) (Math.PI * 2),
                0xB5F4DC, 0x449EB0, 12, false, -1));
    }

    private static void wake(Player player, int core, int edge, float radius) {
        Vec3 velocity = player.getDeltaMovement();
        double speed = velocity.length();
        if (speed < .04) {
            return;
        }
        float length = (float) Math.min(2.4, speed * 5);
        Vec3 direction = velocity.scale(1 / speed);
        Vec3 origin = player.position().add(0, player.getBbHeight() * .35, 0).subtract(direction.scale(length));
        ClientCombatFx.accept(new CombatFxPayload(Shape.WAKE, origin, direction,
                radius, length, 0, core, edge, 10, false, -1));
    }

    static boolean quietWingFlight(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WORLD_ECHO_WINGS.get())
                && (ClientWorldEchoFlightCache.isFlying(player) || player.getAbilities().flying
                || player.isFallFlying() || player.isLocalPlayer() && ClientGameEvents.isWorldEchoFlying());
    }

    public static void trueSight(net.minecraft.world.entity.Entity target) {
        float half = target.getBbWidth() * 0.5F;
        ClientCombatFx.accept(new CombatFxPayload(Shape.SIGIL,
                new Vec3(0, target.getBbHeight() * 0.55, 0), Vec3.ZERO,
                half + Math.max(0.25F, half * 0.35F), 0, 0,
                0xE4EEFF, 0x8EADD7, 8, false, target.getId()));
    }
}
