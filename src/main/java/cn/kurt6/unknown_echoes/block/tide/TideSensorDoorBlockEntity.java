package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 潮汐感应门 BlockEntity:感应扫描 + 开门保持倒计时(全部服务端)。
 * 关闭态每 10 tick 扫一次身边玩家:持潮汐回响 → 整组开启,保持时长按开门者研究等级
 * (5.3"倒影入口稳定时间":基线 10 秒,研究 3 起 20 秒,ServerConfig 可调);
 * 无权限的玩家贴近时只收到含蓄水纹提示(节流)。倒计时归零 → 整组自动闭合。
 */
public class TideSensorDoorBlockEntity extends BlockEntity {

    /** 感应半径(格)。 */
    private static final double SENSE_RANGE = 3.0D;
    /** 无权限提示半径与节流间隔。 */
    private static final double HINT_RANGE = 2.5D;
    private static final int HINT_INTERVAL_TICKS = 80;

    private int openTicksRemaining = 0;

    public TideSensorDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIDE_SENSOR_DOOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TideSensorDoorBlockEntity door) {
        if (state.getValue(TideSensorDoorBlock.OPEN)) {
            if (door.openTicksRemaining > 0 && --door.openTicksRemaining <= 0) {
                closeGroup(level, pos);
            }
            return;
        }
        if (level.getGameTime() % 10 != 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = new AABB(pos).inflate(SENSE_RANGE);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (EchoPermission.canUseTideMechanism(player)) {
                openGroup(level, pos, player);
                return;
            }
            // 无权限:贴近才给含蓄提示,且按时间节流,避免刷屏(红线 #7)
            if (level.getGameTime() % HINT_INTERVAL_TICKS == 0
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                            <= HINT_RANGE * HINT_RANGE) {
                serverLevel.sendParticles(ParticleTypes.UNDERWATER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.02);
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.tide_door.locked"), true);
                cn.kurt6.unknown_echoes.ability.EchoAbilityManager.recordFailure(
                        player, EchoAbilityType.TIDE_ECHO, "door_locked");
            }
        }
    }

    /** 整组开启:洪泛相邻感应门,保持时长按开门者潮汐研究等级。 */
    public static void openGroup(Level level, BlockPos origin, ServerPlayer opener) {
        int hold = JournalManager.getResearchLevel(opener, EchoAbilityType.TIDE_ECHO) >= 3
                ? ServerConfig.TIDE_DOOR_HOLD_TICKS_RESEARCH3.get()
                : ServerConfig.TIDE_DOOR_HOLD_TICKS.get();
        boolean changed = floodSet(level, origin, true, hold);
        if (changed) {
            level.playSound(null, origin, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 1.2F, 0.8F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NAUTILUS,
                        origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                        16, 0.5, 0.6, 0.5, 0.08);
            }
            opener.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_door.opened"), true);
        }
    }

    public static void closeGroup(Level level, BlockPos origin) {
        if (floodSet(level, origin, false, 0)) {
            level.playSound(null, origin, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,
                    SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    /** 洪泛遍历相邻感应门统一设置开闭与保持时长;返回是否有方块被改动。 */
    private static boolean floodSet(Level level, BlockPos origin, boolean open, int holdTicks) {
        boolean changed = false;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty() && visited.size() <= TideSensorDoorBlock.MAX_CONNECTED) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TideSensorDoorBlock)) {
                continue;
            }
            if (state.getValue(TideSensorDoorBlock.OPEN) != open) {
                level.setBlock(pos, state.setValue(TideSensorDoorBlock.OPEN, open), 3);
                changed = true;
            }
            if (level.getBlockEntity(pos) instanceof TideSensorDoorBlockEntity door) {
                door.openTicksRemaining = holdTicks;
                door.setChanged();
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.add(next)
                        && level.getBlockState(next).getBlock() instanceof TideSensorDoorBlock) {
                    queue.add(next);
                }
            }
        }
        return changed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("OpenTicks", this.openTicksRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.openTicksRemaining = tag.getInt("OpenTicks");
    }
}
