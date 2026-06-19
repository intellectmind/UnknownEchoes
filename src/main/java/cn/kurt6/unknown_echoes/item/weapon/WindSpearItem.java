package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 风纹长矛(V0.6B 三件武器之一,V0.6C 强化):伤害 8 / 攻速 1.2(V3.2 上调,原设定基线 6)。
 * 右键"风驰突刺":服务端写入一段朝视线方向的冲量,冷却 8 秒;
 * V0.6C 起突刺化作风刃——路径上的敌人受 1.2×面板伤害并被风压掀开(突刺不再只是位移)。
 * 位移采用速度冲量而非传送:落点由物理碰撞决定,天然不能穿过风门、
 * 屏障与任何机关封锁方块——这正是"位移落点服务端校验"的实现方式(红线 #1 检查清单)。
 * 风刃伤害走常规 hurt 入口,Boss 未破防双保险照常钳制(红线 #4)。
 */
public class WindSpearItem extends SwordItem {

    private final int dashCooldownTicks;
    private final double dashStrength;
    private final float bladeDamage;
    /** 风刃判定:沿突刺路径的扫掠长度与半径。 */
    private static final double BLADE_LENGTH = 6.0;
    private static final double BLADE_RADIUS = 1.4;
    /** 风刃伤害 = 面板伤害 × 系数。 */
    public WindSpearItem(Properties properties) {
        this(8.0, 1.35, 160, 1.5, 8.0F * 0.9F, properties);
    }

    public WindSpearItem(double damage, double speed, int cooldownTicks, double dashStrength,
                         float bladeDamage, Properties properties) {
        super(EchoMetalTier.INSTANCE, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.dashCooldownTicks = cooldownTicks;
        this.dashStrength = dashStrength;
        this.bladeDamage = bladeDamage;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_spear.skill")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_spear.detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.wind_spear.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            // 垂直分量收紧:突刺是水平机动,不做飞天工具(能力/机关进度不靠它,纯手感)
            Vec3 dash = new Vec3(look.x, Mth.clamp(look.y * 0.5 + 0.1, -0.1, 0.45), look.z)
                    .normalize().scale(dashStrength);
            if (level instanceof ServerLevel serverLevel) {
                this.sweepWindBlade(serverLevel, player, dash.normalize());
            }
            player.setDeltaMovement(dash);
            player.hurtMarked = true;   // 服务端写运动后立即同步客户端
            player.getCooldowns().addCooldown(this, dashCooldownTicks);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 风刃扫掠(V0.6C 强化):突刺瞬间沿冲刺方向切出一道风刃——
     * 路径上的敌对生物受 1.2×面板伤害并被横向风压掀开;全程风迹粒子 + 风裂演出。
     */
    private void sweepWindBlade(ServerLevel level, Player player, Vec3 dir) {
        Vec3 start = player.position().add(0, player.getBbHeight() * 0.5, 0);
        // 风迹:沿路径铺云迹与电火花,突刺看起来像一道撕开的风
        for (double d = 0.5; d <= BLADE_LENGTH; d += 0.5) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.25, 0.25, 0.25, 0.01);
            if (d % 1.5 < 0.5) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(BLADE_LENGTH),
                e -> e != player && e.isAlive() && (e instanceof Enemy || e.isAttackable())
                        && !e.isAlliedTo(player))) {
            Vec3 toVictim = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(start);
            double along = toVictim.dot(dir);
            if (along < 0 || along > BLADE_LENGTH) {
                continue;
            }
            if (toVictim.subtract(dir.scale(along)).length() > BLADE_RADIUS) {
                continue;
            }
            if (victim.hurt(player.damageSources().playerAttack(player), bladeDamage)) {
                // 风压掀开:垂直于突刺方向把目标推离路径,玩家穿阵而过
                Vec3 side = toVictim.subtract(dir.scale(along));
                Vec3 push = (side.lengthSqr() > 1.0E-4 ? side.normalize() : new Vec3(-dir.z, 0, dir.x))
                        .scale(0.6).add(0, 0.35, 0);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push));
                victim.hurtMarked = true;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                        1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.GUST,
                        victim.getX(), victim.getY() + 0.3, victim.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }
}
