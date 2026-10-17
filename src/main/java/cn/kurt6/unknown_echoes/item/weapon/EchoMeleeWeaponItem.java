package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 第一维度通用近战武器实现:正式武器共享基础数值和服务端命中特效。 */
public class EchoMeleeWeaponItem extends SwordItem implements WeaponSkill {
    private static final String BROADSWORD_TARGET_KEY = "ue_broadsword_target";
    private static final String BROADSWORD_COUNT_KEY = "ue_broadsword_count";
    private static final String BROADSWORD_TIME_KEY = "ue_broadsword_time";
    private static final String HAMMER_TIME_KEY = "ue_bell_hammer_time";
    private static final int MAX_ACTIVE_HAMMER_MARKS = 64;
    /** 攻击瞬间的蓄力值:Player.attack 在 hurt 之前就重置蓄力条,事件里现读恒为 0,由 CommonEvents 预先记录。 */
    public static final String ATTACK_CHARGE_KEY = "ue_attack_charge";

    protected final String tooltipKey;
    private static final int BROADSWORD_PRIMARY_COOLDOWN = 120;
    private static final int BROADSWORD_SECONDARY_COOLDOWN = 300;
    private static final int BATTLEAXE_PRIMARY_COOLDOWN = 160;
    private static final int BATTLEAXE_SECONDARY_COOLDOWN = 300;
    private static final int SILENCE_PRIMARY_COOLDOWN = 140;
    private static final int SILENCE_SECONDARY_COOLDOWN = 360;
    private static final int BELL_HAMMER_PRIMARY_COOLDOWN = 180;
    private static final int BELL_HAMMER_SECONDARY_COOLDOWN = 400;

    public EchoMeleeWeaponItem(double damage, double speed, String tooltipKey, Properties properties) {
        this(EchoMetalTier.INSTANCE, damage, speed, tooltipKey, properties);
    }

    protected EchoMeleeWeaponItem(Tier tier, double damage, double speed,
                                  String tooltipKey, Properties properties) {
        super(tier, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return cn.kurt6.unknown_echoes.particle.ParticleRules.data(stack).total() == 50 || super.isFoil(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        boolean matchesThemedMaterial = switch (tooltipKey) {
            case "echo_battleaxe" ->
                    repairCandidate.is(ModItems.BROKEN_CLOCKWORK_PLATE.get());
            case "broken_bell_hammer" ->
                    repairCandidate.is(ModItems.BROKEN_CLOCKWORK_PLATE.get())
                            || repairCandidate.is(ModItems.BROKEN_BELL_UPGRADE_CORE.get());
            case "silence_scythe" -> repairCandidate.is(ModItems.SILENT_RESIN.get());
            case "crystal_song_staff" ->
                    repairCandidate.is(ModItems.CRYSTAL_RESONANCE_ROD.get())
                            || repairCandidate.is(ModItems.CRYSTAL_SONG_UPGRADE_CORE.get());
            case "echo_oathblade" ->
                    repairCandidate.is(ModItems.OATHBOUND_ALLOY.get())
                            || repairCandidate.is(ModItems.MEMORY_STONE_SLAB.get());
            default -> false;
        };
        return matchesThemedMaterial || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean hasSecondarySkill() {
        return switch (tooltipKey) {
            case "echo_broadsword", "echo_battleaxe", "silence_scythe", "broken_bell_hammer" -> true;
            default -> false;
        };
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, net.minecraft.world.InteractionHand hand) {
        if (!hasSecondarySkill()) {
            return false;
        }
        int cooldown = switch (tooltipKey) {
            case "echo_broadsword" -> BROADSWORD_PRIMARY_COOLDOWN;
            case "echo_battleaxe" -> BATTLEAXE_PRIMARY_COOLDOWN;
            case "silence_scythe" -> SILENCE_PRIMARY_COOLDOWN;
            case "broken_bell_hammer" -> BELL_HAMMER_PRIMARY_COOLDOWN;
            default -> 0;
        };
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), cooldown, 0)) {
            return false;
        }
        switch (tooltipKey) {
            case "echo_broadsword" -> activateBroadswordPrimary(level, player);
            case "echo_battleaxe" -> activateBattleaxePrimary(level, player);
            case "silence_scythe" -> activateSilencePrimary(level, player);
            case "broken_bell_hammer" -> activateBellHammerPrimary(level, player);
            default -> {
                return false;
            }
        }
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, net.minecraft.world.InteractionHand hand) {
        if (!hasSecondarySkill()) {
            return false;
        }
        int cooldown = switch (tooltipKey) {
            case "echo_broadsword" -> BROADSWORD_SECONDARY_COOLDOWN;
            case "echo_battleaxe" -> BATTLEAXE_SECONDARY_COOLDOWN;
            case "silence_scythe" -> SILENCE_SECONDARY_COOLDOWN;
            case "broken_bell_hammer" -> BELL_HAMMER_SECONDARY_COOLDOWN;
            default -> 0;
        };
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), cooldown, 0,
                () -> switch (tooltipKey) {
                    case "echo_broadsword" ->
                            WeaponSkillSupport.needsAbsorptionRefresh(player, 120, 1, 8.0F)
                                    || WeaponSkillSupport.needsEffectRefresh(
                                    player, MobEffects.DAMAGE_RESISTANCE, 60, 0);
                    case "broken_bell_hammer" ->
                            WeaponSkillSupport.needsEffectRefresh(
                                    player, MobEffects.DAMAGE_RESISTANCE, 120, 0)
                                    || WeaponSkillSupport.needsEffectRefresh(
                                    player, MobEffects.DAMAGE_BOOST, 160, 0)
                                    || WeaponSkillSupport.needsAbsorptionRefresh(player, 80, 0, 4.0F);
                    default -> true;
                })) {
            return false;
        }
        switch (tooltipKey) {
            case "echo_broadsword" -> activateBroadswordSecondary(level, player);
            case "echo_battleaxe" -> activateBattleaxeSecondary(level, player);
            case "silence_scythe" -> activateSilenceSecondary(level, player);
            case "broken_bell_hammer" -> activateBellHammerSecondary(level, player);
            default -> {
                return false;
            }
        }
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private String skillKey(String slot) {
        return "ue_weapon_skill_" + tooltipKey + "_" + slot + "_until";
    }

    private void activateBroadswordPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        for (LivingEntity target : WeaponSkillSupport.coneTargets(level, player, 6.0D, 0.42D, 5)) {
            if (WeaponSkillSupport.hurt(player, target, 12.0F)) {
                WeaponFx.hitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
                }
            }
        }
        WeaponFx.slashArc(level, player, fx, 6.0D, WeaponFx.halfAngleOf(0.42D));
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_BROADSWORD_PRIMARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void activateBroadswordSecondary(ServerLevel level, ServerPlayer player) {
        WeaponSkillSupport.applyAbsorption(player, 120, 1, 8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.of(tooltipKey), 1.05D, 2);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_BROADSWORD_SECONDARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void activateBattleaxePrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        for (LivingEntity target : WeaponSkillSupport.coneTargets(level, player, 5.0D, 0.25D, 4)) {
            if (WeaponSkillSupport.hurt(player, target, 18.0F)) {
                WeaponFx.heavyHitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1, true, false, true));
                }
            }
        }
        WeaponFx.slashArc(level, player, fx, 5.0D, WeaponFx.halfAngleOf(0.25D));
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_BATTLEAXE_PRIMARY.get(),
                SoundSource.PLAYERS, 0.85F, 1.0F);
    }

    private void activateBattleaxeSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        int affected = 0;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 5.0D, 6)) {
            if (!WeaponSkillSupport.hurt(player, target, 13.0F)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1, true, false, true));
                target.knockback(0.7D, player.getX() - target.getX(), player.getZ() - target.getZ());
                target.hurtMarked = true;
            }
            affected++;
        }
        WeaponFx.groundRing(level, player, fx, 5.0D);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_ECHO_BATTLEAXE_SECONDARY.get(),
                SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private void activateSilencePrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        for (LivingEntity target : WeaponSkillSupport.coneTargets(level, player, 7.0D, 0.15D, 7)) {
            if (WeaponSkillSupport.hurt(player, target, 14.0F)) {
                WeaponFx.hitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1, true, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, true, false, true));
                }
            }
        }
        WeaponFx.slashArc(level, player, fx, 7.0D, WeaponFx.halfAngleOf(0.15D));
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_SILENCE_SCYTHE_PRIMARY.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private void activateSilenceSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 7.0D, 8)) {
            if (!WeaponSkillSupport.hurt(player, target, 9.0F)) {
                continue;
            }
            WeaponFx.hitMark(level, target, fx);
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, false, true));
            }
        }
        WeaponFx.groundRing(level, player, fx, 7.0D);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_SILENCE_SCYTHE_SECONDARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private void activateBellHammerPrimary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 5.0D, 8)) {
            if (WeaponSkillSupport.hurt(player, target, 20.0F)) {
                WeaponFx.heavyHitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.knockback(0.8D, player.getX() - target.getX(), player.getZ() - target.getZ());
                    target.hurtMarked = true;
                }
            }
        }
        WeaponFx.groundRing(level, player, fx, 5.0D);
        WeaponFx.releaseBurst(level, player.position().add(0, .9, 0), fx, 1.1D);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_BROKEN_BELL_HAMMER_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void activateBellHammerSecondary(ServerLevel level, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, true, false, true));
        WeaponSkillSupport.applyAbsorption(player, 80, 0, 4.0F);
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.of(tooltipKey), 1.15D, 3);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_BROKEN_BELL_HAMMER_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".passive")
                .withStyle(ChatFormatting.GRAY));
    }

    /** Incoming 阶段只预览追加伤害，不提交连击、冷却或状态。 */
    public float getDamageEventBonus(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return switch (tooltipKey) {
            case "echo_broadsword" -> nextBroadswordCount(level, player, target) >= 3 ? 3.0F : 0.0F;
            case "broken_bell_hammer" -> bellHammerBonus(level, player, target);
            case "echo_oathblade" -> oathbladeBonus(target);
            default -> 0.0F;
        };
    }

    /** Post 阶段确认 hurt 成功后，才提交命中特效与持久状态。 */
    public void applyPostDamageEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        switch (tooltipKey) {
            case "echo_broadsword" -> applyBroadsword(level, player, target);
            case "echo_battleaxe" -> applyBattleaxe(player, target);
            case "silence_scythe" -> applySilenceScythe(level, target);
            case "crystal_song_staff" -> applyCrystalSongStaff(level, target);
            case "broken_bell_hammer" -> applyBellHammer(level, player, target);
            default -> {
            }
        }
    }

    public boolean wouldExposeTarget(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return "echo_broadsword".equals(tooltipKey)
                && nextBroadswordCount(level, player, target) >= 3;
    }

    private int nextBroadswordCount(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy)) {
            return 0;
        }
        var data = player.getPersistentData();
        String targetId = target.getUUID().toString();
        long now = level.getGameTime();
        long last = data.getLong(BROADSWORD_TIME_KEY);
        int previousCount = Math.max(0, Math.min(2, data.getInt(BROADSWORD_COUNT_KEY)));
        boolean continues = targetId.equals(data.getString(BROADSWORD_TARGET_KEY))
                && last >= 0L && last <= now && now - last <= 100L;
        return continues ? previousCount + 1 : 1;
    }

    private void applyBroadsword(ServerLevel level, ServerPlayer player, LivingEntity target) {
        int count = nextBroadswordCount(level, player, target);
        if (count <= 0) {
            return;
        }
        var data = player.getPersistentData();
        String targetId = target.getUUID().toString();
        data.putString(BROADSWORD_TARGET_KEY, targetId);
        data.putLong(BROADSWORD_TIME_KEY, level.getGameTime());
        if (count < 3) {
            data.putInt(BROADSWORD_COUNT_KEY, count);
            return;
        }
        data.putInt(BROADSWORD_COUNT_KEY, 0);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, true));
        WeaponFx.heavyHitMark(level, target, WeaponFxTheme.of(tooltipKey));
    }

    private void applyBattleaxe(ServerPlayer player, LivingEntity target) {
        if (!isStrongAttack(player) || isBossLike(target) || !(target instanceof Enemy)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0, true, false, true));
    }

    private void applySilenceScythe(ServerLevel level, LivingEntity target) {
        if (isBossLike(target) || !(target instanceof Enemy)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        WeaponFx.hitMark(level, target, WeaponFxTheme.of(tooltipKey));
    }

    private float bellHammerBonus(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isStrongAttack(player) || !(target instanceof Enemy)) {
            return 0.0F;
        }
        var data = target.getPersistentData();
        String timeKey = HAMMER_TIME_KEY + "_" + player.getUUID();
        long now = level.getGameTime();
        if (isValidHammerMark(data, timeKey, now)) {
            return isBossLike(target) ? 2.0F : 5.0F;
        }
        return 0.0F;
    }

    private void applyBellHammer(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isStrongAttack(player) || !(target instanceof Enemy)) {
            return;
        }
        var data = target.getPersistentData();
        String timeKey = HAMMER_TIME_KEY + "_" + player.getUUID();
        long now = level.getGameTime();
        if (isValidHammerMark(data, timeKey, now)) {
            WeaponFx.heavyHitMark(level, target, WeaponFxTheme.of(tooltipKey));
            level.playSound(null, target.blockPosition(), ModSounds.WEAPON_BROKEN_BELL_HAMMER_PRIMARY.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            data.remove(timeKey);
            pruneHammerMarks(data, now, MAX_ACTIVE_HAMMER_MARKS);
            return;
        }
        pruneHammerMarks(data, now, MAX_ACTIVE_HAMMER_MARKS - 1);
        data.putLong(timeKey, now);
    }

    private static boolean isValidHammerMark(CompoundTag data, String timeKey, long now) {
        if (!data.contains(timeKey, Tag.TAG_LONG)) {
            return false;
        }
        long markedAt = data.getLong(timeKey);
        return markedAt >= 0L && markedAt <= now && now - markedAt <= 60L;
    }

    private static void pruneHammerMarks(CompoundTag data, long now, int maxCount) {
        List<HammerMark> active = new ArrayList<>();
        for (String key : List.copyOf(data.getAllKeys())) {
            if (!key.startsWith(HAMMER_TIME_KEY + "_")) {
                continue;
            }
            if (isValidHammerMark(data, key, now)) {
                active.add(new HammerMark(key, data.getLong(key)));
            } else {
                data.remove(key);
            }
        }
        int excess = active.size() - maxCount;
        if (excess <= 0) {
            return;
        }
        active.sort(Comparator.comparingLong(HammerMark::markedAt));
        for (int i = 0; i < excess; i++) {
            data.remove(active.get(i).key());
        }
    }

    private record HammerMark(String key, long markedAt) {
    }

    private void applyCrystalSongStaff(ServerLevel level, LivingEntity target) {
        if (!(target instanceof Enemy) || isBossLike(target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
        WeaponFx.hitMark(level, target, WeaponFxTheme.CRYSTAL_SONG_STAFF);
        level.playSound(null, target.blockPosition(),
                ModSounds.WEAPON_CRYSTAL_SONG_STAFF_PRIMARY.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private float oathbladeBonus(LivingEntity target) {

        if (isBoss(target)) {
            return 0.0F;
        }
        return target.getType().is(ModEntityTags.ECHO_REALM_HOSTILES) ? 2.0F : 0.0F;
    }

    protected static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModEntityTags.ECHO_BOSSES)
                || target.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean isBossLike(LivingEntity target) {
        return isBoss(target);
    }

    private static boolean isStrongAttack(ServerPlayer player) {
        return player.getPersistentData().getFloat(ATTACK_CHARGE_KEY) >= 0.9F;
    }
}
