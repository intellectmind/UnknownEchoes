package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
import cn.kurt6.unknown_echoes.entity.boss.InvertedGatekeeper;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** 可关闭的房间级重力场核心，不修改全局物理或其他 Mod 的飞行状态。 */
public class GravityFieldCoreBlock extends BaseEntityBlock {
    public static final MapCodec<GravityFieldCoreBlock> CODEC = simpleCodec(GravityFieldCoreBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(2, 3, 2, 14, 7, 14));

    public GravityFieldCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(ACTIVE, Boolean.FALSE)
                .setValue(INVERTED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, INVERTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityFieldCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.GRAVITY_FIELD_CORE.get(),
                GravityFieldCoreBlockEntity::serverTick);
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
        if (!EchoPermission.canUseGravityMechanism(serverPlayer)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.45F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_field.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.GRAVITY_ECHO,
                    "gravity_field_locked");
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(pos) instanceof GravityFieldCoreBlockEntity core)) {
            return InteractionResult.CONSUME;
        }

        if (state.getValue(ACTIVE)) {
            core.deactivate();
            ServerLevel serverLevel = (ServerLevel) level;
            for (InvertedGatekeeper boss : serverLevel.getEntitiesOfClass(InvertedGatekeeper.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(36.0D),
                    entity -> entity.isAlive())) {
                boss.onGravityCoreDeactivated(serverLevel, pos);
            }
        } else {
            core.activate();
            if (emitAnchorGuidance((ServerLevel) level, pos)) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.gravity_field.started_with_anchors"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    static boolean emitAnchorGuidance(ServerLevel level, BlockPos pos) {
        var anchors = SpaceAnchorBlock.findArrayAnchorsNear(level, pos);
        if (anchors.isEmpty()) {
            return false;
        }
        Vec3 source = Vec3.atCenterOf(pos).add(0.0D, 0.45D, 0.0D);
        for (BlockPos anchor : anchors) {
            Vec3 target = Vec3.atCenterOf(anchor).add(0.0D, 0.75D, 0.0D);
            BossFx.guidanceMarker(level, target, ParticleTypes.SCULK_SOUL);
            BossFx.guidanceFlow(level, source, target,
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD,
                    level.getGameTime());
        }
        return true;
    }
}
