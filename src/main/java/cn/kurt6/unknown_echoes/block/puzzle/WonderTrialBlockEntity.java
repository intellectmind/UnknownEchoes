package cn.kurt6.unknown_echoes.block.puzzle;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class WonderTrialBlockEntity extends BlockEntity {
    public static final String MOB_TAG = "UnknownEchoesWonderTrial";
    private static final int[][] GUARD_OFFSETS = {
            {-2, -2}, {2, 2}, {2, -2}, {-2, 2}, {0, 3}, {-3, 0}, {0, -3}, {3, 0},
            {-4, -4}, {0, -4}, {4, -4}, {4, 0}, {4, 4}, {0, 4}, {-4, 4}, {-4, 0}
    };
    private int theme = -1;
    private BlockPos previous;
    private BlockPos destination;

    public WonderTrialBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WONDER_TRIAL.get(), pos, state);
    }

    public void configure(int theme, BlockPos previous, BlockPos destination) {
        this.theme = theme;
        this.previous = previous;
        this.destination = destination;
        setChanged();
    }

    public void interact(ServerPlayer player) {
        if (!(level instanceof ServerLevel server) || theme < 0 || !EchoPermission.canUseEchoMechanism(player)) return;
        WonderTrialData data = WonderTrialData.get(server);
        if (previous != null && !data.complete(previous)) {
            point(player, "message.unknown_echoes.wonder.trial.previous", previous);
            return;
        }
        WonderTrialProgress progress = data.at(worldPosition);
        if (progress.complete()) {
            point(player, "message.unknown_echoes.wonder.trial.complete", destination);
            if (previous != null) EchoChaseManager.interact(player, this);
            return;
        }
        if (server.getDifficulty() == Difficulty.PEACEFUL) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.wonder.trial.peaceful"), true);
            return;
        }
        if (player.isShiftKeyDown()) {
            progress.reset();
            data.setDirty();
            player.displayClientMessage(Component.translatable("message.unknown_echoes.wonder.trial.reset"), true);
        } else if (progress.begin()) {
            data.setDirty();
            server.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.8F, 0.8F + progress.wave() * 0.15F);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.wonder.trial.rule"), false);
        }
        status(player, progress);
    }

    public static void point(ServerPlayer player, String key, BlockPos pos) {
        if (pos != null) player.displayClientMessage(Component.translatable(key, pos.getX(), pos.getY(), pos.getZ()), false);
    }

    private void status(ServerPlayer player, WonderTrialProgress progress) {
        if (progress.complete()) return;
        player.displayClientMessage(Component.translatable(progress.active()
                ? "message.unknown_echoes.wonder.trial.wave" : "message.unknown_echoes.wonder.trial.rest",
                progress.wave() + 1, WonderTrialProgress.WAVES,
                progress.quota() - progress.spawned() + progress.remaining().size()), true);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WonderTrialBlockEntity trial) {
        if (!(level instanceof ServerLevel server) || trial.theme < 0 || level.getGameTime() % 10 != 0) return;
        WonderTrialData data = WonderTrialData.get(server);
        WonderTrialProgress progress = data.find(pos);
        if (progress == null) return;
        int phase = progress.complete() ? 3 : progress.active() ? 1 : progress.wave() > 0 ? 2 : 0;
        boolean changed = state.getValue(WonderTrialBlock.PHASE) != phase;
        if (changed) {
            level.setBlock(pos, state.setValue(WonderTrialBlock.PHASE, phase), 3);
            if (phase == 2 || phase == 3) server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        if (progress.complete() && !changed) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        List<ServerPlayer> players = server.getEntitiesOfClass(ServerPlayer.class,
                new AABB(x - 18, y - 18, z - 18, x + 18, y + 18, z + 18),
                p -> p.isAlive() && p.distanceToSqr(x, y, z) < 18 * 18
                        && EchoPermission.canUseEchoMechanism(p));
        if (progress.complete() && changed) {
            for (ServerPlayer player : players) point(player, "message.unknown_echoes.wonder.trial.complete", trial.destination);
        }
        if (players.isEmpty() || progress.complete()) return;
        for (ServerPlayer player : players) trial.status(player, progress);
        if (!progress.canSpawn() || server.getDifficulty() == Difficulty.PEACEFUL) return;
        UUID run = progress.run();
        int wave = progress.wave();
        Mob mob = trial.createGuard(server, wave, progress.spawned());
        if (mob == null) return;
        UUID id = mob.getUUID();
        boolean reserved = false;
        boolean added = false;
        try {
            if (!trial.positionGuard(server, mob)) {
                for (ServerPlayer player : players) player.displayClientMessage(Component.translatable("message.unknown_echoes.wonder.trial.space"), true);
                return;
            }
            mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
            // 初始化可能改变碰撞箱；登记名册前按最终体型重新确认落点。
            if (!EntitySpawnSafety.isLoadedAndCollisionFree(server, mob) && !trial.positionGuard(server, mob)) {
                for (ServerPlayer player : players) player.displayClientMessage(Component.translatable("message.unknown_echoes.wonder.trial.space"), true);
                return;
            }
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            mob.restrictTo(pos, 10);
            mob.setTarget(players.get(0));
            CompoundTag marker = new CompoundTag();
            marker.putLong("Pos", pos.asLong());
            marker.putUUID("Run", run);
            marker.putString("Dimension", server.dimension().location().toString());
            mob.getPersistentData().put(MOB_TAG, marker);
            // 初始化和选敌回调可重置试炼或替换基座；旧生成不能登记到新的轮次。
            if (!run.equals(progress.run()) || wave != progress.wave() || !mob.isAlive()
                    || !server.hasChunkAt(pos) || server.getBlockEntity(pos) != trial) return;
            // 添加实体会同步触发回调；先预留 UUID，失败时只撤销同一轮仍待击败的实体。
            if (!progress.add(id)) return;
            reserved = true;
            data.setDirty();
            added = server.addFreshEntity(mob);
        } finally {
            if (!added) {
                if (reserved && progress.cancelSpawn(run, id)) data.setDirty();
                mob.discard();
            } else if (!run.equals(progress.run()) || !progress.remaining().contains(id)) {
                mob.discard();
            }
        }
    }

    public int chaseTheme() { return theme; }

    public boolean chaseAvailable(ServerPlayer player) {
        return level instanceof ServerLevel server && EchoChaseCatalog.valid(theme) && previous != null
                && EchoPermission.canUseEchoMechanism(player) && WonderTrialData.get(server).complete(previous)
                && WonderTrialData.get(server).complete(worldPosition);
    }

    Mob createGuard(ServerLevel server, int wave, int spawned) {
        List<EntityType<?>> guards = switch (theme) {
            case 0 -> List.of(ModEntities.ECHO_WANDERER.get(), ModEntities.CRYSTAL_NOISE_WISP.get(), ModEntities.SILENT_WALKER.get());
            case 1 -> List.of(ModEntities.MIRROR_DUST_DECOY.get(), ModEntities.ECHO_WANDERER.get(), ModEntities.WIND_ERODED_SENTINEL.get());
            case 2 -> List.of(ModEntities.MIRROR_DUST_DECOY.get(), ModEntities.CRYSTAL_NOISE_WISP.get(), ModEntities.SILENT_WALKER.get());
            case 3 -> List.of(ModEntities.WIND_ERODED_SENTINEL.get(), ModEntities.ECHO_WANDERER.get(), ModEntities.CRYSTAL_NOISE_WISP.get());
            case 4 -> List.of(ModEntities.CRYSTAL_NOISE_WISP.get(), ModEntities.WIND_ERODED_SENTINEL.get(), ModEntities.MIRROR_DUST_DECOY.get());
            case 5 -> List.of(ModEntities.SILENT_WALKER.get(), ModEntities.MIRROR_DUST_DECOY.get(), ModEntities.BROKEN_BELL_GUARD.get());
            default -> List.of(ModEntities.BROKEN_BELL_GUARD.get(), ModEntities.SILENT_WALKER.get(), ModEntities.CRYSTAL_NOISE_WISP.get());
        };
        // 用已成功生成的名额轮换，失败重试不跳过怪物；波次推进后更换首发组合。
        EntityType<?> type = guards.get((wave + spawned) % guards.size());
        return type.create(server) instanceof Mob mob ? mob : null;
    }

    boolean positionGuard(ServerLevel server, Mob mob) {
        double x = mob.getX(), y = mob.getY(), z = mob.getZ();
        float yaw = mob.getYRot(), pitch = mob.getXRot();
        for (int[] offset : GUARD_OFFSETS) {
            BlockPos target = worldPosition.offset(offset[0], 0, offset[1]);
            if (!server.hasChunkAt(target) || !server.getBlockState(target.below()).isFaceSturdy(server, target.below(), Direction.UP)) continue;
            mob.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, yaw, pitch);
            if (EntitySpawnSafety.isLoadedAndCollisionFree(server, mob)) {
                mob.setDeltaMovement(0.0D, 0.0D, 0.0D);
                mob.resetFallDistance();
                mob.getNavigation().stop();
                return true;
            }
        }
        mob.moveTo(x, y, z, yaw, pitch);
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Theme", theme);
        if (previous != null) tag.putLong("Previous", previous.asLong());
        if (destination != null) tag.putLong("Destination", destination.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        theme = tag.contains("Theme") ? tag.getInt("Theme") : -1;
        previous = tag.contains("Previous") ? BlockPos.of(tag.getLong("Previous")) : null;
        destination = tag.contains("Destination") ? BlockPos.of(tag.getLong("Destination")) : null;
        if (theme < 0 || theme > 6 || destination == null || destination.distSqr(worldPosition) > 256 * 256
                || (previous != null && previous.distSqr(worldPosition) > 256 * 256)) theme = -1;
    }
}
