package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** 主动战技共用的服务端预算与目标筛选，避免各武器出现不同的边界语义。 */
final class WeaponSkillSupport {
    private static final Set<BeginKey> BEGINNING = new HashSet<>();
    private record BeginKey(UUID playerId, String skillKey) {}

    private WeaponSkillSupport() {
    }

    static boolean begin(ServerPlayer player, String key, int cooldownTicks, int energyCost) {
        return begin(player, key, cooldownTicks, energyCost, () -> true);
    }

    /** 检查冷却后再确认技能是否仍有实际收益，避免外部增益完整覆盖时白扣资源。 */
    static boolean begin(ServerPlayer player, String key, int cooldownTicks, int energyCost,
                         BooleanSupplier benefitCheck) {
        Level source = player.level();
        if (!canBegin(player, source)) {
            return false;
        }
        BeginKey beginKey = new BeginKey(player.getUUID(), key);
        if (!BEGINNING.add(beginKey)) {
            return false;
        }
        try {
            long now = player.server.overworld().getGameTime();
            long readyAt = normalizeCooldownDeadline(player, key, now, cooldownTicks);
            if (readyAt > now) {
                long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.weapon_skill.cooldown", seconds), true);
                return false;
            }
            if (!benefitCheck.getAsBoolean()) {
                return false;
            }
            if (!canBegin(player, source)) {
                return false;
            }
            ArtifactManager.EnergyPayment payment = ArtifactManager.spendEnergyQuietly(player, energyCost);
            if (payment == null) {
                return false;
            }
            player.getPersistentData().putLong(key, safeDeadline(now, cooldownTicks));
            if (payment.needsSync()) {
                ArtifactManager.syncToClient(player);
            }
            return canBegin(player, source);
        } finally {
            BEGINNING.remove(beginKey);
        }
    }

    private static boolean canBegin(ServerPlayer player, Level source) {
        return player.level() == source && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    /** 方向型弓技必须在服务端确认弓弦已经拉满，避免只拿着武器按键就消耗技能。 */
    static boolean requireBowDraw(ServerPlayer player, ItemStack stack,
                                  InteractionHand hand, int requiredDrawTicks) {
        Level source = player.level();
        if (!canCheckCharge(player, source, stack, hand)) {
            return false;
        }
        if (hasRequiredDraw(player, source, stack, hand, requiredDrawTicks)) {
            return true;
        }
        if (canCheckCharge(player, source, stack, hand)) {
            tellChargeRequired(player);
        }
        return false;
    }

    /** 弩技允许两种合法时机:弩已经完成原版装填,或仍按住右键且服务端蓄力已到终点。 */
    static boolean requireCrossbowCharge(ServerPlayer player, ItemStack stack,
                                         InteractionHand hand, int requiredChargeTicks) {
        Level source = player.level();
        if (!canCheckCharge(player, source, stack, hand)) {
            return false;
        }
        if (CrossbowItem.isCharged(stack) || hasRequiredDraw(player, source, stack, hand, requiredChargeTicks)) {
            return true;
        }
        if (canCheckCharge(player, source, stack, hand)) {
            tellChargeRequired(player);
        }
        return false;
    }

    private static boolean canCheckCharge(ServerPlayer player, Level source, ItemStack stack, InteractionHand hand) {
        return canBegin(player, source) && !stack.isEmpty() && player.getItemInHand(hand) == stack;
    }

    private static boolean isUsingStack(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return player.isUsingItem() && player.getUsedItemHand() == hand && player.getUseItem() == stack;
    }

    private static boolean hasRequiredDraw(ServerPlayer player, Level source, ItemStack stack,
                                            InteractionHand hand, int requiredDrawTicks) {
        if (!isUsingStack(player, stack, hand)) {
            return false;
        }
        int duration = stack.getUseDuration(player);
        return canCheckCharge(player, source, stack, hand) && isUsingStack(player, stack, hand)
                && player.getUseItemRemainingTicks() <= duration - requiredDrawTicks;
    }

    private static void tellChargeRequired(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.weapon_skill.require_charge"), true);
    }

    /** 消耗已完成装填的弩技窗口,避免随后松开右键又射出同一发普通弩矢。 */
    static void consumeCrossbowCharge(ItemStack stack) {
        if (stack.getItem() instanceof CrossbowItem) {
            stack.remove(DataComponents.CHARGED_PROJECTILES);
        }
    }

    static long normalizeCooldownDeadline(ServerPlayer player, String key,
                                          long now, int maximumDuration) {
        long readyAt = player.getPersistentData().getLong(key);
        long maximumReadyAt = safeDeadline(now, maximumDuration);
        if (readyAt > maximumReadyAt) {
            readyAt = maximumReadyAt;
            player.getPersistentData().putLong(key, readyAt);
        }
        return readyAt;
    }

    static boolean needsEffectRefresh(LivingEntity entity, Holder<MobEffect> effect,
                                      int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(entity, effect, durationTicks, amplifier);
    }

    static boolean needsAbsorptionRefresh(LivingEntity entity, int durationTicks,
                                          int amplifier, float absorptionHealth) {
        return FoodEffectSupport.needsAbsorptionRefresh(
                entity, durationTicks, amplifier, absorptionHealth);
    }

    /** 统一走物品侧共享结算，避免武器与消耗品的护盾边界分叉。 */
    static void applyAbsorption(ServerPlayer player, int durationTicks,
                                int amplifier, float absorptionHealth) {
        FoodEffectSupport.applyAbsorption(
                player, durationTicks, amplifier, absorptionHealth);
    }

    static long safeDeadline(long now, long duration) {
        long boundedDuration = Math.max(0L, duration);
        return now > Long.MAX_VALUE - boundedDuration
                ? Long.MAX_VALUE : now + boundedDuration;
    }

    static void finish(ServerPlayer player, ItemStack stack, InteractionHand hand, int durabilityCost) {
        Item item = stack.getItem();

        if (player.isUsingItem()
                && player.getUsedItemHand() == hand
                && player.getUseItem() == stack) {
            player.stopUsingItem();
        }
        if (durabilityCost > 0) {
            stack.hurtAndBreak(durabilityCost, player, player.getSlotForHand(hand));
        }
        player.swing(hand, true);
        player.awardStat(Stats.ITEM_USED.get(item));
    }

    static List<LivingEntity> radiusTargets(ServerLevel level, ServerPlayer player,
                                             double radius, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(radius),
                        target -> isHostile(player, target)
                                && player.distanceToSqr(target) <= radius * radius)
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(limit)
                .toList();
    }

    static List<LivingEntity> coneTargets(ServerLevel level, ServerPlayer player,
                                          double range, double minDot, int limit) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(origin, origin.add(look.scale(range))).inflate(range * 0.55D),
                        target -> isHostile(player, target))
                .stream()
                .filter(target -> {
                    Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
                    double distance = offset.length();
                    return distance > 0.0D && distance <= range
                            && offset.scale(1.0D / distance).dot(look) >= minDot
                            && player.hasLineOfSight(target);
                })
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(limit)
                .toList();
    }

    static List<LivingEntity> lineTargets(ServerLevel level, ServerPlayer player,
                                          double range, double width, int limit) {
        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = origin.add(direction.scale(range));
        return level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(origin, end).inflate(width),
                        target -> isHostile(player, target))
                .stream()
                .filter(target -> {
                    Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
                    double along = offset.dot(direction);
                    double targetRadius = target.getBbWidth() * 0.5D + width;
                    return along >= 0.25D && along <= range
                            && offset.subtract(direction.scale(along)).lengthSqr()
                            <= targetRadius * targetRadius
                            && player.hasLineOfSight(target);
                })
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(limit)
                .toList();
    }

    static boolean hurt(ServerPlayer player, LivingEntity target, float damage) {
        boolean[] hurt = {false};
        WeaponEffectGuard.run(player,
                () -> hurt[0] = target.hurt(player.damageSources().playerAttack(player), damage));
        return hurt[0];
    }

    static boolean isHostile(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator()
                && !target.isAlliedTo(player) && target instanceof Enemy;
    }
}
