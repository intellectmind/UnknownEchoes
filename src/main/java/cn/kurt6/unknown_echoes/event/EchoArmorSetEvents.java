package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * 套装件数效果(第 6 章,服务端判定):装备只增强既有能力/场景体验,不授予权限。
 * 成本控制:40 tick 周期检查 + 事件回调,不做每 tick 大范围扫描。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class EchoArmorSetEvents {

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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || player.tickCount % 40 != 0) {
            return;
        }
        int pieces = EchoArmorSets.explorerPieces(player);
        // 2 件:遗迹内可见度提升(亮度表现;周期刷新,时长盖过检查间隔避免闪烁)
        if (pieces >= 2 && player.level() instanceof ServerLevel serverLevel
                && serverLevel.structureManager()
                        .getStructureWithPieceAt(player.blockPosition(), ECHO_RUINS).isValid()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, false));
        }
        // 4 件:小幅移速 + 小幅抗性(瞬态修饰符,卸下装备后下个检查周期移除)
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, 0.05D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, pieces >= 4);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, pieces >= 4);
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, pieces >= 4);

        boolean inEchoRealm = player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID);
        applyEchoRealmSetEffects(player, inEchoRealm);
    }

    private static void applyEchoRealmSetEffects(ServerPlayer player, boolean inEchoRealm) {
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
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
        }

        if (tide >= 2 && player.isEyeInFluid(FluidTags.WATER)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
        if (tide >= 4 && player.isEyeInFluid(FluidTags.WATER)) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 80, 0, true, false, true));
        }
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), TIDE_SWIM_SPEED_ID, 0.08D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, tide >= 4 && player.isEyeInFluid(FluidTags.WATER));

        if (trueSight >= 2 && inEchoRealm) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }

        boolean inSilentSwamp = isInBiome(player, SILENT_SWAMP_ID);
        if (silent >= 2 && inSilentSwamp) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (silent >= 4 && inSilentSwamp) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.WEAKNESS);
        }

        boolean oathActive = oath >= 4 && inEchoRealm;
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), OATH_SPEED_ID, 0.04D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, oath >= 2 && inEchoRealm);
        applyModifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), OATH_TOUGHNESS_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive);
        applyModifier(player.getAttribute(Attributes.ARMOR), OATH_ARMOR_ID, 1.0D,
                AttributeModifier.Operation.ADD_VALUE, oathActive);
        if (oath >= 2 && inEchoRealm) {
            reduceMinorControl(player);
        }
    }

    private static void sendNearbyResourceHints(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int sent = 0;
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -3, -6),
                center.offset(6, 3, 6))) {
            if (sent >= 3) {
                return;
            }
            if (level.getBlockState(pos).is(RESOURCE_BLOCKS)
                    && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 4.0D) {
                level.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D,
                        2, 0.18D, 0.12D, 0.18D, 0.0D);
                sent++;
            }
        }
    }

    private static void reduceMinorControl(ServerPlayer player) {
        shortenEffect(player, MobEffects.MOVEMENT_SLOWDOWN);
        shortenEffect(player, MobEffects.WEAKNESS);
        shortenEffect(player, MobEffects.DIG_SLOWDOWN);
        shortenEffect(player, MobEffects.BLINDNESS);
    }

    private static void shortenEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.getDuration() <= 80 || instance.getAmplifier() > 1) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, 80, instance.getAmplifier(),
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
     * 探索者 4 件:略微降低回响生物的仇恨范围——
     * 远距离(>8 格)的索敌被取消,近身仍正常战斗;只影响本 Mod 的环境敌对生物(tag 可调)。
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
        if (event.getEntity() instanceof ServerPlayer player
                && (EchoArmorSets.windWalkerPieces(player) >= 2
                || EchoArmorSets.echoTravelerPieces(player) >= 2)
                && player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.85F);
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
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isPvpDamage(event.getSource())) {
            return;
        }
        boolean inEchoRealm = player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID);
        if (inEchoRealm && EchoArmorSets.echoTravelerPieces(player) >= 2
                && isEnvironmentalDamage(event.getSource())) {
            event.setAmount(event.getAmount() * 0.85F);
        }
        if (event.getSource().getEntity() != null
                && event.getSource().getEntity().getType().is(ModEntityTags.ILLUSION_MOBS)
                && EchoArmorSets.trueSightShadowPieces(player) >= 4) {
            event.setAmount(event.getAmount() * 0.75F);
        }
        if (inEchoRealm && EchoArmorSets.echoOathPieces(player) >= 2
                && isEnvironmentalDamage(event.getSource())) {
            event.setAmount(event.getAmount() * 0.80F);
        }
        if (inEchoRealm && EchoArmorSets.echoOathPieces(player) >= 4
                && event.getSource().getEntity() != null
                && !(event.getSource().getEntity() instanceof Player)) {
            event.setAmount(event.getAmount() * 0.90F);
        }
    }

    private static boolean isPvpDamage(DamageSource source) {
        return source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player;
    }

    private static boolean isEnvironmentalDamage(DamageSource source) {
        return source.getEntity() == null && source.getDirectEntity() == null;
    }

    /** 实体 tag 引用集中处。 */
    public static final class ModTags {
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> ECHO_REALM_HOSTILES =
                TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("echo_realm_hostiles"));
    }

    public static final class TrueSightShadowTags {
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> ILLUSION_MOBS =
                TagKey.create(Registries.ENTITY_TYPE, UnknownEchoes.id("illusion_mobs"));
    }
}
