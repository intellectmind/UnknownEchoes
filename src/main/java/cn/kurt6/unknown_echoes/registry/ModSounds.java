package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 仓库内没有 .ogg：音频在 sounds.json 里引用原版事件做变调。
 * sounds 数组是加权随机变体池,每次发声只播一条,不是叠加混音。
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, UnknownEchoes.MODID);

    // ── 能力表现 ──
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_GLIDE =
            register("ability.wind_echo.glide");

    // ── Boss 战音轨（注册但无代码播放,预留给资源包覆盖）──
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_MUSIC =
            register("music.forgotten_colossus");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_MUSIC =
            register("music.abyss_watcher");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_MUSIC =
            register("music.mirror_guardian");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_MUSIC =
            register("music.storm_weaver");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_MUSIC =
            register("music.silent_priest");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_MUSIC =
            register("music.tide_lantern_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_MUSIC =
            register("music.mirror_dust_butler");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_MUSIC =
            register("music.crystal_songkeeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_MUSIC =
            register("music.broken_bell_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_MUSIC =
            register("music.dream_bloom_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_MUSIC =
            register("music.lost_recorder_chief");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_PROPHET_MUSIC =
            register("music.void_prophet");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_WARDEN_MUSIC =
            register("music.rift_warden");
    public static final DeferredHolder<SoundEvent, SoundEvent> WORLD_ECHO_MUSIC =
            register("music.world_echo");

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
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_WATER_SHADE_HURT =
            register("entity.tide_water_shade.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_WATER_SHADE_DEATH =
            register("entity.tide_water_shade.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_AMBIENT =
            register("entity.mirror_dust_decoy.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_HURT =
            register("entity.mirror_dust_decoy.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_DECOY_DEATH =
            register("entity.mirror_dust_decoy.death");

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

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(UnknownEchoes.id(name)));
    }
}
