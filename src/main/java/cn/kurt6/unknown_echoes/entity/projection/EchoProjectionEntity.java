package cn.kurt6.unknown_echoes.entity.projection;

import cn.kurt6.unknown_echoes.item.EchoProjectorItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 回响投影:回响投影器召唤的短暂残影,用于单人完成合作机关。 */
public class EchoProjectionEntity extends LivingEntity implements GeoEntity {
    public static final int DEFAULT_LIFETIME_TICKS = 600;
    private static final int DISSIPATE_DURATION_TICKS = 12;

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.echo_projection.idle");
    private static final RawAnimation MANIFEST_ANIM =
            RawAnimation.begin().then("animation.echo_projection.manifest", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DISSIPATE_ANIM =
            RawAnimation.begin().then("animation.echo_projection.dissipate", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int lifeTicks = DEFAULT_LIFETIME_TICKS;
    private int dissipateTicks = -1;
    private boolean manifestTriggered;
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
        if (!this.manifestTriggered) {
            this.manifestTriggered = true;
            this.triggerAnim("projection", "manifest");
        }
        if (this.dissipateTicks >= 0) {
            if (--this.dissipateTicks <= 0) {
                this.finishDissipate();
            }
            return;
        }
        if (this.ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerId);
            if (owner != null && !EchoProjectorItem.isCurrentProjection(owner, this.getUUID())) {
                this.dissipate();
                return;
            }
        }
        if (--this.lifeTicks <= 0) {
            this.dissipate();
        }
    }

    /** 消散:粒子 + 音效 + 移除。 */
    public void dissipate() {
        if (this.dissipateTicks >= 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }
        if (this.ownerId != null) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerId);
            if (owner != null) {
                EchoProjectorItem.clearCurrentProjection(owner, this.getUUID());
            }
        }
        this.dissipateTicks = DISSIPATE_DURATION_TICKS;
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.triggerAnim("projection", "dissipate");
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + this.getBbHeight() * 0.45D, this.getZ(),
                10, 0.28D, 0.48D, 0.28D, 0.015D);
    }

    private void finishDissipate() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(),
                    24, 0.36D, 0.7D, 0.36D, 0.035D);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.15D, this.getZ(),
                    14, 0.5D, 0.08D, 0.5D, 0.02D);
            this.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.dissipate();
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

    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putInt("DissipateTicks", this.dissipateTicks);
        tag.putBoolean("ManifestTriggered", this.manifestTriggered);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.contains("LifeTicks")
                ? Math.max(0, Math.min(tag.getInt("LifeTicks"), DEFAULT_LIFETIME_TICKS))
                : DEFAULT_LIFETIME_TICKS;
        this.dissipateTicks = tag.contains("DissipateTicks")
                ? Math.max(-1, Math.min(tag.getInt("DissipateTicks"), DISSIPATE_DURATION_TICKS))
                : -1;
        this.manifestTriggered = tag.getBoolean("ManifestTriggered");
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "projection", 4,
                state -> state.setAndContinue(IDLE_ANIM))
                .triggerableAnim("manifest", MANIFEST_ANIM)
                .triggerableAnim("dissipate", DISSIPATE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
