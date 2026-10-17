package cn.kurt6.unknown_echoes.artifact;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.item.InventoryPayment;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.network.ArtifactSyncPayload;
import cn.kurt6.unknown_echoes.registry.ModAttachments;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ArtifactManager {
    private static final String PERPETUAL_CORE_CARRIED_KEY = "ue_perpetual_echo_core_carried";
    private static final String PENDING_CREDENTIAL_RETURN_PREFIX = "ue_pending_artifact_credential_";

    /** 持久化时间统一使用主世界时钟，避免跨维度或重启后冷却漂移。 */
    public static long gameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    /** 更改调谐消耗的回响粉尘数量；首次选择词条免费。 */
    public static final int TUNING_COST_DUST = 2;

    public static ArtifactData getData(Player player) {
        return player.getData(ModAttachments.ARTIFACT_DATA);
    }

    public static boolean isEnabled(ArtifactType type) {
        return !ServerConfig.DISABLED_ARTIFACTS.get().contains(type.getId());
    }

    /** 制作凭据物品:组件存"神器 id + 凭据序号 + owner",与玩家数据比对后才可用。 */
    public static ItemStack makeCredential(ArtifactType type, int serial, ServerPlayer owner) {
        ItemStack stack = new ItemStack(type.getCredentialItem());
        stack.set(ModDataComponents.ARTIFACT_ID.get(), type.getId());
        stack.set(ModDataComponents.CREDENTIAL_SERIAL.get(), serial);
        stack.set(ModDataComponents.ARTIFACT_OWNER.get(), owner.getUUID().toString());
        writeOwnerLore(stack, owner);
        return stack;
    }

    /** 凭据校验:物品序号必须等于玩家数据中的当前序号(旧凭据/他人凭据均不通过)。 */
    public static boolean validateCredential(ServerPlayer player, ArtifactType type, ItemStack stack) {
        if (!getData(player).isClaimed(type)) {
            return false;
        }
        if (!stack.is(type.getCredentialItem())) {
            return false;
        }
        String id = stack.get(ModDataComponents.ARTIFACT_ID.get());
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        if (!type.getId().equals(id) || serial == null || serial <= 0
                || serial != getData(player).getSerial(type)) {
            return false;
        }
        String owner = stack.get(ModDataComponents.ARTIFACT_OWNER.get());
        if (owner == null || owner.isBlank()
                || !owner.equals(player.getUUID().toString())) {
            return false;
        }
        writeOwnerLore(stack, player);
        return true;
    }

    /** 登录或重生时补写有效凭据的主人名，不暴露内部序号。 */
    public static void refreshCredentialOwnerNames(ServerPlayer player) {
        boolean found = refreshCredentialOwnerNames(player, player.getInventory().items);
        found |= refreshCredentialOwnerNames(player, player.getInventory().offhand);
        if (found) {
            player.getInventory().setChanged();
        }
    }

    private static boolean refreshCredentialOwnerNames(ServerPlayer player, List<ItemStack> stacks) {
        boolean found = false;
        for (ItemStack stack : stacks) {
            ArtifactType type = credentialType(stack);
            if (type != null && validateCredential(player, type, stack)) {
                found = true;
            }
        }
        return found;
    }

    private static void writeOwnerLore(ItemStack stack, ServerPlayer owner) {
        Component ownerLine = Component.translatable("tooltip.unknown_echoes.artifact.owner",
                        owner.getGameProfile().getName())
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC);
        ItemLore lore = new ItemLore(List.of(ownerLine));
        if (!lore.equals(stack.get(DataComponents.LORE))) {
            stack.set(DataComponents.LORE, lore);
        }
    }

    /** 是否是任意神器凭据物品。用于掉落/丢弃保护,不代表当前可用。 */
    public static boolean isArtifactCredential(ItemStack stack) {
        return credentialType(stack) != null;
    }

    /** 当前玩家正在持有的有效神器凭据。旧序号/他人凭据不受防丢保护,可丢弃清理。 */
    public static boolean isCurrentCredential(ServerPlayer player, ItemStack stack) {
        ArtifactType type = credentialType(stack);
        return type != null && validateCredential(player, type, stack);
    }

    /** 把绑定凭据放回背包。落点判据与信物归还同源,见 InventoryPayment.placeBound。 */
    public static boolean returnCredentialToInventory(ServerPlayer player, ItemStack stack) {
        return InventoryPayment.placeBound(player, stack);
    }


    /** 丢弃事件已先从背包移除物品；先落持久标记，再尝试立即归还，避免停服窗口吞掉凭据。 */
    public static boolean returnTossedCredential(ServerPlayer player, ItemStack stack) {
        ArtifactType type = credentialType(stack);
        if (type == null || !validateCredential(player, type, stack)) {
            return false;
        }
        String key = pendingCredentialReturnKey(type);
        player.getPersistentData().putBoolean(key, true);
        if (!returnCredentialToInventory(player, stack.copyWithCount(1))) {
            return false;
        }
        player.getPersistentData().remove(key);
        return true;
    }

    /** 死亡掉落移除前记录待归还类型；凭据内容可由玩家权威数据和当前序号确定性重建。 */
    public static void queueCredentialReturn(ServerPlayer player, ItemStack stack) {
        ArtifactType type = credentialType(stack);
        if (type != null && validateCredential(player, type, stack)) {
            player.getPersistentData().putBoolean(pendingCredentialReturnKey(type), true);
        }
    }

    /** 登录、重生及背包腾位后重试；返回 true 表示仍有凭据等待空位。 */
    public static boolean restorePendingCredentialReturns(ServerPlayer player) {
        ArtifactData data = getData(player);
        boolean remaining = false;
        for (ArtifactType type : ArtifactType.values()) {
            String key = pendingCredentialReturnKey(type);
            if (!player.getPersistentData().getBoolean(key)) {
                continue;
            }
            if (!data.isClaimed(type) || data.getSerial(type) <= 0
                    || normalizeCredentials(player, type)) {
                player.getPersistentData().remove(key);
                continue;
            }
            if (giveCredential(player, type)) {
                player.getPersistentData().remove(key);
            } else {
                remaining = true;
            }
        }
        return remaining;
    }

    /** NeoForge persistent data 不自动跨死亡克隆，待归还标记必须显式复制。 */
    public static void copyPendingCredentialReturns(Player original, Player replacement) {
        for (ArtifactType type : ArtifactType.values()) {
            String key = pendingCredentialReturnKey(type);
            if (original.getPersistentData().getBoolean(key)) {
                replacement.getPersistentData().putBoolean(key, true);
            }
        }
    }

    /** 领取资格判定(只读)。资格本体是个人探索进度,永不作为掉落物或材料消耗。 */
    public static boolean hasClaimRequirement(ServerPlayer player, ArtifactType type) {
        return switch (type) {
            case STORM_COMPASS -> EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)
                    && hasStructure(player, "sky_observatory")
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("storm_weaver"));
            case TIDE_LANTERN -> EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                    && hasStructure(player, "tide_lighthouse_reef")
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("tide_lantern_keeper"));
            case ECHO_LENS -> EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO)
                    && hasStructure(player, "mirror_dust_cloister")
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("mirror_dust_butler"));
            case TIME_HOURGLASS -> EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO)
                    && hasStructure(player, "folded_archive");
            case DIMENSION_ANCHOR -> EchoAbilityManager.hasActivatedBeacon(
                    player, EchoPermission.ABYSS_BEACON_ID)
                    && EchoAbilityManager.hasUnlockedDimension(player, EchoPermission.DEEP_ECHOES_ID);
            case RIFT_ANCHOR -> EchoAbilityManager.hasAbility(player, EchoAbilityType.VOID_ECHO)
                    && EchoAbilityManager.hasDefeatedBoss(player, EchoPermission.RIFT_WARDEN_ID);
        };
    }

    /** 记录台交互:未领取→校验个人进度、写数据、发凭据;已领取→复领(作废旧序号)。 */
    public static boolean claimOrReissue(ServerPlayer player, ArtifactType type) {
        if (!isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return false;
        }
        ArtifactData data = getData(player);
        if (data.isClaimed(type)) {
            if (normalizeCredentials(player, type)) {
                actionbar(player, "message.unknown_echoes.artifact.already_bound");
                syncToClient(player);
                return true;
            }
            if (data.getSerial(type) == Integer.MAX_VALUE) {
                actionbar(player, "message.unknown_echoes.artifact.serial_exhausted");
                return false;
            }
            if (!hasCredentialSpace(player)) {
                actionbar(player, "message.unknown_echoes.artifact.need_space");
                return false;
            }

            data.nextSerial(type);
            if (!giveCredential(player, type)) {
                actionbar(player, "message.unknown_echoes.artifact.need_space");
                return false;
            }
            player.playNotifySound(ModSounds.ARTIFACT_CREDENTIAL_ISSUE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            syncToClient(player);
            return true;
        }
        if (!hasClaimRequirement(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.not_eligible");
            return false;
        }
        if (data.getSerial(type) == Integer.MAX_VALUE) {
            actionbar(player, "message.unknown_echoes.artifact.serial_exhausted");
            return false;
        }
        if (!hasCredentialSpace(player)) {
            actionbar(player, "message.unknown_echoes.artifact.need_space");
            return false;
        }
        data.setLevel(type, eligibleLevel(player, type));
        data.nextSerial(type);
        if (!giveCredential(player, type)) {
            actionbar(player, "message.unknown_echoes.artifact.need_space");
            return false;
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
        syncToClient(player);
        ModAdvancements.checkArtifactAdvancements(player);
        return true;
    }

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
        ModAdvancements.checkArtifactAdvancements(player);
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
                    && countPages(player, 17, 24) >= 2;
            case TIDE_LANTERN -> countMechanisms(player, "tide_pillar:") >= 2
                    && countPages(player, 42, 51) >= 3;
            case ECHO_LENS -> countMechanisms(player, "truesight_chest:") >= 2
                    && countPages(player, 56, 65) >= 3;
            case TIME_HOURGLASS -> countStructures(player, "inverted_chasm_bridge",
                    "silent_abyss_anchor", "spatial_rift", "void_vein_chamber") >= 2;
            case DIMENSION_ANCHOR -> countStructures(player, "folded_archive",
                    "inverted_chasm_bridge", "silent_abyss_anchor") >= 2;
            case RIFT_ANCHOR -> countStructures(player, "world_resonance_hall",
                    "voidscar_bastion", "riftglass_citadel", "shattered_codex_vault",
                    "riftworn_memorial_court") >= 2;
        };
    }

    private static boolean hasFinalUpgradeProgress(ServerPlayer player, ArtifactType type) {
        if (!hasFirstUpgradeProgress(player, type)) {
            return false;
        }
        return switch (type) {
            case STORM_COMPASS -> JournalManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO)
                    >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("storm_weaver"));
            case TIDE_LANTERN -> JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO)
                    >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("tide_lantern_keeper"));
            case ECHO_LENS -> JournalManager.getResearchLevel(player, EchoAbilityType.TRUE_SIGHT_ECHO)
                    >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL
                    && EchoAbilityManager.hasDefeatedBoss(player, UnknownEchoes.id("mirror_dust_butler"));
            case TIME_HOURGLASS -> JournalManager.getResearchLevel(player, EchoAbilityType.GRAVITY_ECHO)
                    >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL
                    && EchoAbilityManager.hasDefeatedBoss(player, EchoPermission.VOID_PROPHET_ID);
            case DIMENSION_ANCHOR -> JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO) >= 2
                    && EchoAbilityManager.hasAbility(player, EchoAbilityType.GRAVITY_ECHO);
            case RIFT_ANCHOR -> JournalManager.getResearchLevel(player, EchoAbilityType.VOID_ECHO)
                    >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL
                    && EchoAbilityManager.hasDefeatedBoss(player, EchoPermission.RIFT_WARDEN_ID);
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

    public static List<String> getAvailableTuningWords(ServerPlayer player, ArtifactType type) {
        List<String> words = new ArrayList<>(type.getTuningWords());
        if (JournalManager.getResearchLevel(player, type.getLinkedAbility())
                >= ArtifactType.HIDDEN_WORD_RESEARCH_LEVEL) {
            words.add(type.getHiddenWord());
        }
        return words;
    }

    /** 调谐:首次选择免费,改调谐消耗回响粉尘 ×2;随时可换。 */
    public static boolean tune(ServerPlayer player, ArtifactType type, String word) {
        if (!isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return false;
        }
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
        }

        // 必须先写入再收费:setTuning 对非法词是静默 no-op,顺序反了就是"扣了钱没生效"。
        data.setTuning(type, word);
        if (!word.equals(data.getTuning(type))) {
            actionbar(player, "message.unknown_echoes.artifact.tuning_unknown");
            return false;
        }
        if (!firstChoice) {
            consumeItem(player, ModItems.ECHO_DUST.get(), TUNING_COST_DUST);
        }
        ArtifactAnchorHelper.normalizeAfterTuning(player, type);
        player.sendSystemMessage(Component.translatable("message.unknown_echoes.artifact.tuned",
                Component.translatable("artifact.unknown_echoes." + type.getId()),
                Component.translatable("artifact.unknown_echoes." + type.getId() + ".tuning." + word)));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.5F);
        syncToClient(player);
        return true;
    }

    public static int getMaxEnergy() {
        return ServerConfig.ENERGY_MAX.get();
    }

    /** 玩家当前共享能量上限；套装不扩充上限,避免占用强度预算。 */
    public static int getMaxEnergy(ServerPlayer player) {
        return getMaxEnergy();
    }

    /** 当前恢复速率,单位是每 tick 恢复多少个 1/100 energyTwentieths。 */
    public static int getRegenHundredthsPerTick(ServerPlayer player) {
        double perSecond = ServerConfig.ENERGY_REGEN_PER_SECOND.get();
        if (player.level().dimension().location().equals(EchoPermission.ECHO_REALM_ID)) {
            perSecond *= ServerConfig.ENERGY_REALM_REGEN_MULTIPLIER.get();
        }
        perSecond *= ServerConfig.ENERGY_NATURAL_REGEN_MULTIPLIER.get();
        int rate = (int) Math.round(perSecond * 100.0D);
        int tideResearch = JournalManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO);
        if (tideResearch >= 1 && isInWaterOrRain(player)) {
            rate = (int) Math.round(rate * (tideResearch >= 4 ? 1.25D : 1.10D));
        }
        if (hasTideRegenBonus(player)) {
            int bonus = getData(player).getLevel(ArtifactType.TIDE_LANTERN) >= 3 ? 20 : 10;
            rate = rate * (100 + bonus) / 100;
        }
        if (EchoArmorSets.echoOathPieces(player) >= 4) {
            rate = rate * 115 / 100;
        }
        return rate;
    }

    /** 潮汐灯盏常驻收益:水中或雨中能量恢复 +10%/+20%。 */
    public static boolean hasTideRegenBonus(ServerPlayer player) {
        return isEnabled(ArtifactType.TIDE_LANTERN)
                && getData(player).getLevel(ArtifactType.TIDE_LANTERN) > 0
                && isInWaterOrRain(player);
    }

    private static boolean isInWaterOrRain(ServerPlayer player) {
        return player.isInWater()
                || player.isEyeInFluid(FluidTags.WATER)
                || player.level().isRainingAt(player.blockPosition());
    }

    /** 永续核心只改变共享能量结算，不授予神器、能力或任何进度权限。 */
    public static boolean hasPerpetualEchoCore(ServerPlayer player) {
        return countItem(player, ModItems.PERPETUAL_ECHO_CORE.get()) > 0;
    }

    /** 携带状态变化时立即重推满能量展示；稳定状态不产生额外网络流量。 */
    public static void syncPerpetualEchoCoreState(ServerPlayer player) {
        boolean carried = hasPerpetualEchoCore(player);
        if (player.getPersistentData().getBoolean(PERPETUAL_CORE_CARRIED_KEY) == carried) {
            return;
        }
        player.getPersistentData().putBoolean(PERPETUAL_CORE_CARRIED_KEY, carried);
        syncToClient(player);
    }

    /** 惰性结算能量恢复(按 gameTime 差值补足,无每 tick 开销),返回结算后的 1/20 点值。 */
    private static int settleEnergy(ServerPlayer player) {
        ArtifactData data = getData(player);
        long now = gameTime(player);
        int max20 = getMaxEnergy(player) * 20;
        if (hasPerpetualEchoCore(player)) {
            return fillPerpetualEnergy(data, max20, now);
        }
        int value = data.getEnergyTwentieths();
        if (value < 0) {
            value = max20;
            data.setEnergyRemainderHundredths(0);
        } else {
            value = Math.min(value, max20);
            long lastGameTime = data.getEnergyGameTime();
            long elapsed = lastGameTime >= 0L && lastGameTime <= now ? now - lastGameTime : 0L;
            long remainder = data.getEnergyRemainderHundredths();
            long rate = Math.max(0L, getRegenHundredthsPerTick(player));
            long restored = rate > 0L && elapsed > (Long.MAX_VALUE - remainder) / rate
                    ? Long.MAX_VALUE
                    : remainder + elapsed * rate;
            int gain = (int) Math.min(Integer.MAX_VALUE, restored / 100L);
            value = (int) Math.min(max20, (long) value + gain);
            data.setEnergyRemainderHundredths(value >= max20 ? 0 : (int) (restored % 100L));
        }
        data.setEnergyTwentieths(value);
        data.setEnergyGameTime(now);
        return value;
    }

    private static int fillPerpetualEnergy(ArtifactData data, int max20, long now) {
        data.setEnergyTwentieths(max20);
        data.setEnergyRemainderHundredths(0);
        data.setEnergyGameTime(now);
        return max20;
    }

    public static int getEnergy(ServerPlayer player) {
        return settleEnergy(player) / 20;
    }

    /** 只读预检主动效果是否有足够的精确能量,不产生提示也不扣除能量。 */
    public static boolean hasEnoughEnergy(ServerPlayer player, int points) {
        if (points <= 0 || hasPerpetualEchoCore(player)) {
            return true;
        }
        return settleEnergy(player) >= scaledEnergyCostTwentieths(points);
    }

    /** 扣除能量(主动型神器预留接口;+ 维度锚/时间沙漏接入)。服务端判定与扣除。 */
    public static boolean spendEnergy(ServerPlayer player, int points) {
        EnergyPayment payment = spendEnergyQuietly(player, points);
        if (payment == null) {
            return false;
        }
        if (payment.needsSync()) {
            syncToClient(player);
        }
        return true;
    }

    /** 支付记录先返回调用方，再由调用方进入可能失败的同步或传送回调。 */
    static EnergyPayment spendEnergyQuietly(ServerPlayer player, int points) {
        if (points <= 0) {
            return new EnergyPayment(0, false);
        }
        if (hasPerpetualEchoCore(player)) {
            ArtifactData data = getData(player);
            int max20 = getMaxEnergy(player) * 20;
            boolean needsSync = data.getEnergyTwentieths() != max20
                    || !player.getPersistentData().getBoolean(PERPETUAL_CORE_CARRIED_KEY);
            fillPerpetualEnergy(data, max20, gameTime(player));
            player.getPersistentData().putBoolean(PERPETUAL_CORE_CARRIED_KEY, true);
            return new EnergyPayment(0, needsSync);
        }
        int cost20 = scaledEnergyCostTwentieths(points);
        int value = settleEnergy(player);
        if (value < cost20) {
            actionbar(player, "message.unknown_echoes.artifact.no_energy");
            return null;
        }
        getData(player).setEnergyTwentieths(value - cost20);
        return new EnergyPayment(cost20, true);
    }

    /** 按支付记录退款，不重读倍率或永续核心；调用方在收尾时同步。 */
    static void refundSpentEnergy(ServerPlayer player, EnergyPayment payment) {
        if (payment.spentTwentieths() <= 0) {
            return;
        }
        int value = settleEnergy(player);
        int max20 = getMaxEnergy(player) * 20;
        int next = (int) Math.min(max20, (long) value + payment.spentTwentieths());
        getData(player).setEnergyTwentieths(next);
        if (next >= max20) {
            getData(player).setEnergyRemainderHundredths(0);
        }
    }

    record EnergyPayment(int spentTwentieths, boolean needsSync) {
    }

    /** 与实际扣除完全一致的耗能，供服务端同步 UI；单位为 1/20 点。 */
    private static int scaledEnergyCostTwentieths(int points) {
        double multiplier = ServerConfig.ARTIFACT_ENERGY_COST_MULTIPLIER.get();
        long scaledCost = Math.round((double) Math.max(0, points) * 20.0D * multiplier);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, scaledCost));
    }

    public static void refillEnergy(ServerPlayer player, int points) {
        if (points <= 0) {
            return;
        }
        int value = settleEnergy(player);
        int max20 = getMaxEnergy(player) * 20;
        if (value >= max20) {
            getData(player).setEnergyRemainderHundredths(0);
            return;
        }
        int next = (int) Math.min(max20, (long) value + (long) points * 20L);
        getData(player).setEnergyTwentieths(next);
        if (next >= max20) {
            getData(player).setEnergyRemainderHundredths(0);
        }
        syncToClient(player);
    }

    public static boolean isOnCooldown(ServerPlayer player, ArtifactType type) {
        return gameTime(player) < normalizedCooldownEnd(player, type);
    }

    /** 启动服务端权威冷却，时长受 ServerConfig 全局倍率影响。 */
    public static void startCooldown(ServerPlayer player, ArtifactType type, int seconds) {
        reserveCooldown(player, type, seconds);
        syncToClient(player);
    }

    /** 支付前静默预留冷却；支付同步携带该状态，明确支付失败时由调用方释放。 */
    public static void reserveCooldown(ServerPlayer player, ArtifactType type, int seconds) {
        long ticks = scaledCooldownTicks(seconds);
        long now = gameTime(player);
        getData(player).setCooldown(type, safeDeadline(now, ticks), ticks);
    }

    private static long normalizedCooldownEnd(ServerPlayer player, ArtifactType type) {
        ArtifactData data = getData(player);
        long now = gameTime(player);
        long end = data.getCooldownEnd(type);
        if (end <= now) {
            if (end != 0L || data.getCooldownDuration(type) != 0L) {
                data.clearCooldown(type);
            }
            return 0L;
        }

        long configuredMaximum = scaledCooldownTicks(
                ArtifactEffectManager.getCooldownSeconds(player, type));
        long recordedDuration = data.getCooldownDuration(type);
        long maximumDuration = recordedDuration > 0L
                ? recordedDuration : configuredMaximum;
        long maximumEnd = safeDeadline(now, maximumDuration);
        if (end > maximumEnd) {
            data.setCooldown(type, maximumEnd, maximumDuration);
            return maximumEnd;
        }
        return end;
    }

    private static long scaledCooldownTicks(int seconds) {
        return Math.max(0L, Math.round((double) seconds * 20.0D
                * ServerConfig.ARTIFACT_COOLDOWN_MULTIPLIER.get()));
    }

    private static long safeDeadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    public static boolean adminGrant(ServerPlayer player, ArtifactType type) {
        ArtifactData data = getData(player);
        if (data.getSerial(type) == Integer.MAX_VALUE) {
            actionbar(player, "message.unknown_echoes.artifact.serial_exhausted");
            return false;
        }
        if (!hasCredentialSpace(player)) {
            actionbar(player, "message.unknown_echoes.artifact.need_space");
            return false;
        }
        if (!data.isClaimed(type)) {
            data.setLevel(type, 1);
        }
        data.nextSerial(type);
        if (!giveCredential(player, type)) {
            return false;
        }
        syncToClient(player);
        return true;
    }

    public static void adminRevoke(ServerPlayer player, ArtifactType type) {
        ArtifactData data = getData(player);
        if (data.isClaimed(type)) {
            data.removeArtifact(type);
            data.clearCooldown(type);
            player.getCooldowns().removeCooldown(type.getCredentialItem());
            ArtifactEffectManager.onArtifactRevoked(player, type);
            syncToClient(player);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        ArtifactData data = getData(player);
        int energy20 = settleEnergy(player);
        long now = gameTime(player);
        List<String> entries = new ArrayList<>();
        for (ArtifactType type : ArtifactType.values()) {
            if (data.isClaimed(type)) {
                long cooldownEnd = normalizedCooldownEnd(player, type);
                long remaining = cooldownEnd - now;
                if (remaining > 0L && !player.getCooldowns().isOnCooldown(type.getCredentialItem())) {
                    player.getCooldowns().addCooldown(type.getCredentialItem(),
                            (int) Math.min(Integer.MAX_VALUE, remaining));
                }
                int energyCost20 = scaledEnergyCostTwentieths(
                        ArtifactEffectManager.getEnergyCost(player, type));
                entries.add(type.getId() + ":" + data.getLevel(type) + ":" + data.getTuning(type)
                        + ":" + data.getSerial(type) + ":" + cooldownEnd
                        + ":" + energyCost20);
            }
        }
        PacketDistributor.sendToPlayer(player, new ArtifactSyncPayload(entries,
                energy20, getMaxEnergy(player), getRegenHundredthsPerTick(player),
                gameTime(player)));
    }

    private static boolean hasCredentialSpace(ServerPlayer player) {
        return InventoryPayment.hasBoundItemSpace(player);
    }


    private static boolean giveCredential(ServerPlayer player, ArtifactType type) {
        ItemStack credential = makeCredential(type, getData(player).getSerial(type), player);
        return returnCredentialToInventory(player, credential);
    }

    private static String pendingCredentialReturnKey(ArtifactType type) {
        return PENDING_CREDENTIAL_RETURN_PREFIX + type.getId();
    }

    private static ArtifactType credentialType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String id = stack.get(ModDataComponents.ARTIFACT_ID.get());
        ArtifactType byId = id == null ? null : ArtifactType.byId(id);
        if (byId != null && stack.is(byId.getCredentialItem())) {
            return byId;
        }
        for (ArtifactType type : ArtifactType.values()) {
            if (stack.is(type.getCredentialItem())) {
                return type;
            }
        }
        return null;
    }

    private static boolean normalizeCredentials(ServerPlayer player, ArtifactType type) {
        boolean foundValid = normalizeCredentialList(player, type, player.getInventory().items, false);
        foundValid = normalizeCredentialList(player, type, player.getInventory().offhand, foundValid);
        player.getInventory().setChanged();
        return foundValid;
    }

    private static boolean normalizeCredentialList(ServerPlayer player, ArtifactType type,
                                                   List<ItemStack> stacks, boolean foundValid) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (credentialType(stack) != type) {
                continue;
            }
            boolean valid = validateCredential(player, type, stack);
            if (valid && !foundValid) {
                foundValid = true;
            } else {
                stacks.set(i, ItemStack.EMPTY);
            }
        }
        return foundValid;
    }

    private static int countItem(ServerPlayer player, Item item) {
        return InventoryPayment.count(player, item);
    }


    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        InventoryPayment.consume(player, item, amount);
    }


    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
