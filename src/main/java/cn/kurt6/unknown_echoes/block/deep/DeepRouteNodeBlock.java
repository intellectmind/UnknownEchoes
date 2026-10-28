package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.fx.ProgressionFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 裂层门厅三路线校准节点。物理路线仅作引导，完成状态由服务端个人记录决定。 */
public class DeepRouteNodeBlock extends Block {
    public static final MapCodec<DeepRouteNodeBlock> CODEC = simpleCodec(DeepRouteNodeBlock::new);
    public static final EnumProperty<Route> ROUTE = EnumProperty.create("route", Route.class);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    private static final String ROUTE_PREFIX = "deep_route:fracture_hall:";
    private static final String HALL_COMPLETE_KEY = ROUTE_PREFIX + "complete";

    public DeepRouteNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROUTE, Route.WIND));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROUTE);
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
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)
                || !EchoPermission.canEnterDeepEchoes(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_route.dormant"), true);
            return InteractionResult.CONSUME;
        }

        Route route = state.getValue(ROUTE);
        if (!EchoAbilityManager.hasAbility(serverPlayer, route.ability)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_route.missing_" + route.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }
        String routeKey = ROUTE_PREFIX + route.getSerializedName();
        boolean alreadyActivated = EchoAbilityManager.hasActivatedMechanism(serverPlayer, routeKey);
        boolean completedHall = wouldCompleteAll(serverPlayer, route);
        if (alreadyActivated && (!completedHall || hasCompletedHall(serverPlayer))) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_route.already_" + route.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        boolean recorded = completedHall
                ? EchoAbilityManager.activateMechanisms(serverPlayer, routeKey, HALL_COMPLETE_KEY)
                : EchoAbilityManager.activateMechanism(serverPlayer, routeKey);
        if (!recorded) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        if (!isCurrentFeedback(serverLevel, pos, state, serverPlayer, route, completedHall)) {
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        if (!isCurrentFeedback(serverLevel, pos, state, serverPlayer, route, completedHall)) {
            return InteractionResult.CONSUME;
        }
        if (completedHall) {
            ModAdvancements.award(serverPlayer, ModAdvancements.FRACTURE_HALL);
        }
        playFeedback(serverLevel, pos, state, serverPlayer, route, completedHall);
        if (!isCurrentFeedback(serverLevel, pos, state, serverPlayer, route, completedHall)) {
            return InteractionResult.CONSUME;
        }
        if (completedHall) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_route.hall_complete"), true);
        }
        return InteractionResult.CONSUME;
    }

    public static boolean hasCompletedAll(Player player) {
        for (Route route : Route.values()) {
            if (!EchoAbilityManager.hasActivatedMechanism(player,
                    ROUTE_PREFIX + route.getSerializedName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean wouldCompleteAll(Player player, Route current) {
        for (Route route : Route.values()) {
            if (route != current && !EchoAbilityManager.hasActivatedMechanism(player,
                    ROUTE_PREFIX + route.getSerializedName())) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasCompletedHall(Player player) {
        return EchoAbilityManager.hasActivatedMechanism(player, HALL_COMPLETE_KEY);
    }

    /** 管理员测试入口使用；正式玩法仍需逐座交互。 */
    public static boolean prepareVoidProphetForTesting(ServerPlayer player) {
        return EchoAbilityManager.activateBeaconRoute(player,
                EchoPermission.ABYSS_BEACON_ID, EchoPermission.DEEP_ECHOES_ID,
                ROUTE_PREFIX + Route.WIND.getSerializedName(),
                ROUTE_PREFIX + Route.TIDE.getSerializedName(),
                ROUTE_PREFIX + Route.TRUE_SIGHT.getSerializedName(),
                HALL_COMPLETE_KEY);
    }

    // 交互事件或进度通知可能已改变现场；已提交记录保留，旧节点不再继续演出。
    private static boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                                ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D)
                && state.getBlock() instanceof DeepRouteNodeBlock
                && DeepArrayBlockAccess.getIfReady(level, pos) == state;
    }

    private static boolean isCurrentFeedback(ServerLevel level, BlockPos pos, BlockState state,
                                             ServerPlayer player, Route route, boolean completedHall) {
        return isCurrentInteraction(level, pos, state, player)
                && level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)
                && EchoPermission.canEnterDeepEchoes(player)
                && EchoAbilityManager.hasAbility(player, route.ability)
                && EchoAbilityManager.hasActivatedMechanism(player, ROUTE_PREFIX + route.getSerializedName())
                && (!completedHall || hasCompletedHall(player));
    }

    private static void playFeedback(ServerLevel level, BlockPos pos, BlockState state,
                                     ServerPlayer player, Route route, boolean completedHall) {
        if (!isCurrentFeedback(level, pos, state, player, route, completedHall)) {
            return;
        }
        MechanismFx.emit(level, ProgressionFx.node(Vec3.atBottomCenterOf(pos).add(0, .8, 0),
                route.theme, completedHall));
        if (!isCurrentFeedback(level, pos, state, player, route, completedHall)) {
            return;
        }
        if (completedHall) {
            level.playSound(null, pos, ModSounds.BLOCK_DEEP_ROUTE_COMPLETE.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
        } else {
            switch (route) {
                case WIND -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_ROUTE_WIND.get(),
                        SoundSource.BLOCKS, 0.9F, 1.0F);
                case TIDE -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_ROUTE_TIDE.get(),
                        SoundSource.BLOCKS, 0.9F, 1.0F);
                case TRUE_SIGHT -> level.playSound(null, pos, ModSounds.BLOCK_DEEP_ROUTE_SIGHT.get(),
                        SoundSource.BLOCKS, 0.9F, 1.0F);
            }
        }
    }

    public enum Route implements StringRepresentable {
        WIND("wind", EchoAbilityType.WIND_ECHO, ProgressionFx.Theme.WIND),
        TIDE("tide", EchoAbilityType.TIDE_ECHO, ProgressionFx.Theme.TIDE),
        TRUE_SIGHT("true_sight", EchoAbilityType.TRUE_SIGHT_ECHO, ProgressionFx.Theme.TRUE_SIGHT);

        private final String name;
        private final EchoAbilityType ability;
        private final ProgressionFx.Theme theme;

        Route(String name, EchoAbilityType ability, ProgressionFx.Theme theme) {
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
