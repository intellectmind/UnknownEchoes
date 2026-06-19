package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 神器台座(V0.6D 12.2):RECORD=记录台(领取/复领凭据),TUNING=升级台(升级/调谐)。
 * 交互只打开回响总览"神器"页并携带台座上下文(5.8:不开独立原版界面);
 * 实际领取/升级/调谐由 ArtifactActionPayload 回到服务端,经 ArtifactManager 校验距离与资格落账。
 */
public class ArtifactStationBlock extends Block {

    public enum Mode { RECORD, TUNING }

    public static final MapCodec<ArtifactStationBlock> CODEC = simpleCodec(
            properties -> new ArtifactStationBlock(Mode.RECORD, properties));

    /** 玩家与台座的最大有效操作距离(服务端校验 ArtifactActionPayload 用)。 */
    public static final double USE_RANGE = 6.0D;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(5, 3, 5, 11, 9, 11),
            Block.box(3, 9, 3, 13, 13, 13));

    private final Mode mode;

    public ArtifactStationBlock(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.1F);
            // 先推一次最新神器/能量数据,再让客户端打开神器页(数据先行,UI 只展示)
            ArtifactManager.syncToClient(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenArtifactStationPayload(
                    mode == Mode.RECORD ? OpenArtifactStationPayload.MODE_RECORD
                            : OpenArtifactStationPayload.MODE_TUNING, pos));
        }
        return InteractionResult.CONSUME;
    }

    /** 服务端校验:玩家附近 pos 处确实是要求模式的台座(防伪造坐标的操作包)。 */
    public static boolean validateStation(ServerPlayer player, BlockPos pos, Mode requiredMode) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                > USE_RANGE * USE_RANGE) {
            return false;
        }
        return player.level().getBlockState(pos).getBlock() instanceof ArtifactStationBlock station
                && station.getMode() == requiredMode;
    }
}
