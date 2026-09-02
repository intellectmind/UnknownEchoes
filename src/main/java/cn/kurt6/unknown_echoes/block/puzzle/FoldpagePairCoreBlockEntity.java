package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对页核心:翻牌配对谜题。八块对页书板两两同源,翻开时以尘粒颜色短暂显形;
 * 连续翻开同色两页则永久归位(LIT),全部归位后清除周围封印石。
 */
public class FoldpagePairCoreBlockEntity extends BlockEntity {
    private static final int EFFECT_RADIUS = PuzzleSolveEffects.EFFECT_RADIUS;
    private static final int MAX_TILES = 8;
    private static final int MAX_TILE_VERTICAL_OFFSET = 4;
    private static final Vector3f[] PAIR_COLORS = {
            new Vector3f(0.30F, 0.90F, 0.90F),
            new Vector3f(0.95F, 0.80F, 0.30F),
            new Vector3f(0.85F, 0.40F, 0.90F),
            new Vector3f(0.55F, 0.90F, 0.40F),
    };

    private final List<BlockPos> tiles = new ArrayList<>();
    private int[] pairIds = new int[0];
    /** 当前已翻开等待配对的页下标;-1 = 无。会话内临时状态,不持久化。 */
    private int firstFlip = -1;

    public FoldpagePairCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOLDPAGE_PAIR_CORE.get(), pos, state);
    }

    public void configure(List<BlockPos> tilePositions, long seed) {
        if (!areValidTilePositions(tilePositions)) {
            return;
        }
        this.tiles.clear();
        tilePositions.forEach(pos -> this.tiles.add(pos.immutable()));
        this.pairIds = new int[tilePositions.size()];
        for (int i = 0; i < this.pairIds.length; i++) {
            this.pairIds[i] = i / 2;
        }
        RandomSource random = RandomSource.create(seed);
        for (int i = this.pairIds.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int value = this.pairIds[i];
            this.pairIds[i] = this.pairIds[j];
            this.pairIds[j] = value;
        }
        this.firstFlip = -1;
        this.setChanged();
    }

    /** 书板只应路由到结构生成时明确登记它的核心，避免相邻谜题串线。 */
    boolean ownsTile(BlockPos tilePos) {
        return this.tiles.contains(tilePos);
    }

    public void flip(BlockPos tilePos, ServerPlayer player) {
        Level level = this.level;
        if (level == null || level.isClientSide
                || this.getBlockState().getValue(PuzzleCoreBlock.ACTIVE)) {
            return;
        }
        int index = this.tiles.indexOf(tilePos);
        if (index < 0 || index >= this.pairIds.length) {
            return;
        }
        BlockState tileState = level.getBlockState(tilePos);
        if (!tileState.is(ModBlocks.FOLDPAGE_TILE.get())) {
            return;
        }
        if (tileState.getValue(FoldpageTileBlock.LIT)) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.foldpage.archived"), true);
            }
            return;
        }

        reveal(level, tilePos, this.pairIds[index]);
        if (this.firstFlip < 0 || this.firstFlip == index) {
            this.firstFlip = index;
            return;
        }

        int firstIndex = this.firstFlip;
        this.firstFlip = -1;
        BlockPos firstTilePos = this.tiles.get(firstIndex);
        if (!level.hasChunkAt(firstTilePos)) {
            this.firstFlip = index;
            return;
        }
        BlockState firstTileState = level.getBlockState(firstTilePos);
        if (!firstTileState.is(ModBlocks.FOLDPAGE_TILE.get())
                || firstTileState.getValue(FoldpageTileBlock.LIT)) {
            this.firstFlip = index;
            return;
        }
        if (this.pairIds[firstIndex] == this.pairIds[index]) {
            lightTile(level, firstTilePos);
            lightTile(level, tilePos);
            level.playSound(null, tilePos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.2F, 1.4F);
            if (allTilesLit(level)) {
                solve(level);
            }
        } else {
            level.playSound(null, tilePos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.9F, 0.6F);
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.foldpage.mismatch"), true);
            }
        }
    }

    public int matchedPairs() {
        Level level = this.level;
        if (level == null) {
            return 0;
        }
        int lit = 0;
        for (BlockPos tile : this.tiles) {
            if (!level.hasChunkAt(tile)) {
                continue;
            }
            BlockState state = level.getBlockState(tile);
            if (state.is(ModBlocks.FOLDPAGE_TILE.get()) && state.getValue(FoldpageTileBlock.LIT)) {
                lit++;
            }
        }
        return lit / 2;
    }

    public int totalPairs() {
        return this.pairIds.length / 2;
    }

    private void reveal(Level level, BlockPos tilePos, int pairId) {
        Vector3f color = PAIR_COLORS[Math.floorMod(pairId, PAIR_COLORS.length)];
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(color, 1.2F),
                    tilePos.getX() + 0.5D, tilePos.getY() + 1.1D, tilePos.getZ() + 0.5D,
                    16, 0.32D, 0.25D, 0.32D, 0.01D);
        }
        level.playSound(null, tilePos, SoundEvents.BOOK_PAGE_TURN,
                SoundSource.BLOCKS, 1.0F, 0.8F + 0.15F * pairId);
    }

    private void lightTile(Level level, BlockPos tilePos) {
        BlockState state = level.getBlockState(tilePos);
        if (state.is(ModBlocks.FOLDPAGE_TILE.get()) && !state.getValue(FoldpageTileBlock.LIT)) {
            level.setBlock(tilePos, state.setValue(FoldpageTileBlock.LIT, Boolean.TRUE), 3);
        }
    }

    private boolean allTilesLit(Level level) {
        for (BlockPos tile : this.tiles) {
            if (!level.hasChunkAt(tile)) {
                return false;
            }
            BlockState state = level.getBlockState(tile);
            if (!state.is(ModBlocks.FOLDPAGE_TILE.get()) || !state.getValue(FoldpageTileBlock.LIT)) {
                return false;
            }
        }
        return !this.tiles.isEmpty();
    }

    private void solve(Level level) {
        PuzzleSolveEffects.settle(level, this.worldPosition,
                this.getBlockState().setValue(PuzzleCoreBlock.ACTIVE, Boolean.TRUE));
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] positions = new long[this.tiles.size()];
        for (int i = 0; i < this.tiles.size(); i++) {
            positions[i] = this.tiles.get(i).asLong();
        }
        tag.putLongArray("Tiles", positions);
        tag.putIntArray("PairIds", this.pairIds);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tiles.clear();
        long[] savedTiles = tag.getLongArray("Tiles");
        int[] savedPairIds = tag.getIntArray("PairIds");
        if (savedTiles.length == 0 || savedTiles.length > MAX_TILES
                || savedTiles.length % 2 != 0 || savedPairIds.length != savedTiles.length) {
            this.pairIds = new int[0];
            this.firstFlip = -1;
            return;
        }
        List<BlockPos> loadedTiles = new ArrayList<>(savedTiles.length);
        for (long packed : savedTiles) {
            loadedTiles.add(BlockPos.of(packed));
        }
        if (!areValidTilePositions(loadedTiles) || !areValidPairIds(savedPairIds)) {

            this.pairIds = new int[0];
        } else {
            this.tiles.addAll(loadedTiles);
            this.pairIds = savedPairIds;
        }
        this.firstFlip = -1;
    }

    private boolean areValidTilePositions(List<BlockPos> positions) {
        if (positions.isEmpty() || positions.size() > MAX_TILES || positions.size() % 2 != 0) {
            return false;
        }
        Set<BlockPos> unique = new HashSet<>();
        for (BlockPos pos : positions) {
            long dx = (long) pos.getX() - this.worldPosition.getX();
            long dy = (long) pos.getY() - this.worldPosition.getY();
            long dz = (long) pos.getZ() - this.worldPosition.getZ();
            if (Math.abs(dx) > FoldpageTileBlock.CORE_SEARCH_RADIUS
                    || Math.abs(dy) > MAX_TILE_VERTICAL_OFFSET
                    || Math.abs(dz) > FoldpageTileBlock.CORE_SEARCH_RADIUS
                    || !unique.add(pos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areValidPairIds(int[] ids) {
        int pairCount = ids.length / 2;
        int[] counts = new int[pairCount];
        for (int id : ids) {
            if (id < 0 || id >= pairCount || ++counts[id] > 2) {
                return false;
            }
        }
        for (int count : counts) {
            if (count != 2) {
                return false;
            }
        }
        return true;
    }
}
