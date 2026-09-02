package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.ecology.RealmCommunalFeastManager;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
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

/** 消耗三域料理，为附近玩家提供一次不写进度的共享远征支援。 */
public final class CommunalEchoTableBlock extends Block {
    public static final MapCodec<CommunalEchoTableBlock> CODEC =
            simpleCodec(CommunalEchoTableBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public static final int ACTIVE_TICKS = 1200;
    public static final int EFFECT_TICKS = 1200;
    public static final double EFFECT_RADIUS = 10.0D;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 4, 10, 4),
            Block.box(12, 0, 1, 15, 10, 4),
            Block.box(1, 0, 12, 4, 10, 15),
            Block.box(12, 0, 12, 15, 10, 15),
            Block.box(0, 9, 0, 16, 12, 16),
            Block.box(3, 12, 3, 13, 14, 13));

    public CommunalEchoTableBlock(Properties properties) {
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
        RealmCommunalFeastManager.FeastResult feastResult =
                RealmCommunalFeastManager.shareFeast(serverLevel, pos, requested);
        playActivation(serverLevel, pos, requested);
        if (feastResult.chimeActivated()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.communal_echo_table.circuit.ready"), false);
        }
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
                8, 0.25D, 0.12D, 0.25D, 0.01D);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 0.3F, 1.8F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Mode mode = state.getValue(MODE);
        if (mode == Mode.IDLE || random.nextInt(2) != 0) {
            return;
        }
        ParticleOptions particle = particleFor(mode);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.16D + random.nextDouble() * 0.34D;
        level.addParticle(particle,
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.82D + random.nextDouble() * 0.28D,
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                0.0D, 0.008D + random.nextDouble() * 0.01D, 0.0D);
    }

    private static Mode modeFor(ItemStack stack) {
        if (stack.is(ModItems.FAR_BELL_TRAIL_LOAF.get())) {
            return Mode.FAR_BELL;
        }
        if (stack.is(ModItems.PRISMATIC_CANOPY_TART.get())) {
            return Mode.CANOPY;
        }
        if (stack.is(ModItems.INTERLAYER_PROVISION_PIE.get())) {
            return Mode.INTERLAYER;
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
                case FAR_BELL -> {
                    addEffect(player, MobEffects.MOVEMENT_SPEED, EFFECT_TICKS);
                    addEffect(player, MobEffects.DIG_SPEED, EFFECT_TICKS / 2);
                }
                case CANOPY -> {
                    player.removeEffect(MobEffects.BLINDNESS);
                    addEffect(player, MobEffects.NIGHT_VISION, EFFECT_TICKS);
                    addEffect(player, MobEffects.REGENERATION, 160, 1);
                }
                case INTERLAYER -> {
                    player.removeEffect(MobEffects.DARKNESS);
                    player.fallDistance = 0.0F;
                    FoodEffectSupport.applyAbsorption(player, 900, 1, 8.0F);
                    addEffect(player, MobEffects.DAMAGE_RESISTANCE, 400);
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
        addEffect(player, effect, duration, 0);
    }

    private static void addEffect(ServerPlayer player,
                                  net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                  int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static void playActivation(ServerLevel level, BlockPos pos, Mode mode) {
        ParticleOptions particle = particleFor(mode);
        level.sendParticles(particle,
                pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D,
                48, 0.72D, 0.3D, 0.72D, 0.045D);
        level.sendParticles(ParticleTypes.GLOW,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                18, 0.5D, 0.25D, 0.5D, 0.02D);
        var sound = switch (mode) {
            case FAR_BELL -> SoundEvents.NOTE_BLOCK_BELL.value();
            case CANOPY -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case INTERLAYER -> SoundEvents.RESPAWN_ANCHOR_CHARGE;
            case IDLE -> SoundEvents.WOOD_PLACE;
        };
        float pitch = switch (mode) {
            case FAR_BELL -> 1.25F;
            case CANOPY -> 1.55F;
            case INTERLAYER -> 0.72F;
            case IDLE -> 1.0F;
        };
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.9F, pitch);
    }

    private static ParticleOptions particleFor(Mode mode) {
        return switch (mode) {
            case FAR_BELL -> ParticleTypes.NOTE;
            case CANOPY -> ParticleTypes.HAPPY_VILLAGER;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
            case IDLE -> ParticleTypes.SMOKE;
        };
    }

    private static void showHint(Player player, BlockState state) {
        Mode mode = state.getValue(MODE);
        player.displayClientMessage(Component.translatable(mode == Mode.IDLE
                ? "message.unknown_echoes.communal_echo_table.hint"
                : "message.unknown_echoes.communal_echo_table.cooling"), true);
    }

    public enum Mode implements StringRepresentable {
        IDLE("idle"),
        FAR_BELL("far_bell"),
        CANOPY("canopy"),
        INTERLAYER("interlayer");

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
