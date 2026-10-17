package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/** 无声圣堂的范围支援遗物。它不造成伤害，且不会控制或推离 Boss。 */
public final class HushedPilgrimageBellItem extends Item implements ItemActiveSkill {
    private static final int ENERGY_COST = 12;
    private static final int COOLDOWN_TICKS = 200;
    private static final double RADIUS = 16.0D;
    private static final double RADIUS_SQR = RADIUS * RADIUS;
    private static final int RING_STEPS = 32;

    public HushedPilgrimageBellItem(Properties properties) {
        super(properties);
    }

    /** 由「物品主动技能」键触发；右键不释放。 */
    @Override
    public boolean activateItemSkill(ServerLevel serverLevel, ServerPlayer serverPlayer,
                                     ItemStack stack, InteractionHand hand) {
        if (!canActivate(serverLevel, serverPlayer, stack, hand)
                || serverPlayer.getCooldowns().isOnCooldown(this)) {
            return false;
        }

        Vec3 origin = serverPlayer.position();
        List<ServerPlayer> allies = serverLevel.getPlayers(
                target -> isAlly(serverLevel, serverPlayer, target, origin));
        List<LivingEntity> hostiles = serverLevel.getEntitiesOfClass(LivingEntity.class,
                serverPlayer.getBoundingBox().inflate(RADIUS),
                target -> isHostile(serverLevel, serverPlayer, target, origin));
        if (allies.stream().noneMatch(HushedPilgrimageBellItem::canBenefitFromSupport)
                && hostiles.stream().noneMatch(
                target -> canBenefitFromHostile(serverPlayer, target))) {
            return false;
        }
        // 能量同步与效果事件都可能重入；明确支付失败时才释放冷却。
        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!ArtifactManager.spendEnergy(serverPlayer, ENERGY_COST)) {
            serverPlayer.getCooldowns().removeCooldown(this);
            return false;
        }

        for (ServerPlayer ally : allies) {
            if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
                return true;
            }
            if (!isAlly(serverLevel, serverPlayer, ally, origin)) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, 200, 0, true, false, true));
            if (canActivate(serverLevel, serverPlayer, stack, hand)
                    && isAlly(serverLevel, serverPlayer, ally, origin)) {
                FoodEffectSupport.applyAbsorption(ally, 300, 1, 8.0F);
            }
        }

        for (LivingEntity target : hostiles) {
            if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
                return true;
            }
            if (!isHostile(serverLevel, serverPlayer, target, origin)) {
                continue;
            }
            if (isBoss(target)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 100, 0, true, false, true));
                continue;
            }
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 200, 1, true, false, true));
            if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
                return true;
            }
            if (!isHostile(serverLevel, serverPlayer, target, origin) || isBoss(target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 200, 1, true, false, true));
            if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
                return true;
            }
            if (!isHostile(serverLevel, serverPlayer, target, origin) || isBoss(target)) {
                continue;
            }
            Vec3 away = target.position().subtract(origin);
            if (away.horizontalDistanceSqr() > 0.01D) {
                Vec3 push = away.normalize().scale(0.65D);
                target.push(push.x, 0.18D, push.z);
                target.hurtMarked = true;
            }
        }

        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }
        for (int step = 0; step < RING_STEPS; step++) {
            double angle = Math.PI * 2.0D * step / RING_STEPS;
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                    origin.x + Math.cos(angle) * RADIUS * 0.5D,
                    origin.y + 0.35D,
                    origin.z + Math.sin(angle) * RADIUS * 0.5D,
                    2, 0.06D, 0.05D, 0.06D, 0.0D);
        }
        serverLevel.playSound(null, origin.x, origin.y, origin.z,
                ModSounds.ITEM_PILGRIMAGE_BELL_RING.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        if (canActivate(serverLevel, serverPlayer, stack, hand)) {
            serverPlayer.swing(hand, true);
        }
        return true;
    }

    private boolean canActivate(ServerLevel level, ServerPlayer player,
                                ItemStack stack, InteractionHand hand) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && stack.is(this) && player.getItemInHand(hand) == stack;
    }

    private static boolean isAlly(ServerLevel level, ServerPlayer player,
                                  ServerPlayer target, Vec3 origin) {
        return target.level() == level && target.isAlive() && !target.isSpectator()
                && target.position().distanceToSqr(origin) <= RADIUS_SQR
                && (target == player || !player.canHarmPlayer(target));
    }

    private static boolean isHostile(ServerLevel level, ServerPlayer player,
                                     LivingEntity target, Vec3 origin) {
        return target.level() == level && target instanceof Enemy && target.isAlive()
                && target.position().distanceToSqr(origin) <= RADIUS_SQR
                && !target.isAlliedTo(player);
    }

    private static boolean canBenefitFromSupport(ServerPlayer player) {
        if (needsEffectRefresh(player.getEffect(MobEffects.DAMAGE_RESISTANCE), 200, 0)
                || FoodEffectSupport.needsAbsorptionRefresh(player, 300, 1, 8.0F)) {
            return true;
        }
        return false;
    }

    private static boolean canBenefitFromHostile(ServerPlayer player, LivingEntity target) {
        if (isBoss(target)) {
            return needsEffectRefresh(target.getEffect(MobEffects.WEAKNESS), 100, 0);
        }
        return needsEffectRefresh(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN), 200, 1)
                || needsEffectRefresh(target.getEffect(MobEffects.WEAKNESS), 200, 1)
                || target.position().subtract(player.position()).horizontalDistanceSqr() > 0.01D;
    }

    private static boolean needsEffectRefresh(MobEffectInstance current,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(current, durationTicks, amplifier);
    }

    private static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModEntityTags.ECHO_BOSSES)
                || target.getType().is(Tags.EntityTypes.BOSSES);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip.unknown_echoes.hushed_pilgrimage_bell.skill",
                        Component.keybind("key.unknown_echoes.item_active_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.unknown_echoes.hushed_pilgrimage_bell.ally")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.unknown_echoes.hushed_pilgrimage_bell.detail")
                .withStyle(ChatFormatting.GRAY));
    }
}
