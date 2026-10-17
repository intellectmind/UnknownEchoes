package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** 世界共鸣祭坛：逐次调谐五种能力后召唤最终 Boss，不要求额外关键物品。 */
public class WorldResonanceAltarBlock extends BaseEntityBlock {
    public static final MapCodec<WorldResonanceAltarBlock> CODEC = simpleCodec(WorldResonanceAltarBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty GRAND = BooleanProperty.create("grand");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(3, 4, 3, 13, 10, 13),
            Block.box(1, 10, 1, 15, 15, 15));

    public WorldResonanceAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(GRAND, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GRAND);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorldResonanceAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WORLD_RESONANCE_ALTAR.get(),
                WorldResonanceAltarBlockEntity::serverTick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof WorldResonanceAltarBlockEntity altar
                && altar.getResonanceCount() > 0) {
            EchoAbilityType[] abilities = EchoAbilityType.values();
            int start = random.nextInt(abilities.length);
            for (int offset = 0; offset < abilities.length; offset++) {
                EchoAbilityType ability = abilities[(start + offset) % abilities.length];
                if (!altar.hasResonance(ability)) {
                    continue;
                }
                Vec3 local = WorldResonanceAltarBlockEntity.localResonatorOffset(ability);
                double x = pos.getX() + local.x;
                double y = pos.getY() + local.y + random.nextDouble() * 0.24D;
                double z = pos.getZ() + local.z;
                var particle = switch (ability) {
                    case WIND_ECHO -> ParticleTypes.CLOUD;
                    case TIDE_ECHO -> ParticleTypes.SPLASH;
                    case TRUE_SIGHT_ECHO -> ParticleTypes.ENCHANT;
                    case GRAVITY_ECHO -> ParticleTypes.PORTAL;
                    case VOID_ECHO -> ParticleTypes.REVERSE_PORTAL;
                };
                level.addParticle(particle, x, y, z,
                        0.0D, 0.012D + random.nextDouble() * 0.018D, 0.0D);
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.END_ROD, x, y + 0.18D, z,
                            0.0D, 0.018D, 0.0D);
                }
                return;
            }
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.9D + random.nextDouble() * 0.7D;
        double z = pos.getZ() + 0.5D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.55D + random.nextDouble() * 0.3D;
        level.addParticle(ParticleTypes.ENCHANT,
                x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                -Math.cos(angle) * 0.02D, 0.015D, -Math.sin(angle) * 0.02D);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y + 0.25D, z,
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!isCurrentInteraction(state, serverLevel, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.world_resonance.wrong_dimension");
            return InteractionResult.CONSUME;
        }

        EchoAbilityType missing = firstMissingAbility(serverPlayer);
        if (missing != null || !EchoPermission.canSummonWorldEcho(serverPlayer)) {
            Component missingName = missing == null
                    ? Component.translatable("dimension.unknown_echoes.deep_echoes")
                    : Component.translatable("ability.unknown_echoes." + missing.getId());
            PlayerSoundFeedback.deny(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.missing", missingName), true);
            return InteractionResult.CONSUME;
        }

        WorldResonanceAltarBlockEntity altar = getOrCreateAltar(serverLevel, pos, state);
        if (altar == null || !altar.isCurrentAltar(serverLevel)
                || !isCurrentInteraction(state, serverLevel, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        WorldEcho active = altar.findActiveBoss(serverLevel);
        if (!isCurrentInteraction(state, serverLevel, pos, serverPlayer)
                || !altar.isCurrentAltar(serverLevel)
                || firstMissingAbility(serverPlayer) != null || !EchoPermission.canSummonWorldEcho(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (active != null) {
            active.registerParticipant(serverPlayer);
            active.onPhasePylonActivated(serverLevel, pos);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.active"), true);
            return InteractionResult.CONSUME;
        }

        boolean endingComplete = EchoAbilityManager.hasActivatedMechanism(serverPlayer,
                EchoPermission.WORLD_ECHO_COMPLETE_KEY);
        if (!endingComplete
                && EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.WORLD_ECHO_ID)) {
            if (!EchoAbilityManager.hasActivatedMechanism(serverPlayer,
                    EchoPermission.WORLD_ECHO_REWARD_READY_KEY)) {
                if (!EchoAbilityManager.activateMechanism(serverPlayer,
                        EchoPermission.WORLD_ECHO_REWARD_READY_KEY)) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.progress_record.full"), true);
                    return InteractionResult.CONSUME;
                }
                EchoAbilityManager.syncToClient(serverPlayer);
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.await_record"), true);
            return InteractionResult.CONSUME;
        }

        if (!altar.isFullyResonant()) {
            if (altar.isResonanceLocked(serverLevel.getGameTime())) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.world_resonance.resonance_settling"), true);
                return InteractionResult.CONSUME;
            }
            EchoAbilityType resonated = altar.resonateNext(serverLevel);
            if (resonated == null) {
                String message = altar.isRitualInProgress()
                        ? "message.unknown_echoes.world_resonance.ritual_active"
                        : "message.unknown_echoes.world_resonance.recovering";
                serverPlayer.displayClientMessage(Component.translatable(message), true);
                return InteractionResult.CONSUME;
            }
            if (!isCurrentInteraction(state, serverLevel, pos, serverPlayer)
                    || !altar.isCurrentAltar(serverLevel)) {
                return InteractionResult.CONSUME;
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.resonated",
                    Component.translatable("ability.unknown_echoes." + resonated.getId()),
                    altar.getResonanceCount(), EchoAbilityType.values().length), true);
            if (!altar.isFullyResonant()) {
                return InteractionResult.CONSUME;
            }
        }

        if (!isCurrentInteraction(state, serverLevel, pos, serverPlayer)
                || !altar.isCurrentAltar(serverLevel)
                || firstMissingAbility(serverPlayer) != null || !EchoPermission.canSummonWorldEcho(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!altar.beginStarting(serverLevel, serverPlayer.getUUID())) {
            String message = altar.isRitualInProgress()
                    ? "message.unknown_echoes.world_resonance.ritual_active"
                    : "message.unknown_echoes.world_resonance.recovering";
            serverPlayer.displayClientMessage(Component.translatable(message), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    private static boolean isCurrentInteraction(BlockState state, ServerLevel level, BlockPos pos,
                                                 ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && BossRechallengeAltarBlockEntity.getReadyAltarState(level, pos) == state;
    }

    @Nullable
    private static EchoAbilityType firstMissingAbility(ServerPlayer player) {
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            if (!EchoAbilityManager.hasAbility(player, ability)) {
                return ability;
            }
        }
        return null;
    }

    @Nullable
    public static WorldResonanceAltarBlockEntity getOrCreateAltar(ServerLevel level, BlockPos pos,
                                                                   BlockState state) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || level.isOutsideBuildHeight(pos) || chunk.getBlockState(pos) != state
                || !(state.getBlock() instanceof WorldResonanceAltarBlock)) {
            return null;
        }
        BlockEntity existing = chunk.getBlockEntity(pos);
        if (existing instanceof WorldResonanceAltarBlockEntity altar) {
            return altar.isCurrentAltar(level) ? altar : null;
        }
        if (existing != null) {
            return null;
        }
        WorldResonanceAltarBlockEntity altar = new WorldResonanceAltarBlockEntity(pos, state);
        level.setBlockEntity(altar);
        chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos) == state
                && chunk.getBlockEntity(pos) == altar && !altar.isRemoved() ? altar : null;
    }

    static boolean completeSummon(ServerLevel level, BlockPos altarPos, ServerPlayer player,
                                  WorldResonanceAltarBlockEntity altar) {
        if (!altarPos.equals(altar.getBlockPos()) || !altar.beginSummonCompletion(level, player)) {
            return false;
        }
        WorldEcho echo = null;
        boolean summoned = false;
        try {
            BlockState altarState = BossRechallengeAltarBlockEntity.getReadyAltarState(level, altarPos);
            echo = ModEntities.WORLD_ECHO.get().create(level);
            if (echo == null) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            BlockPos spawnPos = findSpawnPos(level, altarPos, altar.isGrandArena());
            if (spawnPos == null) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            echo.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, echo)) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            echo.setPersistenceRequired();
            echo.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.TRIGGERED, null);
            if (!altar.isCurrentSummonContext(level, player)
                    || BossRechallengeAltarBlockEntity.getReadyAltarState(level, altarPos) != altarState
                    || !EntitySpawnSafety.isLoadedAndCollisionFree(level, echo)) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            if (!altar.reserveStartingBoss(echo)) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            echo.registerParticipant(player);
            if (!level.addFreshEntity(echo)) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            if (!isCurrentSummon(level, altarPos, altarState, player, altar, echo)
                    || !altar.markActive(echo)
                    || !isCurrentSummon(level, altarPos, altarState, player, altar, echo)
                    || !altar.ownsActiveBoss(altar.getArenaId(), echo.getUUID())) {
                reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
                return false;
            }
            summoned = true;
            return true;
        } finally {
            try {
                if (!summoned) {
                    try {
                        if (echo != null) {
                            altar.abortSummon(echo.getUUID());
                        }
                        altar.abortStarting();
                    } finally {
                        if (echo != null) {
                            echo.discard();
                        }
                    }
                }
            } finally {
                altar.finishSummonCompletion();
            }
        }
    }

    private static boolean isCurrentSummon(ServerLevel level, BlockPos altarPos, BlockState altarState,
                                           ServerPlayer player, WorldResonanceAltarBlockEntity altar,
                                           WorldEcho echo) {
        return altar.isCurrentSummonContext(level, player)
                && BossRechallengeAltarBlockEntity.getReadyAltarState(level, altarPos) == altarState
                && echo.isAlive() && level.getEntity(echo.getUUID()) == echo
                && echo.matchesAltar(altar.getArenaId(), altarPos);
    }

    @Nullable
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos, boolean grand) {
        Direction[] directions = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        int[] distances = grand ? new int[]{12, 15, 9} : new int[]{10, 8, 12};
        for (int distance : distances) {
            for (Direction direction : directions) {
                BlockPos candidate = altarPos.relative(direction, distance);
                BlockPos clearanceTop = candidate.above(14);
                BlockPos floor = candidate.below();
                if (!level.getWorldBorder().isWithinBounds(candidate)
                        || !level.isInWorldBounds(candidate)
                        || !level.isInWorldBounds(clearanceTop) || !level.isInWorldBounds(floor)) {
                    continue;
                }
                var chunk = level.getChunkSource().getChunkNow(candidate.getX() >> 4, candidate.getZ() >> 4);
                if (chunk == null || !chunk.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    continue;
                }
                boolean clear = true;
                for (int y = 0; y <= 14; y++) {
                    if (!chunk.getBlockState(candidate.above(y)).getCollisionShape(
                            level, candidate.above(y)).isEmpty()) {
                        clear = false;
                        break;
                    }
                }
                if (clear) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void reject(ServerPlayer player, Level level, BlockPos pos, String messageKey) {
        PlayerSoundFeedback.deny(player);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
