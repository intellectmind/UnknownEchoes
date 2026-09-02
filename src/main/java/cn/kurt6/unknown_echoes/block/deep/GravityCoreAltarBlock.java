package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
import cn.kurt6.unknown_echoes.entity.boss.VoidProphet;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import cn.kurt6.unknown_echoes.world.LoadedAreaCheck;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** 校准大厅核心：管理重力信物、虚空先知战斗与重力回响授予。 */
public class GravityCoreAltarBlock extends BaseEntityBlock {
    public static final MapCodec<GravityCoreAltarBlock> CODEC = simpleCodec(GravityCoreAltarBlock::new);

    private static final int NODE_DISTANCE = 15;
    private static final int REQUIRED_CALIBRATIONS =
            DeepCalibrationNodeBlock.Calibration.values().length;
    private static final int BOSS_RANGE_H = 32;
    private static final int BOSS_RANGE_V = 16;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public GravityCoreAltarBlock(Properties properties) {
        super(properties);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BossRechallengeAltarBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return interact(state, level, pos, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        interact(state, level, pos, player, hitResult);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos,
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
            reject(serverPlayer, level, pos, "message.unknown_echoes.gravity_core.wrong_dimension");
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canEnterDeepEchoes(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.gravity_core.need_entry");
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canStartVoidProphet(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.gravity_core.need_routes");
            return InteractionResult.CONSUME;
        }
        if (!DeepAltarScanLimiter.allow(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        int completedCalibrations = countCompletedCalibrations(serverPlayer, level, pos);
        if (completedCalibrations < REQUIRED_CALIBRATIONS) {
            guideIncompleteCalibrations(serverLevel, pos, serverPlayer);
            reject(serverPlayer, level, pos, "message.unknown_echoes.gravity_core.need_calibration",
                    REQUIRED_CALIBRATIONS - completedCalibrations);
            return InteractionResult.CONSUME;
        }
        if (rejectDuringRechallengeCooldown(serverLevel, state, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.GRAVITY_ECHO)) {
            summonOrJoinProphet(serverLevel, pos, serverPlayer);
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(
                serverPlayer, EchoPermission.VOID_PROPHET_ID);
        if (!ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.GRAVITY_ECHO)) {
            ResonanceTokenManager.grantRitual(serverPlayer, EchoAbilityType.GRAVITY_ECHO);
            if (bossDefeated) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.gravity_core.ready"), true);
            } else {
                summonOrJoinProphet(serverLevel, pos, serverPlayer);
            }
            return InteractionResult.CONSUME;
        }

        if (!ResonanceTokenManager.hasValidToken(serverPlayer, EchoAbilityType.GRAVITY_ECHO)) {
            ResonanceTokenManager.reissueIfMissing(serverPlayer, EchoAbilityType.GRAVITY_ECHO);
            return InteractionResult.CONSUME;
        }
        if (!bossDefeated) {
            summonOrJoinProphet(serverLevel, pos, serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.GRAVITY_TRIAL_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        if (ResonanceTokenManager.consumeToken(serverPlayer, EchoAbilityType.GRAVITY_ECHO)
                && EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.GRAVITY_ECHO)) {
            RewardAltarBlock.playAbsorbCeremony(level, pos);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean rejectDuringRechallengeCooldown(ServerLevel level, BlockState state,
                                                            BlockPos pos, ServerPlayer player) {
        long remaining = BossRechallengeAltarBlockEntity.getOrCreate(level, pos, state)
                .remainingCooldownTicks(level);
        if (remaining <= 0L) {
            return false;
        }
        long seconds = (remaining + 19L) / 20L;
        reject(player, level, pos, "message.unknown_echoes.gravity_core.rechallenge_cooldown", seconds);
        return true;
    }

    static boolean hasCompletedCalibrations(ServerPlayer player, Level level, BlockPos center) {
        return countCompletedCalibrations(player, level, center) >= REQUIRED_CALIBRATIONS;
    }

    private static int countCompletedCalibrations(ServerPlayer player, Level level, BlockPos center) {
        EnumSet<DeepCalibrationNodeBlock.Calibration> completed =
                EnumSet.noneOf(DeepCalibrationNodeBlock.Calibration.class);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos cursor = center.relative(direction, NODE_DISTANCE);
            if (!level.hasChunkAt(cursor)) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof DeepCalibrationNodeBlock)) {
                continue;
            }
            DeepCalibrationNodeBlock.Calibration calibration =
                    state.getValue(DeepCalibrationNodeBlock.CALIBRATION);
            if (DeepCalibrationNodeBlock.isActivated(player, level, cursor, calibration)) {
                completed.add(calibration);
            }
        }
        return completed.size();
    }

    private static void guideIncompleteCalibrations(ServerLevel level, BlockPos center,
                                                    ServerPlayer player) {
        Vec3 altar = Vec3.atCenterOf(center).add(0.0D, 0.65D, 0.0D);
        BossFx.guidanceMarker(level, altar, ParticleTypes.ELECTRIC_SPARK);
        for (BlockPos nodePos : findCalibrationNodes(level, center)) {
            BlockState nodeState = level.getBlockState(nodePos);
            DeepCalibrationNodeBlock.Calibration calibration =
                    nodeState.getValue(DeepCalibrationNodeBlock.CALIBRATION);
            if (DeepCalibrationNodeBlock.isActivated(player, level, nodePos, calibration)) {
                continue;
            }
            var particle = switch (calibration) {
                case WIND -> ParticleTypes.CLOUD;
                case TIDE -> ParticleTypes.SPLASH;
                case TRUE_SIGHT -> ParticleTypes.ENCHANT;
            };
            Vec3 node = Vec3.atCenterOf(nodePos).add(0.0D, 0.45D, 0.0D);
            BossFx.guidanceMarker(level, node, particle);
            BossFx.guidanceFlow(level, altar, node,
                    ParticleTypes.GLOW, particle, level.getGameTime());
        }
    }

    @Nullable
    static BlockPos findCoreNear(Level level, BlockPos nodePos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = nodePos.relative(direction, NODE_DISTANCE);
            if (level.hasChunkAt(candidate)
                    && level.getBlockState(candidate).getBlock() instanceof GravityCoreAltarBlock
                    && findCalibrationNodes(level, candidate).size()
                    == DeepCalibrationNodeBlock.Calibration.values().length) {
                return candidate;
            }
        }
        return null;
    }

    public static List<BlockPos> findCalibrationNodes(Level level, BlockPos center) {
        List<BlockPos> nodes = new ArrayList<>(3);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = center.relative(direction, NODE_DISTANCE);
            if (level.hasChunkAt(candidate)
                    && level.getBlockState(candidate).getBlock() instanceof DeepCalibrationNodeBlock) {
                nodes.add(candidate);
            }
        }
        return nodes;
    }

    private static void summonOrJoinProphet(ServerLevel level, BlockPos altarPos, ServerPlayer player) {
        BossRechallengeAltarBlockEntity altar = BossRechallengeAltarBlockEntity.getOrCreate(
                level, altarPos, level.getBlockState(altarPos));
        if (altar.findActiveBoss(level) instanceof VoidProphet active) {
            active.registerParticipant(player);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_core.boss_active"), true);
            return;
        }
        if (!areArenaChunksLoaded(level, altarPos)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_core.arena_loading"), true);
            return;
        }
        List<VoidProphet> recovered = level.getEntitiesOfClass(VoidProphet.class,
                new AABB(altarPos).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H),
                boss -> boss.isAlive() && !boss.isRemoved()
                        && altarPos.equals(boss.deepRechallengeAltar()));
        if (!recovered.isEmpty()) {
            VoidProphet owner = recovered.stream()
                    .filter(boss -> altar.ownsBoss(boss.getUUID()))
                    .findFirst()
                    .orElse(altar.canAdoptUnownedBoss() ? recovered.getFirst() : null);
            for (VoidProphet boss : recovered) {
                if (boss != owner) {
                    boss.discard();
                }
            }
            if (owner != null) {
                altar.replaceActiveBoss(owner.getUUID());
                owner.registerParticipant(player);
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.gravity_core.boss_active"), true);
                return;
            }
        }
        if (altar.hasActiveBoss()) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_core.boss_active"), true);
            return;
        }

        VoidProphet prophet = ModEntities.VOID_PROPHET.get().create(level);
        if (prophet == null) {
            reject(player, level, altarPos, "message.unknown_echoes.gravity_core.summon_failed");
            return;
        }
        BlockPos spawnPos = findSpawnPos(level, altarPos);
        if (spawnPos == null) {
            prophet.discard();
            reject(player, level, altarPos, "message.unknown_echoes.gravity_core.summon_failed");
            return;
        }
        prophet.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, prophet)) {
            prophet.discard();
            reject(player, level, altarPos, "message.unknown_echoes.gravity_core.summon_failed");
            return;
        }
        prophet.setPersistenceRequired();
        prophet.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED, null);
        prophet.bindArena(altarPos);
        prophet.registerParticipant(player);
        if (!altar.reserveBoss(prophet.getUUID())) {
            prophet.discard();
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.gravity_core.boss_active"), true);
            return;
        }
        if (!level.addFreshEntity(prophet)) {
            prophet.discard();
            altar.releaseBossReservation(prophet.getUUID());
            reject(player, level, altarPos, "message.unknown_echoes.gravity_core.summon_failed");
            return;
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                spawnPos.getX() + 0.5, spawnPos.getY() + 2.5, spawnPos.getZ() + 0.5,
                120, 1.5, 2.5, 1.5, 0.25);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 1.8F, 0.75F);
    }

    /** 落点判据由 EntitySpawnSafety.findAltarSpawnPos 统一持有;两座祭坛与其 Boss 归位共用。 */
    @Nullable
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos) {
        return EntitySpawnSafety.findAltarSpawnPos(level, altarPos);
    }

    private static boolean areArenaChunksLoaded(ServerLevel level, BlockPos altarPos) {
        return LoadedAreaCheck.horizontalRadiusLoaded(level, altarPos, BOSS_RANGE_H);
    }

    private static void reject(ServerPlayer player, Level level, BlockPos pos, String messageKey,
                               Object... args) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, 0.55F);
        player.displayClientMessage(Component.translatable(messageKey, args), true);
    }
}
