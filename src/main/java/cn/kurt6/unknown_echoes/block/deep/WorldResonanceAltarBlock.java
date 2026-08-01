package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
/** 世界共鸣祭坛：五种能力齐全后直接召唤最终 Boss，不要求额外关键物品。 */
public class WorldResonanceAltarBlock extends BaseEntityBlock {
    public static final MapCodec<WorldResonanceAltarBlock> CODEC = simpleCodec(WorldResonanceAltarBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(3, 4, 3, 13, 10, 13),
            Block.box(1, 10, 1, 15, 15, 15));

    public WorldResonanceAltarBlock(Properties properties) {
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
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.active"), true);
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.hasActivatedMechanism(serverPlayer,
                EchoPermission.WORLD_ECHO_COMPLETE_KEY)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.complete"), true);
            return InteractionResult.CONSUME;
        }
        if (EchoAbilityManager.hasDefeatedBoss(serverPlayer, EchoPermission.WORLD_ECHO_ID)) {
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

        if (!altar.beginStarting(serverLevel, serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.world_resonance.recovering"), true);
            return InteractionResult.CONSUME;
        }

        summon(serverLevel, pos, serverPlayer, altar);
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

    private static WorldResonanceAltarBlockEntity getOrCreateAltar(ServerLevel level, BlockPos pos,
                                                                    BlockState state) {
        if (level.getBlockEntity(pos) instanceof WorldResonanceAltarBlockEntity altar) {
            return altar;
        }
        // 旧版本已生成的普通方块没有 BlockEntity NBT，首次交互时就地补建事务状态。
        WorldResonanceAltarBlockEntity altar = new WorldResonanceAltarBlockEntity(pos, state);
        level.setBlockEntity(altar);
        return altar;
    }

    private static void summon(ServerLevel level, BlockPos altarPos, ServerPlayer player,
                               WorldResonanceAltarBlockEntity altar) {
        WorldEcho echo = ModEntities.WORLD_ECHO.get().create(level);
        if (echo == null) {
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return;
        }
        BlockPos spawnPos = findSpawnPos(level, altarPos);
        if (spawnPos == null) {
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return;
        }
        echo.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(echo)) {
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return;
        }
        echo.setPersistenceRequired();
        echo.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.TRIGGERED, null);
        echo.bindToAltar(altar.getArenaId(), altarPos);
        echo.registerParticipant(player);
        if (!level.addFreshEntity(echo)) {
            altar.abortStarting();
            reject(player, level, altarPos, "message.unknown_echoes.world_resonance.summon_failed");
            return;
        }
        altar.markActive(echo);

        level.sendParticles(ParticleTypes.END_ROD,
                spawnPos.getX() + 0.5, spawnPos.getY() + 3.0, spawnPos.getZ() + 0.5,
                180, 2.2, 3.0, 2.2, 0.25);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                spawnPos.getX() + 0.5, spawnPos.getY() + 3.0, spawnPos.getZ() + 0.5,
                220, 2.0, 2.8, 2.0, 0.35);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE, 2.5F, 0.8F);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.world_resonance.summoned"), true);
    }

    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos altarPos) {
        Direction[] directions = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        for (Direction direction : directions) {
            BlockPos candidate = altarPos.relative(direction, 10);
            BlockPos floor = candidate.below();
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above(7)).isAir()
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return candidate;
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
