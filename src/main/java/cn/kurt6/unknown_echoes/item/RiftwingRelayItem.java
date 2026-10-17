package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RiftwingRelayItem extends Item implements ItemActiveSkill {
    public static final int ENERGY_COST = 6;
    public static final int COOLDOWN_TICKS = 30;
    public static final int WING_RESONANCE_COOLDOWN_TICKS = 20;

    public RiftwingRelayItem(Properties properties) {
        super(properties);
    }

    /** 由「物品主动技能」键触发；右键不释放。 */
    @Override
    public boolean activateItemSkill(ServerLevel serverLevel, ServerPlayer serverPlayer,
                                     ItemStack stack, InteractionHand hand) {
        if (serverPlayer.isPassenger()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.riftwing_relay.passenger"), true);
            return false;
        }
        if (serverPlayer.level() != serverLevel || !serverPlayer.isAlive()
                || serverPlayer.isSpectator() || !stack.is(this)
                || serverPlayer.getItemInHand(hand) != stack
                || serverPlayer.getCooldowns().isOnCooldown(this)) {
            return false;
        }

        boolean wingResonance = serverPlayer.getItemBySlot(EquipmentSlot.CHEST)
                .is(ModItems.WORLD_ECHO_WINGS.get());
        // 支付及缓降效果之前预留冷却，只有明确支付失败才允许立即重试。
        serverPlayer.getCooldowns().addCooldown(this,
                wingResonance ? WING_RESONANCE_COOLDOWN_TICKS : COOLDOWN_TICKS);
        if (!ArtifactManager.spendEnergy(serverPlayer, ENERGY_COST)) {
            serverPlayer.getCooldowns().removeCooldown(this);
            return false;
        }
        if (!canContinue(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }
        Vec3 origin = serverPlayer.position();
        Vec3 look = serverPlayer.getLookAngle().normalize();
        double horizontalBoost = wingResonance ? 2.05D : 1.65D;
        double verticalBoost = wingResonance ? 1.45D : 1.25D;
        double minimumLift = wingResonance ? 0.48D : 0.35D;
        serverPlayer.setDeltaMovement(momentumSafeBoost(
                serverPlayer.getDeltaMovement(), look,
                horizontalBoost, verticalBoost, minimumLift));
        serverPlayer.hurtMarked = true;
        serverPlayer.fallDistance = 0.0F;
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                wingResonance ? 180 : 120, 0, true, false, true));

        if (!canContinue(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + 0.8D, origin.z,
                42, 0.35D, 0.45D, 0.35D, 0.16D);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                origin.x, origin.y + 0.8D, origin.z,
                18, 0.22D, 0.28D, 0.22D, 0.08D);
        if (wingResonance) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    origin.x, origin.y + 0.8D, origin.z,
                    28, 0.42D, 0.5D, 0.42D, 0.2D);
        }
        serverLevel.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.85F, 1.25F);
        if (canContinue(serverLevel, serverPlayer, stack, hand)) {
            serverPlayer.swing(hand, true);
        }
        return true;
    }

    private boolean canContinue(ServerLevel level, ServerPlayer player,
                                ItemStack stack, InteractionHand hand) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && stack.is(this) && player.getItemInHand(hand) == stack;
    }

    private static Vec3 momentumSafeBoost(Vec3 current, Vec3 look,
                                          double horizontalBoost, double verticalBoost,
                                          double minimumLift) {
        double lookHorizontalLength = Math.hypot(look.x, look.z);
        double currentHorizontalSpeed = Math.hypot(current.x, current.z);
        double desiredHorizontalSpeed = horizontalBoost * lookHorizontalLength;
        double nextX = current.x;
        double nextZ = current.z;
        if (lookHorizontalLength > 1.0E-6D) {
            double nextHorizontalSpeed = Math.max(currentHorizontalSpeed, desiredHorizontalSpeed);
            nextX = look.x / lookHorizontalLength * nextHorizontalSpeed;
            nextZ = look.z / lookHorizontalLength * nextHorizontalSpeed;
        }
        double desiredVerticalSpeed = Math.max(
                minimumLift, look.y * verticalBoost + 0.25D);
        return new Vec3(nextX, Math.max(current.y, desiredVerticalSpeed), nextZ);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.riftwing_relay.skill",
                        Component.keybind("key.unknown_echoes.item_active_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.riftwing_relay.synergy")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
