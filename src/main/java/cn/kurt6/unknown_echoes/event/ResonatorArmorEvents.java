package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 共鸣者套装终局战斗与回访效果。所有结算由服务端事件和瞬态属性完成。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class ResonatorArmorEvents {
    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 40;
    private static final int FOOD_RESTORE_INTERVAL_TICKS = 200;
    private static final int NIGHT_VISION_DURATION_TICKS = 600;
    private static final int PASSIVE_EFFECT_DURATION_TICKS = 100;
    private static final int PASSIVE_EFFECT_REFRESH_THRESHOLD_TICKS = 60;
    private static final double RESONATOR_TWO_PIECE_HEALTH = 8.0D;
    private static final double RESONATOR_FOUR_PIECE_HEALTH = 8.0D;
    private static final double RESONATOR_MOVEMENT_SPEED = 0.10D;
    private static final double RESONATOR_ATTACK_SPEED = 0.15D;
    private static final ResourceLocation TWO_PIECE_HEALTH_ID =
            UnknownEchoes.id("resonator_two_piece_health");
    private static final ResourceLocation FOUR_PIECE_HEALTH_ID =
            UnknownEchoes.id("resonator_four_piece_health");
    private static final ResourceLocation MOVEMENT_SPEED_ID =
            UnknownEchoes.id("resonator_movement_speed");
    private static final ResourceLocation ATTACK_SPEED_ID =
            UnknownEchoes.id("resonator_attack_speed");
    private static final ResourceLocation ARMOR_ID = UnknownEchoes.id("resonator_armor");
    private static final ResourceLocation TOUGHNESS_ID = UnknownEchoes.id("resonator_toughness");
    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID =
            UnknownEchoes.id("resonator_knockback_resistance");

    private ResonatorArmorEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % EFFECT_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        int pieces = player.isSpectator() ? 0 : EchoArmorSets.resonatorPieces(player);
        applySetAttributes(player, pieces);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (pieces < 4) {

            NightVisionSupport.release(player, NightVisionSupport.Source.RESONATOR);
            return;
        }

        refreshNightVision(player);
        refreshPassiveEffect(player, MobEffects.FIRE_RESISTANCE);
        refreshPassiveEffect(player, MobEffects.REGENERATION);
        removeMinorEffect(player, MobEffects.DARKNESS);
        removeMinorEffect(player, MobEffects.WEAKNESS);
        removeMinorEffect(player, MobEffects.MOVEMENT_SLOWDOWN);
        removeMinorEffect(player, MobEffects.DIG_SLOWDOWN);
        if (player.tickCount % FOOD_RESTORE_INTERVAL_TICKS == 0) {
            FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() < 20) {
                foodData.eat(1, 0.10F);
            }
        }
    }

    private static void applySetAttributes(ServerPlayer player, int pieces) {
        boolean twoPieces = pieces >= 2;
        boolean fourPieces = pieces >= 4;
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), TWO_PIECE_HEALTH_ID,
                RESONATOR_TWO_PIECE_HEALTH, AttributeModifier.Operation.ADD_VALUE, twoPieces);
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), FOUR_PIECE_HEALTH_ID,
                RESONATOR_FOUR_PIECE_HEALTH, AttributeModifier.Operation.ADD_VALUE, fourPieces);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_ID,
                RESONATOR_MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, twoPieces);
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID,
                RESONATOR_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, twoPieces);
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID,
                4.0D, AttributeModifier.Operation.ADD_VALUE, fourPieces);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID,
                4.0D, AttributeModifier.Operation.ADD_VALUE, fourPieces);
        applyModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_RESISTANCE_ID,
                0.20D, AttributeModifier.Operation.ADD_VALUE, fourPieces);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int pieces = EchoArmorSets.resonatorPieces(player);
        if (pieces >= 2) {
            event.setDamageMultiplier(event.getDamageMultiplier() * (pieces >= 4 ? 0.20F : 0.60F));
        }
    }

    /** 在目标自己的 hurt 阶段之后只强化普通目标，避免越过 Boss 的阶段钳制。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOutgoingDamage(LivingIncomingDamageEvent event) {
        ServerPlayer attacker = responsiblePlayer(event.getSource());
        if (attacker == null || event.getEntity() instanceof Player
                || EchoArmorSets.resonatorPieces(attacker) < 4 || isBoss(event.getEntity())) {
            return;
        }
        event.setAmount(event.getAmount() * 1.25F);
    }

    /** 在护甲、药水和大多数 Mod 修正之后乘算，标准 hurt 管线内的伤害类型无法绕过。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || EchoArmorSets.resonatorPieces(player) < 2
                || isPvpDamage(event.getSource())) {
            return;
        }
        int pieces = EchoArmorSets.resonatorPieces(player);
        boolean inDeepEchoes = isInDeepEchoes(player);
        Entity attacker = responsibleEntity(event.getSource());
        float multiplier = 1.0F;
        if (attacker == null
                && !event.getSource().is(DamageTypeTags.IS_FALL)) {
            multiplier = inDeepEchoes ? 0.45F : 0.65F;
        } else if (pieces >= 4 && attacker != null) {
            boolean deepHostile = inDeepEchoes
                    && attacker.getType().is(ModEntityTags.DEEP_ECHOES_HOSTILES);
            multiplier = deepHostile ? 0.55F : 0.70F;
        }
        if (multiplier < 1.0F) {
            event.setNewDamage(event.getNewDamage() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer attacker = responsiblePlayer(event.getSource());
        if (attacker == null || event.getEntity() instanceof Player
                || EchoArmorSets.resonatorPieces(attacker) < 4) {
            return;
        }
        attacker.heal(4.0F);
        if (attacker.getAbsorptionAmount() < 8.0F) {
            attacker.setAbsorptionAmount(Math.min(8.0F,
                    attacker.getAbsorptionAmount() + 4.0F));
        }
    }

    private static boolean isInDeepEchoes(ServerPlayer player) {
        return player.level().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID);
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(ModEntityTags.ECHO_BOSSES)
                || entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean isPvpDamage(DamageSource source) {
        return isPlayerControlled(source.getEntity()) || isPlayerControlled(source.getDirectEntity());
    }

    private static boolean isPlayerControlled(Entity entity) {
        return entity instanceof Player
                || entity instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player;
    }

    private static ServerPlayer responsiblePlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static Entity responsibleEntity(DamageSource source) {
        return source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
    }

    private static void refreshNightVision(ServerPlayer player) {
        NightVisionSupport.sustain(player, NightVisionSupport.Source.RESONATOR,
                NIGHT_VISION_DURATION_TICKS, EFFECT_REFRESH_INTERVAL_TICKS, true,
                player.server.overworld().getGameTime());
    }

    private static void refreshPassiveEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || (instance.getAmplifier() == 0
                && instance.getDuration() > 0
                && instance.getDuration() <= PASSIVE_EFFECT_REFRESH_THRESHOLD_TICKS)) {
            player.addEffect(new MobEffectInstance(effect,
                    PASSIVE_EFFECT_DURATION_TICKS, 0, true, false, true));
        }
    }

    private static void removeMinorEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() <= 1) {
            player.removeEffect(effect);
        }
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active) {
            if (!attribute.hasModifier(id)) {
                attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else {
            attribute.removeModifier(id);
        }
    }
}
