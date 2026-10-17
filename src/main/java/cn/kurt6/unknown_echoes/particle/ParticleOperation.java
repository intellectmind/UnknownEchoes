package cn.kurt6.unknown_echoes.particle;

public record ParticleOperation(ParticleData result, int experienceDelta, boolean success, boolean returnsBottle) {
    public static ParticleOperation plan(ParticleData data, int action, int roll) {
        if (action < 0 || action > 25 || roll < 0 || roll >= 100) throw new IllegalArgumentException("Invalid particle operation");
        if (action == 0) return new ParticleOperation(data.upgrade(), 0, true, false);
        if (action <= 16) {
            if (data.total() >= data.capacity()) throw new IllegalArgumentException("Full particle capacity");
            ParticleKind kind = ParticleKind.values()[(action - 1) % 8];
            boolean success = roll < ParticleRules.successPercent(data.count(kind));
            return new ParticleOperation(success ? data.change(kind, 1) : data,
                    -ParticleRules.injectionCost(data.count(kind)), success, false);
        }
        if (action == 25) {
            if (data.total() == 0) throw new IllegalArgumentException("No particles to clear");
            return new ParticleOperation(data.clear(), ParticleRules.clearRefund(data), true, true);
        }
        ParticleKind kind = ParticleKind.values()[action - 17];
        if (data.count(kind) == 0) throw new IllegalArgumentException("No particles to remove");
        return new ParticleOperation(data.change(kind, -1), ParticleRules.singleRefund(data.count(kind)), true, true);
    }
}
