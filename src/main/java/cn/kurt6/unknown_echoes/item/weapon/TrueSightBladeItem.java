package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class TrueSightBladeItem extends SwordItem implements WeaponSkill {

    private static final int REVEAL_COOLDOWN_TICKS = 200;
    private static final double REVEAL_SEARCH_RADIUS = 48.0;

    /** 镜痕连斩：叠层窗口与爆发伤害由服务端维护。 */
    private static final String MARK_COUNT_KEY = "ue_mirror_mark_count";
    private static final String MARK_TIME_KEY = "ue_mirror_mark_time";
    private static final int MAX_ACTIVE_MIRROR_MARKS = 64;
    private static final int PRIMARY_COOLDOWN_TICKS = 100;
    private static final int SECONDARY_COOLDOWN_TICKS = 280;
    private final int markWindowTicks;
    private final float shatterDamage;

    public TrueSightBladeItem(double damage, double speed, int markWindowTicks,
                              float shatterDamage, Properties properties) {
        super(EchoMetalTier.INSTANCE, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.markWindowTicks = markWindowTicks;
        this.shatterDamage = shatterDamage;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return cn.kurt6.unknown_echoes.particle.ParticleRules.data(stack).total() == 50 || super.isFoil(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ILLUSION_PRISM.get())
                || repairCandidate.is(ModItems.BROKEN_REFLECTION.get())
                || repairCandidate.is(ModItems.MIRROR_GUARDIAN_CORE.get())
                || repairCandidate.is(ModItems.TRUE_SIGHT_CORE.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        castPrimary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        castSecondary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private void castPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.TRUE_SIGHT_BLADE;
        SkillAim aim = skillAim(player, 9.0D, 1.0D);
        Predicate<LivingEntity> targetCheck = target -> isCurrentLineTarget(level, player, target, aim, 9.0D, 1.0D);
        for (LivingEntity target : aimedSkillTargets(level, aim, targetCheck, 5)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!targetCheck.test(target)) {
                continue;
            }
            float damage = target.getType().is(ModEntityTags.ILLUSION_MOBS) ? 20.0F : 14.0F;
            boolean hurt = WeaponSkillSupport.hurt(player, target, damage);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !targetCheck.test(target)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!targetCheck.test(target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.pierceBeam(level, player, fx, aim.origin(), aim.direction(), 9.0D);
        if (isAtSkillOrigin(level, player, aim)) {
            Vec3 direction = aim.direction();
            player.setDeltaMovement(direction.x * 1.15D,
                    Math.max(0.05D, direction.y * 0.6D + 0.05D), direction.z * 1.15D);
            if (!isAtSkillOrigin(level, player, aim)) {
                return;
            }
            player.hurtMarked = true;
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(aim.feet()), ModSounds.WEAPON_TRUE_SIGHT_BLADE_PRIMARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void castSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.TRUE_SIGHT_BLADE;
        double radius = 10.0D;
        Vec3 center = player.position();
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : radiusSkillTargets(level, player, center, bounds, radius, 8)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            float damage = target.getType().is(ModEntityTags.ILLUSION_MOBS) ? 18.0F : 11.0F;
            boolean hurt = WeaponSkillSupport.hurt(player, target, damage);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, true, false, true));
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_TRUE_SIGHT_BLADE_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private record SkillAim(Vec3 feet, Vec3 origin, Vec3 direction, AABB bounds) {
    }

    private static boolean isCurrentSkillActor(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    private static SkillAim skillAim(ServerPlayer player, double range, double padding) {
        Vec3 feet = player.position();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        return new SkillAim(feet, origin, direction,
                new AABB(origin, origin.add(direction.scale(range))).inflate(padding));
    }

    private static List<LivingEntity> aimedSkillTargets(ServerLevel level, SkillAim aim,
                                                        Predicate<LivingEntity> targetCheck, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class, aim.bounds(), targetCheck)
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(aim.feet())))
                .limit(limit)
                .toList();
    }

    private static boolean isCurrentAimTarget(ServerLevel level, ServerPlayer player, LivingEntity target, SkillAim aim) {
        return isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.getBoundingBox().intersects(aim.bounds());
    }

    private static boolean isCurrentLineTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                SkillAim aim, double range, double width) {
        if (!isCurrentAimTarget(level, player, target, aim)) {
            return false;
        }
        Vec3 offset = target.getBoundingBox().getCenter().subtract(aim.origin());
        double along = offset.dot(aim.direction());
        double targetRadius = target.getBbWidth() * 0.5D + width;
        return along >= 0.25D && along <= range
                && offset.subtract(aim.direction().scale(along)).lengthSqr() <= targetRadius * targetRadius
                && hasSightFrom(level, player, target, aim.origin());
    }

    private static boolean hasSightFrom(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 origin) {
        Vec3 end = target.getEyePosition();
        AABB box = target.getBoundingBox();
        if (end.distanceTo(origin) > 128.0D) {
            return false;
        }
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS && isCurrentSkillActor(level, player)
                && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && end.equals(target.getEyePosition()) && box.equals(target.getBoundingBox());
    }

    private static List<LivingEntity> radiusSkillTargets(ServerLevel level, ServerPlayer player,
                                                         Vec3 center, AABB bounds, double radius, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        target -> isCurrentRadiusTarget(level, player, target, center, bounds, radius))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)))
                .limit(limit)
                .toList();
    }

    private static boolean isCurrentRadiusTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                   Vec3 center, AABB bounds, double radius) {
        return isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.position().distanceToSqr(center) <= radius * radius
                && target.getBoundingBox().intersects(bounds);
    }

    private static boolean isAtSkillOrigin(ServerLevel level, ServerPlayer player, SkillAim aim) {
        return isCurrentSkillActor(level, player) && player.position().distanceToSqr(aim.feet()) <= 1.0E-6D;
    }

    private static boolean isCurrentPassiveHit(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return isCurrentSkillActor(level, player) && target.level() == level && !target.isRemoved() &&
                target instanceof Enemy && !target.isSpectator() && !target.isAlliedTo(player);
    }

    private static boolean isCurrentPassiveTarget(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return isCurrentPassiveHit(level, player, target) && target.isAlive();
    }

    private static String skillKey(String slot) {
        return "ue_weapon_skill_true_sight_blade_" + slot + "_until";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.passive")
                .withStyle(ChatFormatting.GRAY));
    }

    /** Incoming 阶段只预览镜痕破碎伤害，不提前提交层数或冷却。 */
    public float getDamageEventBonus(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isCurrentPassiveTarget(level, player, target)) {
            return 0.0F;
        }
        if (nextMirrorMarkCount(level, player, target) < 3) {
            return 0.0F;
        }
        return target.getType().is(ModEntityTags.ILLUSION_MOBS)
                ? shatterDamage * 2.0F : shatterDamage;
    }

    public boolean wouldExposeTarget(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return isCurrentPassiveTarget(level, player, target) && nextMirrorMarkCount(level, player, target) >= 3;
    }

    /** Post 阶段确认 hurt 成功后，才提交镜痕和真身暴露。 */
    public void applyPostDamageEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isCurrentPassiveHit(level, player, target)) {
            return;
        }
        AABB revealBounds = target.getBoundingBox().inflate(REVEAL_SEARCH_RADIUS);
        Vec3 revealOrigin = player.getEyePosition();
        BlockPos soundPos = player.blockPosition();
        this.tickMirrorMark(level, player, target);
        if (!isCurrentPassiveHit(level, player, target) || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        LivingEntity real = findRealBody(level, player, target, revealBounds);
        if (!isCurrentRealBody(level, player, target, real, revealBounds)
                || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        player.getCooldowns().addCooldown(this, REVEAL_COOLDOWN_TICKS);
        if (!isCurrentRealBody(level, player, target, real, revealBounds)) {
            return;
        }
        int glowTicks = cn.kurt6.unknown_echoes.ability.EchoArmorSets.hasListenerClueBonus(player) ? 120 : 60;
        real.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0, false, false, false));
        if (!isCurrentRealBody(level, player, target, real, revealBounds)) {
            return;
        }
        spawnRevealTrail(level, revealOrigin, real);
        if (!isCurrentRealBody(level, player, target, real, revealBounds)) {
            return;
        }
        level.playSound(null, soundPos,
                ModSounds.WEAPON_TRUE_SIGHT_BLADE_SECONDARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 裂镜连斩:4 秒窗口内连续命中同一目标叠镜痕(目标身上浮现裂镜微光),
     * 第 3 层破碎——额外爆发伤害(幻象类双倍)+ 短暂发光 + 碎镜环。
     */
    private int nextMirrorMarkCount(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isCurrentPassiveHit(level, player, target)) {
            return 0;
        }
        var data = target.getPersistentData();
        String suffix = "_" + player.getUUID();
        long now = level.getGameTime();
        return nextMirrorMarkCount(data, suffix, now);
    }

    private void tickMirrorMark(ServerLevel level, ServerPlayer player, LivingEntity target) {
        int count = nextMirrorMarkCount(level, player, target);
        if (count <= 0) {
            return;
        }
        Vec3 markOrigin = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        BlockPos soundPos = target.blockPosition();
        var data = target.getPersistentData();
        String suffix = "_" + player.getUUID();
        String countKey = MARK_COUNT_KEY + suffix;
        String timeKey = MARK_TIME_KEY + suffix;
        long now = level.getGameTime();
        boolean refreshing = isValidMirrorMark(data, suffix, now);
        pruneMirrorMarks(data, now, suffix,
                refreshing ? MAX_ACTIVE_MIRROR_MARKS : MAX_ACTIVE_MIRROR_MARKS - 1);
        count = nextMirrorMarkCount(data, suffix, now);
        data.putLong(timeKey, now);
        if (count < 3) {
            data.putInt(countKey, count);
            if (!isCurrentPassiveTarget(level, player, target)) {
                return;
            }
            // 层数只能靠这里读出来:粒子数按 count 递增,玩家据此判断还差几刀到破碎。
            for (int i = 0; i < count; i++) {
                if (!isCurrentPassiveTarget(level, player, target)) {
                    return;
                }
                double angle = Math.PI * 2 * i / count;
                level.sendParticles(WeaponFxTheme.TRUE_SIGHT_BLADE.coreDust(1.3F),
                        markOrigin.x + Math.cos(angle) * 0.7,
                        markOrigin.y,
                        markOrigin.z + Math.sin(angle) * 0.7, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (!isCurrentPassiveTarget(level, player, target)) {
                return;
            }
            if (count == 1) {
                level.playSound(null, soundPos,
                    ModSounds.WEAPON_TRUE_SIGHT_BLADE_MARK_ONE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            } else {
                level.playSound(null, soundPos,
                    ModSounds.WEAPON_TRUE_SIGHT_BLADE_MARK_TWO.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return;
        }

        data.remove(countKey);
        data.remove(timeKey);
        if (!isCurrentPassiveTarget(level, player, target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        if (!isCurrentPassiveTarget(level, player, target)) {
            return;
        }
        WeaponFx.heavyHitMark(level, target, WeaponFxTheme.TRUE_SIGHT_BLADE);
        if (!isCurrentPassiveTarget(level, player, target)) {
            return;
        }
        level.playSound(null, soundPos,
                ModSounds.WEAPON_TRUE_SIGHT_BLADE_SHATTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private int nextMirrorMarkCount(CompoundTag data, String suffix, long now) {
        int count = Math.max(0, Math.min(2, data.getInt(MARK_COUNT_KEY + suffix)));
        return isValidMirrorMark(data, suffix, now) ? count + 1 : 1;
    }

    private boolean isValidMirrorMark(CompoundTag data, String suffix, long now) {
        String countKey = MARK_COUNT_KEY + suffix;
        String timeKey = MARK_TIME_KEY + suffix;
        if (!data.contains(countKey, Tag.TAG_INT)
                || data.getInt(countKey) < 1
                || data.getInt(countKey) > 2
                || !data.contains(timeKey, Tag.TAG_LONG)) {
            return false;
        }
        long markedAt = data.getLong(timeKey);
        return markedAt >= 0L && markedAt <= now && now - markedAt <= this.markWindowTicks;
    }

    private void pruneMirrorMarks(CompoundTag data, long now, String protectedSuffix,
                                  int maxCount) {
        List<MirrorMark> active = new ArrayList<>();
        List<String> staleSuffixes = new ArrayList<>();
        String timePrefix = MARK_TIME_KEY + "_";
        for (String key : List.copyOf(data.getAllKeys())) {
            if (!key.startsWith(timePrefix)) {
                continue;
            }
            String suffix = key.substring(MARK_TIME_KEY.length());
            if (isValidMirrorMark(data, suffix, now)) {
                active.add(new MirrorMark(suffix, data.getLong(key)));
            } else {
                staleSuffixes.add(suffix);
            }
        }
        for (String suffix : staleSuffixes) {
            removeMirrorMark(data, suffix);
        }

        String countPrefix = MARK_COUNT_KEY + "_";
        for (String key : List.copyOf(data.getAllKeys())) {
            if (key.startsWith(countPrefix)) {
                String suffix = key.substring(MARK_COUNT_KEY.length());
                if (!data.contains(MARK_TIME_KEY + suffix, Tag.TAG_LONG)) {
                    data.remove(key);
                }
            }
        }

        int excess = active.size() - maxCount;
        if (excess <= 0) {
            return;
        }
        active.sort(Comparator.comparingLong(MirrorMark::markedAt));
        for (MirrorMark mark : active) {
            if (excess <= 0) {
                break;
            }
            if (!mark.suffix().equals(protectedSuffix)) {
                removeMirrorMark(data, mark.suffix());
                excess--;
            }
        }
    }

    private static void removeMirrorMark(CompoundTag data, String suffix) {
        data.remove(MARK_COUNT_KEY + suffix);
        data.remove(MARK_TIME_KEY + suffix);
    }

    private record MirrorMark(String suffix, long markedAt) {
    }

    /** 命中的是分身/假身时,返回场上的真身;否则 null。 */
    private static LivingEntity findRealBody(ServerLevel level, ServerPlayer player, LivingEntity hit, AABB bounds) {
        if (hit instanceof MirrorGuardian guardian && guardian.isIllusion()) {
            for (MirrorGuardian real : level.getEntitiesOfClass(MirrorGuardian.class, bounds,
                    candidate -> isCurrentRealBody(level, player, hit, candidate, bounds))) {
                if (isCurrentRealBody(level, player, hit, real, bounds)) {
                    return real;
                }
            }
        }
        if (hit instanceof AbyssWatcher watcher && watcher.isClone()) {
            for (AbyssWatcher real : level.getEntitiesOfClass(AbyssWatcher.class, bounds,
                    candidate -> isCurrentRealBody(level, player, hit, candidate, bounds))) {
                if (isCurrentRealBody(level, player, hit, real, bounds)) {
                    return real;
                }
            }
        }
        return null;
    }

    private static boolean isCurrentRealBody(ServerLevel level, ServerPlayer player, LivingEntity hit,
                                              LivingEntity real, AABB bounds) {
        if (!isCurrentPassiveHit(level, player, hit) || real == null) {
            return false;
        }
        if (!(real.level() == level && real.isAlive() && !real.isSpectator() && !real.isAlliedTo(player)
                && real.getBoundingBox().intersects(bounds))) {
            return false;
        }
        return (hit instanceof MirrorGuardian guardian && real instanceof MirrorGuardian original
                    && !original.isIllusion() && guardian.isIllusionOf(original))
                || (hit instanceof AbyssWatcher watcher && real instanceof AbyssWatcher watcherReal
                    && !watcherReal.isClone() && watcher.isCloneOf(watcherReal));
    }

    /** 从玩家眼前向真身铺一道短粒子轨迹(方向提示,不给精确坐标 UI)。 */
    private static void spawnRevealTrail(ServerLevel level, Vec3 from, LivingEntity real) {
        Vec3 dir = real.position().add(0, real.getBbHeight() * 0.5, 0).subtract(from).normalize();
        // 7 格是方向提示的上限:铺到真身会把精确坐标交给客户端,谜题就白设了。
        WeaponFx.dashTrail(level, from, dir, 7.0D, WeaponFxTheme.TRUE_SIGHT_BLADE);
    }
}
