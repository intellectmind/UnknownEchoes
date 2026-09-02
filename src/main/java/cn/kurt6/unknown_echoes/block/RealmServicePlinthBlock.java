package cn.kurt6.unknown_echoes.block;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.ecology.RealmFacilityCircuitManager;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 六座三域公共巨构共用的服务设施，只转换普通资源，不写关键进度。 */
public final class RealmServicePlinthBlock extends Block {
    public static final MapCodec<RealmServicePlinthBlock> CODEC =
            simpleCodec(RealmServicePlinthBlock::new);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public static final int TRACING_PAPER_COST = 3;
    public static final int BREAD_COST = 4;
    public static final int BONE_MEAL_COST = 2;
    public static final int ENERGY_REFILL = 6;
    public static final int DEEP_SAMPLE_COST = 2;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(3, 3, 3, 13, 10, 13),
            Block.box(0, 9, 1, 16, 14, 15),
            Block.box(5, 14, 5, 11, 16, 11));

    public RealmServicePlinthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.WAYFARER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return ItemInteractionResult.CONSUME;
        }

        Mode mode = modeForDimension(serverLevel);
        if (mode == null || state.getValue(MODE) != mode) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_service_plinth.wrong_realm"), true);
            return ItemInteractionResult.CONSUME;
        }

        boolean handled = switch (mode) {
            case WAYFARER -> useWayfarerService(serverLevel, pos, serverPlayer, stack);
            case CONFLUENCE -> useConfluenceService(serverLevel, pos, serverPlayer, stack);
            case INTERLAYER -> useInterlayerService(serverLevel, pos, serverPlayer, stack, hand);
        };
        if (!handled) {
            showHint(serverPlayer, mode);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        Mode current = modeForDimension(level);
        if (current == null || state.getValue(MODE) != current) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_service_plinth.wrong_realm"), true);
            return InteractionResult.CONSUME;
        }

        RealmFacilityCircuitManager.Result result = RealmFacilityCircuitManager.tryTune(
                serverLevel, pos, serverPlayer, current);
        if (result == RealmFacilityCircuitManager.Result.NOT_LINKED) {
            showHint(serverPlayer, current);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean useWayfarerService(ServerLevel level, BlockPos pos,
                                               ServerPlayer player, ItemStack stack) {
        if (stack.is(ModItems.RECORD_TRACING_PAPER.get())
                && stack.getCount() >= TRACING_PAPER_COST) {
            craft(level, pos, player, stack, TRACING_PAPER_COST, ModItems.CLUE_MAP.get());
            return true;
        }
        if (stack.is(Items.BREAD) && stack.getCount() >= BREAD_COST) {
            craft(level, pos, player, stack, BREAD_COST, ModItems.FAR_BELL_TRAIL_LOAF.get());
            return true;
        }
        return false;
    }

    private static boolean useConfluenceService(ServerLevel level, BlockPos pos,
                                                ServerPlayer player, ItemStack stack) {
        if (stack.is(Items.BONE_MEAL) && stack.getCount() >= BONE_MEAL_COST) {
            Item[] cuttings = {
                    ModItems.ECHO_FLOWER.get(), ModItems.DREAM_FLOWER.get(),
                    ModItems.STAR_MIST_BLOOM.get(), ModItems.CRYSTAL_BELLFLOWER.get()
            };
            craft(level, pos, player, stack, BONE_MEAL_COST,
                    cuttings[player.getRandom().nextInt(cuttings.length)]);
            return true;
        }
        if (stack.is(ModItems.ECHO_SHARD.get())) {
            int current = ArtifactManager.getEnergy(player);
            int maximum = ArtifactManager.getMaxEnergy(player);
            if (current >= maximum) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm_service_plinth.energy_full"), true);
                return true;
            }
            consume(player, stack, 1);
            int restored = Math.min(ENERGY_REFILL, maximum - current);
            ArtifactManager.refillEnergy(player, ENERGY_REFILL);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_service_plinth.energy", restored), true);
            playSuccess(level, pos, Mode.CONFLUENCE);
            return true;
        }
        return false;
    }

    private static boolean useInterlayerService(ServerLevel level, BlockPos pos,
                                                ServerPlayer player, ItemStack stack,
                                                InteractionHand hand) {
        Item food = deepFoodFor(stack);
        if (food != null && stack.getCount() >= DEEP_SAMPLE_COST) {
            Item container = containerFor(food);
            if (container != null && !player.getAbilities().instabuild) {
                ItemStack containerStack = findInventoryItem(player, container);
                if (containerStack.isEmpty()) {
                    return false;
                }
                containerStack.shrink(1);
            }
            craft(level, pos, player, stack, DEEP_SAMPLE_COST, food);
            return true;
        }
        if (!stack.is(ModItems.RESONANCE_ALLOY_INGOT.get())) {
            return false;
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(otherHand);
        if (!target.isDamageableItem() || !target.isDamaged()) {
            return false;
        }
        consume(player, stack, 1);
        target.setDamageValue(0);
        playSuccess(level, pos, Mode.INTERLAYER);
        return true;
    }

    private static Item deepFoodFor(ItemStack stack) {
        if (stack.is(ModItems.FOLDED_SPOREBLOOM.get())) {
            return ModItems.FOLDED_SPORE_CRISP.get();
        }
        if (stack.is(ModItems.GRAVITY_BELL.get())) {
            return ModItems.GRAVITY_BELL_JELLY.get();
        }
        if (stack.is(ModItems.HUSH_THORN.get())) {
            return ModItems.HUSH_THORN_CANDY.get();
        }
        if (stack.is(ModItems.WEIGHTLESS_BLOOM.get())) {
            return ModItems.WEIGHTLESS_BLOOM_TEA.get();
        }
        if (stack.is(ModItems.VOIDCAP.get())) {
            return ModItems.VOIDCAP_STEW.get();
        }
        return null;
    }

    private static Item containerFor(Item food) {
        if (food == ModItems.WEIGHTLESS_BLOOM_TEA.get()) {
            return Items.GLASS_BOTTLE;
        }
        if (food == ModItems.VOIDCAP_STEW.get()) {
            return Items.BOWL;
        }
        return null;
    }

    private static ItemStack findInventoryItem(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void craft(ServerLevel level, BlockPos pos, ServerPlayer player,
                              ItemStack input, int cost, Item outputItem) {
        consume(player, input, cost);
        ItemStack output = new ItemStack(outputItem);
        if (!player.addItem(output)) {
            player.drop(output, false);
        }
        playSuccess(level, pos, modeForDimension(level));
    }

    private static void consume(ServerPlayer player, ItemStack stack, int count) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(count);
        }
    }

    private static void showHint(Player player, Mode mode) {
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.realm_service_plinth.hint."
                        + mode.getSerializedName()), true);
    }

    /** 维度 → 服务档位。映射表归 RealmHabitatChimeBlock 持有,这里只做类型转换。 */
    private static Mode modeForDimension(Level level) {
        RealmHabitatChimeBlock.Mode shared = RealmHabitatChimeBlock.modeForDimension(level);
        if (shared == null) {
            return null;
        }
        return switch (shared) {
            case WAYFARER -> Mode.WAYFARER;
            case CONFLUENCE -> Mode.CONFLUENCE;
            case INTERLAYER -> Mode.INTERLAYER;
        };
    }

    private static void playSuccess(ServerLevel level, BlockPos pos, Mode mode) {
        if (mode == null) {
            return;
        }
        level.sendParticles(particleFor(mode),
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                32, 0.45D, 0.35D, 0.45D, 0.035D);
        level.playSound(null, pos, switch (mode) {
            case WAYFARER -> SoundEvents.BOOK_PAGE_TURN;
            case CONFLUENCE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case INTERLAYER -> SoundEvents.ANVIL_USE;
        }, SoundSource.BLOCKS, 0.8F, switch (mode) {
            case WAYFARER -> 1.2F;
            case CONFLUENCE -> 1.5F;
            case INTERLAYER -> 0.8F;
        });
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        Mode mode = state.getValue(MODE);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.18D + random.nextDouble() * 0.3D;
        level.addParticle(particleFor(mode),
                pos.getX() + 0.5D + Math.cos(angle) * radius,
                pos.getY() + 1.0D + random.nextDouble() * 0.5D,
                pos.getZ() + 0.5D + Math.sin(angle) * radius,
                0.0D, 0.012D, 0.0D);
    }

    private static ParticleOptions particleFor(Mode mode) {
        return switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.HAPPY_VILLAGER;
            case INTERLAYER -> ParticleTypes.REVERSE_PORTAL;
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
