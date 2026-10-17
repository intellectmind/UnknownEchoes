package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.entity.mob.RiftPageStalker;

import cn.kurt6.unknown_echoes.entity.mob.BrokenBellAxeman;

import cn.kurt6.unknown_echoes.entity.mob.ReedMaskRaider;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.mob.SporeCrownedWretch;
import cn.kurt6.unknown_echoes.entity.mob.CairnShellBrute;
import cn.kurt6.unknown_echoes.entity.mob.MossplateHound;
import cn.kurt6.unknown_echoes.entity.mob.SawtoothPrawn;
import cn.kurt6.unknown_echoes.entity.mob.LanternjawAngler;
import cn.kurt6.unknown_echoes.entity.mob.AmberbackBeetle;
import cn.kurt6.unknown_echoes.entity.mob.MossfangToad;
import cn.kurt6.unknown_echoes.entity.mob.NeedlefinLionfish;
import cn.kurt6.unknown_echoes.entity.mob.RiftfangEel;
import cn.kurt6.unknown_echoes.entity.mob.HollowReedSerpent;
import cn.kurt6.unknown_echoes.entity.mob.SlatehornBoar;
import cn.kurt6.unknown_echoes.entity.mob.ReefjawShark;
import cn.kurt6.unknown_echoes.entity.mob.LanternSeahorse;
import cn.kurt6.unknown_echoes.entity.mob.EmberclawScorpion;
import cn.kurt6.unknown_echoes.entity.mob.MossbackHedgehog;
import cn.kurt6.unknown_echoes.entity.mob.SailtailMinnow;
import cn.kurt6.unknown_echoes.entity.mob.MossSailSnail;
import cn.kurt6.unknown_echoes.entity.mob.CrystalSpineUrchin;
import cn.kurt6.unknown_echoes.entity.mob.AshSailSerpent;
import cn.kurt6.unknown_echoes.entity.mob.PetalwingButterfly;
import cn.kurt6.unknown_echoes.entity.mob.MosslightFirefly;
import cn.kurt6.unknown_echoes.entity.mob.BellNeedleWasp;
import cn.kurt6.unknown_echoes.entity.mob.ShardwingSkate;

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
import cn.kurt6.unknown_echoes.entity.mob.WindDriftJelly;
import cn.kurt6.unknown_echoes.entity.mob.ReedwingDragonfly;
import cn.kurt6.unknown_echoes.entity.mob.GaleShrike;
import cn.kurt6.unknown_echoes.entity.mob.AshLanternBat;
import cn.kurt6.unknown_echoes.entity.mob.VoidWanderer;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, UnknownEchoes.MODID);

    /** 碰撞箱按静态模型主体登记,模型单位再乘渲染缩放;fx_* 与翼/武器/装饰附肢不计入。 */

    public static final DeferredHolder<EntityType<?>, EntityType<EchoWanderer>> ECHO_WANDERER =
            ENTITY_TYPES.register("echo_wanderer",
                    () -> EntityType.Builder.of(EchoWanderer::new, MobCategory.MONSTER)
                            .sized(0.50F, 1.95F)
                            .clientTrackingRange(8)
                            .build("echo_wanderer"));

    public static final DeferredHolder<EntityType<?>, EntityType<ForgottenColossus>> FORGOTTEN_COLOSSUS =
            ENTITY_TYPES.register("forgotten_colossus",
                    () -> EntityType.Builder.of(ForgottenColossus::new, MobCategory.MONSTER)
                            .sized(3.15F, 7.05F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("forgotten_colossus"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssWatcher>> ABYSS_WATCHER =
            ENTITY_TYPES.register("abyss_watcher",
                    () -> EntityType.Builder.of(AbyssWatcher::new, MobCategory.MONSTER)
                            .sized(2.15F, 3.70F)
                            .clientTrackingRange(10)
                            .build("abyss_watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidProphet>> VOID_PROPHET =
            ENTITY_TYPES.register("void_prophet",
                    () -> EntityType.Builder.of(VoidProphet::new, MobCategory.MONSTER)
                            .sized(3.30F, 5.55F)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build("void_prophet"));

    public static final DeferredHolder<EntityType<?>, EntityType<RiftWarden>> RIFT_WARDEN =
            ENTITY_TYPES.register("rift_warden",
                    () -> EntityType.Builder.of(RiftWarden::new, MobCategory.MONSTER)
                            .sized(2.70F, 6.25F)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build("rift_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<WorldEcho>> WORLD_ECHO =
            ENTITY_TYPES.register("world_echo",
                    () -> EntityType.Builder.of(WorldEcho::new, MobCategory.MONSTER)
                            .sized(3.25F, 8.55F)
                            .clientTrackingRange(16)
                            .fireImmune()
                            .build("world_echo"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorGuardian>> MIRROR_GUARDIAN =
            ENTITY_TYPES.register("mirror_guardian",
                    () -> EntityType.Builder.of(MirrorGuardian::new, MobCategory.MONSTER)
                            .sized(2.35F, 6.85F)
                            .clientTrackingRange(10)
                            .build("mirror_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<SilentWalker>> SILENT_WALKER =
            ENTITY_TYPES.register("silent_walker",
                    () -> EntityType.Builder.of(SilentWalker::new, MobCategory.MONSTER)
                            .sized(1.30F, 1.45F)
                            .clientTrackingRange(8)
                            .build("silent_walker"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalFeatherBird>> CRYSTAL_FEATHER_BIRD =
            ENTITY_TYPES.register("crystal_feather_bird",
                    () -> EntityType.Builder.of(CrystalFeatherBird::new, MobCategory.CREATURE)
                            .sized(0.65F, 0.70F)
                            .clientTrackingRange(8)
                            .build("crystal_feather_bird"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoProjectionEntity>> ECHO_PROJECTION =
            ENTITY_TYPES.register("echo_projection",
                    () -> EntityType.Builder.of(EchoProjectionEntity::new, MobCategory.MISC)
                            .sized(0.90F, 1.90F)
                            .clientTrackingRange(8)
                            .fireImmune()
                            .build("echo_projection"));

    public static final DeferredHolder<EntityType<?>, EntityType<StormWeaver>> STORM_WEAVER =
            ENTITY_TYPES.register("storm_weaver",
                    () -> EntityType.Builder.of(StormWeaver::new, MobCategory.MONSTER)
                            .sized(4.75F, 3.05F)
                            .clientTrackingRange(10)
                            .build("storm_weaver"));

    public static final DeferredHolder<EntityType<?>, EntityType<SilentPriest>> SILENT_PRIEST =
            ENTITY_TYPES.register("silent_priest",
                    () -> EntityType.Builder.of(SilentPriest::new, MobCategory.MONSTER)
                            .sized(1.35F, 4.00F)
                            .clientTrackingRange(10)
                            .build("silent_priest"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideBoltEntity>> TIDE_BOLT =
            ENTITY_TYPES.register("tide_bolt",
                    () -> EntityType.Builder.<TideBoltEntity>of(TideBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("tide_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideLanternKeeper>> TIDE_LANTERN_KEEPER =
            ENTITY_TYPES.register("tide_lantern_keeper",
                    () -> EntityType.Builder.of(TideLanternKeeper::new, MobCategory.MONSTER)
                            .sized(1.60F, 3.35F)
                            .clientTrackingRange(10)
                            .build("tide_lantern_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideWaterShade>> TIDE_WATER_SHADE =
            ENTITY_TYPES.register("tide_water_shade",
                    () -> EntityType.Builder.of(TideWaterShade::new, MobCategory.MONSTER)
                            .sized(0.50F, 1.15F)
                            .clientTrackingRange(8)
                            .build("tide_water_shade"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDustButler>> MIRROR_DUST_BUTLER =
            ENTITY_TYPES.register("mirror_dust_butler",
                    () -> EntityType.Builder.of(MirrorDustButler::new, MobCategory.MONSTER)
                            .sized(2.10F, 4.10F)
                            .clientTrackingRange(10)
                            .build("mirror_dust_butler"));

    public static final DeferredHolder<EntityType<?>, EntityType<cn.kurt6.unknown_echoes.entity.mob.LostTraveler>> LOST_TRAVELER =
            ENTITY_TYPES.register("lost_traveler",
                    () -> EntityType.Builder.of(cn.kurt6.unknown_echoes.entity.mob.LostTraveler::new, MobCategory.CREATURE)
                            .sized(1.00F, 2.15F)
                            .clientTrackingRange(10)
                            .build("lost_traveler"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoDeer>> ECHO_DEER =
            ENTITY_TYPES.register("echo_deer",
                    () -> EntityType.Builder.of(EchoDeer::new, MobCategory.CREATURE)
                            .sized(1.50F, 1.60F)
                            .clientTrackingRange(8)
                            .build("echo_deer"));

    public static final DeferredHolder<EntityType<?>, EntityType<GlowRabbit>> GLOW_RABBIT =
            ENTITY_TYPES.register("glow_rabbit",
                    () -> EntityType.Builder.of(GlowRabbit::new, MobCategory.CREATURE)
                            .sized(0.65F, 0.45F)
                            .clientTrackingRange(8)
                            .build("glow_rabbit"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorTailFish>> MIRROR_TAIL_FISH =
            ENTITY_TYPES.register("mirror_tail_fish",
                    () -> EntityType.Builder.of(MirrorTailFish::new, MobCategory.WATER_AMBIENT)
                            .sized(0.50F, 0.45F)
                            .clientTrackingRange(4)
                            .build("mirror_tail_fish"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoCuttlefish>> ECHO_CUTTLEFISH =
            ENTITY_TYPES.register("echo_cuttlefish",
                    () -> EntityType.Builder.of(EchoCuttlefish::new, MobCategory.WATER_AMBIENT)
                            .sized(0.95F, 0.45F)
                            .clientTrackingRange(6)
                            .build("echo_cuttlefish"));

    public static final DeferredHolder<EntityType<?>, EntityType<PearlbackRay>> PEARLBACK_RAY =
            ENTITY_TYPES.register("pearlback_ray",
                    () -> EntityType.Builder.of(PearlbackRay::new, MobCategory.WATER_AMBIENT)
                            .sized(0.95F, 0.25F)
                            .clientTrackingRange(6)
                            .build("pearlback_ray"));

    public static final DeferredHolder<EntityType<?>, EntityType<MossBackTurtle>> MOSS_BACK_TURTLE =
            ENTITY_TYPES.register("moss_back_turtle",
                    () -> EntityType.Builder.of(MossBackTurtle::new, MobCategory.CREATURE)
                            .sized(1.00F, 0.55F)
                            .clientTrackingRange(8)
                            .build("moss_back_turtle"));

    public static final DeferredHolder<EntityType<?>, EntityType<DreamingDeer>> DREAMING_DEER =
            ENTITY_TYPES.register("dreaming_deer",
                    () -> EntityType.Builder.of(DreamingDeer::new, MobCategory.CREATURE)
                            .sized(1.30F, 1.60F)
                            .clientTrackingRange(8)
                            .build("dreaming_deer"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalChimeSpirit>> CRYSTAL_CHIME_SPIRIT =
            ENTITY_TYPES.register("crystal_chime_spirit",
                    () -> EntityType.Builder.of(CrystalChimeSpirit::new, MobCategory.CREATURE)
                            .sized(0.50F, 0.75F)
                            .clientTrackingRange(8)
                            .build("crystal_chime_spirit"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellGuard>> BROKEN_BELL_GUARD =
            ENTITY_TYPES.register("broken_bell_guard",
                    () -> EntityType.Builder.of(BrokenBellGuard::new, MobCategory.MONSTER)
                            .sized(1.50F, 2.25F)
                            .clientTrackingRange(8)
                            .build("broken_bell_guard"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalNoiseWisp>> CRYSTAL_NOISE_WISP =
            ENTITY_TYPES.register("crystal_noise_wisp",
                    () -> EntityType.Builder.of(CrystalNoiseWisp::new, MobCategory.MONSTER)
                            .sized(0.75F, 1.25F)
                            .clientTrackingRange(8)
                            .build("crystal_noise_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellCrow>> BROKEN_BELL_CROW =
            ENTITY_TYPES.register("broken_bell_crow",
                    () -> EntityType.Builder.of(BrokenBellCrow::new, MobCategory.CREATURE)
                            .sized(0.70F, 0.70F)
                            .clientTrackingRange(8)
                            .build("broken_bell_crow"));

    public static final DeferredHolder<EntityType<?>, EntityType<LostRecorder>> LOST_RECORDER =
            ENTITY_TYPES.register("lost_recorder",
                    () -> EntityType.Builder.of(LostRecorder::new, MobCategory.MONSTER)
                            .sized(0.60F, 1.85F)
                            .clientTrackingRange(8)
                            .build("lost_recorder"));

    public static final DeferredHolder<EntityType<?>, EntityType<WindSacMoth>> WIND_SAC_MOTH =
            ENTITY_TYPES.register("wind_sac_moth",
                    () -> EntityType.Builder.of(WindSacMoth::new, MobCategory.CREATURE)
                            .sized(0.85F, 0.60F)
                            .clientTrackingRange(8)
                            .build("wind_sac_moth"));

    public static final DeferredHolder<EntityType<?>, EntityType<WindErodedSentinel>> WIND_ERODED_SENTINEL =
            ENTITY_TYPES.register("wind_eroded_sentinel",
                    () -> EntityType.Builder.of(WindErodedSentinel::new, MobCategory.MONSTER)
                            .sized(0.70F, 1.40F)
                            .clientTrackingRange(8)
                            .build("wind_eroded_sentinel"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoRemnant>> ECHO_REMNANT =
            ENTITY_TYPES.register("echo_remnant",
                    () -> EntityType.Builder.of(EchoRemnant::new, MobCategory.MONSTER)
                            .sized(0.55F, 1.55F)
                            .clientTrackingRange(8)
                            .build("echo_remnant"));

    public static final DeferredHolder<EntityType<?>, EntityType<WaterShadow>> WATER_SHADOW =
            ENTITY_TYPES.register("water_shadow",
                    () -> EntityType.Builder.of(WaterShadow::new, MobCategory.MONSTER)
                            .sized(0.50F, 1.30F)
                            .clientTrackingRange(8)
                            .build("water_shadow"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDustDecoy>> MIRROR_DUST_DECOY =
            ENTITY_TYPES.register("mirror_dust_decoy",
                    () -> EntityType.Builder.of(MirrorDustDecoy::new, MobCategory.MONSTER)
                            .sized(0.95F, 1.60F)
                            .clientTrackingRange(8)
                            .build("mirror_dust_decoy"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalSongkeeper>> CRYSTAL_SONGKEEPER =
            ENTITY_TYPES.register("crystal_songkeeper",
                    () -> EntityType.Builder.of(CrystalSongkeeper::new, MobCategory.MONSTER)
                            .sized(2.20F, 3.20F)
                            .clientTrackingRange(10)
                            .build("crystal_songkeeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellKeeper>> BROKEN_BELL_KEEPER =
            ENTITY_TYPES.register("broken_bell_keeper",
                    () -> EntityType.Builder.of(BrokenBellKeeper::new, MobCategory.MONSTER)
                            .sized(1.80F, 4.00F)
                            .clientTrackingRange(10)
                            .build("broken_bell_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<DreamBloomKeeper>> DREAM_BLOOM_KEEPER =
            ENTITY_TYPES.register("dream_bloom_keeper",
                    () -> EntityType.Builder.of(DreamBloomKeeper::new, MobCategory.MONSTER)
                            .sized(2.30F, 3.40F)
                            .clientTrackingRange(10)
                            .build("dream_bloom_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<LostRecorderChief>> LOST_RECORDER_CHIEF =
            ENTITY_TYPES.register("lost_recorder_chief",
                    () -> EntityType.Builder.of(LostRecorderChief::new, MobCategory.MONSTER)
                            .sized(1.20F, 4.05F)
                            .clientTrackingRange(10)
                            .build("lost_recorder_chief"));

    public static final DeferredHolder<EntityType<?>, EntityType<InvertedGatekeeper>> INVERTED_GATEKEEPER =
            ENTITY_TYPES.register("inverted_gatekeeper",
                    () -> EntityType.Builder.of(InvertedGatekeeper::new, MobCategory.MONSTER)
                            .sized(3.60F, 3.65F)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build("inverted_gatekeeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshScribe>> ASH_SCRIBE =
            ENTITY_TYPES.register("ash_scribe",
                    () -> EntityType.Builder.of(AshScribe::new, MobCategory.MONSTER)
                            .sized(2.15F, 4.10F)
                            .clientTrackingRange(12)
                            .fireImmune()
                            .build("ash_scribe"));

    public static final DeferredHolder<EntityType<?>, EntityType<FoldpageBug>> FOLDPAGE_BUG =
            ENTITY_TYPES.register("foldpage_bug",
                    () -> EntityType.Builder.of(FoldpageBug::new, MobCategory.CREATURE)
                            .sized(0.85F, 0.40F)
                            .clientTrackingRange(8)
                            .build("foldpage_bug"));

    public static final DeferredHolder<EntityType<?>, EntityType<AnchorShellBeast>> ANCHOR_SHELL_BEAST =
            ENTITY_TYPES.register("anchor_shell_beast",
                    () -> EntityType.Builder.of(AnchorShellBeast::new, MobCategory.CREATURE)
                            .sized(1.05F, 1.10F)
                            .clientTrackingRange(8)
                            .build("anchor_shell_beast"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssLurker>> ABYSS_LURKER =
            ENTITY_TYPES.register("abyss_lurker",
                    () -> EntityType.Builder.of(AbyssLurker::new, MobCategory.MONSTER)
                            .sized(1.20F, 0.80F)
                            .clientTrackingRange(8)
                            .build("abyss_lurker"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidWanderer>> VOID_WANDERER =
            ENTITY_TYPES.register("void_wanderer",
                    () -> EntityType.Builder.of(VoidWanderer::new, MobCategory.MONSTER)
                            .sized(0.45F, 0.80F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("void_wanderer"));

    public static final DeferredHolder<EntityType<?>, EntityType<RiftGlider>> RIFT_GLIDER =
            ENTITY_TYPES.register("rift_glider",
                    () -> EntityType.Builder.of(RiftGlider::new, MobCategory.CREATURE)
                            .sized(0.75F, 0.65F)
                            .clientTrackingRange(10)
                            .build("rift_glider"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshbackGrazer>> ASHBACK_GRAZER =
            ENTITY_TYPES.register("ashback_grazer",
                    () -> EntityType.Builder.of(AshbackGrazer::new, MobCategory.CREATURE)
                            .sized(1.65F, 1.00F)
                            .clientTrackingRange(8)
                            .build("ashback_grazer"));

    public static final DeferredHolder<EntityType<?>, EntityType<HushlightJelly>> HUSHLIGHT_JELLY =
            ENTITY_TYPES.register("hushlight_jelly",
                    () -> EntityType.Builder.of(HushlightJelly::new, MobCategory.CREATURE)
                            .sized(0.95F, 1.15F)
                            .clientTrackingRange(10)
                            .build("hushlight_jelly"));

    public static final DeferredHolder<EntityType<?>, EntityType<FoldedMantis>> FOLDED_MANTIS =
            ENTITY_TYPES.register("folded_mantis",
                    () -> EntityType.Builder.of(FoldedMantis::new, MobCategory.MONSTER)
                            .sized(1.70F, 0.90F)
                            .clientTrackingRange(8)
                            .build("folded_mantis"));

    public static final DeferredHolder<EntityType<?>, EntityType<GravityRam>> GRAVITY_RAM =
            ENTITY_TYPES.register("gravity_ram",
                    () -> EntityType.Builder.of(GravityRam::new, MobCategory.MONSTER)
                            .sized(1.40F, 1.20F)
                            .clientTrackingRange(10)
                            .build("gravity_ram"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshveilStalker>> ASHVEIL_STALKER =
            ENTITY_TYPES.register("ashveil_stalker",
                    () -> EntityType.Builder.of(AshveilStalker::new, MobCategory.MONSTER)
                            .sized(1.50F, 0.85F)
                            .clientTrackingRange(8)
                            .build("ashveil_stalker"));
    public static final DeferredHolder<EntityType<?>, EntityType<WindDriftJelly>> WIND_DRIFT_JELLY =
            ENTITY_TYPES.register("wind_drift_jelly",
                    () -> EntityType.Builder.of(WindDriftJelly::new, MobCategory.CREATURE)
                            .sized(0.65F, 0.60F)
                            .clientTrackingRange(10)
                            .build("wind_drift_jelly"));

    public static final DeferredHolder<EntityType<?>, EntityType<ReedwingDragonfly>> REEDWING_DRAGONFLY =
            ENTITY_TYPES.register("reedwing_dragonfly",
                    () -> EntityType.Builder.of(ReedwingDragonfly::new, MobCategory.CREATURE)
                            .sized(0.40F, 0.30F)
                            .clientTrackingRange(10)
                            .build("reedwing_dragonfly"));

    public static final DeferredHolder<EntityType<?>, EntityType<GaleShrike>> GALE_SHRIKE =
            ENTITY_TYPES.register("gale_shrike",
                    () -> EntityType.Builder.of(GaleShrike::new, MobCategory.MONSTER)
                            .sized(0.50F, 0.55F)
                            .clientTrackingRange(10)
                            .build("gale_shrike"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshLanternBat>> ASH_LANTERN_BAT =
            ENTITY_TYPES.register("ash_lantern_bat",
                    () -> EntityType.Builder.of(AshLanternBat::new, MobCategory.MONSTER)
                            .sized(0.40F, 0.45F)
                            .clientTrackingRange(10)
                            .build("ash_lantern_bat"));

    public static final DeferredHolder<EntityType<?>, EntityType<PetalwingButterfly>> PETALWING_BUTTERFLY =
            ENTITY_TYPES.register("petalwing_butterfly",
                    () -> EntityType.Builder.of(PetalwingButterfly::new, MobCategory.CREATURE)
                            .sized(0.55F, 0.20F)
                            .clientTrackingRange(10)
                            .build("petalwing_butterfly"));

    public static final DeferredHolder<EntityType<?>, EntityType<MosslightFirefly>> MOSSLIGHT_FIREFLY =
            ENTITY_TYPES.register("mosslight_firefly",
                    () -> EntityType.Builder.of(MosslightFirefly::new, MobCategory.CREATURE)
                            .sized(0.60F, 0.25F)
                            .clientTrackingRange(10)
                            .build("mosslight_firefly"));

    public static final DeferredHolder<EntityType<?>, EntityType<BellNeedleWasp>> BELL_NEEDLE_WASP =
            ENTITY_TYPES.register("bell_needle_wasp",
                    () -> EntityType.Builder.of(BellNeedleWasp::new, MobCategory.MONSTER)
                            .sized(0.85F, 0.35F)
                            .clientTrackingRange(10)
                            .build("bell_needle_wasp"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShardwingSkate>> SHARDWING_SKATE =
            ENTITY_TYPES.register("shardwing_skate",
                    () -> EntityType.Builder.of(ShardwingSkate::new, MobCategory.MONSTER)
                            .sized(0.45F, 0.30F)
                            .clientTrackingRange(10)
                            .build("shardwing_skate"));

    public static final DeferredHolder<EntityType<?>, EntityType<SailtailMinnow>> SAILTAIL_MINNOW =
            ENTITY_TYPES.register("sailtail_minnow",
                    () -> EntityType.Builder.of(SailtailMinnow::new, MobCategory.CREATURE)
                            .sized(0.60F, 0.25F)
                            .clientTrackingRange(10)
                            .build("sailtail_minnow"));

    public static final DeferredHolder<EntityType<?>, EntityType<MossSailSnail>> MOSS_SAIL_SNAIL =
            ENTITY_TYPES.register("moss_sail_snail",
                    () -> EntityType.Builder.of(MossSailSnail::new, MobCategory.CREATURE)
                            .sized(0.80F, 0.55F)
                            .clientTrackingRange(10)
                            .build("moss_sail_snail"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalSpineUrchin>> CRYSTAL_SPINE_URCHIN =
            ENTITY_TYPES.register("crystal_spine_urchin",
                    () -> EntityType.Builder.of(CrystalSpineUrchin::new, MobCategory.MONSTER)
                            .sized(0.40F, 0.35F)
                            .clientTrackingRange(10)
                            .build("crystal_spine_urchin"));

    public static final DeferredHolder<EntityType<?>, EntityType<AshSailSerpent>> ASH_SAIL_SERPENT =
            ENTITY_TYPES.register("ash_sail_serpent",
                    () -> EntityType.Builder.of(AshSailSerpent::new, MobCategory.MONSTER)
                            .sized(1.15F, 0.35F)
                            .clientTrackingRange(10)
                            .build("ash_sail_serpent"));



    public static final DeferredHolder<EntityType<?>, EntityType<MossbackHedgehog>> MOSSBACK_HEDGEHOG =
            ENTITY_TYPES.register("mossback_hedgehog",
                    () -> EntityType.Builder.of(MossbackHedgehog::new, MobCategory.CREATURE)
                            .sized(0.85F, 0.60F)
                            .clientTrackingRange(10)
                            .build("mossback_hedgehog"));


    public static final DeferredHolder<EntityType<?>, EntityType<EmberclawScorpion>> EMBERCLAW_SCORPION =
            ENTITY_TYPES.register("emberclaw_scorpion",
                    () -> EntityType.Builder.of(EmberclawScorpion::new, MobCategory.MONSTER)
                            .sized(0.65F, 0.30F)
                            .clientTrackingRange(10)
                            .build("emberclaw_scorpion"));


    public static final DeferredHolder<EntityType<?>, EntityType<LanternSeahorse>> LANTERN_SEAHORSE =
            ENTITY_TYPES.register("lantern_seahorse",
                    () -> EntityType.Builder.of(LanternSeahorse::new, MobCategory.WATER_AMBIENT)
                            .sized(0.45F, 0.70F)
                            .clientTrackingRange(10)
                            .build("lantern_seahorse"));


    public static final DeferredHolder<EntityType<?>, EntityType<ReefjawShark>> REEFJAW_SHARK =
            ENTITY_TYPES.register("reefjaw_shark",
                    () -> EntityType.Builder.of(ReefjawShark::new, MobCategory.MONSTER)
                            .sized(0.85F, 0.30F)
                            .clientTrackingRange(10)
                            .build("reefjaw_shark"));


    public static final DeferredHolder<EntityType<?>, EntityType<SlatehornBoar>> SLATEHORN_BOAR =
            ENTITY_TYPES.register("slatehorn_boar",
                    () -> EntityType.Builder.of(SlatehornBoar::new, MobCategory.MONSTER)
                            .sized(0.95F, 0.70F)
                            .clientTrackingRange(10)
                            .build("slatehorn_boar"));


    public static final DeferredHolder<EntityType<?>, EntityType<HollowReedSerpent>> HOLLOW_REED_SERPENT =
            ENTITY_TYPES.register("hollow_reed_serpent",
                    () -> EntityType.Builder.of(HollowReedSerpent::new, MobCategory.MONSTER)
                            .sized(1.40F, 0.30F)
                            .clientTrackingRange(10)
                            .build("hollow_reed_serpent"));


    public static final DeferredHolder<EntityType<?>, EntityType<RiftfangEel>> RIFTFANG_EEL =
            ENTITY_TYPES.register("riftfang_eel",
                    () -> EntityType.Builder.of(RiftfangEel::new, MobCategory.MONSTER)
                            .sized(1.40F, 0.30F)
                            .clientTrackingRange(10)
                            .build("riftfang_eel"));


    public static final DeferredHolder<EntityType<?>, EntityType<NeedlefinLionfish>> NEEDLEFIN_LIONFISH =
            ENTITY_TYPES.register("needlefin_lionfish",
                    () -> EntityType.Builder.of(NeedlefinLionfish::new, MobCategory.MONSTER)
                            .sized(0.60F, 0.40F)
                            .clientTrackingRange(10)
                            .build("needlefin_lionfish"));


    public static final DeferredHolder<EntityType<?>, EntityType<MossfangToad>> MOSSFANG_TOAD =
            ENTITY_TYPES.register("mossfang_toad",
                    () -> EntityType.Builder.of(MossfangToad::new, MobCategory.MONSTER)
                            .sized(0.70F, 0.45F)
                            .clientTrackingRange(10)
                            .build("mossfang_toad"));


    public static final DeferredHolder<EntityType<?>, EntityType<AmberbackBeetle>> AMBERBACK_BEETLE =
            ENTITY_TYPES.register("amberback_beetle",
                    () -> EntityType.Builder.of(AmberbackBeetle::new, MobCategory.MONSTER)
                            .sized(0.85F, 0.55F)
                            .clientTrackingRange(10)
                            .build("amberback_beetle"));


    public static final DeferredHolder<EntityType<?>, EntityType<LanternjawAngler>> LANTERNJAW_ANGLER =
            ENTITY_TYPES.register("lanternjaw_angler",
                    () -> EntityType.Builder.of(LanternjawAngler::new, MobCategory.MONSTER)
                            .sized(0.55F, 0.50F)
                            .clientTrackingRange(10)
                            .build("lanternjaw_angler"));


    public static final DeferredHolder<EntityType<?>, EntityType<SawtoothPrawn>> SAWTOOTH_PRAWN =
            ENTITY_TYPES.register("sawtooth_prawn",
                    () -> EntityType.Builder.of(SawtoothPrawn::new, MobCategory.MONSTER)
                            .sized(1.00F, 0.30F)
                            .clientTrackingRange(10)
                            .build("sawtooth_prawn"));

    public static final DeferredHolder<EntityType<?>, EntityType<ReedMaskRaider>> REED_MASK_RAIDER =
            ENTITY_TYPES.register("reed_mask_raider",
                    () -> EntityType.Builder.of(ReedMaskRaider::new, MobCategory.MONSTER)
                            .sized(0.70F, 1.50F)
                            .clientTrackingRange(10)
                            .build("reed_mask_raider"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellAxeman>> BROKEN_BELL_AXEMAN =
            ENTITY_TYPES.register("broken_bell_axeman",
                    () -> EntityType.Builder.of(BrokenBellAxeman::new, MobCategory.MONSTER)
                            .sized(0.75F, 1.30F)
                            .clientTrackingRange(10)
                            .build("broken_bell_axeman"));

    public static final DeferredHolder<EntityType<?>, EntityType<RiftPageStalker>> RIFT_PAGE_STALKER =
            ENTITY_TYPES.register("rift_page_stalker",
                    () -> EntityType.Builder.of(RiftPageStalker::new, MobCategory.MONSTER)
                            .sized(0.55F, 2.15F)
                            .clientTrackingRange(10)
                            .build("rift_page_stalker"));


    public static final DeferredHolder<EntityType<?>, EntityType<MossplateHound>> MOSSPLATE_HOUND =
            ENTITY_TYPES.register("mossplate_hound",
                    () -> EntityType.Builder.of(MossplateHound::new, MobCategory.MONSTER)
                            .sized(1.30F, 0.80F)
                            .clientTrackingRange(10)
                            .build("mossplate_hound"));


    public static final DeferredHolder<EntityType<?>, EntityType<CairnShellBrute>> CAIRN_SHELL_BRUTE =
            ENTITY_TYPES.register("cairn_shell_brute",
                    () -> EntityType.Builder.of(CairnShellBrute::new, MobCategory.MONSTER)
                            .sized(0.90F, 1.45F)
                            .clientTrackingRange(10)
                            .build("cairn_shell_brute"));


    public static final DeferredHolder<EntityType<?>, EntityType<SporeCrownedWretch>> SPORE_CROWNED_WRETCH =
            ENTITY_TYPES.register("spore_crowned_wretch",
                    () -> EntityType.Builder.of(SporeCrownedWretch::new, MobCategory.MONSTER)
                            .sized(0.45F, 1.80F)
                            .clientTrackingRange(10)
                            .build("spore_crowned_wretch"));


    public static final DeferredHolder<EntityType<?>, EntityType<cn.kurt6.unknown_echoes.entity.projectile.SporePellet>> SPORE_PELLET =
            ENTITY_TYPES.register("spore_pellet",
                    () -> EntityType.Builder.<cn.kurt6.unknown_echoes.entity.projectile.SporePellet>of(
                                    cn.kurt6.unknown_echoes.entity.projectile.SporePellet::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(2).build("spore_pellet"));

}
