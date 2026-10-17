package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = "unknown_echoes")
public final class FieldTrickLure {
    private static final String KEY = "UnknownEchoesFieldLure";
    private static final int DURATION_TICKS = 160;
    private static final double RADIUS = 8.0D;
    private static final int MAX_TARGETS = 6;
    private static final int REPATH_RETRY_TICKS = 40;

    private FieldTrickLure() { }

    static boolean eligible(ServerPlayer player, Mob mob) {
        return (mob instanceof Enemy || mob.getType().is(ModEntityTags.ECHO_BOSSES)
                || mob.getType().is(Tags.EntityTypes.BOSSES))
                && mob.level() == player.level() && mob.isAlive() && !mob.isSpectator() && !mob.isNoAi()
                && !mob.isAlliedTo(player) && !player.isAlliedTo(mob);
    }

    static boolean apply(ServerLevel level, ServerPlayer player, Vec3 center) {
        if (!validOwner(level, player, center)) return false;
        List<Mob> targets = level.getEntitiesOfClass(Mob.class,
                new AABB(center, center).inflate(RADIUS),
                mob -> eligible(player, mob) && mob.distanceToSqr(center) <= RADIUS * RADIUS
                        && !active(mob, level)).stream()
                .sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(center))).toList();
        int applied = 0;
        for (Mob mob : targets) {
            if (!validOwner(level, player, center) || applied >= MAX_TARGETS) break;
            if (!eligible(player, mob) || mob.distanceToSqr(center) > RADIUS * RADIUS || active(mob, level)) continue;
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Owner", player.getUUID());
            tag.putString("Dimension", level.dimension().location().toString());
            tag.putLong("Until", level.getGameTime() + DURATION_TICKS);
            tag.putDouble("X", center.x);
            tag.putDouble("Y", center.y);
            tag.putDouble("Z", center.z);
            mob.getPersistentData().put(KEY, tag);
            applied++;
            guide(mob, level, tag);
        }
        return applied > 0;
    }

    private static boolean validOwner(ServerLevel level, ServerPlayer player, Vec3 center) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && player.distanceToSqr(center) <= 24 * 24;
    }

    private static boolean active(Mob mob, ServerLevel level) {
        if (!mob.getPersistentData().contains(KEY)) return false;
        CompoundTag tag = mob.getPersistentData().getCompound(KEY);
        ServerPlayer owner = tag.hasUUID("Owner") ? level.getServer().getPlayerList().getPlayer(tag.getUUID("Owner")) : null;
        Vec3 center = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        long remaining = tag.getLong("Until") - level.getGameTime();
        return remaining > 0 && remaining <= DURATION_TICKS && owner != null
                && validOwner(level, owner, center) && eligible(owner, mob)
                && tag.getString("Dimension").equals(level.dimension().location().toString())
                && mob.distanceToSqr(center) <= 16 * 16
                && level.hasChunkAt(BlockPos.containing(center));
    }

    private static boolean current(Mob mob, CompoundTag tag) {
        return mob.getPersistentData().getCompound(KEY) == tag;
    }

    private static void clear(Mob mob, CompoundTag tag) {
        if (!current(mob, tag)) return;
        mob.getPersistentData().remove(KEY);
        BlockPos target = BlockPos.containing(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
        var navigation = mob.getNavigation();
        if (target.equals(navigation.getTargetPos())) navigation.stop();
    }

    private static void guide(Mob mob, ServerLevel level, CompoundTag tag) {
        mob.setTarget(null);
        if (!current(mob, tag)) return;
        if (!active(mob, level)) {
            clear(mob, tag);
            return;
        }
        Vec3 center = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        BlockPos target = BlockPos.containing(center);
        var navigation = mob.getNavigation();
        if (navigation.isInProgress() && target.equals(navigation.getTargetPos())) {
            // 重提相同路径会重置原生卡住检测，执行中只维护速度。
            navigation.setSpeedModifier(1.1D);
            tag.remove("RetryAt");
            return;
        }
        long now = level.getGameTime();
        long nextRetry = now > Long.MAX_VALUE - REPATH_RETRY_TICKS ? Long.MAX_VALUE : now + REPATH_RETRY_TICKS;
        long retryAt = tag.getLong("RetryAt");
        if (retryAt > now && retryAt <= nextRetry) return;
        tag.remove("RetryAt");
        boolean moved = navigation.moveTo(center.x, center.y, center.z, 1.1D);
        if (!current(mob, tag)) return;
        if (!active(mob, level)) {
            clear(mob, tag);
        } else if (!moved) {
            tag.putLong("RetryAt", nextRetry);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || mob.tickCount % 10 != 0 || !mob.getPersistentData().contains(KEY)) return;
        CompoundTag tag = mob.getPersistentData().getCompound(KEY);
        if (!active(mob, level)) {
            clear(mob, tag);
            return;
        }
        guide(mob, level, tag);
        if (!current(mob, tag) || !active(mob, level)) return;
        level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getEyeY() + 0.3D, mob.getZ(),
                1, 0, 0, 0, 0);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level
                && event.getNewAboutToBeSetTarget() != null && active(mob, level)) event.setCanceled(true);
    }

}
