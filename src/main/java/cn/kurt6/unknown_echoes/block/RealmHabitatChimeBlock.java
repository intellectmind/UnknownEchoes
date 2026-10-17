package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** 三域公共巨构的生态设施，只召集附近既有生灵，不生成实体或写入进度。 */
public final class RealmHabitatChimeBlock extends Block {
    public static final MapCodec<RealmHabitatChimeBlock> CODEC =
            simpleCodec(RealmHabitatChimeBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final int ACTIVE_TICKS = 200;
    public static final double CALL_RADIUS = 24.0D;
    public static final int MAX_RESPONDERS = 6;
    private record ActivationKey(ServerLevel level, BlockPos pos) { }
    private static final java.util.Set<ActivationKey> ACTIVATING = new java.util.HashSet<>();

    public record ActivationResult(int responders, boolean active) {
        private static final ActivationResult NONE = new ActivationResult(0, false);
    }

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(3, 3, 3, 13, 6, 13),
            Block.box(2, 6, 2, 5, 14, 5),
            Block.box(11, 6, 2, 14, 14, 5),
            Block.box(2, 6, 11, 5, 14, 14),
            Block.box(11, 6, 11, 14, 14, 14),
            Block.box(3, 12, 3, 13, 15, 13),
            Block.box(6, 7, 6, 10, 13, 10));

    public RealmHabitatChimeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MODE, Mode.WAYFARER)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Mode mode = modeForDimension(context.getLevel());
        return defaultBlockState().setValue(MODE, mode == null ? Mode.WAYFARER : mode);
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
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !serverPlayer.isAlive()
                || !serverPlayer.canInteractWithBlock(pos, 1.0D)
                || serverPlayer.isSpectator()
                || serverPlayer.level() != serverLevel
                || serverLevel.getBlockState(pos) != state) {
            return InteractionResult.CONSUME;
        }

        Mode mode = modeForDimension(serverLevel);
        if (mode == null || state.getValue(MODE) != mode) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_habitat_chime.wrong_realm"), true);
            return InteractionResult.CONSUME;
        }
        if (state.getValue(ACTIVE)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_habitat_chime.resting"), true);
            return InteractionResult.CONSUME;
        }

        activateFromFacility(serverLevel, pos, mode);
        return InteractionResult.CONSUME;
    }

    public static int activateFromFacility(ServerLevel level, BlockPos pos, Mode expectedMode) {
        if (!level.hasChunkAt(pos) || !isAvailable(level.getBlockState(pos), expectedMode)
                || modeForDimension(level) != expectedMode) return 0;
        List<PathfinderMob> residents = level.getEntitiesOfClass(PathfinderMob.class,
                new AABB(pos).inflate(CALL_RADIUS),
                mob -> mob.isAlive() && mob.getTarget() == null && !mob.isLeashed()
                        && !mob.isPassenger() && !mob.isVehicle()
                        && mob.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= CALL_RADIUS * CALL_RADIUS
                        && matchesMode(mob, expectedMode));
        return activateFromFacility(level, pos, expectedMode, residents, mob -> {
        });
    }

    public static int activateFromFacility(ServerLevel level, BlockPos pos, Mode expectedMode,
                                           List<PathfinderMob> residents,
                                           Consumer<PathfinderMob> onResponder) {
        return activateWithResidents(level, pos, expectedMode, residents, onResponder).responders();
    }

    public static ActivationResult activateWithResidents(ServerLevel level, BlockPos pos, Mode expectedMode,
                                                           List<PathfinderMob> residents,
                                                           Consumer<PathfinderMob> onResponder) {
        return activateWithResidents(level, pos, expectedMode, residents, mob -> true, onResponder);
    }

    /** 调用方的额外资格随导航回调重新校验，不能只在收集候选时检查一次。 */
    public static ActivationResult activateWithResidents(ServerLevel level, BlockPos pos, Mode expectedMode,
                                                           List<PathfinderMob> residents,
                                                           Predicate<PathfinderMob> canRespond,
                                                           Consumer<PathfinderMob> onResponder) {
        ActivationKey key = new ActivationKey(level, pos.immutable());
        if (!ACTIVATING.add(key)) return ActivationResult.NONE;
        try {
            if (!level.hasChunkAt(pos)) return ActivationResult.NONE;
            BlockState state = level.getBlockState(pos);
            if (!isAvailable(state, expectedMode)
                    || modeForDimension(level) != expectedMode) {
                return ActivationResult.NONE;
            }
            BlockState activeState = state.setValue(ACTIVE, true);
            boolean written;
            try {
                written = level.setBlock(pos, activeState, Block.UPDATE_ALL);
            } finally {
                // 方块通知抛异常时，已提交的冷却仍必须按原排期恢复。
                if (isCurrentChime(level, pos, activeState)) {
                    level.scheduleTick(pos, activeState.getBlock(), ACTIVE_TICKS);
                }
            }
            if (!written || !isCurrentChime(level, pos, activeState)) return ActivationResult.NONE;
            int responders = callNearbyResidents(level, pos, expectedMode, activeState, residents, canRespond, onResponder);
            if (isCurrentChime(level, pos, activeState)) playCall(level, pos, expectedMode);
            return new ActivationResult(responders, isCurrentChime(level, pos, activeState));
        } finally {
            ACTIVATING.remove(key);
        }
    }

    private static boolean isAvailable(BlockState state, Mode mode) {
        return state.getBlock() instanceof RealmHabitatChimeBlock
                && state.getValue(MODE) == mode && !state.getValue(ACTIVE);
    }

    private static boolean isCurrentChime(ServerLevel level, BlockPos pos, BlockState expected) {
        return level.hasChunkAt(pos) && level.getBlockState(pos) == expected;
    }

    private static boolean isEligibleResident(ServerLevel level, BlockPos pos, Mode mode, PathfinderMob mob) {
        return mob.level() == level && !mob.isRemoved() && mob.isAlive() && mob.getTarget() == null
                && !mob.isLeashed() && !mob.isPassenger() && !mob.isVehicle() && matchesMode(mob, mode)
                && mob.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= CALL_RADIUS * CALL_RADIUS;
    }

    private static int callNearbyResidents(ServerLevel level, BlockPos pos, Mode mode, BlockState expected,
                                           List<PathfinderMob> residents,
                                           Predicate<PathfinderMob> canRespond,
                                           Consumer<PathfinderMob> onResponder) {
        List<PathfinderMob> responders = new ArrayList<>(residents);
        responders.removeIf(mob -> !isEligibleResident(level, pos, mode, mob) || !canRespond.test(mob));
        responders.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)));

        int moved = 0;
        for (PathfinderMob mob : responders) {
            if (moved >= MAX_RESPONDERS || !isCurrentChime(level, pos, expected)) {
                break;
            }
            if (!isEligibleResident(level, pos, mode, mob)) continue;
            if (!canRespond.test(mob)) continue;
            double angle = moved * Math.PI * 2.0D / MAX_RESPONDERS;
            double targetX = pos.getX() + 0.5D + Math.cos(angle) * 3.0D;
            double targetZ = pos.getZ() + 0.5D + Math.sin(angle) * 3.0D;
            mob.getLookControl().setLookAt(pos.getX() + 0.5D,
                    pos.getY() + 1.0D, pos.getZ() + 0.5D);
            if (mob.getNavigation().moveTo(targetX, pos.getY(), targetZ, 1.0D)) {
                if (!isCurrentChime(level, pos, expected)) break;
                if (!isEligibleResident(level, pos, mode, mob)) continue;
                if (!canRespond.test(mob)) continue;
                onResponder.accept(mob);
                moved++;
            }
        }
        return moved;
    }

    public static boolean matchesMode(PathfinderMob mob, Mode mode) {
        return switch (mode) {
            case WAYFARER -> mob.getType() == ModEntities.LOST_TRAVELER.get()
                    || mob instanceof Animal
                    && (!(mob instanceof TamableAnimal tamable) || !tamable.isTame());
            case CONFLUENCE -> mob.getType() == ModEntities.CRYSTAL_CHIME_SPIRIT.get()
                    || mob.getType() == ModEntities.CRYSTAL_FEATHER_BIRD.get()
                    || mob.getType() == ModEntities.ECHO_DEER.get()
                    || mob.getType() == ModEntities.GLOW_RABBIT.get()
                    || mob.getType() == ModEntities.MOSS_BACK_TURTLE.get()
                    || mob.getType() == ModEntities.DREAMING_DEER.get()
                    || mob.getType() == ModEntities.BROKEN_BELL_CROW.get()
                    || mob.getType() == ModEntities.WIND_SAC_MOTH.get();
            case INTERLAYER -> mob.getType() == ModEntities.FOLDPAGE_BUG.get()
                    || mob.getType() == ModEntities.ANCHOR_SHELL_BEAST.get()
                    || mob.getType() == ModEntities.ASHBACK_GRAZER.get()
                    || mob.getType() == ModEntities.ASHVEIL_STALKER.get()
                    || mob.getType() == ModEntities.FOLDED_MANTIS.get()
                    || mob.getType() == ModEntities.GRAVITY_RAM.get()
                    || mob.getType() == ModEntities.HUSHLIGHT_JELLY.get()
                    || mob.getType() == ModEntities.RIFT_GLIDER.get();
        };
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                8, 0.3D, 0.2D, 0.3D, 0.01D);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(2) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.25D + random.nextDouble() * 0.5D;
        level.addParticle(particleFor(state.getValue(MODE)),
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 0.8D + random.nextDouble() * 0.65D,
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                -Math.sin(angle) * 0.01D, 0.02D, Math.cos(angle) * 0.01D);
    }

    public static Mode modeForDimension(Level level) {
        ResourceLocation dimensionId = level.dimension().location();
        if (dimensionId.equals(Level.OVERWORLD.location())) {
            return Mode.WAYFARER;
        }
        if (dimensionId.equals(EchoPermission.ECHO_REALM_ID)) {
            return Mode.CONFLUENCE;
        }
        if (dimensionId.equals(EchoPermission.DEEP_ECHOES_ID)) {
            return Mode.INTERLAYER;
        }
        return null;
    }

    private static void playCall(ServerLevel level, BlockPos pos, Mode mode) {
        level.sendParticles(particleFor(mode),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                36, 0.85D, 0.5D, 0.85D, 0.05D);
        level.playSound(null, pos, switch (mode) {
            case WAYFARER -> SoundEvents.BELL_BLOCK;
            case CONFLUENCE -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case INTERLAYER -> SoundEvents.RESPAWN_ANCHOR_AMBIENT;
        }, SoundSource.BLOCKS, 0.9F, switch (mode) {
            case WAYFARER -> 1.15F;
            case CONFLUENCE -> 1.45F;
            case INTERLAYER -> 0.75F;
        });
    }

    private static ParticleOptions particleFor(Mode mode) {
        return switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
    }

    public enum Mode implements StringRepresentable {
        WAYFARER("wayfarer"),
        CONFLUENCE("confluence"),
        INTERLAYER("interlayer");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
