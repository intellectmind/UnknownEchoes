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
    private static final int[][] ANCHOR_OFFSETS = {
            {0, 0}, {-5, 9}, {5, 9}
    };
    private static final int[][] CORE_RELATIVE_OFFSETS = {
            {0, -5}, {-5, 4}, {5, 4}
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
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)
                || !EchoPermission.canUseGravityMechanism(serverPlayer)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.45F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.locked"), true);
            EchoAbilityManager.recordFailure(serverPlayer, EchoAbilityType.GRAVITY_ECHO,
                    "space_anchor_locked");
            return InteractionResult.CONSUME;
        }
        if (EchoPermission.hasVoidRouteReady(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.route_ready"), true);
            return InteractionResult.CONSUME;
        }

        String key = mechanismKey(level, pos);
        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.space_anchor.already"), true);
            return InteractionResult.CONSUME;
        }
        int activatedBefore = countActivatedNearby(serverPlayer, level, pos);
        boolean completed = activatedBefore + 1 >= REQUIRED_ANCHORS;
        boolean recorded = completed
                ? EchoAbilityManager.activateMechanisms(serverPlayer, key,
                        EchoPermission.VOID_ROUTE_READY_KEY)
                : EchoAbilityManager.activateMechanism(serverPlayer, key);
        if (!recorded) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return InteractionResult.CONSUME;
        }
        int activated = countActivatedNearby(serverPlayer, level, pos);
        EchoAbilityManager.syncToClient(serverPlayer);
        playFeedback(serverLevel, pos, completed);
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
        for (int rotation = 0; rotation < 4; rotation++) {
            List<BlockPos> anchors = new ArrayList<>(REQUIRED_ANCHORS);
            for (int[] offset : CORE_RELATIVE_OFFSETS) {
                int x = offset[0];
                int z = offset[1];
                for (int step = 0; step < rotation; step++) {
                    int rotatedX = -z;
                    z = x;
                    x = rotatedX;
                }
                BlockPos candidate = corePos.offset(x, 0, z);
                if (level.getBlockState(candidate).getBlock() instanceof SpaceAnchorBlock) {
                    anchors.add(candidate);
                }
            }
            if (anchors.size() == REQUIRED_ANCHORS) {
                return anchors;
            }
        }
        return List.of();
    }

    private static int countActivatedNearby(ServerPlayer player, Level level, BlockPos center) {
        int best = 1;
        // 当前节点可能对应结构中的任一锚点，枚举四种结构旋转并校验完整三角布局。
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int[] origin : ANCHOR_OFFSETS) {
                int count = 0;
                for (int[] target : ANCHOR_OFFSETS) {
                    int x = target[0] - origin[0];
                    int z = target[1] - origin[1];
                    for (int step = 0; step < rotation; step++) {
                        int rotatedX = -z;
                        z = x;
                        x = rotatedX;
                    }
                    BlockPos cursor = center.offset(x, 0, z);
                    if (level.getBlockState(cursor).getBlock() instanceof SpaceAnchorBlock
                            && EchoAbilityManager.hasActivatedMechanism(player, mechanismKey(level, cursor))) {
                        count++;
                    }
                }
                best = Math.max(best, count);
            }
        }
        return best;
    }

    private static void playFeedback(ServerLevel level, BlockPos pos, boolean completed) {
        level.sendParticles(completed ? ParticleTypes.SCULK_SOUL : ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                completed ? 60 : 28, 0.45, 0.7, 0.45, 0.06);
        level.playSound(null, pos, completed ? SoundEvents.RESPAWN_ANCHOR_SET_SPAWN
                        : SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, completed ? 1.25F : 0.9F, completed ? 0.75F : 1.15F);
    }
}
