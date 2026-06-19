package cn.kurt6.unknown_echoes.entity.projection;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 回响投影:回响投影器召唤的短暂残影,用于单人完成合作机关。
 * - 是 LivingEntity,所以可以触发石质压力板等"生物类"机关
 * - 不攻击、无碰撞推挤、不可被伤害、不拾取物品
 * - 不触发 Boss 机制/信标/奖励(这些交互都要求真实玩家)
 * - 默认 30 秒后消散,与召唤者绑定(再次召唤会替换旧投影)
 */
public class EchoProjectionEntity extends LivingEntity implements GeoEntity {
    public static final int DEFAULT_LIFETIME_TICKS = 600;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_projection.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int lifeTicks = DEFAULT_LIFETIME_TICKS;
    private UUID ownerId;

    public EchoProjectionEntity(EntityType<? extends EchoProjectionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(this.ownerId);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5),
                        0.0, 0.01, 0.0);
            }
            return;
        }
        if (--this.lifeTicks <= 0) {
            this.dissipate();
        }
    }

    /** 消散:粒子 + 音效 + 移除。 */
    public void dissipate() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    20, 0.3, 0.6, 0.3, 0.03);
            this.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // 投影不推挤其他实体
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // 投影没有装备
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.getInt("LifeTicks");
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 5,
                state -> state.setAndContinue(IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
