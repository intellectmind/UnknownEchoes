package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.fx.ProgressionFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.RiftWarden;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 虚空脉室核心：管理虚空信物、裂隙看守者战斗与虚空回响授予。 */
public class VoidCoreAltarBlock extends BaseEntityBlock {
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
        // 右键事件缓存的状态可能已因换块或传送失效。
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
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
        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            if (backfillOwnedTrial(serverLevel, state, pos, serverPlayer)) {
                requestWardenBattle(serverLevel, state, pos, serverPlayer);
            }
            return InteractionResult.CONSUME;
        }

        boolean bossDefeated = EchoAbilityManager.hasDefeatedBoss(
                serverPlayer, EchoPermission.RIFT_WARDEN_ID);
        if (!ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            ResonanceTokenManager.grantRitual(serverPlayer, EchoAbilityType.VOID_ECHO);
            if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
                return InteractionResult.CONSUME;
            }
            if (bossDefeated) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.void_core.ready"), true);
            } else {
                requestWardenBattle(serverLevel, state, pos, serverPlayer);
            }
            return InteractionResult.CONSUME;
        }

        if (!ResonanceTokenManager.hasValidToken(serverPlayer, EchoAbilityType.VOID_ECHO)) {
            ResonanceTokenManager.reissueIfMissing(serverPlayer, EchoAbilityType.VOID_ECHO);
            return InteractionResult.CONSUME;
        }
        if (!bossDefeated) {
            requestWardenBattle(serverLevel, state, pos, serverPlayer);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.activateMechanism(serverPlayer, EchoPermission.VOID_TRIAL_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        // 个人试炼记录会检查成就；回调结束后再确认本次祭坛仍有效。
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (ResonanceTokenManager.consumeToken(serverPlayer, EchoAbilityType.VOID_ECHO)
                && EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.VOID_ECHO)
                && RewardAltarBlock.isCurrentInteraction(state, level, pos, serverPlayer)) {
            RewardAltarBlock.playAbsorbCeremony(level, pos);
        }
        return InteractionResult.CONSUME;
    }

    /** 复战冷却只限制战斗，不阻塞已完成个人条件的领取与信物补发。 */
    private static void requestWardenBattle(ServerLevel level, BlockState state, BlockPos pos,
                                             ServerPlayer player) {
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, player)
                || rejectDuringRechallengeCooldown(level, state, pos, player)
                || !RewardAltarBlock.isCurrentInteraction(state, level, pos, player)) {
            return;
        }
        summonOrJoinWarden(level, pos, player);
    }

    private static boolean backfillOwnedTrial(ServerLevel level, BlockState state, BlockPos pos,
                                               ServerPlayer player) {
        if (EchoAbilityManager.hasActivatedMechanism(player, EchoPermission.VOID_TRIAL_KEY)) {
            return true;
        }
        if (!EchoAbilityManager.activateMechanism(player, EchoPermission.VOID_TRIAL_KEY)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return false;
        }
        if (!RewardAltarBlock.isCurrentInteraction(state, level, pos, player)) {
            return false;
        }
        EchoAbilityManager.syncToClient(player);
        return RewardAltarBlock.isCurrentInteraction(state, level, pos, player)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.VOID_ECHO);
    }

    private static boolean rejectDuringRechallengeCooldown(ServerLevel level, BlockState state,
                                                            BlockPos pos, ServerPlayer player) {
        BossRechallengeAltarBlockEntity altar = BossRechallengeAltarBlockEntity.getOrCreate(level, pos, state);
        if (altar == null || !altar.isCurrentAltar(level, state)) {
            return true;
        }
        long remaining = altar.remainingCooldownTicks(level);
        if (!altar.isCurrentAltar(level, state)) {
            return true;
        }
        if (remaining <= 0L) {
            return false;
        }
        long seconds = (remaining + 19L) / 20L;
        reject(player, level, pos, "message.unknown_echoes.void_core.rechallenge_cooldown", seconds);
        return true;
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
        MechanismFx.emit(level, ProgressionFx.node(altar, ProgressionFx.Theme.VOID, false));
        for (BlockPos stabilizerPos : findStabilizerPositions(level, center)) {
            if (VoidStabilizerBlock.isActivated(player, level, stabilizerPos)) {
                continue;
            }
            Vec3 stabilizer = Vec3.atCenterOf(stabilizerPos).add(0.0D, 0.55D, 0.0D);
            MechanismFx.emit(level, ProgressionFx.guidance(stabilizer, altar, ProgressionFx.Theme.VOID));
        }
    }

    static int countActivatedStabilizersNear(ServerPlayer player, Level level, BlockPos stabilizerPos) {
        int best = 0;

        for (int rotation = 0; rotation < 4; rotation++) {
            for (int[] offset : STABILIZER_OFFSETS) {
                int[] rotated = rotateOffset(offset[0], offset[1], rotation);
                BlockPos altarPos = stabilizerPos.offset(-rotated[0], 0, -rotated[1]);
                BlockState state = DeepArrayBlockAccess.getIfReady(level, altarPos);
                if (state != null && state.getBlock() instanceof VoidCoreAltarBlock
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
                BlockState state = DeepArrayBlockAccess.getIfReady(level, candidate);
                if (state == null || !(state.getBlock() instanceof VoidCoreAltarBlock)) {
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
                BlockState state = DeepArrayBlockAccess.getIfReady(level, candidate);
                if (state != null && state.getBlock() instanceof VoidStabilizerBlock) {
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
            BlockState state = DeepArrayBlockAccess.getIfReady(level, candidate);
            if (state != null && state.getBlock() instanceof VoidStabilizerBlock) {
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
            BlockState state = DeepArrayBlockAccess.getIfReady(level, cursor);
            if (state != null && state.getBlock() instanceof VoidStabilizerBlock
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
        BlockState state = BossRechallengeAltarBlockEntity.getReadyAltarState(level, altarPos);
        if (state == null || !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        BossRechallengeAltarBlockEntity altar = BossRechallengeAltarBlockEntity.getOrCreate(
                level, altarPos, state);
        if (altar == null || !altar.isCurrentAltar(level, state) || altar.isRecoveryInProgress()) {
            return;
        }
        if (altar.findActiveBoss(level) instanceof RiftWarden active) {
            active.registerParticipant(player);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_core.boss_active"), true);
            return;
        }
        if (!areArenaChunksLoaded(level, altarPos)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_core.arena_loading"), true);
            return;
        }
        var recovery = altar.recoverBosses(level, state, RiftWarden.class, BOSS_RANGE_H, BOSS_RANGE_V);
        if (recovery.interrupted()) {
            return;
        }
        if (recovery.boss() instanceof RiftWarden owner) {
            owner.registerParticipant(player);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.void_core.boss_active"), true);
            return;
        }
        if (altar.hasActiveBoss()) {
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
            warden.discard();
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }
        warden.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, warden)) {
            warden.discard();
            reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
            return;
        }
        warden.setPersistenceRequired();
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED, null);
        warden.bindArena(altarPos);
        warden.registerParticipant(player);
        boolean spawned = false;
        try {
            if (!altar.reserveBoss(warden.getUUID())) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.void_core.boss_active"), true);
                return;
            }
            if (!altar.isCurrentAltar(level, state) || !areArenaChunksLoaded(level, altarPos)
                    || !level.addFreshEntity(warden)
                    || !warden.isAlive() || level.getEntity(warden.getUUID()) != warden
                    || player.level() != level || player.isSpectator()
                    || !altar.isCurrentAltar(level, state) || !areArenaChunksLoaded(level, altarPos)
                    || !altar.ownsBoss(warden.getUUID())) {
                reject(player, level, altarPos, "message.unknown_echoes.void_core.summon_failed");
                return;
            }
            spawned = true;
        } finally {
            // 入世或登记回调也可能抛异常；先清候选实体，再释放本次 UUID 的预留。
            if (!spawned) {
                try {
                    warden.discard();
                } finally {
                    altar.releaseBossReservation(warden.getUUID());
                }
            }
        }

        MechanismFx.emit(level, ProgressionFx.summon(warden.position(),
                warden.getBbWidth(), warden.getBbHeight(), ProgressionFx.Theme.VOID));
        level.playSound(null, spawnPos, ModSounds.BLOCK_VOID_CORE_MANIFEST.get(),
                SoundSource.HOSTILE, 1.6F, 1.0F);
    }

    /** 落点判据由 EntitySpawnSafety.findAltarSpawnPos 统一持有;两座祭坛与其 Boss 归位共用。 */
    @Nullable
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos) {
        return EntitySpawnSafety.findAltarSpawnPos(level, altarPos);
    }

    private static boolean areArenaChunksLoaded(ServerLevel level, BlockPos altarPos) {
        return BossRechallengeAltarBlockEntity.areArenaChunksReady(level, altarPos, BOSS_RANGE_H);
    }

    private static void reject(ServerPlayer player, Level level, BlockPos pos, String messageKey,
                               Object... args) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable(messageKey, args), true);
    }
}
