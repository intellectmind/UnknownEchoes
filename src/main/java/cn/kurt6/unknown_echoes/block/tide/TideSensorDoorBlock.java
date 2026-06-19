package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 潮汐感应门(V0.6E,5.3"进入镜湖倒影层"权限落地):倒影回廊深层内室的自动门。
 * 复用风门模式(服务端校验能力数据,飞行/传送/水下呼吸 Mod 绕不过):
 * 持潮汐回响的玩家靠近(或右键)时整组联动开启;无权限只看到模糊水纹。
 * "倒影入口稳定时间" = 开启保持时长(ServerConfig,潮汐研究 3 延长)——到时自动闭合。
 * 关闭态为整块实体(水下密封),开启态无碰撞。
 */
public class TideSensorDoorBlock extends BaseEntityBlock {
    public static final MapCodec<TideSensorDoorBlock> CODEC = simpleCodec(TideSensorDoorBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    /** 联动开关的最大方块数(同风门,防异常超大区域)。 */
    static final int MAX_CONNECTED = 32;

    /** 开启态保留顶部门楣,既是视觉残留也保证方块可被指向。 */
    private static final VoxelShape OPEN_SHAPE = Block.box(0, 14, 0, 16, 16, 16);

    public TideSensorDoorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TideSensorDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TIDE_SENSOR_DOOR.get(),
                TideSensorDoorBlockEntity::serverTick);
    }

    /** 右键 = 感应的主动形式:同样走服务端权限,开门时长同感应开启。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (state.getValue(OPEN)) {
            return InteractionResult.CONSUME;
        }
        if (cn.kurt6.unknown_echoes.ability.EchoPermission.canUseTideMechanism(serverPlayer)) {
            TideSensorDoorBlockEntity.openGroup(level, pos, serverPlayer);
        } else {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                    SoundSource.BLOCKS, 0.8F, 0.6F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_door.locked"), true);
            cn.kurt6.unknown_echoes.ability.EchoAbilityManager.recordFailure(serverPlayer,
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.TIDE_ECHO, "door_locked");
        }
        return InteractionResult.CONSUME;
    }
}
