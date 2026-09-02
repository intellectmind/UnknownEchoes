package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.AshScribe;
import cn.kurt6.unknown_echoes.entity.boss.BrokenBellKeeper;
import cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper;
import cn.kurt6.unknown_echoes.entity.boss.DreamBloomKeeper;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.InvertedGatekeeper;
import cn.kurt6.unknown_echoes.entity.boss.LostRecorderChief;
import cn.kurt6.unknown_echoes.entity.boss.MirrorDustButler;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import cn.kurt6.unknown_echoes.entity.boss.SilentPriest;
import cn.kurt6.unknown_echoes.entity.boss.StormWeaver;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.entity.mob.BrokenBellCrow;
import cn.kurt6.unknown_echoes.entity.mob.AbyssLurker;
import cn.kurt6.unknown_echoes.entity.mob.AnchorShellBeast;
import cn.kurt6.unknown_echoes.entity.mob.AshbackGrazer;
import cn.kurt6.unknown_echoes.entity.mob.AshveilStalker;
import cn.kurt6.unknown_echoes.entity.mob.CrystalFeatherBird;
import cn.kurt6.unknown_echoes.entity.mob.CrystalChimeSpirit;
import cn.kurt6.unknown_echoes.entity.mob.CrystalNoiseWisp;
import cn.kurt6.unknown_echoes.entity.mob.BrokenBellGuard;
import cn.kurt6.unknown_echoes.entity.mob.DreamingDeer;
import cn.kurt6.unknown_echoes.entity.mob.EchoDeer;
import cn.kurt6.unknown_echoes.entity.mob.EchoCuttlefish;
import cn.kurt6.unknown_echoes.entity.mob.EchoRemnant;
import cn.kurt6.unknown_echoes.entity.mob.EchoWanderer;
import cn.kurt6.unknown_echoes.entity.mob.FoldpageBug;
import cn.kurt6.unknown_echoes.entity.mob.FoldedMantis;
import cn.kurt6.unknown_echoes.entity.mob.GravityRam;
import cn.kurt6.unknown_echoes.entity.mob.GlowRabbit;
import cn.kurt6.unknown_echoes.entity.mob.HushlightJelly;
import cn.kurt6.unknown_echoes.entity.mob.LostRecorder;
import cn.kurt6.unknown_echoes.entity.mob.MirrorDustDecoy;
import cn.kurt6.unknown_echoes.entity.mob.MirrorTailFish;
import cn.kurt6.unknown_echoes.entity.mob.MossBackTurtle;
import cn.kurt6.unknown_echoes.entity.mob.PearlbackRay;
import cn.kurt6.unknown_echoes.entity.mob.RiftGlider;
import cn.kurt6.unknown_echoes.entity.mob.SilentWalker;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import cn.kurt6.unknown_echoes.entity.mob.WaterShadow;
import cn.kurt6.unknown_echoes.entity.mob.WindErodedSentinel;
import cn.kurt6.unknown_echoes.entity.mob.WindSacMoth;
import cn.kurt6.unknown_echoes.entity.mob.VoidWanderer;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ECHO_WANDERER.get(), EchoWanderer.createAttributes().build());
        event.put(ModEntities.FORGOTTEN_COLOSSUS.get(), ForgottenColossus.createAttributes().build());
        event.put(ModEntities.ABYSS_WATCHER.get(), AbyssWatcher.createAttributes().build());
        event.put(ModEntities.VOID_PROPHET.get(), VoidProphet.createAttributes().build());
        event.put(ModEntities.RIFT_WARDEN.get(), RiftWarden.createAttributes().build());
        event.put(ModEntities.WORLD_ECHO.get(), WorldEcho.createAttributes().build());
        event.put(ModEntities.MIRROR_GUARDIAN.get(), MirrorGuardian.createAttributes().build());
        event.put(ModEntities.SILENT_WALKER.get(), SilentWalker.createAttributes().build());
        event.put(ModEntities.CRYSTAL_FEATHER_BIRD.get(), CrystalFeatherBird.createAttributes().build());
        event.put(ModEntities.ECHO_PROJECTION.get(), EchoProjectionEntity.createAttributes().build());
        event.put(ModEntities.STORM_WEAVER.get(), StormWeaver.createAttributes().build());
        event.put(ModEntities.SILENT_PRIEST.get(), SilentPriest.createAttributes().build());
        event.put(ModEntities.TIDE_LANTERN_KEEPER.get(), TideLanternKeeper.createAttributes().build());
        event.put(ModEntities.TIDE_WATER_SHADE.get(), TideWaterShade.createAttributes().build());
        event.put(ModEntities.MIRROR_DUST_BUTLER.get(), MirrorDustButler.createAttributes().build());
        event.put(ModEntities.LOST_TRAVELER.get(),
                cn.kurt6.unknown_echoes.entity.mob.LostTraveler.createAttributes().build());

        event.put(ModEntities.ECHO_DEER.get(), EchoDeer.createAttributes().build());
        event.put(ModEntities.GLOW_RABBIT.get(), GlowRabbit.createAttributes().build());
        event.put(ModEntities.MIRROR_TAIL_FISH.get(), MirrorTailFish.createAttributes().build());
        event.put(ModEntities.ECHO_CUTTLEFISH.get(), EchoCuttlefish.createAttributes().build());
        event.put(ModEntities.PEARLBACK_RAY.get(), PearlbackRay.createAttributes().build());
        event.put(ModEntities.MOSS_BACK_TURTLE.get(), MossBackTurtle.createAttributes().build());

        event.put(ModEntities.DREAMING_DEER.get(), DreamingDeer.createAttributes().build());
        event.put(ModEntities.CRYSTAL_CHIME_SPIRIT.get(), CrystalChimeSpirit.createAttributes().build());
        event.put(ModEntities.BROKEN_BELL_GUARD.get(), BrokenBellGuard.createAttributes().build());
        event.put(ModEntities.CRYSTAL_NOISE_WISP.get(), CrystalNoiseWisp.createAttributes().build());
        event.put(ModEntities.BROKEN_BELL_CROW.get(), BrokenBellCrow.createAttributes().build());
        event.put(ModEntities.LOST_RECORDER.get(), LostRecorder.createAttributes().build());
        event.put(ModEntities.WIND_SAC_MOTH.get(), WindSacMoth.createAttributes().build());
        event.put(ModEntities.WIND_ERODED_SENTINEL.get(), WindErodedSentinel.createAttributes().build());
        event.put(ModEntities.ECHO_REMNANT.get(), EchoRemnant.createAttributes().build());
        event.put(ModEntities.WATER_SHADOW.get(), WaterShadow.createAttributes().build());
        event.put(ModEntities.MIRROR_DUST_DECOY.get(), MirrorDustDecoy.createAttributes().build());
        event.put(ModEntities.CRYSTAL_SONGKEEPER.get(), CrystalSongkeeper.createAttributes().build());
        event.put(ModEntities.BROKEN_BELL_KEEPER.get(), BrokenBellKeeper.createAttributes().build());
        event.put(ModEntities.DREAM_BLOOM_KEEPER.get(), DreamBloomKeeper.createAttributes().build());
        event.put(ModEntities.LOST_RECORDER_CHIEF.get(), LostRecorderChief.createAttributes().build());
        event.put(ModEntities.INVERTED_GATEKEEPER.get(), InvertedGatekeeper.createAttributes().build());
        event.put(ModEntities.ASH_SCRIBE.get(), AshScribe.createAttributes().build());

        event.put(ModEntities.FOLDPAGE_BUG.get(), FoldpageBug.createAttributes().build());
        event.put(ModEntities.ANCHOR_SHELL_BEAST.get(), AnchorShellBeast.createAttributes().build());
        event.put(ModEntities.ABYSS_LURKER.get(), AbyssLurker.createAttributes().build());
        event.put(ModEntities.VOID_WANDERER.get(), VoidWanderer.createAttributes().build());
        event.put(ModEntities.RIFT_GLIDER.get(), RiftGlider.createAttributes().build());
        event.put(ModEntities.ASHBACK_GRAZER.get(), AshbackGrazer.createAttributes().build());
        event.put(ModEntities.HUSHLIGHT_JELLY.get(), HushlightJelly.createAttributes().build());
        event.put(ModEntities.FOLDED_MANTIS.get(), FoldedMantis.createAttributes().build());
        event.put(ModEntities.GRAVITY_RAM.get(), GravityRam.createAttributes().build());
        event.put(ModEntities.ASHVEIL_STALKER.get(), AshveilStalker.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {

        event.register(ModEntities.ECHO_WANDERER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SILENT_WALKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CRYSTAL_FEATHER_BIRD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.ECHO_DEER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GLOW_RABBIT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MOSS_BACK_TURTLE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.DREAMING_DEER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CRYSTAL_CHIME_SPIRIT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BROKEN_BELL_GUARD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CRYSTAL_NOISE_WISP.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BROKEN_BELL_CROW.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LOST_RECORDER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WIND_SAC_MOTH.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WIND_ERODED_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ECHO_REMNANT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MIRROR_DUST_DECOY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.FOLDPAGE_BUG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ANCHOR_SHELL_BEAST.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ABYSS_LURKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getMaxLocalRawBrightness(pos) <= 5
                                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOID_WANDERER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.noCollision(type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))
                                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RIFT_GLIDER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ASHBACK_GRAZER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HUSHLIGHT_JELLY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.noCollision(type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))
                                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FOLDED_MANTIS.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GRAVITY_RAM.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ASHVEIL_STALKER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getMaxLocalRawBrightness(pos) <= 7
                                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.MIRROR_TAIL_FISH.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ECHO_CUTTLEFISH.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.PEARLBACK_RAY.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WATER_SHADOW.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
