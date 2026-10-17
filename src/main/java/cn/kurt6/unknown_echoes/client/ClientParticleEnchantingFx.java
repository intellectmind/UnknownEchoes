package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.particle.ParticleData;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.particle.ParticleVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientParticleEnchantingFx {
    private record Previous(Vec3 center, ParticleData data, Item item, boolean firstPerson, long tick) { }
    private record Pose(Vec3 center, Vec3 look) { }
    private static final Map<UUID, Previous> PREVIOUS = new HashMap<>();
    private static ClientLevel activeLevel;
    private ClientParticleEnchantingFx() { }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (activeLevel != mc.level) { PREVIOUS.clear(); activeLevel = mc.level; }
        if (mc.level == null || mc.player == null || mc.isPaused() || !mc.level.tickRateManager().runsNormally()) return;
        if (!ClientConfig.ENABLE_ECHO_PARTICLES.get()) { PREVIOUS.clear(); return; }
        long now = mc.level.getGameTime();
        if (now % ParticleVisuals.HELD_INTERVAL != 0) return;
        PREVIOUS.entrySet().removeIf(e -> now - e.getValue().tick > 16);
        ParticleStatus setting = mc.options.particles().get();
        int limit = setting == ParticleStatus.MINIMAL ? 1 : setting == ParticleStatus.DECREASED ? 2 : ParticleVisuals.MAX_HELD_PLAYERS;
        NearestFxCandidates<Player> nearest = null;
        for (Player player : mc.level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isInvisible()
                    || firstPerson(player) || ClientEquipmentFx.quietWingFlight(player)) continue;
            double distance = player.distanceToSqr(mc.player);
            if (distance <= 24 * 24) {
                if (nearest == null) nearest = new NearestFxCandidates<>(16);
                nearest.add(player, distance);
            }
        }
        if (nearest == null) return;
        int shown = 0;
        while (shown < limit && !nearest.isEmpty()) {
            Player player = nearest.removeNearest();
            ParticleData data = ParticleRules.data(player.getMainHandItem());
            if (data.total() == 0) { PREVIOUS.remove(player.getUUID()); continue; }
            boolean firstPerson = firstPerson(player);
            Pose pose = pose(player, firstPerson, 1);
            Vec3 center = pose.center;
            Item item = player.getMainHandItem().getItem();
            Previous previous = PREVIOUS.put(player.getUUID(), new Previous(center, data, item, firstPerson, now));
            Vec3 from = previous != null && previous.data.equals(data) && previous.item == item && previous.firstPerson == firstPerson
                    && now - previous.tick == ParticleVisuals.HELD_INTERVAL ? previous.center : null;
            ClientLevel level = mc.level;
            for (var cue : ParticleVisuals.held(data, center, from, pose.look, now)) {
                if (!cue.followsHand()) { ClientCombatFx.accept(cue.payload()); continue; }
                ClientCombatFx.acceptFollowing(player.getUUID(), cue.payload(), partial -> {
                    if (mc.level != level || player.level() != level || !player.isAlive() || player.isSpectator() || player.isInvisible()
                            || firstPerson(player) != firstPerson || ClientEquipmentFx.quietWingFlight(player)
                            || player.getMainHandItem().getItem() != item || !ParticleRules.data(player.getMainHandItem()).equals(data)) return null;
                    Pose live = pose(player, firstPerson, (float) partial);
                    return cue.at(center, live.center, live.look);
                });
            }
            if (data.total() >= 35 && now % 16 == 0 && setting != ParticleStatus.MINIMAL) {
                Vec3 glyph = center.add(ParticleVisuals.handSide(pose.look, player.getMainArm() == HumanoidArm.LEFT).scale(.14)).add(0, .06, 0);
                mc.level.addParticle(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                        glyph.x - center.x, .08, glyph.z - center.z);
            }
            shown++;
        }
    }

    private static boolean firstPerson(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    private static Pose pose(Player player, boolean firstPerson, float partial) {
        float yaw = firstPerson ? Mth.rotLerp(partial, player.yRotO, player.getYRot())
                : Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        Vec3 flat = Vec3.directionFromRotation(0, yaw);
        Vec3 look = firstPerson ? player.getViewVector(partial) : flat;
        return new Pose(ParticleVisuals.handAnchor(player.getPosition(partial), player.getEyeHeight(), player.getBbHeight(),
                look, flat, player.getAttackAnim(partial), player.getMainArm() == HumanoidArm.LEFT, firstPerson), look);
    }
}
