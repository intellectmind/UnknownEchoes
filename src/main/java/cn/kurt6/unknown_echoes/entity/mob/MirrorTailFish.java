package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/** 镜尾鱼:镜湖水域环境鱼群(生命 3)。 */
public class MirrorTailFish extends EchoSchoolingFish implements GeoEntity {
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_tail_fish.swim");
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.mirror_tail_fish.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MirrorTailFish(EntityType<? extends EchoSchoolingFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSchoolingFish.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(3, new CircleRealSigilGoal(this));
    }

    @Override
    public int getMaxSchoolSize() {
        return 6;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.MIRROR_TAIL_FISH_FLOP.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIRROR_TAIL_FISH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIRROR_TAIL_FISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIRROR_TAIL_FISH_DEATH.get();
    }

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
     * 绕真符印游动:低频扫描附近镜像符印,只锁定服务端判定为 REAL 的,再沿它周围一圈水中路径点巡游。
     * 假符印永远不会成为圆心,所以玩家观察"哪面墙有鱼群打转"就能拿到线索,而抓包读不出答案。
     */
    static class CircleRealSigilGoal extends Goal {
        /** 扫描半径(水平);倒影回廊符印墙间距以内,体积小避免高频开销。 */
        private static final int SCAN_RADIUS_H = 8;
        private static final int SCAN_RADIUS_V = 4;
        /** 两次扫描之间的最小间隔 tick(canUse 失败后)。 */
        private static final int SCAN_INTERVAL = 200;
        private static final int MOVE_RETRY_TICKS = 20;

        private final MirrorTailFish fish;
        private BlockPos sigilPos;
        private ServerLevel sigilLevel;
        private Path circlePath;
        private int circleTicks;
        private double angle;
        private int scanCooldown;
        private int nextMoveAttemptTick;

        CircleRealSigilGoal(MirrorTailFish fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(this.fish.level() instanceof ServerLevel serverLevel)
                    || !this.fish.isAlive() || this.fish.isNoAi() || !this.fish.isInWater()) {
                return false;
            }
            if (this.scanCooldown > 0) {
                --this.scanCooldown;
                return false;
            }
            this.scanCooldown = SCAN_INTERVAL + this.fish.getRandom().nextInt(100);
            this.sigilPos = findNearbyRealSigil();
            this.sigilLevel = this.sigilPos != null ? serverLevel : null;
            return this.sigilPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.circleTicks > 0 && this.sigilPos != null && this.sigilLevel != null
                    && this.fish.level() == this.sigilLevel && this.fish.isAlive()
                    && !this.fish.isNoAi() && this.fish.isInWater()
                    && MirrorSigilBlockEntity.isReal(this.sigilLevel, this.sigilPos);
        }

        @Override
        public void start() {
            this.circleTicks = 300 + this.fish.getRandom().nextInt(200);
            this.angle = this.fish.getRandom().nextDouble() * Math.PI * 2;
            this.circlePath = null;
            this.nextMoveAttemptTick = 0;
        }

        @Override
        public void stop() {
            if (this.circlePath != null && this.fish.getNavigation().getPath() == this.circlePath) {
                this.fish.getNavigation().stop();
            }
            this.circlePath = null;
            this.circleTicks = 0;
            this.sigilPos = null;
            this.sigilLevel = null;
        }

        @Override
        public void tick() {
            // 仅更新运行中 Goal 的轮次不会调用 canContinueToUse。
            if (!this.canContinueToUse()) {
                this.stop();
                return;
            }
            if (--this.circleTicks <= 0) {
                this.stop();
                return;
            }
            if (this.fish.getNavigation().isDone() && this.fish.tickCount >= this.nextMoveAttemptTick) {

                this.angle += 0.9D;
                double r = 2.5D;
                double x = this.sigilPos.getX() + 0.5D + Math.cos(this.angle) * r;
                double z = this.sigilPos.getZ() + 0.5D + Math.sin(this.angle) * r;
                double y = this.sigilPos.getY() + 0.5D
                        + Mth.clamp(this.fish.getRandom().nextDouble() - 0.5D, -0.4D, 0.4D);
                if (this.fish.getNavigation().moveTo(x, y, z, 1.0D)) {
                    this.circlePath = this.fish.getNavigation().getPath();
                    this.nextMoveAttemptTick = 0;
                } else {
                    this.circlePath = null;
                    this.nextMoveAttemptTick = this.fish.tickCount + MOVE_RETRY_TICKS;
                }
            }
        }

        /** 服务端扫描:找最近的真符印;状态读 BlockEntity,不碰客户端可见数据。 */
        private BlockPos findNearbyRealSigil() {
            ServerLevel level = (ServerLevel) this.fish.level();
            BlockPos center = this.fish.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            int minChunkX = (center.getX() - SCAN_RADIUS_H) >> 4;
            int maxChunkX = (center.getX() + SCAN_RADIUS_H) >> 4;
            int minChunkZ = (center.getZ() - SCAN_RADIUS_H) >> 4;
            int maxChunkZ = (center.getZ() + SCAN_RADIUS_H) >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    for (var blockEntity : java.util.List.copyOf(chunk.getBlockEntities().values())) {
                        if (!(blockEntity instanceof MirrorSigilBlockEntity)) {
                            continue;
                        }
                        BlockPos pos = blockEntity.getBlockPos();
                        if (Math.abs((long) pos.getX() - center.getX()) > SCAN_RADIUS_H
                                || Math.abs((long) pos.getY() - center.getY()) > SCAN_RADIUS_V
                                || Math.abs((long) pos.getZ() - center.getZ()) > SCAN_RADIUS_H
                                || !MirrorSigilBlockEntity.isReal(level, pos)) {
                            continue;
                        }
                        double dist = pos.distSqr(center);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos.immutable();
                        }
                    }
                }
            }
            return best;
        }
    }
}
