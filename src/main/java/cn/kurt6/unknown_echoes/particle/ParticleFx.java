package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ParticleFx {
    private static final Map<ServerPlayer, Long> LAST_HIT_SOUND = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> LAST_TARGET_SOUND = new WeakHashMap<>();
    private static final Map<ServerPlayer, Vec3> LAST_POSITION = new WeakHashMap<>();
    // 10 tick 覆盖最长命中尾音；玩家和目标分别限频，避免群攻与多人围攻叠音。
    private static final int HIT_SOUND_INTERVAL = 10;
    private ParticleFx() { }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleKind kind) {
        MechanismFx.emit(level, List.of(ParticleVisuals.cue(Shape.RING, center, Vec3.ZERO,
                Math.max(.05, Math.min(7, radius)), 0, 0, kind, 12)));
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleKind kind) {
        Vec3 delta = to.subtract(from);
        MechanismFx.emit(level, List.of(ParticleVisuals.cue(Shape.BEAM, from, delta.normalize(),
                .04, Math.min(14, delta.length()), 0, kind, 8)));
    }

    public static void hit(ServerPlayer player, LivingEntity target, ParticleData data) {
        MechanismFx.emit(player.serverLevel(), ParticleVisuals.hit(data,
                target.position().add(0, target.getBbHeight() * .5, 0), player.getLookAngle(), target.getBbWidth()));
        long now = player.server.overworld().getGameTime();
        if (withinHitInterval(LAST_HIT_SOUND.get(player), now)
                || withinHitInterval(LAST_TARGET_SOUND.get(target), now)) return;
        LAST_HIT_SOUND.put(player, now);
        LAST_TARGET_SOUND.put(target, now);
        ServerLevel level = player.serverLevel();
        BlockPos pos = target.blockPosition();
        switch (ParticleVisuals.dominant(data)) {
            case FIRE -> level.playSound(null, pos, ModSounds.PARTICLE_FIRE_HIT.get(), SoundSource.PLAYERS, .12F, 1.0F);
            case FROST -> level.playSound(null, pos, ModSounds.PARTICLE_FROST_HIT.get(), SoundSource.PLAYERS, .16F, 1.0F);
            case VENOM -> level.playSound(null, pos, ModSounds.PARTICLE_VENOM_HIT.get(), SoundSource.PLAYERS, .28F, 1.0F);
            case THUNDER -> level.playSound(null, pos, ModSounds.PARTICLE_THUNDER_HIT.get(), SoundSource.PLAYERS, .9F, 1.0F);
            case VOID -> level.playSound(null, pos, ModSounds.PARTICLE_VOID_HIT.get(), SoundSource.PLAYERS, .5F, 1.0F);
            case HOLY -> level.playSound(null, pos, ModSounds.PARTICLE_HOLY_HIT.get(), SoundSource.PLAYERS, .32F, 1.0F);
            case SHADOW -> level.playSound(null, pos, ModSounds.PARTICLE_SHADOW_HIT.get(), SoundSource.PLAYERS, .42F, 1.0F);
            case EARTH -> level.playSound(null, pos, ModSounds.PARTICLE_EARTH_HIT.get(), SoundSource.PLAYERS, .4F, 1.0F);
        }
    }

    private static boolean withinHitInterval(Long last, long now) {
        return last != null && now >= last && now - last < HIT_SOUND_INTERVAL;
    }

    public static void held(ServerPlayer player) {
        Vec3 previous = LAST_POSITION.put(player, player.position());
        double moved = previous == null ? 0 : previous.distanceToSqr(player.position());
        if (!player.isAlive() || player.isSpectator() || player.isInvisible() || player.tickCount % 240 != 0
                || moved < .0025 || moved > 16 || ParticleRules.data(player.getMainHandItem()).total() != 50) return;
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.PARTICLE_RESONANCE.get(), SoundSource.PLAYERS, .14F, 1.0F);
    }

    public static void operation(ServerPlayer player, BlockPos pos, ParticleData data, int action, boolean success) {
        ServerLevel level = player.serverLevel();
        MechanismFx.emit(level, ParticleVisuals.operation(Vec3.atCenterOf(pos).add(0, .33, 0), data, action, success));
        if (!success) player.playNotifySound(ModSounds.PARTICLE_REJECT.get(), SoundSource.BLOCKS, .08F, 1.0F);
        else if (action == 0) player.playNotifySound(ModSounds.PARTICLE_UPGRADE.get(), SoundSource.BLOCKS, .65F, 1.0F);
        else if (action >= 17) player.playNotifySound(ModSounds.PARTICLE_PURIFY.get(), SoundSource.BLOCKS, .35F, 1.0F);
        else player.playNotifySound(ModSounds.PARTICLE_INJECT.get(), SoundSource.BLOCKS, .5F, 1.0F);
    }

    public static void clear() { LAST_HIT_SOUND.clear(); LAST_TARGET_SOUND.clear(); LAST_POSITION.clear(); }
}
