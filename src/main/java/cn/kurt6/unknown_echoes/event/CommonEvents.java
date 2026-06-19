package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.command.EchoCommands;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.boss.BossMaterialRewards;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.item.weapon.EchoMeleeWeaponItem;
import cn.kurt6.unknown_echoes.item.weapon.TrueSightBladeItem;
import cn.kurt6.unknown_echoes.item.weapon.WeaponEffectGuard;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public class CommonEvents {
    private static final Map<UUID, Integer> LAST_ENERGY_REGEN_RATE = new HashMap<>();
    private static final float TIDE_UNDERWATER_MINING_MULTIPLIER = 1.3F;
    private static final float TIDE_UNDERWATER_MINING_MULTIPLIER_RESEARCH2 = 1.5F;
    private static final float TIDE_WET_DAMAGE_BONUS = 0.08F;
    private static final float TIDE_WET_DAMAGE_CAP = 3.0F;
    private static final float TRUE_SIGHT_ILLUSION_WEAKNESS_BONUS = 0.10F;
    private static final float TRUE_SIGHT_EXPOSED_WEAKNESS_BONUS = 0.08F;
    private static final float TRUE_SIGHT_WEAKNESS_BONUS_CAP = 0.20F;
    private static final String LONGBOW_MARK_PREFIX = "ue_longbow_mark_";
    private static final String LONGBOW_MARK_TIME_PREFIX = "ue_longbow_mark_time_";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
            BossMaterialRewards.retryPendingPersonalRewards(player);
            MiniBossEntity.retryPendingFirstKillRewards(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cn.kurt6.unknown_echoes.ability.WindGlideTracker.clear(event.getEntity().getUUID());
        LAST_ENERGY_REGEN_RATE.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            ArtifactEffectManager.clearPlayer(player);
        }
    }

    /** 风之空中状态服务端追踪(V0.6E):逐 tick 维护离地/二段跳/滑翔/落地冷却。 */
    @SubscribeEvent
    public static void onPlayerTickGlide(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cn.kurt6.unknown_echoes.ability.WindGlideTracker.tick(player);
            ArtifactEffectManager.tick(player);
            if (player.tickCount % 100 == 0) {
                BossMaterialRewards.retryPendingPersonalRewards(player);
                MiniBossEntity.retryPendingFirstKillRewards(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArtifactEffectManager.clearPlayer(player);
            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArtifactEffectManager.clearPlayer(player);
            // 立即同步能力数据,避免客户端在维度切换期间读到空缓存导致卡顿
            EchoAbilityManager.syncToClient(player);
            cn.kurt6.unknown_echoes.journal.JournalManager.syncJournal(player);
            // 能量恢复速率随维度变化(境域内翻倍),换维度后重推一次供客户端外推
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
            if (event.getTo().location().equals(EchoPermission.ECHO_REALM_ID)) {
                EchoAbilityManager.unlockDimension(player, EchoPermission.ECHO_REALM_ID);
            }
        }
    }

    /** 风之研究 1:滑翔入门后才获得小幅摔落保护;基础风之回响只保留二段跳。 */
    /** 潮汐弩"湖水凝矢"(V3.2):不携带箭也能装填——湖水替它凝出弩矢;
     *  玩家背包里的箭(含药水箭)仍然优先消耗,虚拟箭只在完全没箭时出现。 */
    @SubscribeEvent
    public static void onGetProjectile(net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent event) {
        if (event.getProjectileItemStack().isEmpty()
                && event.getProjectileWeaponItemStack().getItem()
                        instanceof cn.kurt6.unknown_echoes.item.weapon.TideCrossbowItem) {
            event.setProjectileItemStack(new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.ARROW));
        }
    }

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
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        boolean hasTide = EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO);
        boolean inWater = player.isEyeInFluid(FluidTags.WATER);
        int tideResearch = hasTide
                ? JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO)
                : 0;
        // 研究 2 强化:只在"有潮汐 + 在水下"时挂瞬态修饰符,离水即移除(不入存档)
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
            syncEnergyRateIfChanged(player);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
        if (tideResearch >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
            // 首版用短时夜视占位"水下视野清晰度";后续可替换为客户端水雾/色调表现。
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
        syncEnergyRateIfChanged(player);
    }

    /** 真视研究 2/3:日常感知强化,逐步抵抗失明/黑暗并在暗处保持低调夜视。 */
    @SubscribeEvent
    public static void onPlayerTickTrueSight(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0
                || !EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)) {
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
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                        220, 0, true, false, true));
            }
        }
    }

    /** 潮汐能量恢复加成随水/雨状态变化,只在速率变化时重推展示数据。 */
    private static void syncEnergyRateIfChanged(ServerPlayer player) {
        int rate = cn.kurt6.unknown_echoes.artifact.ArtifactManager.getRegenHundredthsPerTick(player);
        Integer previous = LAST_ENERGY_REGEN_RATE.put(player.getUUID(), rate);
        if (previous != null && previous != rate) {
            cn.kurt6.unknown_echoes.artifact.ArtifactManager.syncToClient(player);
        }
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

    /** 真视研究 1/2:看穿幻象/暴露目标的弱点,给玩家所有伤害来源通用小幅增伤。
     *  不检查武器,避免能力研究与某把武器绑定;Boss 未破防保护在实体侧最后钳制。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyEchoBowEffects(event, player);
        applyHeldWeaponEffects(event, player);
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
        LivingEntity target = event.getEntity();
        if (target.getType().is(ModEntityTags.ILLUSION_MOBS)) {
            bonus += research >= 4 ? 0.15F : TRUE_SIGHT_ILLUSION_WEAKNESS_BONUS;
        }
        if (research >= 2 && target.hasEffect(MobEffects.GLOWING)) {
            bonus += TRUE_SIGHT_EXPOSED_WEAKNESS_BONUS;
        }
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() * (1.0F + Math.min(bonus, TRUE_SIGHT_WEAKNESS_BONUS_CAP)));
        }
    }

    /** 潮汐研究 2+:对"潮湿/水生"普通敌人小幅增伤(文档 §4.7.2);
     *  Boss 未破防时仍由实体侧未破防伤害上限钳制,等效"只在破防窗口内生效";PvP 默认不生效。 */
    private static void applyTideWetBonus(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (!EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                || JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO) < 2) {
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
            float bonus = Math.min(event.getAmount() * TIDE_WET_DAMAGE_BONUS, TIDE_WET_DAMAGE_CAP);
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        ArtifactEffectManager.onUseItemTick(event);
    }

    private static void applyHeldWeaponEffects(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (WeaponEffectGuard.isActive(player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || !target.isAlive()) {
            return;
        }
        var item = player.getMainHandItem().getItem();
        if (item instanceof EchoMeleeWeaponItem weapon) {
            weapon.applyDamageEventEffect(level, player, target);
        } else if (item instanceof TrueSightBladeItem weapon) {
            weapon.applyDamageEventEffect(level, player, target);
        }
    }

    private static void applyEchoBowEffects(LivingIncomingDamageEvent event, ServerPlayer player) {
        boolean projectile = event.getSource().getDirectEntity() instanceof AbstractArrow;
        LivingEntity target = event.getEntity();
        if (projectile && isHolding(player, ModItems.ECHO_LONGBOW.get())) {
            markLongbowTarget(player, target);
        } else if (!projectile && consumeLongbowMark(player, target)) {
            event.setAmount(event.getAmount() + 2.0F);
        }
        if (projectile && isHolding(player, ModItems.DREAM_BLOOM_BOW.get())
                && target instanceof net.minecraft.world.entity.monster.Enemy) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    100, 1, true, false, true));
        }
    }

    private static void markLongbowTarget(ServerPlayer player, LivingEntity target) {
        var data = target.getPersistentData();
        String suffix = player.getUUID().toString();
        data.putBoolean(LONGBOW_MARK_PREFIX + suffix, true);
        data.putLong(LONGBOW_MARK_TIME_PREFIX + suffix, player.level().getGameTime());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
    }

    private static boolean consumeLongbowMark(ServerPlayer player, LivingEntity target) {
        var data = target.getPersistentData();
        String suffix = player.getUUID().toString();
        if (!data.getBoolean(LONGBOW_MARK_PREFIX + suffix)) {
            return false;
        }
        long age = player.level().getGameTime() - data.getLong(LONGBOW_MARK_TIME_PREFIX + suffix);
        data.remove(LONGBOW_MARK_PREFIX + suffix);
        data.remove(LONGBOW_MARK_TIME_PREFIX + suffix);
        return age <= 200;
    }

    private static boolean isHolding(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    /** 关键机关方块保护:生存模式下不可破坏(管理员/创造除外)。 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!ServerConfig.PROTECT_CRITICAL_BLOCKS.get()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isCreative() || player.hasPermissions(2)) {
            return;
        }
        // 关键机关方块统一走 critical_blocks tag(新增关键方块只改 JSON,不再漏代码清单)
        if (event.getState().is(ModBlocks.CRITICAL_BLOCKS_TAG)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.block.protected"), true);
            return;
        }
        // 晶歌守谱者存活时,场地音柱受护:防止挖掉破坏调音谜题(野外音柱不受影响)
        if (event.getState().is(ModBlocks.CRYSTAL_SONG_CLUSTER.get())
                && player.level() instanceof ServerLevel songLevel
                && findTunableSongkeeper(songLevel, event.getPos()) != null) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.crystal_songkeeper.guarded"), true);
            return;
        }
        // Boss 场地封锁:守护者存活时场地内禁止挖掘——堵死"挖洞进场/挖墙取宝"绕过
        if (player.level() instanceof ServerLevel serverLevel
                && findNearbyAliveBoss(serverLevel, event.getPos()) != null) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.boss.area_sealed"), true);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            applyEchoToolBreakReward(serverPlayer, event.getState());
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isHolding(player, ModItems.ECHO_FISHING_ROD.get())
                || !(player.level() instanceof ServerLevel level)
                || !player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            return;
        }
        event.damageRodBy(Math.max(0, event.getRodDamage() - 1));
        if (level.random.nextFloat() < 0.35F) {
            ItemStack reward = level.random.nextBoolean()
                    ? new ItemStack(ModItems.TIDE_SALT.get(), 2)
                    : new ItemStack(ModItems.PEARL_CLAM.get());
            give(player, reward);
        }
    }

    private static float applyEchoToolBreakSpeed(ServerPlayer player,
                                                net.minecraft.world.level.block.state.BlockState state,
                                                float speed) {
        ItemStack tool = player.getMainHandItem();
        if (tool.is(ModItems.ECHO_PICKAXE.get()) && state.is(ModBlocks.RESOURCE_BLOCKS_TAG)) {
            return speed * 1.25F;
        }
        if (tool.is(ModItems.ECHO_SHOVEL.get()) && isEchoLooseBlock(state)) {
            return speed * 1.45F;
        }
        if (tool.is(ModItems.ECHO_HATCHET.get()) && isEchoLog(state)) {
            return speed * 1.35F;
        }
        if (tool.is(ModItems.ECHO_HOE.get()) && isEchoPlant(state)) {
            return speed * 1.35F;
        }
        return speed;
    }

    private static void applyEchoToolBreakReward(ServerPlayer player,
                                                 net.minecraft.world.level.block.state.BlockState state) {
        ServerLevel level = player.serverLevel();
        ItemStack tool = player.getMainHandItem();
        if (tool.is(ModItems.ECHO_SHOVEL.get()) && isEchoLooseBlock(state)
                && level.random.nextFloat() < 0.20F) {
            give(player, new ItemStack(ModItems.ECHO_DUST.get()));
        } else if (tool.is(ModItems.ECHO_HOE.get()) && isEchoPlant(state)
                && level.random.nextFloat() < 0.25F) {
            give(player, plantByproduct(state));
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
                || state.is(ModBlocks.WIND_ERODED_RUBBLE.get())
                || state.is(ModBlocks.TIDE_SMOOTH_STONE.get());
    }

    private static boolean isEchoLog(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.ECHO_LOG.get())
                || state.is(ModBlocks.WHISPERING_LOG.get())
                || state.is(ModBlocks.TIDEWOOD_LOG.get());
    }

    private static boolean isEchoPlant(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.DREAM_FLOWER.get())
                || state.is(ModBlocks.MUFFLE_MOSS.get())
                || state.is(ModBlocks.CRYSTAL_SONG_CLUSTER.get());
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

    /** Boss 场地封锁半径(覆盖整座神殿/竞技场)。 */
    private static final double BOSS_SEAL_RADIUS = 24.0D;

    /** 找出该位置附近仍存活的主线 Boss(分身/假身不算)。 */
    private static LivingEntity findNearbyAliveBoss(ServerLevel level, net.minecraft.core.BlockPos pos) {
        AABB area = new AABB(pos).inflate(BOSS_SEAL_RADIUS);
        for (var colossus : level.getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus.class, area,
                LivingEntity::isAlive)) {
            return colossus;
        }
        for (var watcher : level.getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher.class, area,
                boss -> boss.isAlive() && !boss.isClone())) {
            return watcher;
        }
        for (var guardian : level.getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian.class, area,
                boss -> boss.isAlive() && !boss.isIllusion())) {
            return guardian;
        }
        return null;
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

    /** 就近(TUNE_RANGE 内)存活的晶歌守谱者,用于调音路由与场地音柱保护。 */
    private static cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper findTunableSongkeeper(
            ServerLevel level, net.minecraft.core.BlockPos pos) {
        AABB area = new AABB(pos).inflate(cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.TUNE_RANGE);
        for (var keeper : level.getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.CrystalSongkeeper.class, area,
                LivingEntity::isAlive)) {
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
