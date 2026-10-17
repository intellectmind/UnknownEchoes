package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.network.OpenArtifactStationPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
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
        BlockPos source = pos.immutable();
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.level() == level
                && validateStation(serverPlayer, level.dimension().location(), source, mode)) {
            serverPlayer.playNotifySound(ModSounds.ARTIFACT_STATION_USE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            if (serverPlayer.level() != level
                    || !validateStation(serverPlayer, level.dimension().location(), source, mode)) {
                return InteractionResult.CONSUME;
            }
            ArtifactManager.syncToClient(serverPlayer);
            if (serverPlayer.level() != level
                    || !validateStation(serverPlayer, level.dimension().location(), source, mode)) {
                return InteractionResult.CONSUME;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new OpenArtifactStationPayload(
                    mode == Mode.RECORD ? OpenArtifactStationPayload.MODE_RECORD
                            : OpenArtifactStationPayload.MODE_TUNING, source, level.dimension().location()));
        }
        return InteractionResult.CONSUME;
    }

    public static boolean validateStation(ServerPlayer player, ResourceLocation dimension,
                                          BlockPos pos, Mode requiredMode) {
        ServerLevel source = player.serverLevel();
        if (!isActorCurrent(player, source, dimension, pos)) {
            return false;
        }
        LevelChunk chunk = source.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos).getBlock() instanceof ArtifactStationBlock station
                && station.getMode() == requiredMode && isActorCurrent(player, source, dimension, pos);
    }

    private static boolean isActorCurrent(ServerPlayer player, ServerLevel source,
                                           ResourceLocation dimension, BlockPos pos) {
        return player.isAlive() && !player.isSpectator() && !player.isRemoved() && !player.hasDisconnected()
                && player.level() == source && source.dimension().location().equals(dimension)
                && player.server.getLevel(source.dimension()) == source
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player
                && !source.isOutsideBuildHeight(pos)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= USE_RANGE * USE_RANGE;
    }
}
