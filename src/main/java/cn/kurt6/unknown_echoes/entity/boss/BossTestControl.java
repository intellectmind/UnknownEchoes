package cn.kurt6.unknown_echoes.entity.boss;

/** 仅供管理员命令调用；正式伤害仍由 BossPhaseGate 校验。 */
public interface BossTestControl {
    boolean advanceTestPhase();

    default boolean openTestWindow() { return false; }

    float phaseDamageFloor();

    default boolean acceptsTestControl() { return true; }
}
