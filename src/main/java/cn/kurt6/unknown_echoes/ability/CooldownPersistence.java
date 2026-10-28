package cn.kurt6.unknown_echoes.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** 死亡重生时搬运写在 getPersistentData 根层的冷却截止时间。 */
public final class CooldownPersistence {
    private static final List<String> DEADLINE_SUFFIXES = List.of("_until", "_ready_at");
    // _ready 也用于待触发增益，只有明确的冷却键可以跨重生保留。
    private static final List<String> EXPLICIT_DEADLINE_KEYS = List.of(
            "ue_particle_control_ready", "ue_particle_holy_ready", "ue_particle_earth_ready");

    private CooldownPersistence() {
    }

    public static void copyCooldownDeadlines(Player source, Player target) {
        CompoundTag from = source.getPersistentData();
        CompoundTag to = target.getPersistentData();
        for (String key : from.getAllKeys()) {
            if (!isDeadlineKey(key) || !from.contains(key, Tag.TAG_LONG)) {
                continue;
            }
            to.putLong(key, from.getLong(key));
        }
    }

    private static boolean isDeadlineKey(String key) {
        if (!key.startsWith("ue_")) {
            return false;
        }
        if (EXPLICIT_DEADLINE_KEYS.contains(key)) {
            return true;
        }
        for (String suffix : DEADLINE_SUFFIXES) {
            if (key.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
