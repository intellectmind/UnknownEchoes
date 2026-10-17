package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.Nullable;

/** 由深层核心祭坛管理的主线 Boss；活动所有权与冷却均绑定具体祭坛。 */
public interface DeepRechallengeBoundBoss extends BossDeathSettlement {
    @Nullable
    BlockPos deepRechallengeAltar();

    default boolean registerDeepRechallengePresence(ServerLevel level) {
        BlockPos altarPos = deepRechallengeAltar();
        if (altarPos != null && this instanceof Entity boss) {
            return BossRechallengeAltarBlockEntity.onBossAdded(level, altarPos, boss);
        }
        return true;
    }

    default void finishDeepRechallenge(ServerLevel level) {
        BlockPos altarPos = deepRechallengeAltar();
        if (altarPos != null && this instanceof Entity boss) {
            BossRechallengeAltarBlockEntity.startCooldown(
                    level, altarPos, boss.getUUID());
        }
    }

    default void clearDeepRechallengePresence(ServerLevel level) {
        BlockPos altarPos = deepRechallengeAltar();
        if (altarPos != null && this instanceof Entity boss) {
            BossRechallengeAltarBlockEntity.onBossDestroyed(
                    level, altarPos, boss.getUUID());
        }
    }

    /** 区块卸载时暂停战斗；祭坛损坏或 UUID 失配时释放并销毁异常 Boss。 */
    default boolean tickDeepRechallengeBinding(ServerLevel level, BooleanSupplier combatActive) {
        BlockPos altarPos = deepRechallengeAltar();
        if (altarPos == null || !(this instanceof Entity boss)) {
            return false;
        }
        BossRechallengeAltarBlockEntity.BindingState state = BossRechallengeAltarBlockEntity.bindingState(
                level, altarPos, boss);
        if (state == BossRechallengeAltarBlockEntity.BindingState.INTERRUPTED) {
            return true;
        }
        if (state == BossRechallengeAltarBlockEntity.BindingState.UNAVAILABLE) {
            BossBindingPause.pause(level, boss, combatActive,
                    () -> altarPos.equals(deepRechallengeAltar())
                            && BossRechallengeAltarBlockEntity.bindingState(level, altarPos, boss)
                            == BossRechallengeAltarBlockEntity.BindingState.UNAVAILABLE);
            return true;
        }
        if (state == BossRechallengeAltarBlockEntity.BindingState.INVALID) {
            DeepBossBindingCleanup.discardInvalid(level, altarPos, boss);
            return true;
        }
        return false;
    }

    default boolean isDeepRechallengeCombatBlocked(ServerLevel level) {
        BlockPos altarPos = deepRechallengeAltar();
        return altarPos != null && this instanceof Entity boss
                && BossRechallengeAltarBlockEntity.bindingState(
                        level, altarPos, boss)
                != BossRechallengeAltarBlockEntity.BindingState.VALID;
    }
}
