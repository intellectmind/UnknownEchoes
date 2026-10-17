package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.world.AltarPillarScan;
import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/** 风之核心祭坛：服务端确认 Boss 击败记录和机关完成记录后，吸收绑定信物并解锁能力。 */
public class RewardAltarBlock extends Block {
    public static final MapCodec<RewardAltarBlock> CODEC = simpleCodec(RewardAltarBlock::new);

    /** 搜索本场地记忆柱的范围(格)。祭坛偏在场地一侧,水平半径要够到对角的记忆柱。 */
    private static final int PILLAR_RANGE_H = 20;
    private static final int PILLAR_RANGE_V = 6;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 11, 12),
            Block.box(2, 11, 2, 14, 15, 14));

    public RewardAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        // 右键事件缓存的状态可能已因换块或传送失效。
        if (!isCurrentInteraction(state, level, pos, serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            MainBossRechallengeData.trySummon(serverPlayer, pos,
                    MainBossRechallengeData.BossKind.FORGOTTEN_COLOSSUS);
            return InteractionResult.CONSUME;
        }

        if (!EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.FORGOTTEN_COLOSSUS_ID)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(Component.translatable("message.unknown_echoes.altar.locked"), true);
            return InteractionResult.CONSUME;
        }

        Optional<List<BlockPos>> scan = findNearbyMemoryPillars(level, pos);
        if (scan.isEmpty()) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.area_loading"), true);
            return InteractionResult.CONSUME;
        }

        boolean hasPillars = !scan.get().isEmpty();
        if (hasPillars && !ResonanceTokenManager.hasRitualRecord(serverPlayer, EchoAbilityType.WIND_ECHO)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.unknown_echoes.altar.need_ritual"), true);
            return InteractionResult.CONSUME;
        }

        if (hasPillars) {
            if (!ResonanceTokenManager.consumeToken(serverPlayer, EchoAbilityType.WIND_ECHO)) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.7F);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.altar.token_waiting"), true);
                return InteractionResult.CONSUME;
            }
        }

        // 先写入能力，再进入吸收音效的外部回调，避免已扣信物却未获得能力。
        if (EchoAbilityManager.unlockAbility(serverPlayer, EchoAbilityType.WIND_ECHO)
                && hasPillars && isCurrentInteraction(state, level, pos, serverPlayer)) {
            playAbsorbCeremony(level, pos);
        }
        return InteractionResult.CONSUME;
    }

    private Optional<List<BlockPos>> findNearbyMemoryPillars(Level level, BlockPos center) {
        return AltarPillarScan.find(level, center, PILLAR_RANGE_H, PILLAR_RANGE_V,
                state -> state.getBlock() instanceof cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock, true);
    }

    /** 在扣信物、召唤或演出前校验实际现场，不用事件前缓存的状态授权旧调用。 */
    public static boolean isCurrentInteraction(BlockState state, Level level, BlockPos pos, ServerPlayer player) {
        if (!(level instanceof ServerLevel server) || player.level() != level
                || !player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || !player.canInteractWithBlock(pos, 1.0D) || server.isOutsideBuildHeight(pos)) {
            return false;
        }
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getBlockState(pos) == state;
    }

    public static void playAbsorbCeremony(Level level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.BLOCK_REWARD_ALTAR_ABSORB.get(), SoundSource.BLOCKS, 1.3F, 1.0F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            double cx = pos.getX() + 0.5;
            double cz = pos.getZ() + 0.5;
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 12;
                double r = 0.9 - i * 0.03;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        cx + Math.cos(angle) * r, pos.getY() + 0.6 + i * 0.09, cz + Math.sin(angle) * r,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    cx, pos.getY() + 1.2, cz, 20, 0.4, 0.5, 0.4, 0.1);
        }
    }
}
