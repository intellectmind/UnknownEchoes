package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.entity.AnimalHarvest;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.ai.FleeThreatGoal;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** 晶羽鸟:漂浮群岛被动飞行生物(生命 8)。 */
public class CrystalFeatherBird extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.crystal_feather_bird.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.crystal_feather_bird.walk");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.crystal_feather_bird.fly");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 喂食荧光花的冷却(tick),归零后才能再次喂出晶羽。 */
    private int feedCooldown;

    public CrystalFeatherBird(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.6D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** 躲避排在 PanicGoal 之后、繁殖之前:被打到的反应优先,但要在敌怪贴上来之前就走开。 */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new FleeThreatGoal(this, 12.0D, 1.15D, 1.45D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D,
                stack -> stack.is(Items.WHEAT_SEEDS) || stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0D) {
            this.setDeltaMovement(movement.multiply(1.0D, 0.6D, 1.0D));
        }
        if (!this.level().isClientSide && this.feedCooldown > 0) {
            --this.feedCooldown;
        }
    }

    /** 喂食荧光花:冷却就绪时无伤获得晶羽×1;冷却中给含蓄提示。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        Level interactionLevel = this.level();
        if (!interactionLevel.isClientSide && !AnimalHarvest.canInteract(interactionLevel, this, player)) {
            return InteractionResult.CONSUME;
        }
        ItemStack stack = player.getItemInHand(hand);
        if ((stack.is(ModItems.ECHO_FLOWER.get()) || stack.is(ModItems.ECHO_SHEARS.get())) && !this.isBaby()) {
            if (!this.level().isClientSide) {
                if (this.feedCooldown <= 0) {
                    boolean echoShears = stack.is(ModItems.ECHO_SHEARS.get());
                    // 先占用本只生物的冷却，耐久或损坏回调重入时不能再次采集。
                    this.feedCooldown = ServerConfig.CRYSTAL_FEATHER_FEED_COOLDOWN.get();
                    if (echoShears) {
                        stack.hurtAndBreak(1, player, getSlotForHand(hand));
                    } else {
                        stack.consume(1, player);
                    }
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    this.playSound(ModSounds.CRYSTAL_FEATHER_BIRD_SONG.get(), 0.8F, 1.0F);
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    this.spawnAtLocation(ModItems.CRYSTAL_FEATHER.get());
                    if (!AnimalHarvest.canHarvest(interactionLevel, this, player)) {
                        return InteractionResult.CONSUME;
                    }
                    if (echoShears) {
                        this.spawnAtLocation(ModItems.CRYSTAL_FEATHER.get());
                    }
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.crystal_feather_bird.not_ready"), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FeedCooldown", this.feedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.feedCooldown = Math.max(0, Math.min(tag.getInt("FeedCooldown"),
                ServerConfig.CRYSTAL_FEATHER_FEED_COOLDOWN.get()));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CRYSTAL_FEATHER_BIRD.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRYSTAL_FEATHER_BIRD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRYSTAL_FEATHER_BIRD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRYSTAL_FEATHER_BIRD_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (!this.onGround()) {
                return state.setAndContinue(FLY_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIM : IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
