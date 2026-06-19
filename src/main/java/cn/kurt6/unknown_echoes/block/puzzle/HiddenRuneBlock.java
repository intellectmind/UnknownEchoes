package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
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
import java.util.Set;

/**
 * 隐纹石砖(幻象墙):外观与回响石砖完全一致,且不可破坏(挖掘绕过无效)。
 * - 体验层:持真视回响的玩家在客户端能看到它周期性泛出微光粒子(ClientGameEvents)。
 * - 权限层:持真视回响右键 → 服务端校验后,整片相连的幻象墙一次性消散成通路。
 * 没有真视:看不出、挖不动、右键只得到一句普通石砖的描述——判定永远在服务端。
 */
public class HiddenRuneBlock extends Block {
    public static final MapCodec<HiddenRuneBlock> CODEC = simpleCodec(HiddenRuneBlock::new);

    /** 单次消散的最大连通方块数(覆盖隐藏房整面幻象墙,同时防滥用)。 */
    private static final int MAX_DISSOLVE = 64;

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
        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            // 没有真视:表现得像一块普通石砖
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.hidden_rune.plain"), true);
            return InteractionResult.CONSUME;
        }
        dissolveConnected(level, pos);
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.hidden_rune.dissolved"), true);
        // 回访点研究来源(V0.6B):每片幻象墙首次消散 +1 真视研究点(个人绑定,按起点坐标去重)
        if (player instanceof ServerPlayer serverPlayer) {
            String key = "research_revisit:hidden_rune:" + level.dimension().location() + ":" + pos.asLong();
            if (!EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
                EchoAbilityManager.activateMechanism(serverPlayer, key);
                EchoAbilityManager.addResearchPoints(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO, 1);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.research.insight"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** 广度优先消散与起点相连的所有幻象墙方块。 */
    private void dissolveConnected(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.4F, 0.8F);
        while (!queue.isEmpty() && visited.size() <= MAX_DISSOLVE) {
            BlockPos pos = queue.poll();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.size() < MAX_DISSOLVE && !visited.contains(next)
                        && level.getBlockState(next).is(this)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
    }
}
