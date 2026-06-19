package cn.kurt6.unknown_echoes.ability;

import javax.annotation.Nullable;

/**
 * 回响能力类型。V0.1 只实现 WIND_ECHO,其余为后续版本预留。
 */
public enum EchoAbilityType {
    WIND_ECHO("wind_echo"),
    TIDE_ECHO("tide_echo"),
    TRUE_SIGHT_ECHO("true_sight_echo"),
    GRAVITY_ECHO("gravity_echo"),
    VOID_ECHO("void_echo");

    private final String id;

    EchoAbilityType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static EchoAbilityType byId(String id) {
        for (EchoAbilityType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
