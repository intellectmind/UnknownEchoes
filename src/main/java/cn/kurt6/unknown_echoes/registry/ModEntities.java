package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.BrokenBellKeeper;
import cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper;
import cn.kurt6.unknown_echoes.entity.boss.DreamBloomKeeper;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.LostRecorderChief;
import cn.kurt6.unknown_echoes.entity.boss.MirrorDustButler;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.entity.boss.SilentPriest;
import cn.kurt6.unknown_echoes.entity.boss.StormWeaver;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import cn.kurt6.unknown_echoes.entity.mob.BrokenBellCrow;
import cn.kurt6.unknown_echoes.entity.mob.CrystalFeatherBird;
import cn.kurt6.unknown_echoes.entity.mob.CrystalChimeSpirit;
import cn.kurt6.unknown_echoes.entity.mob.CrystalNoiseWisp;
import cn.kurt6.unknown_echoes.entity.mob.BrokenBellGuard;
import cn.kurt6.unknown_echoes.entity.mob.DreamingDeer;
import cn.kurt6.unknown_echoes.entity.mob.EchoDeer;
import cn.kurt6.unknown_echoes.entity.mob.EchoRemnant;
import cn.kurt6.unknown_echoes.entity.mob.EchoWanderer;
import cn.kurt6.unknown_echoes.entity.mob.GlowRabbit;
import cn.kurt6.unknown_echoes.entity.mob.LostRecorder;
import cn.kurt6.unknown_echoes.entity.mob.MirrorDustDecoy;
import cn.kurt6.unknown_echoes.entity.mob.MirrorTailFish;
import cn.kurt6.unknown_echoes.entity.mob.MossBackTurtle;
import cn.kurt6.unknown_echoes.entity.mob.SilentWalker;
import cn.kurt6.unknown_echoes.entity.mob.TideWaterShade;
import cn.kurt6.unknown_echoes.entity.mob.WaterShadow;
import cn.kurt6.unknown_echoes.entity.mob.WindErodedSentinel;
import cn.kurt6.unknown_echoes.entity.mob.WindSacMoth;
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

    public static final DeferredHolder<EntityType<?>, EntityType<EchoWanderer>> ECHO_WANDERER =
            ENTITY_TYPES.register("echo_wanderer",
                    () -> EntityType.Builder.of(EchoWanderer::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("echo_wanderer"));

    public static final DeferredHolder<EntityType<?>, EntityType<ForgottenColossus>> FORGOTTEN_COLOSSUS =
            ENTITY_TYPES.register("forgotten_colossus",
                    () -> EntityType.Builder.of(ForgottenColossus::new, MobCategory.MONSTER)
                            .sized(2.2F, 5.4F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build("forgotten_colossus"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssWatcher>> ABYSS_WATCHER =
            ENTITY_TYPES.register("abyss_watcher",
                    () -> EntityType.Builder.of(AbyssWatcher::new, MobCategory.MONSTER)
                            .sized(2.6F, 2.4F)
                            .clientTrackingRange(10)
                            .build("abyss_watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorGuardian>> MIRROR_GUARDIAN =
            ENTITY_TYPES.register("mirror_guardian",
                    () -> EntityType.Builder.of(MirrorGuardian::new, MobCategory.MONSTER)
                            .sized(1.1F, 3.2F)
                            .clientTrackingRange(10)
                            .build("mirror_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<SilentWalker>> SILENT_WALKER =
            ENTITY_TYPES.register("silent_walker",
                    () -> EntityType.Builder.of(SilentWalker::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.1F)
                            .clientTrackingRange(8)
                            .build("silent_walker"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalFeatherBird>> CRYSTAL_FEATHER_BIRD =
            ENTITY_TYPES.register("crystal_feather_bird",
                    () -> EntityType.Builder.of(CrystalFeatherBird::new, MobCategory.CREATURE)
                            .sized(0.5F, 0.7F)
                            .clientTrackingRange(8)
                            .build("crystal_feather_bird"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoProjectionEntity>> ECHO_PROJECTION =
            ENTITY_TYPES.register("echo_projection",
                    () -> EntityType.Builder.of(EchoProjectionEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .fireImmune()
                            .build("echo_projection"));

    // ---- V0.6A Mini Boss ----

    public static final DeferredHolder<EntityType<?>, EntityType<StormWeaver>> STORM_WEAVER =
            ENTITY_TYPES.register("storm_weaver",
                    () -> EntityType.Builder.of(StormWeaver::new, MobCategory.MONSTER)
                            .sized(1.8F, 1.6F)
                            .clientTrackingRange(10)
                            .build("storm_weaver"));

    public static final DeferredHolder<EntityType<?>, EntityType<SilentPriest>> SILENT_PRIEST =
            ENTITY_TYPES.register("silent_priest",
                    () -> EntityType.Builder.of(SilentPriest::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.4F)
                            .clientTrackingRange(10)
                            .build("silent_priest"));

    // ---- V0.6B 弹射物 ----

    /** 潮汐弩矢:潮汐弩专属弹射物(水下无衰减 + 符文方向标记)。 */
    public static final DeferredHolder<EntityType<?>, EntityType<TideBoltEntity>> TIDE_BOLT =
            ENTITY_TYPES.register("tide_bolt",
                    () -> EntityType.Builder.<TideBoltEntity>of(TideBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("tide_bolt"));

    // ---- V0.6C 镜湖 Mini Boss ----

    public static final DeferredHolder<EntityType<?>, EntityType<TideLanternKeeper>> TIDE_LANTERN_KEEPER =
            ENTITY_TYPES.register("tide_lantern_keeper",
                    () -> EntityType.Builder.of(TideLanternKeeper::new, MobCategory.MONSTER)
                            .sized(1.1F, 1.9F)
                            .clientTrackingRange(10)
                            .build("tide_lantern_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideWaterShade>> TIDE_WATER_SHADE =
            ENTITY_TYPES.register("tide_water_shade",
                    () -> EntityType.Builder.of(TideWaterShade::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.3F)
                            .clientTrackingRange(8)
                            .build("tide_water_shade"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDustButler>> MIRROR_DUST_BUTLER =
            ENTITY_TYPES.register("mirror_dust_butler",
                    () -> EntityType.Builder.of(MirrorDustButler::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.95F)
                            .clientTrackingRange(10)
                            .build("mirror_dust_butler"));

    // ---- V0.6D 迷途旅者 ----

    public static final DeferredHolder<EntityType<?>, EntityType<cn.kurt6.unknown_echoes.entity.mob.LostTraveler>> LOST_TRAVELER =
            ENTITY_TYPES.register("lost_traveler",
                    () -> EntityType.Builder.of(cn.kurt6.unknown_echoes.entity.mob.LostTraveler::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.9F)
                            .clientTrackingRange(10)
                            .build("lost_traveler"));

    // ---- V0.6F 普通环境生物(九章 9.1,只提供材料/食物/弱线索,不参与进度) ----

    public static final DeferredHolder<EntityType<?>, EntityType<EchoDeer>> ECHO_DEER =
            ENTITY_TYPES.register("echo_deer",
                    () -> EntityType.Builder.of(EchoDeer::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.6F)
                            .clientTrackingRange(8)
                            .build("echo_deer"));

    public static final DeferredHolder<EntityType<?>, EntityType<GlowRabbit>> GLOW_RABBIT =
            ENTITY_TYPES.register("glow_rabbit",
                    () -> EntityType.Builder.of(GlowRabbit::new, MobCategory.CREATURE)
                            .sized(0.45F, 0.5F)
                            .clientTrackingRange(8)
                            .build("glow_rabbit"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorTailFish>> MIRROR_TAIL_FISH =
            ENTITY_TYPES.register("mirror_tail_fish",
                    () -> EntityType.Builder.of(MirrorTailFish::new, MobCategory.WATER_AMBIENT)
                            .sized(0.5F, 0.35F)
                            .clientTrackingRange(4)
                            .build("mirror_tail_fish"));

    public static final DeferredHolder<EntityType<?>, EntityType<MossBackTurtle>> MOSS_BACK_TURTLE =
            ENTITY_TYPES.register("moss_back_turtle",
                    () -> EntityType.Builder.of(MossBackTurtle::new, MobCategory.CREATURE)
                            .sized(1.0F, 0.5F)
                            .clientTrackingRange(8)
                            .build("moss_back_turtle"));

    // ---- V0.7C 后期群系生态实体基础 ----

    public static final DeferredHolder<EntityType<?>, EntityType<DreamingDeer>> DREAMING_DEER =
            ENTITY_TYPES.register("dreaming_deer",
                    () -> EntityType.Builder.of(DreamingDeer::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.55F)
                            .clientTrackingRange(8)
                            .build("dreaming_deer"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalChimeSpirit>> CRYSTAL_CHIME_SPIRIT =
            ENTITY_TYPES.register("crystal_chime_spirit",
                    () -> EntityType.Builder.of(CrystalChimeSpirit::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.9F)
                            .clientTrackingRange(8)
                            .build("crystal_chime_spirit"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellGuard>> BROKEN_BELL_GUARD =
            ENTITY_TYPES.register("broken_bell_guard",
                    () -> EntityType.Builder.of(BrokenBellGuard::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.2F)
                            .clientTrackingRange(8)
                            .build("broken_bell_guard"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalNoiseWisp>> CRYSTAL_NOISE_WISP =
            ENTITY_TYPES.register("crystal_noise_wisp",
                    () -> EntityType.Builder.of(CrystalNoiseWisp::new, MobCategory.MONSTER)
                            .sized(0.6F, 0.9F)
                            .clientTrackingRange(8)
                            .build("crystal_noise_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellCrow>> BROKEN_BELL_CROW =
            ENTITY_TYPES.register("broken_bell_crow",
                    () -> EntityType.Builder.of(BrokenBellCrow::new, MobCategory.CREATURE)
                            .sized(0.55F, 0.65F)
                            .clientTrackingRange(8)
                            .build("broken_bell_crow"));

    public static final DeferredHolder<EntityType<?>, EntityType<LostRecorder>> LOST_RECORDER =
            ENTITY_TYPES.register("lost_recorder",
                    () -> EntityType.Builder.of(LostRecorder::new, MobCategory.MONSTER)
                            .sized(0.65F, 1.95F)
                            .clientTrackingRange(8)
                            .build("lost_recorder"));

    public static final DeferredHolder<EntityType<?>, EntityType<WindSacMoth>> WIND_SAC_MOTH =
            ENTITY_TYPES.register("wind_sac_moth",
                    () -> EntityType.Builder.of(WindSacMoth::new, MobCategory.CREATURE)
                            .sized(0.55F, 0.45F)
                            .clientTrackingRange(8)
                            .build("wind_sac_moth"));

    public static final DeferredHolder<EntityType<?>, EntityType<WindErodedSentinel>> WIND_ERODED_SENTINEL =
            ENTITY_TYPES.register("wind_eroded_sentinel",
                    () -> EntityType.Builder.of(WindErodedSentinel::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(8)
                            .build("wind_eroded_sentinel"));

    public static final DeferredHolder<EntityType<?>, EntityType<EchoRemnant>> ECHO_REMNANT =
            ENTITY_TYPES.register("echo_remnant",
                    () -> EntityType.Builder.of(EchoRemnant::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.85F)
                            .clientTrackingRange(8)
                            .build("echo_remnant"));

    public static final DeferredHolder<EntityType<?>, EntityType<WaterShadow>> WATER_SHADOW =
            ENTITY_TYPES.register("water_shadow",
                    () -> EntityType.Builder.of(WaterShadow::new, MobCategory.MONSTER)
                            .sized(0.55F, 1.2F)
                            .clientTrackingRange(8)
                            .build("water_shadow"));

    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDustDecoy>> MIRROR_DUST_DECOY =
            ENTITY_TYPES.register("mirror_dust_decoy",
                    () -> EntityType.Builder.of(MirrorDustDecoy::new, MobCategory.MONSTER)
                            .sized(0.55F, 1.55F)
                            .clientTrackingRange(8)
                            .build("mirror_dust_decoy"));

    public static final DeferredHolder<EntityType<?>, EntityType<CrystalSongkeeper>> CRYSTAL_SONGKEEPER =
            ENTITY_TYPES.register("crystal_songkeeper",
                    () -> EntityType.Builder.of(CrystalSongkeeper::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.2F)
                            .clientTrackingRange(10)
                            .build("crystal_songkeeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenBellKeeper>> BROKEN_BELL_KEEPER =
            ENTITY_TYPES.register("broken_bell_keeper",
                    () -> EntityType.Builder.of(BrokenBellKeeper::new, MobCategory.MONSTER)
                            .sized(1.0F, 2.4F)
                            .clientTrackingRange(10)
                            .build("broken_bell_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<DreamBloomKeeper>> DREAM_BLOOM_KEEPER =
            ENTITY_TYPES.register("dream_bloom_keeper",
                    () -> EntityType.Builder.of(DreamBloomKeeper::new, MobCategory.MONSTER)
                            .sized(0.95F, 1.8F)
                            .clientTrackingRange(10)
                            .build("dream_bloom_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<LostRecorderChief>> LOST_RECORDER_CHIEF =
            ENTITY_TYPES.register("lost_recorder_chief",
                    () -> EntityType.Builder.of(LostRecorderChief::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.25F)
                            .clientTrackingRange(10)
                            .build("lost_recorder_chief"));
}
