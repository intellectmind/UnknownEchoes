package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.JourneyManager;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class WonderPuzzleCoreBlockEntity extends BlockEntity {
    private int theme = -1;
    private int[] values = new int[0];
    private long[] inputs = new long[0];
    private long[] gates = new long[0];
    private long[] trials = new long[0];
    private int selected = -1;
    private UUID selectingPlayer;
    // 邻块通知可同步回调交互，保护范围须覆盖显示、完成恢复和研习派发。
    private boolean interacting;
    private boolean completing;
    private boolean openingReward;

    public WonderPuzzleCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WONDER_PUZZLE_CORE.get(), pos, state);
    }

    public void configure(int theme, List<BlockPos> inputs, List<BlockPos> gates) {
        this.theme = theme;
        this.values = WonderPuzzleLogic.initial(theme);
        this.inputs = inputs.stream().mapToLong(BlockPos::asLong).toArray();
        this.gates = gates.stream().mapToLong(BlockPos::asLong).toArray();
        setChanged();
    }

    public boolean accepts(BlockPos pos) {
        return Arrays.stream(inputs).anyMatch(value -> value == pos.asLong());
    }

    public void bindTrials(List<BlockPos> positions) {
        trials = positions.stream().mapToLong(BlockPos::asLong).toArray();
        setChanged();
    }

    private boolean isCurrentCore() {
        return level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ModBlocks.WONDER_PUZZLE_CORE.get());
    }

    private boolean isEligiblePlayer(ServerPlayer player) {
        return level != null && player.isAlive() && !player.hasDisconnected() && player.level() == level
                && EchoPermission.canUseEchoMechanism(player);
    }

    private boolean ready(ServerPlayer player) {
        if (completing || openingReward || !isCurrentCore() || !isEligiblePlayer(player)
                || theme < 0 || inputs.length != values.length || gates.length != 6
                || !WonderPuzzleLogic.valid(theme, values)) return false;
        if (getBlockState().getValue(PuzzleCoreBlock.ACTIVE) || WonderPuzzleLogic.solved(theme, values)) {
            if (!getBlockState().getValue(PuzzleCoreBlock.ACTIVE) && !updateDisplays(player)) return false;
            completePuzzle(player);
            return false;
        }
        for (long pos : inputs) {
            if (!level.hasChunkAt(BlockPos.of(pos))
                    || !level.getBlockState(BlockPos.of(pos)).is(ModBlocks.WONDER_PUZZLE_DIAL.get())) {
                sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.unloaded"), true);
                return false;
            }
        }
        for (long pos : gates) {
            if (!level.hasChunkAt(BlockPos.of(pos))) {
                sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.unloaded"), true);
                return false;
            }
        }
        return true;
    }

    public void inspect(ServerPlayer player) {
        if (interacting) return;
        interacting = true;
        try {
            inspectBoard(player);
        } finally {
            interacting = false;
        }
    }

    private void inspectBoard(ServerPlayer player) {
        if (completing || openingReward || !isCurrentCore() || !isEligiblePlayer(player)) return;
        if (getBlockState().getValue(PuzzleCoreBlock.ACTIVE) && studyAvailable(player, theme)) {
            if (!openReward(player)) return;
            if (player.isShiftKeyDown()) {
                cn.kurt6.unknown_echoes.study.WorkshopStudyManager.open(player, this, theme);
            } else sendFeedback(player, Component.translatable("study.unknown_echoes.enter"), true);
            return;
        }
        if (!ready(player)) return;
        if (player.isShiftKeyDown()) {
            values = WonderPuzzleLogic.initial(theme);
            selected = -1;
            selectingPlayer = null;
            setChanged();
            if (!updateDisplays(player)) return;
            if (!sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.reset"), true)) return;
        }
        String rule = switch (WonderPuzzleLogic.mode(theme)) {
            case WonderPuzzleLogic.WATER -> "message.unknown_echoes.wonder.water.rule";
            case WonderPuzzleLogic.OPTICS -> "message.unknown_echoes.wonder.optics.rule";
            case WonderPuzzleLogic.BALANCE -> "message.unknown_echoes.wonder.balance.rule";
            default -> "message.unknown_echoes.wonder.gears.rule";
        };
        if (!sendFeedback(player, Component.translatable(rule), false)) return;
        String goal = switch (WonderPuzzleLogic.mode(theme)) {
            case WonderPuzzleLogic.WATER -> "message.unknown_echoes.wonder.water.goal";
            case WonderPuzzleLogic.OPTICS -> "message.unknown_echoes.wonder.optics.goal";
            case WonderPuzzleLogic.BALANCE -> "message.unknown_echoes.wonder.balance.goal";
            default -> "message.unknown_echoes.wonder.gears.goal";
        };
        if (!sendFeedback(player, Component.translatable(goal), false)) return;
        if (!sendFeedback(player, Component.translatable(WonderPuzzleLogic.mode(theme) == WonderPuzzleLogic.WATER
                ? "message.unknown_echoes.wonder.water.controls" : "message.unknown_echoes.wonder.controls"), false)) return;
        if (!updateDisplays(player)) return;
        showStatus(player);
        showBeams(player);
    }

    public void activate(BlockPos pos, ServerPlayer player) {
        if (interacting) return;
        interacting = true;
        try {
            activateInput(pos, player);
        } finally {
            interacting = false;
        }
    }

    private void activateInput(BlockPos pos, ServerPlayer player) {
        if (!ready(player) || !accepts(pos)) return;
        int index = -1;
        for (int i = 0; i < inputs.length; i++) if (inputs[i] == pos.asLong()) index = i;
        if (WonderPuzzleLogic.mode(theme) == WonderPuzzleLogic.WATER) {
            // 取水与倒水属于同一次玩家操作，其他玩家不能沿用未完成的取水选择。
            if (selected < 0 || !player.getUUID().equals(selectingPlayer)) {
                selected = index;
                selectingPlayer = player.getUUID();
                if (!sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.water.selected", index + 1), true)) return;
                updateDisplays(player);
                return;
            }
            values = WonderPuzzleLogic.pour(theme, values, selected, index);
            selected = -1;
            selectingPlayer = null;
        } else {
            values = WonderPuzzleLogic.turn(theme, values, index, player.isShiftKeyDown());
        }
        setChanged();
        if (!updateDisplays(player)) return;
        level.playSound(null, pos, ModSounds.BLOCK_WONDER_CORE_INPUT.get(),
                SoundSource.BLOCKS, 0.7F, 0.8F + 0.05F * Math.clamp(index, 0, 7));
        showStatus(player);
        showBeams(player);
        if (WonderPuzzleLogic.solved(theme, values)) {
            completePuzzle(player);
        }
    }

    private void completePuzzle(ServerPlayer player) {
        if (completing || !isEligiblePlayer(player) || !isCurrentCore() || !WonderPuzzleLogic.solved(theme, values)) return;
        completing = true;
        try {
            BlockState state = level.getBlockState(worldPosition);
            if (!state.is(ModBlocks.WONDER_PUZZLE_CORE.get())) return;
            boolean active = state.getValue(PuzzleCoreBlock.ACTIVE);
            if (!active) {
                if (!level.setBlock(worldPosition, state.setValue(PuzzleCoreBlock.ACTIVE, true), 3)) return;
                if (!isCurrentCore() || !isEligiblePlayer(player)) return;
                BlockState committed = level.getBlockState(worldPosition);
                if (!committed.is(ModBlocks.WONDER_PUZZLE_CORE.get())
                        || !committed.getValue(PuzzleCoreBlock.ACTIVE)) return;
            }
            openReward(player);
            if (!isCurrentCore() || !isEligiblePlayer(player)) return;
            if (!active) {
                level.playSound(null, worldPosition, ModSounds.BLOCK_WONDER_CORE_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } finally {
            completing = false;
        }
    }

    public boolean studyAvailable(ServerPlayer player, int expectedTheme) {
        if (completing || openingReward || !isCurrentCore() || !(level instanceof ServerLevel server) || theme != expectedTheme
                || !isEligiblePlayer(player) || gates.length != 6 || inputs.length != values.length
                || !getBlockState().getValue(PuzzleCoreBlock.ACTIVE) || !WonderPuzzleLogic.solved(theme, values)) return false;
        for (long encoded : trials) if (!WonderTrialData.get(server).complete(BlockPos.of(encoded))) return false;
        for (long encoded : gates) if (!server.hasChunkAt(BlockPos.of(encoded))) return false;
        return true;
    }

    private boolean openReward(ServerPlayer player) {
        if (openingReward || !isEligiblePlayer(player) || !isCurrentCore() || !(level instanceof ServerLevel server)) return false;
        openingReward = true;
        try {
            for (long encoded : trials) {
                BlockPos pos = BlockPos.of(encoded);
                if (!WonderTrialData.get(server).complete(pos)) {
                    WonderTrialBlockEntity.point(player, "message.unknown_echoes.wonder.trial.reward", pos);
                    return false;
                }
            }
            for (long encoded : gates) {
                if (!server.hasChunkAt(BlockPos.of(encoded))) {
                    sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.unloaded"), true);
                    return false;
                }
            }
            // 只打开本核心绑定的锚定门；按半径清封印会误开邻室或其他楼层。
            for (long encoded : gates) {
                BlockPos gate = BlockPos.of(encoded);
                if (!isCurrentCore() || !server.hasChunkAt(gate)) return false;
                if (!isEligiblePlayer(player)) return false;
                if (server.getBlockState(gate).is(ModBlocks.ANCHORED_SEALED_STONE.get())) {
                    if (!server.setBlock(gate, Blocks.AIR.defaultBlockState(), 3)) return false;
                }
            }
            if (!isCurrentCore() || !isEligiblePlayer(player)) return false;
            for (long encoded : gates) {
                BlockPos gate = BlockPos.of(encoded);
                if (!server.hasChunkAt(gate) || server.getBlockState(gate).is(ModBlocks.ANCHORED_SEALED_STONE.get())) return false;
            }
            String kind = switch (WonderPuzzleLogic.mode(theme)) {
                case WonderPuzzleLogic.WATER -> "water";
                case WonderPuzzleLogic.OPTICS -> "optics";
                case WonderPuzzleLogic.BALANCE -> "balance";
                default -> "gears";
            };
            JourneyManager.recordPuzzle(player, worldPosition, kind);
            if (!isCurrentCore() || !isEligiblePlayer(player)) return false;
            return sendFeedback(player, Component.translatable("message.unknown_echoes.wonder.solved"), true);
        } finally {
            openingReward = false;
        }
    }

    private boolean sendFeedback(ServerPlayer player, Component message, boolean actionBar) {
        if (!isCurrentCore() || !isEligiblePlayer(player)) return false;
        player.displayClientMessage(message, actionBar);
        return isCurrentCore() && isEligiblePlayer(player);
    }

    private void showStatus(ServerPlayer player) {
        if (!isCurrentCore() || !isEligiblePlayer(player)) return;
        int[] target = WonderPuzzleLogic.target(theme);
        Component status = switch (WonderPuzzleLogic.mode(theme)) {
            case WonderPuzzleLogic.WATER -> Component.translatable("message.unknown_echoes.wonder.water.status",
                    Arrays.toString(WonderPuzzleLogic.capacities(theme)), Arrays.toString(values), Arrays.toString(target));
            case WonderPuzzleLogic.OPTICS -> Component.translatable("message.unknown_echoes.wonder.optics.status",
                    WonderPuzzleLogic.trace(values, 0).exit() == target[0] ? 1 : 0,
                    WonderPuzzleLogic.trace(values, 2).exit() == target[1] ? 1 : 0,
                    WonderPuzzleLogic.illuminatedCount(values));
            case WonderPuzzleLogic.BALANCE -> Component.translatable("message.unknown_echoes.wonder.balance.status",
                    Arrays.toString(WonderPuzzleLogic.weights(theme)), WonderPuzzleLogic.moments(theme, values)[0],
                    WonderPuzzleLogic.moments(theme, values)[1], target[0]);
            default -> Component.translatable("message.unknown_echoes.wonder.gears.status", Arrays.toString(values));
        };
        sendFeedback(player, status, true);
    }

    private boolean updateDisplays(ServerPlayer player) {
        boolean[] lit = WonderPuzzleLogic.mode(theme) == WonderPuzzleLogic.OPTICS
                ? WonderPuzzleLogic.illuminated(values) : new boolean[values.length];
        for (int i = 0; i < inputs.length; i++) {
            BlockPos pos = BlockPos.of(inputs[i]);
            if (!isEligiblePlayer(player)) return false;
            if (!isCurrentCore() || !level.hasChunkAt(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.WONDER_PUZZLE_DIAL.get())) return false;
            int displayed = values[i];
            if (WonderPuzzleLogic.mode(theme) == WonderPuzzleLogic.OPTICS && displayed != 0) {
                BlockPos across = BlockPos.of(inputs[1]).subtract(BlockPos.of(inputs[0]));
                BlockPos along = BlockPos.of(inputs[3]).subtract(BlockPos.of(inputs[0]));
                int sign = displayed == 1 ? -1 : 1;
                displayed = (across.getX() + sign * along.getX()) * (across.getZ() + sign * along.getZ()) < 0 ? 1 : 2;
            }
            BlockState display = state.setValue(WonderPuzzleDialBlock.VALUE, displayed)
                    .setValue(WonderPuzzleDialBlock.LIT, i == selected || lit[i]);
            if (state.equals(display)) continue;
            if (!level.setBlock(pos, display, 3)) return false;
            if (!isCurrentCore() || !isEligiblePlayer(player) || !level.hasChunkAt(pos) || !level.getBlockState(pos).equals(display)) return false;
        }
        return true;
    }

    private void showBeams(ServerPlayer player) {
        if (!isCurrentCore() || !isEligiblePlayer(player)
                || !(level instanceof ServerLevel server) || WonderPuzzleLogic.mode(theme) != WonderPuzzleLogic.OPTICS) return;
        BlockPos origin = BlockPos.of(inputs[0]);
        BlockPos across = BlockPos.of(inputs[1]).subtract(origin);
        BlockPos along = BlockPos.of(inputs[3]).subtract(origin);
        for (int ray = 0; ray < 2; ray++) {
            var path = WonderPuzzleLogic.trace(values, ray * 2).path();
            DustParticleOptions dust = new DustParticleOptions(ray == 0
                    ? new Vector3f(1.0F, 0.65F, 0.15F) : new Vector3f(0.15F, 0.8F, 1.0F), 0.8F);
            for (int step = 1; step < path.size(); step++) {
                int[] a = path.get(step - 1);
                int[] b = path.get(step);
                for (int sample = 0; sample < 4; sample++) {
                    double t = sample / 4.0D;
                    double x = a[0] + (b[0] - a[0]) * t;
                    double z = a[1] + (b[1] - a[1]) * t;
                    if (!isCurrentCore() || !isEligiblePlayer(player)) return;
                    server.sendParticles(dust, origin.getX() + 0.5D + across.getX() * x + along.getX() * z,
                            origin.getY() + 1.15D, origin.getZ() + 0.5D + across.getZ() * x + along.getZ() * z,
                            1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Theme", theme);
        tag.putIntArray("Values", values);
        tag.putLongArray("Inputs", inputs);
        tag.putLongArray("Gates", gates);
        tag.putLongArray("Trials", trials);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        theme = tag.contains("Theme") ? tag.getInt("Theme") : -1;
        values = tag.getIntArray("Values");
        inputs = tag.getLongArray("Inputs");
        gates = tag.getLongArray("Gates");
        trials = tag.getLongArray("Trials");
        selected = -1;
        selectingPlayer = null;
        if (!WonderPuzzleLogic.valid(theme, values) || inputs.length != values.length || gates.length != 6
                || (trials.length != 0 && trials.length != 2)
                || Arrays.stream(trials).distinct().count() != trials.length
                || Arrays.stream(trials).anyMatch(p -> BlockPos.of(p).distSqr(worldPosition) > 256 * 256)
                || Arrays.stream(inputs).distinct().count() != inputs.length
                || Arrays.stream(gates).distinct().count() != gates.length
                || java.util.stream.LongStream.concat(Arrays.stream(inputs), Arrays.stream(gates))
                    .anyMatch(p -> BlockPos.of(p).distSqr(worldPosition) > 180)) {
            theme = -1;
            inputs = new long[0];
            gates = new long[0];
            values = new int[0];
        }
    }
}
