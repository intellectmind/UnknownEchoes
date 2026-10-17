package cn.kurt6.unknown_echoes.world.structure;

import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;

/** 在登记结构范围之前固定整座建筑的地面高度。 */
abstract class TerrainAnchoredStructure extends SinglePieceStructure {
    private final PieceConstructor constructor;
    private final int width;
    private final int depth;

    protected TerrainAnchoredStructure(PieceConstructor constructor, int width, int depth,
                                       StructureSettings settings) {
        super(constructor, width, depth, settings);
        this.constructor = constructor;
        this.width = width;
        this.depth = depth;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var stub = onTopOfChunkCenter(context,
                Heightmap.Types.WORLD_SURFACE_WG,
                builder -> builder.addPiece(constructor.construct(context.random(),
                        context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()))).orElseThrow();
        var position = stub.position();
        var biome = context.biomeSource().getNoiseBiome(
                net.minecraft.core.QuartPos.fromBlock(position.getX()),
                net.minecraft.core.QuartPos.fromBlock(position.getY()),
                net.minecraft.core.QuartPos.fromBlock(position.getZ()), context.randomState().sampler());
        if (!context.validBiome().test(biome)) {
            return Optional.empty();
        }
        if (!hasDryFootprint(context)) {
            return Optional.empty();
        }
        var builder = stub.getPiecesBuilder();
        for (var piece : builder.build().pieces()) {
            if (piece instanceof EchoStructurePiece echo && !echo.prepareTerrain(context)) {
                return Optional.empty();
            }
        }
        return Optional.of(new GenerationStub(stub.position(),
                com.mojang.datafixers.util.Either.right(builder)));
    }

    private boolean hasDryFootprint(GenerationContext context) {
        int x = context.chunkPos().getMinBlockX();
        int z = context.chunkPos().getMinBlockZ();
        int seaLevel = context.chunkGenerator().getSeaLevel();
        // 四角沿用原版宽深偏移，不按包围盒末格取点。
        return cornerAboveSeaLevel(context, x, z, seaLevel)
                && cornerAboveSeaLevel(context, x, z + depth, seaLevel)
                && cornerAboveSeaLevel(context, x + width, z, seaLevel)
                && cornerAboveSeaLevel(context, x + width, z + depth, seaLevel);
    }

    private static boolean cornerAboveSeaLevel(GenerationContext context, int x, int z, int seaLevel) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()) >= seaLevel;
    }
}
