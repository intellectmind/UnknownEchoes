package cn.kurt6.unknown_echoes.block.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class DeepBiomePlantBlock extends FlowerBlock {
    private final Supplier<? extends Block> nativeGround;

    public DeepBiomePlantBlock(Holder<MobEffect> effect, float effectSeconds, Properties properties,
            Supplier<? extends Block> nativeGround) {
        super(effect, effectSeconds, properties);
        this.nativeGround = nativeGround;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(nativeGround.get()) || super.mayPlaceOn(state, level, pos);
    }

    /** 空手采下整株，作为不触发普通破坏流程的便捷取回方式。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level,
                                               BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.CONSUME;
        }
        Item harvestItem = asItem();
        if (harvestItem == Items.AIR || !serverLevel.removeBlock(pos, false)) {
            return InteractionResult.CONSUME;
        }
        giveHarvest(serverPlayer, harvestItem);
        presentHarvest(serverLevel, pos);
        return InteractionResult.CONSUME;
    }

    /** 找到原生植株后可用骨粉培育一份新株：保留可再生性，但每份都要付出骨粉成本。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                                              net.minecraft.world.level.Level level, BlockPos pos,
                                              Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (!stack.is(Items.BONE_MEAL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return ItemInteractionResult.CONSUME;
        }
        Item harvestItem = asItem();
        if (harvestItem == Items.AIR) {
            return ItemInteractionResult.CONSUME;
        }
        stack.consume(1, player);
        giveHarvest(serverPlayer, harvestItem);
        presentHarvest(serverLevel, pos);
        return ItemInteractionResult.CONSUME;
    }

    private static void giveHarvest(ServerPlayer player, Item harvestItem) {
        ItemStack harvest = new ItemStack(harvestItem);
        if (!player.getInventory().add(harvest)) {
            player.drop(harvest, false);
        }
    }

    private static void presentHarvest(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                6, 0.18D, 0.25D, 0.18D, 0.02D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.45F, 1.35F);
    }
}
