package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 局部重力场运行状态。每三秒切换方向，十二秒后自动关闭。 */
public class GravityFieldCoreBlockEntity extends BlockEntity {
    private static final int ACTIVE_TICKS = 240;
    private static final int PHASE_TICKS = 60;
    private static final double FIELD_RADIUS = 8.0D;
    private static final double FIELD_HEIGHT = 6.0D;
    /** 每两 tick 施加一次，必须覆盖同期原版重力才会形成可见的反转上浮。 */
    private static final double FULL_ACCELERATION = 0.19D;
    private static final double ATTUNED_ACCELERATION = 0.10D;

    private int remainingTicks;
    private int phaseTicks;

    public GravityFieldCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_FIELD_CORE.get(), pos, state);
    }

    public void activate() {
        if (level == null) {
            return;
        }
        remainingTicks = ACTIVE_TICKS;
        phaseTicks = 0;
        level.setBlock(worldPosition, getBlockState()
                .setValue(GravityFieldCoreBlock.ACTIVE, Boolean.TRUE)
                .setValue(GravityFieldCoreBlock.INVERTED, Boolean.TRUE), 3);
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 1.0F, 0.75F);
        setChanged();
    }

    public void deactivate() {
        if (level == null) {
            return;
        }
        remainingTicks = 0;
        phaseTicks = 0;
        level.setBlock(worldPosition, getBlockState()
                .setValue(GravityFieldCoreBlock.ACTIVE, Boolean.FALSE)
                .setValue(GravityFieldCoreBlock.INVERTED, Boolean.FALSE), 3);
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.BLOCKS, 0.8F, 1.15F);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  GravityFieldCoreBlockEntity core) {
        // 方块被外部改回非激活状态时，立即停止旧 NBT 中残留的计时器。
        if (!state.getValue(GravityFieldCoreBlock.ACTIVE)) {
            if (core.remainingTicks > 0 || core.phaseTicks > 0) {
                core.remainingTicks = 0;
                core.phaseTicks = 0;
                core.setChanged();
            }
            return;
        }
        if (core.remainingTicks <= 0) {
            if (state.getValue(GravityFieldCoreBlock.ACTIVE)) {
                core.deactivate();
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        core.remainingTicks--;
        core.phaseTicks++;
        if (core.phaseTicks >= PHASE_TICKS) {
            core.phaseTicks = 0;
            level.setBlock(pos, state.setValue(GravityFieldCoreBlock.INVERTED,
                    !state.getValue(GravityFieldCoreBlock.INVERTED)), 3);
            state = level.getBlockState(pos);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.85F,
                    state.getValue(GravityFieldCoreBlock.INVERTED) ? 1.35F : 0.65F);
            notifyPhase(serverLevel, pos, state.getValue(GravityFieldCoreBlock.INVERTED));
        }
        if (core.remainingTicks == 0) {
            notifyPlayers(serverLevel, pos, "message.unknown_echoes.gravity_field.stopped");
            core.deactivate();
            return;
        }
        if (core.remainingTicks % 20 == 0) {
            core.setChanged();
            Vec3 center = Vec3.atCenterOf(pos);
            BossFx.ring(serverLevel, center, FIELD_RADIUS, 24, 0.12D,
                    state.getValue(GravityFieldCoreBlock.INVERTED)
                            ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL);
            GravityFieldCoreBlock.emitAnchorGuidance(serverLevel, pos);
        }
        if (level.getGameTime() % 2 != 0) {
            return;
        }

        boolean inverted = state.getValue(GravityFieldCoreBlock.INVERTED);
        AABB field = fieldBounds(pos);
        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, field,
                candidate -> candidate.isAlive() && !candidate.isNoGravity()
                        && (candidate instanceof LivingEntity
                        || candidate instanceof ItemEntity
                        || candidate instanceof ExperienceOrb
                        || candidate instanceof Projectile))) {
            if (entity instanceof ServerPlayer player
                    && (player.isSpectator() || player.getAbilities().flying)) {
                continue;
            }
            boolean attuned = entity instanceof ServerPlayer player
                    && EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO);
            double acceleration = attuned ? ATTUNED_ACCELERATION : FULL_ACCELERATION;
            Vec3 motion = entity.getDeltaMovement();
            double nextY = inverted
                    ? Math.min(0.38D, motion.y + acceleration)
                    : Math.max(-0.55D, motion.y - acceleration);
            entity.setDeltaMovement(motion.x, nextY, motion.z);
            entity.hasImpulse = true;
            if (entity instanceof LivingEntity living && (inverted || attuned)) {
                living.fallDistance = 0.0F;
            }
            if (entity instanceof ServerPlayer player) {
                player.hurtMarked = true;
            }
        }

        if (level.getGameTime() % 4 == 0) {
            double y = pos.getY() + 1.0 + (level.getGameTime() % 20) * 0.2;
            serverLevel.sendParticles(inverted ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL,
                    pos.getX() + 0.5, y, pos.getZ() + 0.5,
                    8, 1.6, 1.1, 1.6, 0.04);
        }
    }

    private static void notifyPhase(ServerLevel level, BlockPos pos, boolean inverted) {
        notifyPlayers(level, pos, inverted
                ? "message.unknown_echoes.gravity_field.phase_up"
                : "message.unknown_echoes.gravity_field.phase_down");
    }

    private static void notifyPlayers(ServerLevel level, BlockPos pos, String messageKey) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, fieldBounds(pos),
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    private static AABB fieldBounds(BlockPos pos) {
        return new AABB(pos.getX() + 0.5D - FIELD_RADIUS, pos.getY() - 2.0D,
                pos.getZ() + 0.5D - FIELD_RADIUS, pos.getX() + 0.5D + FIELD_RADIUS,
                pos.getY() + FIELD_HEIGHT, pos.getZ() + 0.5D + FIELD_RADIUS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putInt("PhaseTicks", phaseTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        remainingTicks = Math.max(0, Math.min(ACTIVE_TICKS, tag.getInt("RemainingTicks")));
        phaseTicks = Math.max(0, Math.min(PHASE_TICKS - 1, tag.getInt("PhaseTicks")));
    }
}
