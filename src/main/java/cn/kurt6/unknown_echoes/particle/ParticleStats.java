package cn.kurt6.unknown_echoes.particle;

public final class ParticleStats {
    private ParticleStats() { }
    public static int fireSpreadRadius(int n) { return n >= 30 ? 2 : 1; }
    public static int fireLayers(int n) { return n >= 40 ? 3 : n >= 25 ? 2 : 1; }
    public static int fireExplosionRadius(int n) { return n == 50 ? 7 : n >= 45 ? 6 : n >= 15 ? 5 : 3; }
    public static int fireExplosionDamage(int n) { return n == 50 ? 10 : 5; }
    public static int fireFieldRadius(int n) { return n == 50 ? 6 : n >= 35 ? 5 : 4; }
    public static int fireFieldTicks(int n) { return n >= 35 ? 120 : 80; }
    public static float frostFrozenMultiplier(int n) { return n >= 10 ? n == 50 ? 1.4F : 1.25F : 1; }
    public static float frostShatter(int n) { return n >= 25 ? n >= 40 ? 5 : 2.5F : 0; }
    public static int frostFieldRadius(int n) { return n == 50 ? 7 : n >= 45 ? 6 : 5; }
    public static int venomSpreadRadius(int n) { return n == 50 ? 4 : n >= 15 ? 3 : n >= 10 ? 2 : 1; }
    public static int venomLayers(int n) { return n >= 40 ? 2 : 1; }
    public static int venomFieldRadius(int n) { return n == 50 ? 7 : n >= 35 ? 6 : n >= 30 ? 5 : 4; }
    public static int venomFieldTicks(int n) { return n >= 45 ? 160 : n >= 25 ? 100 : 80; }
    public static float thunderMultiplier(int n) { return n >= 15 ? 1.6F : 1; }
    public static float thunderCriticalChance(int n) { return n >= 30 ? n >= 40 ? 0.12F : 0.1F : 0; }
    public static int thunderSlowTicks(int n) { return n >= 45 ? 50 : n >= 35 ? 40 : n >= 25 ? 30 : 20; }
    public static float voidKillHeal(int n) { return n >= 45 ? 0.15F : n >= 40 ? 0.12F : n >= 30 ? 0.1F : n >= 25 ? 0.07F : 0.05F; }
    public static float holyBreakHeal(int n) { return n >= 40 ? 6 : n >= 25 ? 4 : n >= 15 ? 2 : 0; }
    public static int holyBreakRadius(int n) { return n == 50 ? 7 : 4; }
    public static int holyBreakAllyHeal(int n) { return n == 50 ? 8 : 4; }
    public static int holyFieldRadius(int n) { return n == 50 ? 7 : n >= 35 ? 6 : 4; }
    public static int shadowInvisibleTicks(int n) { return n >= 40 ? 80 : n >= 25 ? 60 : 40; }
    public static int earthAreaRadius(int n) { return n == 50 ? 7 : n >= 45 ? 4 : 3; }
    public static int earthLandingRadius(int n) { return n == 50 ? 7 : n >= 35 ? 6 : n >= 20 ? 4 : 3; }
    public static float earthLandingDamage(int n) { return n >= 20 ? 10 : n >= 15 ? 7.5F : 5; }
    private static final float[] FIRESECONDS = {0F, 2F, 2F, 3F, 3F, 4F, 4F, 5F, 5F, 5F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F};
    public static float fireSeconds(int n) { return FIRESECONDS[n]; }
    private static final float[] FROSTSLOW = {0F, 0.15F, 0.15F, 0.2F, 0.2F, 0.25F, 0.25F, 0.28F, 0.3F, 0.32F, 0.35F, 0.37F, 0.38F, 0.4F, 0.42F, 0.44F, 0.45F, 0.46F, 0.47F, 0.48F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F};
    public static float frostSlow(int n) { return FROSTSLOW[n]; }
    private static final float[] FROSTCHANCE = {0F, 0F, 0F, 0.05F, 0.05F, 0.1F, 0.1F, 0.12F, 0.15F, 0.18F, 0.2F, 0.22F, 0.24F, 0.26F, 0.28F, 0.3F, 0.32F, 0.34F, 0.36F, 0.38F, 0.4F, 0.42F, 0.44F, 0.46F, 0.48F, 0.5F, 0.52F, 0.54F, 0.56F, 0.58F, 0.6F, 0.62F, 0.64F, 0.66F, 0.68F, 0.7F, 0.72F, 0.74F, 0.76F, 0.78F, 0.8F, 0.82F, 0.84F, 0.86F, 0.88F, 0.9F, 0.91F, 0.92F, 0.93F, 0.94F, 0.95F};
    public static float frostChance(int n) { return FROSTCHANCE[n]; }
    private static final float[] FROSTSECONDS = {0F, 0F, 0F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 1.5F, 1.5F, 1.5F, 1.5F, 1.5F, 1.5F, 2F, 2F, 2F, 2F, 2F, 2F, 2F, 2F, 2F, 2.5F, 2.5F, 2.5F, 2.5F, 2.5F, 2.5F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F};
    public static float frostSeconds(int n) { return FROSTSECONDS[n]; }
    public static int frostDurationTicks(int n) { return (int) (frostSeconds(n) * 20) + (n == 15 ? 10 : 0); }
    private static final float[] VENOMLEVEL = {0F, 1F, 1F, 1F, 1F, 2F, 2F, 2F, 2F, 2F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 3F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F};
    public static float venomLevel(int n) { return VENOMLEVEL[n]; }
    private static final float[] VENOMSECONDS = {0F, 4F, 4F, 4F, 5F, 5F, 5F, 5F, 6F, 6F, 6F, 6F, 6F, 7F, 7F, 7F, 7F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F};
    public static float venomSeconds(int n) { return VENOMSECONDS[n]; }
    private static final float[] THUNDERCHANCE = {0F, 0.1F, 0.12F, 0.14F, 0.16F, 0.2F, 0.22F, 0.24F, 0.26F, 0.28F, 0.35F, 0.37F, 0.39F, 0.41F, 0.43F, 0.45F, 0.47F, 0.49F, 0.51F, 0.53F, 0.53F, 0.53F, 0.54F, 0.56F, 0.58F, 0.6F, 0.62F, 0.64F, 0.66F, 0.68F, 0.7F, 0.72F, 0.74F, 0.76F, 0.78F, 0.78F, 0.78F, 0.78F, 0.78F, 0.78F, 0.8F, 0.82F, 0.84F, 0.86F, 0.88F, 0.9F, 0.91F, 0.92F, 0.93F, 0.94F, 0.94F};
    public static float thunderChance(int n) { return THUNDERCHANCE[n]; }
    private static final float[] THUNDERTARGETS = {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 2F, 2F, 2F, 2F, 2F, 2F, 2F, 3F, 3F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F};
    public static float thunderTargets(int n) { return THUNDERTARGETS[n]; }
    private static final float[] VOIDPIERCE = {0F, 0.1F, 0.12F, 0.14F, 0.16F, 0.2F, 0.22F, 0.24F, 0.26F, 0.28F, 0.35F, 0.37F, 0.39F, 0.41F, 0.43F, 0.45F, 0.47F, 0.49F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F};
    public static float voidPierce(int n) { return VOIDPIERCE[n]; }
    private static final float[] VOIDSTEAL = {0F, 0F, 0F, 0F, 0F, 0.03F, 0.03F, 0.03F, 0.04F, 0.04F, 0.05F, 0.05F, 0.05F, 0.06F, 0.06F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.05F, 0.05F, 0.05F, 0.05F, 0.05F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.07F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F};
    public static float voidSteal(int n) { return VOIDSTEAL[n]; }
    private static final float[] HOLYHEAL = {0F, 1F, 1F, 1F, 1F, 2F, 2F, 2F, 2F, 3F, 4F, 4F, 4F, 4F, 5F, 5F, 5F, 5F, 6F, 6F, 6F, 7F, 7F, 7F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F};
    public static float holyHeal(int n) { return HOLYHEAL[n]; }
    private static final float[] HOLYCHANCE = {0F, 0F, 0F, 0F, 0F, 0.1F, 0.1F, 0.12F, 0.14F, 0.16F, 0.2F, 0.22F, 0.24F, 0.26F, 0.28F, 0.3F, 0.32F, 0.34F, 0.36F, 0.38F, 0.38F, 0.38F, 0.39F, 0.41F, 0.43F, 0.45F, 0.47F, 0.49F, 0.51F, 0.53F, 0.55F, 0.57F, 0.59F, 0.61F, 0.63F, 0.63F, 0.63F, 0.63F, 0.63F, 0.63F, 0.63F, 0.63F, 0.64F, 0.66F, 0.68F, 0.7F, 0.72F, 0.74F, 0.76F, 0.78F, 0.8F};
    public static float holyChance(int n) { return HOLYCHANCE[n]; }
    private static final float[] HOLYSHIELD = {0F, 0F, 0F, 0F, 0F, 4F, 4F, 4F, 4F, 4F, 4F, 5F, 5F, 5F, 5F, 5F, 6F, 6F, 6F, 6F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F, 8F};
    public static float holyShield(int n) { return HOLYSHIELD[n]; }
    private static final float[] SHADOWBACKSTAB = {0F, 0.24F, 0.28F, 0.31F, 0.35F, 0.42F, 0.46F, 0.49F, 0.53F, 0.56F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F};
    public static float shadowBackstab(int n) { return SHADOWBACKSTAB[n]; }
    public static int shadowSpeedPercent(int n) { return n >= 45 ? 30 : n >= 20 ? 20 : n >= 15 ? 10 : 0; }
    private static final float[] EARTHCHANCE = {0F, 0.15F, 0.18F, 0.21F, 0.24F, 0.25F, 0.28F, 0.31F, 0.34F, 0.37F, 0.4F, 0.43F, 0.46F, 0.49F, 0.52F, 0.55F, 0.58F, 0.61F, 0.64F, 0.67F, 0.67F, 0.67F, 0.67F, 0.67F, 0.67F, 0.7F, 0.73F, 0.76F, 0.79F, 0.82F, 0.85F, 0.88F, 0.91F, 0.94F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F, 0.95F};
    public static float earthChance(int n) { return EARTHCHANCE[n]; }
    private static final float[] EARTHDISTANCE = {0F, 1F, 1F, 1F, 1F, 2F, 2F, 2F, 2F, 2F, 3F, 3F, 3F, 3F, 3F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 4F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F, 6F};
    public static float earthDistance(int n) { return EARTHDISTANCE[n]; }
}
