package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 灰烬抄录员武器线：高伤快速装填弩，以持续燃烧与焚稿范围压制回访战斗。 */
public final class AshScriptCrossbowItem extends CrossbowItem implements WeaponSkill {
    private static final int QUICK_LOAD_TICKS = 7;
    private static final int PRIMARY_COOLDOWN_TICKS = 140;
    private static final int SECONDARY_COOLDOWN_TICKS = 320;

    public AshScriptCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.requireCrossbowCharge(player, stack, hand,
                CrossbowItem.getChargeDuration(stack, player) - QUICK_LOAD_TICKS)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("primary"),
                PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        WeaponSkillSupport.consumeCrossbowCharge(stack);
        for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player,
                26.0D, 1.3D, 6)) {
            if (!WeaponSkillSupport.hurt(player, target, 19.0F)) {
                continue;
            }
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        100, 1, true, false, true));
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ASH_SCRIPT_CROSSBOW);
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        WeaponFx.pierceBeam(level, player, WeaponFxTheme.ASH_SCRIPT_CROSSBOW, eye, look,
                WeaponFx.clippedBeamLength(level, player, eye, look, 26.0D));
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 0.95F, 0.82F);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"),
                SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                160, 0, true, false, true));
        int hits = 0;
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player,
                8.0D, 10)) {
            if (!WeaponSkillSupport.hurt(player, target, 14.0F)) {
                continue;
            }
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 120));
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        100, 1, true, false, true));
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.ASH_SCRIPT_CROSSBOW);
            hits++;
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.ASH_SCRIPT_CROSSBOW, 8.0D);
        level.sendParticles(ParticleTypes.SMALL_FLAME,
                player.getX(), player.getY() + 0.6D, player.getZ(),
                Math.max(8, hits * 3), 2.0D, 0.35D, 2.0D, 0.03D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 1.0F, 0.68F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private static String skillKey(String slot) {
        return "ue_weapon_skill_ash_script_crossbow_" + slot + "_until";
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft - QUICK_LOAD_TICKS);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon,
                                          ItemStack ammo, boolean isCrit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(5.4D);
            arrow.setRemainingFireTicks(Math.max(arrow.getRemainingFireTicks(), 80));
        }
        return projectile;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ASH_SCRIPT_MECHANISM.get())
                || repairCandidate.is(ModItems.DEEP_ALLOY_INGOT.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.ash_script_crossbow.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.ash_script_crossbow.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.ash_script_crossbow.secondary_buff")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.ash_script_crossbow.passive")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
