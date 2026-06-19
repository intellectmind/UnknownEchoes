package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义音效事件。实际音频在 sounds.json 中引用原版音效事件做变调/混合,
 * V0.1 不引入自定义 ogg 文件。
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, UnknownEchoes.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_AMBIENT =
            register("entity.echo_wanderer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_HURT =
            register("entity.echo_wanderer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_WANDERER_DEATH =
            register("entity.echo_wanderer.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_AMBIENT =
            register("entity.forgotten_colossus.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_HURT =
            register("entity.forgotten_colossus.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_DEATH =
            register("entity.forgotten_colossus.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_STEP =
            register("entity.forgotten_colossus.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_ATTACK =
            register("entity.forgotten_colossus.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> COLOSSUS_MUSIC =
            register("music.forgotten_colossus");

    public static final DeferredHolder<SoundEvent, SoundEvent> ABYSS_WATCHER_MUSIC =
            register("music.abyss_watcher");

    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_GUARDIAN_MUSIC =
            register("music.mirror_guardian");

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

    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_WALKER_HURT =
            register("entity.silent_walker.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_WALKER_DEATH =
            register("entity.silent_walker.death");

    /** 风之回响滑翔:开始时较响一声,滑行期间低音量持续触发(变体随机)。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND_GLIDE =
            register("ability.wind_echo.glide");

    // ---- V0.6A Mini Boss(音乐事件独立注册,sounds.json 映射共享 Boss 音轨,便于资源包按 Boss 覆盖) ----

    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_MUSIC =
            register("music.storm_weaver");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_AMBIENT =
            register("entity.storm_weaver.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_HURT =
            register("entity.storm_weaver.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_DEATH =
            register("entity.storm_weaver.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_WEAVER_SHATTER =
            register("entity.storm_weaver.shatter");

    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_MUSIC =
            register("music.silent_priest");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_AMBIENT =
            register("entity.silent_priest.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_HURT =
            register("entity.silent_priest.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_DEATH =
            register("entity.silent_priest.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENT_PRIEST_WAVE =
            register("entity.silent_priest.wave");

    // ---- V0.6C 镜湖 Mini Boss ----

    /** 音乐事件 music.tide_lantern_keeper:暂映射共享 Boss 音轨(10.4.2,后续可换专属变体)。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_MUSIC =
            register("music.tide_lantern_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_AMBIENT =
            register("entity.tide_lantern_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_HURT =
            register("entity.tide_lantern_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_DEATH =
            register("entity.tide_lantern_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_BEAM =
            register("entity.tide_lantern_keeper.beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_DASH =
            register("entity.tide_lantern_keeper.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_LANTERN_KEEPER_SHATTER =
            register("entity.tide_lantern_keeper.shatter");

    /** 音乐事件 music.mirror_dust_butler:暂映射镜像守护者主题的稀疏变体(10.4.2)。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_MUSIC =
            register("music.mirror_dust_butler");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_AMBIENT =
            register("entity.mirror_dust_butler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_HURT =
            register("entity.mirror_dust_butler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_DEATH =
            register("entity.mirror_dust_butler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_ATTACK =
            register("entity.mirror_dust_butler.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_DUST_BUTLER_SHATTER =
            register("entity.mirror_dust_butler.shatter");

    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_MUSIC =
            register("music.crystal_songkeeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_AMBIENT =
            register("entity.crystal_songkeeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_HURT =
            register("entity.crystal_songkeeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_DEATH =
            register("entity.crystal_songkeeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_SONGKEEPER_ATTACK =
            register("entity.crystal_songkeeper.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_MUSIC =
            register("music.broken_bell_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_AMBIENT =
            register("entity.broken_bell_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_HURT =
            register("entity.broken_bell_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_DEATH =
            register("entity.broken_bell_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROKEN_BELL_KEEPER_ATTACK =
            register("entity.broken_bell_keeper.attack");

    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_MUSIC =
            register("music.dream_bloom_keeper");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_AMBIENT =
            register("entity.dream_bloom_keeper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_HURT =
            register("entity.dream_bloom_keeper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_DEATH =
            register("entity.dream_bloom_keeper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DREAM_BLOOM_KEEPER_BLOOM =
            register("entity.dream_bloom_keeper.bloom");

    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_MUSIC =
            register("music.lost_recorder_chief");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_AMBIENT =
            register("entity.lost_recorder_chief.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_HURT =
            register("entity.lost_recorder_chief.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_DEATH =
            register("entity.lost_recorder_chief.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOST_RECORDER_CHIEF_REVIEW =
            register("entity.lost_recorder_chief.review");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(UnknownEchoes.id(name)));
    }
}
