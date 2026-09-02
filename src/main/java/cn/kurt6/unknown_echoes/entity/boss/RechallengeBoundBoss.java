package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 由原场地祭坛管理的主线 Boss；绑定只记录祭坛位置，不改变个人奖励结算。 */
public interface RechallengeBoundBoss {
    String RECHALLENGE_ALTAR_TAG = "RechallengeAltar";

    MainBossRechallengeData.BossKind rechallengeKind();

    @Nullable
    BlockPos rechallengeAltar();

    void bindRechallengeAltar(BlockPos altarPos);

    enum BindingState {
        UNBOUND,
        UNAVAILABLE,
        INVALID,
        VALID
    }

    default void bindRechallengeArena(BlockPos altarPos, BlockPos arenaCenter) {
        bindRechallengeAltar(altarPos);
    }

    default void saveRechallengeBinding(CompoundTag tag) {
        BlockPos altarPos = rechallengeAltar();
        if (altarPos != null) {
            tag.putLong(RECHALLENGE_ALTAR_TAG, altarPos.asLong());
        }
    }

    default void loadRechallengeBinding(CompoundTag tag) {
        if (tag.contains(RECHALLENGE_ALTAR_TAG, Tag.TAG_LONG)) {
            bindRechallengeAltar(BlockPos.of(tag.getLong(RECHALLENGE_ALTAR_TAG)));
        }
    }

    default void finishRechallenge(ServerLevel level) {
        BlockPos altarPos = rechallengeAltar();
        if (altarPos != null && this instanceof Entity boss) {
            MainBossRechallengeData.onBossDefeated(
                    level, altarPos, rechallengeKind(), boss.getUUID());
        }
    }

    default boolean registerRechallengePresence(ServerLevel level) {
        if (this instanceof Entity boss) {
            return MainBossRechallengeData.onBossAdded(level, boss);
        }
        return true;
    }

    /** 区块卸载时暂停战斗，祭坛被替换时销毁异常实例。 */
    default boolean tickRechallengeBinding(ServerLevel level) {
        BlockPos altarPos = rechallengeAltar();
        if (altarPos == null || !(this instanceof Entity boss)) {
            return false;
        }
        BindingState state;
        if (!level.hasChunkAt(altarPos)) {
            state = BindingState.UNAVAILABLE;
        } else if (!rechallengeKind().isAltarState(level.getBlockState(altarPos))) {
            state = BindingState.INVALID;
        } else if (!rechallengeKind().isRealBossEntity(boss)) {

            state = BindingState.VALID;
        } else {
            state = MainBossRechallengeData.hasActiveBinding(
                    level, altarPos, rechallengeKind(), boss.getUUID())
                    ? BindingState.VALID : BindingState.INVALID;
        }
        if (state == BindingState.UNAVAILABLE) {
            if (boss instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setDeltaMovement(Vec3.ZERO);
            } else {
                boss.setDeltaMovement(Vec3.ZERO);
            }
            return true;
        }
        if (state == BindingState.INVALID) {
            MainBossRechallengeData.onBossDestroyed(level, boss);
            boss.discard();
            return true;
        }
        return false;
    }

    default boolean isRechallengeCombatBlocked(ServerLevel level) {
        BlockPos altarPos = rechallengeAltar();
        if (altarPos == null || !(this instanceof Entity boss)) {
            return false;
        }
        if (!level.hasChunkAt(altarPos)) {
            return true;
        }
        if (!rechallengeKind().isAltarState(level.getBlockState(altarPos))) {
            return true;
        }
        return rechallengeKind().isRealBossEntity(boss)
                && !MainBossRechallengeData.hasActiveBinding(
                        level, altarPos, rechallengeKind(), boss.getUUID());
    }
}
