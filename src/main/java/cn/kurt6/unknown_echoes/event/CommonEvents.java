package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.ability.ResonanceTokenManager;
import cn.kurt6.unknown_echoes.ability.RecipeUnlocks;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.block.AquaticBiomePlantBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepBiomePlantBlock;
import cn.kurt6.unknown_echoes.command.EchoCommands;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.config.MobConfigHandler;
import cn.kurt6.unknown_echoes.entity.boss.BossMaterialRewards;
import cn.kurt6.unknown_echoes.entity.boss.BossDeathSettlement;
import cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss;
import cn.kurt6.unknown_echoes.entity.boss.AshScribe;
import cn.kurt6.unknown_echoes.entity.boss.InvertedGatekeeper;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.entity.boss.RechallengeBoundBoss;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import cn.kurt6.unknown_echoes.item.tool.DeepAxeItem;
import cn.kurt6.unknown_echoes.item.NightVisionSupport;
import cn.kurt6.unknown_echoes.item.tool.DeepHoeItem;
import cn.kurt6.unknown_echoes.item.tool.DeepPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.DeepShovelItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHatchetItem;
import cn.kurt6.unknown_echoes.item.tool.EchoHoeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoPickaxeItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShearsItem;
import cn.kurt6.unknown_echoes.item.tool.EchoShovelItem;
import cn.kurt6.unknown_echoes.item.tool.LandmarkPickaxeItem;
import cn.kurt6.unknown_echoes.item.weapon.EchoMeleeWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.TrueSightBladeItem;
import cn.kurt6.unknown_echoes.item.weapon.WeaponEffectGuard;
import cn.kurt6.unknown_echoes.item.weapon.WindSpearItem;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.mixin.ItemDropAccessor;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.world.MainBossRechallengeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class CommonEvents {
    private static final String COMBAT_CONFIG_APPLIED_TAG = "unknown_echoes_combat_config_applied";
    private static final Map<UUID, Integer> LAST_ENERGY_REGEN_RATE = new HashMap<>();
    private static final Set<UUID> AREA_MINING_PLAYERS = new HashSet<>();
    private static final ArrayDeque<PendingCropReplant> PENDING_CROP_REPLANTS = new ArrayDeque<>();
    /** 真视研究 3 的夜视轮询周期，与 onPlayerTickTrueSight 的 tick 取模保持一致。 */
    private static final int TRUE_SIGHT_POLL_INTERVAL_TICKS = 40;
    private static final int TIDE_POLL_INTERVAL_TICKS = 20;
    private static final int TRUE_SIGHT_NIGHT_VISION_DURATION_TICKS =
            cn.kurt6.unknown_echoes.item.NightVisionSupport.sustainedDuration(
                    TRUE_SIGHT_POLL_INTERVAL_TICKS);
    /** 首次 Boss 补救只处理旧存档遗漏，不需要秒级响应；40 tick 足够且减半结构解析开销。 */
    private static final int AQUATIC_BOSS_RECOVERY_INTERVAL_TICKS =
            MainBossRechallengeData.INITIAL_RECOVERY_INTERVAL_TICKS;
    private static final float TIDE_UNDERWATER_MINING_MULTIPLIER = 1.3F;
    private static final float TIDE_UNDERWATER_MINING_MULTIPLIER_RESEARCH2 = 1.5F;
    private static final float TIDE_WET_DAMAGE_BONUS_RESEARCH_2 = 0.08F;
    private static final float TIDE_WET_DAMAGE_BONUS_RESEARCH_3 = 0.12F;
    private static final float TIDE_WET_DAMAGE_BONUS_RESEARCH_4 = 0.16F;
    private static final float TIDE_WET_DAMAGE_CAP_RESEARCH_2 = 3.0F;
    private static final float TIDE_WET_DAMAGE_CAP_RESEARCH_3 = 4.0F;
    private static final float TIDE_WET_DAMAGE_CAP_RESEARCH_4 = 5.0F;
    private static final float TRUE_SIGHT_ILLUSION_WEAKNESS_BONUS = 0.10F;
    private static final float TRUE_SIGHT_EXPOSED_WEAKNESS_BONUS = 0.08F;
    private static final float TRUE_SIGHT_WEAKNESS_BONUS_CAP = 0.20F;
    private static final int LONGBOW_MARK_DURATION_TICKS = 200;
    private static final int MAX_LONGBOW_MARKS_PER_TARGET = 64;
    private static final String LONGBOW_MARK_PREFIX = "ue_longbow_mark_";
    private static final String LONGBOW_MARK_TIME_PREFIX = "ue_longbow_mark_time_";

    /** 时间沙漏在实体 tick 前拦截冻结目标；玩家自身和客户端表现不受影响。 */
    @SubscribeEvent
    public static void onEntityTickFreeze(EntityTickEvent.Pre event) {
        if (event.getEntity().level() instanceof ServerLevel
                && ArtifactEffectManager.onEntityTick(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ArtifactEffectManager.onEntityLeave(event.getEntity());
        if (event.getEntity().getRemovalReason() == null
                || !event.getEntity().getRemovalReason().shouldDestroy()
                || event.getEntity() instanceof BossDeathSettlement settlement && settlement.isSettlingDeath()) {
            return;
        }
        // 原版掉落、进度等回调可同步移除 Boss；已提交死亡的原场地由 die 的 finally 结算。
        if (event.getEntity() instanceof RechallengeBoundBoss) {
            cn.kurt6.unknown_echoes.world.MainBossRechallengeData.onBossDestroyed(
                    serverLevel, event.getEntity());
        }
        if (event.getEntity() instanceof DeepRechallengeBoundBoss boundBoss) {
            boundBoss.clearDeepRechallengePresence(serverLevel);
        }
        if (event.getEntity() instanceof MiniBossEntity boss && boss.hasSpawnerBinding()) {
            boss.clearSpawnerPresence(serverLevel);
        }
    }

    /** Boss 加载当 tick 即启用场地封锁，避免首次心跳前出现破坏与开箱空窗。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(
            net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && event.getEntity() instanceof RechallengeBoundBoss boundBoss
                && !boundBoss.registerRechallengePresence(serverLevel)) {
            event.setCanceled(true);
            event.getEntity().discard();
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel
                && event.getEntity() instanceof LivingEntity living
                && isSealingMainBoss(living)) {
            noteMainBossPresence(serverLevel);
        }
        if (event.getLevel() instanceof ServerLevel
                && event.getEntity() instanceof LivingEntity living) {
            applyCombatConfig(living);
        }
    }

    /** 对整合包作者暴露的战斗倍率只在实体加入世界时应用一次；持久标记避免区块重载重复叠乘。 */
    private static void applyCombatConfig(LivingEntity living) {
        boolean worldEcho = living instanceof WorldEcho;
        boolean miniBoss = living instanceof MiniBossEntity;
        boolean deepMiniBoss = living instanceof AshScribe || living instanceof InvertedGatekeeper;
        if (living instanceof cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher watcher
                && watcher.isClone()) {
            return;
        }
        if (living instanceof cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian guardian
                && guardian.isIllusion()) {
            return;
        }
        boolean mainBoss = living instanceof RechallengeBoundBoss
                || living instanceof DeepRechallengeBoundBoss;
        boolean ordinaryMob = MobConfigHandler.isOrdinaryMob(living);
        if (!worldEcho && !miniBoss && !mainBoss && !ordinaryMob) {
            return;
        }

        CompoundTag persistent = living.getPersistentData();
        if (persistent.getBoolean(COMBAT_CONFIG_APPLIED_TAG)) {
            return;
        }

        double healthMultiplier;
        double armorMultiplier;
        double movementSpeedMultiplier;
        double followRangeMultiplier;
        if (worldEcho) {
            healthMultiplier = ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get()
                    * ServerConfig.WORLD_ECHO_HEALTH_MULTIPLIER.get();
            armorMultiplier = ServerConfig.DEEP_BOSS_ARMOR_MULTIPLIER.get()
                    * ServerConfig.WORLD_ECHO_ARMOR_MULTIPLIER.get();
            movementSpeedMultiplier = ServerConfig.DEEP_BOSS_MOVEMENT_SPEED_MULTIPLIER.get()
                    * ServerConfig.WORLD_ECHO_MOVEMENT_SPEED_MULTIPLIER.get();
            followRangeMultiplier = ServerConfig.DEEP_BOSS_FOLLOW_RANGE_MULTIPLIER.get()
                    * ServerConfig.WORLD_ECHO_FOLLOW_RANGE_MULTIPLIER.get();
        } else if (miniBoss) {
            healthMultiplier = (deepMiniBoss ? ServerConfig.DEEP_MINIBOSS_HEALTH_MULTIPLIER.get()
                    : ServerConfig.MINIBOSS_HEALTH_MULTIPLIER.get());
            armorMultiplier = (deepMiniBoss ? ServerConfig.DEEP_MINIBOSS_ARMOR_MULTIPLIER.get()
                    : ServerConfig.MINIBOSS_ARMOR_MULTIPLIER.get());
            movementSpeedMultiplier = (deepMiniBoss
                    ? ServerConfig.DEEP_MINIBOSS_MOVEMENT_SPEED_MULTIPLIER.get()
                    : ServerConfig.MINIBOSS_MOVEMENT_SPEED_MULTIPLIER.get());
            followRangeMultiplier = (deepMiniBoss
                    ? ServerConfig.DEEP_MINIBOSS_FOLLOW_RANGE_MULTIPLIER.get()
                    : ServerConfig.MINIBOSS_FOLLOW_RANGE_MULTIPLIER.get());
        } else if (ordinaryMob) {
            boolean monster = MobConfigHandler.isMonster(living);
            healthMultiplier = monster ? ServerConfig.MONSTER_HEALTH_MULTIPLIER.get()
                    : ServerConfig.CREATURE_HEALTH_MULTIPLIER.get();
            armorMultiplier = monster ? ServerConfig.MONSTER_ARMOR_MULTIPLIER.get()
                    : ServerConfig.CREATURE_ARMOR_MULTIPLIER.get();
            movementSpeedMultiplier = monster ? ServerConfig.MONSTER_MOVEMENT_SPEED_MULTIPLIER.get()
                    : ServerConfig.CREATURE_MOVEMENT_SPEED_MULTIPLIER.get();
            followRangeMultiplier = monster ? ServerConfig.MONSTER_FOLLOW_RANGE_MULTIPLIER.get()
                    : ServerConfig.CREATURE_FOLLOW_RANGE_MULTIPLIER.get();
        } else {
            boolean deepBoss = living instanceof DeepRechallengeBoundBoss;
            healthMultiplier = deepBoss ? ServerConfig.DEEP_BOSS_HEALTH_MULTIPLIER.get()
                    : ServerConfig.BOSS_HEALTH_MULTIPLIER.get();
            armorMultiplier = deepBoss ? ServerConfig.DEEP_BOSS_ARMOR_MULTIPLIER.get()
                    : ServerConfig.BOSS_ARMOR_MULTIPLIER.get();
            movementSpeedMultiplier = deepBoss
                    ? ServerConfig.DEEP_BOSS_MOVEMENT_SPEED_MULTIPLIER.get()
                    : ServerConfig.BOSS_MOVEMENT_SPEED_MULTIPLIER.get();
            followRangeMultiplier = deepBoss
                    ? ServerConfig.DEEP_BOSS_FOLLOW_RANGE_MULTIPLIER.get()
                    : ServerConfig.BOSS_FOLLOW_RANGE_MULTIPLIER.get();
        }

        float oldMaxHealth = living.getMaxHealth();
        float healthRatio = oldMaxHealth > 0.0F
                ? Math.max(0.0F, Math.min(1.0F, living.getHealth() / oldMaxHealth)) : 1.0F;
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMultiplier);
        }
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armor.getBaseValue() * armorMultiplier);
        }
        AttributeInstance armorToughness = living.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armorToughness != null) {
            armorToughness.setBaseValue(armorToughness.getBaseValue() * armorMultiplier);
        }
        AttributeInstance movementSpeed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(movementSpeed.getBaseValue() * movementSpeedMultiplier);
        }
        AttributeInstance flyingSpeed = living.getAttribute(Attributes.FLYING_SPEED);
        if (flyingSpeed != null) {
            flyingSpeed.setBaseValue(flyingSpeed.getBaseValue() * movementSpeedMultiplier);
        }
        AttributeInstance followRange = living.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(followRange.getBaseValue() * followRangeMultiplier);
        }
        if (maxHealth != null) {
            living.setHealth(living.getMaxHealth() * healthRatio);
        }
        persistent.putBoolean(COMBAT_CONFIG_APPLIED_TAG, true);
    }

    /** 在伤害事件入口缩放，覆盖属性攻击和技能里直接传入的固定伤害。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBossIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        double multiplier;
        if (attacker instanceof WorldEcho) {
            multiplier = ServerConfig.DEEP_BOSS_DAMAGE_MULTIPLIER.get()
                    * ServerConfig.WORLD_ECHO_DAMAGE_MULTIPLIER.get();
        } else if (attacker instanceof AshScribe || attacker instanceof InvertedGatekeeper) {
            multiplier = ServerConfig.DEEP_MINIBOSS_DAMAGE_MULTIPLIER.get();
        } else if (attacker instanceof MiniBossEntity) {
            multiplier = ServerConfig.MINIBOSS_DAMAGE_MULTIPLIER.get();
        } else if (attacker instanceof RechallengeBoundBoss
                || attacker instanceof DeepRechallengeBoundBoss) {
            multiplier = attacker instanceof DeepRechallengeBoundBoss
                    ? ServerConfig.DEEP_BOSS_DAMAGE_MULTIPLIER.get()
                    : ServerConfig.BOSS_DAMAGE_MULTIPLIER.get();
        } else if (MobConfigHandler.isOrdinaryMob(attacker)) {
            multiplier = MobConfigHandler.isMonster(attacker) ? ServerConfig.MONSTER_DAMAGE_MULTIPLIER.get()
                    : ServerConfig.CREATURE_DAMAGE_MULTIPLIER.get();
        } else {
            return;
        }
        event.setAmount(Math.max(0.0F, event.getAmount() * (float) multiplier));
    }

    /** 原场地复战 Boss 不允许穿越传送门，否则原维度活动锁与场地绑定会分裂。 */
    @SubscribeEvent
    public static void onBossTravelToDimension(
            net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
        boolean firstDimensionBoss = event.getEntity() instanceof RechallengeBoundBoss boundBoss
                && boundBoss.rechallengeAltar() != null;
        boolean deepBoss = event.getEntity() instanceof DeepRechallengeBoundBoss boundBoss
                && boundBoss.deepRechallengeAltar() != null;
        boolean miniBoss = event.getEntity() instanceof MiniBossEntity boss
                && boss.hasSpawnerBinding();
        boolean finalBoss = event.getEntity() instanceof WorldEcho boss
                && !boss.canBindToAltar();
        if (firstDimensionBoss || deepBoss || miniBoss || finalBoss) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArtifactEffectManager.onPlayerLoggedIn(player);
            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.retryAllPagesReward(player);
            ModAdvancements.checkJournalAdvancements(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);
            ArtifactManager.refreshCredentialOwnerNames(player);
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
            LAST_ENERGY_REGEN_RATE.put(player.getUUID(),
                    cn.kurt6.unknown_echoes.artifact.ArtifactManager.getRegenHundredthsPerTick(player));
            restorePendingArtifactCredentials(player, true);
            BossMaterialRewards.loadPendingPersonalRewards(player);
            MiniBossEntity.loadPendingFirstKillRewards(player);
            cn.kurt6.unknown_echoes.item.WorldtreeFruitItem.applyHealthBonus(player);
            ModAdvancements.checkDeepExplorationAdvancements(player,
                    JournalManager.getData(player).getStructures(),
                    JournalManager.getData(player).getBiomes());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cn.kurt6.unknown_echoes.ability.WindGlideTracker.clear(event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.clear(event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity.clearMarkCooldown(event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.network.ModNetworking.clearRateLimit(event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.item.NightVisionSupport.clearClaims(
                event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.trade.EchoTradeManager.clearSessionView(event.getEntity().getUUID());
        cn.kurt6.unknown_echoes.block.deep.DeepAltarScanLimiter.clear(event.getEntity().getUUID());
        LAST_ENERGY_REGEN_RATE.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            ArtifactEffectManager.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ArtifactEffectManager.saveTideEffectsBeforeStop(event.getServer());
        ArtifactEffectManager.restoreFrozenEntities(event.getServer());
        PendingCropReplant task;
        while ((task = PENDING_CROP_REPLANTS.pollFirst()) != null) {
            refundCropReplantSeed(event.getServer(), task);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        cn.kurt6.unknown_echoes.ability.WindGlideTracker.clearAll();
        cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.clearAll();
        ArtifactEffectManager.clearAll();
        cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity.clearLoadedPlatforms();
        cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity.clearAllMarkCooldowns();
        cn.kurt6.unknown_echoes.network.ModNetworking.clearAllRateLimits();
        cn.kurt6.unknown_echoes.item.NightVisionSupport.clearAllClaims();
        cn.kurt6.unknown_echoes.trade.EchoTradeManager.clearAllSessions();
        cn.kurt6.unknown_echoes.block.deep.DeepAltarScanLimiter.clearAll();
        cn.kurt6.unknown_echoes.ecology.RealmFacilityCircuitManager.clearAll();
        LAST_ENERGY_REGEN_RATE.clear();
        MAIN_BOSS_PRESENCE.clear();
        PENDING_CROP_REPLANTS.clear();
    }

    /** 区域型夜视租约：玩家走出世界事件半径后没有回调，靠租约过期立刻收掉效果。 */
    @SubscribeEvent
    public static void onServerTickNightVisionLease(ServerTickEvent.Post event) {
        cn.kurt6.unknown_echoes.item.NightVisionSupport.tickClaims(
                event.getServer(), event.getServer().overworld().getGameTime());
    }

    /** 掉落事件不保证方块已被移除，统一在本 tick 结束后补种。 */
    @SubscribeEvent
    public static void onServerTickCropReplant(ServerTickEvent.Post event) {
        int pendingCount = PENDING_CROP_REPLANTS.size();
        for (int i = 0; i < pendingCount; i++) {
            PendingCropReplant task = PENDING_CROP_REPLANTS.pollFirst();
            if (task == null) {
                break;
            }
            ServerLevel level = event.getServer().getLevel(task.dimension());
            if (level != null && level.hasChunkAt(task.pos()) && level.getBlockState(task.pos()).isAir()
                    && task.replanted().canSurvive(level, task.pos())
                    && level.setBlock(task.pos(), task.replanted(),
                            net.minecraft.world.level.block.Block.UPDATE_ALL)) {
                continue;
            }
            refundCropReplantSeed(event.getServer(), task);
        }
    }

    private static void refundCropReplantSeed(net.minecraft.server.MinecraftServer server,
                                               PendingCropReplant task) {
        cn.kurt6.unknown_echoes.world.CropReplantRefundData.refund(
                server, task.playerId(), task.refund());
    }

    /** 数据包重载后只做一次全量配方书补缺；玩家登录已有同步入口负责同一检查。 */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getRelevantPlayers().forEach(RecipeUnlocks::checkAndUnlock);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickGlide(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cn.kurt6.unknown_echoes.ability.WindGlideTracker.tick(player);
            cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.tick(player);
            ArtifactEffectManager.tick(player);
            if (player.tickCount % 20 == 0) {
                ArtifactManager.syncPerpetualEchoCoreState(player);
            }
            if (player.tickCount % 100 == 0) {
                RecipeUnlocks.checkPendingUnlocks(player);
                restorePendingArtifactCredentials(player, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cn.kurt6.unknown_echoes.trade.EchoTradeManager.clearSessionView(player.getUUID());
            cn.kurt6.unknown_echoes.ability.WindGlideTracker.clear(player.getUUID());
            cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.clear(player.getUUID());
            ArtifactEffectManager.onPlayerRespawn(player, event.isEndConquered());
            restorePendingArtifactCredentials(player, true);
            ArtifactManager.refreshCredentialOwnerNames(player);
            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
            cn.kurt6.unknown_echoes.item.WorldtreeFruitItem.applyHealthBonus(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {

        cn.kurt6.unknown_echoes.ability.CooldownPersistence.copyCooldownDeadlines(
                event.getOriginal(), event.getEntity());
        cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.copyCooldowns(
                event.getOriginal(), event.getEntity());
        cn.kurt6.unknown_echoes.item.WorldtreeFruitItem.copyPersistentData(
                event.getOriginal(), event.getEntity());
        cn.kurt6.unknown_echoes.ecology.RealmFacilityCircuitManager.copyCooldowns(
                event.getOriginal(), event.getEntity());
        ArtifactManager.copyPendingCredentialReturns(event.getOriginal(), event.getEntity());
        ResonanceTokenManager.copyPendingTokenReturns(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cn.kurt6.unknown_echoes.trade.EchoTradeManager.clearSessionView(player.getUUID());
            cn.kurt6.unknown_echoes.ability.WindGlideTracker.clear(player.getUUID());
            cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler.clear(player.getUUID());
            cn.kurt6.unknown_echoes.entity.projectile.TideBoltEntity.clearMarkCooldown(player.getUUID());
            ArtifactEffectManager.onPlayerChangedDimension(player);

            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);

            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
        }
    }

    /** 个人绑定凭据:玩家数据是权威,有效凭据不进入掉落/抢夺循环。 */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getEntity().getItem();
        if (ArtifactManager.isCurrentCredential(player, stack)) {
            event.setCanceled(true);
            if (ArtifactManager.returnTossedCredential(player, stack)) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.artifact.bound_returned"), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.artifact.need_space"), true);
            }
            return;
        }
        if (!ResonanceTokenManager.isCurrentToken(player, stack)) {
            return;
        }
        event.setCanceled(true);
        if (ResonanceTokenManager.returnTossedToken(player, stack)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.token.bound_returned"), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.token.need_space"), true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Iterator<net.minecraft.world.entity.item.ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next().getItem();
            if (ArtifactManager.isCurrentCredential(player, stack)) {
                ArtifactManager.queueCredentialReturn(player, stack);
                iterator.remove();
            } else if (ResonanceTokenManager.isCurrentToken(player, stack)) {
                ResonanceTokenManager.queueTokenReturn(player, stack);
                iterator.remove();
            }
        }
    }

    private static void restorePendingArtifactCredentials(ServerPlayer player, boolean notify) {
        if (ArtifactManager.restorePendingCredentialReturns(player) && notify) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.artifact.need_space"), true);
        }
        if (ResonanceTokenManager.restorePendingTokenReturns(player) && notify) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.token.need_space"), true);
        }
    }

    /**
     * 潮汐弩"湖水凝矢":不携带箭也能装填——湖水替它凝出弩矢;
     * 玩家背包里的箭(含药水箭)仍然优先消耗,虚拟箭只在完全没箭时出现。
     */
    @SubscribeEvent
    public static void onGetProjectile(net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent event) {
        if (event.getProjectileItemStack().isEmpty()
                && event.getProjectileWeaponItemStack().getItem()
                        instanceof cn.kurt6.unknown_echoes.item.weapon.TideCrossbowItem) {
            net.minecraft.world.item.ItemStack virtualArrow = new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.ARROW);
            net.minecraft.world.item.component.CustomData.update(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA, virtualArrow,
                    tag -> tag.putBoolean("ue_tide_virtual", true));
            event.setProjectileItemStack(virtualArrow);
        }
    }

    /** 摔落保护从风之研究 1 起生效;基础风之回响只有二段跳。 */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        ArtifactEffectManager.onFall(event);
        if (event.getEntity() instanceof ServerPlayer player
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)) {
            int research = JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO);
            if (research >= 1) {
                event.setDamageMultiplier(event.getDamageMultiplier()
                        * (research >= 3 ? 0.35F : 0.6F));
            }
        }
    }

    /** 潮汐研究 2:水下方块交互距离 +2(读取水下符文更从容)。表现/容错强化,不解锁权限。 */
    private static final ResourceLocation TIDE_REACH_MODIFIER_ID = UnknownEchoes.id("tide_research_reach");

    /** 潮汐回响体验层:基础只保留水下呼吸;加速/视野从潮汐研究 1 开始。 */
    @SubscribeEvent
    public static void onPlayerTickTide(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TIDE_POLL_INTERVAL_TICKS != 0) {
            return;
        }
        boolean hasTide = EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO);
        boolean inWater = player.isEyeInFluid(FluidTags.WATER);
        int tideResearch = hasTide
                ? JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO)
                : 0;

        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reach != null) {
            boolean want = hasTide && inWater && tideResearch >= 2;
            boolean has = reach.hasModifier(TIDE_REACH_MODIFIER_ID);
            if (want && !has) {
                reach.addTransientModifier(new AttributeModifier(TIDE_REACH_MODIFIER_ID,
                        2.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!want && has) {
                reach.removeModifier(TIDE_REACH_MODIFIER_ID);
            }
        }
        if (!hasTide || !inWater) {
            NightVisionSupport.release(player, NightVisionSupport.Source.TIDE);
            syncEnergyRateIfChanged(player);
            return;
        }
        addEffectIfMissing(player, MobEffects.WATER_BREATHING, 60);
        if (tideResearch >= 1) {
            addEffectIfMissing(player, MobEffects.DOLPHINS_GRACE, 60);

            NightVisionSupport.sustain(player, NightVisionSupport.Source.TIDE, 260,
                    TIDE_POLL_INTERVAL_TICKS, true, player.server.overworld().getGameTime());
        } else {
            NightVisionSupport.release(player, NightVisionSupport.Source.TIDE);
        }
        syncEnergyRateIfChanged(player);
    }

    /** 真视研究 2/3:日常感知强化,逐步抵抗失明/黑暗并在暗处保持低调夜视。 */
    @SubscribeEvent
    public static void onPlayerTickTrueSight(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % TRUE_SIGHT_POLL_INTERVAL_TICKS != 0) {
            return;
        }
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            NightVisionSupport.release(player, NightVisionSupport.Source.TRUE_SIGHT);
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.TRUE_SIGHT_ECHO);
        if (research >= 2) {
            player.removeEffect(MobEffects.BLINDNESS);
        }
        if (research >= 3) {
            player.removeEffect(MobEffects.DARKNESS);
            if (player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)
                    || player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 4) {
                refreshNightVision(player, TRUE_SIGHT_NIGHT_VISION_DURATION_TICKS);
            } else {

                cn.kurt6.unknown_echoes.item.NightVisionSupport.release(player,
                        cn.kurt6.unknown_echoes.item.NightVisionSupport.Source.TRUE_SIGHT);
            }
        } else {
            NightVisionSupport.release(player, NightVisionSupport.Source.TRUE_SIGHT);
        }
    }

    /**
     * 进入水下主线场地时补生成旧存档中因区块顺序遗漏的首次 Boss。
     * 两座神殿都只在回声境域，维度预判可以让其它维度完全不做结构解析。
     */
    @SubscribeEvent
    public static void onPlayerTickAquaticBossRecovery(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % AQUATIC_BOSS_RECOVERY_INTERVAL_TICKS != 0
                || !player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            return;
        }
        MainBossRechallengeData.ensureInitialBossForPlayer(
                player, MainBossRechallengeData.BossKind.MIRROR_GUARDIAN);
        MainBossRechallengeData.ensureInitialBossForPlayer(
                player, MainBossRechallengeData.BossKind.ABYSS_WATCHER);
    }

    /** 潮汐能量恢复加成随水/雨状态变化,只在速率变化时重推展示数据。 */
    private static void syncEnergyRateIfChanged(ServerPlayer player) {
        int rate = cn.kurt6.unknown_echoes.artifact.ArtifactManager.getRegenHundredthsPerTick(player);
        Integer previous = LAST_ENERGY_REGEN_RATE.put(player.getUUID(), rate);
        if (previous != null && previous != rate) {
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
        }
    }

    /** 周期性能力效果仅在缺失或即将过期时补发；夜视走独立阈值，避免屏幕闪烁。 */
    private static void addEffectIfMissing(ServerPlayer player,
                                           net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() > 0
                || current.getDuration() < 0
                || current.getDuration() > Math.max(1, duration / 2))) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
    }

    private static void refreshNightVision(ServerPlayer player, int duration) {
        NightVisionSupport.sustain(player, NightVisionSupport.Source.TRUE_SIGHT,
                duration, TRUE_SIGHT_POLL_INTERVAL_TICKS, true,
                player.server.overworld().getGameTime());
    }

    /** 潮汐研究 1/2:水下挖掘惩罚降低 30%/50%。 */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getNewSpeed() <= 0.0F) {
            return;
        }
        event.setNewSpeed(applyEchoToolBreakSpeed(player, event.getState(), event.getNewSpeed()));
        if (!player.isEyeInFluid(FluidTags.WATER)
                || !EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)) {
            return;
        }
        int researchLevel = JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO);
        if (researchLevel < 1) {
            return;
        }
        float multiplier = researchLevel >= 2
                ? TIDE_UNDERWATER_MINING_MULTIPLIER_RESEARCH2
                : TIDE_UNDERWATER_MINING_MULTIPLIER;
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }

    /** 真视研究 1/2:看穿幻象/暴露目标的弱点,给玩家所有伤害来源通用小幅增伤。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        ServerPlayer player = responsiblePlayer(event.getSource());
        if (player == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || target.isAlliedTo(player)) {
            return;
        }
        boolean weaponChain = WeaponEffectGuard.isActive(player);
        if (weaponChain) {
            return;
        }
        boolean exposesTarget = applyEchoBowDamage(event, player);
        exposesTarget |= applyHeldWeaponDamage(event, player);
        ArtifactEffectManager.onIncomingDamage(event);
        applyTideWetBonus(event, player);
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.TRUE_SIGHT_ECHO);
        if (research < 1) {
            return;
        }
        float bonus = 0.0F;
        if (target.getType().is(ModEntityTags.ILLUSION_MOBS)) {
            bonus += research >= 4 ? 0.15F : TRUE_SIGHT_ILLUSION_WEAKNESS_BONUS;
        }
        if (research >= 2 && (target.hasEffect(MobEffects.GLOWING) || exposesTarget)) {
            bonus += TRUE_SIGHT_EXPOSED_WEAKNESS_BONUS;
        }
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() * (1.0F + Math.min(bonus, TRUE_SIGHT_WEAKNESS_BONUS_CAP)));
        }
    }

    /** 只在最终结算实际消耗生命或吸收值后提交命中状态。 */
    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        ServerPlayer player = responsiblePlayer(event.getSource());
        if (player == null || WeaponEffectGuard.isActive(player)
                || !didDamageLand(event)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || target.isAlliedTo(player)) {
            return;
        }
        try {
            applyEchoBowHitEffects(event, player);
            applyHeldWeaponHitEffects(event, player);
        } finally {
            ArtifactEffectManager.onDamageApplied(event);
        }
    }

    private static boolean didDamageLand(LivingDamageEvent.Post event) {
        return event.getNewDamage() > 0.0F
                || event.getReduction(DamageContainer.Reduction.ABSORPTION) > 0.0F;
    }

    /** 兼容投射物或特殊伤害源只在 directEntity 中保留玩家施法者的情况。 */
    private static ServerPlayer responsiblePlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    /** 潮汐研究 2 级起对潮湿或水生普通敌人增伤；Boss 未破防时仍受实体侧伤害上限限制，PvP 默认不生效。 */
    private static void applyTideWetBonus(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)) {
            return;
        }
        int research = JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO);
        if (research < 2) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target instanceof Player && !ServerConfig.ARTIFACT_PVP_EFFECTS.get()) {
            return;
        }
        net.minecraft.world.entity.MobCategory category = target.getType().getCategory();
        boolean aquatic = target instanceof net.minecraft.world.entity.animal.WaterAnimal
                || category == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                || category == net.minecraft.world.entity.MobCategory.WATER_AMBIENT
                || category == net.minecraft.world.entity.MobCategory.UNDERGROUND_WATER_CREATURE;
        if (target.isInWaterRainOrBubble() || aquatic) {
            float multiplier = research >= 4 ? TIDE_WET_DAMAGE_BONUS_RESEARCH_4
                    : research >= 3 ? TIDE_WET_DAMAGE_BONUS_RESEARCH_3
                    : TIDE_WET_DAMAGE_BONUS_RESEARCH_2;
            float cap = research >= 4 ? TIDE_WET_DAMAGE_CAP_RESEARCH_4
                    : research >= 3 ? TIDE_WET_DAMAGE_CAP_RESEARCH_3
                    : TIDE_WET_DAMAGE_CAP_RESEARCH_2;
            float bonus = Math.min(event.getAmount() * multiplier, cap);
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        ArtifactEffectManager.onUseItemTick(event);
    }

    /** Player.attack 在 hurt 之前就重置蓄力条:在攻击入口先记下蓄力值,供武器特效判定"蓄力重击"。 */
    @SubscribeEvent
    public static void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putFloat(EchoMeleeWeaponItem.ATTACK_CHARGE_KEY,
                    player.getAttackStrengthScale(0.5F));
        }
    }

    private static boolean applyHeldWeaponDamage(LivingIncomingDamageEvent event, ServerPlayer player) {
        ItemStack held = meleeWeaponOf(event.getSource(), player);
        if (held == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        LivingEntity target = event.getEntity();
        if (!target.isAlive()) {
            return false;
        }
        var item = held.getItem();
        if (item instanceof TrueSightBladeItem
                && target.getType().is(ModEntityTags.ILLUSION_MOBS)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        if (WeaponEffectGuard.isActive(player)) {
            return false;
        }
        float bonus = 0.0F;
        boolean exposesTarget = false;
        if (item instanceof EchoMeleeWeaponItem weapon) {
            bonus = weapon.getDamageEventBonus(level, player, target);
            exposesTarget = weapon.wouldExposeTarget(level, player, target);
        } else if (item instanceof TrueSightBladeItem weapon) {
            bonus = weapon.getDamageEventBonus(level, player, target);
            exposesTarget = weapon.wouldExposeTarget(level, player, target);
        }
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
        }
        return exposesTarget;
    }

    private static void applyHeldWeaponHitEffects(LivingDamageEvent.Post event, ServerPlayer player) {
        ItemStack held = meleeWeaponOf(event.getSource(), player);
        if (held == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = event.getEntity();
        var item = held.getItem();
        if (item instanceof EchoMeleeWeaponItem weapon) {
            weapon.applyPostDamageEffect(level, player, target);
        } else if (item instanceof TrueSightBladeItem weapon) {
            weapon.applyPostDamageEffect(level, player, target);
        }
    }

    /**
     * 取本次近战伤害实际使用的武器;不是近战路径时返回 null。
     * 不能改成判 getDirectEntity() == player 再读主手:战斗类 Mod 会用玩家之外的 directEntity 结算。
     */
    @Nullable
    private static ItemStack meleeWeaponOf(DamageSource source, ServerPlayer player) {
        if (source.getDirectEntity() instanceof AbstractArrow) {
            return null;  // 远程走 applyEchoBowDamage
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            return weapon;
        }
        // 自定义 DamageSource 可能不带武器信息：只要伤害仍归因于本玩家，就退回读主手。
        return source.getEntity() == player || source.getDirectEntity() == player
                ? player.getMainHandItem() : null;
    }

    private static boolean applyEchoBowDamage(LivingIncomingDamageEvent event, ServerPlayer player) {
        AbstractArrow arrow = event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                ? directArrow : null;
        ItemStack firedFromWeapon = arrow == null ? null : arrow.getWeaponItem();
        LivingEntity target = event.getEntity();

        if (firedFromWeapon != null && firedFromWeapon.is(ModItems.ECHO_LONGBOW.get())) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        if (firedFromWeapon != null && firedFromWeapon.is(ModItems.DREAM_BLOOM_BOW.get())) {
            event.setAmount(event.getAmount() + 2.0F);
        }
        boolean leavesLongbowMark = arrow != null && arrow.isCritArrow()
                && firedFromWeapon != null && firedFromWeapon.is(ModItems.ECHO_LONGBOW.get());
        ItemStack melee = meleeWeaponOf(event.getSource(), player);
        if (melee != null && isEchoMeleeWeapon(melee)
                && hasLongbowMark(player, target)) {
            event.setAmount(event.getAmount() + 2.0F);
        }
        return leavesLongbowMark && target instanceof net.minecraft.world.entity.monster.Enemy;
    }

    private static void applyEchoBowHitEffects(LivingDamageEvent.Post event, ServerPlayer player) {
        AbstractArrow arrow = event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                ? directArrow : null;
        ItemStack firedFromWeapon = arrow == null ? null : arrow.getWeaponItem();
        LivingEntity target = event.getEntity();
        ItemStack melee = meleeWeaponOf(event.getSource(), player);
        if (melee != null && isEchoMeleeWeapon(melee)) {
            consumeLongbowMark(player, target);
        }
        if (arrow != null && arrow.isCritArrow()
                && firedFromWeapon != null && firedFromWeapon.is(ModItems.ECHO_LONGBOW.get())) {
            markLongbowTarget(player, target);
        }
        if (firedFromWeapon != null && firedFromWeapon.is(ModItems.DREAM_BLOOM_BOW.get())
                && target instanceof net.minecraft.world.entity.monster.Enemy
                && !target.getType().is(ModEntityTags.ECHO_BOSSES)
                && !target.getType().is(Tags.EntityTypes.BOSSES)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    100, 1, true, false, true));
        }
    }

    private static boolean hasLongbowMark(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.monster.Enemy)) {
            return false;
        }
        String suffix = player.getUUID().toString();
        return isValidLongbowMark(target.getPersistentData(), suffix, player.level().getGameTime());
    }

    private static void markLongbowTarget(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.monster.Enemy)) {
            return;
        }
        var data = target.getPersistentData();
        String suffix = player.getUUID().toString();
        long now = player.level().getGameTime();
        boolean refreshing = isValidLongbowMark(data, suffix, now);
        pruneLongbowMarks(data, now, suffix,
                refreshing ? MAX_LONGBOW_MARKS_PER_TARGET : MAX_LONGBOW_MARKS_PER_TARGET - 1);
        data.remove(LONGBOW_MARK_PREFIX + suffix);
        data.putLong(LONGBOW_MARK_TIME_PREFIX + suffix, now);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
    }

    private static boolean consumeLongbowMark(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.monster.Enemy)) {
            return false;
        }
        var data = target.getPersistentData();
        String suffix = player.getUUID().toString();
        long now = player.level().getGameTime();
        boolean marked = isValidLongbowMark(data, suffix, now);
        data.remove(LONGBOW_MARK_PREFIX + suffix);
        data.remove(LONGBOW_MARK_TIME_PREFIX + suffix);
        return marked;
    }

    private static boolean isValidLongbowMark(CompoundTag data, String suffix, long now) {
        String key = LONGBOW_MARK_TIME_PREFIX + suffix;
        if (!data.contains(key, Tag.TAG_LONG)) {
            return false;
        }
        long markedAt = data.getLong(key);
        return markedAt >= 0L && markedAt <= now && now - markedAt <= LONGBOW_MARK_DURATION_TICKS;
    }

    private static void pruneLongbowMarks(CompoundTag data, long now, String protectedSuffix,
                                          int maxCount) {
        List<String> staleKeys = new ArrayList<>();
        List<LongbowMark> validMarks = new ArrayList<>();
        for (String key : data.getAllKeys()) {
            if (key.startsWith(LONGBOW_MARK_TIME_PREFIX)) {
                String suffix = key.substring(LONGBOW_MARK_TIME_PREFIX.length());
                if (isValidLongbowMark(data, suffix, now)) {
                    validMarks.add(new LongbowMark(suffix, data.getLong(key)));
                } else {
                    staleKeys.add(key);
                }
            } else if (key.startsWith(LONGBOW_MARK_PREFIX)) {
                staleKeys.add(key);
            }
        }
        for (String key : staleKeys) {
            data.remove(key);
            if (key.startsWith(LONGBOW_MARK_TIME_PREFIX)) {
                data.remove(LONGBOW_MARK_PREFIX + key.substring(LONGBOW_MARK_TIME_PREFIX.length()));
            }
        }
        int excess = validMarks.size() - maxCount;
        if (excess <= 0) {
            return;
        }
        validMarks.sort(java.util.Comparator.comparingLong(LongbowMark::markedAt));
        for (LongbowMark mark : validMarks) {
            if (excess <= 0) {
                break;
            }
            if (!mark.suffix().equals(protectedSuffix)) {
                data.remove(LONGBOW_MARK_TIME_PREFIX + mark.suffix());
                data.remove(LONGBOW_MARK_PREFIX + mark.suffix());
                excess--;
            }
        }
    }

    private record LongbowMark(String suffix, long markedAt) {
    }

    private static boolean isEchoMeleeWeapon(ItemStack stack) {
        return stack.getItem() instanceof EchoMeleeWeaponItem
                || stack.getItem() instanceof TrueSightBladeItem
                || stack.getItem() instanceof WindSpearItem;
    }

    /** 关键机关方块保护:生存模式下不可破坏(管理员/创造除外)。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        boolean bypassProtection = player.isCreative() || player.hasPermissions(2);
        if (ServerConfig.PROTECT_CRITICAL_BLOCKS.get() && !bypassProtection) {

            if (event.getState().is(ModBlocks.CRITICAL_BLOCKS_TAG)) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.unknown_echoes.block.protected"), true);
                return;
            }

            if (event.getState().is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())
                    && player.level() instanceof ServerLevel songLevel
                    && findTunableSongkeeper(songLevel, event.getPos()) != null) {
                event.setCanceled(true);
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.crystal_songkeeper.guarded"), true);
                return;
            }

            if (player.level() instanceof ServerLevel serverLevel
                    && findNearbyAliveBoss(serverLevel, event.getPos()) != null) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.unknown_echoes.boss.area_sealed"), true);
                return;
            }
        }
    }

    /** 原始方块完成耐久和掉落判定后触发范围采掘，避免邻块先耗尽工具导致原始掉落丢失。 */
    private static void applyAreaMining(ServerPlayer player, ServerLevel level, BlockPos origin,
                                        net.minecraft.world.level.block.state.BlockState originState) {
        if (player.serverLevel() != level || !isAreaToolSkillActive(player)
                || AREA_MINING_PLAYERS.contains(player.getUUID())) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        List<BlockPos> targets;
        if (tool.getItem() instanceof EchoPickaxeItem
                && originState.is(ModBlocks.RESOURCE_BLOCKS_TAG)) {
            targets = connectedBlocks(level, origin, originState, 3);
        } else if (tool.getItem() instanceof EchoHatchetItem
                && originState.is(BlockTags.LOGS)) {
            targets = connectedBlocks(level, origin, originState, 7);
        } else if (tool.getItem() instanceof EchoShovelItem
                && isEchoLooseBlock(originState)) {
            targets = miningPlane(player, origin, originState, tool);
        } else if (tool.getItem() instanceof EchoHoeItem
                && originState.getBlock() instanceof CropBlock originCrop
                && originCrop.isMaxAge(originState)) {
            targets = miningPlane(player, origin, originState, tool);
        } else if (tool.getItem() instanceof EchoShearsItem
                && (originState.is(BlockTags.LEAVES) || isShearableEchoFlora(originState))) {
            targets = connectedBlocks(level, origin, originState, 11);
        } else if (tool.getItem() instanceof DeepAxeItem && originState.is(BlockTags.LOGS)) {
            targets = connectedBlocks(level, origin, originState, 15);
        } else if (tool.getItem() instanceof DeepHoeItem
                && (isMatureCrop(originState) || isEffectiveMiningTarget(tool, originState))) {
            targets = miningPlane(player, origin, originState, tool);
        } else if ((tool.getItem() instanceof DeepPickaxeItem
                || tool.getItem() instanceof DeepShovelItem
                || tool.getItem() instanceof LandmarkPickaxeItem)
                && isEffectiveMiningTarget(tool, originState)) {
            targets = miningPlane(player, origin, originState, tool);
        } else {
            return;
        }
        if (targets.isEmpty()) {
            return;
        }

        AREA_MINING_PLAYERS.add(player.getUUID());
        try {
            for (BlockPos target : targets) {
                // 耗尽的旧栈也会返回空气；绑定本批工具与世界，不能让空手或替换物继续挖掘。
                if (tool.isEmpty() || player.getMainHandItem() != tool
                        || player.serverLevel() != level) {
                    break;
                }
                player.gameMode.destroyBlock(target);
            }
        } finally {
            AREA_MINING_PLAYERS.remove(player.getUUID());
        }
    }

    /** 连锁/范围采集只认改键，不借用潜行（潜行要留给正常的走边缘与放置）。 */
    private static boolean isAreaToolSkillActive(ServerPlayer player) {
        return cn.kurt6.unknown_echoes.network.ModNetworking.isToolSkillKeyActive(player);
    }

    private static List<BlockPos> connectedBlocks(ServerLevel level, BlockPos origin,
                                                   net.minecraft.world.level.block.state.BlockState originState,
                                                   int limit) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.add(next) || next.equals(origin) || !level.hasChunkAt(next)) {
                    continue;
                }
                var state = level.getBlockState(next);
                if (state.getBlock() != originState.getBlock()
                        || level.getBlockEntity(next) != null) {
                    continue;
                }
                result.add(next.immutable());
                queue.addLast(next.immutable());
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private static List<BlockPos> miningPlane(ServerPlayer player, BlockPos origin,
                                              net.minecraft.world.level.block.state.BlockState originState,
                                              ItemStack tool) {
        var look = player.getLookAngle();
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);
        boolean harvestingCrops = (tool.getItem() instanceof EchoHoeItem
                || tool.getItem() instanceof DeepHoeItem)
                && originState.getBlock() instanceof CropBlock originCrop
                && originCrop.isMaxAge(originState);
        Direction.Axis axis = harvestingCrops || ay > ax && ay > az
                ? Direction.Axis.Y : ax > az ? Direction.Axis.X : Direction.Axis.Z;
        float originHardness = originState.getDestroySpeed(player.serverLevel(), origin);
        List<BlockPos> result = new ArrayList<>(8);
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                BlockPos target = switch (axis) {
                    case X -> origin.offset(0, first, second);
                    case Y -> origin.offset(first, 0, second);
                    case Z -> origin.offset(first, second, 0);
                };
                if (!player.serverLevel().hasChunkAt(target)
                        || player.serverLevel().getBlockEntity(target) != null) {
                    continue;
                }
                var state = player.serverLevel().getBlockState(target);
                float hardness = state.getDestroySpeed(player.serverLevel(), target);
                boolean validTarget = harvestingCrops
                        ? isMatureCrop(state)
                        : isEffectiveMiningTarget(tool, state);
                if (state.isAir() || hardness < 0.0F || !validTarget
                        || originHardness >= 0.0F && hardness > Math.max(1.0F, originHardness * 1.5F)) {
                    continue;
                }
                result.add(target.immutable());
            }
        }
        return result;
    }

    /** 回响、深层和地标镐将有熔炉配方的方块掉落直接转化为熔炼产物。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            applyEchoToolBreakReward(player, event);
        }

        autoSmeltDrops(event);
    }

    private static void autoSmeltDrops(BlockDropsEvent event) {
        if (!(event.getTool().getItem() instanceof EchoPickaxeItem
                || event.getTool().getItem() instanceof DeepPickaxeItem
                || event.getTool().getItem() instanceof LandmarkPickaxeItem)) {
            return;
        }
        var enchantments = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (event.getTool().getEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH)) > 0) {
            return;
        }
        List<ItemEntity> extraDrops = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack input = drop.getItem();
            if (input.isEmpty()) {
                continue;
            }
            SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
            event.getLevel().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, event.getLevel())
                    .ifPresent(recipe -> {
                        ItemStack output = recipe.value().assemble(
                                recipeInput, event.getLevel().registryAccess());
                        if (output.isEmpty() || output.getItem() == input.getItem()) {
                            return;
                        }
                        long total = (long) output.getCount() * input.getCount();
                        if (total > Integer.MAX_VALUE) {
                            return;
                        }
                        output.setCount((int) Math.min(total, output.getMaxStackSize()));
                        drop.setItem(output);
                        int remaining = (int) total - output.getCount();
                        while (remaining > 0) {
                            ItemStack remainder = output.copyWithCount(
                                    Math.min(remaining, output.getMaxStackSize()));
                            extraDrops.add(copySmeltedDrop(drop, remainder));
                            remaining -= remainder.getCount();
                        }
                    });
        }
        event.getDrops().addAll(extraDrops);
    }

    /** 拆出的同批熔炼产物沿用原掉落的拾取限制和生命周期。 */
    private static ItemEntity copySmeltedDrop(ItemEntity original, ItemStack stack) {
        ItemEntity copy = original.copy();
        copy.setItem(stack);
        copy.setDeltaMovement(original.getDeltaMovement());
        copy.setTarget(original.getTarget());
        ItemDropAccessor source = (ItemDropAccessor) original;
        ItemDropAccessor target = (ItemDropAccessor) copy;
        copy.setPickUpDelay(source.unknownEchoes$getPickupDelay());
        target.unknownEchoes$setThrower(source.unknownEchoes$getThrower());
        target.unknownEchoes$setHealth(source.unknownEchoes$getHealth());
        return copy;
    }

    /** 由掉落结算钩子在事件总线返回后调用，此时取消状态和掉落列表才已确定。 */
    public static void onBlockDropsResolved(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof ServerPlayer player)
                || !event.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                || event.getDrops().stream().noneMatch(drop -> !drop.getItem().isEmpty())) {
            return;
        }
        autoReplantMatureCrop(event, player);
        applyAreaMining(player, event.getLevel(), event.getPos(), event.getState());
    }

    /** 范围收割会用本次掉落或玩家库存中的一份种子补种，避免凭空复制种子。 */
    private static void autoReplantMatureCrop(BlockDropsEvent event, ServerPlayer player) {
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof EchoHoeItem || tool.getItem() instanceof DeepHoeItem)
                || !isAreaToolSkillActive(player)
                || !(event.getState().getBlock() instanceof CropBlock crop)
                || !crop.isMaxAge(event.getState())) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        var replanted = crop.getStateForAge(0);
        if (!replanted.canSurvive(level, pos)) {
            return;
        }

        ItemStack seed = crop.getCloneItemStack(level, pos, event.getState());
        if (seed.isEmpty()) {
            return;
        }
        ItemStack refund = ItemStack.EMPTY;
        if (!player.getAbilities().instabuild) {
            refund = consumeReplantSeed(event, player, seed);
            if (refund.isEmpty()) {
                return;
            }
        }
        PENDING_CROP_REPLANTS.addLast(new PendingCropReplant(
                level.dimension(), pos.immutable(), replanted, player.getUUID(), refund));
    }

    private static ItemStack consumeReplantSeed(BlockDropsEvent event, ServerPlayer player,
                                               ItemStack seed) {
        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next().getItem();
            if (!stack.is(seed.getItem())) {
                continue;
            }
            ItemStack consumed = stack.split(1);
            if (stack.isEmpty()) {
                drops.remove();
            }
            return consumed;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(seed.getItem())) {
                ItemStack consumed = stack.split(1);
                player.getInventory().setChanged();
                return consumed;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(seed.getItem())) {
                ItemStack consumed = stack.split(1);
                player.getInventory().setChanged();
                return consumed;
            }
        }
        return ItemStack.EMPTY;
    }

    private record PendingCropReplant(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockPos pos, net.minecraft.world.level.block.state.BlockState replanted,
            UUID playerId, ItemStack refund) {
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isReelingWithEchoFishingRod(player)
                || !(player.level() instanceof ServerLevel level)
                || !player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            return;
        }

        event.damageRodBy(Math.max(1, event.getRodDamage() - 1));
    }

    private static boolean isReelingWithEchoFishingRod(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.ECHO_FISHING_ROD.get())) {
            return true;
        }

        return !(mainHand.getItem() instanceof FishingRodItem)
                && player.getOffhandItem().is(ModItems.ECHO_FISHING_ROD.get());
    }

    private static float applyEchoToolBreakSpeed(ServerPlayer player,
                                                net.minecraft.world.level.block.state.BlockState state,
                                                float speed) {
        ItemStack tool = player.getMainHandItem();
        if (isEchoResourcePickaxe(tool) && state.is(ModBlocks.RESOURCE_BLOCKS_TAG)) {
            return speed * 1.25F;
        }
        if (isEchoLooseShovel(tool) && isEchoLooseBlock(state)) {
            return speed * 1.45F;
        }
        if (isEchoLogAxe(tool) && isEchoLog(state)) {
            return speed * 1.35F;
        }
        if (isEchoEcologyHoe(tool) && isEchoPlant(state)) {
            return speed * 1.35F;
        }
        return speed;
    }

    private static void applyEchoToolBreakReward(ServerPlayer player, BlockDropsEvent event) {
        ServerLevel level = player.serverLevel();
        var state = event.getState();
        ItemStack tool = event.getTool();
        if (event.isCanceled() || event.getDrops().stream().noneMatch(drop -> !drop.getItem().isEmpty())) {
            return;
        }
        if (isEchoLogAxe(tool) && isEchoLog(state)
                && level.random.nextFloat() < 0.20F) {
            consumeRecoveredBlockDrop(event);
            addToolByproduct(event, new ItemStack(ModItems.SILENT_RESIN.get()));
        } else if (isEchoLooseShovel(tool) && isEchoLooseBlock(state)
                && level.random.nextFloat() < 0.20F) {
            consumeRecoveredBlockDrop(event);
            addToolByproduct(event, new ItemStack(ModItems.ECHO_DUST.get()));
        } else if (isEchoEcologyHoe(tool) && isEchoPlant(state)
                && level.random.nextFloat() < 0.25F) {
            consumeRecoveredBlockDrop(event);
            addToolByproduct(event, plantByproduct(state));
        }
    }

    private static void addToolByproduct(BlockDropsEvent event, ItemStack reward) {
        BlockPos pos = event.getPos();
        ItemEntity drop = new ItemEntity(event.getLevel(),
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, reward);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static boolean isEchoEcologyHoe(ItemStack tool) {
        return tool.is(ModItems.ECHO_HOE.get())
                || tool.is(ModItems.DEEP_ALLOY_HOE.get());
    }

    private static boolean isEchoLooseShovel(ItemStack tool) {
        return tool.is(ModItems.ECHO_SHOVEL.get())
                || tool.is(ModItems.DEEP_ALLOY_SHOVEL.get());
    }

    private static boolean isEchoLogAxe(ItemStack tool) {
        return tool.is(ModItems.ECHO_HATCHET.get())
                || tool.is(ModItems.DEEP_ALLOY_AXE.get());
    }

    private static boolean isEchoResourcePickaxe(ItemStack tool) {
        return tool.is(ModItems.ECHO_PICKAXE.get())
                || tool.is(ModItems.DEEP_ALLOY_PICKAXE.get());
    }

    /** 若目标会掉回自身，将其中一份转化为工具副产物，避免反复放置复制材料。 */
    private static void consumeRecoveredBlockDrop(BlockDropsEvent event) {
        var recoveredItem = event.getState().getBlock().asItem();
        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next().getItem();
            if (!stack.is(recoveredItem)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                drops.remove();
            }
            return;
        }
    }

    private static ItemStack plantByproduct(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(ModBlocks.DREAM_FLOWER.get())) {
            return new ItemStack(ModItems.DREAM_NECTAR.get());
        }
        if (state.is(ModBlocks.MUFFLE_MOSS.get())) {
            return new ItemStack(ModItems.SILENCE_MOSS.get());
        }
        return new ItemStack(ModItems.RESONANT_CRYSTAL_NOTE.get());
    }

    private static boolean isEchoLooseBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.MIRROR_SAND.get())
                || state.is(ModBlocks.WIND_ERODED_RUBBLE.get());
    }

    private static boolean isMatureCrop(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean isEffectiveMiningTarget(
            ItemStack tool, net.minecraft.world.level.block.state.BlockState state) {
        return !state.isAir() && tool.getDestroySpeed(state) > 1.0F && tool.isCorrectToolForDrops(state);
    }

    private static boolean isEchoLog(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.ECHO_LOG.get())
                || state.is(ModBlocks.WHISPERING_LOG.get())
                || state.is(ModBlocks.TIDEWOOD_LOG.get());
    }

    private static boolean isEchoPlant(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof DeepBiomePlantBlock
                || state.getBlock() instanceof AquaticBiomePlantBlock
                || state.is(ModBlocks.DREAM_FLOWER.get())
                || state.is(ModBlocks.MUFFLE_MOSS.get())
                || state.is(ModBlocks.CRYSTAL_SONG_CLUSTER.get());
    }

    private static boolean isShearableEchoFlora(
            net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof DeepBiomePlantBlock
                || state.getBlock() instanceof AquaticBiomePlantBlock
                || state.is(ModBlocks.DREAM_FLOWER.get())
                || state.is(ModBlocks.MUFFLE_MOSS.get());
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    /** Boss 场地宝箱封锁:守护者存活时,场地内的箱子打不开(掉落物只有材料,但仍要先过试炼)。 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)
                || player.isCreative() || player.hasPermissions(2)) {
            return;
        }
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ChestBlock)
                || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity boss = findNearbyAliveBoss(serverLevel, event.getPos());
        if (boss != null) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.chest.sealed"), true);
        }
    }

    /** Boss 场地封锁范围。主线场地最大边长约 65 格，不能按 Boss 瞬时站位只查近身范围。 */
    private static final double BOSS_SEAL_RADIUS = 64.0D;
    private static final double BOSS_SEAL_VERTICAL_RADIUS = 32.0D;

    /** 各维度主线 Boss 心跳时间戳:Boss aiStep 周期上报;区块加载/卸载事件不对称,不能用计数器。 */
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Long>
            MAIN_BOSS_PRESENCE = new HashMap<>();
    private static final long MAIN_BOSS_PRESENCE_WINDOW_TICKS = 60L;

    public static void noteMainBossPresence(ServerLevel level) {
        MAIN_BOSS_PRESENCE.put(level.dimension(), level.getGameTime());
    }

    private static LivingEntity findNearbyAliveBoss(ServerLevel level, net.minecraft.core.BlockPos pos) {
        Long lastSeen = MAIN_BOSS_PRESENCE.get(level.dimension());
        long now = level.getGameTime();
        if (lastSeen == null || lastSeen > now
                || now - lastSeen > MAIN_BOSS_PRESENCE_WINDOW_TICKS) {
            return null;
        }
        AABB area = new AABB(pos).inflate(
                BOSS_SEAL_RADIUS, BOSS_SEAL_VERTICAL_RADIUS, BOSS_SEAL_RADIUS);
        java.util.List<LivingEntity> bosses = level.getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> isSealingMainBoss(entity)
                        && horizontalDistanceSqr(entity, pos) <= BOSS_SEAL_RADIUS * BOSS_SEAL_RADIUS);
        return bosses.isEmpty() ? null : bosses.getFirst();
    }

    private static double horizontalDistanceSqr(LivingEntity entity, BlockPos pos) {
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static boolean isSealingMainBoss(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher watcher) {
            return !watcher.isClone();
        }
        if (entity instanceof cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian guardian) {
            return !guardian.isIllusion();
        }
        return entity instanceof cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus
                || entity instanceof cn.kurt6.unknown_echoes.entity.boss.VoidProphet
                || entity instanceof cn.kurt6.unknown_echoes.entity.boss.RiftWarden
                || entity instanceof cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
    }

    /** 晶歌守谱者节奏谜题:右键场地音柱 = 调音(就近守谱者按序判定)。 */
    @SubscribeEvent
    public static void onTuneCrystalPillar(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.getBlockState(event.getPos()).is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())) {
            return;
        }
        cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper keeper =
                findTunableSongkeeper(serverLevel, event.getPos());
        if (keeper != null) {
            keeper.onTonePillarClicked(event.getPos(), player);
            event.setCanceled(true);
        }
    }

    private static cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper findTunableSongkeeper(
            ServerLevel level, net.minecraft.core.BlockPos pos) {
        AABB area = new AABB(pos).inflate(cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.TUNE_RANGE);
        for (var keeper : level.getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.class, area,
                keeper -> keeper.isAlive()
                        && keeper.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D)
                        <= cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.TUNE_RANGE
                        * cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.TUNE_RANGE)) {
            return keeper;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EchoCommands.register(event.getDispatcher());
        cn.kurt6.unknown_echoes.command.EchoAdminCommands.register(event.getDispatcher());
    }
}
