package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.block.FacilityActivation;
import cn.kurt6.unknown_echoes.block.FacilitySupport;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.ExplorationFx.Provision;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;
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
            // 主手无对应材料时留给副手，不能让提示或默认交互提前结束本次右键。
            if (hand == InteractionHand.MAIN_HAND
                    && modeFor(player.getOffhandItem()) != Mode.IDLE) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                showHint(player, state, level, pos);
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
            showHint(serverPlayer, state, level, pos);
            return ItemInteractionResult.CONSUME;
        }

        if (!FacilityActivation.activate(serverLevel, pos, state,
                state.setValue(MODE, requested), serverPlayer, stack, ACTIVE_TICKS)) {
            return ItemInteractionResult.CONSUME;
        }
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
        showHint(player, state, level, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == Mode.IDLE) {
            return;
        }
        level.setBlock(pos, state.setValue(MODE, Mode.IDLE), Block.UPDATE_ALL);
        MechanismFx.emit(level, ExplorationFx.provision(Vec3.atBottomCenterOf(pos), themeFor(state.getValue(MODE)), true));
        level.playSound(null, pos, ModSounds.BLOCK_PROVISION_COOL.get(), SoundSource.BLOCKS, 0.35F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Mode mode = state.getValue(MODE);
        if (mode == Mode.IDLE || !ClientConfig.ENABLE_ECHO_PARTICLES.get() || random.nextInt(4) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.10D + random.nextDouble() * 0.14D;
        level.addParticle(themeFor(mode).ember(),
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.72D + random.nextDouble() * 0.18D,
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                0.0D, 0.001D + random.nextDouble() * 0.002D, 0.0D);
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
                new AABB(pos).inflate(EFFECT_RADIUS),
                player -> FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player));
        int supported = 0;
        for (ServerPlayer player : players) {
            if (!FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player)) continue;
            switch (mode) {
                case FOLDED -> {
                    addEffect(player, MobEffects.MOVEMENT_SPEED, EFFECT_TICKS);
                    if (!FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player)) continue;
                    addEffect(player, MobEffects.DIG_SPEED, EFFECT_TICKS);
                }
                case GRAVITY -> {
                    player.fallDistance = 0.0F;
                    addEffect(player, MobEffects.SLOW_FALLING, EFFECT_TICKS);
                    if (!FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player)) continue;
                    addEffect(player, MobEffects.JUMP, EFFECT_TICKS);
                }
                case HUSHED -> {
                    player.removeEffect(MobEffects.GLOWING);
                    if (!FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player)) continue;
                    addEffect(player, MobEffects.NIGHT_VISION, EFFECT_TICKS);
                    if (!FacilitySupport.isEligible(level, pos, EFFECT_RADIUS, player)) continue;
                    addEffect(player, MobEffects.INVISIBILITY, 300);
                }
                case IDLE -> {
                }
            }
            supported++;
        }
        return supported;
    }

    private static void addEffect(ServerPlayer player,
                                  net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                  int duration) {
        player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
    }

    private static void playActivation(ServerLevel level, BlockPos pos, Mode mode) {
        Provision theme = themeFor(mode);
        if (theme == null) return;
        MechanismFx.emit(level, ExplorationFx.provision(Vec3.atBottomCenterOf(pos), theme, false));
        switch (mode) {
            case FOLDED -> level.playSound(null, pos, ModSounds.BLOCK_PROVISION_FOLDED.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            case GRAVITY -> level.playSound(null, pos, ModSounds.BLOCK_PROVISION_GRAVITY.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            case HUSHED -> level.playSound(null, pos, ModSounds.BLOCK_PROVISION_HUSHED.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            case IDLE -> { }
        }
    }

    private static Provision themeFor(Mode mode) {
        return switch (mode) {
            case FOLDED -> Provision.FOLDED;
            case GRAVITY -> Provision.GRAVITY;
            case HUSHED -> Provision.HUSHED;
            case IDLE -> null;
        };
    }

    private static void showHint(Player player, BlockState state, Level level, BlockPos pos) {
        if (player.level() != level || !player.isAlive() || player.isSpectator()
                || !player.canInteractWithBlock(pos, 1.0D) || level.getBlockState(pos) != state) {
            return;
        }
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
