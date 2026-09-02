package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 倒悬司阍武器线：快速蓄力的深层长弓，以浮空控制换取稳定射击窗口。 */
public final class InvertedBowItem extends BowItem implements WeaponSkill {
    private static final int QUICK_DRAW_TICKS = 5;
    private static final int PRIMARY_COOLDOWN_TICKS = 120;
    private static final int SECONDARY_COOLDOWN_TICKS = 280;

    public InvertedBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.requireBowDraw(player, stack, hand,
                20 - QUICK_DRAW_TICKS)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("primary"),
                PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player,
                24.0D, 1.1D, 4)) {
            if (!WeaponSkillSupport.hurt(player, target, 18.0F)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.GRAVITY_INVERSION_BOW);
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                        45, 0, true, false, true));
            }
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        WeaponFx.pierceBeam(level, player, WeaponFxTheme.GRAVITY_INVERSION_BOW, eye, look,
                WeaponFx.clippedBeamLength(level, player, eye, look, 24.0D));
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.58F);
        WeaponSkillSupport.finish(player, stack, hand, 1);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"),
                SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                120, 0, true, false, true));
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player,
                8.0D, 8)) {
            if (!WeaponSkillSupport.hurt(player, target, 13.0F)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.GRAVITY_INVERSION_BOW);
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                // 悬浮把目标抬离地面,升柱让"为什么飘起来"有可见的来源。
                WeaponFx.dashTrail(level, target.position(), new Vec3(0.0D, 1.0D, 0.0D),
                        target.getBbHeight() + 1.0D, WeaponFxTheme.GRAVITY_INVERSION_BOW);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                        60, 0, true, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        80, 1, true, false, true));
            }
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.GRAVITY_INVERSION_BOW, 8.0D);
        level.playSound(null, player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.PLAYERS, 1.0F, 0.72F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private static String skillKey(String slot) {
        return "ue_weapon_skill_gravity_inversion_bow_" + slot + "_until";
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft - QUICK_DRAW_TICKS);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
                                    float velocity, float inaccuracy, float angle,
                                    LivingEntity target) {
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(4.6D);
        }
        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.INVERTED_BOW_LIMB.get())
                || repairCandidate.is(ModItems.DEEP_ALLOY_INGOT.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.gravity_inversion_bow.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.gravity_inversion_bow.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.unknown_echoes.gravity_inversion_bow.secondary_buff")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.gravity_inversion_bow.passive")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
