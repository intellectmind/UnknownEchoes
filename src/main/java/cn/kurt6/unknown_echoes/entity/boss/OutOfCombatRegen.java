package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 脱战回血的唯一实现:四类 Boss 都必须走这里,各自再写一份会让配置项只对一部分生效。
 * 配置项名保留 miniboss_ 前缀以兼容已有存档的 server config。
 */
final class OutOfCombatRegen {

    private OutOfCombatRegen() {
    }

    /**
     * 推进一次脱战计时并按需回血,返回更新后的计时值(tick)。
     * 调用方每 tick 传入自己的计数器并写回;计数器不持久化,重载即脱战重置。
     */
    static int tick(Mob boss, int outOfCombatTicks) {
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive()) {
            return 0;
        }
        double regenPercent = ServerConfig.MINIBOSS_OUT_OF_COMBAT_REGEN_PERCENT.get();
        if (regenPercent <= 0.0D) {
            return outOfCombatTicks;
        }
        int delayTicks = ServerConfig.MINIBOSS_OUT_OF_COMBAT_DELAY_TICKS.get();
        int next = outOfCombatTicks + 1;
        if (next > delayTicks && next % 20 == 0 && boss.getHealth() < boss.getMaxHealth()) {
            boss.heal((float) (boss.getMaxHealth() * regenPercent));
        }
        return next;
    }
}
