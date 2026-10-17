package cn.kurt6.unknown_echoes.entity.boss;

/** 原版已提交死亡、场地收尾仍由当前死亡调用负责时，移除事件不得提前释放归属。 */
public interface BossDeathSettlement {
    boolean isSettlingDeath();
}
