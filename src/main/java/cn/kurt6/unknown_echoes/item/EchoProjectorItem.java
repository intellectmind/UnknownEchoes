package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 回响投影器:对地面使用,召唤一个回响投影(可踩压力板等机关,30 秒消散)。
 * 需要玩家已聆听过任意回响能力;每名玩家同时只存在一个投影。
 */
public class EchoProjectorItem extends Item {
    private static final int COOLDOWN_TICKS = 60;

    public EchoProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.CONSUME;
        }

        if (EchoAbilityManager.getData(player).getUnlockedAbilities().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.projector.no_ability"), true);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.5F);
            return InteractionResult.CONSUME;
        }

        // 同一玩家旧投影先消散
        for (EchoProjectionEntity old : level.getEntitiesOfClass(EchoProjectionEntity.class,
                new AABB(player.blockPosition()).inflate(128.0D),
                p -> p.getOwner().map(player.getUUID()::equals).orElse(false))) {
            old.dissipate();
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        EchoProjectionEntity projection = ModEntities.ECHO_PROJECTION.get().create(level);
        if (projection == null) {
            return InteractionResult.CONSUME;
        }
        projection.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                player.getYRot(), 0.0F);
        projection.setOwner(player.getUUID());
        level.addFreshEntity(projection);
        level.playSound(null, spawnPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_projector")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
