package cn.kurt6.unknown_echoes.block.beacon;

/**
 * 信标等级。V0.1 只实现 RESONANCE,其余为后续版本预留。
 */
public enum EchoBeaconTier {
    RESONANCE("resonance");
    // 预留:LOST("lost"), ABYSS("abyss"), VOID("void"), FINALE("finale")

    private final String id;

    EchoBeaconTier(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
