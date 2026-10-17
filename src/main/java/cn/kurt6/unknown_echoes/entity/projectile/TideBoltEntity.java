package cn.kurt6.unknown_echoes.entity.projectile;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.item.weapon.WeaponProjectileFx;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 潮汐弩矢( 强化):潮汐弩发射的弹射物。 */
public class TideBoltEntity extends AbstractArrow {

    /** 每名玩家的符文方向搜索冷却；仅用于瞬态表现，不写入存档。 */
    private static final Map<UUID, Long> NEXT_MARK_SEARCH = new HashMap<>();

    public static void clearMarkCooldown(UUID playerId) {
        NEXT_MARK_SEARCH.remove(playerId);
    }

    /** 服务器停止时清理瞬态线索冷却,避免单机世界切换串状态。 */
    public static void clearAllMarkCooldowns() {
        NEXT_MARK_SEARCH.clear();
    }

    /** 潮爆半径与伤害系数(相对直击伤害)。 */

    /** 潮爆只结算一次(命中实体后弹体仍可能再撞方块)。 */
    private boolean burstDone = false;

    public TideBoltEntity(EntityType<? extends TideBoltEntity> type, Level level) {
        super(type, level);
    }

    public TideBoltEntity(Level level, LivingEntity owner,
                          ItemStack pickupStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.TIDE_BOLT.get(), owner, level, pickupStack, firedFromWeapon);
    }

    public void setVirtualAmmo() {
        this.pickup = Pickup.CREATIVE_ONLY;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    /** 水中减速系数走服务端配置，不用原版的固定 0.6。 */
    @Override
    protected float getWaterInertia() {
        return ServerConfig.TIDE_BOLT_WATER_INERTIA.get().floatValue();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround && !this.isRemoved() && this.tickCount % 2 == 0) {
            WeaponProjectileFx.tideTrail(this.level(), this.position(), this.getDeltaMovement());
        }
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult result) {
        Entity directHit = result.getEntity();
        // 受伤回调可能让目标瞬移或改变体型，潮爆仍以本次接触时的位置结算。
        Vec3 impactCenter = directHit.position().add(0, directHit.getBbHeight() * 0.5, 0);
        double impactSpeed = this.getDeltaMovement().length();

        double originalBaseDamage = this.getBaseDamage();
        if (directHit.isInWater()) {
            this.setBaseDamage(originalBaseDamage * ServerConfig.TIDE_BOLT_WET_DAMAGE_MULTIPLIER.get());
        }
        super.onHitEntity(result);
        this.setBaseDamage(originalBaseDamage);
        this.tideBurst(impactCenter, directHit, impactSpeed);
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {

        double impactSpeed = this.getDeltaMovement().length();
        super.onHitBlock(result);
        this.tideBurst(result.getLocation(), null, impactSpeed);
    }

    /** 潮爆:命中点水压炸裂,范围伤害 + 径向冲开 + 水花环演出。 */
    private void tideBurst(Vec3 center, @Nullable Entity directHit,
                           double impactSpeed) {
        if (this.burstDone || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.burstDone = true;
        double burstRadius = ServerConfig.TIDE_BOLT_BURST_RADIUS.get();
        double radiusSqr = burstRadius * burstRadius;
        float burstDamage = (float) (this.getBaseDamage() * impactSpeed
                * ServerConfig.TIDE_BOLT_BURST_DAMAGE_FACTOR.get());
        burstDamage = Math.max(2.0F, Math.min(burstDamage, 6.5F));
        var owner = this.getOwner();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(burstRadius),
                e -> validBurstTarget(e, owner, directHit, serverLevel, center, radiusSqr))) {
            if (!validBurstTarget(victim, owner, directHit, serverLevel, center, radiusSqr)) {
                continue;
            }
            boolean hurt = victim.hurt(owner instanceof LivingEntity living
                            ? this.damageSources().mobProjectile(this, living)
                            : this.damageSources().generic(),
                    burstDamage);
            if (!hurt || !validBurstTarget(victim, owner, directHit, serverLevel, center, radiusSqr)
                    || isBoss(victim)) {
                continue;
            }
            Vec3 away = victim.position().subtract(center).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 dir = away.normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.6, 0.25, dir.z * 0.6));
                victim.hurtMarked = true;
            }
        }

        WeaponProjectileFx.tideImpact(serverLevel, center, burstRadius);
        serverLevel.playSound(null, BlockPos.containing(center),
                ModSounds.PROJECTILE_TIDE_BOLT_BURST.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean validBurstTarget(LivingEntity victim, @Nullable Entity owner,
                                           @Nullable Entity directHit, ServerLevel level,
                                           Vec3 center, double radiusSqr) {
        return victim.level() == level && victim != owner && victim != directHit
                && victim instanceof Enemy && victim.isAlive() && victim.distanceToSqr(center) <= radiusSqr
                && !(owner instanceof LivingEntity living && victim.isAlliedTo(living));
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        applyAmmoEffects(target);
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !(this.getOwner() instanceof ServerPlayer player)
                || !(target instanceof Enemy)
                || isBoss(target)) {
            return;
        }
        long now = serverLevel.getGameTime();
        Long nextSearch = NEXT_MARK_SEARCH.get(player.getUUID());
        long cooldown = ServerConfig.TIDE_BOLT_MARK_COOLDOWN_TICKS.get();
        long maximumNextSearch = now > Long.MAX_VALUE - cooldown
                ? Long.MAX_VALUE : now + cooldown;
        if (nextSearch != null && nextSearch > maximumNextSearch) {
            nextSearch = maximumNextSearch;
            NEXT_MARK_SEARCH.put(player.getUUID(), nextSearch);
        }
        if (nextSearch != null && now < nextSearch) {
            return;
        }
        NEXT_MARK_SEARCH.put(player.getUUID(), maximumNextSearch);
        findMarkRune(serverLevel, target.blockPosition(), ServerConfig.TIDE_BOLT_RUNE_SEARCH_RADIUS.get())
                .ifPresent(runePos -> {
                    Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 dir = Vec3.atCenterOf(runePos).subtract(from).normalize();

                    int steps = cn.kurt6.unknown_echoes.ability.EchoArmorSets
                            .hasListenerClueBonus(player) ? 14 : 8;
                    for (int i = 1; i <= steps; i++) {
                        Vec3 p = from.add(dir.scale(0.8 * i));
                        serverLevel.sendParticles(ParticleTypes.GLOW,
                                p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                });
    }

    private static Optional<BlockPos> findMarkRune(ServerLevel level, BlockPos origin, int radius) {
        int minChunkX = (origin.getX() - radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int chunksX = ((origin.getX() + radius) >> 4) - minChunkX + 1;
        int chunksZ = ((origin.getZ() + radius) >> 4) - minChunkZ + 1;
        boolean[] checkedChunks = new boolean[chunksX * chunksZ];
        LevelChunk[] loadedChunks = new LevelChunk[checkedChunks.length];
        // 沿用原版搜索顺序；加载状态只在本次同步搜索中复用。
        return BlockPos.findClosestMatch(origin, radius, 8, pos -> {
            if (level.isOutsideBuildHeight(pos)) {
                return false;
            }
            int index = ((pos.getX() >> 4) - minChunkX) * chunksZ + (pos.getZ() >> 4) - minChunkZ;
            if (!checkedChunks[index]) {
                loadedChunks[index] = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                checkedChunks[index] = true;
            }
            return loadedChunks[index] != null && loadedChunks[index].getBlockState(pos).is(ModBlocks.TIDE_RUNE.get());
        });
    }

    private void applyAmmoEffects(LivingEntity target) {
        ItemStack ammo = this.getPickupItemStackOrigin();
        if (ammo.is(Items.SPECTRAL_ARROW)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0),
                    this.getEffectSource());
        }

        PotionContents potionContents = ammo.getOrDefault(
                DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        potionContents.potion().ifPresent(potion -> {
            for (MobEffectInstance effect : potion.value().getEffects()) {
                target.addEffect(new MobEffectInstance(
                                effect.getEffect(),
                                Math.max(effect.mapDuration(duration -> duration / 8), 1),
                                effect.getAmplifier(), effect.isAmbient(), effect.isVisible()),
                        this.getEffectSource());
            }
        });
        for (MobEffectInstance effect : potionContents.customEffects()) {
            target.addEffect(effect, this.getEffectSource());
        }
    }

    private static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModEntityTags.ECHO_BOSSES)
                || target.getType().is(Tags.EntityTypes.BOSSES);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BurstDone", this.burstDone);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.burstDone = tag.getBoolean("BurstDone");
    }
}
