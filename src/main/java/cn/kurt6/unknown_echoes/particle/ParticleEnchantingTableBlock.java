package cn.kurt6.unknown_echoes.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import cn.kurt6.unknown_echoes.config.ClientConfig;

public class ParticleEnchantingTableBlock extends Block {
    public static final MapCodec<ParticleEnchantingTableBlock> CODEC = simpleCodec(ParticleEnchantingTableBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ParticleEnchantingTableBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!ClientConfig.ENABLE_ECHO_PARTICLES.get() || random.nextInt(6) != 0) return;
        double angle = random.nextDouble() * Math.PI * 2;
        level.addParticle(ParticleTypes.END_ROD, pos.getX() + .5 + Math.cos(angle) * .25,
                pos.getY() + .82, pos.getZ() + .5 + Math.sin(angle) * .25, 0, .008, 0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && !player.isSpectator()) {
            server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new ParticleEnchantingMenu(
                    id, inventory, ContainerLevelAccess.create(level, pos)), Component.translatable("container.unknown_echoes.particle_enchanting")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
