package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public final class ParticleRules {
    public static final float DAMAGE_PER_PARTICLE = 0.2F;
    public static final Set<String> WEAPONS = Set.of("echo_broadsword", "echo_battleaxe", "wind_spear",
            "true_sight_blade", "silence_scythe", "crystal_song_staff", "broken_bell_hammer", "echo_oathblade",
            "gravity_hammer", "rift_dagger", "anchor_blade", "folded_glaive", "palimpsest_saber", "orrery_lance",
            "world_rift_greatsword", "fivefold_star_spear", "echo_longbow", "dream_bloom_bow",
            "gravity_inversion_bow", "tide_crossbow", "ash_script_crossbow");
    private static final int[] CAPACITIES = {0, 5, 10, 20, 35, 50};
    private static final int[] LEVELS = {0, 30, 50, 75, 100, 150};
    public record Cost(String item, int count) { }
    private static final List<List<Cost>> COSTS = List.of(List.of(),
            List.of(new Cost("echo_metal_ingot", 2), new Cost("echo_dust", 8)),
            List.of(new Cost("attuned_echo_gem", 1), new Cost("echo_dust", 4)),
            List.of(new Cost("deep_alloy_ingot", 2), new Cost("void_rift_shard", 2)),
            List.of(new Cost("deep_alloy_ingot", 2), new Cost("palimpsest_leaf", 1), new Cost("orrery_prism_core", 1)),
            List.of(new Cost("resonance_alloy_ingot", 2), new Cost("world_echo_residue", 2)));

    private ParticleRules() { }
    public static int capacity(int tier) { return CAPACITIES[tier]; }
    public static int level(int tier) { return LEVELS[tier]; }
    public static List<Cost> upgradeCost(int tier) { return COSTS.get(tier); }
    public static int injectionCost(int count) { return 10 + count; }
    public static int successPercent(int count) { return count <= 10 ? 100 : count <= 20 ? 80 : count <= 35 ? 65 : 50; }
    public static int singleRefund(int count) { return count <= 0 ? 0 : (count + 9) / 2; }
    public static int clearRefund(ParticleData data) {
        return data.counts().stream().mapToInt(n -> n * (n + 19) / 2).sum() * 3 / 10;
    }
    public static int resonance(int count) { return count >= 50 ? 4 : count >= 35 ? 3 : count >= 20 ? 2 : count >= 10 ? 1 : 0; }
    public static int nextResonance(int count) { return switch (resonance(count)) { case 0 -> 10; case 1 -> 20; case 2 -> 35; case 3 -> 50; default -> 50; }; }
    public static boolean eligible(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return !stack.isEmpty() && stack.getCount() == 1 && id.getNamespace().equals("unknown_echoes")
                && WEAPONS.contains(id.getPath());
    }
    public static ParticleData data(ItemStack stack) {
        return eligible(stack) ? stack.getOrDefault(ModDataComponents.PARTICLES.get(), ParticleData.EMPTY) : ParticleData.EMPTY;
    }

    public static int experienceAtLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (int) Math.min(Integer.MAX_VALUE, (9L * level * level - 325L * level + 4440) / 2);
    }
}
