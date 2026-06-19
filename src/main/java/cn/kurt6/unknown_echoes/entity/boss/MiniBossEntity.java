package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.block.boss.MiniBossSpawnerBlockEntity;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Mini Boss 公共框架(设计文档 10.4.1 / V0.6A):强化守护者 / 区域守护者共用的战斗骨架。
 * 与主线 Boss 的差异:不发放基础能力、场地更小、可重复挑战(重复击败只给普通奖励)。
 *
 * 框架职责(子类不要各自复制):
 * - 未破防双保险:{@link #isDamageGuarded()} 为 true 时,伤害 = min(伤害 x 乘数, 上限),
 *   沿用 ServerConfig 的 BOSS_UNBROKEN_* 两项(设计红线 #4)。
 * - 参与者个人结算:伤害过 / 战斗期间在场即登记;死亡时只对"仍在结算半径内"的参与者
 *   逐人调用 {@link #grantSettlement},首杀与重复击败由个人 Boss 击败记录区分(红线 #3)。
 * - 场地重开:死亡时通知绑定的 {@link MiniBossSpawnerBlockEntity} 启动重开计时
 *   (MINIBOSS_ARENA_REOPEN / _TICKS,服务器可配置)。
 * - 常规 Boss 体征:Boss 血条、不自然消失、不可拴绳、脱战回血、场地锚点限制。
 */
public abstract class MiniBossEntity extends Monster implements GeoEntity {
    private static final String PENDING_REWARD_PREFIX = "pending_miniboss_reward|";

    protected final ServerBossEvent bossEvent;
    protected final Set<UUID> participants = new HashSet<>();

    /** 场地锚点:出生点。游荡与机制落点都锁在 arenaRadius 内。 */
    protected BlockPos homePos = null;
    /** 绑定的重开计时器(MiniBossSpawnerBlock)位置,由 spawner 生成时写入。 */
    protected BlockPos spawnerPos = null;

    private int guardedHintCooldown = 0;
    private int outOfCombatTicks = 0;

    protected MiniBossEntity(EntityType<? extends Monster> type, Level level,
                             BossEvent.BossBarColor barColor) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), barColor,
                BossEvent.BossBarOverlay.PROGRESS);
    }

    // ---- 子类契约 ----

    /** 个人击败记录用的 Boss ID(EchoAbilityManager.markBossDefeated)。 */
    protected abstract ResourceLocation minibossId();

    /** 场地半径:游荡限制与参与登记基于它。 */
    protected abstract int arenaRadius();

    /** 当前是否处于"未破防"机制保护(true = 伤害乘数 + 单次上限生效)。 */
    protected abstract boolean isDamageGuarded();

    /**
     * 个人结算:对每位场地内参与者调用一次。
     * firstKill = 该玩家第一次击败本守护者;重复击败只给普通奖励与研究进度(10.4.1)。
     */
    protected abstract void grantSettlement(ServerPlayer player, boolean firstKill);

    /** 未破防时打在身上的含蓄提示 lang 键;返回 null 则不提示。 */
    protected String guardedHintKey() {
        return null;
    }

    /** 结算半径:覆盖场地外缘一圈,死亡瞬间在范围内的参与者才拿奖励(红线 #4)。 */
    protected double settlementRadius() {
        return arenaRadius() + 12;
    }

    // ---- 公共逻辑 ----

    public void bindSpawner(BlockPos spawnerPos) {
        this.spawnerPos = spawnerPos;
    }

    public BlockPos arenaAnchor() {
        return this.homePos != null ? this.homePos : this.blockPosition();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, arenaRadius());
        }
        if (this.guardedHintCooldown > 0) {
            this.guardedHintCooldown--;
        }
        // 参与者登记:战斗期间(有目标)每秒登记结算半径内玩家
        if (this.tickCount % 20 == 0 && this.getTarget() != null) {
            double radius = settlementRadius();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) <= radius * radius) {
                    this.participants.add(player.getUUID());
                }
            }
        }
        this.tickOutOfCombatRegen();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** 脱战回血:无存活目标持续 5 秒后每秒回 2% 最大生命,减员消耗战不可行。 */
    private void tickOutOfCombatRegen() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.outOfCombatTicks = 0;
            return;
        }
        if (++this.outOfCombatTicks > 100 && this.outOfCombatTicks % 20 == 0
                && this.getHealth() < this.getMaxHealth()) {
            this.heal(this.getMaxHealth() * 0.02F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 守护者之间不互相伤害(召唤物 / 环境怪误伤免疫)
        if (!this.level().isClientSide && source.getEntity() instanceof Enemy) {
            return false;
        }
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer attacker) {
            this.participants.add(attacker.getUUID());
        }
        // 未破防双保险:乘数 + 单次上限,沿用主线 Boss 的 ServerConfig 两项(红线 #4)
        if (!this.level().isClientSide && this.isDamageGuarded()
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float multiplier = ServerConfig.BOSS_UNBROKEN_DAMAGE_MULTIPLIER.get().floatValue();
            float cap = ServerConfig.BOSS_UNBROKEN_MAX_DAMAGE.get().floatValue();
            amount = Math.min(amount * multiplier, cap);
            String hintKey = guardedHintKey();
            if (hintKey != null && this.guardedHintCooldown <= 0
                    && source.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(hintKey), true);
                this.guardedHintCooldown = 40;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 个人结算:首杀写击败记录 + 完整奖励;重复击败只给普通奖励(grantSettlement 自行区分)
        double radius = settlementRadius();
        for (ServerPlayer player : serverLevel.players()) {
            if (this.participants.contains(player.getUUID())
                    && player.distanceToSqr(this) <= radius * radius) {
                boolean firstKill = !EchoAbilityManager.hasDefeatedBoss(player, minibossId());
                if (firstKill) {
                    EchoAbilityManager.markBossDefeated(player, minibossId());
                }
                this.grantSettlement(player, firstKill);
            }
        }
        // 场地重开:通知绑定的计时器(没有绑定时为一次性生成,保持已有行为)
        if (this.spawnerPos != null
                && serverLevel.getBlockEntity(this.spawnerPos) instanceof MiniBossSpawnerBlockEntity spawner) {
            spawner.onBossDefeated();
        }
    }

    /** 结算发放物品:优先进背包,满了落在玩家脚下(普通材料允许掉落,红线 #2 只针对关键奖励)。 */
    protected static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.level().addFreshEntity(
                    new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
        }
    }

    protected void giveFirstKillItem(ServerPlayer player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (canFitEntireStack(player, copy) && player.getInventory().add(copy)) {
            return;
        }
        rememberPendingFirstKillItem(player, stack);
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.miniboss.reward_pending", stack.getHoverName()), true);
    }

    public static void retryPendingFirstKillRewards(ServerPlayer player) {
        Set<String> mechanisms = EchoAbilityManager.getData(player).getActivatedMechanisms();
        Iterator<String> iterator = mechanisms.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            PendingReward reward = parsePendingReward(key);
            if (reward == null) {
                continue;
            }
            ItemStack stack = new ItemStack(reward.item(), reward.count());
            if (canFitEntireStack(player, stack) && player.getInventory().add(stack)) {
                iterator.remove();
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.miniboss.reward_claimed", stack.getHoverName()), true);
            }
        }
    }

    private void rememberPendingFirstKillItem(ServerPlayer player, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        EchoAbilityManager.getData(player).getActivatedMechanisms().add(
                PENDING_REWARD_PREFIX + minibossId() + "|" + itemId + "|" + stack.getCount());
    }

    private static PendingReward parsePendingReward(String key) {
        if (!key.startsWith(PENDING_REWARD_PREFIX)) {
            return null;
        }
        String[] parts = key.substring(PENDING_REWARD_PREFIX.length()).split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(parts[1]);
        int count;
        try {
            count = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (itemId == null || count <= 0) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? null : new PendingReward(item, count);
    }

    private static boolean canFitEntireStack(ServerPlayer player, ItemStack stack) {
        int remaining = stack.getCount();
        int max = stack.getMaxStackSize();
        for (ItemStack slot : player.getInventory().items) {
            if (slot.isEmpty()) {
                remaining -= max;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                remaining -= Math.max(0, max - slot.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private record PendingReward(Item item, int count) {
    }

    // ---- Boss 体征 ----

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
        // 守护者不自然消失;场地重开由 MiniBossSpawnerBlockEntity 负责
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

    // ---- NBT ----

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
        this.participants.clear();
        for (Tag entry : tag.getList("Participants", Tag.TAG_INT_ARRAY)) {
            this.participants.add(NbtUtils.loadUUID(entry));
        }
        int[] home = tag.getIntArray("HomePos");
        if (home.length == 3) {
            this.homePos = new BlockPos(home[0], home[1], home[2]);
            this.restrictTo(this.homePos, arenaRadius());
        }
        int[] spawner = tag.getIntArray("SpawnerPos");
        if (spawner.length == 3) {
            this.spawnerPos = new BlockPos(spawner[0], spawner[1], spawner[2]);
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
