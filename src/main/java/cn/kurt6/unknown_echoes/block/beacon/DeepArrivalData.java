package cn.kurt6.unknown_echoes.block.beacon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.OptionalInt;

final class DeepArrivalData extends SavedData {
    private static final String DATA_NAME = "unknown_echoes_deep_arrival";
    private static final Factory<DeepArrivalData> FACTORY =
            new Factory<>(DeepArrivalData::new, DeepArrivalData::load);

    private OptionalInt arrivalY = OptionalInt.empty();

    static DeepArrivalData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    OptionalInt arrivalY() {
        return arrivalY;
    }

    void rememberArrival(int y) {
        if (arrivalY.isEmpty()) {
            arrivalY = OptionalInt.of(y);
            setDirty();
        }
    }

    static DeepArrivalData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeepArrivalData data = new DeepArrivalData();
        if (tag.contains("ArrivalY", Tag.TAG_INT)) {
            data.arrivalY = OptionalInt.of(tag.getInt("ArrivalY"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        arrivalY.ifPresent(y -> tag.putInt("ArrivalY", y));
        return tag;
    }
}
