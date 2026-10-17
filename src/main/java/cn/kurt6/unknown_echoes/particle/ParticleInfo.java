package cn.kurt6.unknown_echoes.particle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ParticleInfo {
    private ParticleInfo() { }
    private static Component text(String key, Object... args) { return Component.translatable("particle.unknown_echoes." + key, args); }
    private static String number(float value) { return String.format(Locale.ROOT, "%.3f", value).replaceAll("\\.?0+$", ""); }
    private static int percent(float value) { return Math.round(value * 100); }

    public static List<Component> describe(ParticleData data, ParticleKind kind) {
        int count = data.count(kind);
        List<Component> lines = new ArrayList<>();
        lines.add(text("tooltip", Component.translatable(kind.key()), count, ParticleRules.resonance(count)));
        lines.add(text("role." + kind.id()).copy().withStyle(ChatFormatting.AQUA));
        List<Component> current = effects(kind, count);
        if (count > 0) {
            lines.add(text("current_effects").copy().withStyle(ChatFormatting.AQUA));
            lines.add(text("contribution", number(count * ParticleRules.DAMAGE_PER_PARTICLE)));
            lines.addAll(current);
        } else lines.add(text("not_installed").copy().withStyle(ChatFormatting.DARK_GRAY));
        if (count == 50) lines.add(text("resonance_complete"));
        else {
            int next = ParticleRules.nextResonance(count), needed = next - count;
            lines.add(text("next_resonance", ParticleRules.resonance(next), next, needed).copy().withStyle(ChatFormatting.AQUA));
            if (data.total() + needed > data.capacity()) lines.add(text(data.tier() < 5 ? "need_capacity" : "need_reallocate").copy().withStyle(ChatFormatting.DARK_GRAY));
            lines.add(text("contribution", number(next * ParticleRules.DAMAGE_PER_PARTICLE)));
            for (Component effect : effects(kind, next)) if (!current.contains(effect)) lines.add(effect);
        }
        return List.copyOf(lines);
    }

    static List<Component> effects(ParticleKind kind, int n) {
        if (n == 0) return List.of();
        List<Component> lines = new ArrayList<>();
        switch (kind) {
            case FIRE -> {
                lines.add(text("stats.fire", number(ParticleStats.fireSeconds(n))));
                lines.add(text("detail.fire_layers", ParticleStats.fireLayers(n)));
                if (n >= 7) lines.add(text("detail.fire_spread", ParticleStats.fireSpreadRadius(n)));
                if (n >= 10) lines.add(text("detail.fire_explosion", ParticleStats.fireExplosionRadius(n), ParticleStats.fireExplosionDamage(n)));
                if (n >= 20) lines.add(text("detail.fire_field", ParticleStats.fireFieldRadius(n), number(ParticleStats.fireFieldTicks(n) / 20F)));
            }
            case FROST -> {
                lines.add(text("stats.frost", percent(ParticleStats.frostSlow(n)), percent(ParticleStats.frostChance(n)), number(ParticleStats.frostDurationTicks(n) / 20F)));
                if (n >= 10) lines.add(text("detail.frozen_bonus", percent(ParticleStats.frostFrozenMultiplier(n) - 1)));
                if (n >= 20) lines.add(text("detail.freeze_spread"));
                if (n >= 25) lines.add(text("detail.shatter", number(ParticleStats.frostShatter(n))));
                if (n >= 35) lines.add(text("detail.frost_field", ParticleStats.frostFieldRadius(n)));
                lines.add(text("detail.freeze_limit"));
            }
            case VENOM -> {
                lines.add(text("stats.venom", (int) ParticleStats.venomLevel(n), number(ParticleStats.venomSeconds(n))));
                lines.add(text("detail.venom_layers", ParticleStats.venomLayers(n)));
                if (n >= 5) lines.add(text("detail.venom_spread", ParticleStats.venomSpreadRadius(n)));
                if (n >= 20) lines.add(text("detail.venom_field", ParticleStats.venomFieldRadius(n), number(ParticleStats.venomFieldTicks(n) / 20F)));
            }
            case THUNDER -> {
                lines.add(n < 10 ? text("detail.thunder_unawakened", percent(ParticleStats.thunderChance(n)))
                        : text("stats.thunder", percent(ParticleStats.thunderChance(n)), (int) ParticleStats.thunderTargets(n)));
                if (n >= 10) {
                    lines.add(text(n >= 35 ? "detail.thunder_fan" : "detail.thunder_chain"));
                    lines.add(text("detail.thunder_damage", number(ParticleRules.DAMAGE_PER_PARTICLE * n * 0.6F * ParticleStats.thunderMultiplier(n))));
                }
                if (n >= 20) lines.add(text("detail.thunder_slow", number(ParticleStats.thunderSlowTicks(n) / 20F)));
                if (n >= 30) lines.add(text("detail.thunder_critical", percent(ParticleStats.thunderCriticalChance(n))));
                if (n == 50) lines.add(text("detail.thunder_storm", number(10 * ParticleStats.thunderMultiplier(n))));
            }
            case VOID -> {
                lines.add(text("stats.void", percent(ParticleStats.voidPierce(n)), percent(ParticleStats.voidSteal(n))));
                if (n >= 5) lines.add(text(n >= 20 ? "steal_direct" : "steal_void"));
                if (n >= 10) lines.add(text("detail.void_kill", percent(ParticleStats.voidKillHeal(n))));
                if (n == 50) lines.add(text("execution_limit", 8));
            }
            case HOLY -> {
                lines.add(text("stats.holy", number(ParticleStats.holyHeal(n)), percent(ParticleStats.holyChance(n)), number(ParticleStats.holyShield(n))));
                if (n >= 5) lines.add(text("detail.holy_shield"));
                if (n >= 10) lines.add(text("detail.holy_bonus"));
                if (n >= 15) lines.add(text("detail.holy_break", number(ParticleStats.holyBreakHeal(n))));
                if (n >= 20) lines.add(text("detail.holy_allies", ParticleStats.holyBreakRadius(n), ParticleStats.holyBreakAllyHeal(n)));
                if (n >= 30) lines.add(text("detail.holy_field", ParticleStats.holyFieldRadius(n)));
                lines.add(text("healing_interval"));
            }
            case SHADOW -> {
                lines.add(text("stats.shadow", percent(ParticleStats.shadowBackstab(n))));
                if (n >= 10) lines.add(text("detail.shadow_kill", number(ParticleStats.shadowInvisibleTicks(n) / 20F)));
                if (n >= 15) lines.add(text("shadow_speed", ParticleStats.shadowSpeedPercent(n)));
                if (n >= 30) lines.add(text("detail.shadow_next"));
                if (n >= 35) lines.add(text("detail.shadow_invisible"));
                if (n >= 30) lines.add(text("detail.shadow_once"));
                if (n == 50) lines.add(text("detail.shadow_clone"));
            }
            case EARTH -> {
                lines.add(text("stats.earth", percent(ParticleStats.earthChance(n))));
                if (n >= 10) {
                    lines.add(text("detail.earth_landing", ParticleStats.earthLandingRadius(n), number(ParticleStats.earthLandingDamage(n))));
                    lines.add(text("detail.earth_fall"));
                }
                if (n >= 30) lines.add(text(n >= 45 ? "detail.earth_launch" : "detail.earth_area", ParticleStats.earthAreaRadius(n)));
                if (n >= 35) lines.add(text(n >= 45 ? "detail.earth_landing_launch" : "detail.earth_landing_push"));
                if (n == 50) lines.add(text("detail.earth_damage"));
            }
        }
        if (kind == ParticleKind.FROST || kind == ParticleKind.EARTH || kind == ParticleKind.THUNDER && n >= 20) lines.add(text("control_interval"));
        if (kind == ParticleKind.FIRE || kind == ParticleKind.VENOM) lines.add(text("detail.dot_limit"));
        if (kind == ParticleKind.FIRE && n >= 20 || kind == ParticleKind.FROST && n >= 35
                || kind == ParticleKind.VENOM && n >= 20 || kind == ParticleKind.HOLY && n >= 30) lines.add(text("detail.field_limit"));
        return lines.stream().map(line -> (Component) line.copy().withStyle(ChatFormatting.GRAY)).toList();
    }
}
