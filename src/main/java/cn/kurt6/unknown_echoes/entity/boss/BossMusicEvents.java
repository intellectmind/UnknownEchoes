package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.network.BossMusicPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class BossMusicEvents {
    private static final ResourceLocation NONE = UnknownEchoes.id("none");
    private static final Map<UUID, UUID> SELECTED = new HashMap<>();

    private BossMusicEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        LivingEntity selected = null;
        boolean muted = !player.isAlive();
        if (!muted) {
            UUID previous = SELECTED.get(player.getUUID());
            if (previous != null && player.serverLevel().getEntity(previous) instanceof LivingEntity living
                    && living instanceof BossMusicSource && living.isAlive()
                    && player.distanceToSqr(living) <= 56.0D * 56.0D) selected = living;
            if (selected == null) {
                double closest = Double.MAX_VALUE;
                int rank = Integer.MAX_VALUE;
                for (LivingEntity boss : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(48), e -> e.isAlive() && e instanceof BossMusicSource)) {
                    if (boss instanceof SilentPriest priest && silencesPlayer(player, priest)) {
                        muted = true;
                    }
                    double distance = player.distanceToSqr(boss);
                    int priority = priority(boss);
                    if (distance <= 48.0D * 48.0D && (priority < rank || priority == rank && distance < closest)) {
                        selected = boss;
                        rank = priority;
                        closest = distance;
                    }
                }
            } else {
                muted = !player.serverLevel().getEntitiesOfClass(SilentPriest.class,
                        player.getBoundingBox().inflate(SilentPriest.DOMAIN_RADIUS),
                        priest -> silencesPlayer(player, priest)).isEmpty();
            }
        }
        ResourceLocation type = NONE;
        int stage = 0;
        if (selected != null) {
            SELECTED.put(player.getUUID(), selected.getUUID());
            type = BuiltInRegistries.ENTITY_TYPE.getKey(selected.getType());
            stage = ((BossMusicSource) selected).musicStage();
        } else SELECTED.remove(player.getUUID());
        // 包中只传实体类型和公共阶段；镜像的 UUID、位置及真假均留在服务端。
        PacketDistributor.sendToPlayer(player, new BossMusicPayload(player.level().dimension().location(),
                type, stage, muted));
    }

    private static int priority(LivingEntity boss) {
        if (boss instanceof VoidProphet) return 0;
        if (boss instanceof RiftWarden) return 1;
        if (boss instanceof WorldEcho) return 2;
        if (boss instanceof ForgottenColossus) return 3;
        if (boss instanceof AbyssWatcher) return 4;
        if (boss instanceof MirrorGuardian) return 5;
        return 6;
    }

    private static boolean silencesPlayer(ServerPlayer player, SilentPriest priest) {
        return priest.isAlive() && priest.isDomainActive()
                && player.distanceToSqr(priest) <= SilentPriest.DOMAIN_RADIUS * SilentPriest.DOMAIN_RADIUS;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        SELECTED.remove(event.getEntity().getUUID());
    }
}
