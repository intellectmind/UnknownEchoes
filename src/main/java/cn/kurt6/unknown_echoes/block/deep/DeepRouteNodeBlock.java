package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
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
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
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
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, routeKey)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.deep_route.already_" + route.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        boolean completedHall = wouldCompleteAll(serverPlayer, route);
        boolean recorded = completedHall
                ? EchoAbilityManager.activateMechanisms(serverPlayer, routeKey, HALL_COMPLETE_KEY)
                : EchoAbilityManager.activateMechanism(serverPlayer, routeKey);
        if (!recorded) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        playFeedback((ServerLevel) level, pos, route, completedHall);
        serverPlayer.displayClientMessage(Component.translatable(completedHall
                ? "message.unknown_echoes.deep_route.hall_complete"
                : "message.unknown_echoes.deep_route.completed_" + route.getSerializedName()), true);
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

    private static void playFeedback(ServerLevel level, BlockPos pos, Route route, boolean completedHall) {
        var particle = switch (route) {
            case WIND -> ParticleTypes.CLOUD;
            case TIDE -> ParticleTypes.SPLASH;
            case TRUE_SIGHT -> ParticleTypes.ENCHANT;
        };
        level.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.8,
                pos.getZ() + 0.5, completedHall ? 42 : 24, 0.35, 0.4, 0.35, 0.04);
        level.playSound(null, pos, completedHall ? SoundEvents.RESPAWN_ANCHOR_CHARGE
                        : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.9F, completedHall ? 0.75F : route.pitch);
    }

    public enum Route implements StringRepresentable {
        WIND("wind", EchoAbilityType.WIND_ECHO, 1.25F),
        TIDE("tide", EchoAbilityType.TIDE_ECHO, 0.9F),
        TRUE_SIGHT("true_sight", EchoAbilityType.TRUE_SIGHT_ECHO, 1.5F);

        private final String name;
        private final EchoAbilityType ability;
        private final float pitch;

        Route(String name, EchoAbilityType ability, float pitch) {
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
