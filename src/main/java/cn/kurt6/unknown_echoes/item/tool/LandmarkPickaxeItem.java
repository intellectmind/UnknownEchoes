package cn.kurt6.unknown_echoes.item.tool;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.item.FoodEffectSupport;
import cn.kurt6.unknown_echoes.item.ItemActiveSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 深层大型地标专用采集工具；主动增益不授予结构或主线权限。 */
public final class LandmarkPickaxeItem extends PickaxeItem implements ItemActiveSkill {
    private static final int ENERGY_COST = 6;
    private static final int COOLDOWN_TICKS = 360;

    private final Kind kind;
    private final String tooltipKey;

    public LandmarkPickaxeItem(Kind kind, String tooltipKey, Tier tier, Properties properties) {
        super(tier, properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
    }

    /** 主动增益由「物品主动技能」键触发；右键不释放技能。 */
    @Override
    public boolean activateItemSkill(ServerLevel level, ServerPlayer player,
                                     ItemStack stack, InteractionHand hand) {
        return activate(level, player);
    }

    private boolean activate(ServerLevel level, ServerPlayer player) {
        String cooldownKey = "ue_" + tooltipKey + "_active_until";
        long now = player.server.overworld().getGameTime();
        long readyAt = player.getPersistentData().getLong(cooldownKey);
        long maximumReadyAt = safeDeadline(now, COOLDOWN_TICKS);
        if (readyAt > maximumReadyAt) {
            readyAt = maximumReadyAt;
            player.getPersistentData().putLong(cooldownKey, readyAt);
        }
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.landmark_gear.cooldown", seconds), true);
            return false;
        }
        if (!canBenefit(player)) {
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, ENERGY_COST)) {
            return false;
        }

        player.getPersistentData().putLong(cooldownKey, safeDeadline(now, COOLDOWN_TICKS));
        if (kind == Kind.ARCHIVE_CHISEL) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,
                    500, 1, true, false, true));
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    54, 0.6D, 0.8D, 0.6D, 0.12D);
            level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE,
                    SoundSource.PLAYERS, 0.9F, 1.25F);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                    500, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    320, 1, true, false, true));
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.8D, player.getZ(),
                    64, 0.8D, 1.0D, 0.8D, 0.1D);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.9F, 1.35F);
        }
        return true;
    }

    private boolean canBenefit(Player player) {
        return switch (kind) {
            case ARCHIVE_CHISEL -> needsEffectRefresh(
                    player, MobEffects.DIG_SPEED, 500, 1);
            case ORRERY_CALIBRATOR -> needsEffectRefresh(
                    player, MobEffects.SLOW_FALLING, 500, 0)
                    || needsEffectRefresh(player, MobEffects.MOVEMENT_SPEED, 320, 1);
        };
    }

    private static boolean needsEffectRefresh(Player player, Holder<MobEffect> effect,
                                              int durationTicks, int amplifier) {
        return FoodEffectSupport.needsEffectRefresh(player, effect, durationTicks, amplifier);
    }

    private static long safeDeadline(long now, long duration) {
        long boundedDuration = Math.max(0L, duration);
        return now > Long.MAX_VALUE - boundedDuration
                ? Long.MAX_VALUE : now + boundedDuration;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + tooltipKey + ".active",
                        Component.keybind("key.unknown_echoes.item_active_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.tool." + tooltipKey + ".detail",
                        Component.keybind("key.unknown_echoes.echo_tool_skill"))
                .withStyle(ChatFormatting.GRAY));
    }

    public enum Kind {
        ARCHIVE_CHISEL,
        ORRERY_CALIBRATOR
    }
}
