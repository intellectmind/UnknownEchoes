package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.world.structure.LoadedStructureLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 套装件数效果(服务端判定):装备只增强既有能力/场景体验,不授予权限。 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class EchoArmorSetEvents {

    private static final int SET_EFFECT_REFRESH_INTERVAL_TICKS = 40;
    /** 夜视时长统一由 NightVisionSupport 按轮询周期换算，保证剩余时长不进入原版闪烁区。 */
    private static final int SUSTAINED_NIGHT_VISION_DURATION_TICKS =
            NightVisionSupport.sustainedDuration(SET_EFFECT_REFRESH_INTERVAL_TICKS);
    private static final float TRUE_SIGHT_ILLUSION_DAMAGE_MULTIPLIER = 1.15F;
    private static final float TRUE_SIGHT_ILLUSION_RESISTANCE_MULTIPLIER = 0.70F;

    /** 探索者 2 件:判定"遗迹内"的结构集合(数据包可调)。 */
    private static final TagKey<Structure> ECHO_RUINS = TagKey.create(Registries.STRUCTURE,
            UnknownEchoes.id("echo_ruins"));
    private static final TagKey<Block> RESOURCE_BLOCKS = TagKey.create(Registries.BLOCK,
            UnknownEchoes.id("resource_blocks"));

    /** 探索者 4 件:回响生物对穿戴者的索敌距离上限(格),超过即放弃仇恨。 */
    private static final double EXPLORER_AGGRO_RANGE = 8.0D;
    private static final ResourceLocation SILENT_SWAMP_ID = UnknownEchoes.id("silent_swamp");

    private static final ResourceLocation SPEED_ID = UnknownEchoes.id("explorer_set_speed");
    private static final ResourceLocation TOUGHNESS_ID = UnknownEchoes.id("explorer_set_toughness");
    private static final ResourceLocation ARMOR_ID = UnknownEchoes.id("explorer_set_armor");
    private static final ResourceLocation WIND_SPEED_ID = UnknownEchoes.id("wind_walker_set_speed");
    private static final ResourceLocation TIDE_SWIM_SPEED_ID = UnknownEchoes.id("tide_stalker_set_speed");
    private static final ResourceLocation OATH_SPEED_ID = UnknownEchoes.id("echo_oath_set_speed");
    private static final ResourceLocation OATH_TOUGHNESS_ID = UnknownEchoes.id("echo_oath_set_toughness");
    private static final ResourceLocation OATH_ARMOR_ID = UnknownEchoes.id("echo_oath_set_armor");
    private static final ResourceLocation OATH_REALM_TOUGHNESS_ID = UnknownEchoes.id("echo_oath_set_realm_toughness");
    private static final ResourceLocation OATH_REALM_ARMOR_ID = UnknownEchoes.id("echo_oath_set_realm_armor");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % SET_EFFECT_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        int pieces = EchoArmorSets.explorerPieces(player);
        if (player.isSpectator()) {
            NightVisionSupport.release(player, NightVisionSupport.Source.ARMOR_SET);
            clearSetModifiers(player);
            return;
        }

        boolean nightVisionSupplied = hasExplorerNightVision(player, pieces);

        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, 0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, pieces >= 4);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, pieces >= 4);
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, pieces >= 4);

        boolean inEchoRealm = player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID);
        nightVisionSupplied |= applyEchoRealmSetEffects(player, inEchoRealm);
        boolean inDeepEchoes = player.level().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID);
        nightVisionSupplied |= applyDeepSetEffects(player, inDeepEchoes);

        if (nightVisionSupplied) {
            refreshNightVision(player, SUSTAINED_NIGHT_VISION_DURATION_TICKS, true);
        } else {

            NightVisionSupport.release(player, NightVisionSupport.Source.ARMOR_SET);
        }
    }

    private static boolean hasExplorerNightVision(ServerPlayer player, int explorerPieces) {
        return explorerPieces >= 2
                && player.level() instanceof ServerLevel serverLevel
                && LoadedStructureLookup.hasStructurePieceAt(
                        serverLevel, player.blockPosition(), ECHO_RUINS);
    }

    private static boolean applyEchoRealmSetEffects(ServerPlayer player, boolean inEchoRealm) {
        int traveler = EchoArmorSets.echoTravelerPieces(player);
        int wind = EchoArmorSets.windWalkerPieces(player);
        int tide = EchoArmorSets.tideStalkerPieces(player);
        int trueSight = EchoArmorSets.trueSightShadowPieces(player);
        int silent = EchoArmorSets.silentWatchPieces(player);
        int oath = EchoArmorSets.echoOathPieces(player);

        if (traveler >= 4 && inEchoRealm) {
            sendNearbyResourceHints(player);
        }

        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), WIND_SPEED_ID, 0.03D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, wind >= 2 && inEchoRealm);
        if (wind >= 2 && inEchoRealm && !player.onGround() && !player.isInWater()) {
            addEffectIfMissing(player, MobEffects.SLOW_FALLING, 60, true);
        }

        boolean nightVisionSupplied = tide >= 2 && player.isEyeInFluid(FluidTags.WATER);
        if (tide >= 4 && player.isEyeInFluid(FluidTags.WATER)) {
            addEffectIfMissing(player, MobEffects.DOLPHINS_GRACE, 80, true);
        }
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TIDE_SWIM_SPEED_ID, 0.08D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, tide >= 4 && player.isEyeInFluid(FluidTags.WATER));

        nightVisionSupplied |= trueSight >= 2 && inEchoRealm;

        boolean inSilentSwamp = isInBiome(player, SILENT_SWAMP_ID);
        if (silent >= 2) {
            protectFromSilentControl(player, MobEffects.BLINDNESS, inSilentSwamp);
            protectFromSilentControl(player, MobEffects.DARKNESS, inSilentSwamp);
        }
        if (silent >= 4) {
            protectFromSilentControl(player, MobEffects.MOVEMENT_SLOWDOWN, inSilentSwamp);
            protectFromSilentControl(player, MobEffects.WEAKNESS, inSilentSwamp);
        }

        boolean oathActive = oath >= 4;
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), OATH_SPEED_ID, 0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, oath >= 2);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), OATH_TOUGHNESS_ID, 2.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive);
        applyModifier(player.getAttribute(Attributes.ARMOR), OATH_ARMOR_ID, 2.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), OATH_REALM_TOUGHNESS_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive && inEchoRealm);
        applyModifier(player.getAttribute(Attributes.ARMOR), OATH_REALM_ARMOR_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive && inEchoRealm);
        if (oath >= 2) {
            reduceMinorControl(player);
        }
        return nightVisionSupplied;
    }

    private static boolean applyDeepSetEffects(ServerPlayer player, boolean inDeepEchoes) {
        if (EchoArmorSets.abyssWalkerPieces(player) < 4) {
            return false;
        }
        int maxDuration = inDeepEchoes ? 40 : 80;
        shortenEffect(player, MobEffects.DARKNESS, maxDuration);
        shortenEffect(player, MobEffects.WEAKNESS, maxDuration);
        shortenEffect(player, MobEffects.MOVEMENT_SLOWDOWN, maxDuration);
        shortenEffect(player, MobEffects.DIG_SLOWDOWN, maxDuration);
        return inDeepEchoes;
    }

    /** 套装轮询仅在效果缺失或即将过期时补发；夜视走独立阈值。 */
    private static void addEffectIfMissing(ServerPlayer player, Holder<MobEffect> effect,
                                            int duration, boolean showIcon) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() > 0
                || current.getDuration() < 0
                || current.getDuration() > Math.max(1, duration / 2))) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, showIcon));
    }

    private static void refreshNightVision(ServerPlayer player, int duration, boolean showIcon) {
        NightVisionSupport.sustain(player, NightVisionSupport.Source.ARMOR_SET, duration,
                SET_EFFECT_REFRESH_INTERVAL_TICKS, showIcon,
                player.server.overworld().getGameTime());
    }

    private static void sendNearbyResourceHints(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int sent = 0;
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int xOffset = -6; xOffset <= 6; xOffset++) {
            for (int zOffset = -6; zOffset <= 6; zOffset++) {
                if (xOffset * xOffset + zOffset * zOffset > 6 * 6) {
                    continue;
                }
                cursor.set(center.getX() + xOffset, center.getY(), center.getZ() + zOffset);
                if (!level.hasChunkAt(cursor)) {
                    continue;
                }
                for (int yOffset = -3; yOffset <= 3; yOffset++) {
                    cursor.setY(center.getY() + yOffset);
                    if (!level.getBlockState(cursor).is(RESOURCE_BLOCKS)
                            || player.distanceToSqr(cursor.getX() + 0.5D,
                            cursor.getY() + 0.5D, cursor.getZ() + 0.5D) <= 4.0D) {
                        continue;
                    }
                    level.sendParticles(player, ParticleTypes.END_ROD, false,
                            cursor.getX() + 0.5D, cursor.getY() + 0.75D, cursor.getZ() + 0.5D,
                            2, 0.18D, 0.12D, 0.18D, 0.0D);
                    if (++sent >= 3) {
                        return;
                    }
                }
            }
        }
    }

    private static void reduceMinorControl(ServerPlayer player) {
        shortenEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 60);
        shortenEffect(player, MobEffects.WEAKNESS, 60);
        shortenEffect(player, MobEffects.DIG_SLOWDOWN, 60);
        shortenEffect(player, MobEffects.BLINDNESS, 60);
    }

    private static void protectFromSilentControl(ServerPlayer player, Holder<MobEffect> effect,
                                                   boolean inSilentSwamp) {
        if (inSilentSwamp) {
            player.removeEffect(effect);
        } else {
            shortenEffect(player, effect, 100);
        }
    }

    private static void shortenEffect(ServerPlayer player, Holder<MobEffect> effect) {
        shortenEffect(player, effect, 80);
    }

    private static void shortenEffect(ServerPlayer player, Holder<MobEffect> effect, int maxDuration) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.getDuration() <= maxDuration || instance.getAmplifier() > 1) {
            return;
        }
        player.removeEffect(effect);
        player.addEffect(new MobEffectInstance(effect, maxDuration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon()));
    }

    private static boolean isInBiome(ServerPlayer player, ResourceLocation biomeId) {
        return player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().equals(biomeId))
                .orElse(false);
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

    /**
     * 探索者 4 件:降低回响生物的仇恨范围,取消 8 格外的索敌,近身仍正常战斗。
     * 只影响本 Mod 的环境敌对生物(tag 可调)。
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getEntity().getType().is(ModTags.ECHO_REALM_HOSTILES)) {
            return;
        }
        if (event.getEntity().distanceTo(player) > EXPLORER_AGGRO_RANGE
                && EchoArmorSets.explorerPieces(player) >= 4) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if ((EchoArmorSets.windWalkerPieces(player) >= 2
                || EchoArmorSets.echoTravelerPieces(player) >= 2)
                && player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.85F);
        }
        if (EchoArmorSets.abyssWalkerPieces(player) >= 2) {
            boolean inDeepEchoes = player.level().dimension().location()
                    .equals(EchoPermission.DEEP_ECHOES_ID);
            event.setDamageMultiplier(event.getDamageMultiplier()
                    * (inDeepEchoes ? 0.65F : 0.80F));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.isEyeInFluid(FluidTags.WATER)
                && EchoArmorSets.tideStalkerPieces(player) >= 2) {
            event.setNewSpeed(event.getNewSpeed() * 1.15F);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() != attacker
                && !event.getEntity().isAlliedTo(attacker)
                && event.getEntity().getType().is(ModEntityTags.ILLUSION_MOBS)
                && EchoArmorSets.trueSightShadowPieces(attacker) >= 2) {
            event.setAmount(event.getAmount() * TRUE_SIGHT_ILLUSION_DAMAGE_MULTIPLIER);
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isPvpDamage(event.getSource())) {
            return;
        }
        boolean inEchoRealm = player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID);
        boolean inDeepEchoes = player.level().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID);
        if (inEchoRealm && EchoArmorSets.echoTravelerPieces(player) >= 2
                && isEnvironmentalDamage(event.getSource())) {
            event.setAmount(event.getAmount() * 0.85F);
        }
        if (event.getSource().getEntity() != null
                && event.getSource().getEntity().getType().is(ModEntityTags.ILLUSION_MOBS)
                && EchoArmorSets.trueSightShadowPieces(player) >= 4) {
            event.setAmount(event.getAmount() * TRUE_SIGHT_ILLUSION_RESISTANCE_MULTIPLIER);
        }

        int oath = EchoArmorSets.echoOathPieces(player);
        if (oath >= 2 && isEnvironmentalDamage(event.getSource())) {
            event.setAmount(event.getAmount() * 0.75F);
        }
        if (oath >= 4 && event.getSource().getEntity() != null
                && !(event.getSource().getEntity() instanceof Player)) {
            event.setAmount(event.getAmount() * (inEchoRealm ? 0.75F : 0.85F));
        }
        int abyssPieces = EchoArmorSets.abyssWalkerPieces(player);
        if (abyssPieces >= 2 && isEnvironmentalDamage(event.getSource())
                && !event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setAmount(event.getAmount() * (inDeepEchoes ? 0.70F : 0.85F));
        }
        if (abyssPieces >= 4 && event.getSource().getEntity() != null
                && !(event.getSource().getEntity() instanceof Player)) {
            boolean deepHostile = inDeepEchoes
                    && event.getSource().getEntity().getType()
                    .is(ModEntityTags.DEEP_ECHOES_HOSTILES);
            event.setAmount(event.getAmount() * (deepHostile ? 0.80F : 0.90F));
        }
    }

    private static boolean isPvpDamage(DamageSource source) {
        return isPlayerControlled(source.getEntity()) || isPlayerControlled(source.getDirectEntity());
    }

    private static boolean isPlayerControlled(Entity entity) {
        return entity instanceof Player
                || entity instanceof TamableAnimal tamable && tamable.getOwner() instanceof Player;
    }

    private static void clearSetModifiers(ServerPlayer player) {
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), WIND_SPEED_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TIDE_SWIM_SPEED_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), OATH_SPEED_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), OATH_TOUGHNESS_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR), OATH_ARMOR_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), OATH_REALM_TOUGHNESS_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
        applyModifier(player.getAttribute(Attributes.ARMOR), OATH_REALM_ARMOR_ID, 0.0D,
                AttributeModifier.Operation.ADD_VALUE, false);
    }

    private static boolean isEnvironmentalDamage(DamageSource source) {
        return source.getEntity() == null && source.getDirectEntity() == null;
    }

    public static final class ModTags {
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> ECHO_REALM_HOSTILES =
                TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_realm_hostiles"));
    }

    public static final class TrueSightShadowTags {
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> ILLUSION_MOBS =
                TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("illusion_mobs"));
    }
}
