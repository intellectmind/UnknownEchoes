package cn.kurt6.unknown_echoes.world.structure;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public final class StructureMobSpawns {
    private StructureMobSpawns() {
    }

    public static void configure(SpawnerBlockEntity spawner, HolderLookup.Provider registries, ResourceLocation type) {
        if (!type.getNamespace().equals("unknown_echoes")) {
            return;
        }
        int count = switch (type.getPath()) {
            case "cairn_shell_brute", "broken_bell_axeman" -> 1;
            case "mossplate_hound", "spore_crowned_wretch", "reed_mask_raider", "rift_page_stalker" -> 2;
            default -> 0;
        };
        if (count == 0) {
            return;
        }
        // 生成阶段尚未绑定 Level，直接保存并加载刷怪笼的持久参数。
        var tag = spawner.saveWithoutMetadata(registries);
        tag.putShort("Delay", (short) 200);
        tag.putShort("MinSpawnDelay", (short) 300);
        tag.putShort("MaxSpawnDelay", (short) 600);
        tag.putShort("SpawnCount", (short) count);
        tag.putShort("MaxNearbyEntities", (short) (count * 2));
        tag.putShort("RequiredPlayerRange", (short) 16);
        tag.putShort("SpawnRange", (short) 3);
        spawner.loadWithComponents(tag, registries);
    }
}
