package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** 静默深渊前厅空间锚点。三个同场锚点均为个人进度，完成后写路线就绪记录。 */
public class SpaceAnchorBlock extends Block {
    public static final MapCodec<SpaceAnchorBlock> CODEC = simpleCodec(SpaceAnchorBlock::new);

    private static final String KEY_PREFIX = "deep_space_anchor:";
    // 局部 x/y/z 相对中央核心；紧凑布局须兼容已生成区块的个人锚点记录。
    private static final int[][][] CORE_RELATIVE_LAYOUTS = {
            {{0, 1, -5}, {-7, 1, 7}, {7, 1, 7}},
            {{0, 0, -5}, {-5, 0, 4}, {5, 0, 4}}
    };
    private static final int REQUIRED_ANCHORS = 3;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 4, 13),
            Block.box(5, 4, 5, 11, 16, 11));

    public SpaceAnchorBlock(Properties properties) {
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
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)
                || !EchoPermission.canUseGravityMechanism(serverPlayer)) {
            PlayerSoundFeedback.deny(serverPlayer);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.GRAVITY_ECHO,
                    "space_anchor_locked");
            return InteractionResult.CONSUME;
        }
        if (EchoPermission.hasVoidRouteReady(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.completed"), true);
            return InteractionResult.CONSUME;
        }

        String key = mechanismKey(level, pos);
        boolean alreadyActivated = EchoAbilityManager.hasActivatedMechanism(serverPlayer, key);
        int activatedBefore = countActivatedNearby(serverPlayer, level, pos);
        boolean completed = activatedBefore + (alreadyActivated ? 0 : 1) >= REQUIRED_ANCHORS;
        if (alreadyActivated && !completed) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.already"), true);
            return InteractionResult.CONSUME;
        }
        boolean recorded = completed
                ? EchoAbilityManager.activateMechanisms(serverPlayer, key,
                        EchoPermission.VOID_ROUTE_READY_KEY)
                : EchoAbilityManager.activateMechanism(serverPlayer, key);
        if (!recorded) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        EchoAbilityManager.syncToClient(serverPlayer);
        if (completed) {
            ModAdvancements.award(serverPlayer, ModAdvancements.SPACE_ANCHORS);
        }
        if (!isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        int activated = countActivatedNearby(serverPlayer, level, pos);
        playFeedback(serverLevel, pos, state, serverPlayer, completed);
        Component message = completed
                ? Component.translatable("message.unknown_echoes.space_anchor.completed")
                : Component.translatable("message.unknown_echoes.space_anchor.activated",
                        activated, REQUIRED_ANCHORS);
        serverPlayer.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }

    public static String mechanismKey(Level level, BlockPos pos) {
        return KEY_PREFIX + level.dimension().location() + ":"
                + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** 从静默锚阵中央重力核心反推同一结构的三座空间锚，供交互引导使用。 */
    public static List<BlockPos> findArrayAnchorsNear(Level level, BlockPos corePos) {
        for (int[][] layout : CORE_RELATIVE_LAYOUTS) {
            for (int rotation = 0; rotation < 4; rotation++) {
                List<BlockPos> anchors = arrayAnchors(level, corePos, layout, rotation);
                if (!anchors.isEmpty()) {
                    return anchors;
                }
            }
        }
        return List.of();
    }

    private static BlockPos rotatedOffset(int[] offset, int rotation) {
        int x = offset[0];
        int z = offset[2];
        for (int step = 0; step < rotation; step++) {
            int rotatedX = -z;
            z = x;
            x = rotatedX;
        }
        return new BlockPos(x, offset[1], z);
    }

    private static List<BlockPos> arrayAnchors(Level level, BlockPos corePos, int[][] layout, int rotation) {
        List<BlockPos> anchors = new ArrayList<>(REQUIRED_ANCHORS);
        for (int[] offset : layout) {
            BlockPos delta = rotatedOffset(offset, rotation);
            BlockPos candidate = corePos.offset(delta.getX(), delta.getY(), delta.getZ());
            BlockState state = DeepArrayBlockAccess.getIfReady(level, candidate);
            if (state == null || !(state.getBlock() instanceof SpaceAnchorBlock)) {
                return List.of();
            }
            anchors.add(candidate);
        }
        return anchors;
    }

    private static int countActivatedNearby(ServerPlayer player, Level level, BlockPos center) {
        int best = 1;

        for (int[][] layout : CORE_RELATIVE_LAYOUTS) {
            for (int rotation = 0; rotation < 4; rotation++) {
                for (int[] origin : layout) {
                    BlockPos delta = rotatedOffset(origin, rotation);
                    BlockPos corePos = center.offset(-delta.getX(), -delta.getY(), -delta.getZ());
                    int count = 0;
                    for (BlockPos cursor : arrayAnchors(level, corePos, layout, rotation)) {
                        if (EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, cursor))) {
                            count++;
                        }
                    }
                    best = Math.max(best, count);
                }
            }
        }
        return best;
    }

    // 交互事件或进度通知可能已改变现场；已提交记录保留，旧节点不再继续演出。
    private static boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state,
                                                ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.canInteractWithBlock(pos, 1.0D)
                && state.getBlock() instanceof SpaceAnchorBlock
                && DeepArrayBlockAccess.getIfReady(level, pos) == state;
    }

    private static void playFeedback(ServerLevel level, BlockPos pos, BlockState state,
                                     ServerPlayer player, boolean completed) {
        MechanismFx.emit(level, ProgressionFx.node(Vec3.atBottomCenterOf(pos).add(0, 1, 0),
                ProgressionFx.Theme.GRAVITY, completed));
        if (!isCurrentInteraction(level, pos, state, player)) {
            return;
        }
        if (completed) {
            level.playSound(null, pos, ModSounds.BLOCK_SPACE_ANCHOR_COMPLETE.get(),
                    SoundSource.BLOCKS, 1.1F, 1.0F);
        } else {
            level.playSound(null, pos, ModSounds.BLOCK_SPACE_ANCHOR_ACTIVATE.get(),
                    SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }
}
