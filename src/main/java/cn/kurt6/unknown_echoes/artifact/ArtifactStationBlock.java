package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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

public class ArtifactStationBlock extends Block {

    public enum Mode implements StringRepresentable {
        RECORD("record"),
        TUNING("tuning");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final MapCodec<ArtifactStationBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Mode.CODEC.fieldOf("mode").forGetter(block -> block.mode),
                    propertiesCodec()
            ).apply(instance, ArtifactStationBlock::new));

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
