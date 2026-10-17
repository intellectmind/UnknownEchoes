package cn.kurt6.unknown_echoes.particle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.UUID;

final class ParticleShadowClaims {
    private static final String KEY = "ue_particle_shadow_pending";
    // 只保存待命中的凭据；淘汰使旧攻击失效，不能恢复已消费的增伤。
    private static final int LIMIT = 128;

    private ParticleShadowClaims() { }

    private static ListTag pending(CompoundTag tag) {
        ListTag list = tag.getList(KEY, Tag.TAG_INT_ARRAY);
        if (list.size() > LIMIT) return new ListTag();
        for (int i = 0; i < list.size(); i++) if (list.getIntArray(i).length != 4) return new ListTag();
        return list;
    }

    static void grant(CompoundTag tag, UUID attack) {
        ListTag list = pending(tag);
        var id = NbtUtils.createUUID(attack);
        if (!list.contains(id)) {
            if (list.size() == LIMIT) list.remove(0);
            list.add(id);
        }
        tag.put(KEY, list);
        tag.remove("ue_particle_shadow_used");
    }

    static boolean available(CompoundTag tag, UUID attack) {
        return pending(tag).contains(NbtUtils.createUUID(attack));
    }

    static void claim(CompoundTag tag, UUID attack) {
        ListTag list = pending(tag);
        list.removeIf(id -> id.equals(NbtUtils.createUUID(attack)));
        if (list.isEmpty()) tag.remove(KEY); else tag.put(KEY, list);
    }
}
