package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Boss BGM 使用独立 Ogg 文件；实体短音效引用原版事件做变调。
 * sounds 数组是加权随机变体池,每次发声只播一条,不是叠加混音。
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, UnknownEchoes.MODID);

    // ── 能力表现 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_GLIDE =
            register("ability.wind_echo.glide");

    // ── Boss 战音轨 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_MUSIC =
            register("music.forgotten_colossus");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_MUSIC =
            register("music.abyss_watcher");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_MUSIC =
            register("music.mirror_guardian");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_MUSIC =
            register("music.void_prophet");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_MUSIC =
            register("music.rift_warden");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_MUSIC =
            register("music.world_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIBOSS_MUSIC =
            register("music.miniboss");

    // ── 回响森林 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_AMBIENT =
            register("entity.echo_wanderer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_HURT =
            register("entity.echo_wanderer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_DEATH =
            register("entity.echo_wanderer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_REMNANT_AMBIENT =
            register("entity.echo_remnant.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_REMNANT_HURT =
            register("entity.echo_remnant.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_REMNANT_DEATH =
            register("entity.echo_remnant.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_DEER_AMBIENT =
            register("entity.echo_deer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_DEER_HURT =
            register("entity.echo_deer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_DEER_DEATH =
            register("entity.echo_deer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_RABBIT_AMBIENT =
            register("entity.glow_rabbit.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_RABBIT_HURT =
            register("entity.glow_rabbit.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_RABBIT_DEATH =
            register("entity.glow_rabbit.death");

    // ── 主线 Boss：遗忘巨像 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_AMBIENT =
            register("entity.forgotten_colossus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_HURT =
            register("entity.forgotten_colossus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_DEATH =
            register("entity.forgotten_colossus.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_STEP =
            register("entity.forgotten_colossus.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_STOMP =
            register("entity.forgotten_colossus.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_ATTACK =
            register("entity.forgotten_colossus.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_ROCK_RAIN =
            register("entity.forgotten_colossus.rock_rain");

    // ── 主线 Boss：深渊观测者 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_AMBIENT =
            register("entity.abyss_watcher.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_AMBIENT_LAND =
            register("entity.abyss_watcher.ambient_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_HURT =
            register("entity.abyss_watcher.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_HURT_LAND =
            register("entity.abyss_watcher.hurt_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_DEATH =
            register("entity.abyss_watcher.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_DEATH_LAND =
            register("entity.abyss_watcher.death_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_BURST =
            register("entity.abyss_watcher.burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_DASH =
            register("entity.abyss_watcher.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_VORTEX =
            register("entity.abyss_watcher.vortex");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_BLADES =
            register("entity.abyss_watcher.blades");

    // ── 主线 Boss：镜像守护者 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_AMBIENT =
            register("entity.mirror_guardian.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_HURT =
            register("entity.mirror_guardian.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_DEATH =
            register("entity.mirror_guardian.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK =
            register("entity.mirror_guardian.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_SWAP =
            register("entity.mirror_guardian.swap");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_SHATTER =
            register("entity.mirror_guardian.shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_NOVA =
            register("entity.mirror_guardian.nova");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_COLLAPSE =
            register("entity.mirror_guardian.collapse");

    // ── 主线 Boss：裂隙看守者 / 虚空先知 / 世界回响体 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_AMBIENT =
            register("entity.void_prophet.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_HURT =
            register("entity.void_prophet.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_SONIC =
            register("entity.void_prophet.sonic");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_SUMMON =
            register("entity.void_prophet.summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_AMBIENT =
            register("entity.rift_warden.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_HURT =
            register("entity.rift_warden.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_DEATH =
            register("entity.rift_warden.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_RIFT =
            register("entity.rift_warden.rift");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_AMBIENT =
            register("entity.world_echo.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_HURT =
            register("entity.world_echo.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_DEATH =
            register("entity.world_echo.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_FLAP =
            register("entity.world_echo.flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_PHASE =
            register("entity.world_echo.phase");

    // ── Mini Boss：风暴编织者 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_AMBIENT =
            register("entity.storm_weaver.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_HURT =
            register("entity.storm_weaver.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_DEATH =
            register("entity.storm_weaver.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_SHATTER =
            register("entity.storm_weaver.shatter");

    // ── Mini Boss：沉默祭司 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_AMBIENT =
            register("entity.silent_priest.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_HURT =
            register("entity.silent_priest.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_DEATH =
            register("entity.silent_priest.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_WAVE =
            register("entity.silent_priest.wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_DOMAIN =
            register("entity.silent_priest.domain");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_MARK =
            register("entity.silent_priest.mark");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_SWEEP =
            register("entity.silent_priest.sweep");

    // ── Mini Boss：潮汐执灯者 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_AMBIENT =
            register("entity.tide_lantern_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_HURT =
            register("entity.tide_lantern_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_DEATH =
            register("entity.tide_lantern_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_BEAM =
            register("entity.tide_lantern_keeper.beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_BEAM_WARN =
            register("entity.tide_lantern_keeper.beam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_DASH =
            register("entity.tide_lantern_keeper.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_TAIL_SWEEP =
            register("entity.tide_lantern_keeper.tail_sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_SHATTER =
            register("entity.tide_lantern_keeper.shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_RELIGHT =
            register("entity.tide_lantern_keeper.relight");

    // ── Mini Boss：镜尘执事 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_AMBIENT =
            register("entity.mirror_dust_butler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_HURT =
            register("entity.mirror_dust_butler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_DEATH =
            register("entity.mirror_dust_butler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_ATTACK =
            register("entity.mirror_dust_butler.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_VOLLEY =
            register("entity.mirror_dust_butler.volley");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_VEIL =
            register("entity.mirror_dust_butler.veil");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_SHATTER =
            register("entity.mirror_dust_butler.shatter");

    // ── Mini Boss：晶歌守谱者 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_AMBIENT =
            register("entity.crystal_songkeeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_HURT =
            register("entity.crystal_songkeeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_DEATH =
            register("entity.crystal_songkeeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_ATTACK =
            register("entity.crystal_songkeeper.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_NOTE_BOLT =
            register("entity.crystal_songkeeper.note_bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_WAVE =
            register("entity.crystal_songkeeper.wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_BIG_WAVE =
            register("entity.crystal_songkeeper.big_wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_TUNE_DEMO =
            register("entity.crystal_songkeeper.tune_demo");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_TUNE_INPUT =
            register("entity.crystal_songkeeper.tune_input");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_TUNE_FAIL =
            register("entity.crystal_songkeeper.tune_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_CHARGE =
            register("entity.crystal_songkeeper.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_STAGGER =
            register("entity.crystal_songkeeper.stagger");

    // ── Mini Boss：残钟看守 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_AMBIENT =
            register("entity.broken_bell_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_HURT =
            register("entity.broken_bell_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_DEATH =
            register("entity.broken_bell_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_ATTACK =
            register("entity.broken_bell_keeper.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_TOLL =
            register("entity.broken_bell_keeper.toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_GEARBURST =
            register("entity.broken_bell_keeper.gearburst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_EXPOSED =
            register("entity.broken_bell_keeper.exposed");

    // ── Mini Boss：沉眠花守 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_AMBIENT =
            register("entity.dream_bloom_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_HURT =
            register("entity.dream_bloom_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_DEATH =
            register("entity.dream_bloom_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_BLOOM =
            register("entity.dream_bloom_keeper.bloom");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_BUDS =
            register("entity.dream_bloom_keeper.buds");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_SPORE_BOLT =
            register("entity.dream_bloom_keeper.spore_bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_MIST =
            register("entity.dream_bloom_keeper.mist");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_NOVA =
            register("entity.dream_bloom_keeper.nova");

    // ── Mini Boss：迷失记录者首领 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_AMBIENT =
            register("entity.lost_recorder_chief.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_HURT =
            register("entity.lost_recorder_chief.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_DEATH =
            register("entity.lost_recorder_chief.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_REVIEW =
            register("entity.lost_recorder_chief.review");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_EXPOSED =
            register("entity.lost_recorder_chief.exposed");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_INKBOLT =
            register("entity.lost_recorder_chief.inkbolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_PAGESTORM =
            register("entity.lost_recorder_chief.pagestorm");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_PULSE =
            register("entity.lost_recorder_chief.pulse");

    // ── Mini Boss：倒悬司阍 / 灰烬抄录员 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_AMBIENT =
            register("entity.inverted_gatekeeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_HURT =
            register("entity.inverted_gatekeeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_DEATH =
            register("entity.inverted_gatekeeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK =
            register("entity.inverted_gatekeeper.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_INVERT =
            register("entity.inverted_gatekeeper.invert");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_AMBIENT =
            register("entity.ash_scribe.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_HURT =
            register("entity.ash_scribe.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_DEATH =
            register("entity.ash_scribe.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK =
            register("entity.ash_scribe.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_TRANSCRIBE =
            register("entity.ash_scribe.transcribe");

    // ── 失语沼泽与镜湖敌怪 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_WALKER_HURT =
            register("entity.silent_walker.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_WALKER_DEATH =
            register("entity.silent_walker.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_SHADOW_AMBIENT =
            register("entity.water_shadow.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_SHADOW_HURT =
            register("entity.water_shadow.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_SHADOW_DEATH =
            register("entity.water_shadow.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATER_SHADOW_STRIKE =
            register("entity.water_shadow.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_WATER_SHADE_HURT =
            register("entity.tide_water_shade.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_WATER_SHADE_DEATH =
            register("entity.tide_water_shade.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_WATER_SHADE_STRIKE =
            register("entity.tide_water_shade.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_AMBIENT =
            register("entity.mirror_dust_decoy.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_HURT =
            register("entity.mirror_dust_decoy.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_DEATH =
            register("entity.mirror_dust_decoy.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_STRIKE =
            register("entity.mirror_dust_decoy.attack.strike");

    // ── 回声峭壁与晶歌林地敌怪 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_ERODED_SENTINEL_AMBIENT =
            register("entity.wind_eroded_sentinel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_ERODED_SENTINEL_HURT =
            register("entity.wind_eroded_sentinel.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_ERODED_SENTINEL_DEATH =
            register("entity.wind_eroded_sentinel.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_ERODED_SENTINEL_SLASH =
            register("entity.wind_eroded_sentinel.slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_NOISE_WISP_AMBIENT =
            register("entity.crystal_noise_wisp.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_NOISE_WISP_HURT =
            register("entity.crystal_noise_wisp.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_NOISE_WISP_DEATH =
            register("entity.crystal_noise_wisp.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_NOISE_WISP_JAM =
            register("entity.crystal_noise_wisp.jam");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_GUARD_AMBIENT =
            register("entity.broken_bell_guard.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_GUARD_HURT =
            register("entity.broken_bell_guard.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_GUARD_DEATH =
            register("entity.broken_bell_guard.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_GUARD_TOLL =
            register("entity.broken_bell_guard.toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_AMBIENT =
            register("entity.lost_recorder.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_HURT =
            register("entity.lost_recorder.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_DEATH =
            register("entity.lost_recorder.death");

    // ── 深层回响敌怪 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_LURKER_AMBIENT =
            register("entity.abyss_lurker.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_LURKER_HURT =
            register("entity.abyss_lurker.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_LURKER_DEATH =
            register("entity.abyss_lurker.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_LURKER_POUNCE =
            register("entity.abyss_lurker.pounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_WANDERER_AMBIENT =
            register("entity.void_wanderer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_WANDERER_HURT =
            register("entity.void_wanderer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_WANDERER_DEATH =
            register("entity.void_wanderer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_WANDERER_PHASE =
            register("entity.void_wanderer.phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDED_MANTIS_AMBIENT =
            register("entity.folded_mantis.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDED_MANTIS_HURT =
            register("entity.folded_mantis.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDED_MANTIS_DEATH =
            register("entity.folded_mantis.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDED_MANTIS_SLASH =
            register("entity.folded_mantis.slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_RAM_AMBIENT =
            register("entity.gravity_ram.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_RAM_HURT =
            register("entity.gravity_ram.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_RAM_DEATH =
            register("entity.gravity_ram.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVITY_RAM_CHARGE =
            register("entity.gravity_ram.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHVEIL_STALKER_AMBIENT =
            register("entity.ashveil_stalker.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHVEIL_STALKER_HURT =
            register("entity.ashveil_stalker.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHVEIL_STALKER_DEATH =
            register("entity.ashveil_stalker.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHVEIL_STALKER_POUNCE =
            register("entity.ashveil_stalker.pounce");

    // ── 生态生物 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDPAGE_BUG_AMBIENT =
            register("entity.foldpage_bug.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDPAGE_BUG_HURT =
            register("entity.foldpage_bug.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDPAGE_BUG_DEATH =
            register("entity.foldpage_bug.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLDPAGE_BUG_GUIDE =
            register("entity.foldpage_bug.guide");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_SHELL_BEAST_AMBIENT =
            register("entity.anchor_shell_beast.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_SHELL_BEAST_HURT =
            register("entity.anchor_shell_beast.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_SHELL_BEAST_DEATH =
            register("entity.anchor_shell_beast.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_SHELL_BEAST_PULSE =
            register("entity.anchor_shell_beast.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAMING_DEER_AMBIENT =
            register("entity.dreaming_deer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAMING_DEER_HURT =
            register("entity.dreaming_deer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAMING_DEER_DEATH =
            register("entity.dreaming_deer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_FEATHER_BIRD_AMBIENT =
            register("entity.crystal_feather_bird.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_FEATHER_BIRD_HURT =
            register("entity.crystal_feather_bird.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_FEATHER_BIRD_DEATH =
            register("entity.crystal_feather_bird.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_CROW_AMBIENT =
            register("entity.broken_bell_crow.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_CROW_HURT =
            register("entity.broken_bell_crow.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_CROW_DEATH =
            register("entity.broken_bell_crow.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_SAC_MOTH_AMBIENT =
            register("entity.wind_sac_moth.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_SAC_MOTH_HURT =
            register("entity.wind_sac_moth.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_SAC_MOTH_DEATH =
            register("entity.wind_sac_moth.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_CHIME_SPIRIT_AMBIENT =
            register("entity.crystal_chime_spirit.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_CHIME_SPIRIT_HURT =
            register("entity.crystal_chime_spirit.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_CHIME_SPIRIT_DEATH =
            register("entity.crystal_chime_spirit.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_BACK_TURTLE_AMBIENT =
            register("entity.moss_back_turtle.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_BACK_TURTLE_HURT =
            register("entity.moss_back_turtle.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_BACK_TURTLE_DEATH =
            register("entity.moss_back_turtle.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUSHLIGHT_JELLY_AMBIENT =
            register("entity.hushlight_jelly.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUSHLIGHT_JELLY_HURT =
            register("entity.hushlight_jelly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUSHLIGHT_JELLY_DEATH =
            register("entity.hushlight_jelly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_GLIDER_AMBIENT =
            register("entity.rift_glider.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_GLIDER_HURT =
            register("entity.rift_glider.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_GLIDER_DEATH =
            register("entity.rift_glider.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHBACK_GRAZER_AMBIENT =
            register("entity.ashback_grazer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHBACK_GRAZER_HURT =
            register("entity.ashback_grazer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASHBACK_GRAZER_DEATH =
            register("entity.ashback_grazer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_TAIL_FISH_AMBIENT =
            register("entity.mirror_tail_fish.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_TAIL_FISH_HURT =
            register("entity.mirror_tail_fish.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_TAIL_FISH_DEATH =
            register("entity.mirror_tail_fish.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_TAIL_FISH_FLOP =
            register("entity.mirror_tail_fish.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_CUTTLEFISH_AMBIENT =
            register("entity.echo_cuttlefish.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_CUTTLEFISH_HURT =
            register("entity.echo_cuttlefish.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_CUTTLEFISH_DEATH =
            register("entity.echo_cuttlefish.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_CUTTLEFISH_FLOP =
            register("entity.echo_cuttlefish.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEARLBACK_RAY_AMBIENT =
            register("entity.pearlback_ray.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEARLBACK_RAY_HURT =
            register("entity.pearlback_ray.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEARLBACK_RAY_DEATH =
            register("entity.pearlback_ray.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PEARLBACK_RAY_FLOP =
            register("entity.pearlback_ray.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_TRAVELER_AMBIENT =
            register("entity.lost_traveler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_TRAVELER_HURT =
            register("entity.lost_traveler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_TRAVELER_DEATH =
            register("entity.lost_traveler.death");

    // ── 武器、神器与世界事件 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_BROADSWORD_PRIMARY =
            register("weapon.echo_broadsword.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_BROADSWORD_SECONDARY =
            register("weapon.echo_broadsword.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_BATTLEAXE_PRIMARY =
            register("weapon.echo_battleaxe.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_BATTLEAXE_SECONDARY =
            register("weapon.echo_battleaxe.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_LONGBOW_PRIMARY =
            register("weapon.echo_longbow.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_LONGBOW_SECONDARY =
            register("weapon.echo_longbow.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_WIND_SPEAR_PRIMARY =
            register("weapon.wind_spear.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_WIND_SPEAR_SECONDARY =
            register("weapon.wind_spear.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TIDE_CROSSBOW_PRIMARY =
            register("weapon.tide_crossbow.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TIDE_CROSSBOW_SECONDARY =
            register("weapon.tide_crossbow.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRUE_SIGHT_BLADE_PRIMARY =
            register("weapon.true_sight_blade.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRUE_SIGHT_BLADE_SECONDARY =
            register("weapon.true_sight_blade.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SILENCE_SCYTHE_PRIMARY =
            register("weapon.silence_scythe.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SILENCE_SCYTHE_SECONDARY =
            register("weapon.silence_scythe.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CRYSTAL_SONG_STAFF_PRIMARY =
            register("weapon.crystal_song_staff.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_CRYSTAL_SONG_STAFF_SECONDARY =
            register("weapon.crystal_song_staff.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BROKEN_BELL_HAMMER_PRIMARY =
            register("weapon.broken_bell_hammer.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BROKEN_BELL_HAMMER_SECONDARY =
            register("weapon.broken_bell_hammer.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_DREAM_BLOOM_BOW_PRIMARY =
            register("weapon.dream_bloom_bow.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_DREAM_BLOOM_BOW_SECONDARY =
            register("weapon.dream_bloom_bow.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRAVITY_INVERSION_BOW_PRIMARY =
            register("weapon.gravity_inversion_bow.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRAVITY_INVERSION_BOW_SECONDARY =
            register("weapon.gravity_inversion_bow.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ASH_SCRIPT_CROSSBOW_PRIMARY =
            register("weapon.ash_script_crossbow.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ASH_SCRIPT_CROSSBOW_SECONDARY =
            register("weapon.ash_script_crossbow.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_OATHBLADE_PRIMARY =
            register("weapon.echo_oathblade.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ECHO_OATHBLADE_SECONDARY =
            register("weapon.echo_oathblade.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRAVITY_HAMMER_PRIMARY =
            register("weapon.gravity_hammer.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_GRAVITY_HAMMER_SECONDARY =
            register("weapon.gravity_hammer.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RIFT_DAGGER_PRIMARY =
            register("weapon.rift_dagger.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_RIFT_DAGGER_SECONDARY =
            register("weapon.rift_dagger.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ANCHOR_BLADE_PRIMARY =
            register("weapon.anchor_blade.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ANCHOR_BLADE_SECONDARY =
            register("weapon.anchor_blade.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FOLDED_GLAIVE_PRIMARY =
            register("weapon.folded_glaive.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FOLDED_GLAIVE_SECONDARY =
            register("weapon.folded_glaive.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_PALIMPSEST_SABER_PRIMARY =
            register("weapon.palimpsest_saber.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_PALIMPSEST_SABER_SECONDARY =
            register("weapon.palimpsest_saber.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ORRERY_LANCE_PRIMARY =
            register("weapon.orrery_lance.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_ORRERY_LANCE_SECONDARY =
            register("weapon.orrery_lance.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_WORLD_RIFT_GREATSWORD_PRIMARY =
            register("weapon.world_rift_greatsword.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_WORLD_RIFT_GREATSWORD_SECONDARY =
            register("weapon.world_rift_greatsword.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FIVEFOLD_STAR_SPEAR_PRIMARY =
            register("weapon.fivefold_star_spear.primary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_FIVEFOLD_STAR_SPEAR_SECONDARY =
            register("weapon.fivefold_star_spear.secondary");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRUE_SIGHT_BLADE_MARK_ONE =
            register("weapon.true_sight_blade.mark_one");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRUE_SIGHT_BLADE_MARK_TWO =
            register("weapon.true_sight_blade.mark_two");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_TRUE_SIGHT_BLADE_SHATTER =
            register("weapon.true_sight_blade.shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_STORM_COMPASS_USE =
            register("artifact.storm_compass.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_TIDE_LANTERN_USE =
            register("artifact.tide_lantern.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_ECHO_LENS_USE =
            register("artifact.echo_lens.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_TIME_HOURGLASS_USE =
            register("artifact.time_hourglass.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_DIMENSION_ANCHOR_USE =
            register("artifact.dimension_anchor.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_RIFT_ANCHOR_USE =
            register("artifact.rift_anchor.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_DIMENSION_ANCHOR_BIND =
            register("artifact.dimension_anchor.bind");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_RIFT_ANCHOR_BIND =
            register("artifact.rift_anchor.bind");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_GRAVITY_ECHO_ATTUNE =
            register("ability.gravity_echo.attune");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABILITY_VOID_ECHO_STEP =
            register("ability.void_echo.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_ECHO_FOG_PULSE =
            register("world_event.echo_fog.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_MIRROR_ANOMALY_PULSE =
            register("world_event.mirror_anomaly.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_STORM_OMEN_PULSE =
            register("world_event.storm_omen.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_FAR_BELL_PROCESSION_PULSE =
            register("world_event.far_bell_procession.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_MEMORY_FIREFLY_TIDE_PULSE =
            register("world_event.memory_firefly_tide.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_GRAVITY_TIDE_PULSE =
            register("world_event.gravity_tide.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_ARCHIVE_DRIFT_PULSE =
            register("world_event.archive_drift.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_SILENT_PULSE_PULSE =
            register("world_event.silent_pulse.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_FOLDED_PROCESSION_PULSE =
            register("world_event.folded_procession.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_CRYSTAL_CHORUS_PULSE =
            register("world_event.crystal_chorus.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_RIFT_AFTERIMAGE_PULSE =
            register("world_event.rift_afterimage.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_INTERLAYER_RESPITE_PULSE =
            register("world_event.interlayer_respite.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_METEOR_AFTERGLOW_PULSE =
            register("world_event.meteor_afterglow.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_ECHO_STORM_PULSE =
            register("world_event.echo_storm.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_EVENT_STRATA_AFTERSHOCK_PULSE =
            register("world_event.strata_aftershock.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PUZZLE_SOLVE =
            register("block.puzzle.solve");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTILE_TIDE_BOLT_BURST =
            register("projectile.tide_bolt.burst");

    // ── 机关演出阶段 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_OPEN =
            register("block.world_resonance.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_CONNECT =
            register("block.world_resonance.connect");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_FOCUS =
            register("block.world_resonance.focus");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_READY =
            register("block.world_resonance.ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_COLLAPSE =
            register("block.world_resonance.collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_WIND =
            register("block.world_resonance.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_TIDE =
            register("block.world_resonance.tide");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_SIGHT =
            register("block.world_resonance.sight");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_GRAVITY =
            register("block.world_resonance.gravity");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_VOID =
            register("block.world_resonance.void");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ENDING_RECORD_WRITE =
            register("block.ending_record.write");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ENDING_RECORD_SEAL =
            register("block.ending_record.seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ENDING_RECORD_ARCHIVE =
            register("block.ending_record.archive");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GRAVITY_FIELD_ACTIVATE =
            register("block.gravity_field.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GRAVITY_FIELD_DEACTIVATE =
            register("block.gravity_field.deactivate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GRAVITY_FIELD_RISE =
            register("block.gravity_field.rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GRAVITY_FIELD_FALL =
            register("block.gravity_field.fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_ROUTE_WIND =
            register("block.deep_route.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_ROUTE_TIDE =
            register("block.deep_route.tide");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_ROUTE_SIGHT =
            register("block.deep_route.sight");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_ROUTE_COMPLETE =
            register("block.deep_route.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_CALIBRATION_WIND =
            register("block.deep_calibration.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_CALIBRATION_TIDE =
            register("block.deep_calibration.tide");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_CALIBRATION_SIGHT =
            register("block.deep_calibration.sight");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_CALIBRATION_COMPLETE =
            register("block.deep_calibration.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SPACE_ANCHOR_ACTIVATE =
            register("block.space_anchor.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SPACE_ANCHOR_COMPLETE =
            register("block.space_anchor.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_VOID_STABILIZER_ACTIVATE =
            register("block.void_stabilizer.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_VOID_STABILIZER_COMPLETE =
            register("block.void_stabilizer.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_GRAVITY_CORE_MANIFEST =
            register("block.gravity_core.manifest");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_VOID_CORE_MANIFEST =
            register("block.void_core.manifest");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WORLD_RESONANCE_MANIFEST =
            register("block.world_resonance.manifest");

    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ENTRANCE_PULSE =
            register("entity.world_echo.entrance_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ENTRANCE_READY =
            register("entity.world_echo.entrance_ready");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WIND_PLATFORM_ACTIVATE =
            register("block.wind_platform.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WIND_PLATFORM_LOCKED =
            register("block.wind_platform.locked");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_DOOR_OPEN =
            register("block.tide_door.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_DOOR_CLOSE =
            register("block.tide_door.close");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_DOOR_LOCKED =
            register("block.tide_door.locked");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_BUOY_EXTINGUISH =
            register("block.tide_buoy.extinguish");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_BUOY_RELIGHT =
            register("block.tide_buoy.relight");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_OBSERVATORY_READ =
            register("block.observatory.read");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_OBSERVATORY_LOCKED =
            register("block.observatory.locked");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PROVISION_FOLDED =
            register("block.deep_provision.folded");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PROVISION_GRAVITY =
            register("block.deep_provision.gravity");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PROVISION_HUSHED =
            register("block.deep_provision.hushed");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PROVISION_COOL =
            register("block.deep_provision.cool");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_RUNE_GUARDED =
            register("block.tide_rune.guarded");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_RUNE_PURIFY =
            register("block.tide_rune.purify");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_RUNE_READ =
            register("block.tide_rune.read");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_PILLAR_ACTIVATE =
            register("block.tide_pillar.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_SEAT_FLASH =
            register("block.tide_seat.flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TIDE_SEAT_READY =
            register("block.tide_seat.ready");

    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_FIRE_HIT =
            register("particle.fire.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_FROST_HIT =
            register("particle.frost.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_VENOM_HIT =
            register("particle.venom.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_THUNDER_HIT =
            register("particle.thunder.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_VOID_HIT =
            register("particle.void.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_HOLY_HIT =
            register("particle.holy.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_SHADOW_HIT =
            register("particle.shadow.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_EARTH_HIT =
            register("particle.earth.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_RESONANCE =
            register("particle.resonance");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_UPGRADE =
            register("block.particle_enchanting.upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_INJECT =
            register("block.particle_enchanting.inject");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_REJECT =
            register("block.particle_enchanting.reject");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTICLE_PURIFY =
            register("block.particle_enchanting.purify");

    // Boss 攻击动作
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_STOMP_WARN =
            register("entity.forgotten_colossus.attack.stomp_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_SLAM_WARN =
            register("entity.forgotten_colossus.attack.slam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_BEAM_WARN =
            register("entity.forgotten_colossus.attack.beam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_ROCKS_LAND =
            register("entity.forgotten_colossus.attack.rocks_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_BEAM_RELEASE =
            register("entity.forgotten_colossus.attack.beam_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGOTTEN_COLOSSUS_ATTACK_SLAM_HIT =
            register("entity.forgotten_colossus.attack.slam_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_SLASH_WARN =
            register("entity.mirror_guardian.attack.slash_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_IMAGE_WARN =
            register("entity.mirror_guardian.attack.image_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_NOVA_WARN =
            register("entity.mirror_guardian.attack.nova_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_BARRAGE_WARN =
            register("entity.mirror_guardian.attack.barrage_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_SPIKE_WARN =
            register("entity.mirror_guardian.attack.spike_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_ATTACK_SPIKE_RELEASE =
            register("entity.mirror_guardian.attack.spike_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_ATTACK_DIVE_RELEASE =
            register("entity.storm_weaver.attack.dive_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_ATTACK_BLADES_WARN =
            register("entity.storm_weaver.attack.blades_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_ATTACK_CYCLONE =
            register("entity.storm_weaver.attack.cyclone");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_ATTACK_DIVE_WARN =
            register("entity.storm_weaver.attack.dive_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_ATTACK_BLADE_RELEASE =
            register("entity.storm_weaver.attack.blade_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_ATTACK_DASH_RELEASE =
            register("entity.abyss_watcher.attack.dash_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_ATTACK_BLADES_HIT =
            register("entity.abyss_watcher.attack.blades_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_ATTACK_VORTEX_PULL =
            register("entity.abyss_watcher.attack.vortex_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_ATTACK_BURST_HIT =
            register("entity.abyss_watcher.attack.burst_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_ATTACK_MELEE =
            register("entity.abyss_watcher.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_ATTACK_MELEE =
            register("entity.tide_lantern_keeper.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_ATTACK_MELEE =
            register("entity.dream_bloom_keeper.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_ATTACK_MELEE =
            register("entity.lost_recorder_chief.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_MELEE =
            register("entity.inverted_gatekeeper.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_MELEE =
            register("entity.ash_scribe.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_ATTACK_MELEE =
            register("entity.silent_priest.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_ATTACK_SWEEP_WARN =
            register("entity.silent_priest.attack.sweep_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_ATTACK_WAVE_RELEASE =
            register("entity.silent_priest.attack.wave_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_ATTACK_MARK_RELEASE =
            register("entity.silent_priest.attack.mark_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_ATTACK_DASH_WARN =
            register("entity.tide_lantern_keeper.attack.dash_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_ATTACK_VOLLEY_WARN =
            register("entity.mirror_dust_butler.attack.volley_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_ATTACK_ORB_RELEASE =
            register("entity.mirror_dust_butler.attack.orb_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_ATTACK_ORB_HIT =
            register("entity.mirror_dust_butler.attack.orb_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_ATTACK_TOLL_WARN =
            register("entity.broken_bell_keeper.attack.toll_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_ATTACK_GEAR_HIT =
            register("entity.broken_bell_keeper.attack.gear_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_SLAM_WARN =
            register("entity.inverted_gatekeeper.attack.slam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_THROW_WARN =
            register("entity.inverted_gatekeeper.attack.throw_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_PULSE_WARN =
            register("entity.inverted_gatekeeper.attack.pulse_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_THROW_RELEASE =
            register("entity.inverted_gatekeeper.attack.throw_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ATTACK_PULSE_RELEASE =
            register("entity.inverted_gatekeeper.attack.pulse_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_REWRITE_WARN =
            register("entity.ash_scribe.attack.rewrite_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_INKCUT_WARN =
            register("entity.ash_scribe.attack.inkcut_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_PAGESTORM_WARN =
            register("entity.ash_scribe.attack.pagestorm_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_BURN_WARN =
            register("entity.ash_scribe.attack.burn_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_BURN_RELEASE =
            register("entity.ash_scribe.attack.burn_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_INKCUT_RELEASE =
            register("entity.ash_scribe.attack.inkcut_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SCRIBE_ATTACK_PAGES_RELEASE =
            register("entity.ash_scribe.attack.pages_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_HOP_WARN =
            register("entity.void_prophet.attack.hop_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_HOP_RELEASE =
            register("entity.void_prophet.attack.hop_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_SCAR_HIT =
            register("entity.void_prophet.attack.scar_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_REND_RELEASE =
            register("entity.void_prophet.attack.rend_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_RECOIL_WARN =
            register("entity.void_prophet.attack.recoil_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_RECOIL_PULL =
            register("entity.void_prophet.attack.recoil_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_RECOIL_RELEASE =
            register("entity.void_prophet.attack.recoil_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_CONSTELLATION_HIT =
            register("entity.void_prophet.attack.constellation_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_LANCE_WARN =
            register("entity.void_prophet.attack.lance_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_LANCE_RELEASE =
            register("entity.void_prophet.attack.lance_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_CORE_WARN =
            register("entity.void_prophet.attack.core_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_ATTACK_CORE_RELEASE =
            register("entity.void_prophet.attack.core_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_SLASH_WARN =
            register("entity.rift_warden.attack.slash_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_SLASH_RELEASE =
            register("entity.rift_warden.attack.slash_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_SLAM_WARN =
            register("entity.rift_warden.attack.slam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_SLAM_RELEASE =
            register("entity.rift_warden.attack.slam_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_PULL_WARN =
            register("entity.rift_warden.attack.pull_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_BREAK_WARN =
            register("entity.rift_warden.attack.break_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_BREAK_RELEASE =
            register("entity.rift_warden.attack.break_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_CHARGE_WARN =
            register("entity.rift_warden.attack.charge_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_CHARGE_RELEASE =
            register("entity.rift_warden.attack.charge_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_CHARGE_SLASH =
            register("entity.rift_warden.attack.charge_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_STORM_WARN =
            register("entity.rift_warden.attack.storm_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_ATTACK_STORM_RELEASE =
            register("entity.rift_warden.attack.storm_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_SKYFALL_HIT =
            register("entity.world_echo.attack.skyfall_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_WINGS_WARN =
            register("entity.world_echo.attack.wings_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_WINGS_RELEASE =
            register("entity.world_echo.attack.wings_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_AERIAL_WARN =
            register("entity.world_echo.attack.aerial_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_AERIAL_RELEASE =
            register("entity.world_echo.attack.aerial_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_REAP_WARN =
            register("entity.world_echo.attack.reap_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_REAP_DASH =
            register("entity.world_echo.attack.reap_dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_REAP_HIT =
            register("entity.world_echo.attack.reap_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_BEAM_WARN =
            register("entity.world_echo.attack.beam_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_BEAM_RELEASE =
            register("entity.world_echo.attack.beam_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_FLAME_RELEASE =
            register("entity.world_echo.attack.flame_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_METEOR_WARN =
            register("entity.world_echo.attack.meteor_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_METEOR_HIT =
            register("entity.world_echo.attack.meteor_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_NOVA_RELEASE =
            register("entity.world_echo.attack.nova_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_LANCE_RELEASE =
            register("entity.world_echo.attack.lance_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_SLAM_HIT =
            register("entity.world_echo.attack.slam_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_BARRAGE_RELEASE =
            register("entity.world_echo.attack.barrage_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_WIND_RELEASE =
            register("entity.world_echo.attack.wind_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_TIDE_RELEASE =
            register("entity.world_echo.attack.tide_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_SIGHT_RELEASE =
            register("entity.world_echo.attack.sight_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_GRAVITY_RELEASE =
            register("entity.world_echo.attack.gravity_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_VOID_RELEASE =
            register("entity.world_echo.attack.void_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_MELEE =
            register("entity.world_echo.attack.melee");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_WIND_WARN =
            register("entity.world_echo.attack.wind_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_TIDE_WARN =
            register("entity.world_echo.attack.tide_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_SIGHT_WARN =
            register("entity.world_echo.attack.sight_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_GRAVITY_WARN =
            register("entity.world_echo.attack.gravity_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_ATTACK_VOID_WARN =
            register("entity.world_echo.attack.void_warn");

    public static final DeferredHolder<SoundEvent, SoundEvent> FEEDBACK_TOKEN_GRANTED =
            register("feedback.token_granted");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEDBACK_PAGES_COMPLETE =
            register("feedback.pages_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEDBACK_JOURNEY_REWARD =
            register("feedback.journey_reward");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_CREDENTIAL_ISSUE =
            register("artifact.credential.issue");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARTIFACT_STATION_USE =
            register("artifact.station.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_REWARD_ALTAR_ABSORB =
            register("block.reward_altar.absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ECHO_PROJECTOR_ACTIVATE =
            register("item.echo_projector.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_FIELD_TRICK_ACTIVATE =
            register("item.field_trick.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_STORM_COMPASS_LOCATE =
            register("item.storm_compass.locate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_WORLDTREE_FRUIT_CONSUME =
            register("item.worldtree_fruit.consume");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_PILGRIMAGE_BELL_RING =
            register("item.pilgrimage_bell.ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_FIELD_LENS_CONFIRM =
            register("item.field_lens.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_FIELD_LENS_INSPECT =
            register("item.field_lens.inspect");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_FEATHER_BIRD_SONG =
            register("entity.crystal_feather_bird.song");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUSHLIGHT_JELLY_PULSE =
            register("entity.hushlight_jelly.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_PROJECTION_FADE =
            register("entity.echo_projection.fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEEP_PLANT_HARVEST =
            register("block.deep_plant.harvest");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHIME_CORE_CONFIRM =
            register("block.chime_core.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_FOLDPAGE_CORE_CONFIRM =
            register("block.foldpage_core.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PROJECTION_PLATE_CONFIRM =
            register("block.projection_plate.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MIRROR_SIGIL_CONFIRM =
            register("block.mirror_sigil.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_TRUE_SIGHT_STELE_CONFIRM =
            register("block.true_sight_stele.confirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> INVERTED_GATEKEEPER_ORBIT =
            register("entity.inverted_gatekeeper.orbit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_VEILED =
            register("entity.mirror_guardian.veiled");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEDBACK_DENIED =
            register("feedback.denied");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_REVEALING_POWDER_USE =
            register("item.revealing_powder.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_MEMORY_ECHO_STONE_USE =
            register("item.memory_echo_stone.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_ECHO_BANDAGE_USE =
            register("item.echo_bandage.use");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHIME_CORE_START =
            register("block.chime_core.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHIME_CORE_NOTE =
            register("block.chime_core.note");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PUZZLE_INPUT =
            register("block.puzzle.input");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PUZZLE_REJECT =
            register("block.puzzle.reject");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_LIGHTS_OUT_INPUT =
            register("block.lights_out.input");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_FOLDPAGE_CORE_MISMATCH =
            register("block.foldpage_core.mismatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WONDER_CORE_INPUT =
            register("block.wonder_core.input");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_WONDER_CORE_COMPLETE =
            register("block.wonder_core.complete");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(UnknownEchoes.id(name)));
    }
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_DRIFT_JELLY_AMBIENT =
            register("entity.wind_drift_jelly.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_DRIFT_JELLY_HURT =
            register("entity.wind_drift_jelly.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_DRIFT_JELLY_DEATH =
            register("entity.wind_drift_jelly.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEDWING_DRAGONFLY_AMBIENT =
            register("entity.reedwing_dragonfly.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEDWING_DRAGONFLY_HURT =
            register("entity.reedwing_dragonfly.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEDWING_DRAGONFLY_DEATH =
            register("entity.reedwing_dragonfly.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> GALE_SHRIKE_AMBIENT =
            register("entity.gale_shrike.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> GALE_SHRIKE_HURT =
            register("entity.gale_shrike.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> GALE_SHRIKE_DEATH =
            register("entity.gale_shrike.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> GALE_SHRIKE_SWOOP =
            register("entity.gale_shrike.swoop");

    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LANTERN_BAT_AMBIENT =
            register("entity.ash_lantern_bat.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LANTERN_BAT_HURT =
            register("entity.ash_lantern_bat.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LANTERN_BAT_DEATH =
            register("entity.ash_lantern_bat.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LANTERN_BAT_SWOOP =
            register("entity.ash_lantern_bat.swoop");

    public static final DeferredHolder<SoundEvent, SoundEvent> PETALWING_BUTTERFLY_AMBIENT = register("entity.petalwing_butterfly.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PETALWING_BUTTERFLY_HURT = register("entity.petalwing_butterfly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PETALWING_BUTTERFLY_DEATH = register("entity.petalwing_butterfly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSLIGHT_FIREFLY_AMBIENT = register("entity.mosslight_firefly.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSLIGHT_FIREFLY_HURT = register("entity.mosslight_firefly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSLIGHT_FIREFLY_DEATH = register("entity.mosslight_firefly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_NEEDLE_WASP_AMBIENT = register("entity.bell_needle_wasp.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_NEEDLE_WASP_HURT = register("entity.bell_needle_wasp.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_NEEDLE_WASP_DEATH = register("entity.bell_needle_wasp.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_NEEDLE_WASP_SWOOP = register("entity.bell_needle_wasp.swoop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARDWING_SKATE_AMBIENT = register("entity.shardwing_skate.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARDWING_SKATE_HURT = register("entity.shardwing_skate.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARDWING_SKATE_DEATH = register("entity.shardwing_skate.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARDWING_SKATE_CHARGE = register("entity.shardwing_skate.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARDWING_SKATE_BEAM = register("entity.shardwing_skate.beam");

    public static final DeferredHolder<SoundEvent, SoundEvent> SAILTAIL_MINNOW_AMBIENT = register("entity.sailtail_minnow.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAILTAIL_MINNOW_HURT = register("entity.sailtail_minnow.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAILTAIL_MINNOW_DEATH = register("entity.sailtail_minnow.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_SAIL_SNAIL_AMBIENT = register("entity.moss_sail_snail.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_SAIL_SNAIL_HURT = register("entity.moss_sail_snail.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSS_SAIL_SNAIL_DEATH = register("entity.moss_sail_snail.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SPINE_URCHIN_AMBIENT = register("entity.crystal_spine_urchin.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SPINE_URCHIN_HURT = register("entity.crystal_spine_urchin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SPINE_URCHIN_DEATH = register("entity.crystal_spine_urchin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SPINE_URCHIN_CHARGE = register("entity.crystal_spine_urchin.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SPINE_URCHIN_PULSE = register("entity.crystal_spine_urchin.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SAIL_SERPENT_AMBIENT = register("entity.ash_sail_serpent.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SAIL_SERPENT_HURT = register("entity.ash_sail_serpent.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SAIL_SERPENT_DEATH = register("entity.ash_sail_serpent.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SAIL_SERPENT_SWOOP = register("entity.ash_sail_serpent.swoop");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSBACK_HEDGEHOG_AMBIENT = register("entity.mossback_hedgehog.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSBACK_HEDGEHOG_HURT = register("entity.mossback_hedgehog.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSBACK_HEDGEHOG_DEATH = register("entity.mossback_hedgehog.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> EMBERCLAW_SCORPION_AMBIENT = register("entity.emberclaw_scorpion.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> EMBERCLAW_SCORPION_HURT = register("entity.emberclaw_scorpion.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> EMBERCLAW_SCORPION_DEATH = register("entity.emberclaw_scorpion.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERN_SEAHORSE_AMBIENT = register("entity.lantern_seahorse.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERN_SEAHORSE_HURT = register("entity.lantern_seahorse.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERN_SEAHORSE_DEATH = register("entity.lantern_seahorse.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERN_SEAHORSE_FLOP = register("entity.lantern_seahorse.flop");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEFJAW_SHARK_AMBIENT = register("entity.reefjaw_shark.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEFJAW_SHARK_HURT = register("entity.reefjaw_shark.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> REEFJAW_SHARK_DEATH = register("entity.reefjaw_shark.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SLATEHORN_BOAR_AMBIENT = register("entity.slatehorn_boar.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SLATEHORN_BOAR_HURT = register("entity.slatehorn_boar.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SLATEHORN_BOAR_DEATH = register("entity.slatehorn_boar.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOLLOW_REED_SERPENT_AMBIENT = register("entity.hollow_reed_serpent.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOLLOW_REED_SERPENT_HURT = register("entity.hollow_reed_serpent.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOLLOW_REED_SERPENT_DEATH = register("entity.hollow_reed_serpent.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFTFANG_EEL_AMBIENT = register("entity.riftfang_eel.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFTFANG_EEL_HURT = register("entity.riftfang_eel.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFTFANG_EEL_DEATH = register("entity.riftfang_eel.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> NEEDLEFIN_LIONFISH_AMBIENT = register("entity.needlefin_lionfish.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> NEEDLEFIN_LIONFISH_HURT = register("entity.needlefin_lionfish.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> NEEDLEFIN_LIONFISH_DEATH = register("entity.needlefin_lionfish.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSFANG_TOAD_AMBIENT = register("entity.mossfang_toad.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSFANG_TOAD_HURT = register("entity.mossfang_toad.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSFANG_TOAD_DEATH = register("entity.mossfang_toad.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBERBACK_BEETLE_AMBIENT = register("entity.amberback_beetle.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBERBACK_BEETLE_HURT = register("entity.amberback_beetle.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBERBACK_BEETLE_DEATH = register("entity.amberback_beetle.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERNJAW_ANGLER_AMBIENT = register("entity.lanternjaw_angler.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERNJAW_ANGLER_HURT = register("entity.lanternjaw_angler.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERNJAW_ANGLER_DEATH = register("entity.lanternjaw_angler.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SAWTOOTH_PRAWN_AMBIENT = register("entity.sawtooth_prawn.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SAWTOOTH_PRAWN_HURT = register("entity.sawtooth_prawn.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SAWTOOTH_PRAWN_DEATH = register("entity.sawtooth_prawn.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> REED_MASK_RAIDER_AMBIENT = register("entity.reed_mask_raider.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> REED_MASK_RAIDER_HURT = register("entity.reed_mask_raider.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> REED_MASK_RAIDER_DEATH = register("entity.reed_mask_raider.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_AXEMAN_AMBIENT = register("entity.broken_bell_axeman.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_AXEMAN_HURT = register("entity.broken_bell_axeman.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_AXEMAN_DEATH = register("entity.broken_bell_axeman.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PAGE_STALKER_AMBIENT = register("entity.rift_page_stalker.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PAGE_STALKER_HURT = register("entity.rift_page_stalker.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PAGE_STALKER_DEATH = register("entity.rift_page_stalker.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> REED_MASK_RAIDER_PREPARE = register("entity.reed_mask_raider.attack.prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_AXEMAN_PREPARE = register("entity.broken_bell_axeman.attack.prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PAGE_STALKER_PREPARE = register("entity.rift_page_stalker.attack.prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_LANTERN_BAT_STRIKE = register("entity.ash_lantern_bat.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> GALE_SHRIKE_STRIKE = register("entity.gale_shrike.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASH_SAIL_SERPENT_STRIKE = register("entity.ash_sail_serpent.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL_NEEDLE_WASP_STRIKE = register("entity.bell_needle_wasp.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> REED_MASK_RAIDER_STRIKE = register("entity.reed_mask_raider.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_AXEMAN_STRIKE = register("entity.broken_bell_axeman.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PAGE_STALKER_STRIKE = register("entity.rift_page_stalker.attack.strike");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSPLATE_HOUND_AMBIENT = register("entity.mossplate_hound.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSPLATE_HOUND_HURT = register("entity.mossplate_hound.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSPLATE_HOUND_DEATH = register("entity.mossplate_hound.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> CAIRN_SHELL_BRUTE_AMBIENT = register("entity.cairn_shell_brute.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> CAIRN_SHELL_BRUTE_HURT = register("entity.cairn_shell_brute.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> CAIRN_SHELL_BRUTE_DEATH = register("entity.cairn_shell_brute.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_CROWNED_WRETCH_AMBIENT = register("entity.spore_crowned_wretch.ambient");

    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_CROWNED_WRETCH_HURT = register("entity.spore_crowned_wretch.hurt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_CROWNED_WRETCH_DEATH = register("entity.spore_crowned_wretch.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBERBACK_BEETLE_STRIKE = register("entity.amberback_beetle.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMBERCLAW_SCORPION_STRIKE = register("entity.emberclaw_scorpion.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLLOW_REED_SERPENT_STRIKE = register("entity.hollow_reed_serpent.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERNJAW_ANGLER_STRIKE = register("entity.lanternjaw_angler.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOSSFANG_TOAD_STRIKE = register("entity.mossfang_toad.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> NEEDLEFIN_LIONFISH_STRIKE = register("entity.needlefin_lionfish.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> REEFJAW_SHARK_STRIKE = register("entity.reefjaw_shark.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFTFANG_EEL_STRIKE = register("entity.riftfang_eel.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAWTOOTH_PRAWN_STRIKE = register("entity.sawtooth_prawn.attack.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLATEHORN_BOAR_STRIKE = register("entity.slatehorn_boar.attack.strike");

}
