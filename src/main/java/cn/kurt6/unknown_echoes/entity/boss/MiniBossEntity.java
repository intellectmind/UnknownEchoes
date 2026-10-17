package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.fx.BossLifecycleFx;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlock;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.entity.EntityHitboxProfiles;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class MiniBossEntity extends Monster implements GeoEntity, BossDamageGuard, BossMusicSource, BossDeathSettlement {
    private static final String PENDING_REWARD_PREFIX = "pending_miniboss_reward|";
    private static final int MAX_PENDING_REWARD_COUNT = 4096;
    protected final ServerBossEvent bossEvent;
    protected final Set<UUID> participants = new HashSet<>();

    /** 场地锚点:出生点。游荡与机制落点都锁在 arenaRadius 内。 */
    protected BlockPos homePos = null;
    /** 绑定的重开计时器(MiniBossSpawnerBlock)位置,由 spawner 生成时写入。 */
    protected BlockPos spawnerPos = null;

    private int guardedHintCooldown = 0;
    private int outOfCombatTicks = 0;
    private boolean spawnerBindingPaused = false;
    private boolean handlingSpawnerCleanup;
    private boolean handlingDeath = false;
    private DeathContext deathContext = null;

    private record DeathContext(ServerLevel level, Vec3 position, AABB bounds, BlockPos spawnerPos,
                                UUID bossUuid, ResourceLocation bossId, double radius, Set<UUID> participants) {}


    protected MiniBossEntity(EntityType<? extends Monster> type, Level level,
                             BossEvent.BossBarColor barColor) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), barColor,
                BossEvent.BossBarOverlay.PROGRESS);
    }

    /** 让大型小 Boss 使用贴合主体的 X/Z 长方形碰撞箱；未配置 profile 的实体保持注册尺寸。 */
    @Override
    protected AABB makeBoundingBox() {
        return EntityHitboxProfiles.make(this);
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        EntityHitboxProfiles.refresh(this);
    }

    /** 个人击败记录用的 Boss ID(EchoAbilityManager.markBossDefeated)。 */
    protected abstract ResourceLocation minibossId();

    /** 场地半径:游荡限制与参与登记基于它。 */
    protected abstract int arenaRadius();

    /** 需要整只身体位于水柱中的水下 MiniBoss 覆盖此方法。 */
    public boolean requiresWaterSpawn() {
        return false;
    }

    /** 当前是否处于"未破防"机制保护(true = 伤害乘数 + 单次上限生效)。 */
    protected abstract boolean isDamageGuarded();

    /** 本场唯一掉落。firstKill=true 时使用首杀包，否则使用重复挑战包。 */
    protected abstract void grantSettlement(boolean firstKill);

    /** 未破防时打在身上的含蓄提示 lang 键;返回 null 则不提示。 */
    protected String guardedHintKey() {
        return null;
    }

    /** 本只 Mini Boss 的演出配色。形状走 MiniBossFx，不要在子类里拼裸粒子。 */
    protected final MiniBossFxTheme.Theme fxTheme() {
        return MiniBossFxTheme.of(this.minibossId().getPath());
    }

    /**
     * 近战命中与未破防反馈共用的主题色。
     *
     * 从 fxTheme 的 core 换算，不要再写一张手填的 id -> 色号表：两张表会各自漂移，
     * 而屏幕脉冲与粒子同屏出现，颜色不一致时玩家读成两只怪。
     */
    protected int combatImpactColor() {
        return MiniBossFxTheme.packedCore(this.fxTheme());
    }

    protected void tickMechanicGuidance(ServerLevel level) {
    }

    /** 结算半径:覆盖场地外缘一圈,死亡瞬间在范围内的参与者才拿奖励。 */
    protected double settlementRadius() {
        return arenaRadius() + ServerConfig.MINIBOSS_SETTLEMENT_RADIUS_BONUS.get();
    }

    public void bindSpawner(BlockPos spawnerPos) {
        this.spawnerPos = spawnerPos;
    }

    public BlockPos arenaAnchor() {
        return this.homePos != null ? this.homePos : this.blockPosition();
    }

    @Override
    public void aiStep() {
        if (this.shouldStopBossAi()) {
            return;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.spawnerBindingPaused = this.tickSpawnerBinding(serverLevel);
            if (this.spawnerBindingPaused) {
                return;
            }
        } else {
            this.spawnerBindingPaused = false;
        }
        super.aiStep();
        if (this.shouldStopBossAi()) {
            return;
        }
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.isAlive() && this.tickCount % 20 == 0
                && !this.retrySpawnerRegistration(serverLevel)) {
            this.discard();
            return;
        }
        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, arenaRadius());
        }
        if (this.guardedHintCooldown > 0) {
            this.guardedHintCooldown--;
        }
        List<ServerPlayer> nearbyPlayers = null;

        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            nearbyPlayers = nearbySettlementPlayers(serverLevel);
            for (ServerPlayer player : nearbyPlayers) {
                BossNbtData.addParticipant(this.participants, player.getUUID());
            }
        }
        int guidanceInterval = ServerConfig.MINIBOSS_GUIDANCE_INTERVAL_TICKS.get();
        if (this.tickCount % guidanceInterval == 0 && this.isDamageGuarded()) {
            List<ServerPlayer> guidancePlayers =
                    nearbyPlayers == null ? nearbySettlementPlayers(serverLevel) : nearbyPlayers;
            if (this.hasNearbyGuidanceViewer(guidancePlayers)) {
                this.tickMechanicGuidance(serverLevel);
            }
        }
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    private List<ServerPlayer> nearbySettlementPlayers(ServerLevel level) {
        double radius = settlementRadius();
        AABB bounds = this.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(ServerPlayer.class, bounds,
                player -> !player.isSpectator() && player.distanceToSqr(this) <= radius * radius);
    }

    private boolean hasNearbyGuidanceViewer(List<ServerPlayer> nearbyPlayers) {
        for (ServerPlayer player : nearbyPlayers) {
            if (player.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** 脱战回血:判据与配置口径由 OutOfCombatRegen 统一持有。 */
    private void tickOutOfCombatRegen() {
        this.outOfCombatTicks = OutOfCombatRegen.tick(this, this.outOfCombatTicks);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.isSpawnerCombatBlocked(serverLevel)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        if (!this.level().isClientSide && source.getEntity() instanceof Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker
                && !attacker.isSpectator()) {
            BossNbtData.addParticipant(this.participants, attacker.getUUID());
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level() instanceof ServerLevel serverLevel
                && this.isSpawnerCombatBlocked(serverLevel)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (target instanceof LivingEntity victim) {
                MiniBossFx.hitMark(serverLevel, victim, this.fxTheme());
            }
            BossFx.cinematicImpact(serverLevel, impact, 18.0D, 0.16F, 3,
                    this.combatImpactColor());
        }
        return hit;
    }

    @Override
    public boolean isMechanicDamageGuarded() {
        return this.isDamageGuarded();
    }

    @Override
    public boolean shouldGuardDamage(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void onMechanicDamageGuarded(DamageSource source) {
        String hintKey = guardedHintKey();
        if (hintKey != null && this.guardedHintCooldown <= 0
                && source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(hintKey), true);
            if (this.level() instanceof ServerLevel serverLevel) {
                BossFx.cinematicImpact(serverLevel,
                        this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D),
                        18.0D, 0.12F, 3, this.combatImpactColor());
            }
            this.guardedHintCooldown = 40;
        }
    }

    @Override
    public boolean isSettlingDeath() {
        return this.handlingDeath && this.dead;
    }

    @Override
    public void die(DamageSource source) {
        if (this.handlingDeath || this.dead) {
            return;
        }
        this.handlingDeath = true;
        try {
            if (this.discardOnDeath()) {
                this.discard();
                return;
            }
            if (!(this.level() instanceof ServerLevel level)) {
                super.die(source);
                return;
            }
            DeathContext context = new DeathContext(level, this.position(), this.getBoundingBox(),
                    this.spawnerPos, this.getUUID(), this.minibossId(), this.settlementRadius(),
                    Set.copyOf(this.participants));
            try {
                super.die(source);
            } finally {
                if (this.dead) {
                    this.deathContext = context;
                    settleMiniBossDeath(context);
                }
            }
        } finally {
            this.handlingDeath = false;
        }
    }

    private void settleMiniBossDeath(DeathContext context) {
        try {
            boolean hasEligibleParticipant = false;
            boolean firstKillDrop = false;
            for (ServerPlayer player : nearbyDeathPlayers(context)) {
                if (context.participants().contains(player.getUUID())
                        && isDeathParticipant(context, player)) {
                    boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player, context.bossId());
                    hasEligibleParticipant = true;
                    firstKillDrop |= firstKill;
                    if (firstKill) {
                        runDeathStep("personal progress", () -> {
                            if (!EchoAbilityManager.markBossDefeated(player, context.bossId())
                                    && isDeathParticipant(context, player)) {
                                player.sendSystemMessage(Component.translatable(
                                        "message.unknown_echoes.progress_record.full"));
                            }
                        });
                    }
                }
            }
            if (hasEligibleParticipant) {
                this.grantSettlement(firstKillDrop);
            }
        } finally {
            if (context.spawnerPos() != null) {
                runDeathStep("spawner settlement", () -> MiniBossSpawnerBlockEntity.onBossDefeated(
                        context.level(), context.spawnerPos(), context.bossUuid()));
            }
        }
        runDeathStep("death effects", () -> {
            if (isCurrentDeathPresentation(context.level(), context.position())) {
                BossLifecycleFx.death(context.level(), context.position(), this.getBbWidth(), this.getBbHeight(),
                        this.combatImpactColor());
            }
        });
        runDeathStep("death cleanup", () -> onDeathCommitted(context.level(), context.position()));
    }

    public final void playSummonEffects(ServerLevel level) {
        BossLifecycleFx.summon(level, this.position(), this.getBbWidth(), this.getBbHeight(), this.fxTheme().trace(1));
    }

    /** 子类特殊死亡收尾统一在一次已提交结算之后执行。 */
    protected void onDeathCommitted(ServerLevel level, Vec3 deathPos) {
    }

    /** 无战利品的镜像直接移除，仍受共用死亡重入保护。 */
    protected boolean discardOnDeath() {
        return false;
    }

    final void dropSettlementItem(ItemStack stack) {
        DeathContext context = this.deathContext;
        if (context == null) {
            BossMaterialRewards.drop(this, stack);
            return;
        }
        runDeathStep("material", () -> BossMaterialRewards.drop(context.level(),
                context.position().x, context.position().y + 0.5D, context.position().z, stack));
    }

    private List<ServerPlayer> nearbyDeathPlayers(DeathContext context) {
        return context.level().getEntitiesOfClass(ServerPlayer.class,
                context.bounds().inflate(context.radius()), player -> isDeathParticipant(context, player));
    }

    private boolean isDeathParticipant(DeathContext context, ServerPlayer player) {
        return player.level() == context.level() && player.isAlive() && !player.isSpectator()
                && !player.isRemoved() && !player.hasDisconnected()
                && player.position().distanceToSqr(context.position()) <= context.radius() * context.radius();
    }

    protected final void runDeathStep(String step, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            cn.kurt6.unknown_echoes.UnknownEchoes.LOGGER.error("Mini boss death {} failed for {}", step, this.getUUID(), failure);
        }
    }

    protected final boolean isCurrentDeathPresentation(ServerLevel level, Vec3 deathPos) {
        return this.dead && !this.isRemoved() && this.level() == level
                && level.getEntity(this.getUUID()) == this
                && this.position().distanceToSqr(deathPos) < 0.0001D;
    }

    protected final ServerLevel deathCleanupLevel() {
        return this.deathContext != null ? this.deathContext.level()
                : this.level() instanceof ServerLevel level ? level : null;
    }

    protected final AABB deathCleanupBounds() {
        return this.deathContext != null ? this.deathContext.bounds() : this.getBoundingBox();
    }

    protected final boolean hasCommittedDeath() {
        return this.deathContext != null;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level() instanceof ServerLevel serverLevel
                && !this.retrySpawnerRegistration(serverLevel)) {
            this.discard();
        }
    }

    private boolean retrySpawnerRegistration(ServerLevel level) {
        return this.spawnerPos == null || MiniBossSpawnerBlockEntity.bindingState(level, this.spawnerPos, this)
                != MiniBossSpawnerBlockEntity.BindingState.INVALID;
    }

    private boolean tickSpawnerBinding(ServerLevel level) {
        BlockPos boundSpawnerPos = this.spawnerPos;
        if (boundSpawnerPos == null) {
            return false;
        }
        MiniBossSpawnerBlockEntity.BindingState state =
                MiniBossSpawnerBlockEntity.bindingState(level, boundSpawnerPos, this);
        if (state == MiniBossSpawnerBlockEntity.BindingState.INTERRUPTED) {
            return true;
        }
        if (state == MiniBossSpawnerBlockEntity.BindingState.UNAVAILABLE) {
            BossBindingPause.pause(level, this, () -> !this.shouldStopBossAi(),
                    () -> boundSpawnerPos.equals(this.spawnerPos)
                            && MiniBossSpawnerBlockEntity.bindingState(level, boundSpawnerPos, this)
                            == MiniBossSpawnerBlockEntity.BindingState.UNAVAILABLE);
            return true;
        }
        if (state == MiniBossSpawnerBlockEntity.BindingState.INVALID) {
            this.discardInvalidSpawnerBinding(level, boundSpawnerPos);
            return true;
        }
        return false;
    }

    private boolean isCurrentSpawnerBindingSource(ServerLevel level, BlockPos boundSpawnerPos) {
        return this.level() == level && !this.shouldStopBossAi() && !this.isRemoved()
                && level.getEntity(this.getUUID()) == this && boundSpawnerPos.equals(this.spawnerPos);
    }

    private void discardInvalidSpawnerBinding(ServerLevel level, BlockPos boundSpawnerPos) {
        if (this.handlingSpawnerCleanup) {
            return;
        }
        this.handlingSpawnerCleanup = true;
        try {
            var chunk = level.getChunkSource().getChunkNow(boundSpawnerPos.getX() >> 4, boundSpawnerPos.getZ() >> 4);
            if (!this.isCurrentSpawnerBindingSource(level, boundSpawnerPos) || chunk == null
                    || MiniBossSpawnerBlockEntity.bindingState(level, boundSpawnerPos, this)
                    != MiniBossSpawnerBlockEntity.BindingState.INVALID) {
                return;
            }
            var state = chunk.getBlockState(boundSpawnerPos);
            var spawner = chunk.getBlockEntity(boundSpawnerPos);
            MiniBossSpawnerBlockEntity.onBossDestroyed(level, boundSpawnerPos, this.getUUID());
            var currentChunk = level.getChunkSource().getChunkNow(boundSpawnerPos.getX() >> 4, boundSpawnerPos.getZ() >> 4);
            if (!this.isCurrentSpawnerBindingSource(level, boundSpawnerPos) || currentChunk == null
                    || currentChunk.getBlockState(boundSpawnerPos) != state
                    || currentChunk.getBlockEntity(boundSpawnerPos) != spawner
                    || MiniBossSpawnerBlockEntity.bindingState(level, boundSpawnerPos, this)
                    != MiniBossSpawnerBlockEntity.BindingState.INVALID) {
                return;
            }
            this.discard();
        } finally {
            this.handlingSpawnerCleanup = false;
        }
    }

    private boolean isSpawnerCombatBlocked(ServerLevel level) {
        return this.spawnerPos != null
                && MiniBossSpawnerBlockEntity.bindingState(level, this.spawnerPos, this)
                != MiniBossSpawnerBlockEntity.BindingState.VALID;
    }

    /** Death callbacks and the remaining corpse lifetime must not run combat or binding cleanup. */
    protected final boolean shouldStopBossAi() {
        return this.handlingDeath || this.dead || !this.isAlive();
    }

    protected final boolean isSpawnerBindingPaused() {
        return this.spawnerBindingPaused || this.shouldStopBossAi();
    }

    public void clearSpawnerPresence(ServerLevel level) {
        if (this.spawnerPos != null) {
            MiniBossSpawnerBlockEntity.onBossDestroyed(level, this.spawnerPos, this.getUUID());
        }
    }

    public boolean hasSpawnerBinding() {
        return this.spawnerPos != null;
    }

    public BlockPos spawnerPos() {
        return this.spawnerPos;
    }

    public static void loadPendingFirstKillRewards(ServerPlayer player) {
        Set<String> mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        Iterator<String> iterator = mechanisms.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.startsWith(PENDING_REWARD_PREFIX)) {
                continue;
            }
            PendingReward reward = parsePendingReward(key);
            if (reward == null) {
                iterator.remove();
                continue;
            }
            ItemStack stack = new ItemStack(reward.item(), reward.count());
            if (reward.pageId() >= 0) {
                stack.set(ModDataComponents.PAGE_ID.get(), reward.pageId());
            }
            BossRewardDropData.queueForRetry(player.serverLevel(),
                    player.getX(), player.getY(), player.getZ(), stack);
            iterator.remove();
        }
    }

    private static PendingReward parsePendingReward(String key) {
        if (!key.startsWith(PENDING_REWARD_PREFIX)) {
            return null;
        }
        String[] parts = key.substring(PENDING_REWARD_PREFIX.length()).split("\\|", 4);
        if (parts.length < 3) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(parts[1]);
        int count;
        int pageId = -1;
        try {
            count = Integer.parseInt(parts[2]);
            if (parts.length == 4) {
                pageId = Integer.parseInt(parts[3]);
            } else if (itemId != null
                    && itemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.ANCIENT_PAGE.get()))) {
                pageId = legacyPageId(parts[0]);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (itemId == null || count <= 0 || count > MAX_PENDING_REWARD_COUNT
                || pageId < -1 || pageId > 999) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        return item == null || item == Items.AIR ? null : new PendingReward(item, count, pageId);
    }

    private static int legacyPageId(String bossId) {
        ResourceLocation id = ResourceLocation.tryParse(bossId);
        if (id == null) {
            return -1;
        }
        return switch (id.getPath()) {
            case "storm_weaver" -> 23;
            case "tide_lantern_keeper" -> 43;
            case "mirror_dust_butler" -> 56;
            case "silent_priest" -> 70;
            case "crystal_songkeeper" -> 80;
            case "dream_bloom_keeper" -> 86;
            case "broken_bell_keeper" -> 92;
            case "lost_recorder_chief" -> 104;
            case "inverted_gatekeeper" -> 146;
            case "ash_scribe" -> 167;
            default -> -1;
        };
    }

    private record PendingReward(Item item, int count, int pageId) {
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {

    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag participantsTag = new ListTag();
        for (UUID uuid : this.participants) {
            participantsTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Participants", participantsTag);
        if (this.homePos != null) {
            tag.putIntArray("HomePos",
                    new int[]{this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
        }
        if (this.spawnerPos != null) {
            tag.putIntArray("SpawnerPos",
                    new int[]{this.spawnerPos.getX(), this.spawnerPos.getY(), this.spawnerPos.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BossNbtData.loadUuids(tag, "Participants", this.participants, BossNbtData.MAX_PARTICIPANTS);
        BlockPos entityPos = this.blockPosition();
        this.homePos = BossNbtData.loadPositionNear(tag, "HomePos", entityPos,
                arenaRadius() * 3, arenaRadius() * 2);
        if (this.homePos != null) {
            this.restrictTo(this.homePos, arenaRadius());
        }
        this.spawnerPos = BossNbtData.loadPositionNear(tag, "SpawnerPos",
                this.homePos != null ? this.homePos : entityPos, arenaRadius() * 3, 32);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
