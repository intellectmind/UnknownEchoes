package cn.kurt6.unknown_echoes.block.beacon;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 第一维度毕业入口。每座信标的三段校准按玩家和坐标独立保存。 */
public class AbyssBeaconBlock extends Block {
    public static final MapCodec<AbyssBeaconBlock> CODEC = simpleCodec(AbyssBeaconBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(4, 4, 4, 12, 8, 12),
            Block.box(6, 8, 6, 10, 16, 10));
    private static final String CALIBRATION_PREFIX = "abyss_beacon:";
    private static final int STAGE_COOLDOWN_TICKS = 24;
    private static final int ACTIVATION_COOLDOWN_TICKS = 62;

    public AbyssBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }

        if (level.dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            DeepEchoTeleporter.returnToEchoRealm(serverPlayer);
            return InteractionResult.CONSUME;
        }
        if (!level.dimension().equals(EchoBeaconBlock.ECHO_REALM)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.wrong_realm"), true);
            return InteractionResult.CONSUME;
        }

        EchoCompletionManager.refreshMainlineCompletion(serverPlayer);
        if (!EchoPermission.canCalibrateAbyssBeacon(serverPlayer)) {
            fail(level, pos, serverPlayer, "message.unknown_echoes.abyss_beacon.mainline_required");
            return InteractionResult.CONSUME;
        }

        String baseKey = calibrationKey(level, pos);
        if (serverPlayer.getCooldowns().isOnCooldown(ModItems.ABYSS_BEACON.get())) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.calibration_settling"), true);
            return InteractionResult.CONSUME;
        }
        if (isStageComplete(serverPlayer, baseKey, "complete")) {
            if (EchoPermission.canEnterDeepEchoes(serverPlayer)) {
                DeepEchoTeleporter.enter(serverPlayer, pos);
            }
            return InteractionResult.CONSUME;
        }

        if (!isStageComplete(serverPlayer, baseKey, "wind")) {
            calibrateStage(level, pos, serverPlayer, baseKey, "wind", EchoAbilityType.WIND_ECHO,
                    ParticleTypes.CLOUD, SoundEvents.AMETHYST_BLOCK_RESONATE);
        } else if (!isStageComplete(serverPlayer, baseKey, "tide")) {
            calibrateStage(level, pos, serverPlayer, baseKey, "tide", EchoAbilityType.TIDE_ECHO,
                    ParticleTypes.SPLASH, SoundEvents.CONDUIT_ACTIVATE);
        } else {
            if (!EchoAbilityManager.hasAbility(serverPlayer, EchoAbilityType.TRUE_SIGHT_ECHO)) {
                fail(level, pos, serverPlayer, "message.unknown_echoes.abyss_beacon.need_true_sight");
                return InteractionResult.CONSUME;
            }
            if (!EchoAbilityManager.activateBeaconRoute(serverPlayer,
                    EchoPermission.ABYSS_BEACON_ID, EchoPermission.DEEP_ECHOES_ID,
                    baseKey + ":true_sight", baseKey + ":complete")) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.progress_record.full"), true);
                return InteractionResult.CONSUME;
            }
            playStageFeedback(level, pos, serverPlayer, "true_sight",
                    EchoAbilityType.TRUE_SIGHT_ECHO, ParticleTypes.ENCHANT,
                    SoundEvents.BEACON_ACTIVATE);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.abyss_beacon.activated"));
            if (level instanceof ServerLevel serverLevel) {
                playActivationFx(serverLevel, pos);
            }
            serverPlayer.getCooldowns().addCooldown(
                    ModItems.ABYSS_BEACON.get(), ACTIVATION_COOLDOWN_TICKS);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean calibrateStage(Level level, BlockPos pos, ServerPlayer player, String baseKey,
                                          String stage, EchoAbilityType ability,
                                          net.minecraft.core.particles.ParticleOptions particle,
                                          net.minecraft.sounds.SoundEvent sound) {
        if (!EchoAbilityManager.hasAbility(player, ability)) {
            fail(level, pos, player, "message.unknown_echoes.abyss_beacon.need_" + stage);
            return false;
        }
        if (!EchoAbilityManager.activateMechanism(player, baseKey + ":" + stage)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return false;
        }
        EchoAbilityManager.syncToClient(player);
        playStageFeedback(level, pos, player, stage, ability, particle, sound);
        return true;
    }

    private static void playStageFeedback(Level level, BlockPos pos, ServerPlayer player,
                                          String stage, EchoAbilityType ability,
                                          net.minecraft.core.particles.ParticleOptions particle,
                                          net.minecraft.sounds.SoundEvent sound) {
        player.getCooldowns().addCooldown(ModItems.ABYSS_BEACON.get(), STAGE_COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.abyss_beacon.calibrated_" + stage), true);
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.9F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            playCalibrationFx(serverLevel, pos, ability, particle);
        }
    }

    private static void playCalibrationFx(ServerLevel level, BlockPos pos,
                                          EchoAbilityType ability, ParticleOptions primary) {
        ParticleOptions accent = switch (ability) {
            case WIND_ECHO -> ParticleTypes.END_ROD;
            case TIDE_ECHO -> ParticleTypes.NAUTILUS;
            case TRUE_SIGHT_ECHO -> ParticleTypes.GLOW;
            case GRAVITY_ECHO -> ParticleTypes.ELECTRIC_SPARK;
            case VOID_ECHO -> ParticleTypes.REVERSE_PORTAL;
        };
        int startTick = level.getServer().getTickCount();
        for (int frame = 0; frame <= 22; frame++) {
            int frameIndex = frame;
            level.getServer().tell(new TickTask(startTick + frame, () -> {
                if (!canPlayFx(level, pos)) {
                    return;
                }
                double progress = frameIndex / 22.0D;
                double radius = 1.35D - progress * 0.95D;
                double height = 0.25D + progress * 3.4D;
                double direction = ability == EchoAbilityType.TIDE_ECHO ? -1.0D : 1.0D;
                double phase = frameIndex * 0.42D * direction;
                for (int arm = 0; arm < 5; arm++) {
                    double angle = phase + arm * Math.PI * 2.0D / 5.0D;
                    double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
                    double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
                    level.sendParticles(primary, x, pos.getY() + height, z,
                            2, 0.035D, 0.035D, 0.035D, 0.015D);
                    if ((frameIndex + arm) % 3 == 0) {
                        level.sendParticles(accent, x, pos.getY() + height + 0.12D, z,
                                1, 0.02D, 0.03D, 0.02D, 0.01D);
                    }
                }
                if (frameIndex % 5 == 0) {
                    emitRing(level, pos, primary, 0.7D + progress * 1.5D,
                            0.35D + progress * 1.2D, 20);
                }
            }));
        }
    }

    private static void playActivationFx(ServerLevel level, BlockPos pos) {
        int startTick = level.getServer().getTickCount();
        for (int frame = 0; frame <= 30; frame++) {
            int frameIndex = frame;
            level.getServer().tell(new TickTask(startTick + frame * 2, () -> {
                if (!canPlayFx(level, pos)) {
                    return;
                }
                double progress = frameIndex / 30.0D;
                double pulse = Math.sin(progress * Math.PI);
                double radius = 0.55D + pulse * 2.5D;
                double y = 0.45D + progress * 3.8D;
                double phase = frameIndex * 0.28D;
                for (int arm = 0; arm < 5; arm++) {
                    double angle = phase + arm * Math.PI * 2.0D / 5.0D;
                    double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
                    double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
                    ParticleOptions echoParticle = switch (arm) {
                        case 0 -> ParticleTypes.CLOUD;
                        case 1 -> ParticleTypes.SPLASH;
                        case 2 -> ParticleTypes.ENCHANT;
                        case 3 -> ParticleTypes.ELECTRIC_SPARK;
                        default -> ParticleTypes.REVERSE_PORTAL;
                    };
                    level.sendParticles(echoParticle, x, pos.getY() + y, z,
                            2, 0.025D, 0.04D, 0.025D, 0.01D);
                    level.sendParticles(ParticleTypes.END_ROD, x, pos.getY() + y + 0.08D, z,
                            1, 0.015D, 0.025D, 0.015D, 0.005D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            pos.getX() + 0.5D - Math.cos(angle) * radius * 0.65D,
                            pos.getY() + y * 0.82D,
                            pos.getZ() + 0.5D - Math.sin(angle) * radius * 0.65D,
                            1, 0.025D, 0.04D, 0.025D, 0.02D);
                }
                if (frameIndex % 6 == 0) {
                    emitRing(level, pos, ParticleTypes.ENCHANT,
                            0.8D + progress * 2.6D, 0.3D + progress * 0.9D, 28);
                }
                if (frameIndex == 12) {
                    level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT,
                            SoundSource.BLOCKS, 1.4F, 0.7F);
                } else if (frameIndex == 24) {
                    level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN,
                            SoundSource.BLOCKS, 1.8F, 1.1F);
                } else if (frameIndex == 30) {
                    double x = pos.getX() + 0.5D;
                    double yTop = pos.getY() + 3.6D;
                    double z = pos.getZ() + 0.5D;
                    level.sendParticles(ParticleTypes.FLASH, x, yTop, z,
                            3, 0.0D, 0.0D, 0.0D, 0.0D);
                    level.sendParticles(ParticleTypes.END_ROD, x, yTop, z,
                            110, 1.8D, 2.1D, 1.8D, 0.22D);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, yTop, z,
                            140, 2.1D, 2.3D, 2.1D, 0.3D);
                }
            }));
        }
    }

    private static void emitRing(ServerLevel level, BlockPos pos, ParticleOptions particle,
                                 double radius, double height, int points) {
        for (int point = 0; point < points; point++) {
            double angle = point * Math.PI * 2.0D / points;
            level.sendParticles(particle,
                    pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + height,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius,
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static boolean canPlayFx(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(ModBlocks.ABYSS_BEACON.get());
    }

    private static void fail(Level level, BlockPos pos, ServerPlayer player, String messageKey) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.7F, 0.45F);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private static boolean isStageComplete(ServerPlayer player, String baseKey, String stage) {
        return EchoAbilityManager.hasActivatedMechanism(player, baseKey + ":" + stage);
    }

    private static String calibrationKey(Level level, BlockPos pos) {
        return CALIBRATION_PREFIX + level.dimension().location() + ":"
                + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
