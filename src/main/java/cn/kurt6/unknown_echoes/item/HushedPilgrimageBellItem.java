package cn.kurt6.unknown_echoes.item;

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
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return false;
        }

        List<ServerPlayer> allies = serverLevel.getPlayers(target -> !target.isSpectator()
                && target.distanceToSqr(serverPlayer) <= RADIUS_SQR
                && (target == serverPlayer || !serverPlayer.canHarmPlayer(target)));
        List<LivingEntity> hostiles = serverLevel.getEntitiesOfClass(LivingEntity.class,
                serverPlayer.getBoundingBox().inflate(RADIUS),
                target -> target instanceof Enemy && target.isAlive()
                        && target.distanceToSqr(serverPlayer) <= RADIUS_SQR
                        && !target.isAlliedTo(serverPlayer));
        if (allies.stream().noneMatch(HushedPilgrimageBellItem::canBenefitFromSupport)
                && hostiles.stream().noneMatch(
                target -> canBenefitFromHostile(serverPlayer, target))) {
            return false;
        }
        if (!ArtifactManager.spendEnergy(serverPlayer, ENERGY_COST)) {
            return false;
        }

        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        for (ServerPlayer ally : allies) {
            ally.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, 200, 0, true, false, true));
            FoodEffectSupport.applyAbsorption(ally, 300, 1, 8.0F);
        }

        for (LivingEntity target : hostiles) {
            if (isBoss(target)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 100, 0, true, false, true));
                continue;
            }
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 200, 1, true, false, true));
            target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 200, 1, true, false, true));
            Vec3 away = target.position().subtract(serverPlayer.position());
            if (away.horizontalDistanceSqr() > 0.01D) {
                Vec3 push = away.normalize().scale(0.65D);
                target.push(push.x, 0.18D, push.z);
                target.hurtMarked = true;
            }
        }

        for (int step = 0; step < RING_STEPS; step++) {
            double angle = Math.PI * 2.0D * step / RING_STEPS;
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                    serverPlayer.getX() + Math.cos(angle) * RADIUS * 0.5D,
                    serverPlayer.getY() + 0.35D,
                    serverPlayer.getZ() + Math.sin(angle) * RADIUS * 0.5D,
                    2, 0.06D, 0.05D, 0.06D, 0.0D);
        }
        serverLevel.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.7F, 0.55F);
        serverPlayer.swing(hand, true);
        return true;
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
