package cn.kurt6.unknown_echoes.block.deep;

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
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.world_resonance.wrong_dimension");
            return InteractionResult.CONSUME;
        }
        WorldResonanceAltarBlockEntity altar = getOrCreateAltar(serverLevel, pos, state);

        EchoAbilityType missing = firstMissingAbility(serverPlayer);
        if (missing != null || !EchoPermission.canSummonWorldEcho(serverPlayer)) {
            Component missingName = missing == null
                    ? Component.translatable("dimension.unknown_echoes.deep_echoes")
                    : Component.translatable("ability.unknown_echoes." + missing.getId());
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.4F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.missing", missingName), true);
            return InteractionResult.CONSUME;
        }

        WorldEcho active = altar.findActiveBoss(serverLevel);
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
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.resonated",
                    Component.translatable("ability.unknown_echoes." + resonated.getId()),
                    altar.getResonanceCount(), EchoAbilityType.values().length), true);
            if (!altar.isFullyResonant()) {
                return InteractionResult.CONSUME;
            }
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

    @Nullable
    private static EchoAbilityType firstMissingAbility(ServerPlayer player) {
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            if (!EchoAbilityManager.hasAbility(player, ability)) {
                return ability;
            }
        }
        return null;
    }

    public static WorldResonanceAltarBlockEntity getOrCreateAltar(ServerLevel level, BlockPos pos,
                                                                   BlockState state) {
        if (level.getBlockEntity(pos) instanceof WorldResonanceAltarBlockEntity altar) {
            return altar;
        }

        WorldResonanceAltarBlockEntity altar = new WorldResonanceAltarBlockEntity(pos, state);
        level.setBlockEntity(altar);
        return altar;
    }

    static boolean completeSummon(ServerLevel level, BlockPos altarPos, ServerPlayer player,
                                  WorldResonanceAltarBlockEntity altar) {
        WorldEcho echo = ModEntities.WORLD_ECHO.get().create(level);
        if (echo == null) {
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }
        BlockPos spawnPos = findSpawnPos(level, altarPos, altar.isGrandArena());
        if (spawnPos == null) {
            echo.discard();
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }
        echo.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, echo)) {
            echo.discard();
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }
        echo.setPersistenceRequired();
        echo.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED, null);
        if (!altar.reserveStartingBoss(echo)) {
            echo.discard();
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }
        echo.registerParticipant(player);
        if (!level.addFreshEntity(echo)) {
            echo.discard();
            altar.abortSummon(echo.getUUID());
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }
        if (!altar.markActive(echo)) {
            echo.discard();
            altar.abortSummon(echo.getUUID());
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return false;
        }

        level.sendParticles(ParticleTypes.FLASH,
                spawnPos.getX() + 0.5, spawnPos.getY() + 6.0, spawnPos.getZ() + 0.5,
                4, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD,
                spawnPos.getX() + 0.5, spawnPos.getY() + 6.0, spawnPos.getZ() + 0.5,
                320, 4.2, 6.0, 4.2, 0.32);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                spawnPos.getX() + 0.5, spawnPos.getY() + 6.0, spawnPos.getZ() + 0.5,
                420, 4.0, 5.5, 4.0, 0.45);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 3.5F, 0.72F);
        return true;
    }

    @Nullable
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos, boolean grand) {
        Direction[] directions = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        int[] distances = grand ? new int[]{12, 15, 9} : new int[]{10, 8, 12};
        for (int distance : distances) {
            for (Direction direction : directions) {
                BlockPos candidate = altarPos.relative(direction, distance);
                BlockPos clearanceTop = candidate.above(14);
                if (!level.hasChunkAt(candidate)
                        || !level.getWorldBorder().isWithinBounds(candidate)
                        || !level.isInWorldBounds(candidate)
                        || !level.isInWorldBounds(clearanceTop)) {
                    continue;
                }
                BlockPos floor = candidate.below();
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    continue;
                }
                boolean clear = true;
                for (int y = 0; y <= 14; y++) {
                    if (!level.getBlockState(candidate.above(y)).getCollisionShape(
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
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
