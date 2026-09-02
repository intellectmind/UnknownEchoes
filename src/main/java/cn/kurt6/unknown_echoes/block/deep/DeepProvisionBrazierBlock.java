package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** 消耗可回访植材，为附近玩家提供一次不涉及进度的野炊支援。 */
public final class DeepProvisionBrazierBlock extends Block {
    public static final MapCodec<DeepProvisionBrazierBlock> CODEC =
            simpleCodec(DeepProvisionBrazierBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public static final int ACTIVE_TICKS = 1200;
    public static final int EFFECT_TICKS = 900;
    public static final double EFFECT_RADIUS = 8.0D;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(3, 3, 3, 13, 7, 13),
            Block.box(2, 7, 2, 14, 10, 14),
            Block.box(3, 10, 3, 5, 14, 5),
            Block.box(11, 10, 11, 13, 14, 13));

    public DeepProvisionBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.IDLE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        Mode requested = modeFor(stack);
        if (requested == Mode.IDLE) {
            if (!level.isClientSide) {
                showHint(player, state);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return ItemInteractionResult.CONSUME;
        }
        if (state.getValue(MODE) != Mode.IDLE) {
            showHint(serverPlayer, state);
            return ItemInteractionResult.CONSUME;
        }

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.setBlock(pos, state.setValue(MODE, requested), Block.UPDATE_ALL);
        serverLevel.scheduleTick(pos, this, ACTIVE_TICKS);
        applySupport(serverLevel, pos, requested);
        playActivation(serverLevel, pos, requested);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        showHint(player, state);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == Mode.IDLE) {
            return;
        }
        level.setBlock(pos, state.setValue(MODE, Mode.IDLE), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                8, 0.22D, 0.12D, 0.22D, 0.01D);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 0.35F, 1.6F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Mode mode = state.getValue(MODE);
        if (mode == Mode.IDLE || random.nextInt(2) != 0) {
            return;
        }
        ParticleOptions particle = particleFor(mode);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.12D + random.nextDouble() * 0.28D;
        level.addParticle(particle,
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.72D + random.nextDouble() * 0.35D,
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                0.0D, 0.008D + random.nextDouble() * 0.012D, 0.0D);
    }

    private static Mode modeFor(ItemStack stack) {
        if (stack.is(ModItems.FOLDED_SPOREBLOOM.get())) {
            return Mode.FOLDED;
        }
        if (stack.is(ModItems.GRAVITY_BELL.get())) {
            return Mode.GRAVITY;
        }
        if (stack.is(ModItems.HUSH_THORN.get())) {
            return Mode.HUSHED;
        }
        return Mode.IDLE;
    }

    private static int applySupport(ServerLevel level, BlockPos pos, Mode mode) {
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(pos).inflate(EFFECT_RADIUS), player -> !player.isSpectator()
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= EFFECT_RADIUS * EFFECT_RADIUS);
        for (ServerPlayer player : players) {
            switch (mode) {
                case FOLDED -> {
                    addEffect(player, MobEffects.MOVEMENT_SPEED, EFFECT_TICKS);
                    addEffect(player, MobEffects.DIG_SPEED, EFFECT_TICKS);
                }
                case GRAVITY -> {
                    player.fallDistance = 0.0F;
                    addEffect(player, MobEffects.SLOW_FALLING, EFFECT_TICKS);
                    addEffect(player, MobEffects.JUMP, EFFECT_TICKS);
                }
                case HUSHED -> {
                    player.removeEffect(MobEffects.GLOWING);
                    addEffect(player, MobEffects.NIGHT_VISION, EFFECT_TICKS);
                    addEffect(player, MobEffects.INVISIBILITY, 300);
                }
                case IDLE -> {
                }
            }
        }
        return players.size();
    }

    private static void addEffect(ServerPlayer player,
                                  net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                  int duration) {
        player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
    }

    private static void playActivation(ServerLevel level, BlockPos pos, Mode mode) {
        ParticleOptions particle = particleFor(mode);
        level.sendParticles(particle,
                pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                42, 0.6D, 0.35D, 0.6D, 0.055D);
        level.sendParticles(ParticleTypes.GLOW,
                pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                18, 0.45D, 0.25D, 0.45D, 0.025D);
        float pitch = switch (mode) {
            case FOLDED -> 1.35F;
            case GRAVITY -> 0.85F;
            case HUSHED -> 0.55F;
            case IDLE -> 1.0F;
        };
        level.playSound(null, pos, mode == Mode.GRAVITY
                        ? SoundEvents.RESPAWN_ANCHOR_CHARGE
                        : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.9F, pitch);
    }

    private static ParticleOptions particleFor(Mode mode) {
        return switch (mode) {
            case FOLDED -> ParticleTypes.HAPPY_VILLAGER;
            case GRAVITY -> ParticleTypes.REVERSE_PORTAL;
            case HUSHED -> ParticleTypes.SCULK_SOUL;
            case IDLE -> ParticleTypes.SMOKE;
        };
    }

    private static void showHint(Player player, BlockState state) {
        Mode mode = state.getValue(MODE);
        player.displayClientMessage(Component.translatable(mode == Mode.IDLE
                ? "message.unknown_echoes.deep_provision_brazier.hint"
                : "message.unknown_echoes.deep_provision_brazier.cooling"), true);
    }

    public enum Mode implements StringRepresentable {
        IDLE("idle"),
        FOLDED("folded"),
        GRAVITY("gravity"),
        HUSHED("hushed");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
