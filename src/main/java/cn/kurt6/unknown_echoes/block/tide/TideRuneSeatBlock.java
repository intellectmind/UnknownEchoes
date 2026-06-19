package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.entity.boss.TideLanternKeeper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 水下符文座:潮汐灯塔礁的致盲机制方块(V0.6C 潮汐执灯者)。
 * 复用 TideRuneBlock 的权限/提示思路(无潮汐回响只见模糊水纹),但刻意不写
 * 深渊观测者的净化进度键——这是独立的 Mini Boss 机制(10.4.2 开发边界)。
 * - 有潮汐回响 + 附近执灯者灯亮:交互触发 4 秒致盲与短硬直,符文座进入冷却。
 * - LIT = 可用;冷却后 scheduleTick 自动复位。
 */
public class TideRuneSeatBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<TideRuneSeatBlock> CODEC = simpleCodec(TideRuneSeatBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** 致盲触发后的符文座冷却(tick)。 */
    public static final int COOLDOWN_TICKS = 400;
    /** 符文座生效半径:覆盖整个灯塔礁战场。 */
    private static final double KEEPER_RANGE = 16.0D;

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public TideRuneSeatBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, Boolean.TRUE)
                .setValue(WATERLOGGED, Boolean.TRUE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), 3);
            level.sendParticles(ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        // 服务端权限判定(红线 #1):没有潮汐回响只看到模糊水纹,水下呼吸 Mod 绕不过
        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.blurred"), true);
            return InteractionResult.CONSUME;
        }
        if (!state.getValue(LIT)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.cooling"), true);
            return InteractionResult.CONSUME;
        }
        TideLanternKeeper keeper = findKeeper(serverLevel, pos);
        if (keeper == null || !keeper.isLanternLit()) {
            // 没有可致盲的对象:符文只回应一段水下的余光(教学/回访提示)
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune_seat.idle"), true);
            return InteractionResult.CONSUME;
        }
        keeper.applyRuneBlind(player);
        level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), 3);
        serverLevel.scheduleTick(pos, this, COOLDOWN_TICKS);
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4F, 0.5F);
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.tide_rune_seat.flash"), true);
        return InteractionResult.CONSUME;
    }

    private static TideLanternKeeper findKeeper(ServerLevel level, BlockPos pos) {
        for (TideLanternKeeper keeper : level.getEntitiesOfClass(TideLanternKeeper.class,
                new AABB(pos).inflate(KEEPER_RANGE))) {
            if (keeper.isAlive()) {
                return keeper;
            }
        }
        return null;
    }
}
