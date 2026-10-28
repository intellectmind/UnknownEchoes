package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

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
        // 技能耐久或损坏回调可能更换手持物，按当前栈完成原版使用收尾。
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!isCurrentSkillActor(level, player) || !WeaponSkillSupport.begin(player,
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
        if (!isCurrentSkillActor(level, player) || !WeaponSkillSupport.begin(player,
                "ue_weapon_skill_crystal_song_staff_secondary_until", SECONDARY_COOLDOWN_TICKS, 0)) {
            return false;
        }
        castSecondary(level, player);
        WeaponSkillSupport.finish(player, stack, hand, 3);
        return true;
    }

    private void castSecondary(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.CRYSTAL_SONG_STAFF;
        double radius = 7.0D;
        Vec3 center = player.position();
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : radiusSkillTargets(level, player, center, bounds, radius, 8)) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            boolean hurt = WeaponSkillSupport.hurt(player, target, SECONDARY_DAMAGE);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!hurt || !isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        100, 1, true, false, true));
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
            }
            if (!isCurrentRadiusTarget(level, player, target, center, bounds, radius)) {
                continue;
            }
            WeaponFx.hitMark(level, target, WeaponFxTheme.CRYSTAL_SONG_STAFF);
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.groundRing(level, center, fx, radius);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        level.playSound(null, BlockPos.containing(center), ModSounds.WEAPON_CRYSTAL_SONG_STAFF_SECONDARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 瞬发音波射线:沿视线推进,被实体方块截断,命中首个敌怪结算伤害 + 缓速。 */
    private void fireSonicWave(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(dir.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? RANGE : eye.distanceTo(block.getLocation());
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        if (block.getType() == HitResult.Type.BLOCK) {
            clickTonePillar(level, player, block.getBlockPos());
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }

        WeaponFx.pierceBeam(level, player, WeaponFxTheme.CRYSTAL_SONG_STAFF, eye, dir, reach);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }

        LivingEntity hit = null;
        double bestAlong = reach;
        AABB bounds = new AABB(eye, end).inflate(1.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                bounds, e -> isCurrentSonicTarget(level, player, e, bounds, eye, dir, reach))) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentSonicTarget(level, player, entity, bounds, eye, dir, reach)) {
                continue;
            }
            double along = entity.getBoundingBox().getCenter().subtract(eye).dot(dir);
            if (along < bestAlong) {
                bestAlong = along;
                hit = entity;
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        Vec3 impact = eye.add(dir.scale(Math.min(bestAlong, reach)));
        if (hit != null) {
            LivingEntity victim = hit;
            boolean hurt = WeaponSkillSupport.hurt(player, victim, BOLT_DAMAGE);
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (hurt && isCurrentSonicTarget(level, player, victim, bounds, eye, dir, reach)) {
                WeaponFx.hitMark(level, victim, WeaponFxTheme.CRYSTAL_SONG_STAFF);
                if (isCurrentSonicTarget(level, player, victim, bounds, eye, dir, reach) && !isBoss(victim)) {
                    victim.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
                }
            }
        }
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        WeaponFx.releaseBurst(level, impact, WeaponFxTheme.CRYSTAL_SONG_STAFF, .75D);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        // 末端可能超出可听距离；施法者只收近身反馈，旁观者仍听末端定位声。
        level.playSound(player, impact.x, impact.y, impact.z,
                ModSounds.WEAPON_CRYSTAL_SONG_STAFF_PRIMARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!isCurrentSkillActor(level, player)) {
            return;
        }
        player.playNotifySound(ModSounds.WEAPON_CRYSTAL_SONG_STAFF_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void clickTonePillar(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())
                || !isCurrentSkillActor(level, player)) {
            return;
        }
        AABB bounds = new AABB(pos).inflate(CrystalSongkeeper.TUNE_RANGE);
        for (CrystalSongkeeper keeper : level.getEntitiesOfClass(CrystalSongkeeper.class, bounds,
                candidate -> isCurrentToneKeeper(level, player, candidate, bounds))) {
            if (!isCurrentSkillActor(level, player)) {
                return;
            }
            if (!isCurrentToneKeeper(level, player, keeper, bounds)) {
                continue;
            }
            // 查询和方块读取扩展可能改变现场；只向当前音柱的有效接收者派发一次。
            if (!level.getBlockState(pos).is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())) {
                return;
            }
            if (isCurrentToneKeeper(level, player, keeper, bounds)) {
                keeper.onTonePillarClicked(pos, player);
                return;
            }
        }
    }

    private static boolean isCurrentSkillActor(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.hasDisconnected() && !player.isSpectator();
    }

    private static boolean isCurrentToneKeeper(ServerLevel level, ServerPlayer player,
                                                CrystalSongkeeper keeper, AABB bounds) {
        return isCurrentSkillActor(level, player) && keeper.level() == level && keeper.isAlive() && !keeper.isSpectator()
                && keeper.getBoundingBox().intersects(bounds);
    }

    private static boolean isCurrentSonicTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                 AABB bounds, Vec3 eye, Vec3 dir, double reach) {
        if (!(isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.getBoundingBox().intersects(bounds))) {
            return false;
        }
        Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
        double along = offset.dot(dir);
        return along >= 0.5D && along <= reach
                && offset.subtract(dir.scale(along)).length() <= target.getBbWidth() * 0.5D + 0.6D;
    }

    private static List<LivingEntity> radiusSkillTargets(ServerLevel level, ServerPlayer player,
                                                         Vec3 center, AABB bounds, double radius, int limit) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        target -> isCurrentRadiusTarget(level, player, target, center, bounds, radius))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)))
                .limit(limit)
                .toList();
    }

    private static boolean isCurrentRadiusTarget(ServerLevel level, ServerPlayer player, LivingEntity target,
                                                   Vec3 center, AABB bounds, double radius) {
        return isCurrentSkillActor(level, player) && target.level() == level && WeaponSkillSupport.isHostile(player, target)
                && target.position().distanceToSqr(center) <= radius * radius
                && target.getBoundingBox().intersects(bounds);
    }
}
