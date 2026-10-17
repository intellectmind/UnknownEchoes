package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum ParticleKind {
    FIRE(0xff8547, true), FROST(0x9deaff, false), VENOM(0x8fe865, true),
    THUNDER(0xffdc66, false), VOID(0xb68bff, false), HOLY(0xffefb1, false),
    SHADOW(0xc181ee, false), EARTH(0xccae71, true);

    public final int color;
    public final boolean rare;

    ParticleKind(int color, boolean rare) { this.color = color; this.rare = rare; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String key() { return "particle.unknown_echoes." + id(); }
    public Item material() { return BuiltInRegistries.ITEM.get(UnknownEchoes.id(id() + "_particle_crystal")); }
}
