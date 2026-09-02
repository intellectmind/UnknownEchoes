package cn.kurt6.unknown_echoes.entity.boss;

/** 客户端渲染层读取的只读施法状态，不参与攻击判定。 */
public interface BossVisualState {
    int visualAttackMode();

    int visualAttackAge();

    int visualAttackDuration();
}
