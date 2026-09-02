package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        WeaponFxTheme.Theme fx = WeaponFxTheme.TRUE_SIGHT_BLADE;
        Vec3 direction = player.getLookAngle().normalize();
        for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player, 9.0D, 1.0D, 5)) {
            float damage = target.getType().is(ModEntityTags.ILLUSION_MOBS) ? 20.0F : 14.0F;
            if (WeaponSkillSupport.hurt(player, target, damage)) {
                WeaponFx.hitMark(level, target, fx);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
            }
        }
        WeaponFx.pierceBeam(level, player, fx, player.getEyePosition(), direction, 9.0D);
        player.setDeltaMovement(direction.x * 1.15D,
                Math.max(0.05D, direction.y * 0.6D + 0.05D), direction.z * 1.15D);
        player.hurtMarked = true;
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.9F, 1.45F);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 10.0D, 8)) {
            float damage = target.getType().is(ModEntityTags.ILLUSION_MOBS) ? 18.0F : 11.0F;
            if (!WeaponSkillSupport.hurt(player, target, damage)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.TRUE_SIGHT_BLADE);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, true, false, true));
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.TRUE_SIGHT_BLADE, 10.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.0F, 1.7F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
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
        if (nextMirrorMarkCount(level, player, target) < 3) {
            return 0.0F;
        }
        return target.getType().is(ModEntityTags.ILLUSION_MOBS)
                ? shatterDamage * 2.0F : shatterDamage;
    }

    public boolean wouldExposeTarget(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return nextMirrorMarkCount(level, player, target) >= 3;
    }

    /** Post 阶段确认 hurt 成功后，才提交镜痕和真身暴露。 */
    public void applyPostDamageEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        this.tickMirrorMark(level, player, target);
        if (!player.getCooldowns().isOnCooldown(this)) {
            LivingEntity real = findRealBody(level, target);
            if (real != null) {
                player.getCooldowns().addCooldown(this, REVEAL_COOLDOWN_TICKS);
                int glowTicks = cn.kurt6.unknown_echoes.ability.EchoArmorSets
                        .hasListenerClueBonus(player) ? 120 : 60;
                real.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0, false, false, false));
                spawnRevealTrail(level, player, real);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
        }
    }

    /**
     * 裂镜连斩:4 秒窗口内连续命中同一目标叠镜痕(目标身上浮现裂镜微光),
     * 第 3 层破碎——额外爆发伤害(幻象类双倍)+ 短暂发光 + 碎镜环。
     */
    private int nextMirrorMarkCount(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.monster.Enemy)) {
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
            // 层数只能靠这里读出来:粒子数按 count 递增,玩家据此判断还差几刀到破碎。
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * i / count;
                level.sendParticles(WeaponFxTheme.TRUE_SIGHT_BLADE.coreDust(1.3F),
                        target.getX() + Math.cos(angle) * 0.7,
                        target.getY() + target.getBbHeight() * 0.6,
                        target.getZ() + Math.sin(angle) * 0.7, 1, 0.0, 0.0, 0.0, 0.0);
            }
            level.playSound(null, target.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F + count * 0.3F);
            return;
        }

        data.remove(countKey);
        data.remove(timeKey);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        WeaponFx.heavyHitMark(level, target, WeaponFxTheme.TRUE_SIGHT_BLADE);
        level.playSound(null, target.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.1F, 1.5F);
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
    private static LivingEntity findRealBody(ServerLevel level, LivingEntity hit) {
        if (hit instanceof MirrorGuardian guardian && guardian.isIllusion()) {
            for (MirrorGuardian real : level.getEntitiesOfClass(MirrorGuardian.class,
                    hit.getBoundingBox().inflate(REVEAL_SEARCH_RADIUS),
                    g -> g.isAlive() && !g.isIllusion())) {
                return real;
            }
        }
        if (hit instanceof AbyssWatcher watcher && watcher.isClone()) {
            for (AbyssWatcher real : level.getEntitiesOfClass(AbyssWatcher.class,
                    hit.getBoundingBox().inflate(REVEAL_SEARCH_RADIUS),
                    w -> w.isAlive() && !w.isClone())) {
                return real;
            }
        }
        return null;
    }

    /** 从玩家眼前向真身铺一道短粒子轨迹(方向提示,不给精确坐标 UI)。 */
    private static void spawnRevealTrail(ServerLevel level, ServerPlayer player, LivingEntity real) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = real.position().add(0, real.getBbHeight() * 0.5, 0).subtract(from).normalize();
        // 7 格是方向提示的上限:铺到真身会把精确坐标交给客户端,谜题就白设了。
        WeaponFx.dashTrail(level, from, dir, 7.0D, WeaponFxTheme.TRUE_SIGHT_BLADE);
    }
}
