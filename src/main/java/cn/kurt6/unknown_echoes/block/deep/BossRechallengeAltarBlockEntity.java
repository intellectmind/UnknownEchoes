package cn.kurt6.unknown_echoes.block.deep;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 记录主线 Boss 祭坛的共享重战冷却，避免多人或重启后绕过限制。 */
public class BossRechallengeAltarBlockEntity extends BlockEntity {
    private long cooldownUntil;

    public BossRechallengeAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_RECHALLENGE_ALTAR.get(), pos, state);
    }

    public static BossRechallengeAltarBlockEntity getOrCreate(ServerLevel level, BlockPos pos,
                                                               BlockState state) {
        if (level.getBlockEntity(pos) instanceof BossRechallengeAltarBlockEntity altar) {
            return altar;
        }
        BossRechallengeAltarBlockEntity altar = new BossRechallengeAltarBlockEntity(pos, state);
        level.setBlockEntity(altar);
        return altar;
    }

    /** Boss 死亡后从其绑定的祭坛开始冷却；旧区块首次死亡时按需补建 BlockEntity。 */
    public static void startCooldown(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GravityCoreAltarBlock)
                && !(state.getBlock() instanceof VoidCoreAltarBlock)) {
            return;
        }
        getOrCreate(level, pos, state).startCooldown(level.getGameTime());
    }

    public long remainingCooldownTicks(ServerLevel level) {
        long now = level.getGameTime();
        if (now < cooldownUntil) {
            return cooldownUntil - now;
        }
        if (cooldownUntil != 0L) {
            cooldownUntil = 0L;
            setChanged();
        }
        return 0L;
    }

    private void startCooldown(long now) {
        long duration = Math.max(0, ServerConfig.BOSS_RECHALLENGE_COOLDOWN_TICKS.get());
        cooldownUntil = duration == 0L ? 0L
                : now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("CooldownUntil", cooldownUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cooldownUntil = Math.max(0L, tag.getLong("CooldownUntil"));
    }
}
