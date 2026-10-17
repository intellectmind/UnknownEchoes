package cn.kurt6.unknown_echoes.fx;

public final class DisasterAtmosphereProfile {
    private DisasterAtmosphereProfile() { }

    public static float visibility(double distance) {
        double edge = Math.clamp((StormVolumeGeometry.VIEW_DISTANCE - distance) / 24, 0, 1);
        return (float) (edge * edge * (3 - 2 * edge));
    }

    public static float gust(double tick, boolean deep) {
        double time = tick * (deep ? 0.65D : 1.0D);
        return (float) (0.88D + Math.sin(time * 0.053D) * 0.08D + Math.sin(time * 0.119D + 1.2D) * 0.04D);
    }

    public static float windVolume(double distance, float strength, float shelter, float gust, double volume) {
        double near = Math.clamp(1 - distance / StormVolumeGeometry.VIEW_DISTANCE, 0, 1);
        return (float) (0.65D * near * near * Math.clamp(strength, 0, 1) * Math.clamp(shelter, 0, 1)
                * Math.clamp(gust, 0, 1) * Math.clamp(volume, 0, 1));
    }

    public static float impactVolume(double distance, float radius, float strength, boolean rumble) {
        double range = Math.min(64, radius * (rumble ? 1.75D : 1));
        double near = Math.clamp(1 - distance / Math.max(1, range), 0, 1);
        return (float) (near * near * Math.clamp(strength, 0, 1) * (rumble ? 0.9D : 0.5D));
    }
}
