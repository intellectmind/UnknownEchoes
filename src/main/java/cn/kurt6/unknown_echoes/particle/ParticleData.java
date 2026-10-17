package cn.kurt6.unknown_echoes.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ParticleData(int tier, List<Integer> counts) {
    public static final ParticleData EMPTY = new ParticleData(0, Collections.nCopies(8, 0));
    public static final Codec<ParticleData> CODEC = Codec.INT.listOf(9, 9).comapFlatMap(values -> {
        try { return DataResult.success(new ParticleData(values.getFirst(), values.subList(1, 9))); }
        catch (IllegalArgumentException invalid) { return DataResult.error(invalid::getMessage); }
    }, data -> {
        List<Integer> values = new ArrayList<>(9);
        values.add(data.tier()); values.addAll(data.counts());
        return values;
    });
    public static final StreamCodec<ByteBuf, ParticleData> STREAM_CODEC = new StreamCodec<>() {
        public ParticleData decode(ByteBuf buf) {
            int tier = buf.readUnsignedByte();
            List<Integer> counts = new ArrayList<>(8);
            for (int i = 0; i < 8; i++) counts.add((int) buf.readUnsignedByte());
            return new ParticleData(tier, counts);
        }

        public void encode(ByteBuf buf, ParticleData data) {
            buf.writeByte(data.tier());
            data.counts().forEach(buf::writeByte);
        }
    };

    public ParticleData {
        counts = List.copyOf(counts);
        if (tier < 0 || tier > 5 || counts.size() != 8
                || counts.stream().anyMatch(n -> n < 0 || n > 50)
                || counts.stream().mapToInt(Integer::intValue).sum() > ParticleRules.capacity(tier)) {
            throw new IllegalArgumentException("Invalid particle capacity");
        }
    }

    public int count(ParticleKind kind) { return counts.get(kind.ordinal()); }
    public int total() { return counts.stream().mapToInt(Integer::intValue).sum(); }
    public int capacity() { return ParticleRules.capacity(tier); }
    public ParticleData upgrade() { return new ParticleData(tier + 1, counts); }
    public ParticleData clear() { return new ParticleData(tier, Collections.nCopies(8, 0)); }

    public ParticleData change(ParticleKind kind, int delta) {
        List<Integer> changed = new ArrayList<>(counts);
        changed.set(kind.ordinal(), count(kind) + delta);
        return new ParticleData(tier, changed);
    }
}
