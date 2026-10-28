package cn.kurt6.unknown_echoes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BiomeGrassBlock extends TallGrassBlock {
    private static final TagKey<Block> GLOW_PLANT_GROUND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("unknown_echoes", "glow_plant_ground"));
    private final boolean growsOnDeepStone;

    public BiomeGrassBlock(Properties properties) {
        this(properties, false);
    }

    public BiomeGrassBlock(Properties properties, boolean growsOnDeepStone) {
        super(properties);
        this.growsOnDeepStone = growsOnDeepStone;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || (growsOnDeepStone && state.is(GLOW_PLANT_GROUND));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos).is(this) && state.canSurvive(level, pos);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).is(this)) return;
        popResource(level, pos, new ItemStack(this));
    }
}
