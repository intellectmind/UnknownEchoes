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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 * 营地随时间更换(4.2.1):驻留时间到后熄灭营地篝火并离开,旧营地只留下熄灭的篝火。
 */
public class LostTraveler extends PathfinderMob implements GeoEntity {

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.lost_traveler.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.lost_traveler.walk");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 营地锚点(篝火位置);离开时把它熄灭。 */
    private BlockPos campPos;
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

    /** 设置营地:锚定篝火附近活动,驻留计时开始(WorldEventManager 失落商队事件调用)。 */
    public void setCamp(BlockPos campfirePos, long gameTime) {
        this.campPos = campfirePos;
        this.stayUntil = gameTime + ServerConfig.LOST_TRAVELER_STAY_TICKS.get();
        this.restrictTo(campfirePos, 12);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.stayUntil <= 0
                || this.level().getGameTime() < this.stayUntil) {
            return;
        }
        // 驻留时间到:熄灭营地篝火(旧营地留下熄灭的篝火,4.2.1),旅者悄然离开
        if (this.campPos != null) {
            BlockState state = this.level().getBlockState(this.campPos);
            if (state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
                this.level().setBlock(this.campPos,
                        state.setValue(CampfireBlock.LIT, Boolean.FALSE), 3);
            }
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
        if (this.campPos != null) {
            tag.put("CampPos", NbtUtils.writeBlockPos(this.campPos));
        }
        tag.putLong("StayUntil", this.stayUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "CampPos").ifPresent(pos -> this.campPos = pos);
        this.stayUntil = tag.getLong("StayUntil");
        if (this.campPos != null) {
            this.restrictTo(this.campPos, 12);
        }
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
