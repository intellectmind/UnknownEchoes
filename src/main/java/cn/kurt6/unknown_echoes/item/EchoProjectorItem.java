package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.entity.projection.EchoProjectionEntity;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.world.EntitySpawnSafety;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.UUID;

/** 回响投影器:对地面使用,召唤一个回响投影(可踩压力板等机关,30 秒消散)。 */
public class EchoProjectorItem extends Item {
    private static final int COOLDOWN_TICKS = 60;
    private static final String PROJECTION_ID_KEY = "ue_projection_id";

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
        if (player.getCooldowns().isOnCooldown(this) || !canProject(context, player, level)) {
            return InteractionResult.CONSUME;
        }
        if (EchoAbilityManager.getData(player).getUnlockedAbilities().isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.projector.no_ability"), true);
            PlayerSoundFeedback.deny(player);
            return InteractionResult.CONSUME;
        }

        EchoProjectionEntity projection = null;
        boolean committed = false;
        try {
            // 构造、入世和声音都可能同步回调，先预留本次冷却。
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            UUID previousId = player.getPersistentData().hasUUID(PROJECTION_ID_KEY)
                    ? player.getPersistentData().getUUID(PROJECTION_ID_KEY) : null;
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            projection = ModEntities.ECHO_PROJECTION.get().create(level);
            if (projection == null) {
                return InteractionResult.CONSUME;
            }
            projection.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    player.getYRot(), 0.0F);
            projection.setOwner(player.getUUID());
            if (!EntitySpawnSafety.isLoadedAndCollisionFree(level, projection)
                    || !canProject(context, player, level)) {
                return InteractionResult.CONSUME;
            }
            if (!level.addFreshEntity(projection)
                    || !projection.isAlive() || !canProject(context, player, level)) {
                return InteractionResult.CONSUME;
            }

            player.getPersistentData().putUUID(PROJECTION_ID_KEY, projection.getUUID());
            committed = true;
            if (previousId != null) {
                for (ServerLevel anyLevel : level.getServer().getAllLevels()) {
                    if (anyLevel.getEntity(previousId) instanceof EchoProjectionEntity old
                            && old.getOwner().map(player.getUUID()::equals).orElse(false)) {
                        old.dissipate();
                        break;
                    }
                }
            }
            level.playSound(null, spawnPos, ModSounds.ITEM_ECHO_PROJECTOR_ACTIVATE.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        } finally {
            if (!committed) {
                try {
                    if (projection != null) {
                        projection.discard();
                    }
                } finally {
                    player.getCooldowns().removeCooldown(this);
                }
            }
        }
    }

    private boolean canProject(UseOnContext context, ServerPlayer player, ServerLevel level) {
        return player.level() == level && !player.isSpectator()
                && player.getItemInHand(context.getHand()) == context.getItemInHand()
                && context.getItemInHand().is(this);
    }

    public static boolean isCurrentProjection(ServerPlayer player, UUID projectionId) {
        return player.getPersistentData().hasUUID(PROJECTION_ID_KEY)
                && projectionId.equals(player.getPersistentData().getUUID(PROJECTION_ID_KEY));
    }

    public static void clearCurrentProjection(ServerPlayer player, UUID projectionId) {
        if (isCurrentProjection(player, projectionId)) {
            player.getPersistentData().remove(PROJECTION_ID_KEY);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_projector",
                        Component.keybind("key.use"))
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
