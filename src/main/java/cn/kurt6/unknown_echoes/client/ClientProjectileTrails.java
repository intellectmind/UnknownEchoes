package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.entity.projectile.SporePellet;
import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import cn.kurt6.unknown_echoes.fx.ProjectileTrailGeometry;
import cn.kurt6.unknown_echoes.item.weapon.WeaponProjectileFx;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientProjectileTrails {
    private static ClientLevel activeLevel;
    private static long lastTick = Long.MIN_VALUE;
    private static int emitted;
    private static final Set<Entity> EMITTED = new HashSet<>();

    private ClientProjectileTrails() { }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        var projectile = event.getEntity();
        if (!(projectile instanceof TideBoltEntity) && !(projectile instanceof SporePellet)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || projectile.level() != mc.level || mc.isPaused()
                || !ClientConfig.ENABLE_ECHO_PARTICLES.get() || !projectile.isAlive() || projectile.isInvisible()) return;
        long tick = mc.level.getGameTime();
        if (activeLevel != mc.level || lastTick != tick) {
            activeLevel = mc.level;
            lastTick = tick;
            emitted = 0;
            EMITTED.clear();
        }
        int limit = switch (mc.options.particles().get().getId()) { case 2 -> 2; case 1 -> 4; default -> 8; };
        if (emitted >= limit || EMITTED.contains(projectile) || projectile.distanceToSqr(mc.player) > 32 * 32) return;
        var cue = projectile instanceof SporePellet
                ? ProjectileTrailGeometry.cue(projectile.getPosition(0), projectile.position(),
                        ParticleVisuals.core(ParticleKind.VENOM), ParticleVisuals.edge(ParticleKind.VENOM), ProjectileTrailGeometry.Style.SPORE)
                : WeaponProjectileFx.tideTrailCue(projectile.getPosition(0), projectile.position());
        if (cue != null) {
            emitted++;
            EMITTED.add(projectile);
            ClientCombatFx.accept(cue);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void worldTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != activeLevel || mc.player == null || !ClientConfig.ENABLE_ECHO_PARTICLES.get()) clear();
    }

    private static void clear() {
        activeLevel = null;
        lastTick = Long.MIN_VALUE;
        emitted = 0;
        EMITTED.clear();
    }
}
