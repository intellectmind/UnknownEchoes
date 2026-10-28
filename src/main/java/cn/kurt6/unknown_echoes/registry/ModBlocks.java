package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.AquaticBiomePlantBlock;
import cn.kurt6.unknown_echoes.block.AquaticSeaPickleBlock;
import cn.kurt6.unknown_echoes.block.BiomeGrassBlock;
import cn.kurt6.unknown_echoes.block.CommunalEchoTableBlock;
import cn.kurt6.unknown_echoes.block.HangingFruitBlock;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.block.RealmServicePlinthBlock;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.RubbleBlock;
import cn.kurt6.unknown_echoes.block.RuneBricksBlock;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlock;
import cn.kurt6.unknown_echoes.block.beacon.EchoBeaconBlock;
import cn.kurt6.unknown_echoes.block.beacon.AbyssBeaconBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepCalibrationNodeBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepBiomePlantBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepProvisionBrazierBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepRouteNodeBlock;
import cn.kurt6.unknown_echoes.block.deep.EndingRecordTableBlock;
import cn.kurt6.unknown_echoes.block.deep.GravityCoreAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.GravityFieldCoreBlock;
import cn.kurt6.unknown_echoes.block.deep.GravityFoldDoorBlock;
import cn.kurt6.unknown_echoes.block.deep.SpaceAnchorBlock;
import cn.kurt6.unknown_echoes.block.deep.VoidCoreAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.VoidStabilizerBlock;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlock;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlock;
import cn.kurt6.unknown_echoes.block.boss.ResonanceCandleBlock;
import cn.kurt6.unknown_echoes.block.puzzle.HiddenRuneBlock;
import cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock;
import cn.kurt6.unknown_echoes.block.puzzle.ProjectionResonanceDoorBlock;
import cn.kurt6.unknown_echoes.block.puzzle.ProjectionResonancePlateBlock;
import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlock;
import cn.kurt6.unknown_echoes.block.puzzle.SequenceRuneBlock;
import cn.kurt6.unknown_echoes.block.puzzle.WindDoorBlock;
import cn.kurt6.unknown_echoes.block.tide.TideBuoyBlock;
import cn.kurt6.unknown_echoes.block.tide.TideCoreAltarBlock;
import cn.kurt6.unknown_echoes.block.tide.TidePillarBlock;
import cn.kurt6.unknown_echoes.block.tide.TideRuneBlock;
import cn.kurt6.unknown_echoes.block.tide.TideRuneSeatBlock;
import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlock;
import cn.kurt6.unknown_echoes.block.truesight.TrueSightAltarBlock;
import cn.kurt6.unknown_echoes.block.wind.ObservatoryCoreBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UnknownEchoes.MODID);

    public static final DeferredBlock<cn.kurt6.unknown_echoes.particle.ParticleEnchantingTableBlock> PARTICLE_ENCHANTING_TABLE =
            BLOCKS.register("particle_enchanting_table", () -> new cn.kurt6.unknown_echoes.particle.ParticleEnchantingTableBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 7)));

    /** 关键机关方块 tag:破坏保护等判定统一走这里,新增关键方块只改 JSON(tags/block/critical_blocks.json)。 */
    public static final TagKey<Block> CRITICAL_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, UnknownEchoes.id("critical_blocks"));
    public static final TagKey<Block> RESOURCE_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, UnknownEchoes.id("resource_blocks"));
    public static final TagKey<Block> RUIN_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, UnknownEchoes.id("ruin_blocks"));

    public static final DeferredBlock<Block> ECHO_STONE_BRICKS = BLOCKS.registerSimpleBlock("echo_stone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.0F, 6.0F).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> CRACKED_ECHO_STONE_BRICKS = BLOCKS.registerSimpleBlock("cracked_echo_stone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.0F, 6.0F).requiresCorrectToolForDrops());

    public static final DeferredBlock<RuneBricksBlock> ECHO_RUNE_BRICKS = BLOCKS.register("echo_rune_bricks",
            () -> new RuneBricksBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().lightLevel(state -> 6)));

    public static final DeferredBlock<EchoBeaconBlock> RESONANCE_BEACON = BLOCKS.register("resonance_beacon",
            () -> new EchoBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 10).noLootTable().noOcclusion()));

    public static final DeferredBlock<AbyssBeaconBlock> ABYSS_BEACON = BLOCKS.register("abyss_beacon",
            () -> new AbyssBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    public static final DeferredBlock<DeepRouteNodeBlock> DEEP_ROUTE_NODE = BLOCKS.register("deep_route_node",
            () -> new DeepRouteNodeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 9).noLootTable().noOcclusion()));

    public static final DeferredBlock<DeepCalibrationNodeBlock> DEEP_CALIBRATION_NODE =
            BLOCKS.register("deep_calibration_node",
                    () -> new DeepCalibrationNodeBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 11)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<GravityCoreAltarBlock> GRAVITY_CORE_ALTAR =
            BLOCKS.register("gravity_core_altar",
                    () -> new GravityCoreAltarBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 10)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<GravityFoldDoorBlock> GRAVITY_FOLD_DOOR =
            BLOCKS.register("gravity_fold_door",
                    () -> new GravityFoldDoorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> state.getValue(GravityFoldDoorBlock.OPEN) ? 9 : 3)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<GravityFieldCoreBlock> GRAVITY_FIELD_CORE =
            BLOCKS.register("gravity_field_core",
                    () -> new GravityFieldCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> state.getValue(GravityFieldCoreBlock.ACTIVE) ? 12 : 5)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<SpaceAnchorBlock> SPACE_ANCHOR =
            BLOCKS.register("space_anchor",
                    () -> new SpaceAnchorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 8)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<VoidStabilizerBlock> VOID_STABILIZER =
            BLOCKS.register("void_stabilizer",
                    () -> new VoidStabilizerBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 9)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<VoidCoreAltarBlock> VOID_CORE_ALTAR =
            BLOCKS.register("void_core_altar",
                    () -> new VoidCoreAltarBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 12)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<WorldResonanceAltarBlock> WORLD_RESONANCE_ALTAR =
            BLOCKS.register("world_resonance_altar",
                    () -> new WorldResonanceAltarBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 14)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<EndingRecordTableBlock> ENDING_RECORD_TABLE =
            BLOCKS.register("ending_record_table",
                    () -> new EndingRecordTableBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST).lightLevel(state -> 11)
                            .noLootTable().noOcclusion()));

    public static final DeferredBlock<DeepProvisionBrazierBlock> DEEP_PROVISION_BRAZIER =
            BLOCKS.register("deep_provision_brazier",
                    () -> new DeepProvisionBrazierBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.LANTERN)
                            .lightLevel(state -> state.getValue(DeepProvisionBrazierBlock.MODE)
                                    == DeepProvisionBrazierBlock.Mode.IDLE ? 3 : 10)
                            .noOcclusion()));

    public static final DeferredBlock<CommunalEchoTableBlock> COMMUNAL_ECHO_TABLE =
            BLOCKS.register("communal_echo_table",
                    () -> new CommunalEchoTableBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(3.0F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.WOOD)
                            .lightLevel(state -> state.getValue(CommunalEchoTableBlock.MODE)
                                    == CommunalEchoTableBlock.Mode.IDLE ? 2 : 9)
                            .noOcclusion()));

    public static final DeferredBlock<RealmServicePlinthBlock> REALM_SERVICE_PLINTH =
            BLOCKS.register("realm_service_plinth",
                    () -> new RealmServicePlinthBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(3.2F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.COPPER)
                            .lightLevel(state -> 7).noOcclusion()));

    public static final DeferredBlock<RealmHabitatChimeBlock> REALM_HABITAT_CHIME =
            BLOCKS.register("realm_habitat_chime",
                    () -> new RealmHabitatChimeBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(3.0F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.COPPER)
                            .lightLevel(state -> state.getValue(RealmHabitatChimeBlock.ACTIVE) ? 11 : 4)
                            .noOcclusion()));

    public static final DeferredBlock<ProjectionResonancePlateBlock> PROJECTION_RESONANCE_PLATE =
            BLOCKS.register("projection_resonance_plate",
                    () -> new ProjectionResonancePlateBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(2.8F, 7.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL)
                            .lightLevel(state -> state.getValue(ProjectionResonancePlateBlock.ACTIVE)
                                    ? 9 : 2)
                            .noOcclusion()));

    public static final DeferredBlock<ProjectionResonanceDoorBlock> PROJECTION_RESONANCE_DOOR =
            BLOCKS.register("projection_resonance_door",
                    () -> new ProjectionResonanceDoorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                            // 开启态 shape 为空,必须 noOcclusion,否则遮挡缓存会为不渲染的方块建面。
                            .sound(SoundType.AMETHYST).noLootTable().noOcclusion()));

    public static final DeferredBlock<WindDoorBlock> WIND_DOOR = BLOCKS.register("wind_door",
            () -> new WindDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST).lightLevel(state -> 4)
                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<MemoryPillarBlock> MEMORY_PILLAR = BLOCKS.register("memory_pillar",
            () -> new MemoryPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(MemoryPillarBlock.ACTIVATED) ? 12 : 3)
                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<RewardAltarBlock> REWARD_ALTAR = BLOCKS.register("reward_altar",
            () -> new RewardAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 7).noLootTable().noOcclusion()));

    public static final DeferredBlock<HiddenRuneBlock> HIDDEN_RUNE_BRICKS = BLOCKS.register("hidden_rune_bricks",
            () -> new HiddenRuneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0F, 3600000.0F).noLootTable()));

    public static final DeferredBlock<SequenceRuneBlock> SEQUENCE_RUNE = BLOCKS.register("sequence_rune",
            () -> new SequenceRuneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(SequenceRuneBlock.LIT) ? 11 : 3).noLootTable()));

    public static final DeferredBlock<PuzzleCoreBlock> PUZZLE_CORE = BLOCKS.register("puzzle_core",
            () -> new PuzzleCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> state.getValue(PuzzleCoreBlock.ACTIVE) ? 13 : 5)
                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.WonderTrialBlock> WONDER_TRIAL =
            BLOCKS.register("wonder_trial", () -> new cn.kurt6.unknown_echoes.block.puzzle.WonderTrialBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.COPPER).lightLevel(state -> 12).noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleCoreBlock> WONDER_PUZZLE_CORE =
            BLOCKS.register("wonder_puzzle_core", () -> new cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleCoreBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.COPPER).lightLevel(state -> state.getValue(PuzzleCoreBlock.ACTIVE) ? 13 : 6).noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleDialBlock> WONDER_PUZZLE_DIAL =
            BLOCKS.register("wonder_puzzle_dial", () -> new cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleDialBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                            .sound(SoundType.COPPER).lightLevel(state -> state.getValue(
                                    cn.kurt6.unknown_echoes.block.puzzle.WonderPuzzleDialBlock.LIT) ? 10 : 3).noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.ChimeEchoCoreBlock> CHIME_ECHO_CORE =
            BLOCKS.register("chime_echo_core",
                    () -> new cn.kurt6.unknown_echoes.block.puzzle.ChimeEchoCoreBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(PuzzleCoreBlock.ACTIVE) ? 13 : 6)
                                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.FoldpagePairCoreBlock> FOLDPAGE_PAIR_CORE =
            BLOCKS.register("foldpage_pair_core",
                    () -> new cn.kurt6.unknown_echoes.block.puzzle.FoldpagePairCoreBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F)
                                    .lightLevel(state -> state.getValue(PuzzleCoreBlock.ACTIVE) ? 13 : 5)
                                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.FoldpageTileBlock> FOLDPAGE_TILE =
            BLOCKS.register("foldpage_tile",
                    () -> new cn.kurt6.unknown_echoes.block.puzzle.FoldpageTileBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.WOOD)
                                    .lightLevel(state -> state.getValue(
                                            cn.kurt6.unknown_echoes.block.puzzle.FoldpageTileBlock.LIT) ? 9 : 2)
                                    .noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.puzzle.StepRuneBlock> STEP_RUNE =
            BLOCKS.register("step_rune",
                    () -> new cn.kurt6.unknown_echoes.block.puzzle.StepRuneBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(SequenceRuneBlock.LIT) ? 11 : 3)
                                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<Block> SEALED_STONE = BLOCKS.register("sealed_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()));

    public static final DeferredBlock<Block> ANCHORED_SEALED_STONE = BLOCKS.register("anchored_sealed_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()));

    public static final DeferredBlock<Block> MIRROR_STONE = BLOCKS.registerSimpleBlock("mirror_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<Block> MIRROR_STONE_BRICKS = BLOCKS.registerSimpleBlock("mirror_stone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<TideRuneBlock> TIDE_RUNE = BLOCKS.register("tide_rune",
            () -> new TideRuneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 6).noLootTable()));

    public static final DeferredBlock<TidePillarBlock> TIDE_PILLAR = BLOCKS.register("tide_pillar",
            () -> new TidePillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    public static final DeferredBlock<TideCoreAltarBlock> TIDE_CORE_ALTAR = BLOCKS.register("tide_core_altar",
            () -> new TideCoreAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 9).noLootTable().noOcclusion()));

    public static final DeferredBlock<MirrorSigilBlock> MIRROR_SIGIL = BLOCKS.register("mirror_sigil",
            () -> new MirrorSigilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7).randomTicks().noLootTable()));

    public static final DeferredBlock<TrueSightAltarBlock> TRUE_SIGHT_ALTAR = BLOCKS.register("true_sight_altar",
            () -> new TrueSightAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 9).noLootTable().noOcclusion()));

    public static final DeferredBlock<Block> MUFFLE_MOSS = BLOCKS.registerSimpleBlock("muffle_moss",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.3F).sound(SoundType.MOSS));

    public static final DeferredBlock<BiomeGrassBlock> SILENT_POD_BUSH = BLOCKS.register("silent_pod_bush",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .replaceable().noCollission().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 4)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> WIND_ETCHED_STONE = BLOCKS.registerSimpleBlock("wind_etched_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    /** 风主题结构必须用它收边：借回响石砖（青色）给风蚀岩（浅灰）收边会明显撞色。 */
    public static final DeferredBlock<Block> WIND_ETCHED_BRICKS = BLOCKS.registerSimpleBlock("wind_etched_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    public static final DeferredBlock<Block> SKY_QUARTZ_ORE = BLOCKS.register("sky_quartz_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(2.4F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE)));

    public static final DeferredBlock<Block> CRYSTAL_SONG_CLUSTER = BLOCKS.register("crystal_song_cluster",
            () -> new DropExperienceBlock(UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(1.2F, 3.0F).sound(SoundType.AMETHYST).lightLevel(state -> 10)
                            .requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredBlock<FlowerBlock> RESONANT_MUSHROOM = BLOCKS.register("resonant_mushroom",
            () -> new FlowerBlock(MobEffects.GLOWING, 6.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.FUNGUS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<FlowerBlock> DREAM_FLOWER = BLOCKS.register("dream_flower",
            () -> new FlowerBlock(MobEffects.REGENERATION, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> ECHO_AMBER_ORE = BLOCKS.register("echo_amber_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                            .strength(2.2F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<BiomeGrassBlock> DREAM_MIST_VINE = BLOCKS.register("dream_mist_vine",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().instabreak().sound(SoundType.VINE)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> BROKEN_BELL_ORE = BLOCKS.register("broken_bell_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2),
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW)
                            .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> TIDE_SALT_ORE = BLOCKS.register("tide_salt_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2),
                    BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                            .strength(1.8F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE)));

    public static final DeferredBlock<BiomeGrassBlock> BROKEN_BELL_THORN = BLOCKS.register("broken_bell_thorn",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .replaceable().noCollission().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 4)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> ECHO_CLIFF_STONE = BLOCKS.registerSimpleBlock("echo_cliff_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.8F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<EchoArchiveTerminalBlock> ECHO_ARCHIVE_TERMINAL =
            BLOCKS.register("echo_archive_terminal",
                    () -> new EchoArchiveTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                            .lightLevel(state -> 11).noLootTable().noOcclusion()));

    public static final DeferredBlock<ObservatoryCoreBlock> OBSERVATORY_CORE = BLOCKS.register("observatory_core",
            () -> new ObservatoryCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 9).noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock> WIND_CURRENT_PLATFORM =
            BLOCKS.register("wind_current_platform",
                    () -> new cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(
                                            cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock.ACTIVE) ? 10 : 4)
                                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<RotatedPillarBlock> ECHO_LOG = BLOCKS.register("echo_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F).sound(SoundType.WOOD).lightLevel(state -> 6).ignitedByLava()));

    public static final DeferredBlock<Block> ECHO_PLANKS = BLOCKS.registerSimpleBlock("echo_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<LeavesBlock> ECHO_LEAVES = BLOCKS.register("echo_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F).randomTicks().sound(SoundType.AZALEA_LEAVES).noOcclusion()
                    .lightLevel(state -> 8)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY).ignitedByLava()));

    public static final DeferredBlock<HangingFruitBlock> GLOW_FRUIT =
            BLOCKS.register("glow_fruit",
                    () -> new HangingFruitBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN).noCollission().instabreak()
                            .sound(SoundType.SWEET_BERRY_BUSH).lightLevel(state -> 12)
                            .noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<HangingFruitBlock> WORLDTREE_FRUIT =
            BLOCKS.register("worldtree_fruit",
                    () -> new HangingFruitBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak()
                            .sound(SoundType.AMETHYST).lightLevel(state -> 15)
                            .noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<RotatedPillarBlock> WHISPERING_LOG = BLOCKS.register("whispering_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F).sound(SoundType.WOOD).lightLevel(state -> 5).ignitedByLava()));

    public static final DeferredBlock<Block> WHISPERING_PLANKS = BLOCKS.registerSimpleBlock("whispering_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<Block> WHISPERING_LATTICE = BLOCKS.registerSimpleBlock("whispering_lattice",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    /** 木质而非砖石：可燃、不要求工具、走 mineable/axe，构件不能复用砖石族属性。 */
    public static final DeferredBlock<Block> WHISPERING_TIMBER_FRAME =
            BLOCKS.registerSimpleBlock("whispering_timber_frame",
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<Block> TARRED_SHIP_PLANKS = BLOCKS.registerSimpleBlock("tarred_ship_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<Block> SHIP_DECK_PLANKS = BLOCKS.registerSimpleBlock("ship_deck_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> SHIP_TIMBER = BLOCKS.register("ship_timber",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()));
    public static final DeferredBlock<Block> RIVETED_BRONZE_CASING = BLOCKS.registerSimpleBlock("riveted_bronze_casing",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER));
    public static final DeferredBlock<Block> WEATHERED_SAILCLOTH = BLOCKS.registerSimpleBlock("weathered_sailcloth",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.8F).sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<Block> IVORY_REEF_ASHLAR = BLOCKS.registerSimpleBlock("ivory_reef_ashlar",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                    .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE));
    public static final DeferredBlock<Block> CARVED_BELLSTONE = BLOCKS.registerSimpleBlock("carved_bellstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<LeavesBlock> WHISPERING_LEAVES = BLOCKS.register("whispering_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.2F).randomTicks().sound(SoundType.AZALEA_LEAVES).noOcclusion()
                    .lightLevel(state -> 7)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY).ignitedByLava()));

    public static final DeferredBlock<RotatedPillarBlock> TIDEWOOD_LOG = BLOCKS.register("tidewood_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F).sound(SoundType.WOOD).lightLevel(state -> 5).ignitedByLava()));

    public static final DeferredBlock<Block> TIDEWOOD_PLANKS = BLOCKS.registerSimpleBlock("tidewood_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<LeavesBlock> TIDEWOOD_LEAVES = BLOCKS.register("tidewood_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.2F).randomTicks().sound(SoundType.AZALEA_LEAVES).noOcclusion()
                    .lightLevel(state -> 7)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .pushReaction(PushReaction.DESTROY).ignitedByLava()));

    public static final DeferredBlock<BiomeGrassBlock> GLOW_GRASS = BLOCKS.register("glow_grass",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XYZ).lightLevel(state -> 8)
                    .pushReaction(PushReaction.DESTROY), true));

    public static final DeferredBlock<FlowerBlock> ECHO_FLOWER = BLOCKS.register("echo_flower",
            () -> new FlowerBlock(MobEffects.GLOWING, 8.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 10)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<BiomeGrassBlock> GLOW_FERN = BLOCKS.register("glow_fern",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                    .pushReaction(PushReaction.DESTROY), true));

    public static final DeferredBlock<MiniBossSpawnerBlock> MINIBOSS_SPAWNER = BLOCKS.register("miniboss_spawner",
            () -> new MiniBossSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()));

    public static final DeferredBlock<ResonanceCandleBlock> RESONANCE_CANDLE = BLOCKS.register("resonance_candle",
            () -> new ResonanceCandleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.4F).sound(SoundType.CANDLE).lightLevel(state -> 10)
                    .noOcclusion()));

    public static final DeferredBlock<TideBuoyBlock> TIDE_BUOY = BLOCKS.register("tide_buoy",
            () -> new TideBuoyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.LANTERN)
                    .lightLevel(state -> state.getValue(TideBuoyBlock.LIT) ? 11 : 2)
                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<TideRuneSeatBlock> TIDE_RUNE_SEAT = BLOCKS.register("tide_rune_seat",
            () -> new TideRuneSeatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(TideRuneSeatBlock.LIT) ? 8 : 3)
                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<Block> CRACKED_MIRROR_BRICKS = BLOCKS.register("cracked_mirror_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 2).noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock> TIDE_SENSOR_DOOR =
            BLOCKS.register("tide_sensor_door",
                    () -> new cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(
                                            cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock.OPEN) ? 9 : 5)
                                    .noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.truesight.TrueSightSteleBlock> TRUE_SIGHT_STELE =
            BLOCKS.register("true_sight_stele",
                    () -> new cn.kurt6.unknown_echoes.block.truesight.TrueSightSteleBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 5).noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlock> TRUE_SIGHT_CHEST =
            BLOCKS.register("true_sight_chest",
                    () -> new cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 4).noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock> ARTIFACT_RECORD_TABLE =
            BLOCKS.register("artifact_record_table",
                    () -> new cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock(
                            cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.RECORD,
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock> ARTIFACT_TUNING_TABLE =
            BLOCKS.register("artifact_tuning_table",
                    () -> new cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock(
                            cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.TUNING,
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    public static final DeferredBlock<RubbleBlock> WIND_ERODED_RUBBLE = BLOCKS.register("wind_eroded_rubble",
            () -> new RubbleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.4F).sound(SoundType.GRAVEL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<BiomeGrassBlock> MIRROR_REED = BLOCKS.register("mirror_reed",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable().noCollission().instabreak().sound(SoundType.WET_GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 7)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AquaticBiomePlantBlock> TIDE_GRASS = BLOCKS.register("tide_grass",
            () -> new AquaticBiomePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable().noCollission().noOcclusion().instabreak().sound(SoundType.WET_GRASS)
                    .lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<AquaticSeaPickleBlock> PEARL_ANEMONE = BLOCKS.register("pearl_anemone",
            () -> new AquaticSeaPickleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .noCollission().noOcclusion().instabreak().sound(SoundType.CORAL_BLOCK)
                    .lightLevel(state -> 10).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<FlowerBlock> STAR_MIST_BLOOM = BLOCKS.register("star_mist_bloom",
            () -> new FlowerBlock(MobEffects.SLOW_FALLING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<FlowerBlock> MIRROR_LOTUS = BLOCKS.register("mirror_lotus",
            () -> new FlowerBlock(MobEffects.NIGHT_VISION, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.WET_GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<BiomeGrassBlock> ASH_CHIME_GRASS = BLOCKS.register("ash_chime_grass",
            () -> new BiomeGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 5)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<DeepBiomePlantBlock> FOLDED_SPOREBLOOM = BLOCKS.register("folded_sporebloom",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .noCollission().instabreak().sound(SoundType.FUNGUS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 7)
                            .pushReaction(PushReaction.DESTROY), () -> ModBlocks.FOLDED_SLATE.get()));

    public static final DeferredBlock<DeepBiomePlantBlock> GRAVITY_BELL = BLOCKS.register("gravity_bell",
            () -> new DeepBiomePlantBlock(MobEffects.SLOW_FALLING, 6.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.AMETHYST).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY), () -> ModBlocks.GRAVITY_SHALE.get()));

    public static final DeferredBlock<DeepBiomePlantBlock> HUSH_THORN = BLOCKS.register("hush_thorn",
            () -> new DeepBiomePlantBlock(MobEffects.DARKNESS, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            .noCollission().instabreak().sound(SoundType.SCULK).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 4)
                            .pushReaction(PushReaction.DESTROY), () -> ModBlocks.ABYSSAL_STONE.get()));

    public static final DeferredBlock<Block> MIRROR_DUST_STONE = BLOCKS.registerSimpleBlock("mirror_dust_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.8F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> MIRROR_DUST_BRICKS = BLOCKS.registerSimpleBlock("mirror_dust_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.8F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> FRACTURED_MIRROR_TILE = BLOCKS.registerSimpleBlock("fractured_mirror_tile",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> PRISM_DUST_CLUSTER = BLOCKS.registerSimpleBlock("prism_dust_cluster",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 7.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 10));
    public static final DeferredBlock<DeepBiomePlantBlock> GLASSVEIL_GRASS = BLOCKS.register("glassveil_grass",
            () -> new DeepBiomePlantBlock(MobEffects.NIGHT_VISION, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 3)
                            .pushReaction(PushReaction.DESTROY), MIRROR_DUST_STONE));
    public static final DeferredBlock<DeepBiomePlantBlock> REFLECTION_ORCHID = BLOCKS.register("reflection_orchid",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.AMETHYST).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY), MIRROR_DUST_STONE));

    public static final DeferredBlock<Block> ECHO_STRATA = BLOCKS.registerSimpleBlock("echo_strata",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.1F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> ECHO_STRATA_BRICKS = BLOCKS.registerSimpleBlock("echo_strata_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.1F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> CHISELED_ECHO_STRATA = BLOCKS.registerSimpleBlock("chiseled_echo_strata",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.1F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> ARCHIVE_MOSAIC = BLOCKS.registerSimpleBlock("archive_mosaic",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.1F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RESONANT_CLIFF_CRYSTAL = BLOCKS.registerSimpleBlock("resonant_cliff_crystal",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.2F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8));
    public static final DeferredBlock<DeepBiomePlantBlock> CLIFF_HEATHER = BLOCKS.register("cliff_heather",
            () -> new DeepBiomePlantBlock(MobEffects.SLOW_FALLING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 3)
                            .pushReaction(PushReaction.DESTROY), ECHO_STRATA));
    public static final DeferredBlock<DeepBiomePlantBlock> ARCHIVE_LICHEN = BLOCKS.register("archive_lichen",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .noCollission().instabreak().sound(SoundType.MOSS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 6)
                            .pushReaction(PushReaction.DESTROY), ECHO_STRATA));

    public static final DeferredBlock<Block> SONGSTONE = BLOCKS.registerSimpleBlock("songstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> SONGSTONE_BRICKS = BLOCKS.registerSimpleBlock("songstone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> CHISELED_SONGSTONE = BLOCKS.registerSimpleBlock("chiseled_songstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> SONGSTONE_TILES = BLOCKS.registerSimpleBlock("songstone_tiles",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> CRACKED_SONGSTONE_BRICKS = BLOCKS.registerSimpleBlock(
            "cracked_songstone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> RESONANT_GEODE = BLOCKS.registerSimpleBlock("resonant_geode",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 7.5F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 11));
    public static final DeferredBlock<DeepBiomePlantBlock> TUNING_GRASS = BLOCKS.register("tuning_grass",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 4)
                            .pushReaction(PushReaction.DESTROY), SONGSTONE));
    public static final DeferredBlock<DeepBiomePlantBlock> CRYSTAL_BELLFLOWER = BLOCKS.register("crystal_bellflower",
            () -> new DeepBiomePlantBlock(MobEffects.REGENERATION, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                            .noCollission().instabreak().sound(SoundType.AMETHYST).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY), SONGSTONE));

    public static final DeferredBlock<Block> ASHEN_BELLSTONE = BLOCKS.registerSimpleBlock("ashen_bellstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> ASHEN_BELLSTONE_BRICKS = BLOCKS.registerSimpleBlock("ashen_bellstone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RUSTED_BELLSTONE = BLOCKS.registerSimpleBlock("rusted_bellstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> CLOCKWORK_SCRAP_BLOCK = BLOCKS.registerSimpleBlock("clockwork_scrap_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(4.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> 4));
    public static final DeferredBlock<DeepBiomePlantBlock> CINDER_REED = BLOCKS.register("cinder_reed",
            () -> new DeepBiomePlantBlock(MobEffects.MOVEMENT_SLOWDOWN, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 2)
                            .pushReaction(PushReaction.DESTROY), ASHEN_BELLSTONE));
    public static final DeferredBlock<DeepBiomePlantBlock> CLOCKWORK_THISTLE = BLOCKS.register("clockwork_thistle",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .noCollission().instabreak().sound(SoundType.METAL).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 6)
                            .pushReaction(PushReaction.DESTROY), ASHEN_BELLSTONE));

    public static final DeferredBlock<Block> REEF_LIMESTONE = BLOCKS.registerSimpleBlock("reef_limestone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.6F, 6.5F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> REEF_LIMESTONE_BRICKS = BLOCKS.registerSimpleBlock("reef_limestone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.8F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> TIDEWORN_REEFSTONE = BLOCKS.registerSimpleBlock("tideworn_reefstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(2.7F, 6.5F).requiresCorrectToolForDrops().sound(SoundType.CORAL_BLOCK));
    public static final DeferredBlock<Block> TIDELIGHT_CORAL_BLOCK = BLOCKS.registerSimpleBlock("tidelight_coral_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(2.2F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.CORAL_BLOCK)
                    .lightLevel(state -> 11));
    public static final DeferredBlock<AquaticBiomePlantBlock> LANTERN_SEAFAN = BLOCKS.register("lantern_seafan",
            () -> new AquaticBiomePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .replaceable().noCollission().noOcclusion().instabreak().sound(SoundType.WET_GRASS)
                    .lightLevel(state -> 7).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<AquaticSeaPickleBlock> SHELL_BLOOM = BLOCKS.register("shell_bloom",
            () -> new AquaticSeaPickleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission().noOcclusion().instabreak().sound(SoundType.CORAL_BLOCK)
                    .lightLevel(state -> 12).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> SILENT_MUDSTONE = BLOCKS.registerSimpleBlock("silent_mudstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.TUFF));
    public static final DeferredBlock<Block> SILENT_MUDSTONE_BRICKS = BLOCKS.registerSimpleBlock("silent_mudstone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(3.0F, 7.5F).requiresCorrectToolForDrops().sound(SoundType.TUFF));
    public static final DeferredBlock<Block> ROOTBOUND_MUDSTONE = BLOCKS.registerSimpleBlock("rootbound_mudstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(2.9F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.ROOTED_DIRT));
    public static final DeferredBlock<Block> BOGLIGHT_STONE = BLOCKS.registerSimpleBlock("boglight_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(3.0F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.SCULK)
                    .lightLevel(state -> 9));
    public static final DeferredBlock<DeepBiomePlantBlock> MUTE_REED = BLOCKS.register("mute_reed",
            () -> new DeepBiomePlantBlock(MobEffects.MOVEMENT_SLOWDOWN, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                            .noCollission().instabreak().sound(SoundType.WET_GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 2)
                            .pushReaction(PushReaction.DESTROY), SILENT_MUDSTONE));
    public static final DeferredBlock<DeepBiomePlantBlock> MIRE_BELL = BLOCKS.register("mire_bell",
            () -> new DeepBiomePlantBlock(MobEffects.NIGHT_VISION, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                            .noCollission().instabreak().sound(SoundType.AMETHYST).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY), SILENT_MUDSTONE));

    public static final DeferredBlock<Block> FOLDED_SLATE = BLOCKS.registerSimpleBlock("folded_slate",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.2F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> FOLDED_SLATE_BRICKS = BLOCKS.registerSimpleBlock("folded_slate_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.2F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> CHISELED_FOLDED_SLATE = BLOCKS.registerSimpleBlock("chiseled_folded_slate",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
                    .strength(3.2F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> RESONANT_FOLDED_SLATE = BLOCKS.registerSimpleBlock("resonant_folded_slate",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8));
    public static final DeferredBlock<DeepBiomePlantBlock> SEAM_GRASS = BLOCKS.register("seam_grass",
            () -> new DeepBiomePlantBlock(MobEffects.SLOW_FALLING, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 3)
                            .pushReaction(PushReaction.DESTROY), FOLDED_SLATE));
    public static final DeferredBlock<DeepBiomePlantBlock> ARCHIVE_FUNGUS = BLOCKS.register("archive_fungus",
            () -> new DeepBiomePlantBlock(MobEffects.GLOWING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                            .noCollission().instabreak().sound(SoundType.FUNGUS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 7)
                            .pushReaction(PushReaction.DESTROY), FOLDED_SLATE));

    public static final DeferredBlock<Block> GRAVITY_SHALE = BLOCKS.registerSimpleBlock("gravity_shale",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.4F, 8.5F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> GRAVITY_SHALE_BRICKS = BLOCKS.registerSimpleBlock("gravity_shale_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.4F, 8.5F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> BANDED_GRAVITY_SHALE = BLOCKS.registerSimpleBlock("banded_gravity_shale",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.4F, 8.5F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> SUSPENDED_CRYSTAL = BLOCKS.registerSimpleBlock("suspended_crystal",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 7.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 11));
    public static final DeferredBlock<DeepBiomePlantBlock> INVERSION_REED = BLOCKS.register("inversion_reed",
            () -> new DeepBiomePlantBlock(MobEffects.SLOW_FALLING, 6.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY), GRAVITY_SHALE));
    public static final DeferredBlock<DeepBiomePlantBlock> WEIGHTLESS_BLOOM = BLOCKS.register("weightless_bloom",
            () -> new DeepBiomePlantBlock(MobEffects.SLOW_FALLING, 8.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.AMETHYST).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY), GRAVITY_SHALE));

    public static final DeferredBlock<Block> ABYSSAL_STONE = BLOCKS.registerSimpleBlock("abyssal_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(3.8F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> ABYSSAL_STONE_BRICKS = BLOCKS.registerSimpleBlock("abyssal_stone_bricks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(3.8F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> VEINED_ABYSSAL_STONE = BLOCKS.registerSimpleBlock("veined_abyssal_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(3.8F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> MUTE_CRYSTAL = BLOCKS.registerSimpleBlock("mute_crystal",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.6F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.SCULK)
                    .lightLevel(state -> 6));
    public static final DeferredBlock<Block> RESONANCE_ALLOY_BLOCK = BLOCKS.registerSimpleBlock(
            "resonance_alloy_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> 5));
    public static final DeferredBlock<Block> PALIMPSEST_BLOCK = BLOCKS.registerSimpleBlock(
            "palimpsest_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(4.5F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(state -> 3));
    public static final DeferredBlock<Block> ORRERY_PRISM_BLOCK = BLOCKS.registerSimpleBlock(
            "orrery_prism_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.2F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8));
    public static final DeferredBlock<DeepBiomePlantBlock> HUSH_GRASS = BLOCKS.register("hush_grass",
            () -> new DeepBiomePlantBlock(MobEffects.MOVEMENT_SLOWDOWN, 3.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            .noCollission().instabreak().sound(SoundType.SCULK).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 2)
                            .pushReaction(PushReaction.DESTROY), ABYSSAL_STONE));
    public static final DeferredBlock<DeepBiomePlantBlock> VOIDCAP = BLOCKS.register("voidcap",
            () -> new DeepBiomePlantBlock(MobEffects.NIGHT_VISION, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                            .noCollission().instabreak().sound(SoundType.FUNGUS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 6)
                            .pushReaction(PushReaction.DESTROY), ABYSSAL_STONE));

    public static final DeferredBlock<Block> ECHO_MOSSY_STONE = BLOCKS.registerSimpleBlock("echo_mossy_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.MOSS));

    public static final DeferredBlock<Block> WIND_CHISELED_STONE = BLOCKS.registerSimpleBlock("wind_chiseled_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.4F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE).lightLevel(state -> 2));

    /** 光照必须是 15：原版 isValidSpawn 只拦 getLightEmission() < 14,低于 14 吊灯网格会变成刷怪面。 */
    public static final DeferredBlock<Block> SKY_LAMP_GLASS = BLOCKS.registerSimpleBlock("sky_lamp_glass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F, 1.0F).sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion());

    /**
     * 批量照明的硬约束：纯装饰、不进 resource_blocks、无逆向配方、光照 15。
     * 进 resource_blocks 就是成片免费资源,光照低于 14 则吊灯网格变成刷怪面。
     */
    private static BlockBehaviour.Properties lanternGlassProperties(MapColor mapColor) {
        return BlockBehaviour.Properties.of().mapColor(mapColor)
                .strength(0.6F, 1.0F).sound(SoundType.GLASS)
                .lightLevel(state -> 15).noOcclusion();
    }

    public static final DeferredBlock<Block> REEF_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "reef_lantern_glass", lanternGlassProperties(MapColor.COLOR_CYAN));
    public static final DeferredBlock<Block> BELL_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "bell_lantern_glass", lanternGlassProperties(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<Block> HUSH_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "hush_lantern_glass", lanternGlassProperties(MapColor.COLOR_LIGHT_GRAY));
    public static final DeferredBlock<Block> FOLD_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "fold_lantern_glass", lanternGlassProperties(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> SONG_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "song_lantern_glass", lanternGlassProperties(MapColor.COLOR_GREEN));
    public static final DeferredBlock<Block> ARCHIVE_LANTERN_GLASS = BLOCKS.registerSimpleBlock(
            "archive_lantern_glass", lanternGlassProperties(MapColor.TERRACOTTA_WHITE));

    public static final DeferredBlock<Block> TIDE_SMOOTH_STONE = BLOCKS.registerSimpleBlock("tide_smooth_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<Block> MIRROR_SAND = BLOCKS.registerSimpleBlock("mirror_sand",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.5F).sound(SoundType.SAND));

    public static final DeferredBlock<Block> PEARL_CORAL_BLOCK = BLOCKS.registerSimpleBlock("pearl_coral_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F, 3.0F).sound(SoundType.CORAL_BLOCK).lightLevel(state -> 5));

    /** 复制母方块的强度、音效与工具要求，保证细部构件与母墙体手感一致。 */
    private static BlockBehaviour.Properties masonryPartProperties(
            MapColor mapColor, float destroyTime, float explosionResistance, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(mapColor)
                .strength(destroyTime, explosionResistance)
                .requiresCorrectToolForDrops().sound(sound);
    }

    public static final DeferredBlock<StairBlock> ECHO_STONE_BRICK_STAIRS =
            BLOCKS.register("echo_stone_brick_stairs",
                    () -> new StairBlock(ECHO_STONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_CYAN, 2.0F, 6.0F, SoundType.STONE)));
    public static final DeferredBlock<SlabBlock> ECHO_STONE_BRICK_SLAB =
            BLOCKS.register("echo_stone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_CYAN, 2.0F, 6.0F, SoundType.STONE)));
    public static final DeferredBlock<WallBlock> ECHO_STONE_BRICK_WALL =
            BLOCKS.register("echo_stone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_CYAN, 2.0F, 6.0F, SoundType.STONE)));

    public static final DeferredBlock<StairBlock> ECHO_STRATA_BRICK_STAIRS =
            BLOCKS.register("echo_strata_brick_stairs",
                    () -> new StairBlock(ECHO_STRATA_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_GRAY, 3.1F, 8.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> ECHO_STRATA_BRICK_SLAB =
            BLOCKS.register("echo_strata_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_GRAY, 3.1F, 8.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> ECHO_STRATA_BRICK_WALL =
            BLOCKS.register("echo_strata_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_GRAY, 3.1F, 8.0F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> SONGSTONE_BRICK_STAIRS =
            BLOCKS.register("songstone_brick_stairs",
                    () -> new StairBlock(SONGSTONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.TERRACOTTA_GREEN, 2.9F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<SlabBlock> SONGSTONE_BRICK_SLAB =
            BLOCKS.register("songstone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_GREEN, 2.9F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<WallBlock> SONGSTONE_BRICK_WALL =
            BLOCKS.register("songstone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_GREEN, 2.9F, 7.0F, SoundType.CALCITE)));

    public static final DeferredBlock<StairBlock> ASHEN_BELLSTONE_BRICK_STAIRS =
            BLOCKS.register("ashen_bellstone_brick_stairs",
                    () -> new StairBlock(ASHEN_BELLSTONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_GRAY, 3.5F, 9.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> ASHEN_BELLSTONE_BRICK_SLAB =
            BLOCKS.register("ashen_bellstone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_GRAY, 3.5F, 9.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> ASHEN_BELLSTONE_BRICK_WALL =
            BLOCKS.register("ashen_bellstone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_GRAY, 3.5F, 9.0F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> REEF_LIMESTONE_BRICK_STAIRS =
            BLOCKS.register("reef_limestone_brick_stairs",
                    () -> new StairBlock(REEF_LIMESTONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_LIGHT_BLUE, 2.8F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<SlabBlock> REEF_LIMESTONE_BRICK_SLAB =
            BLOCKS.register("reef_limestone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_BLUE, 2.8F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<WallBlock> REEF_LIMESTONE_BRICK_WALL =
            BLOCKS.register("reef_limestone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_BLUE, 2.8F, 7.0F, SoundType.CALCITE)));

    public static final DeferredBlock<StairBlock> SILENT_MUDSTONE_BRICK_STAIRS =
            BLOCKS.register("silent_mudstone_brick_stairs",
                    () -> new StairBlock(SILENT_MUDSTONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.TERRACOTTA_GREEN, 3.0F, 7.5F, SoundType.TUFF)));
    public static final DeferredBlock<SlabBlock> SILENT_MUDSTONE_BRICK_SLAB =
            BLOCKS.register("silent_mudstone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_GREEN, 3.0F, 7.5F, SoundType.TUFF)));
    public static final DeferredBlock<WallBlock> SILENT_MUDSTONE_BRICK_WALL =
            BLOCKS.register("silent_mudstone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_GREEN, 3.0F, 7.5F, SoundType.TUFF)));

    public static final DeferredBlock<StairBlock> MIRROR_STONE_BRICK_STAIRS =
            BLOCKS.register("mirror_stone_brick_stairs",
                    () -> new StairBlock(MIRROR_STONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_LIGHT_BLUE, 2.5F, 6.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> MIRROR_STONE_BRICK_SLAB =
            BLOCKS.register("mirror_stone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_BLUE, 2.5F, 6.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> MIRROR_STONE_BRICK_WALL =
            BLOCKS.register("mirror_stone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_BLUE, 2.5F, 6.0F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> MIRROR_DUST_BRICK_STAIRS =
            BLOCKS.register("mirror_dust_brick_stairs",
                    () -> new StairBlock(MIRROR_DUST_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_LIGHT_GRAY, 2.8F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<SlabBlock> MIRROR_DUST_BRICK_SLAB =
            BLOCKS.register("mirror_dust_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_GRAY, 2.8F, 7.0F, SoundType.CALCITE)));
    public static final DeferredBlock<WallBlock> MIRROR_DUST_BRICK_WALL =
            BLOCKS.register("mirror_dust_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_GRAY, 2.8F, 7.0F, SoundType.CALCITE)));

    public static final DeferredBlock<StairBlock> FOLDED_SLATE_BRICK_STAIRS =
            BLOCKS.register("folded_slate_brick_stairs",
                    () -> new StairBlock(FOLDED_SLATE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.TERRACOTTA_CYAN, 3.2F, 8.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> FOLDED_SLATE_BRICK_SLAB =
            BLOCKS.register("folded_slate_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_CYAN, 3.2F, 8.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> FOLDED_SLATE_BRICK_WALL =
            BLOCKS.register("folded_slate_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.TERRACOTTA_CYAN, 3.2F, 8.0F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> GRAVITY_SHALE_BRICK_STAIRS =
            BLOCKS.register("gravity_shale_brick_stairs",
                    () -> new StairBlock(GRAVITY_SHALE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_PURPLE, 3.4F, 8.5F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> GRAVITY_SHALE_BRICK_SLAB =
            BLOCKS.register("gravity_shale_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_PURPLE, 3.4F, 8.5F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> GRAVITY_SHALE_BRICK_WALL =
            BLOCKS.register("gravity_shale_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_PURPLE, 3.4F, 8.5F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> ABYSSAL_STONE_BRICK_STAIRS =
            BLOCKS.register("abyssal_stone_brick_stairs",
                    () -> new StairBlock(ABYSSAL_STONE_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_BLACK, 3.8F, 10.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<SlabBlock> ABYSSAL_STONE_BRICK_SLAB =
            BLOCKS.register("abyssal_stone_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_BLACK, 3.8F, 10.0F, SoundType.DEEPSLATE)));
    public static final DeferredBlock<WallBlock> ABYSSAL_STONE_BRICK_WALL =
            BLOCKS.register("abyssal_stone_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_BLACK, 3.8F, 10.0F, SoundType.DEEPSLATE)));

    public static final DeferredBlock<StairBlock> WIND_ETCHED_BRICK_STAIRS =
            BLOCKS.register("wind_etched_brick_stairs",
                    () -> new StairBlock(WIND_ETCHED_BRICKS.get().defaultBlockState(),
                            masonryPartProperties(MapColor.COLOR_LIGHT_GRAY, 2.5F, 6.0F, SoundType.CALCITE)));
    public static final DeferredBlock<SlabBlock> WIND_ETCHED_BRICK_SLAB =
            BLOCKS.register("wind_etched_brick_slab",
                    () -> new SlabBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_GRAY, 2.5F, 6.0F, SoundType.CALCITE)));
    public static final DeferredBlock<WallBlock> WIND_ETCHED_BRICK_WALL =
            BLOCKS.register("wind_etched_brick_wall",
                    () -> new WallBlock(masonryPartProperties(
                            MapColor.COLOR_LIGHT_GRAY, 2.5F, 6.0F, SoundType.CALCITE)));

    /** 木构构件：可燃、不要求工具，因此不能复用 masonryPartProperties（那个带 requiresCorrectToolForDrops）。 */
    private static BlockBehaviour.Properties timberPartProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    public static final DeferredBlock<StairBlock> WHISPERING_TIMBER_STAIRS =
            BLOCKS.register("whispering_timber_stairs",
                    () -> new StairBlock(WHISPERING_TIMBER_FRAME.get().defaultBlockState(),
                            timberPartProperties()));
    public static final DeferredBlock<SlabBlock> WHISPERING_TIMBER_SLAB =
            BLOCKS.register("whispering_timber_slab",
                    () -> new SlabBlock(timberPartProperties()));
    public static final DeferredBlock<WallBlock> WHISPERING_TIMBER_WALL =
            BLOCKS.register("whispering_timber_wall",
                    () -> new WallBlock(timberPartProperties()));

    public static final DeferredBlock<Block> WIND_ASHLAR = BLOCKS.register("wind_ashlar",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(WIND_ETCHED_BRICKS.get())));
    public static final DeferredBlock<Block> WIND_CARVED_FRIEZE = BLOCKS.register("wind_carved_frieze",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(WIND_ETCHED_BRICKS.get())));
    public static final DeferredBlock<Block> WHISPERING_PANELLED_PLANKS = BLOCKS.register("whispering_panelled_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(WHISPERING_TIMBER_FRAME.get())));
    public static final DeferredBlock<Block> WHISPERING_CARVED_PANELS = BLOCKS.register("whispering_carved_panels",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(WHISPERING_TIMBER_FRAME.get())));
    public static final DeferredBlock<Block> FOLDED_SLATE_TILES = BLOCKS.register("folded_slate_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(FOLDED_SLATE_BRICKS.get())));
    public static final DeferredBlock<Block> FOLDED_SLATE_RELIEF = BLOCKS.register("folded_slate_relief",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(FOLDED_SLATE_BRICKS.get())));
}
