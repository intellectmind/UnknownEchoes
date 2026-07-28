package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 校准大厅的三能力节点。完成状态按玩家与结构坐标分别持久化。 */
public class DeepCalibrationNodeBlock extends Block {
    public static final MapCodec<DeepCalibrationNodeBlock> CODEC = simpleCodec(DeepCalibrationNodeBlock::new);
    public static final EnumProperty<Calibration> CALIBRATION =
            EnumProperty.create("calibration", Calibration.class);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private static final String KEY_PREFIX = "deep_calibration:";

    public DeepCalibrationNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CALIBRATION, Calibration.WIND));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CALIBRATION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.wrong_dimension"), true);
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canEnterDeepEchoes(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.need_entry"), true);
            return InteractionResult.CONSUME;
        }
        if (!DeepRouteNodeBlock.hasCompletedHall(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.need_routes"), true);
            return InteractionResult.CONSUME;
        }

        Calibration calibration = state.getValue(CALIBRATION);
        if (!EchoAbilityManager.hasAbility(serverPlayer, calibration.ability)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.missing_"
                            + calibration.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        BlockPos corePos = GravityCoreAltarBlock.findCoreNear(level, pos);
        if (corePos == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.need_core"), true);
            return InteractionResult.CONSUME;
        }

        String key = mechanismKey(level, pos, calibration);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
            drawGuidance((ServerLevel) level, pos, corePos, calibration);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.already_"
                            + calibration.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        EchoAbilityManager.activateMechanism(serverPlayer, key);
        EchoAbilityManager.syncToClient(serverPlayer);
        playFeedback((ServerLevel) level, pos, corePos, calibration, serverPlayer);
        serverPlayer.displayClientMessage(Component.translatable(
                "message.unknown_echoes.deep_calibration.completed_"
                        + calibration.getSerializedName()), true);
        return InteractionResult.CONSUME;
    }

    public static boolean isActivated(ServerPlayer player, Level level, BlockPos pos,
                                      Calibration calibration) {
        return EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, pos, calibration));
    }

    public static String mechanismKey(Level level, BlockPos pos, Calibration calibration) {
        return KEY_PREFIX + level.dimension().location() + ":"
                + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":"
                + calibration.getSerializedName();
    }

    private static void playFeedback(ServerLevel level, BlockPos pos, BlockPos corePos,
                                     Calibration calibration, ServerPlayer player) {
        var particle = switch (calibration) {
            case WIND -> ParticleTypes.CLOUD;
            case TIDE -> ParticleTypes.SPLASH;
            case TRUE_SIGHT -> ParticleTypes.ENCHANT;
        };
        level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.9,
                pos.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.04);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 0.9F, calibration.pitch);

        drawGuidance(level, pos, corePos, calibration);

        if (!GravityCoreAltarBlock.hasCompletedCalibrations(player, level, corePos)) {
            return;
        }
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0.0D, 0.65D, 0.0D);
        for (BlockPos nodePos : GravityCoreAltarBlock.findCalibrationNodes(level, corePos)) {
            BlockState nodeState = level.getBlockState(nodePos);
            Calibration nodeCalibration = nodeState.getValue(CALIBRATION);
            var nodeParticle = switch (nodeCalibration) {
                case WIND -> ParticleTypes.CLOUD;
                case TIDE -> ParticleTypes.SPLASH;
                case TRUE_SIGHT -> ParticleTypes.ENCHANT;
            };
            Vec3 source = Vec3.atCenterOf(nodePos).add(0.0D, 0.45D, 0.0D);
            BossFx.guidanceMarker(level, source, nodeParticle);
            BossFx.guidanceFlow(level, source, altarCenter,
                    ParticleTypes.GLOW, nodeParticle, level.getGameTime());
        }
        BossFx.impactBurst(level, altarCenter, ParticleTypes.END_ROD);
        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.35F, 1.15F);
    }

    private static void drawGuidance(ServerLevel level, BlockPos pos, BlockPos corePos,
                                     Calibration calibration) {
        var particle = switch (calibration) {
            case WIND -> ParticleTypes.CLOUD;
            case TIDE -> ParticleTypes.SPLASH;
            case TRUE_SIGHT -> ParticleTypes.ENCHANT;
        };

        Vec3 nodeCenter = Vec3.atCenterOf(pos).add(0.0D, 0.45D, 0.0D);
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0.0D, 0.65D, 0.0D);
        BossFx.guidanceMarker(level, nodeCenter, particle);
        BossFx.guidanceFlow(level, nodeCenter, altarCenter,
                ParticleTypes.GLOW, particle, level.getGameTime());
    }

    public enum Calibration implements StringRepresentable {
        WIND("wind", EchoAbilityType.WIND_ECHO, 1.25F),
        TIDE("tide", EchoAbilityType.TIDE_ECHO, 0.9F),
        TRUE_SIGHT("true_sight", EchoAbilityType.TRUE_SIGHT_ECHO, 1.5F);

        private final String name;
        private final EchoAbilityType ability;
        private final float pitch;

        Calibration(String name, EchoAbilityType ability, float pitch) {
            this.name = name;
            this.ability = ability;
            this.pitch = pitch;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
