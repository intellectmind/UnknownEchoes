package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 第一维度弓类武器的轻量实现,保留原版弹道并补充玩法说明。 */
public class EchoBowItem extends BowItem implements WeaponSkill {
    private final String tooltipKey;
    private static final int ECHO_LONGBOW_PRIMARY_COOLDOWN = 140;
    private static final int ECHO_LONGBOW_SECONDARY_COOLDOWN = 320;
    private static final int DREAM_BLOOM_PRIMARY_COOLDOWN = 160;
    private static final int DREAM_BLOOM_SECONDARY_COOLDOWN = 360;

    public EchoBowItem(String tooltipKey, Properties properties) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.requireBowDraw(player, stack, hand, 20)) {
            return false;
        }
        int cooldown = tooltipKey.equals("echo_longbow")
                ? ECHO_LONGBOW_PRIMARY_COOLDOWN : DREAM_BLOOM_PRIMARY_COOLDOWN;
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), cooldown, 0)) {
            return false;
        }
        WeaponFxTheme.Theme fx = WeaponFxTheme.of(tooltipKey);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double range = tooltipKey.equals("echo_longbow") ? 22.0D : 24.0D;
        if (tooltipKey.equals("echo_longbow")) {
            for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player, 22.0D, 1.0D, 3)) {
                if (WeaponSkillSupport.hurt(player, target, 12.0F)) {
                    WeaponFx.hitMark(level, target, fx);
                    if (!EchoMeleeWeaponItem.isBoss(target)) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
                    }
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 0.9F, 0.75F);
        } else {
            for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player, 24.0D, 0.8D, 1)) {
                if (WeaponSkillSupport.hurt(player, target, 22.0F)) {
                    WeaponFx.heavyHitMark(level, target, fx);
                    if (!EchoMeleeWeaponItem.isBoss(target)) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1,
                                true, false, true));
                    }
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 1.0F, 0.55F);
        }
        WeaponFx.pierceBeam(level, player, fx, eye, look,
                WeaponFx.clippedBeamLength(level, player, eye, look, range));
        WeaponSkillSupport.finish(player, stack, hand, 1);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        int cooldown = tooltipKey.equals("echo_longbow")
                ? ECHO_LONGBOW_SECONDARY_COOLDOWN : DREAM_BLOOM_SECONDARY_COOLDOWN;
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), cooldown, 0)) {
            return false;
        }
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 7.0D,
                tooltipKey.equals("echo_longbow") ? 6 : 8)) {
            float damage = tooltipKey.equals("echo_longbow") ? 10.0F : 11.0F;
            if (!WeaponSkillSupport.hurt(player, target, damage)) {
                continue;
            }
            if (!EchoMeleeWeaponItem.isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        tooltipKey.equals("echo_longbow") ? 60 : 120,
                        tooltipKey.equals("echo_longbow") ? 0 : 1, true, false, true));
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.of(tooltipKey));
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.of(tooltipKey), 7.0D);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    private String skillKey(String slot) {
        return "ue_weapon_skill_" + tooltipKey + "_" + slot + "_until";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        boolean matchesMaterial = switch (tooltipKey) {
            case "echo_longbow" -> repairCandidate.is(ModItems.WINDWOVEN_THREAD.get());
            case "dream_bloom_bow" -> repairCandidate.is(ModItems.DREAM_NECTAR.get());
            default -> false;
        };
        return matchesMaterial || super.isValidRepairItem(stack, repairCandidate);
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
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
