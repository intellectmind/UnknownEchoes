package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** 记忆柱:Boss 场地机关。玩家激活全部 4 根记忆柱后,遗忘巨像进入破防状态。 */
public class MemoryPillarBlock extends Block {
    public static final MapCodec<MemoryPillarBlock> CODEC = simpleCodec(MemoryPillarBlock::new);
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final double BOSS_SEARCH_RADIUS = 32.0D;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(4, 3, 4, 12, 13, 12),
            Block.box(3, 13, 3, 13, 16, 13));

    public MemoryPillarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVATED, Boolean.FALSE));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        List<ForgottenColossus> bosses = level.getEntitiesOfClass(ForgottenColossus.class,
                new AABB(pos).inflate(BOSS_SEARCH_RADIUS), ForgottenColossus::isAlive);

        if (bosses.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && cn.kurt6.unknown_echoes.ability.EchoAbilityManager.hasDefeatedBoss(serverPlayer,
                        cn.kurt6.unknown_echoes.ability.EchoPermission.FORGOTTEN_COLOSSUS_ID)
                && !cn.kurt6.unknown_echoes.ability.EchoAbilityManager.hasAbility(serverPlayer,
                        cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO)) {
            if (!cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.hasRitualRecord(serverPlayer,
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO)) {
                cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.grantRitual(serverPlayer,
                        cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO);
                return InteractionResult.CONSUME;
            }
            if (cn.kurt6.unknown_echoes.ability.ResonanceTokenManager.reissueIfMissing(serverPlayer,
                    cn.kurt6.unknown_echoes.ability.EchoAbilityType.WIND_ECHO)) {
                return InteractionResult.CONSUME;
            }
        }

        if (bosses.isEmpty()) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.5F);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.pillar.silent"), true);
            return InteractionResult.CONSUME;
        }

        bosses.get(0).onMemoryPillarClicked(pos, player);
        return InteractionResult.CONSUME;
    }
}
