package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** 潮汐弩；弹射物伤害以 ModItems 和 TideBoltEntity 的实际配置为准。 */
public class TideCrossbowItem extends CrossbowItem implements WeaponSkill {
    private static final String VIRTUAL_AMMO_KEY = "ue_tide_virtual";
    private static final int PRIMARY_COOLDOWN_TICKS = 160;
    private static final int SECONDARY_COOLDOWN_TICKS = 360;

    private final double boltBaseDamage;
    private final int quickLoadTicks;

    public TideCrossbowItem(double boltBaseDamage, int quickLoadTicks, Properties properties) {
        super(properties);
        this.boltBaseDamage = boltBaseDamage;
        this.quickLoadTicks = quickLoadTicks;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return cn.kurt6.unknown_echoes.particle.ParticleRules.data(stack).total() == 50 || super.isFoil(stack);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.requireCrossbowCharge(player, stack, hand,
                CrossbowItem.getChargeDuration(stack, player) - quickLoadTicks)) {
            return false;
        }
        if (!WeaponSkillSupport.begin(player, skillKey("primary"), PRIMARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        WeaponSkillSupport.consumeCrossbowCharge(stack);
        for (LivingEntity target : WeaponSkillSupport.lineTargets(level, player, 20.0D, 1.2D, 5)) {
            if (WeaponSkillSupport.hurt(player, target, 15.0F)) {
                WeaponFx.hitMark(level, target, WeaponFxTheme.TIDE_CROSSBOW);
                if (!EchoMeleeWeaponItem.isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            70, 0, true, false, true));
                }
            }
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        WeaponFx.pierceBeam(level, player, WeaponFxTheme.TIDE_CROSSBOW, eye, look,
                WeaponFx.clippedBeamLength(level, player, eye, look, 20.0D));
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_TIDE_CROSSBOW_PRIMARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        WeaponSkillSupport.finish(player, stack, hand, 2);
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player, skillKey("secondary"), SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 300, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 7.0D, 8)) {
            if (WeaponSkillSupport.hurt(player, target, 10.0F)) {
                WeaponFx.hitMark(level, target, WeaponFxTheme.TIDE_CROSSBOW);
                if (target instanceof Enemy && !EchoMeleeWeaponItem.isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            100, 1, true, false, true));
                }
            }
        }
        // BUBBLE 离水当 tick 自毁,陆上放会整段消失,所以护体只用染色 dust 螺旋。
        WeaponFx.groundRing(level, player, WeaponFxTheme.TIDE_CROSSBOW, 7.0D);
        WeaponFx.wardSpiral(level, player, WeaponFxTheme.TIDE_CROSSBOW, 1.1D, 3);
        level.playSound(null, player.blockPosition(), ModSounds.WEAPON_TIDE_CROSSBOW_SECONDARY.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private String skillKey(String slot) {
        return "ue_weapon_skill_tide_crossbow_" + slot + "_until";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.MIRROR_TIDE_ALLOY_PLATE.get())
                || repairCandidate.is(ModItems.WATCHER_EYE_SHARD.get())
                || repairCandidate.is(ModItems.BLACK_TIDE_THREAD.get())
                || repairCandidate.is(ModItems.TIDE_CORE.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.skill",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.skill_effect")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary_effect")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.secondary_buff")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.ammo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tide_crossbow.passive")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft - quickLoadTicks);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon,
                                          ItemStack ammo, boolean isCrit) {
        if (!isVanillaTideAmmo(ammo)) {

            return super.createProjectile(level, shooter, weapon, ammo, isCrit);
        }
        TideBoltEntity bolt = new TideBoltEntity(level, shooter,
                ammo.copyWithCount(1), weapon.copyWithCount(1));
        if (ammo.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).contains(VIRTUAL_AMMO_KEY)) {
            bolt.setVirtualAmmo();
        }
        bolt.setBaseDamage(boltBaseDamage);
        if (isCrit) {
            bolt.setCritArrow(true);
        }
        bolt.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        return bolt;
    }

    private static boolean isVanillaTideAmmo(ItemStack ammo) {
        return ammo.is(Items.ARROW) || ammo.is(Items.TIPPED_ARROW)
                || ammo.is(Items.SPECTRAL_ARROW);
    }

    @Override
    protected void shoot(net.minecraft.server.level.ServerLevel level, LivingEntity shooter,
                         net.minecraft.world.InteractionHand hand, ItemStack weapon,
                         java.util.List<ItemStack> projectiles, float velocity, float inaccuracy,
                         boolean critical, LivingEntity target) {
        cn.kurt6.unknown_echoes.particle.ParticleAttack.shoot(shooter, weapon,
                () -> super.shoot(level, shooter, hand, weapon, projectiles, velocity, inaccuracy, critical, target));
    }
}
