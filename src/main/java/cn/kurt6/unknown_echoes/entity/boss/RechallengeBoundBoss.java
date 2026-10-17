package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.Nullable;

/** 由原场地祭坛管理的主线 Boss；绑定只记录祭坛位置，不改变个人奖励结算。 */
public interface RechallengeBoundBoss extends BossDeathSettlement {
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
    default boolean tickRechallengeBinding(ServerLevel level, BooleanSupplier combatActive) {
        BlockPos altarPos = rechallengeAltar();
        if (altarPos == null || !(this instanceof Entity boss)) {
            return false;
        }
        BindingState state;
        var altarChunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        if (altarChunk == null) {
            state = BindingState.UNAVAILABLE;
        } else if (!rechallengeKind().isAltarState(altarChunk.getBlockState(altarPos))) {
            state = BindingState.INVALID;
        } else if (!rechallengeKind().isRealBossEntity(boss)) {

            state = BindingState.VALID;
        } else {
            state = MainBossRechallengeData.hasActiveBinding(
                    level, altarPos, rechallengeKind(), boss.getUUID())
                    ? BindingState.VALID : BindingState.INVALID;
        }
        if (state == BindingState.UNAVAILABLE) {
            BossBindingPause.pause(level, boss, combatActive,
                    () -> altarPos.equals(rechallengeAltar())
                            && level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4) == null);
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
        var altarChunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
        if (altarChunk == null) {
            return true;
        }
        if (!rechallengeKind().isAltarState(altarChunk.getBlockState(altarPos))) {
            return true;
        }
        return rechallengeKind().isRealBossEntity(boss)
                && !MainBossRechallengeData.hasActiveBinding(
                        level, altarPos, rechallengeKind(), boss.getUUID());
    }
}
