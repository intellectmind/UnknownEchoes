package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.fx.ExplorationFx;
import cn.kurt6.unknown_echoes.fx.MechanismFx;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.HashSet;
import java.util.Set;

/** 潮汐感应门 BlockEntity:感应扫描 + 开门保持倒计时(全部服务端)。 */
public class TideSensorDoorBlockEntity extends BlockEntity {

    /** 感应半径(格)。 */
    private static final double SENSE_RANGE = 3.0D;
    /** 无权限提示半径(格)。 */
    private static final double HINT_RANGE = 2.5D;
    private static final int HINT_INTERVAL_TICKS = 80;

    private int openTicksRemaining = 0;
    private boolean ticking = false;
    private static final ThreadLocal<Map<ServerLevel, Set<BlockPos>>> CHANGING = new ThreadLocal<>();

    private record DoorSnapshot(BlockPos pos, BlockState state, TideSensorDoorBlockEntity door) {}

    public TideSensorDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIDE_SENSOR_DOOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TideSensorDoorBlockEntity door) {
        if (!(level instanceof ServerLevel serverLevel) || door.ticking
                || !door.isCurrentState(serverLevel, state) || isChanging(serverLevel, pos)) {
            return;
        }
        door.ticking = true;
        try {
            if (state.getValue(TideSensorDoorBlock.OPEN)) {
                if (door.openTicksRemaining > 0) {
                    int remaining = --door.openTicksRemaining;
                    if (remaining == 0 || remaining % 20 == 0) {
                        door.setChanged();
                    }
                    if (!door.isCurrentState(serverLevel, state) || door.openTicksRemaining != remaining) {
                        return;
                    }
                }
                if (door.openTicksRemaining <= 0) {
                    closeGroup(level, pos);
                }
                return;
            }
            long now = level.getGameTime();
            if (phase(now, pos.asLong(), 10L) != 0L) {
                return;
            }
            Vec3 center = Vec3.atCenterOf(pos);
            AABB area = new AABB(pos).inflate(SENSE_RANGE);
            boolean hintShown = false;
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area,
                    candidate -> !candidate.isSpectator()
                            && candidate.distanceToSqr(center) <= SENSE_RANGE * SENSE_RANGE)) {
                if (!door.isCurrentState(serverLevel, state)) {
                    return;
                }
                if (!isCurrentPlayer(serverLevel, player)
                        || player.distanceToSqr(center) > SENSE_RANGE * SENSE_RANGE) {
                    continue;
                }
                if (EchoPermission.canUseTideMechanism(player)) {
                    openGroup(level, pos, player, true);
                    return;
                }
                if (phase(now, pos.asLong(), HINT_INTERVAL_TICKS) == 0L
                        && player.distanceToSqr(center) <= HINT_RANGE * HINT_RANGE) {
                    if (!hintShown) {
                        MechanismFx.emit(serverLevel, ExplorationFx.tideLocked(Vec3.atBottomCenterOf(pos),
                                state.getValue(TideSensorDoorBlock.FACING).getAxis() == Direction.Axis.Z));
                        hintShown = true;
                    }
                    if (!door.isCurrentState(serverLevel, state)) {
                        return;
                    }
                    if (!isCurrentPlayer(serverLevel, player)
                            || player.distanceToSqr(center) > HINT_RANGE * HINT_RANGE) {
                        continue;
                    }
                    player.displayClientMessage(
                            Component.translatable("message.unknown_echoes.tide_door.locked"), true);
                    if (door.isCurrentState(serverLevel, state) && isCurrentPlayer(serverLevel, player)
                            && player.distanceToSqr(center) <= HINT_RANGE * HINT_RANGE) {
                        cn.kurt6.unknown_echoes.ability.EchoAbilityManager.recordFailure(
                                player, EchoAbilityType.TIDE_ECHO, "door_locked");
                    }
                }
            }
        } finally {
            door.ticking = false;
        }
    }

    private static long phase(long gameTime, long salt, long interval) {
        return Math.floorMod(Math.floorMod(gameTime, interval)
                + Math.floorMod(salt, interval), interval);
    }

    /** 右键开启保留原版交互距离，自动感应使用独立的三格范围。 */
    public static void openGroup(Level level, BlockPos origin, ServerPlayer opener) {
        openGroup(level, origin, opener, false);
    }

    private static void openGroup(Level level, BlockPos origin, ServerPlayer opener, boolean sensing) {
        if (!(level instanceof ServerLevel serverLevel) || !isCurrentOpener(serverLevel, origin, opener, sensing)) {
            return;
        }
        int hold = JournalManager.getResearchLevel(opener, EchoAbilityType.TIDE_ECHO) >= 3
                ? ServerConfig.TIDE_DOOR_HOLD_TICKS_RESEARCH3.get()
                : ServerConfig.TIDE_DOOR_HOLD_TICKS.get();
        floodSet(serverLevel, origin, true, hold, () -> isCurrentOpener(serverLevel, origin, opener, sensing));
    }

    public static void closeGroup(Level level, BlockPos origin) {
        if (level instanceof ServerLevel serverLevel) {
            floodSet(serverLevel, origin, false, 0, () -> true);
        }
    }

    static boolean isCurrentPlayer(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected();
    }

    boolean isCurrentState(ServerLevel expectedLevel, BlockState state) {
        return level == expectedLevel && !isRemoved() && expectedLevel.hasChunkAt(worldPosition)
                && state.getBlock() instanceof TideSensorDoorBlock && getBlockState() == state
                && expectedLevel.getBlockState(worldPosition) == state
                && expectedLevel.getBlockEntity(worldPosition) == this;
    }

    private static boolean isCurrentOpener(ServerLevel level, BlockPos origin, ServerPlayer player, boolean sensing) {
        return isCurrentPlayer(level, player) && EchoPermission.canUseTideMechanism(player)
                && (sensing ? player.distanceToSqr(Vec3.atCenterOf(origin)) <= SENSE_RANGE * SENSE_RANGE
                : player.canInteractWithBlock(origin, 1.0D));
    }

    private static boolean isChanging(ServerLevel level, BlockPos pos) {
        Map<ServerLevel, Set<BlockPos>> active = CHANGING.get();
        return active != null && active.containsKey(level) && active.get(level).contains(pos);
    }

    private static boolean isCurrentSnapshot(ServerLevel level, DoorSnapshot snapshot, BlockState state) {
        return snapshot.door != null && snapshot.door.isCurrentState(level, state);
    }

    private static boolean isComplete(ServerLevel level, Map<BlockPos, DoorSnapshot> found,
                                       boolean open, BooleanSupplier allowed) {
        if (!allowed.getAsBoolean()) {
            return false;
        }
        for (DoorSnapshot snapshot : found.values()) {
            if (!isCurrentSnapshot(level, snapshot, snapshot.state.setValue(TideSensorDoorBlock.OPEN, open))) {
                return false;
            }
        }
        return true;
    }

    /** 原门体和实体一起保存；临时预留覆盖写入、状态通知和表现回调。 */
    private static void floodSet(ServerLevel level, BlockPos origin, boolean open, int holdTicks,
                                  BooleanSupplier allowed) {
        Map<BlockPos, DoorSnapshot> found = new LinkedHashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty() && found.size() < TideSensorDoorBlock.MAX_CONNECTED) {
            BlockPos pos = queue.removeFirst();
            if (found.containsKey(pos) || !level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TideSensorDoorBlock)) {
                continue;
            }
            TideSensorDoorBlockEntity door = level.getBlockEntity(pos) instanceof TideSensorDoorBlockEntity value ? value : null;
            found.put(pos, new DoorSnapshot(pos, state, door));
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (level.hasChunkAt(next)) {
                    queue.addLast(next.immutable());
                }
            }
        }
        if (found.isEmpty()) {
            return;
        }
        Map<ServerLevel, Set<BlockPos>> active = CHANGING.get();
        if (active == null) {
            active = new java.util.IdentityHashMap<>();
            CHANGING.set(active);
        }
        Set<BlockPos> reserved = active.computeIfAbsent(level, ignored -> new HashSet<>());
        if (found.keySet().stream().anyMatch(reserved::contains)) {
            return;
        }
        reserved.addAll(found.keySet());
        try {
            DoorSnapshot start = found.get(origin);
            if (start == null) {
                return;
            }
            BlockState expectedOrigin = start.state;
            boolean changed = false;
            for (DoorSnapshot snapshot : found.values()) {
                if (!allowed.getAsBoolean() || !isCurrentSnapshot(level, start, expectedOrigin)
                        || !isCurrentSnapshot(level, snapshot, snapshot.state)) {
                    return;
                }
                BlockState target = snapshot.state.setValue(TideSensorDoorBlock.OPEN, open);
                int previous = snapshot.door.openTicksRemaining;
                snapshot.door.openTicksRemaining = holdTicks;
                boolean applied;
                try {
                    applied = target == snapshot.state || level.setBlock(snapshot.pos, target, 3);
                } finally {
                    // 已生效的门保留配套计时；失败只恢复仍属原现场的旧计时。
                    if (isCurrentSnapshot(level, snapshot, target)) {
                        snapshot.door.setChanged();
                    } else if (isCurrentSnapshot(level, snapshot, snapshot.state)) {
                        snapshot.door.openTicksRemaining = previous;
                    }
                }
                if (!applied || !isCurrentSnapshot(level, snapshot, target)) {
                    return;
                }
                changed |= target != snapshot.state;
                if (snapshot.pos.equals(origin)) {
                    expectedOrigin = target;
                }
            }
            if (!changed || !isComplete(level, found, open, allowed)) {
                return;
            }
            level.playSound(null, origin, open ? ModSounds.BLOCK_TIDE_DOOR_OPEN.get() : ModSounds.BLOCK_TIDE_DOOR_CLOSE.get(),
                    SoundSource.BLOCKS, open ? 0.8F : 0.75F, 1.0F);
            if (isComplete(level, found, open, allowed)) {
                MechanismFx.emit(level, ExplorationFx.tideDoor(Vec3.atBottomCenterOf(origin),
                        expectedOrigin.getValue(TideSensorDoorBlock.FACING).getAxis() == Direction.Axis.Z, open));
            }
        } finally {
            reserved.removeAll(found.keySet());
            if (reserved.isEmpty()) {
                active.remove(level);
            }
            if (active.isEmpty()) {
                CHANGING.remove();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("OpenTicks", this.openTicksRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int maximum = Math.max(ServerConfig.TIDE_DOOR_HOLD_TICKS.get(),
                ServerConfig.TIDE_DOOR_HOLD_TICKS_RESEARCH3.get());
        this.openTicksRemaining = Math.max(0, Math.min(tag.getInt("OpenTicks"), maximum));
    }
}
