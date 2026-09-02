package cn.kurt6.unknown_echoes.block.beacon;

public enum EchoBeaconTier {
    RESONANCE("resonance");

    private final String id;

    EchoBeaconTier(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
