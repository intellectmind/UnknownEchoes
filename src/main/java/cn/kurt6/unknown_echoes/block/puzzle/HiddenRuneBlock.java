package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 隐纹石砖(幻象墙):外观与回响石砖完全一致,且不可破坏(挖掘绕过无效)。 */
public class HiddenRuneBlock extends Block {
    public static final MapCodec<HiddenRuneBlock> CODEC = simpleCodec(HiddenRuneBlock::new);

    /** 单次消散的最大连通方块数(覆盖隐藏房整面幻象墙,同时防滥用)。 */
    private static final int MAX_DISSOLVE = 64;
    private static final ThreadLocal<Map<ServerLevel, Set<BlockPos>>> DISSOLVING =
            ThreadLocal.withInitial(java.util.IdentityHashMap::new);

    public HiddenRuneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !state.is(this)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        dissolveConnected(serverLevel, pos.immutable(), state, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private boolean isCurrentInteraction(ServerLevel level, BlockPos origin, BlockState expected,
                                         ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !player.hasDisconnected()
                && player.canInteractWithBlock(origin, 1.0D)
                && EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO)
                && isCurrentState(level, origin, expected);
    }

    private boolean isCurrentState(ServerLevel level, BlockPos pos, BlockState expected) {
        return level.hasChunkAt(pos) && level.getBlockState(pos) == expected;
    }

    /** 先固定本次连通范围，通知回调后复查现场，避免删除后来替换的方块。 */
    private void dissolveConnected(ServerLevel level, BlockPos origin, BlockState original,
                                    ServerPlayer player) {
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        found.add(origin);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (found.size() < MAX_DISSOLVE && !found.contains(next)
                        && level.hasChunkAt(next)
                        && level.getBlockState(next).is(this)) {
                    found.add(next);
                    queue.addLast(next);
                }
            }
        }
        Map<ServerLevel, Set<BlockPos>> active = DISSOLVING.get();
        Set<BlockPos> reserved = active.computeIfAbsent(level, ignored -> new HashSet<>());
        if (found.stream().anyMatch(reserved::contains)) {
            return;
        }
        reserved.addAll(found);
        try {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState expectedOrigin = original;
            for (BlockPos pos : found) {
                if (!isCurrentInteraction(level, origin, expectedOrigin, player)
                        || !isCurrentState(level, pos, original)) {
                    return;
                }
                boolean applied = level.setBlock(pos, air, 3);
                if (pos.equals(origin)) {
                    expectedOrigin = air;
                }
                if (!applied || !isCurrentState(level, pos, air)
                        || !isCurrentInteraction(level, origin, expectedOrigin, player)) {
                    return;
                }
                level.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
            }
            if (!isCompletedBatch(level, origin, player, found)) {
                return;
            }
            level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.4F, 0.8F);
            recordDissolve(level, origin, player, found);
        } finally {
            reserved.removeAll(found);
            if (reserved.isEmpty()) {
                active.remove(level);
            }
            if (active.isEmpty()) {
                DISSOLVING.remove();
            }
        }
    }

    private boolean isCompletedBatch(ServerLevel level, BlockPos origin, ServerPlayer player,
                                      Set<BlockPos> found) {
        BlockState air = Blocks.AIR.defaultBlockState();
        return isCurrentInteraction(level, origin, air, player)
                && found.stream().allMatch(pos -> isCurrentState(level, pos, air));
    }

    private void recordDissolve(ServerLevel level, BlockPos origin, ServerPlayer player, Set<BlockPos> found) {
        if (!isCompletedBatch(level, origin, player, found)) {
            return;
        }
        String key = "research_revisit:hidden_rune:" + level.dimension().location() + ":" + origin.asLong();
        if (EchoResearchManager.recordRevisit(player, EchoResearchLine.TRUE_SIGHT, key)
                && isCompletedBatch(level, origin, player, found)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.research.insight"), true);
        }
    }
}
