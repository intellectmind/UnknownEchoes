package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.network.ArtifactSyncPayload;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器系统统一入口(仿 EchoAbilityManager):读写 ArtifactData + 同步客户端。
 * 领取/复领/升级/调谐/能量/冷却全部经由本类,禁止物品类各写一套(12.2 技术边界)。
 * 所有写操作只接受 ServerPlayer;客户端永远只收展示字段。
 */
public class ArtifactManager {

    /** 改调谐消耗:回响粉尘 ×2(守护者结算通用材料;首次选择词条免费,12.4)。 */
    public static final int TUNING_COST_DUST = 2;

    public static ArtifactData getData(Player player) {
        return player.getData(ModAttachments.ARTIFACT_DATA);
    }

    /** 服务器可单独禁用某件神器(12.5 边界清单)。 */
    public static boolean isEnabled(ArtifactType type) {
        return !ServerConfig.DISABLED_ARTIFACTS.get().contains(type.getId());
    }

    // ---- 凭据(物品只是凭据,功能数据一律读玩家数据) ----

    /** 制作凭据物品:组件存"神器 id + 凭据序号",与玩家数据比对后才可用。 */
    public static ItemStack makeCredential(ArtifactType type, int serial) {
        ItemStack stack = new ItemStack(type.getCredentialItem());
        stack.set(ModDataComponents.ARTIFACT_ID.get(), type.getId());
        stack.set(ModDataComponents.CREDENTIAL_SERIAL.get(), serial);
        return stack;
    }

    /** 凭据校验:物品序号必须等于玩家数据中的当前序号(旧凭据/他人凭据均不通过)。 */
    public static boolean validateCredential(ServerPlayer player, ArtifactType type, ItemStack stack) {
        if (!getData(player).isClaimed(type)) {
            return false;
        }
        String id = stack.get(ModDataComponents.ARTIFACT_ID.get());
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        return type.getId().equals(id) && serial != null
                && serial == getData(player).getSerial(type);
    }

    // ---- 领取与复领(记录台,12.2) ----

    /** 领取资格判定(只读)。资格本体是个人探索进度,永不作为掉落物或材料消耗(红线 #2)。 */
    public static boolean hasClaimRequirement(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case STORM_COMPASS -> EchoAbilityManager.hasDefeatedBoss(player,
                    UnknownEchoes.id("storm_weaver"))
                    || EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)
                    && hasStructure(player, "sky_observatory");
            case TIDE_LANTERN -> EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                    && (hasStructure(player, "tide_lighthouse_reef")
                    || EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("tide_lantern_keeper")));
            case ECHO_LENS -> EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)
                    && (hasStructure(player, "mirror_dust_cloister")
                    || EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("mirror_dust_butler")));
        };
    }

    /** 记录台交互:未领取→校验资格、消耗部件、写数据、发凭据;已领取→复领(作废旧序号)。 */
    public static boolean claimOrReissue(ServerPlayer player, ArtifactType type) {
        if (!isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return false;
        }
        ArtifactData data = getData(player);
        if (data.isClaimed(type)) {
            if (!hasCredentialSpace(player)) {
                actionbar(player, "message.unknown_echoes.artifact.need_space");
                return false;
            }
            // 复领:资格已落账,凭据丢失/被抢都不影响进度;旧凭据从此失效
            data.nextSerial(type);
            giveCredential(player, type);
            actionbar(player, "message.unknown_echoes.artifact.reissued");
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
            syncToClient(player);
            return true;
        }
        if (!hasClaimRequirement(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.not_eligible");
            return false;
        }
        if (!hasCredentialSpace(player)) {
            actionbar(player, "message.unknown_echoes.artifact.need_space");
            return false;
        }
        data.setLevel(type, eligibleLevel(player, type));
        data.nextSerial(type);
        giveCredential(player, type);
        player.sendSystemMessage(Component.translatable("message.unknown_echoes.artifact.claimed",
                Component.translatable("artifact.unknown_echoes." + type.getId())));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
        syncToClient(player);
        return true;
    }

    // ---- 升级与调谐(升级台,12.4) ----

    /** 升级台:按个人探索进度结算可达等级。等级写玩家数据,不写物品。 */
    public static boolean upgrade(ServerPlayer player, ArtifactType type) {
        if (!isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return false;
        }
        ArtifactData data = getData(player);
        if (!data.isClaimed(type)) {
            actionbar(player, "message.unknown_echoes.artifact.not_claimed");
            return false;
        }
        int level = data.getLevel(type);
        if (level >= ArtifactType.MAX_LEVEL) {
            actionbar(player, "message.unknown_echoes.artifact.max_level");
            return false;
        }
        int target = eligibleLevel(player, type);
        if (target <= level) {
            actionbar(player, "message.unknown_echoes.artifact.need_progress");
            return false;
        }
        data.setLevel(type, target);
        player.sendSystemMessage(Component.translatable("message.unknown_echoes.artifact.upgraded",
                Component.translatable("artifact.unknown_echoes." + type.getId()), target));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
        syncToClient(player);
        return true;
    }

    private static int eligibleLevel(ServerPlayer player, ArtifactType type) {
        if (!hasClaimRequirement(player, type)) {
            return 0;
        }
        int level = 1;
        if (hasFirstUpgradeProgress(player, type)) {
            level = 2;
        }
        if (hasFinalUpgradeProgress(player, type)) {
            level = 3;
        }
        return level;
    }

    private static boolean hasFirstUpgradeProgress(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case STORM_COMPASS -> countStructures(player, "sky_observatory",
                    "wind_eroded_tower", "sky_rift") >= 3
                    && countPages(player, 17, 24) >= 6;
            case TIDE_LANTERN -> countMechanisms(player, "tide_pillar:") >= 2
                    && countPages(player, 42, 51) >= 6;
            case ECHO_LENS -> countMechanisms(player, "truesight_chest:") >= 2
                    && countPages(player, 56, 65) >= 6;
        };
    }

    private static boolean hasFinalUpgradeProgress(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case STORM_COMPASS -> JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO) >= 2
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("storm_weaver"));
            case TIDE_LANTERN -> JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO) >= 2
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("tide_lantern_keeper"));
            case ECHO_LENS -> JournalManager.getResearchLevel(player, EchoAbilityType.TRUE_SIGHT_ECHO) >= 2
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("mirror_dust_butler"));
        };
    }

    private static boolean hasStructure(ServerPlayer player, String id) {
        return JournalManager.getData(player).getStructures().contains(UnknownEchoes.id(id).toString());
    }

    private static int countStructures(ServerPlayer player, String... ids) {
        int count = 0;
        for (String id : ids) {
            if (hasStructure(player, id)) {
                count++;
            }
        }
        return count;
    }

    private static int countMechanisms(ServerPlayer player, String prefix) {
        int count = 0;
        for (String key : EchoAbilityManager.getData(player).getActivatedMechanisms()) {
            if (key.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private static long countPages(ServerPlayer player, int minInclusive, int maxInclusive) {
        return JournalManager.getData(player).getPages().stream()
                .filter(id -> id >= minInclusive && id <= maxInclusive)
                .count();
    }

    /** 该玩家当前可选的调谐词条(2 级公开词条;对应能力研究 4 级追加隐藏词条)。 */
    public static List<String> getAvailableTuningWords(ServerPlayer player, ArtifactType type) {
        List<String> words = new ArrayList<>(type.getTuningWords());
        if (JournalManager.getResearchLevel(player, type.getLinkedAbility())
                >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL) {
            words.add(type.getHiddenWord());
        }
        return words;
    }

    /** 调谐:首次选择免费,改调谐消耗回响粉尘 ×2;随时可换(12.4)。 */
    public static boolean tune(ServerPlayer player, ArtifactType type, String word) {
        ArtifactData data = getData(player);
        if (!data.isClaimed(type) || data.getLevel(type) < 2) {
            actionbar(player, "message.unknown_echoes.artifact.tuning_locked");
            return false;
        }
        if (!getAvailableTuningWords(player, type).contains(word)) {
            actionbar(player, "message.unknown_echoes.artifact.tuning_unknown");
            return false;
        }
        if (word.equals(data.getTuning(type))) {
            return true;
        }
        boolean firstChoice = data.getTuning(type).isEmpty();
        if (!firstChoice) {
            if (countItem(player, ModItems.ECHO_DUST.get()) < TUNING_COST_DUST) {
                actionbar(player, "message.unknown_echoes.artifact.need_dust");
                return false;
            }
            consumeItem(player, ModItems.ECHO_DUST.get(), TUNING_COST_DUST);
        }
        data.setTuning(type, word);
        player.sendSystemMessage(Component.translatable("message.unknown_echoes.artifact.tuned",
                Component.translatable("artifact.unknown_echoes." + type.getId()),
                Component.translatable("artifact.unknown_echoes." + type.getId() + ".tuning." + word)));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.5F);
        syncToClient(player);
        return true;
    }

    // ---- 回响能量(主动型神器共享资源池,12.2) ----

    /** 能量上限(整点)。 */
    public static int getMaxEnergy() {
        return ServerConfig.ENERGY_MAX.get();
    }

    /** 当前恢复速率,单位是每 tick 恢复多少个 1/100 energyTwentieths。
     *  base=1 点/秒时等价于每 tick 100,即每秒 20 个 twentieths。 */
    public static int getRegenHundredthsPerTick(ServerPlayer player) {
        int base = ServerConfig.ENERGY_REGEN_PER_SECOND.get();
        if (player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            base *= ServerConfig.ENERGY_REALM_REGEN_MULTIPLIER.get();
        }
        int rate = base * 100;
        if (hasTideRegenBonus(player)) {
            int bonus = getData(player).getLevel(ArtifactType.TIDE_LANTERN) >= 3 ? 20 : 10;
            rate = rate * (100 + bonus) / 100;
        }
        if (EchoArmorSets.echoOathPieces(player) >= 4) {
            rate = rate * 110 / 100;
        }
        return rate;
    }

    /** 潮汐灯盏常驻收益:水中或雨中能量恢复 +10%/+20%。 */
    public static boolean hasTideRegenBonus(ServerPlayer player) {
        return getData(player).getLevel(ArtifactType.TIDE_LANTERN) > 0
                && (player.isInWater()
                || player.isEyeInFluid(FluidTags.WATER)
                || player.level().isRainingAt(player.blockPosition()));
    }

    /** 惰性结算能量恢复(按 gameTime 差值补足,无每 tick 开销),返回结算后的 1/20 点值。 */
    private static int settleEnergy(ServerPlayer player) {
        ArtifactData data = getData(player);
        long now = player.level().getGameTime();
        int max20 = getMaxEnergy() * 20;
        int value = data.getEnergyTwentieths();
        if (value < 0) {
            value = max20; // 首次接触按满池初始化
            data.setEnergyRemainderHundredths(0);
        } else {
            long elapsed = Math.max(0, now - data.getEnergyGameTime());
            long restored = data.getEnergyRemainderHundredths()
                    + elapsed * (long) getRegenHundredthsPerTick(player);
            int gain = (int) Math.min(Integer.MAX_VALUE, restored / 100L);
            value = Math.min(max20, value + gain);
            data.setEnergyRemainderHundredths(value >= max20 ? 0 : (int) (restored % 100L));
        }
        data.setEnergyTwentieths(value);
        data.setEnergyGameTime(now);
        return value;
    }

    /** 当前能量(整点,向下取整)。 */
    public static int getEnergy(ServerPlayer player) {
        return settleEnergy(player) / 20;
    }

    /** 扣除能量(主动型神器预留接口;V0.7+ 维度锚/时间沙漏接入)。服务端判定与扣除(12.5)。 */
    public static boolean spendEnergy(ServerPlayer player, int points) {
        double multiplier = ServerConfig.ARTIFACT_ENERGY_COST_MULTIPLIER.get();
        int cost20 = (int) Math.round(points * 20 * multiplier);
        int value = settleEnergy(player);
        if (value < cost20) {
            actionbar(player, "message.unknown_echoes.artifact.no_energy");
            return false;
        }
        getData(player).setEnergyTwentieths(value - cost20);
        syncToClient(player);
        return true;
    }

    /** 一次性回填能量(读残页/解谜/激活回访点,12.2:20-40 点档位)。 */
    public static void refillEnergy(ServerPlayer player, int points) {
        if (points <= 0) {
            return;
        }
        int value = settleEnergy(player);
        int max20 = getMaxEnergy() * 20;
        if (value >= max20) {
            getData(player).setEnergyRemainderHundredths(0);
            return;
        }
        int next = Math.min(max20, value + points * 20);
        getData(player).setEnergyTwentieths(next);
        if (next >= max20) {
            getData(player).setEnergyRemainderHundredths(0);
        }
        syncToClient(player);
    }

    // ---- 冷却(被动/提示型神器只用冷却,12.2) ----

    public static boolean isOnCooldown(ServerPlayer player, ArtifactType type) {
        return player.level().getGameTime() < getData(player).getCooldownEnd(type);
    }

    /** 启动冷却(秒,服务端权威;全局倍率走 ServerConfig,12.5)。 */
    public static void startCooldown(ServerPlayer player, ArtifactType type, int seconds) {
        long ticks = (long) Math.max(0, seconds * 20 * ServerConfig.ARTIFACT_COOLDOWN_MULTIPLIER.get());
        getData(player).setCooldownEnd(type, player.level().getGameTime() + ticks);
        syncToClient(player);
    }

    // ---- 管理员入口(/echoes admin,V0.6D):绕过领取资格但仍走本类落账与同步 ----

    /** 管理员直接授予神器(等级 1 + 新凭据),不校验资格,不消耗部件。 */
    public static void adminGrant(ServerPlayer player, ArtifactType type) {
        if (!hasCredentialSpace(player)) {
            actionbar(player, "message.unknown_echoes.artifact.need_space");
            return;
        }
        ArtifactData data = getData(player);
        if (!data.isClaimed(type)) {
            data.setLevel(type, 1);
        }
        data.nextSerial(type);
        giveCredential(player, type);
        syncToClient(player);
    }

    /** 管理员撤销神器(清除等级/调谐;序号保留使旧凭据失效)。 */
    public static void adminRevoke(ServerPlayer player, ArtifactType type) {
        ArtifactData data = getData(player);
        if (data.isClaimed(type)) {
            data.removeArtifact(type);
            syncToClient(player);
        }
    }

    // ---- 同步(客户端只收展示字段) ----

    public static void syncToClient(ServerPlayer player) {
        ArtifactData data = getData(player);
        int energy20 = settleEnergy(player);
        List<String> entries = new ArrayList<>();
        for (ArtifactType type : ArtifactType.values()) {
            if (data.isClaimed(type)) {
                entries.add(type.getId() + ":" + data.getLevel(type) + ":" + data.getTuning(type)
                        + ":" + data.getSerial(type) + ":" + data.getCooldownEnd(type));
            }
        }
        PacketDistributor.sendToPlayer(player, new ArtifactSyncPayload(entries,
                energy20, getMaxEnergy(), getRegenHundredthsPerTick(player),
                player.level().getGameTime()));
    }

    // ---- 工具 ----

    private static boolean hasCredentialSpace(ServerPlayer player) {
        return player.getInventory().getFreeSlot() >= 0;
    }

    private static void giveCredential(ServerPlayer player, ArtifactType type) {
        ItemStack credential = makeCredential(type, getData(player).getSerial(type));
        player.getInventory().add(credential);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
