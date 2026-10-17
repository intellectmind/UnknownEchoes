package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** 水下符文:沉没圣殿与镜湖湖底的可读碑文(潮汐回响权限链)。 */
public class TideRuneBlock extends Block {
    public static final MapCodec<TideRuneBlock> CODEC = simpleCodec(TideRuneBlock::new);
    /** 被深渊观测者侵蚀:由 Boss 写入,玩家净化;只在 Boss 战期间出现。 */
    public static final BooleanProperty CORRUPTED = BooleanProperty.create("corrupted");
    /** 覆盖符文簇半径 + Boss 游荡半径,避免 Boss 走到场地边缘时净化通知漏发。 */
    private static final double WATCHER_NOTIFY_RANGE = 64.0D;
    private final Map<ServerLevel, Set<BlockPos>> purifyingRunes = new IdentityHashMap<>();

    /** 铭文文本组数(lang 键 rune.unknown_echoes.tide.<0..N-1>)。 */
    public static final int INSCRIPTION_COUNT = 4;
    private final Set<ServerPlayer> readingPlayers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public TideRuneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CORRUPTED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORRUPTED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        pos = pos.immutable();
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !isCurrentInteraction(serverLevel, pos, state, serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (state.getValue(CORRUPTED)) {
            purify(serverLevel, pos, state, serverPlayer);
            return InteractionResult.CONSUME;
        }
        readInscription(serverLevel, pos, state, serverPlayer);
        return InteractionResult.CONSUME;
    }

    private void readInscription(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (!this.readingPlayers.add(player)) {
            return;
        }
        try {
            if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO)) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.tide_rune.blurred"), true);
                return;
            }

            int index = Math.floorMod(pos.hashCode(), INSCRIPTION_COUNT);
            player.displayClientMessage(Component.translatable("rune.unknown_echoes.tide." + index), false);
            if (!canContinueReading(level, pos, state, player)) {
                return;
            }
            level.playSound(null, pos, ModSounds.BLOCK_TIDE_RUNE_READ.get(), SoundSource.BLOCKS, 0.65F, 1.0F);
            if (!canContinueReading(level, pos, state, player)) {
                return;
            }
            String key = "research_revisit:tide_rune:" + level.dimension().location() + ":" + pos.asLong();
            if (EchoResearchManager.recordRevisit(player, EchoResearchLine.TIDE, key)
                    && canContinueReading(level, pos, state, player)) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.research.insight"), true);
            }
        } finally {
            this.readingPlayers.remove(player);
        }
    }

    private boolean canContinueReading(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        return isCurrentInteraction(level, pos, state, player)
                && EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO);
    }

    private void purify(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        Set<BlockPos> active = this.purifyingRunes.computeIfAbsent(level, ignored -> new HashSet<>());
        if (!active.add(pos)) {
            return;
        }
        try {
            boolean guarded = AbyssWatcher.isRuneGuarded(level, pos);
            if (!isCurrentInteraction(level, pos, state, player)) {
                return;
            }
            if (guarded) {
                MechanismFx.emit(level, ExplorationFx.tideRune(Vec3.atBottomCenterOf(pos), false));
                if (!isCurrentInteraction(level, pos, state, player)) {
                    return;
                }
                level.playSound(null, pos, ModSounds.BLOCK_TIDE_RUNE_GUARDED.get(),
                        SoundSource.BLOCKS, 0.65F, 1.0F);
                if (isCurrentInteraction(level, pos, state, player)) {
                    player.displayClientMessage(
                            Component.translatable("message.unknown_echoes.tide_rune.guarded"), true);
                }
                return;
            }
            BlockState purified = state.setValue(CORRUPTED, Boolean.FALSE);
            try {
                if (!level.setBlock(pos, purified, 3)) {
                    return;
                }
            } finally {
                // 方块通知可能在写入之后抛异常，按最终状态结算。
                if (isCurrentInteraction(level, pos, purified, player)) {
                    notifyNearbyWatcher(level, pos, purified, player);
                }
            }
            if (!isCurrentInteraction(level, pos, purified, player)) {
                return;
            }
            level.playSound(null, pos, ModSounds.BLOCK_TIDE_RUNE_PURIFY.get(), SoundSource.BLOCKS, 0.85F, 1.0F);
            if (isCurrentInteraction(level, pos, purified, player)) {
                MechanismFx.emit(level, ExplorationFx.tideRune(Vec3.atBottomCenterOf(pos), true));
            }
        } finally {
            active.remove(pos);
            if (active.isEmpty()) {
                this.purifyingRunes.remove(level);
            }
        }
    }

    private boolean isCurrentInteraction(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (player.level() != level || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || !player.canInteractWithBlock(pos, 1.0D) || !state.is(this) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos) == state;
    }

    private void notifyNearbyWatcher(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer purifier) {
        for (AbyssWatcher watcher : level.getEntitiesOfClass(AbyssWatcher.class,
                new net.minecraft.world.phys.AABB(pos).inflate(WATCHER_NOTIFY_RANGE))) {
            if (!isCurrentInteraction(level, pos, state, purifier)) {
                return;
            }
            if (!watcher.isClone()) {
                watcher.onRunePurified(pos, purifier);
            }
        }
    }
}
