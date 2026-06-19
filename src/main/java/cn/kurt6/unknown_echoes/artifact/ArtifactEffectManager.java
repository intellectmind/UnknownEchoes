package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ArtifactEffectManager {
    private static final ResourceLocation[] TIDE_HEALTH_IDS = {
            UnknownEchoes.id("tide_lantern_health_1"),
            UnknownEchoes.id("tide_lantern_health_2"),
            UnknownEchoes.id("tide_lantern_health_3")
    };
    private static final ResourceLocation STORM_MOVEMENT_ID = UnknownEchoes.id("storm_compass_movement");
    private static final ResourceLocation LENS_ATTACK_SPEED_ID = UnknownEchoes.id("echo_lens_attack_speed");
    private static final double LENS_RANGE = 24.0D;

    private static final Map<UUID, StormFlight> STORM_FLIGHTS = new HashMap<>();
    private static final Map<UUID, Long> STORM_SAFE_FALL_ENDS = new HashMap<>();
    private static final Map<UUID, Long> TIDE_AUTO_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<UUID, LensMark>> LENS_MARKS = new HashMap<>();
    private static final Map<UUID, Long> LENS_FOCUS_ENDS = new HashMap<>();

    private ArtifactEffectManager() {
    }

    public static boolean use(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case STORM_COMPASS -> useStormCompass(player);
            case TIDE_LANTERN -> useTideLantern(player);
            case ECHO_LENS -> useEchoLens(player);
        };
    }

    public static int getEnergyCost(ServerPlayer player, ArtifactType type) {
        int cost = switch (type) {
            case STORM_COMPASS -> ServerConfig.STORM_COMPASS_ENERGY_COST.get();
            case TIDE_LANTERN -> ServerConfig.TIDE_LANTERN_ENERGY_COST.get();
            case ECHO_LENS -> ServerConfig.ECHO_LENS_ENERGY_COST.get();
        };
        if (type == ArtifactType.STORM_COMPASS && EchoArmorSets.windWalkerPieces(player) >= 4) {
            cost = Math.round(cost * 0.90F);
        }
        return cost;
    }

    public static int getCooldownSeconds(ServerPlayer player, ArtifactType type) {
        int level = level(player, type);
        return switch (type) {
            case STORM_COMPASS -> Math.max(0,
                    ServerConfig.STORM_COMPASS_COOLDOWN_SECONDS.get() - Math.max(0, level - 1) * 20);
            case TIDE_LANTERN -> ServerConfig.TIDE_LANTERN_COOLDOWN_SECONDS.get();
            case ECHO_LENS -> ServerConfig.ECHO_LENS_COOLDOWN_SECONDS.get();
        };
    }

    public static void tick(ServerPlayer player) {
        tickStormFlight(player);
        if (player.tickCount % 40 == 0) {
            applyPassiveAttributes(player);
            tickEchoLensPassive(player);
            cleanupLensMarks(player);
        }
        if (player.tickCount % 20 == 0) {
            tickTideAutoGuard(player);
        }
        if (player.tickCount % 200 == 0) {
            tickTideSustain(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        stopStormFlight(player, false);
        UUID id = player.getUUID();
        STORM_SAFE_FALL_ENDS.remove(id);
        TIDE_AUTO_COOLDOWNS.remove(id);
        LENS_MARKS.remove(id);
        LENS_FOCUS_ENDS.remove(id);
    }

    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasLensFocus(player)) {
            return;
        }
        var item = event.getItem().getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            event.setDuration(Math.max(0, event.getDuration() - 1));
        }
    }

    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        Long safeEnd = STORM_SAFE_FALL_ENDS.get(player.getUUID());
        if (safeEnd != null && now <= safeEnd) {
            event.setDamageMultiplier(0.0F);
            return;
        }
        if (safeEnd != null) {
            STORM_SAFE_FALL_ENDS.remove(player.getUUID());
        }
        if (level(player, ArtifactType.STORM_COMPASS) > 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.7F);
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || level(player, ArtifactType.ECHO_LENS) <= 0
                || (event.getEntity() instanceof Player && !ServerConfig.ARTIFACT_PVP_EFFECTS.get())) {
            return;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null) {
            return;
        }
        LensMark mark = marks.get(event.getEntity().getUUID());
        long now = player.level().getGameTime();
        if (mark == null || mark.endTime < now || mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
            return;
        }
        float amount = event.getAmount();
        float bonus = Math.min(amount * ServerConfig.ECHO_LENS_DAMAGE_BONUS.get().floatValue(),
                ServerConfig.ECHO_LENS_DAMAGE_CAP.get().floatValue());
        if (level(player, ArtifactType.ECHO_LENS) >= 3 && isBrokenBoss(event.getEntity())) {
            bonus += amount * 0.08F;
        }
        event.setAmount(amount + bonus);
        mark.hits--;
        if (mark.hits <= 0) {
            marks.remove(event.getEntity().getUUID());
        }
    }

    private static boolean useStormCompass(ServerPlayer player) {
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.STORM_COMPASS))) {
            return false;
        }
        int level = level(player, ArtifactType.STORM_COMPASS);
        long end = player.level().getGameTime()
                + (long) (ServerConfig.STORM_COMPASS_FLIGHT_SECONDS.get() + Math.max(0, level - 1) * 4) * 20L;
        STORM_FLIGHTS.put(player.getUUID(), new StormFlight(end, level, player.getAbilities().mayfly));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        finishUse(player, ArtifactType.STORM_COMPASS, SoundEvents.ELYTRA_FLYING);
        return true;
    }

    private static boolean useTideLantern(ServerPlayer player) {
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.TIDE_LANTERN))) {
            return false;
        }
        applyTideRecovery(player, level(player, ArtifactType.TIDE_LANTERN), false);
        if (EchoArmorSets.tideStalkerPieces(player) >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0,
                    true, false, true));
        }
        finishUse(player, ArtifactType.TIDE_LANTERN, SoundEvents.AMETHYST_BLOCK_CHIME);
        return true;
    }

    private static boolean useEchoLens(ServerPlayer player) {
        int level = level(player, ArtifactType.ECHO_LENS);
        List<LivingEntity> targets = findLensTargets(player, level >= 2 ? 2 : 1);
        List<BlockPos> hidden = findAuthorizedHiddenContent(player, level);
        if (targets.isEmpty() && hidden.isEmpty()) {
            actionbar(player, "message.unknown_echoes.artifact.echo_lens.no_target");
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, getEnergyCost(player, ArtifactType.ECHO_LENS))) {
            return false;
        }
        Map<UUID, LensMark> marks = LENS_MARKS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        boolean trueSightSet = EchoArmorSets.trueSightShadowPieces(player) >= 4;
        long end = player.level().getGameTime() + (trueSightSet ? 260L : 200L);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    trueSightSet ? 280 : 220, 0, true, false, true));
            marks.put(target.getUUID(), new LensMark(end, 3));
        }
        if (level >= 3) {
            interruptNearbyEnemies(player);
        }
        LENS_FOCUS_ENDS.put(player.getUUID(), player.level().getGameTime() + 160L);
        revealAuthorizedHiddenContent(player, hidden);
        finishUse(player, ArtifactType.ECHO_LENS, SoundEvents.AMETHYST_BLOCK_RESONATE);
        return true;
    }

    private static void finishUse(ServerPlayer player, ArtifactType type, net.minecraft.sounds.SoundEvent sound) {
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.artifact.use." + type.getId()), false);
        player.level().playSound(null, player.blockPosition(), sound,
                SoundSource.PLAYERS, 0.9F, 1.2F);
        int cooldown = getCooldownSeconds(player, type);
        if (cooldown > 0) {
            ArtifactManager.startCooldown(player, type, cooldown);
        }
    }

    private static void tickStormFlight(ServerPlayer player) {
        StormFlight flight = STORM_FLIGHTS.get(player.getUUID());
        if (flight == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= flight.endTime || player.isSpectator()) {
            stopStormFlight(player, true);
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (player.isSprinting() && player.tickCount % 12 == 0) {
            Vec3 look = player.getLookAngle();
            double dash = flight.level >= 2 ? 0.18D : 0.14D;
            player.setDeltaMovement(player.getDeltaMovement()
                    .add(look.x * dash, Math.max(0.0D, look.y) * dash * 0.35D, look.z * dash));
            player.hurtMarked = true;
        }
        if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.2D, player.getZ(),
                    4, 0.25D, 0.08D, 0.25D, 0.01D);
        }
    }

    private static void stopStormFlight(ServerPlayer player, boolean grantSafeFall) {
        StormFlight flight = STORM_FLIGHTS.remove(player.getUUID());
        if (flight == null) {
            return;
        }
        if (!flight.hadMayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (grantSafeFall) {
            int windPieces = EchoArmorSets.windWalkerPieces(player);
            long duration = flight.level >= 3 ? 120L : windPieces >= 4 ? 100L : windPieces >= 2 ? 60L : 0L;
            if (duration > 0L) {
                STORM_SAFE_FALL_ENDS.put(player.getUUID(), player.level().getGameTime() + duration);
            }
        }
    }

    private static void applyPassiveAttributes(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        for (int i = 0; i < TIDE_HEALTH_IDS.length; i++) {
            applyModifier(health, TIDE_HEALTH_IDS[i], tideHealthBonus(i + 1),
                    AttributeModifier.Operation.ADD_VALUE, tideLevel == i + 1);
        }
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), STORM_MOVEMENT_ID,
                level(player, ArtifactType.STORM_COMPASS) > 0 && player.isSprinting() ? 0.04D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                level(player, ArtifactType.STORM_COMPASS) > 0 && player.isSprinting());
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), LENS_ATTACK_SPEED_ID,
                level(player, ArtifactType.ECHO_LENS) > 0 ? 0.05D : 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                level(player, ArtifactType.ECHO_LENS) > 0);
    }

    private static void tickTideSustain(ServerPlayer player) {
        int tideLevel = level(player, ArtifactType.TIDE_LANTERN);
        if (tideLevel <= 0) {
            return;
        }
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18 && player.tickCount % 600 == 0) {
            foodData.eat(1, 0.05F);
        }
        if (tideLevel >= 3) {
            softenControl(player, MobEffects.MOVEMENT_SLOWDOWN);
            softenControl(player, MobEffects.WEAKNESS);
            softenControl(player, MobEffects.DIG_SLOWDOWN);
        }
    }

    private static void softenControl(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null || instance.getDuration() <= 80 || instance.getAmplifier() > 1) {
            return;
        }
        int duration = Math.max(80, instance.getDuration() * 4 / 5);
        player.addEffect(new MobEffectInstance(effect, duration, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon()));
    }

    private static void tickTideAutoGuard(ServerPlayer player) {
        int level = level(player, ArtifactType.TIDE_LANTERN);
        if (level < 3 || player.getHealth() > player.getMaxHealth() * 0.3F) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < TIDE_AUTO_COOLDOWNS.getOrDefault(player.getUUID(), 0L)) {
            return;
        }
        TIDE_AUTO_COOLDOWNS.put(player.getUUID(),
                now + ServerConfig.TIDE_LANTERN_AUTO_COOLDOWN_SECONDS.get() * 20L);
        applyTideRecovery(player, 1, true);
        actionbar(player, "message.unknown_echoes.artifact.tide_lantern.auto");
    }

    private static void applyTideRecovery(ServerPlayer player, int level, boolean weak) {
        int effectiveLevel = Math.max(1, level);
        float heal = weak ? 4.0F : switch (effectiveLevel) {
            case 1 -> 6.0F;
            case 2 -> 8.0F;
            default -> 10.0F;
        };
        int food = weak ? 2 : 2 + effectiveLevel * 2;
        float saturation = weak ? 2.0F : 2.0F + effectiveLevel * 2.0F;
        player.heal(heal);
        FoodData foodData = player.getFoodData();
        foodData.eat(food, saturation / Math.max(1.0F, food * 2.0F));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, weak ? 80 : 160,
                effectiveLevel >= 3 && !weak ? 1 : 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, weak ? 120 : 160,
                effectiveLevel >= 3 && !weak ? 1 : 0, true, false, true));
        if (!weak && effectiveLevel >= 2) {
            clearOneNegativeEffect(player);
        }
    }

    private static void clearOneNegativeEffect(ServerPlayer player) {
        if (removeLowLevelEffect(player, MobEffects.POISON)
                || removeLowLevelEffect(player, MobEffects.WITHER)
                || removeLowLevelEffect(player, MobEffects.MOVEMENT_SLOWDOWN)
                || removeLowLevelEffect(player, MobEffects.WEAKNESS)
                || removeLowLevelEffect(player, MobEffects.DIG_SLOWDOWN)
                || removeLowLevelEffect(player, MobEffects.BLINDNESS)) {
            actionbar(player, "message.unknown_echoes.artifact.tide_lantern.cleaned");
        }
    }

    private static boolean removeLowLevelEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() <= 1) {
            player.removeEffect(effect);
            return true;
        }
        return false;
    }

    private static void tickEchoLensPassive(ServerPlayer player) {
        int level = level(player, ArtifactType.ECHO_LENS);
        if (level <= 0) {
            return;
        }
        int count = level >= 2 ? 2 : 1;
        for (LivingEntity target : findNearbyOrdinaryEnemies(player, level >= 2 ? 20.0D : 16.0D, count)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
        }
    }

    private static List<LivingEntity> findLensTargets(ServerPlayer player, int count) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().inflate(LENS_RANGE);
        List<LivingEntity> candidates = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && canLensAffect(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double forward = to.dot(look);
            if (forward <= 0.0D || forward > LENS_RANGE || !player.hasLineOfSight(entity)) {
                continue;
            }
            double distanceSq = to.lengthSqr() - forward * forward;
            double allowance = Math.max(1.5D, entity.getBbWidth() + 0.75D);
            if (distanceSq <= allowance * allowance) {
                candidates.add(entity);
            }
        }
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        return candidates.size() <= count ? candidates : new ArrayList<>(candidates.subList(0, count));
    }

    private static List<LivingEntity> findNearbyOrdinaryEnemies(ServerPlayer player, double range, int count) {
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> result = player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity instanceof Enemy && !isMainBoss(entity)
                        && player.hasLineOfSight(entity));
        result.sort(Comparator.comparingDouble(player::distanceToSqr));
        return result.size() <= count ? result : new ArrayList<>(result.subList(0, count));
    }

    private static List<BlockPos> findAuthorizedHiddenContent(ServerPlayer player, int lensLevel) {
        int range = lensLevel >= 3 ? 16 : lensLevel >= 2 ? 12 : 8;
        if (EchoArmorSets.trueSightShadowPieces(player) >= 4) {
            range += 4;
        }
        BlockPos center = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -4, -range),
                center.offset(range, 4, range))) {
            if (found.size() >= 6) {
                break;
            }
            if (isAuthorizedHiddenBlock(player, pos)) {
                found.add(pos.immutable());
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())));
        return found;
    }

    private static boolean isAuthorizedHiddenBlock(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        return state.is(ModBlocks.HIDDEN_RUNE_BRICKS.get())
                || state.is(ModBlocks.TRUE_SIGHT_STELE.get())
                || state.is(ModBlocks.TRUE_SIGHT_CHEST.get());
    }

    private static void revealAuthorizedHiddenContent(ServerPlayer player, List<BlockPos> positions) {
        if (positions.isEmpty() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : positions) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    5, 0.28D, 0.25D, 0.28D, 0.01D);
        }
        actionbar(player, "message.unknown_echoes.artifact.echo_lens.hidden_found");
    }

    private static boolean canLensAffect(ServerPlayer player, LivingEntity entity) {
        return !(entity instanceof Player) || ServerConfig.ARTIFACT_PVP_EFFECTS.get();
    }

    private static void interruptNearbyEnemies(ServerPlayer player) {
        int interrupted = 0;
        for (LivingEntity entity : findNearbyOrdinaryEnemies(player, 8.0D, 8)) {
            if (interrupted >= 3) {
                return;
            }
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            interrupted++;
        }
    }

    private static void cleanupLensMarks(ServerPlayer player) {
        Map<UUID, LensMark> marks = LENS_MARKS.get(player.getUUID());
        if (marks == null || marks.isEmpty()) {
            LENS_FOCUS_ENDS.computeIfPresent(player.getUUID(),
                    (id, end) -> end < player.level().getGameTime() ? null : end);
            return;
        }
        long now = player.level().getGameTime();
        for (Iterator<LensMark> it = marks.values().iterator(); it.hasNext(); ) {
            LensMark mark = it.next();
            if (mark.endTime < now || mark.hits <= 0) {
                it.remove();
            }
        }
        LENS_FOCUS_ENDS.computeIfPresent(player.getUUID(), (id, end) -> end < now ? null : end);
    }

    private static boolean hasLensFocus(ServerPlayer player) {
        Long end = LENS_FOCUS_ENDS.get(player.getUUID());
        if (end == null) {
            return false;
        }
        if (end < player.level().getGameTime()) {
            LENS_FOCUS_ENDS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id,
                                      double amount, AttributeModifier.Operation operation, boolean active) {
        if (attribute == null) {
            return;
        }
        if (active && amount > 0.0D && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else if (!active) {
            attribute.removeModifier(id);
        }
    }

    private static double tideHealthBonus(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return Math.min(ServerConfig.TIDE_LANTERN_MAX_HEALTH_BONUS.get(), switch (level) {
            case 1 -> 4.0D;
            case 2 -> 6.0D;
            default -> 8.0D;
        });
    }

    private static int level(ServerPlayer player, ArtifactType type) {
        return ArtifactManager.getData(player).getLevel(type);
    }

    private static boolean isMainBoss(LivingEntity entity) {
        return entity instanceof ForgottenColossus
                || (entity instanceof AbyssWatcher watcher && !watcher.isClone())
                || (entity instanceof MirrorGuardian guardian && !guardian.isIllusion());
    }

    private static boolean isBrokenBoss(LivingEntity entity) {
        return entity instanceof ForgottenColossus colossus && colossus.isBroken()
                || entity instanceof AbyssWatcher watcher && !watcher.isClone() && watcher.isBroken()
                || entity instanceof MirrorGuardian guardian && !guardian.isIllusion() && guardian.isBroken();
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record StormFlight(long endTime, int level, boolean hadMayfly) {
    }

    private static final class LensMark {
        private final long endTime;
        private int hits;

        private LensMark(long endTime, int hits) {
            this.endTime = endTime;
            this.hits = hits;
        }
    }
}
