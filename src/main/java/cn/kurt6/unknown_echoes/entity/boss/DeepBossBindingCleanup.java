package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** 无效绑定的释放通知完成后，才决定是否仍可删除同一实体。 */
final class DeepBossBindingCleanup {
    private static final ThreadLocal<Set<Entity>> CLEANING =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private DeepBossBindingCleanup() {
    }

    static void discardInvalid(ServerLevel level, BlockPos altarPos, Entity boss) {
        Set<Entity> cleaning = CLEANING.get();
        if (!cleaning.add(boss)) {
            return;
        }
        try {
            var chunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
            if (chunk == null
                    || BossRechallengeAltarBlockEntity.bindingState(level, altarPos, boss)
                    != BossRechallengeAltarBlockEntity.BindingState.INVALID) {
                return;
            }
            BlockState state = chunk.getBlockState(altarPos);
            var altar = chunk.getBlockEntity(altarPos);
            BossRechallengeAltarBlockEntity.onBossDestroyed(level, altarPos, boss.getUUID());
            // 释放归属会通知邻块；换绑、死亡、卸载或现场替换后，不续用旧的删除决定。
            var currentChunk = level.getChunkSource().getChunkNow(altarPos.getX() >> 4, altarPos.getZ() >> 4);
            if (currentChunk == null
                    || currentChunk.getBlockState(altarPos) != state || currentChunk.getBlockEntity(altarPos) != altar
                    || BossRechallengeAltarBlockEntity.bindingState(level, altarPos, boss)
                    != BossRechallengeAltarBlockEntity.BindingState.INVALID) {
                return;
            }
            boss.discard();
        } finally {
            cleaning.remove(boss);
            if (cleaning.isEmpty()) {
                CLEANING.remove();
            }
        }
    }

}
