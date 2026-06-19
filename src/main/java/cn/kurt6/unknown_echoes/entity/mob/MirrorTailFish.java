package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * 镜尾鱼:镜湖水域环境鱼群(生命 3)。
 * 鱼群绕真符印游动——倒影层的弱线索表现(九章 9.1 / 红线 #9):
 * 服务端 AI 知道真符印位置并驱动绕游;客户端只收到鱼的位置同步,
 * REAL 标记永不下发,线索只能靠观察行为获得。
 * 掉落镜尾鱼(食物,食用后短暂提升水下视野)。
 */
public class MirrorTailFish extends AbstractSchoolingFish implements GeoEntity {
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_tail_fish.swim");
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_tail_fish.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MirrorTailFish(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSchoolingFish.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 优先级高于随机游动(4),低于逃跑(2):有真符印时倾向绕游
        this.goalSelector.addGoal(3, new CircleRealSigilGoal(this));
    }

    @Override
    public int getMaxSchoolSize() {
        return 6;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.MIRROR_TAIL_FISH_BUCKET.get());
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    // ---- GeckoLib 动画 ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (this.isInWater()) {
                return state.setAndContinue(SWIM_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /**
     * 绕真符印游动:周期性低频扫描附近镜像符印,只锁定服务端判定为 REAL 的;
     * 之后沿其周围一圈水中路径点巡游。假符印永远不会成为圆心,
     * 玩家观察"哪面墙有鱼群打转"即可获得线索,抓包读不出任何答案。
     */
    static class CircleRealSigilGoal extends Goal {
        /** 扫描半径(水平);倒影回廊符印墙间距以内,体积小避免高频开销。 */
        private static final int SCAN_RADIUS_H = 8;
        private static final int SCAN_RADIUS_V = 4;
        /** 两次扫描之间的最小间隔 tick(canUse 失败后)。 */
        private static final int SCAN_INTERVAL = 200;

        private final MirrorTailFish fish;
        private BlockPos sigilPos;
        private int circleTicks;
        private double angle;
        private int scanCooldown;

        CircleRealSigilGoal(MirrorTailFish fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.fish.isInWater() || this.fish.level().isClientSide) {
                return false;
            }
            if (this.scanCooldown > 0) {
                --this.scanCooldown;
                return false;
            }
            this.scanCooldown = SCAN_INTERVAL + this.fish.getRandom().nextInt(100);
            this.sigilPos = findNearbyRealSigil();
            return this.sigilPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.circleTicks > 0 && this.sigilPos != null && this.fish.isInWater();
        }

        @Override
        public void start() {
            this.circleTicks = 300 + this.fish.getRandom().nextInt(200);
            this.angle = this.fish.getRandom().nextDouble() * Math.PI * 2;
        }

        @Override
        public void stop() {
            this.sigilPos = null;
        }

        @Override
        public void tick() {
            --this.circleTicks;
            if (this.fish.getNavigation().isDone()) {
                // 下一个圆周路径点:半径 2.5 格,逐步推进角度
                this.angle += 0.9D;
                double r = 2.5D;
                double x = this.sigilPos.getX() + 0.5D + Math.cos(this.angle) * r;
                double z = this.sigilPos.getZ() + 0.5D + Math.sin(this.angle) * r;
                double y = this.sigilPos.getY() + 0.5D
                        + Mth.clamp(this.fish.getRandom().nextDouble() - 0.5D, -0.4D, 0.4D);
                this.fish.getNavigation().moveTo(x, y, z, 1.0D);
            }
        }

        /** 服务端扫描:找最近的真符印;状态读 BlockEntity,不碰客户端可见数据。 */
        private BlockPos findNearbyRealSigil() {
            ServerLevel level = (ServerLevel) this.fish.level();
            BlockPos center = this.fish.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(
                    center.offset(-SCAN_RADIUS_H, -SCAN_RADIUS_V, -SCAN_RADIUS_H),
                    center.offset(SCAN_RADIUS_H, SCAN_RADIUS_V, SCAN_RADIUS_H))) {
                if (!level.getBlockState(pos).is(ModBlocks.MIRROR_SIGIL.get())) {
                    continue;
                }
                if (!MirrorSigilBlockEntity.isReal(level, pos)) {
                    continue;
                }
                double dist = pos.distSqr(center);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = pos.immutable();
                }
            }
            return best;
        }
    }
}
