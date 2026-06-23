package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.RewardAltarBlock;
import cn.kurt6.unknown_echoes.block.RubbleBlock;
import cn.kurt6.unknown_echoes.block.RuneBricksBlock;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlock;
import cn.kurt6.unknown_echoes.block.beacon.EchoBeaconBlock;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlock;
import cn.kurt6.unknown_echoes.block.boss.ResonanceCandleBlock;
import cn.kurt6.unknown_echoes.block.puzzle.HiddenRuneBlock;
import cn.kurt6.unknown_echoes.block.puzzle.MemoryPillarBlock;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UnknownEchoes.MODID);

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

    // ---- V0.2 谜题机关 ----

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
                    .lightLevel(state -> state.getValue(PuzzleCoreBlock.ACTIVE) ? 13 : 5).noLootTable()));

    public static final DeferredBlock<Block> SEALED_STONE = BLOCKS.register("sealed_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()));

    /** 永久防挖外壳；与会被谜题核心清除的 sealed_stone 分离。 */
    public static final DeferredBlock<Block> ANCHORED_SEALED_STONE = BLOCKS.register("anchored_sealed_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()));

    // ---- V0.5 镜湖 ----

    /** 镜石:镜湖湖底独有资源方块,采掘掉落镜湖碎片(精准采集掉自身)。 */
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

    // ---- V0.5B 镜湖神殿(真视) ----

    /** 镜像符印:真假同貌,REAL 只在服务端逻辑里;randomTicks 驱动真符印的"倒影涟漪"线索。 */
    public static final DeferredBlock<MirrorSigilBlock> MIRROR_SIGIL = BLOCKS.register("mirror_sigil",
            () -> new MirrorSigilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7).randomTicks().noLootTable()));

    public static final DeferredBlock<TrueSightAltarBlock> TRUE_SIGHT_ALTAR = BLOCKS.register("true_sight_altar",
            () -> new TrueSightAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 9).noLootTable().noOcclusion()));

    // ---- V0.5C 失语沼泽 ----

    /** 噤声苔:失语沼泽独有资源方块(消音与药剂材料)。已加入 minecraft:dirt tag,可承载植物。 */
    public static final DeferredBlock<Block> MUFFLE_MOSS = BLOCKS.registerSimpleBlock("muffle_moss",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.3F).sound(SoundType.MOSS));

    /** 沉默荚果丛:采集掉落沉默荚果(见战利品表)。 */
    public static final DeferredBlock<TallGrassBlock> SILENT_POD_BUSH = BLOCKS.register("silent_pod_bush",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .replaceable().noCollission().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)));

    // ---- V0.5C 漂浮群岛 ----

    /** 风纹石:漂浮群岛独有矿物,风之装备与风纹机关材料。 */
    public static final DeferredBlock<Block> WIND_ETCHED_STONE = BLOCKS.registerSimpleBlock("wind_etched_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    /** 天穹石英矿:漂浮群岛补充矿物,用于轻质风系材料与补给配方。 */
    public static final DeferredBlock<Block> SKY_QUARTZ_ORE = BLOCKS.registerSimpleBlock("sky_quartz_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.4F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    // ---- V0.7B 后期群系资源 ----

    /** 晶歌晶簇:晶歌林地地表晶体,采集获得晶歌碎片。 */
    public static final DeferredBlock<Block> CRYSTAL_SONG_CLUSTER = BLOCKS.register("crystal_song_cluster",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.2F, 3.0F).sound(SoundType.AMETHYST).lightLevel(state -> 10)
                    .requiresCorrectToolForDrops().noOcclusion()));

    /** 共鸣蘑菇:晶歌林地补充材料植物,无 tick。 */
    public static final DeferredBlock<FlowerBlock> RESONANT_MUSHROOM = BLOCKS.register("resonant_mushroom",
            () -> new FlowerBlock(MobEffects.GLOWING, 6.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.FUNGUS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY)));

    /** 沉眠花:沉眠花海补给资源,采集获得沉眠花蜜。 */
    public static final DeferredBlock<FlowerBlock> DREAM_FLOWER = BLOCKS.register("dream_flower",
            () -> new FlowerBlock(MobEffects.REGENERATION, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY)));

    /** 回响琥珀矿:沉眠花海与回响森林交界的温和矿物,补充饰品/食物材料。 */
    public static final DeferredBlock<Block> ECHO_AMBER_ORE = BLOCKS.registerSimpleBlock("echo_amber_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.2F, 5.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    /** 梦雾藤:沉眠花海装饰与药剂材料,无 tick。 */
    public static final DeferredBlock<TallGrassBlock> DREAM_MIST_VINE = BLOCKS.register("dream_mist_vine",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().instabreak().sound(SoundType.VINE)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY)));

    /** 残钟锈矿:残钟荒原受控矿物资源,采集获得残钟齿轮。 */
    public static final DeferredBlock<Block> BROKEN_BELL_ORE = BLOCKS.register("broken_bell_ore",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW)
                    .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    /** 潮盐矿:镜湖浅层盐晶,给食物和潮汐材料提供非战斗来源。 */
    public static final DeferredBlock<Block> TIDE_SALT_ORE = BLOCKS.registerSimpleBlock("tide_salt_ore",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .strength(1.8F, 4.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    /** 残钟荆棘:残钟荒原地表危险植物占位,本阶段只作资源与气氛。 */
    public static final DeferredBlock<TallGrassBlock> BROKEN_BELL_THORN = BLOCKS.register("broken_bell_thorn",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .replaceable().noCollission().instabreak().sound(SoundType.SWEET_BERRY_BUSH)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)));

    /** 回声峭壁石:终局峭壁的基底变体,用于区分大档案馆区域。 */
    public static final DeferredBlock<Block> ECHO_CLIFF_STONE = BLOCKS.registerSimpleBlock("echo_cliff_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.8F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    /** 回声大档案馆终端:读取第一维度完成度并结算 T7 个人奖励。 */
    public static final DeferredBlock<EchoArchiveTerminalBlock> ECHO_ARCHIVE_TERMINAL =
            BLOCKS.register("echo_archive_terminal",
                    () -> new EchoArchiveTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                            .lightLevel(state -> 11).noLootTable().noOcclusion()));

    /** 天空观测站核心:服务端校验风之回响,给出湖底方向弱线索(ObservatoryCoreBlock)。 */
    public static final DeferredBlock<ObservatoryCoreBlock> OBSERVATORY_CORE = BLOCKS.register("observatory_core",
            () -> new ObservatoryCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> 9).noLootTable()));

    // ---- V0.6E 风流平台(5.2 权限目标:激活查风之权限,气流只是表现+运动效果) ----

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock> WIND_CURRENT_PLATFORM =
            BLOCKS.register("wind_current_platform",
                    () -> new cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(
                                            cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock.ACTIVE) ? 10 : 4)
                                    .noLootTable().noOcclusion()));

    // ---- 回响森林植被 ----

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

    public static final DeferredBlock<RotatedPillarBlock> WHISPERING_LOG = BLOCKS.register("whispering_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F).sound(SoundType.WOOD).lightLevel(state -> 5).ignitedByLava()));

    public static final DeferredBlock<Block> WHISPERING_PLANKS = BLOCKS.registerSimpleBlock("whispering_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

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

    public static final DeferredBlock<TallGrassBlock> GLOW_GRASS = BLOCKS.register("glow_grass",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XYZ).lightLevel(state -> 8)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<FlowerBlock> ECHO_FLOWER = BLOCKS.register("echo_flower",
            () -> new FlowerBlock(MobEffects.GLOWING, 8.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .noCollission().instabreak().sound(SoundType.GRASS)
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 10)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<TallGrassBlock> GLOW_FERN = BLOCKS.register("glow_fern",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                    .pushReaction(PushReaction.DESTROY)));

    // ---- V0.6A Mini Boss ----

    /** Mini Boss 场地计时器:不可见技术方块,埋在场地内,负责首次生成与场地重开。无 BlockItem。 */
    public static final DeferredBlock<MiniBossSpawnerBlock> MINIBOSS_SPAWNER = BLOCKS.register("miniboss_spawner",
            () -> new MiniBossSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()));

    /** 共鸣烛:沉默祭坛四角机制方块。战斗中可打碎以缩短静默领域——刻意低强度、可徒手快拆。 */
    public static final DeferredBlock<ResonanceCandleBlock> RESONANCE_CANDLE = BLOCKS.register("resonance_candle",
            () -> new ResonanceCandleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(0.4F).sound(SoundType.CANDLE).lightLevel(state -> 10)
                    .noLootTable().noOcclusion()));

    // ---- V0.6C 镜湖区域变体 ----

    /** 潮汐浮标:潮汐灯塔礁破灯机制方块。执灯者冲刺撞灭后 20 秒冷却复亮,不可破坏(critical_blocks)。 */
    public static final DeferredBlock<TideBuoyBlock> TIDE_BUOY = BLOCKS.register("tide_buoy",
            () -> new TideBuoyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.LANTERN)
                    .lightLevel(state -> state.getValue(TideBuoyBlock.LIT) ? 11 : 2)
                    .noLootTable().noOcclusion()));

    /** 水下符文座:潮汐灯塔礁致盲机制方块。潮汐回响交互触发执灯者致盲,20 秒冷却。 */
    public static final DeferredBlock<TideRuneSeatBlock> TIDE_RUNE_SEAT = BLOCKS.register("tide_rune_seat",
            () -> new TideRuneSeatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(TideRuneSeatBlock.LIT) ? 8 : 3)
                    .noLootTable().noOcclusion()));

    /** 碎镜砖:镜尘回廊的碎镜纹路地面与隐藏房地台(结构装饰,critical_blocks 防拆)。 */
    public static final DeferredBlock<Block> CRACKED_MIRROR_BRICKS = BLOCKS.register("cracked_mirror_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 2).noLootTable()));

    // ---- V0.6E 潮汐感应门(5.3"进入镜湖倒影层"权限门,倒影回廊深层内室) ----

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock> TIDE_SENSOR_DOOR =
            BLOCKS.register("tide_sensor_door",
                    () -> new cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> state.getValue(
                                            cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlock.OPEN) ? 9 : 5)
                                    .noLootTable().noOcclusion()));

    // ---- V0.6E 真视碑文 + 真视宝箱(5.4 权限目标:读取隐藏碑文 / 打开真视宝箱) ----

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.truesight.TrueSightSteleBlock> TRUE_SIGHT_STELE =
            BLOCKS.register("true_sight_stele",
                    () -> new cn.kurt6.unknown_echoes.block.truesight.TrueSightSteleBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 5).noLootTable()));

    public static final DeferredBlock<cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlock> TRUE_SIGHT_CHEST =
            BLOCKS.register("true_sight_chest",
                    () -> new cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 4).noLootTable().noOcclusion()));

    // ---- V0.6D 神器台座 ----

    /** 神器记录台:校验领取资格→写 ArtifactData→发放凭据;复领作废旧序号(12.2)。 */
    public static final DeferredBlock<cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock> ARTIFACT_RECORD_TABLE =
            BLOCKS.register("artifact_record_table",
                    () -> new cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock(
                            cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.RECORD,
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    /** 神器升级台:按个人探索进度结算等级;2 级起调谐词条二选一(12.4)。 */
    public static final DeferredBlock<cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock> ARTIFACT_TUNING_TABLE =
            BLOCKS.register("artifact_tuning_table",
                    () -> new cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock(
                            cn.kurt6.unknown_echoes.artifact.ArtifactStationBlock.Mode.TUNING,
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
                                    .lightLevel(state -> 8).noLootTable().noOcclusion()));

    // ---- V0.6F 生态装饰(9.x:低成本环境反馈,无常驻 tick,不参与进度) ----

    /** 风蚀碎石:漂浮群岛地表碎石堆装饰(设定文档点名)。非满格故 noOcclusion。 */
    public static final DeferredBlock<RubbleBlock> WIND_ERODED_RUBBLE = BLOCKS.register("wind_eroded_rubble",
            () -> new RubbleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.4F).sound(SoundType.GRAVEL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    /** 镜湖苇:镜湖岸边浅水 cross 植物装饰(cutout 渲染,gotcha #14)。 */
    public static final DeferredBlock<TallGrassBlock> MIRROR_REED = BLOCKS.register("mirror_reed",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable().noCollission().instabreak().sound(SoundType.WET_GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 7)
                    .pushReaction(PushReaction.DESTROY)));

    /** 潮草:镜湖浅水水生植物,用于潮汐食物与海底景观。 */
    public static final DeferredBlock<SeagrassBlock> TIDE_GRASS = BLOCKS.register("tide_grass",
            () -> new SeagrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .replaceable().noCollission().instabreak().sound(SoundType.WET_GRASS)
                    .lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY)));

    /** 珍珠海葵:海底发光植物,作为珍珠蚌和潮汐食物路线的普通来源。 */
    public static final DeferredBlock<SeaPickleBlock> PEARL_ANEMONE = BLOCKS.register("pearl_anemone",
            () -> new SeaPickleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .noCollission().instabreak().sound(SoundType.CORAL_BLOCK)
                    .lightLevel(state -> 10).pushReaction(PushReaction.DESTROY)));

    /** 星雾花:漂浮群岛/沉眠花海的低密度发光花,只作景观与普通食材来源。 */
    public static final DeferredBlock<FlowerBlock> STAR_MIST_BLOOM = BLOCKS.register("star_mist_bloom",
            () -> new FlowerBlock(MobEffects.SLOW_FALLING, 5.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 9)
                            .pushReaction(PushReaction.DESTROY)));

    /** 镜莲:镜湖岸线点缀植物,提供短效探索补给食材。 */
    public static final DeferredBlock<FlowerBlock> MIRROR_LOTUS = BLOCKS.register("mirror_lotus",
            () -> new FlowerBlock(MobEffects.NIGHT_VISION, 4.0F,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission().instabreak().sound(SoundType.WET_GRASS).noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 8)
                            .pushReaction(PushReaction.DESTROY)));

    /** 灰铃草:残钟荒原与峭壁边缘的稀疏草丛,只做气氛和可采集装饰。 */
    public static final DeferredBlock<TallGrassBlock> ASH_CHIME_GRASS = BLOCKS.register("ash_chime_grass",
            () -> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW)
                    .replaceable().noCollission().instabreak().sound(SoundType.GRASS).noOcclusion()
                    .offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(state -> 5)
                    .pushReaction(PushReaction.DESTROY)));

    // ---- V0.7V 基础景观方块:用于自然过渡与建筑调色,不承载进度权限 ----

    public static final DeferredBlock<Block> ECHO_MOSSY_STONE = BLOCKS.registerSimpleBlock("echo_mossy_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.MOSS));

    public static final DeferredBlock<Block> WIND_CHISELED_STONE = BLOCKS.registerSimpleBlock("wind_chiseled_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.4F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE).lightLevel(state -> 2));

    public static final DeferredBlock<Block> SKY_LAMP_GLASS = BLOCKS.registerSimpleBlock("sky_lamp_glass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F, 1.0F).sound(SoundType.GLASS).lightLevel(state -> 9).noOcclusion());

    public static final DeferredBlock<Block> TIDE_SMOOTH_STONE = BLOCKS.registerSimpleBlock("tide_smooth_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));

    public static final DeferredBlock<Block> MIRROR_SAND = BLOCKS.registerSimpleBlock("mirror_sand",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(0.5F).sound(SoundType.SAND));

    public static final DeferredBlock<Block> PEARL_CORAL_BLOCK = BLOCKS.registerSimpleBlock("pearl_coral_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F, 3.0F).sound(SoundType.CORAL_BLOCK).lightLevel(state -> 5));
}
