package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EchoRealmCoreItem extends Item implements ItemActiveSkill {
    public static final int ENERGY_RESTORE = 60;
    public static final int COOLDOWN_TICKS = 800;
    public static final float OVERFLOW_HEAL = 8.0F;
    public static final int OVERFLOW_ABSORPTION_TICKS = 200;
    private static final int OVERFLOW_ABSORPTION_AMPLIFIER = 1;
    private static final float OVERFLOW_ABSORPTION_HEALTH = 8.0F;

    public EchoRealmCoreItem(Properties properties) {
        super(properties);
    }

    /** 由「物品主动技能」键触发；右键不释放。 */
    @Override
    public boolean activateItemSkill(ServerLevel serverLevel, ServerPlayer serverPlayer,
                                     ItemStack stack, InteractionHand hand) {
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return false;
        }

        ArtifactManager.getEnergy(serverPlayer);
        int maxEnergyTwentieths = ArtifactManager.getMaxEnergy(serverPlayer) * 20;
        if (ArtifactManager.getData(serverPlayer).getEnergyTwentieths() >= maxEnergyTwentieths) {
            if (!canBenefitFromOverflow(serverPlayer)) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.echo_realm_core.full"), true);
                return false;
            }
            applyOverflow(serverPlayer);
        } else {
            ArtifactManager.refillEnergy(serverPlayer, ENERGY_RESTORE);
        }

        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                24, 0.45D, 0.7D, 0.45D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                serverPlayer.getX(), serverPlayer.getY() + 0.8D, serverPlayer.getZ(),
                36, 0.55D, 0.55D, 0.55D, 0.12D);
        serverLevel.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9F, 1.15F);
        serverPlayer.swing(hand, true);
        return true;
    }

    private static boolean canBenefitFromOverflow(ServerPlayer player) {
        return player.getHealth() < player.getMaxHealth()
                || FoodEffectSupport.needsAbsorptionRefresh(player,
                OVERFLOW_ABSORPTION_TICKS, OVERFLOW_ABSORPTION_AMPLIFIER,
                OVERFLOW_ABSORPTION_HEALTH);
    }

    private static void applyOverflow(ServerPlayer player) {
        player.heal(OVERFLOW_HEAL);
        FoodEffectSupport.applyAbsorption(player,
                OVERFLOW_ABSORPTION_TICKS, OVERFLOW_ABSORPTION_AMPLIFIER,
                OVERFLOW_ABSORPTION_HEALTH);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.echo_realm_core.skill",
                        Component.keybind("key.unknown_echoes.item_active_skill"), ENERGY_RESTORE)
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
