package cn.kurt6.unknown_echoes.entity.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** 清除目标会派发事件；事件返回后只冻结仍属于原暂停条件的实体。 */
final class BossBindingPause {
    private static final ThreadLocal<Set<Entity>> PAUSING =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private BossBindingPause() {
    }

    static void pause(ServerLevel level, Entity boss, BooleanSupplier combatActive,
                      BooleanSupplier stillUnavailable) {
        Set<Entity> pausing = PAUSING.get();
        if (!pausing.add(boss)) {
            return;
        }
        try {
            if (!canPause(level, boss, combatActive, stillUnavailable)) {
                return;
            }
            if (boss instanceof Mob mob) {
                mob.setTarget(null);
                if (!canPause(level, boss, combatActive, stillUnavailable)) {
                    return;
                }
                // 目标事件可取消或改写目标；场地仍不可用时仍须停止移动。
                mob.getNavigation().stop();
            }
            boss.setDeltaMovement(Vec3.ZERO);
        } finally {
            pausing.remove(boss);
            if (pausing.isEmpty()) {
                PAUSING.remove();
            }
        }
    }

    private static boolean canPause(ServerLevel level, Entity boss, BooleanSupplier combatActive,
                                    BooleanSupplier stillUnavailable) {
        // 深层祭坛及刷怪器的绑定查询也可能同步通知邻块。
        return isCurrent(level, boss, combatActive) && stillUnavailable.getAsBoolean()
                && isCurrent(level, boss, combatActive);
    }

    private static boolean isCurrent(ServerLevel level, Entity boss, BooleanSupplier combatActive) {
        return boss.level() == level && boss.isAlive() && !boss.isRemoved()
                && level.getEntity(boss.getUUID()) == boss && combatActive.getAsBoolean();
    }
}
