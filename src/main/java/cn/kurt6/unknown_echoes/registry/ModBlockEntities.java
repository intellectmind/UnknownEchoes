package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.archive.EchoArchiveTerminalBlockEntity;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
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

    /** 镜像符印真假标记(V0.6A 自方块状态迁移,红线 #9:不向客户端同步)。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MirrorSigilBlockEntity>> MIRROR_SIGIL =
            BLOCK_ENTITY_TYPES.register("mirror_sigil",
                    () -> BlockEntityType.Builder.of(MirrorSigilBlockEntity::new,
                            ModBlocks.MIRROR_SIGIL.get()).build(null));

    /** 风流平台(V0.6E):激活倒计时 + 上升气流柱。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity>> WIND_CURRENT_PLATFORM =
            BLOCK_ENTITY_TYPES.register("wind_current_platform",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity::new,
                            ModBlocks.WIND_CURRENT_PLATFORM.get()).build(null));

    /** 潮汐感应门(V0.6E):感应扫描 + 开门保持倒计时。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlockEntity>> TIDE_SENSOR_DOOR =
            BLOCK_ENTITY_TYPES.register("tide_sensor_door",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.tide.TideSensorDoorBlockEntity::new,
                            ModBlocks.TIDE_SENSOR_DOOR.get()).build(null));

    /** 真视宝箱(V0.6E):标准战利品容器,开箱入口做真视权限校验。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity>> TRUE_SIGHT_CHEST =
            BLOCK_ENTITY_TYPES.register("true_sight_chest",
                    () -> BlockEntityType.Builder.of(
                            cn.kurt6.unknown_echoes.block.truesight.TrueSightChestBlockEntity::new,
                            ModBlocks.TRUE_SIGHT_CHEST.get()).build(null));

    /** 回声大档案馆终端(V0.7J):完成度读取 + T7 个人奖励入口。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoArchiveTerminalBlockEntity>> ECHO_ARCHIVE_TERMINAL =
            BLOCK_ENTITY_TYPES.register("echo_archive_terminal",
                    () -> BlockEntityType.Builder.of(EchoArchiveTerminalBlockEntity::new,
                            ModBlocks.ECHO_ARCHIVE_TERMINAL.get()).build(null));
}
