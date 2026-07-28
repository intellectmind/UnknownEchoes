package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.BossFx;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import cn.kurt6.unknown_echoes.registry.ModEntities;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 虚空脉室核心：管理虚空信物、裂隙看守者战斗与虚空回响授予。 */
public class VoidCoreAltarBlock extends Block {
    public static final MapCodec<VoidCoreAltarBlock> CODEC = simpleCodec(VoidCoreAltarBlock::new);

    private static final int[][] STABILIZER_OFFSETS = {
            {0, -13}, {-11, 7}, {11, 7}
    };
    private static final int REQUIRED_STABILIZERS = 3;
    private static final int BOSS_RANGE_H = 34;
    private static final int BOSS_RANGE_V = 16;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public VoidCoreAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_core.wrong_dimension");
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canUseGravityMechanism(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_core.need_gravity");
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.hasVoidRouteReady(serverPlayer)) {
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_core.need_anchor_array");
            return InteractionResult.CONSUME;
        }
        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            if (!EchoAbilityManager.hasActivatedMechanism(serverPlayer, EchoPermission.VOID_TRIAL_KEY)) {
                EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.VOID_TRIAL_KEY);
                EchoAbilityManager.syncToClient(serverPlayer);
            }
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_core.claimed"), true);
            return InteractionResult.CONSUME;
        }
        if (!DeepAltarScanLimiter.allow(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        int completedStabilizers = countActivatedStabilizers(serverPlayer, level, pos);
        if (completedStabilizers < REQUIRED_STABILIZERS) {
            guideIncompleteStabilizers(serverLevel, pos, serverPlayer);
            reject(serverPlayer, level, pos, "message.unknown_echoes.void_core.need_stabilizers",
                    REQUIRED_STABILIZERS - completedStabilizers);
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(
                serverPlayer, EchoPermission.RIFT_WARDEN_ID);
        if (!ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            ResonanceTokenManager.grantRitual(serverPlayer, EchoAbilityType.VOID_ECHO);
            if (bossDefeated) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.void_core.ready"), true);
            } else {
                summonOrJoinWarden(serverLevel, pos, serverPlayer);
            }
            return InteractionResult.CONSUME;
        }

        if (!ResonanceTokenManager.hasValidToken(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            ResonanceTokenManager.reissueIfMissing(serverPlayer, EchoAbilityType.VOID_ECHO);
            return InteractionResult.CONSUME;
        }
        if (!bossDefeated) {
            summonOrJoinWarden(serverLevel, pos, serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (ResonanceTokenManager.consumeToken(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            RewardAltarBlock.playAbsorbCeremony(level, pos);
            EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.VOID_TRIAL_KEY);
            EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.VOID_ECHO);
        }
        return InteractionResult.CONSUME;
    }

    private static int countActivatedStabilizers(ServerPlayer player, Level level, BlockPos center) {
        int best = 0;
        for (int rotation = 0; rotation < 4; rotation++) {
            if (countPresentForRotation(level, center, rotation) == REQUIRED_STABILIZERS) {
                best = Math.max(best, countActivatedForRotation(player, level, center, rotation));
            }
        }
        return best;
    }

    private static void guideIncompleteStabilizers(ServerLevel level, BlockPos center,
                                                   ServerPlayer player) {
        Vec3 altar = Vec3.atCenterOf(center).add(0.0D, 0.65D, 0.0D);
        BossFx.guidanceMarker(level, altar, ParticleTypes.END_ROD);
        for (BlockPos stabilizerPos : findStabilizerPositions(level, center)) {
            if (VoidStabilizerBlock.isActivated(player, level, stabilizerPos)) {
                continue;
            }
            Vec3 stabilizer = Vec3.atCenterOf(stabilizerPos).add(0.0D, 0.55D, 0.0D);
            BossFx.guidanceMarker(level, stabilizer, ParticleTypes.SCULK_SOUL);
            BossFx.guidanceFlow(level, altar, stabilizer,
                    ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, level.getGameTime());
        }
    }

    static int countActivatedStabilizersNear(ServerPlayer player, Level level, BlockPos stabilizerPos) {
        int best = 0;
        // 从已点击节点反推祭坛，仅接受同一结构旋转下的完整节点组。
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int[] offset : STABILIZER_OFFSETS) {
                int[] rotated = rotateOffset(offset[0], offset[1], rotation);
                BlockPos altarPos = stabilizerPos.offset(-rotated[0], 0, -rotated[1]);
                if (level.getBlockState(altarPos).getBlock() instanceof VoidCoreAltarBlock
                        && countPresentForRotation(level, altarPos, rotation) == REQUIRED_STABILIZERS) {
                    best = Math.max(best, countActivatedForRotation(player, level, altarPos, rotation));
                }
            }
        }
        return best;
    }

    @Nullable
    static BlockPos findCoreNear(Level level, BlockPos stabilizerPos) {
        BlockPos bestCore = null;
        int bestPresent = 0;
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int[] offset : STABILIZER_OFFSETS) {
                int[] rotated = rotateOffset(offset[0], offset[1], rotation);
                BlockPos candidate = stabilizerPos.offset(-rotated[0], 0, -rotated[1]);
                if (!(level.getBlockState(candidate).getBlock() instanceof VoidCoreAltarBlock)) {
                    continue;
                }
                int present = countPresentForRotation(level, candidate, rotation);
                if (present == REQUIRED_STABILIZERS && present > bestPresent) {
                    bestPresent = present;
                    bestCore = candidate;
                }
            }
        }
        return bestCore;
    }

    public static List<BlockPos> findStabilizerPositions(Level level, BlockPos center) {
        List<BlockPos> best = List.of();
        for (int rotation = 0; rotation < 4; rotation++) {
            List<BlockPos> present = new ArrayList<>(REQUIRED_STABILIZERS);
            for (int[] offset : STABILIZER_OFFSETS) {
                int[] rotated = rotateOffset(offset[0], offset[1], rotation);
                BlockPos candidate = center.offset(rotated[0], 0, rotated[1]);
                if (level.getBlockState(candidate).getBlock() instanceof VoidStabilizerBlock) {
                    present.add(candidate);
                }
            }
            if (present.size() > best.size()) {
                best = present;
            }
        }
        return best;
    }

    private static int countPresentForRotation(Level level, BlockPos center, int rotation) {
        int present = 0;
        for (int[] offset : STABILIZER_OFFSETS) {
            int[] rotated = rotateOffset(offset[0], offset[1], rotation);
            BlockPos candidate = center.offset(rotated[0], 0, rotated[1]);
            if (level.getBlockState(candidate).getBlock() instanceof VoidStabilizerBlock) {
                present++;
            }
        }
        return present;
    }

    private static int countActivatedForRotation(ServerPlayer player, Level level,
                                                  BlockPos center, int rotation) {
        int completed = 0;
        for (int[] offset : STABILIZER_OFFSETS) {
            int[] rotated = rotateOffset(offset[0], offset[1], rotation);
            BlockPos cursor = center.offset(rotated[0], 0, rotated[1]);
            if (level.getBlockState(cursor).getBlock() instanceof VoidStabilizerBlock
                    && VoidStabilizerBlock.isActivated(player, level, cursor)) {
                completed++;
            }
        }
        return completed;
    }

    private static int[] rotateOffset(int x, int z, int rotation) {
        for (int step = 0; step < rotation; step++) {
            int rotatedX = -z;
            z = x;
            x = rotatedX;
        }
        return new int[]{x, z};
    }

    private static void summonOrJoinWarden(ServerLevel level, BlockPos altarPos, ServerPlayer player) {
        AABB arena = new AABB(altarPos).inflate(BOSS_RANGE_H, BOSS_RANGE_V, BOSS_RANGE_H);
        List<RiftWarden> active = level.getEntitiesOfClass(RiftWarden.class, arena,
                warden -> warden.isAlive() && !warden.isRemoved());
        if (!active.isEmpty()) {
            active.getFirst().registerParticipant(player);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_core.boss_active"), true);
            return;
        }

        RiftWarden warden = ModEntities.RIFT_WARDEN.get().create(level);
        if (warden == null) {
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }
        BlockPos spawnPos = findSpawnPos(level, altarPos);
        if (spawnPos == null) {
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }
        warden.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(warden)) {
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }
        warden.setPersistenceRequired();
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED, null);
        warden.bindArena(altarPos);
        warden.registerParticipant(player);
        if (!level.addFreshEntity(warden)) {
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                spawnPos.getX() + 0.5, spawnPos.getY() + 2.8, spawnPos.getZ() + 0.5,
                160, 1.8, 2.8, 1.8, 0.3);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.0F, 0.55F);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.void_core.boss_summoned"), true);
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos) {
        Direction[] directions = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        for (Direction direction : directions) {
            BlockPos candidate = altarPos.relative(direction, 8);
            BlockPos floor = candidate.below();
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above(6)).isAir()
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return candidate;
            }
        }
        return null;
    }

    private static void reject(ServerPlayer player, Level level, BlockPos pos, String messageKey,
                               Object... args) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable(messageKey, args), true);
    }
}
