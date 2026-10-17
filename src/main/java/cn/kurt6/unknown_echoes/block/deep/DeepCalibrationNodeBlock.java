package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.fx.ProgressionFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BooleanSupplier;

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
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
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

        BooleanSupplier current = () -> isCurrentInteraction(serverLevel, pos, state, serverPlayer)
                && EchoPermission.canEnterDeepEchoes(serverPlayer)
                && DeepRouteNodeBlock.hasCompletedHall(serverPlayer)
                && EchoAbilityManager.hasAbility(serverPlayer, calibration.ability);
        BlockPos corePos = GravityCoreAltarBlock.findCoreNear(level, pos);
        if (corePos == null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.need_core"), true);
            return InteractionResult.CONSUME;
        }

        if (!current.getAsBoolean()) {
            return InteractionResult.CONSUME;
        }
        String key = mechanismKey(level, pos, calibration);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
            notifyActiveProphets(serverLevel, pos, current);
            if (!current.getAsBoolean()) {
                return InteractionResult.CONSUME;
            }
            drawGuidance(serverLevel, pos, corePos, calibration);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_calibration.already_"
                            + calibration.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, key)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        playFeedback(serverLevel, pos, corePos, calibration, serverPlayer, current);
        notifyActiveProphets(serverLevel, pos, current);
        return InteractionResult.CONSUME;
    }

    private static boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                                ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D)
                && state.getBlock() instanceof DeepCalibrationNodeBlock
                && DeepArrayBlockAccess.getIfReady(level, pos) == state;
    }

    private static void notifyActiveProphets(ServerLevel level, BlockPos nodePos, BooleanSupplier current) {
        if (!current.getAsBoolean()) {
            return;
        }
        AABB bounds = new AABB(nodePos).inflate(32.0D);
        for (VoidProphet prophet : level.getEntitiesOfClass(VoidProphet.class, bounds,
                boss -> boss.isAlive() && boss.deepRechallengeAltar() != null
                        && boss.deepRechallengeAltar().distManhattan(nodePos) <= 48)) {
            if (!current.getAsBoolean()) {
                return;
            }
            if (prophet.isAlive() && !prophet.isRemoved() && prophet.level() == level) {
                prophet.onCalibrationNodeActivated(level, nodePos);
            }
        }
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
                                     Calibration calibration, ServerPlayer player, BooleanSupplier current) {
        if (!current.getAsBoolean()) {
            return;
        }
        switch (calibration) {
            case WIND -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_CALIBRATION_WIND.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
            case TIDE -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_CALIBRATION_TIDE.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
            case TRUE_SIGHT -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_CALIBRATION_SIGHT.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
        }
        if (!current.getAsBoolean()) {
            return;
        }
        drawGuidance(level, pos, corePos, calibration);
        if (!current.getAsBoolean()
                || !GravityCoreAltarBlock.hasCompletedCalibrations(player, level, corePos)) {
            return;
        }
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0, .65, 0);
        for (BlockPos nodePos : GravityCoreAltarBlock.findCalibrationNodes(level, corePos)) {
            if (nodePos.equals(pos)) continue;
            if (!current.getAsBoolean()) {
                return;
            }
            BlockState nodeState = DeepArrayBlockAccess.getIfReady(level, nodePos);
            if (nodeState == null || !(nodeState.getBlock() instanceof DeepCalibrationNodeBlock)) {
                return;
            }
            Calibration nodeCalibration = nodeState.getValue(CALIBRATION);
            drawGuidance(level, nodePos, corePos, nodeCalibration);
        }
        if (!current.getAsBoolean()
                || !GravityCoreAltarBlock.hasCompletedCalibrations(player, level, corePos)) {
            return;
        }
        MechanismFx.emit(level, ProgressionFx.convergence(altarCenter, ProgressionFx.Theme.GRAVITY));
        if (!current.getAsBoolean()) {
            return;
        }
        level.playSound(null, corePos, ModSounds.BLOCK_DEEP_CALIBRATION_COMPLETE.get(),
                SoundSource.BLOCKS, 1.35F, 1.0F);
    }

    private static void drawGuidance(ServerLevel level, BlockPos pos, BlockPos corePos,
                                     Calibration calibration) {
        Vec3 nodeCenter = Vec3.atCenterOf(pos).add(0, .45, 0);
        Vec3 altarCenter = Vec3.atCenterOf(corePos).add(0, .65, 0);
        MechanismFx.emit(level, ProgressionFx.guidance(nodeCenter, altarCenter, calibration.theme));
    }

    public enum Calibration implements StringRepresentable {
        WIND("wind", EchoAbilityType.WIND_ECHO, ProgressionFx.Theme.WIND),
        TIDE("tide", EchoAbilityType.TIDE_ECHO, ProgressionFx.Theme.TIDE),
        TRUE_SIGHT("true_sight", EchoAbilityType.TRUE_SIGHT_ECHO, ProgressionFx.Theme.TRUE_SIGHT);

        private final String name;
        private final EchoAbilityType ability;
        private final ProgressionFx.Theme theme;

        Calibration(String name, EchoAbilityType ability, ProgressionFx.Theme theme) {
            this.name = name;
            this.ability = ability;
            this.theme = theme;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
