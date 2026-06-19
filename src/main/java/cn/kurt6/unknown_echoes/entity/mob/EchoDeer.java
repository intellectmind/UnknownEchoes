package cn.kurt6.unknown_echoes.entity.mob;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * 回声鹿:回响森林被动生物(生命 14)。
 * 受击逃跑时短暂留下回响轨迹粒子——指向最近已生成遗迹方向的弱线索表现,
 * 不显示坐标、不写日志、不参与任何进度判定(九章 9.1 设定基线)。
 * 掉落回响纤维(探索者套装材料)。设定:它们仍循着回声巡视早已无人的归途。
 */
public class EchoDeer extends Animal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_deer.idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_deer.walk");

    /** 回响轨迹指向的遗迹集合(数据包可调);只取方向,不暴露坐标。 */
    private static final TagKey<Structure> TRAIL_TARGETS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("unknown_echoes", "echo_deer_trail_targets"));

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 轨迹剩余 tick;>0 时沿 trailDirection 逐步洒出回响粒子。 */
    private int trailTicks;
    private Vec3 trailDirection = Vec3.ZERO;
    /** 轨迹冷却:避免连续受击反复做结构检索。 */
    private int trailCooldown;

    public EchoDeer(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.9D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D,
                stack -> stack.is(Items.WHEAT) || stack.is(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // 受击触发回响轨迹:服务端检索最近遗迹方向,只留下粒子方向线索
        if (hurt && !this.level().isClientSide && this.trailCooldown <= 0 && this.isAlive()
                && ServerConfig.ECHO_DEER_TRAIL_COOLDOWN.get() > 0) {
            this.trailCooldown = ServerConfig.ECHO_DEER_TRAIL_COOLDOWN.get();
            ServerLevel serverLevel = (ServerLevel) this.level();
            BlockPos nearest = serverLevel.findNearestMapStructure(
                    TRAIL_TARGETS, this.blockPosition(), 8, false);
            if (nearest != null) {
                Vec3 dir = Vec3.atCenterOf(nearest).subtract(this.position());
                if (dir.horizontalDistanceSqr() > 4.0D) {
                    this.trailDirection = new Vec3(dir.x, 0, dir.z).normalize();
                    this.trailTicks = 50;
                }
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.trailCooldown > 0) {
            --this.trailCooldown;
        }
        // 回响轨迹:身后逐步亮起一串渐远的微光,朝向最近遗迹(纯表现,无数据)
        if (this.trailTicks > 0) {
            --this.trailTicks;
            if (this.trailTicks % 5 == 0) {
                double step = (50 - this.trailTicks) / 5.0D * 0.8D;
                Vec3 point = this.position()
                        .add(this.trailDirection.scale(1.0D + step))
                        .add(0, 0.6D, 0);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.GLOW,
                        point.x, point.y, point.z, 2, 0.15D, 0.2D, 0.15D, 0.0D);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y, point.z, 1, 0.05D, 0.1D, 0.05D, 0.01D);
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.GLOW_BERRIES);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.ECHO_DEER.get().create(level);
    }

    // ---- 声音 ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    // ---- GeckoLib 动画 ----

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
