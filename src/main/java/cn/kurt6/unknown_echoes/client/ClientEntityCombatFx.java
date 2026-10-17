package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.fx.EntityHitPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public final class ClientEntityCombatFx {
    private static final Map<LivingEntity, Integer> LAST_HURT = new WeakHashMap<>();

    private ClientEntityCombatFx() { }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LivingEntity entity) || !entity.level().isClientSide) return;
        if (entity.hurtTime <= 0 || entity.isRemoved()) { LAST_HURT.remove(entity); return; }
        if (mc.level != entity.level() || mc.player == null || mc.isPaused()
                || !ClientConfig.ENABLE_ECHO_PARTICLES.get() || entity.getType().is(ModEntityTags.ECHO_BOSSES)
                || entity.isInvisible() || entity.isSpectator()) return;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!id.getNamespace().equals(UnknownEchoes.MODID)) return;
        int previous = LAST_HURT.getOrDefault(entity, 0);
        LAST_HURT.put(entity, entity.hurtTime);
        if (entity.hurtTime <= previous || entity.distanceToSqr(mc.player) > 40 * 40) return;
        ClientCombatFx.accept(EntityHitPresentation.cue(id.getPath(), entity.getBoundingBox().getCenter(),
                mc.gameRenderer.getMainCamera().getPosition(), entity.getBbWidth(), entity.getBbHeight()));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        LAST_HURT.clear();
    }
}
