package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.AnimalHarvest;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** 苔背龟:失语沼泽浅水被动生物(生命 20,高护甲)。 */
public class MossBackTurtle extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.moss_back_turtle.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.moss_back_turtle.walk");
    private static final RawAnimation HIDE_ANIM =
            RawAnimation.begin().thenLoop("animation.moss_back_turtle.hide");

    /** 缩壳状态同步客户端(只用于播放缩壳动画,减伤判定在服务端)。 */
    private static final EntityDataAccessor<Boolean> HIDING =
            SynchedEntityData.defineId(MossBackTurtle.class, EntityDataSerializers.BOOLEAN);

    /** 缩壳持续时间(tick)。 */
    private static final int HIDE_TICKS = 100;
    private static final float HIDING_DAMAGE_FACTOR = 0.3F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int hideTicks;
    /** 剪取噤声苔的冷却(tick),归零后苔藓长回。 */
    private int shearCooldown;

    public MossBackTurtle(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDING, false);
    }

    /**
     * 不加 PanicGoal:受击后的正解是缩壳减伤(见 hurt),逃跑会与 hideTicks 期间的 stop 导航打架。
     * 躲避只在还没被打到时生效:缩壳中 hideTicks 每 tick stop 导航,躲避拿不到路径自然让位。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeThreatGoal(this, 8.0D, 0.9D, 1.1D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D,
                stack -> stack.is(Items.SEAGRASS), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public boolean isHiding() {
        return this.entityData.get(HIDING);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (!this.level().isClientSide && this.isHiding()) {
            amount *= HIDING_DAMAGE_FACTOR;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            this.hideTicks = HIDE_TICKS;
            this.entityData.set(HIDING, true);
            this.getNavigation().stop();
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.shearCooldown > 0) {
            --this.shearCooldown;
        }
        if (this.hideTicks > 0) {
            --this.hideTicks;
            this.getNavigation().stop();
            if (this.hideTicks == 0) {
                this.entityData.set(HIDING, false);
            }
        }
    }

    /** 剪刀采集噤声苔:冷却就绪时无伤掉落噤声苔×1;冷却中只给含蓄提示。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        Level interactionLevel = this.level();
        if (!interactionLevel.isClientSide && !AnimalHarvest.canInteract(interactionLevel, this, player)) {
            return InteractionResult.CONSUME;
        }
        ItemStack stack = player.getItemInHand(hand);
        if ((stack.is(Items.SHEARS) || stack.is(ModItems.ECHO_SHEARS.get())) && !this.isBaby()) {
            if (!this.level().isClientSide) {
                if (this.shearCooldown <= 0) {
                    boolean echoShears = stack.is(ModItems.ECHO_SHEARS.get());
                    this.shearCooldown = ServerConfig.MOSS_TURTLE_SHEAR_COOLDOWN.get();
                    stack.hurtAndBreak(1, player, getSlotForHand(hand));
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    this.spawnAtLocation(ModItems.SILENCE_MOSS.get());
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    if (echoShears) {
                        this.spawnAtLocation(ModItems.SILENCE_MOSS.get());
                    }
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.moss_back_turtle.not_ready"), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShearCooldown", this.shearCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.shearCooldown = Math.max(0, Math.min(tag.getInt("ShearCooldown"),
                ServerConfig.MOSS_TURTLE_SHEAR_COOLDOWN.get()));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SEAGRASS);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MOSS_BACK_TURTLE.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MOSS_BACK_TURTLE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOSS_BACK_TURTLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOSS_BACK_TURTLE_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isHiding()) {
                return state.setAndContinue(HIDE_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
