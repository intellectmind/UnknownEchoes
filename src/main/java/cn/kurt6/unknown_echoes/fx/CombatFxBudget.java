package cn.kurt6.unknown_echoes.fx;

public record CombatFxBudget(int effects, int admissions, int segments, int facets, int accents) {
    public static CombatFxBudget forSetting(int setting) {
        return switch (setting) {
            case 2 -> new CombatFxBudget(16, 12, 2048, 48, 64);
            case 1 -> new CombatFxBudget(48, 32, 6144, 256, 384);
            default -> new CombatFxBudget(128, 64, 24576, 1024, 3072);
        };
    }

    public int accentsFor(int requested, double distance, double density) {
        double range = distance <= 24 ? 1 : distance <= 48 ? .55 : .25;
        double quality = effects <= 16 ? .15 : effects <= 48 ? .5 : 1;
        return Math.max(0, (int) Math.ceil(requested * range * quality * Math.max(0, Math.min(1, density))));
    }
}
