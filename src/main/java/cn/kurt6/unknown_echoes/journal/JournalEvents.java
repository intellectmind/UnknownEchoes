package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 探索日志自动记录钩子:每 2 秒结算玩家脚下的本 Mod 结构与群系(静止时复用短期位置查询缓存),
 * 击杀本 Mod 生物时记录图鉴。残页阅读记录在 AncientPageItem.use,Boss 与能力进度走 EchoAbilityData。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class JournalEvents {
    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final int CACHE_REFRESH_TICKS = 200;
    private static final Map<UUID, LocationScan> LOCATION_SCANS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.isSpectator()
                || player.tickCount % SCAN_INTERVAL_TICKS != 0
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        scanAndRecord(player, level, false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !player.isSpectator()
                && player.level() instanceof ServerLevel level) {
            scanAndRecord(player, level, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCATION_SCANS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LOCATION_SCANS.clear();
    }

    private static void scanAndRecord(ServerPlayer player, ServerLevel level, boolean forceRefresh) {
        BlockPos pos = player.blockPosition();
        long gameTime = level.getGameTime();
        LocationScan scan = LOCATION_SCANS.get(player.getUUID());
        if (forceRefresh || needsRefresh(scan, level.dimension(), pos, gameTime)) {
            scan = queryLocation(level, pos, gameTime);
            LOCATION_SCANS.put(player.getUUID(), scan);
        }

        for (StructureSite site : scan.sites()) {
            JournalManager.recordStructureSite(player, site.id(), site.bounds().getCenter(), site.bounds());
        }
        scan.biomeId().ifPresent(id -> JournalManager.recordBiome(player, id));
    }

    private static boolean needsRefresh(LocationScan scan, ResourceKey<Level> dimension,
                                        BlockPos pos, long gameTime) {
        return scan == null
                || !scan.dimension().equals(dimension)
                || !scan.pos().equals(pos)
                || gameTime < scan.refreshedAt()
                || gameTime - scan.refreshedAt() >= CACHE_REFRESH_TICKS;
    }

    private static LocationScan queryLocation(ServerLevel level, BlockPos pos, long gameTime) {
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<StructureSite> sites = LoadedStructureLookup
                .sitesAt(level, pos, structure -> {
                    ResourceLocation id = structureRegistry.getKey(structure);
                    return id != null && id.getNamespace().equals(UnknownEchoes.MODID);
                })
                .stream()
                .map(site -> new StructureSite(structureRegistry.getKey(site.structure()), site.bounds()))
                .toList();
        Optional<ResourceLocation> biomeId = level.getBiome(pos)
                .unwrapKey()
                .map(ResourceKey::location)
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID));
        return new LocationScan(level.dimension(), pos.immutable(), gameTime, sites, biomeId);
    }

    private record StructureSite(ResourceLocation id, BoundingBox bounds) { }

    private record LocationScan(ResourceKey<Level> dimension, BlockPos pos, long refreshedAt,
                                List<StructureSite> sites,
                                Optional<ResourceLocation> biomeId) {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (!event.getEntity().getType().is(JournalManager.BESTIARY_MOBS_TAG)
                    && !event.getEntity().getType().is(JournalManager.ECHO_BOSSES_TAG)) {
                return;
            }
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(event.getEntity().getType());
            if (typeId != null && typeId.getNamespace().equals(UnknownEchoes.MODID)) {
                JournalManager.recordMob(player, typeId);
            }
        }
    }
}
