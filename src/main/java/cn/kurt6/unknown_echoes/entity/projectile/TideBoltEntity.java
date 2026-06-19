package cn.kurt6.unknown_echoes.entity.projectile;

import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 潮汐弩矢(V0.6B,V0.6C 强化):潮汐弩发射的弹射物。
 * 武器特性只随武器/材料线存在,不读取玩家能力研究:
 * - 水下弹道无衰减(getWaterInertia 0.99,原版箭 0.6)。
 * - 潮爆:命中生物或方块时水压炸裂,敌人受 50% 弩伤并被冲开。
 * - 对身处水中的目标,直击伤害 +25%。
 * - 命中生物时,若附近存在水下符文,向射手铺一道指向符文的粒子轨迹(冷却 12 秒/人)。
 * 注:直接继承 AbstractArrow(Arrow 的便捷构造写死了 ARROW 类型),药水箭装填后按普通弩矢处理。
 */
public class TideBoltEntity extends AbstractArrow {

    /** 符文方向标记冷却(tick/玩家):设定基线 12 秒。瞬态表现层数据,不入存档。 */
    private static final int MARK_COOLDOWN_TICKS = 240;
    private static final int RUNE_SEARCH_RADIUS = 16;
    private static final Map<UUID, Long> LAST_MARK = new HashMap<>();

    /** 潮爆半径与伤害系数(相对直击伤害)。 */
    private static final double BURST_RADIUS = 2.5;
    private static final float BURST_DAMAGE_FACTOR = 0.5F;
    /** 水中目标直击增伤。 */
    private static final float WET_BONUS = 1.25F;

    /** 潮爆只结算一次(命中实体后弹体仍可能再撞方块)。 */
    private boolean burstDone = false;

    public TideBoltEntity(EntityType<? extends TideBoltEntity> type, Level level) {
        super(type, level);
    }

    public TideBoltEntity(Level level, LivingEntity owner,
                          ItemStack pickupStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.TIDE_BOLT.get(), owner, level, pickupStack, firedFromWeapon);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    /** 潮汐弩矢:水下弹道无衰减(原版箭 0.6)。 */
    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        // 潮汐弩武器特性:水中目标直击 +25%。
        if (result.getEntity().isInWater()) {
            this.setBaseDamage(this.getBaseDamage() * WET_BONUS);
        }
        super.onHitEntity(result);
        this.tideBurst(result.getEntity().position()
                .add(0, result.getEntity().getBbHeight() * 0.5, 0), result.getEntity());
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);
        this.tideBurst(result.getLocation(), null);
    }

    /** 潮爆(V0.6C):命中点水压炸裂,范围伤害 + 径向冲开 + 水花环演出。 */
    private void tideBurst(Vec3 center, @Nullable net.minecraft.world.entity.Entity directHit) {
        if (this.burstDone || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.burstDone = true;
        double burstRadius = BURST_RADIUS;
        float burstDamage = (float) (this.getBaseDamage()
                * this.getDeltaMovement().length() * BURST_DAMAGE_FACTOR);
        burstDamage = Math.max(2.0F, Math.min(burstDamage, 6.5F));
        var owner = this.getOwner();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(burstRadius),
                e -> e != owner && e != directHit && e.isAlive()
                        && !(owner instanceof LivingEntity living && e.isAlliedTo(living)))) {
            victim.hurt(owner instanceof LivingEntity living
                            ? this.damageSources().mobProjectile(this, living)
                            : this.damageSources().generic(),
                    victim.isInWater() ? burstDamage * WET_BONUS : burstDamage);
            Vec3 away = victim.position().subtract(center).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.6, 0.25, dir.z * 0.6));
                victim.hurtMarked = true;
            }
        }
        // 潮爆演出:水花穹顶 + 扩散气泡环 + 一声闷响
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                center.x, center.y, center.z, 30, 0.8, 0.5, 0.8, 0.2);
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    center.x + Math.cos(angle) * burstRadius * 0.7,
                    center.y + 0.2,
                    center.z + Math.sin(angle) * burstRadius * 0.7, 2, 0.1, 0.15, 0.1, 0.02);
        }
        serverLevel.sendParticles(ParticleTypes.GLOW,
                center.x, center.y + 0.3, center.z, 6, 0.4, 0.3, 0.4, 0.02);
        serverLevel.playSound(null, BlockPos.containing(center),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.7F);
        serverLevel.playSound(null, BlockPos.containing(center),
                net.minecraft.sounds.SoundEvents.DOLPHIN_SPLASH,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !(this.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        long now = serverLevel.getGameTime();
        Long last = LAST_MARK.get(player.getUUID());
        if (last != null && now - last < MARK_COOLDOWN_TICKS) {
            return;
        }
        BlockPos.findClosestMatch(target.blockPosition(), RUNE_SEARCH_RADIUS, 8,
                pos -> serverLevel.getBlockState(pos).is(ModBlocks.TIDE_RUNE.get()))
                .ifPresent(runePos -> {
                    LAST_MARK.put(player.getUUID(), now);
                    Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 dir = Vec3.atCenterOf(runePos).subtract(from).normalize();
                    // 聆听者 2 件:线索显示时间延长(11.3)——轨迹更长更密
                    int steps = cn.kurt6.unknown_echoes.ability.EchoArmorSets
                            .hasListenerClueBonus(player) ? 14 : 8;
                    for (int i = 1; i <= steps; i++) {
                        Vec3 p = from.add(dir.scale(0.8 * i));
                        serverLevel.sendParticles(ParticleTypes.GLOW,
                                p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.tide_crossbow.marked"), true);
                });
    }
}
