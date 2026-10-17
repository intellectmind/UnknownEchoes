package cn.kurt6.unknown_echoes.network;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record RealmDisasterPayload(ResourceLocation dimension, Vec3 center, float radius,
                                    float strength, boolean deep, boolean impact,
                                    float sceneHeight, long eventStart) implements CustomPacketPayload {
    public RealmDisasterPayload(ResourceLocation dimension, Vec3 center, float radius,
                                  float strength, boolean deep, boolean impact) {
        this(dimension, center, radius, strength, deep, impact, 0, 0);
    }
    public static final Type<RealmDisasterPayload> TYPE = new Type<>(UnknownEchoes.id("realm_disaster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RealmDisasterPayload> STREAM_CODEC =
            StreamCodec.of(RealmDisasterPayload::write, RealmDisasterPayload::read);

    public boolean valid() {
        return dimension != null && center != null
                && Double.isFinite(center.x) && Double.isFinite(center.y) && Double.isFinite(center.z)
                && Math.abs(center.x) <= 3.2E7 && Math.abs(center.z) <= 3.2E7 && Math.abs(center.y) <= 3.2E7
                && Float.isFinite(radius) && radius > 0 && radius <= 128
                && Float.isFinite(strength) && strength >= 0 && strength <= 1
                && Float.isFinite(sceneHeight) && sceneHeight >= 0 && sceneHeight <= 96
                && eventStart >= 0;
    }

    private static void write(RegistryFriendlyByteBuf b, RealmDisasterPayload p) {
        b.writeResourceLocation(p.dimension);
        b.writeDouble(p.center.x); b.writeDouble(p.center.y); b.writeDouble(p.center.z);
        b.writeFloat(p.radius); b.writeFloat(p.strength);
        b.writeBoolean(p.deep); b.writeBoolean(p.impact);
        b.writeFloat(p.sceneHeight); b.writeVarLong(p.eventStart);
    }

    private static RealmDisasterPayload read(RegistryFriendlyByteBuf b) {
        return new RealmDisasterPayload(b.readResourceLocation(),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean(), b.readFloat(), b.readVarLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
