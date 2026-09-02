package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

public final class DeepEchoesProgressCatalog {
    public static final List<EchoAbilityType> ABILITIES = List.of(
            EchoAbilityType.GRAVITY_ECHO,
            EchoAbilityType.VOID_ECHO);
    public static final List<String> BOSS_PATHS = List.of(
            "void_prophet",
            "rift_warden",
            "world_echo",
            "inverted_gatekeeper",
            "ash_scribe");
    public static final List<ResourceLocation> BOSS_KEYS = BOSS_PATHS.stream()
            .map(UnknownEchoes::id)
            .toList();
    public static final List<String> BOSS_IDS = ids(BOSS_PATHS);
    public static final List<String> BIOME_IDS = DeepEchoesBiomeCatalog.IDS;
    public static final List<String> STRUCTURE_PATHS = List.of(
            "folded_archive",
            "calibration_hall",
            "inverted_mountains",
            "inverted_chasm_bridge",
            "silent_abyss_anchor",
            "spatial_rift",
            "void_vein_chamber",
            "folded_waystation",
            "gravity_shard_spire",
            "hushed_refuge",
            "misfiled_catalog_well",
            "folded_scriptorium",
            "inverted_archive_tower",
            "suspended_survey_station",
            "fractured_anchor_station",
            "severed_archive_vault",
            "resonance_foundry",
            "gravity_crown_observatory",
            "voidscar_bastion",
            "shattered_codex_vault",
            "weightless_orrery_court",
            "hushed_cathedral",
            "riftglass_citadel",
            "folded_celestial_archive",
            "inverted_pendulum_palace",
            "riftworn_memorial_court",
            "palimpsest_ecology_dome",
            "interlayer_expedition_ring",
            "world_resonance_hall");
    public static final List<String> STRUCTURE_IDS = ids(STRUCTURE_PATHS);
    public static final List<String> MOB_PATHS = List.of(
            "foldpage_bug",
            "anchor_shell_beast",
            "abyss_lurker",
            "void_wanderer");
    public static final List<String> MOB_IDS = ids(MOB_PATHS);
    public static final List<ArtifactType> ARTIFACTS = List.of(
            ArtifactType.TIME_HOURGLASS,
            ArtifactType.DIMENSION_ANCHOR,
            ArtifactType.RIFT_ANCHOR);
    public static final int FIRST_PAGE_ID = 128;
    public static final int LAST_PAGE_ID = 207;
    public static final int PAGE_COUNT = LAST_PAGE_ID - FIRST_PAGE_ID + 1;

    private DeepEchoesProgressCatalog() {
    }

    public static int countKnown(Collection<String> known, Collection<String> expected) {
        int count = 0;
        for (String id : expected) {
            if (known.contains(id)) {
                count++;
            }
        }
        return count;
    }

    public static int countPages(Collection<Integer> pages) {
        int count = 0;
        for (int pageId : pages) {
            if (pageId >= FIRST_PAGE_ID && pageId <= LAST_PAGE_ID) {
                count++;
            }
        }
        return count;
    }

    private static List<String> ids(List<String> paths) {
        return paths.stream()
                .map(path -> UnknownEchoes.id(path).toString())
                .toList();
    }
}
