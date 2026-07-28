package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class DeepBiomePlantBlock extends FlowerBlock {
    private final Supplier<? extends Block> nativeGround;

    public DeepBiomePlantBlock(Holder<MobEffect> effect, float effectSeconds, Properties properties,
            Supplier<? extends Block> nativeGround) {
        super(effect, effectSeconds, properties);
        this.nativeGround = nativeGround;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(nativeGround.get()) || super.mayPlaceOn(state, level, pos);
    }
}
