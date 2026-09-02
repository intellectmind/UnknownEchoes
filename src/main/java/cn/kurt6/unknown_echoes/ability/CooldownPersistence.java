package cn.kurt6.unknown_echoes.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** 死亡重生时搬运写在 getPersistentData 根层的冷却截止时间。 */
public final class CooldownPersistence {
    /** 截止时间型键的后缀。新增同类键只要沿用这些后缀就会被自动清理。 */
    private static final List<String> DEADLINE_SUFFIXES = List.of("_until", "_ready_at");

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
        for (String suffix : DEADLINE_SUFFIXES) {
            if (key.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
