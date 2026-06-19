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

/**
 * 晶歌法杖:在通用近战基础上实现设计文档(重设计 §武器技能)规定的"音波远程"——
 * 右键发射一道瞬发音波,命中首个敌怪造成远程伤害 + 缓速,沿途音符轨迹与晶钟音。
 * 近战命中仍保留父类的减速+晶钟效果(CommonEvents 按 EchoMeleeWeaponItem 触发,子类同样命中)。
 *
 * 注:不绕过 Boss 破防/权限/机关(红线 §703)。音波命中晶歌音柱(CRYSTAL_SONG_CLUSTER)时,
 * 由就近的晶歌守谱者在服务端按节奏谜题判定"远程调音"(见 CrystalSongkeeper.onTonePillarClicked)。
 */
public class CrystalSongStaffItem extends EchoMeleeWeaponItem {
    private static final int COOLDOWN_TICKS = 22;
    private static final double RANGE = 14.0;
    private static final float BOLT_DAMAGE = 5.0F;

    public CrystalSongStaffItem(double damage, double speed, String tooltipKey, Properties properties) {
        super(damage, speed, tooltipKey, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            fireSonicWave(serverLevel, serverPlayer);
        }
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    /** 瞬发音波射线:沿视线推进,被实体方块截断,命中首个敌怪结算伤害 + 缓速。 */
    private void fireSonicWave(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(dir.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? RANGE : eye.distanceTo(block.getLocation());

        // 远程调音:音波命中晶歌音柱 → 就近守谱者按节奏谜题判定(实现文档 §7.4/§5.3 的"远程调音")
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

        // 音符轨迹
        for (double d = 1.0; d <= reach; d += 0.6) {
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(ParticleTypes.NOTE, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.6);
        }

        // 沿射线找最近的敌怪
        LivingEntity hit = null;
        double bestAlong = reach;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(1.0),
                e -> e != player && e.isAlive() && e instanceof Enemy)) {
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
            // WeaponEffectGuard:避免远程音波再触发法杖自身的近战命中事件(递归)
            WeaponEffectGuard.run(player,
                    () -> victim.hurt(player.damageSources().playerAttack(player), BOLT_DAMAGE));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.NOTE, impact.x, impact.y, impact.z, 8, 0.3, 0.3, 0.3, 0.8);
        level.playSound(null, BlockPos.containing(impact),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
    }
}
