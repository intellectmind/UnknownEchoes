package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record CombatFxPayload(Shape shape, Vec3 origin, Vec3 direction, float radius,
                              float length, float span, int core, int edge,
                              int duration, boolean boss, int entityId) implements CustomPacketPayload {
    public enum Shape {
        RING, SLASH, BEAM, SPIRAL, DOME, ORBIT, COLUMN, BURST, CONE,
        DEBRIS, SHOCKWAVE, IMPLOSION, COLLAPSE, ARRIVAL, HELIX,
        SIGIL, PETALS, AEGIS, IMPACT, WAKE, VORTEX, SINGULARITY, TRACE,
        BOSS_SUMMON, BOSS_DEATH, BOSS_PHASE
    }

    public static final Type<CombatFxPayload> TYPE = new Type<>(UnknownEchoes.id("combat_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatFxPayload> STREAM_CODEC =
            StreamCodec.of(CombatFxPayload::write, CombatFxPayload::read);

    public boolean valid() {
        return shape != null && finite(origin) && finite(direction)
                && Float.isFinite(radius) && radius >= 0 && radius <= 64
                && Float.isFinite(length) && length >= 0 && length <= 128
                && Float.isFinite(span) && span >= 0 && span <= Math.PI * 8
                && duration >= 2 && duration <= 30 && entityId >= -1;
    }

    private static boolean finite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z)
                && Math.abs(v.x) <= 3.2E7 && Math.abs(v.y) <= 3.2E7 && Math.abs(v.z) <= 3.2E7;
    }

    private static void write(RegistryFriendlyByteBuf b, CombatFxPayload p) {
        b.writeEnum(p.shape);
        b.writeDouble(p.origin.x); b.writeDouble(p.origin.y); b.writeDouble(p.origin.z);
        b.writeFloat((float) p.direction.x); b.writeFloat((float) p.direction.y); b.writeFloat((float) p.direction.z);
        b.writeFloat(p.radius); b.writeFloat(p.length); b.writeFloat(p.span);
        b.writeInt(p.core); b.writeInt(p.edge);
        b.writeVarInt(p.duration); b.writeBoolean(p.boss); b.writeVarInt(p.entityId);
    }

    private static CombatFxPayload read(RegistryFriendlyByteBuf b) {
        return new CombatFxPayload(b.readEnum(Shape.class),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                new Vec3(b.readFloat(), b.readFloat(), b.readFloat()),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readInt(), b.readInt(),
                b.readVarInt(), b.readBoolean(), b.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
