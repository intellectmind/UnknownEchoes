package cn.kurt6.unknown_echoes.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

public final class AerialSpawnRules {
    private AerialSpawnRules() {
    }

    public static boolean openAir(EntityType<? extends Mob> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!level.hasChunkAt(pos) || !level.getFluidState(pos).isEmpty()
                || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return surface > level.getMinBuildHeight() + 1 && pos.getY() >= surface
                && pos.getY() <= surface + 20 && pos.getY() < level.getMaxBuildHeight() - 2;
    }

    public static boolean hunter(EntityType<? extends Mob> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && openAir(type, level, reason, pos, random);
    }

    public static boolean nightHunter(EntityType<? extends Mob> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return hunter(type, level, reason, pos, random) && level.getLevel().getMaxLocalRawBrightness(pos) <= 7;
    }
    public static boolean nightAnimal(EntityType<? extends Mob> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return openAir(type, level, reason, pos, random) && level.getLevel().getMaxLocalRawBrightness(pos) <= 7;
    }

    public static boolean cavernHunter(EntityType<? extends Mob> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.hasChunkAt(pos)
                && pos.getY() > level.getMinBuildHeight() + 1 && pos.getY() < level.getMaxBuildHeight() - 2
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

}
