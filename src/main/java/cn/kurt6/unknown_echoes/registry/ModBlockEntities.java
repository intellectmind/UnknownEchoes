package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlockEntity;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.BossRechallengeAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.GravityFieldCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlockEntity;
import cn.kurt6.unknown_echoes.block.puzzle.PuzzleCoreBlockEntity;
import cn.kurt6.unknown_echoes.block.truesight.MirrorSigilBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UnknownEchoes.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PuzzleCoreBlockEntity>> PUZZLE_CORE =
            BLOCK_ENTITY_TYPES.register("puzzle_core",
                    () -> BlockEntityType.Builder.of(PuzzleCoreBlockEntity::new,
                            ModBlocks.PUZZLE_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MiniBossSpawnerBlockEntity>> MINIBOSS_SPAWNER =
            BLOCK_ENTITY_TYPES.register("miniboss_spawner",
                    () -> BlockEntityType.Builder.of(MiniBossSpawnerBlockEntity::new,
                            ModBlocks.MINIBOSS_SPAWNER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MirrorSigilBlockEntity>> MIRROR_SIGIL =
            BLOCK_ENTITY_TYPES.register("mirror_sigil",
                    () -> BlockEntityType.Builder.of(MirrorSigilBlockEntity::new,
                            ModBlocks.MIRROR_SIGIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity>> WIND_CURRENT_PLATFORM =
            BLOCK_ENTITY_TYPES.register("wind_current_platform",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity::new,
                            ModBlocks.WIND_CURRENT_PLATFORM.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlockEntity>> TIDE_SENSOR_DOOR =
            BLOCK_ENTITY_TYPES.register("tide_sensor_door",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlockEntity::new,
                            ModBlocks.TIDE_SENSOR_DOOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity>> TRUE_SIGHT_CHEST =
            BLOCK_ENTITY_TYPES.register("true_sight_chest",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity::new,
                            ModBlocks.TRUE_SIGHT_CHEST.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoArchiveTerminalBlockEntity>> ECHO_ARCHIVE_TERMINAL =
            BLOCK_ENTITY_TYPES.register("echo_archive_terminal",
                    () -> BlockEntityType.Builder.of(EchoArchiveTerminalBlockEntity::new,
                            ModBlocks.ECHO_ARCHIVE_TERMINAL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityFieldCoreBlockEntity>> GRAVITY_FIELD_CORE =
            BLOCK_ENTITY_TYPES.register("gravity_field_core",
                    () -> BlockEntityType.Builder.of(GravityFieldCoreBlockEntity::new,
                            ModBlocks.GRAVITY_FIELD_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BossRechallengeAltarBlockEntity>> BOSS_RECHALLENGE_ALTAR =
            BLOCK_ENTITY_TYPES.register("boss_rechallenge_altar",
                    () -> BlockEntityType.Builder.of(BossRechallengeAltarBlockEntity::new,
                            ModBlocks.GRAVITY_CORE_ALTAR.get(), ModBlocks.VOID_CORE_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldResonanceAltarBlockEntity>> WORLD_RESONANCE_ALTAR =
            BLOCK_ENTITY_TYPES.register("world_resonance_altar",
                    () -> BlockEntityType.Builder.of(WorldResonanceAltarBlockEntity::new,
                            ModBlocks.WORLD_RESONANCE_ALTAR.get()).build(null));
}
