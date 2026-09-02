package cn.kurt6.unknown_echoes.item.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 晶歌法杖：右键发射音波，命中首个敌怪造成远程伤害与缓速。 */
public class CrystalSongStaffItem extends EchoMeleeWeaponItem implements WeaponSkill {
    private static final int COOLDOWN_TICKS = 40;
    private static final int SECONDARY_COOLDOWN_TICKS = 220;
    private static final double RANGE = 14.0;
    private static final float BOLT_DAMAGE = 12.0F;
    private static final float SECONDARY_DAMAGE = 11.0F;

    public CrystalSongStaffItem(double damage, double speed, String tooltipKey, Properties properties) {
        super(damage, speed, tooltipKey, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            this.activateSkill(serverLevel, serverPlayer, stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_crystal_song_staff_primary_until", COOLDOWN_TICKS, 0)) {
            return false;
        }
        fireSonicWave(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 1);
        return true;
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        if (!WeaponSkillSupport.begin(player,
                "ue_weapon_skill_crystal_song_staff_secondary_until", SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        for (LivingEntity target : WeaponSkillSupport.radiusTargets(level, player, 7.0D, 8)) {
            if (!WeaponSkillSupport.hurt(player, target, SECONDARY_DAMAGE)) {
                continue;
            }
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        100, 1, true, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        60, 0, true, false, true));
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.CRYSTAL_SONG_STAFF);
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.CRYSTAL_SONG_STAFF, 7.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.1F, 0.8F);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    /** 瞬发音波射线:沿视线推进,被实体方块截断,命中首个敌怪结算伤害 + 缓速。 */
    private void fireSonicWave(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(dir.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? RANGE : eye.distanceTo(block.getLocation());

        if (block.getType() == HitResult.Type.BLOCK
                && level.getBlockState(block.getBlockPos())
                        .is(cn.kurt6.unknown_echoes.registry.ModBlocks.CRYSTAL_SONG_CLUSTER.get())) {
            for (var keeper : level.getEntitiesOfClass(
                    cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.class,
                    new AABB(block.getBlockPos()).inflate(
                            cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.TUNE_RANGE),
                    LivingEntity::isAlive)) {
                keeper.onTonePillarClicked(block.getBlockPos(), player);
                break;
            }
        }

        WeaponFx.pierceBeam(level, player, WeaponFxTheme.CRYSTAL_SONG_STAFF, eye, dir, reach);

        LivingEntity hit = null;
        double bestAlong = reach;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(1.0),
                e -> e != player && e.isAlive() && e instanceof Enemy
                        && !e.isAlliedTo(player))) {
            Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eye);
            double along = toEntity.dot(dir);
            if (along < 0.5 || along > reach) {
                continue;
            }
            if (toEntity.subtract(dir.scale(along)).length() > entity.getBbWidth() * 0.5 + 0.6) {
                continue;
            }
            if (along < bestAlong) {
                bestAlong = along;
                hit = entity;
            }
        }

        Vec3 impact = eye.add(dir.scale(Math.min(bestAlong, reach)));
        if (hit != null) {
            LivingEntity victim = hit;

            boolean[] hurt = {false};
            WeaponEffectGuard.run(player,
                    () -> hurt[0] = victim.hurt(
                            player.damageSources().playerAttack(player), BOLT_DAMAGE));
            if (hurt[0]) {
                WeaponFx.hitMark(level, victim, WeaponFxTheme.CRYSTAL_SONG_STAFF);
                if (!isBoss(victim)) {
                    victim.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
                }
            }
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(impact),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
    }
}
