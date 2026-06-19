package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.world.structure.BrokenArchiveStructure;
import cn.kurt6.unknown_echoes.world.structure.EchoTempleStructure;
import cn.kurt6.unknown_echoes.world.structure.LateRegionSiteStructure;
import cn.kurt6.unknown_echoes.world.structure.MirrorDustCloisterStructure;
import cn.kurt6.unknown_echoes.world.structure.ReflectionVaultStructure;
import cn.kurt6.unknown_echoes.world.structure.ForgottenColossusArenaStructure;
import cn.kurt6.unknown_echoes.world.structure.MirrorTempleStructure;
import cn.kurt6.unknown_echoes.world.structure.RegionSiteStructure;
import cn.kurt6.unknown_echoes.world.structure.ResonanceBeaconStructure;
import cn.kurt6.unknown_echoes.world.structure.RunePuzzleRoomStructure;
import cn.kurt6.unknown_echoes.world.structure.SilentAltarStructure;
import cn.kurt6.unknown_echoes.world.structure.SilentHutStructure;
import cn.kurt6.unknown_echoes.world.structure.SilentRingStructure;
import cn.kurt6.unknown_echoes.world.structure.SkyObservatoryStructure;
import cn.kurt6.unknown_echoes.world.structure.SmallEchoRuinStructure;
import cn.kurt6.unknown_echoes.world.structure.SpatialRiftStructure;
import cn.kurt6.unknown_echoes.world.structure.SubmergedRecordRoomStructure;
import cn.kurt6.unknown_echoes.world.structure.SunkenTempleStructure;
import cn.kurt6.unknown_echoes.world.structure.TideLighthouseReefStructure;
import cn.kurt6.unknown_echoes.world.structure.WindErodedTowerStructure;
import cn.kurt6.unknown_echoes.world.structure.WorldWonderStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, UnknownEchoes.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, UnknownEchoes.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<SmallEchoRuinStructure>> SMALL_ECHO_RUIN =
            STRUCTURE_TYPES.register("small_echo_ruin", () -> () -> SmallEchoRuinStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SpatialRiftStructure>> SPATIAL_RIFT =
            STRUCTURE_TYPES.register("spatial_rift", () -> () -> SpatialRiftStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<ResonanceBeaconStructure>> RESONANCE_BEACON_STRUCTURE =
            STRUCTURE_TYPES.register("resonance_beacon_structure", () -> () -> ResonanceBeaconStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<ForgottenColossusArenaStructure>> FORGOTTEN_COLOSSUS_ARENA =
            STRUCTURE_TYPES.register("forgotten_colossus_arena", () -> () -> ForgottenColossusArenaStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RunePuzzleRoomStructure>> RUNE_PUZZLE_ROOM =
            STRUCTURE_TYPES.register("rune_puzzle_room", () -> () -> RunePuzzleRoomStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<EchoTempleStructure>> ECHO_TEMPLE =
            STRUCTURE_TYPES.register("echo_temple", () -> () -> EchoTempleStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SunkenTempleStructure>> SUNKEN_TEMPLE =
            STRUCTURE_TYPES.register("sunken_temple", () -> () -> SunkenTempleStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<MirrorTempleStructure>> MIRROR_TEMPLE =
            STRUCTURE_TYPES.register("mirror_temple", () -> () -> MirrorTempleStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SilentHutStructure>> SILENT_HUT =
            STRUCTURE_TYPES.register("silent_hut", () -> () -> SilentHutStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SilentRingStructure>> SILENT_RING =
            STRUCTURE_TYPES.register("silent_ring", () -> () -> SilentRingStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SkyObservatoryStructure>> SKY_OBSERVATORY =
            STRUCTURE_TYPES.register("sky_observatory", () -> () -> SkyObservatoryStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<BrokenArchiveStructure>> BROKEN_ARCHIVE =
            STRUCTURE_TYPES.register("broken_archive", () -> () -> BrokenArchiveStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WindErodedTowerStructure>> WIND_ERODED_TOWER =
            STRUCTURE_TYPES.register("wind_eroded_tower", () -> () -> WindErodedTowerStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<ReflectionVaultStructure>> REFLECTION_VAULT =
            STRUCTURE_TYPES.register("reflection_vault", () -> () -> ReflectionVaultStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SilentAltarStructure>> SILENT_ALTAR =
            STRUCTURE_TYPES.register("silent_altar", () -> () -> SilentAltarStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<TideLighthouseReefStructure>> TIDE_LIGHTHOUSE_REEF =
            STRUCTURE_TYPES.register("tide_lighthouse_reef", () -> () -> TideLighthouseReefStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<MirrorDustCloisterStructure>> MIRROR_DUST_CLOISTER =
            STRUCTURE_TYPES.register("mirror_dust_cloister", () -> () -> MirrorDustCloisterStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<SubmergedRecordRoomStructure>> SUBMERGED_RECORD_ROOM =
            STRUCTURE_TYPES.register("submerged_record_room", () -> () -> SubmergedRecordRoomStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> MEMORY_PILLAR_COURTYARD =
            STRUCTURE_TYPES.register("memory_pillar_courtyard", () -> () -> RegionSiteStructure.MEMORY_PILLAR_COURTYARD_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> LOST_CAMP =
            STRUCTURE_TYPES.register("lost_camp", () -> () -> RegionSiteStructure.LOST_CAMP_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> OVERWORLD_LOST_CAMP =
            STRUCTURE_TYPES.register("overworld_lost_camp", () -> () -> RegionSiteStructure.OVERWORLD_LOST_CAMP_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> WIND_ERODED_STEPS =
            STRUCTURE_TYPES.register("wind_eroded_steps", () -> () -> RegionSiteStructure.WIND_ERODED_STEPS_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> WIND_RUNE_STONE_CIRCLE =
            STRUCTURE_TYPES.register("wind_rune_stone_circle", () -> () -> RegionSiteStructure.WIND_RUNE_STONE_CIRCLE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> REFLECTION_WELL =
            STRUCTURE_TYPES.register("reflection_well", () -> () -> RegionSiteStructure.REFLECTION_WELL_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> MIRROR_LAKE_CACHE =
            STRUCTURE_TYPES.register("mirror_lake_cache", () -> () -> RegionSiteStructure.MIRROR_LAKE_CACHE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> TIDE_BUOY_ARRAY =
            STRUCTURE_TYPES.register("tide_buoy_array", () -> () -> RegionSiteStructure.TIDE_BUOY_ARRAY_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> REEF_HUT =
            STRUCTURE_TYPES.register("reef_hut", () -> () -> RegionSiteStructure.REEF_HUT_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> LAKEBED_LIGHTHOUSE_RUINS =
            STRUCTURE_TYPES.register("lakebed_lighthouse_ruins", () -> () -> RegionSiteStructure.LAKEBED_LIGHTHOUSE_RUINS_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> ILLUSION_WALL_CHAMBER =
            STRUCTURE_TYPES.register("illusion_wall_chamber", () -> () -> RegionSiteStructure.ILLUSION_WALL_CHAMBER_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> MOSSBACK_TURTLE_NEST =
            STRUCTURE_TYPES.register("mossback_turtle_nest", () -> () -> RegionSiteStructure.MOSSBACK_TURTLE_NEST_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> UNDERGROUND_GREENHOUSE =
            STRUCTURE_TYPES.register("underground_greenhouse", () -> () -> RegionSiteStructure.UNDERGROUND_GREENHOUSE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> BROKEN_BELL_HUT =
            STRUCTURE_TYPES.register("broken_bell_hut", () -> () -> RegionSiteStructure.BROKEN_BELL_HUT_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> BROKEN_BELL_STELE =
            STRUCTURE_TYPES.register("broken_bell_stele", () -> () -> RegionSiteStructure.BROKEN_BELL_STELE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> ARCHIVE_REMNANT_HALL =
            STRUCTURE_TYPES.register("archive_remnant_hall", () -> () -> RegionSiteStructure.ARCHIVE_REMNANT_HALL_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> CLIFF_BRIDGE =
            STRUCTURE_TYPES.register("cliff_bridge", () -> () -> RegionSiteStructure.CLIFF_BRIDGE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<RegionSiteStructure>> ARCHIVE_GATE =
            STRUCTURE_TYPES.register("archive_gate", () -> () -> RegionSiteStructure.ARCHIVE_GATE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<LateRegionSiteStructure>> CRYSTAL_SONG_SHRINE =
            STRUCTURE_TYPES.register("crystal_song_shrine", () -> () -> LateRegionSiteStructure.CRYSTAL_SONG_SHRINE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<LateRegionSiteStructure>> CRYSTAL_SONG_HALL =
            STRUCTURE_TYPES.register("crystal_song_hall", () -> () -> LateRegionSiteStructure.CRYSTAL_SONG_HALL_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<LateRegionSiteStructure>> DREAM_FLOWER_HOUSE =
            STRUCTURE_TYPES.register("dream_flower_house", () -> () -> LateRegionSiteStructure.DREAM_FLOWER_HOUSE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<LateRegionSiteStructure>> ECHO_GRAND_ARCHIVE =
            STRUCTURE_TYPES.register("echo_grand_archive", () -> () -> LateRegionSiteStructure.ECHO_GRAND_ARCHIVE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> ECHO_WORLD_TREE =
            STRUCTURE_TYPES.register("echo_world_tree", () -> () -> WorldWonderStructure.ECHO_WORLD_TREE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> ETERNAL_ECHO_LIGHTHOUSE =
            STRUCTURE_TYPES.register("eternal_echo_lighthouse", () -> () -> WorldWonderStructure.ETERNAL_ECHO_LIGHTHOUSE_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> MIRROR_SEA =
            STRUCTURE_TYPES.register("mirror_sea", () -> () -> WorldWonderStructure.MIRROR_SEA_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> INVERTED_MOUNTAINS =
            STRUCTURE_TYPES.register("inverted_mountains", () -> () -> WorldWonderStructure.INVERTED_MOUNTAINS_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> SKY_RIFT =
            STRUCTURE_TYPES.register("sky_rift", () -> () -> WorldWonderStructure.SKY_RIFT_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> SILENT_GREAT_BOAT =
            STRUCTURE_TYPES.register("silent_great_boat", () -> () -> WorldWonderStructure.SILENT_GREAT_BOAT_CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<WorldWonderStructure>> BROKEN_BELL_TOWER =
            STRUCTURE_TYPES.register("broken_bell_tower", () -> () -> WorldWonderStructure.BROKEN_BELL_TOWER_CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SMALL_ECHO_RUIN_PIECE =
            STRUCTURE_PIECE_TYPES.register("small_echo_ruin_piece",
                    () -> (StructurePieceType.ContextlessType) SmallEchoRuinStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SPATIAL_RIFT_PIECE =
            STRUCTURE_PIECE_TYPES.register("spatial_rift_piece",
                    () -> (StructurePieceType.ContextlessType) SpatialRiftStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> RESONANCE_BEACON_PIECE =
            STRUCTURE_PIECE_TYPES.register("resonance_beacon_piece",
                    () -> (StructurePieceType.ContextlessType) ResonanceBeaconStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> FORGOTTEN_COLOSSUS_ARENA_PIECE =
            STRUCTURE_PIECE_TYPES.register("forgotten_colossus_arena_piece",
                    () -> (StructurePieceType.ContextlessType) ForgottenColossusArenaStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> RUNE_PUZZLE_ROOM_PIECE =
            STRUCTURE_PIECE_TYPES.register("rune_puzzle_room_piece",
                    () -> (StructurePieceType.ContextlessType) RunePuzzleRoomStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ECHO_TEMPLE_PIECE =
            STRUCTURE_PIECE_TYPES.register("echo_temple_piece",
                    () -> (StructurePieceType.ContextlessType) EchoTempleStructure.TemplePiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SUNKEN_TEMPLE_PIECE =
            STRUCTURE_PIECE_TYPES.register("sunken_temple_piece",
                    () -> (StructurePieceType.ContextlessType) SunkenTempleStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> MIRROR_TEMPLE_PIECE =
            STRUCTURE_PIECE_TYPES.register("mirror_temple_piece",
                    () -> (StructurePieceType.ContextlessType) MirrorTempleStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SILENT_HUT_PIECE =
            STRUCTURE_PIECE_TYPES.register("silent_hut_piece",
                    () -> (StructurePieceType.ContextlessType) SilentHutStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SILENT_RING_PIECE =
            STRUCTURE_PIECE_TYPES.register("silent_ring_piece",
                    () -> (StructurePieceType.ContextlessType) SilentRingStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SKY_OBSERVATORY_PIECE =
            STRUCTURE_PIECE_TYPES.register("sky_observatory_piece",
                    () -> (StructurePieceType.ContextlessType) SkyObservatoryStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> BROKEN_ARCHIVE_PIECE =
            STRUCTURE_PIECE_TYPES.register("broken_archive_piece",
                    () -> (StructurePieceType.ContextlessType) BrokenArchiveStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> WIND_ERODED_TOWER_PIECE =
            STRUCTURE_PIECE_TYPES.register("wind_eroded_tower_piece",
                    () -> (StructurePieceType.ContextlessType) WindErodedTowerStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> REFLECTION_VAULT_PIECE =
            STRUCTURE_PIECE_TYPES.register("reflection_vault_piece",
                    () -> (StructurePieceType.ContextlessType) ReflectionVaultStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SILENT_ALTAR_PIECE =
            STRUCTURE_PIECE_TYPES.register("silent_altar_piece",
                    () -> (StructurePieceType.ContextlessType) SilentAltarStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TIDE_LIGHTHOUSE_REEF_PIECE =
            STRUCTURE_PIECE_TYPES.register("tide_lighthouse_reef_piece",
                    () -> (StructurePieceType.ContextlessType) TideLighthouseReefStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> MIRROR_DUST_CLOISTER_PIECE =
            STRUCTURE_PIECE_TYPES.register("mirror_dust_cloister_piece",
                    () -> (StructurePieceType.ContextlessType) MirrorDustCloisterStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SUBMERGED_RECORD_ROOM_PIECE =
            STRUCTURE_PIECE_TYPES.register("submerged_record_room_piece",
                    () -> (StructurePieceType.ContextlessType) SubmergedRecordRoomStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> LATE_REGION_SITE_PIECE =
            STRUCTURE_PIECE_TYPES.register("late_region_site_piece",
                    () -> (StructurePieceType.ContextlessType) LateRegionSiteStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> WORLD_WONDER_PIECE =
            STRUCTURE_PIECE_TYPES.register("world_wonder_piece",
                    () -> (StructurePieceType.ContextlessType) WorldWonderStructure.Piece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> REGION_SITE_PIECE =
            STRUCTURE_PIECE_TYPES.register("region_site_piece",
                    () -> (StructurePieceType.ContextlessType) RegionSiteStructure.Piece::new);
}
