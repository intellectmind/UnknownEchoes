package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.trade.EchoTradeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 迷途旅者(9.2/16.4):仍残留聆者血脉的流浪者——听得见,但听不懂。
 * 非任务型中立 NPC:给出模糊提示、出售少量特殊物品;不发布任务,不出售任何关键进度物。
 * 交易入口走自绘交易 Screen(5.8):右键由服务端下发交易表,不打开原版 Merchant。
 * 营地随时间更换(4.2.1):驻留时间到后离开；事件不放置任何世界方块。
 */
public class LostTraveler extends PathfinderMob implements GeoEntity {

    private static final int MAX_HORIZONTAL_COORDINATE = 30_000_000;
    private static final int MAX_ANCHOR_DISTANCE_SQR = 32 * 32;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.lost_traveler.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.lost_traveler.walk");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 临时驻留锚点；只用于限制活动范围，不对应世界方块。 */
    private BlockPos stayAnchorPos;
    /** 驻留截止 gameTime;0=常驻(刷怪蛋/指令生成的旅者不自动离开)。 */
    private long stayUntil;

    public LostTraveler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** 开始临时驻留：限制活动范围并启动离场计时。 */
    public void startTemporaryStay(BlockPos anchorPos, long gameTime) {
        this.stayAnchorPos = anchorPos;
        long duration = ServerConfig.LOST_TRAVELER_STAY_TICKS.get();
        this.stayUntil = duration > Long.MAX_VALUE - gameTime ? Long.MAX_VALUE : gameTime + duration;
        this.restrictTo(anchorPos, 12);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.stayUntil <= 0
                || this.level().getGameTime() < this.stayUntil) {
            return;
        }
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.CAMPFIRE_CRACKLE, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 0.8F);
        this.discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && this.isAlive()) {
            // 模糊提示(9.2 示例台词风格)+ 下发交易表,客户端打开总览交易页
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.lost_traveler.mutter."
                            + this.random.nextInt(4)).withStyle(net.minecraft.ChatFormatting.GRAY,
                    net.minecraft.ChatFormatting.ITALIC));
            EchoTradeManager.openTrades(serverPlayer, this);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.stayAnchorPos != null) {
            // 保留旧键名，确保已有世界里的旅者仍能恢复驻留锚点。
            tag.put("CampPos", NbtUtils.writeBlockPos(this.stayAnchorPos));
        }
        tag.putLong("StayUntil", this.stayUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stayAnchorPos = null;
        this.clearRestriction();
        NbtUtils.readBlockPos(tag, "CampPos").filter(this::isValidStayAnchor)
                .ifPresent(pos -> this.stayAnchorPos = pos);
        long savedStayUntil = tag.getLong("StayUntil");
        long now = this.level().getGameTime();
        long duration = ServerConfig.LOST_TRAVELER_STAY_TICKS.get();
        long maxStayUntil = duration > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
        this.stayUntil = savedStayUntil <= 0L ? 0L : Math.min(savedStayUntil, maxStayUntil);
        if (this.stayAnchorPos != null) {
            this.restrictTo(this.stayAnchorPos, 12);
        }
    }

    private boolean isValidStayAnchor(BlockPos pos) {
        return Math.abs((long) pos.getX()) <= MAX_HORIZONTAL_COORDINATE
                && Math.abs((long) pos.getZ()) <= MAX_HORIZONTAL_COORDINATE
                && pos.getY() >= -2048 && pos.getY() <= 2047
                && pos.distSqr(this.blockPosition()) <= MAX_ANCHOR_DISTANCE_SQR;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            EchoTradeManager.clearUses(this.getUUID());
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
