package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 第一维度闭环判定与 T7 个人奖励。只读取服务端玩家数据,
 * 一次性奖励记录写入 activatedMechanisms,不依赖可被抢走的掉落物。
 */
public final class EchoCompletionManager {
    private static final String T7_REWARD_KEY = "completion:echo_realm_t7_reward";
    private static final String T7_CORE_KEY = "completion:echo_realm_t7_core";
    private static final String T7_INGOTS_KEY = "completion:echo_realm_t7_ingots";
    private static final String T7_CLUE_MAP_KEY = "completion:echo_realm_t7_clue_map";
    private static final String ARCHIVE_TERMINAL_KEY = "completion:echo_grand_archive_terminal";
    private static final String INDEX_UNLOCK_KEY = "completion:echo_index_unlocked";
    private static final String INDEX_EXPANSION_ONE_KEY = "completion:echo_index_expansion_1";
    private static final String INDEX_EXPANSION_TWO_KEY = "completion:echo_index_expansion_2";
    private static final ResourceLocation ARCHIVE_STRUCTURE = UnknownEchoes.id("echo_grand_archive");
    private static final int REQUIRED_BIOMES = 8;
    private static final int REQUIRED_PAGES = 48;
    private static final int REQUIRED_MINI_BOSSES = 6;
    private static final int REQUIRED_SCORE = 150;
    private static final int INDEX_EXPANSION_ONE_PAGES = 60;
    private static final int DEEP_INDEX_PAGES = 72;
    private static final int DEEP_INDEX_SCORE = 160;
    private static final List<ResourceLocation> MAIN_BOSSES = List.of(
            UnknownEchoes.id("forgotten_colossus"),
            UnknownEchoes.id("abyss_watcher"),
            UnknownEchoes.id("mirror_guardian"));
    private static final List<ResourceLocation> MINI_BOSSES = List.of(
            UnknownEchoes.id("storm_weaver"),
            UnknownEchoes.id("tide_lantern_keeper"),
            UnknownEchoes.id("mirror_dust_butler"),
            UnknownEchoes.id("silent_priest"),
            UnknownEchoes.id("crystal_songkeeper"),
            UnknownEchoes.id("broken_bell_keeper"),
            UnknownEchoes.id("dream_bloom_keeper"),
            UnknownEchoes.id("lost_recorder_chief"));
    private static final List<String> PUZZLE_PREFIXES = List.of(
            "resonance_ritual:", "tide_pillar:", "mirror_sigil:",
            "research_revisit:", "world_event:");

    private EchoCompletionManager() {
    }

    public static boolean handleIndexBookUse(ServerPlayer player) {
        return showSummaryAndClaim(player);
    }

    public static boolean handleArchiveTerminalUse(ServerPlayer player) {
        EchoAbilityManager.activateMechanism(player, ARCHIVE_TERMINAL_KEY);
        JournalManager.recordStructure(player, ARCHIVE_STRUCTURE, player.blockPosition());
        return showSummaryAndClaim(player);
    }

    private static boolean showSummaryAndClaim(ServerPlayer player) {
        List<Component> summary = buildCompletionSummary(player);
        for (Component line : summary) {
            player.sendSystemMessage(line);
        }
        if (!isComplete(player)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.completion.incomplete"), true);
            return false;
        }
        if (hasClaimedT7(player)) {
            claimSupplementalRewards(player);
            claimIndexExpansionRewards(player);
            player.displayClientMessage(Component.translatable("message.unknown_echoes.completion.claimed"), true);
            return true;
        }
        claimT7Reward(player);
        claimIndexExpansionRewards(player);
        return true;
    }

    public static boolean isComplete(ServerPlayer player) {
        return hasCoreAbilities(player)
                && hasCoreBosses(player)
                && visitedBiomes(player) >= REQUIRED_BIOMES
                && pagesRead(player) >= REQUIRED_PAGES
                && miniBossKills(player) >= REQUIRED_MINI_BOSSES
                && hasArchiveTerminal(player)
                && hasArchiveTrials(player)
                && completionScore(player) >= REQUIRED_SCORE;
    }

    public static List<Component> buildCompletionSummary(ServerPlayer player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("completion.unknown_echoes.header").withStyle(ChatFormatting.GOLD));
        lines.add(doneLine("completion.unknown_echoes.abilities", hasCoreAbilities(player)));
        lines.add(doneLine("completion.unknown_echoes.bosses", hasCoreBosses(player)));
        lines.add(thresholdLine("completion.unknown_echoes.biomes",
                visitedBiomes(player), EchoRealmBiomeCatalog.REQUIRED_COUNT, REQUIRED_BIOMES));
        lines.add(thresholdLine("completion.unknown_echoes.pages",
                pagesRead(player), JournalManager.KNOWN_PAGES, REQUIRED_PAGES));
        lines.add(thresholdLine("completion.unknown_echoes.mini_bosses",
                miniBossKills(player), MINI_BOSSES.size(), REQUIRED_MINI_BOSSES));
        lines.add(doneLine("completion.unknown_echoes.archive", hasArchiveTerminal(player)));
        lines.add(doneLine("completion.unknown_echoes.archive_trials", hasArchiveTrials(player)));
        lines.add(countLine("completion.unknown_echoes.score", completionScore(player), REQUIRED_SCORE));
        lines.add(Component.translatable(hasClaimedT7(player)
                        ? "completion.unknown_echoes.reward.claimed"
                        : "completion.unknown_echoes.reward.ready")
                .withStyle(hasClaimedT7(player) ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));
        lines.addAll(buildIndexFeedback(player));
        return lines;
    }

    public static List<Component> buildIndexFeedback(ServerPlayer player) {
        List<Component> lines = new ArrayList<>();
        if (!hasEchoIndexUnlocked(player)) {
            lines.add(Component.translatable("completion.unknown_echoes.index.locked",
                    completionScore(player), REQUIRED_SCORE).withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        lines.add(Component.translatable("completion.unknown_echoes.index.unlocked",
                completionScore(player)).withStyle(ChatFormatting.AQUA));
        if (!hasIndexExpansionOne(player)) {
            lines.add(Component.translatable(canClaimIndexExpansionOne(player)
                            ? "completion.unknown_echoes.index.expansion1.ready"
                            : "completion.unknown_echoes.index.expansion1.progress",
                    pagesRead(player), INDEX_EXPANSION_ONE_PAGES,
                    visitedBiomes(player), REQUIRED_BIOMES).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("completion.unknown_echoes.index.expansion1.claimed")
                .withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("completion.unknown_echoes.index.target",
                Component.translatable("completion.unknown_echoes.index.target." + backfillTarget(player)))
                .withStyle(ChatFormatting.AQUA));
        if (!hasIndexExpansionTwo(player)) {
            lines.add(Component.translatable(canClaimIndexExpansionTwo(player)
                            ? "completion.unknown_echoes.index.expansion2.ready"
                            : "completion.unknown_echoes.index.expansion2.progress",
                    pagesRead(player), DEEP_INDEX_PAGES,
                    completionScore(player), DEEP_INDEX_SCORE).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.translatable("completion.unknown_echoes.index.expansion2.claimed")
                .withStyle(ChatFormatting.GREEN));
        int beforeDeepChecks = lines.size();
        addDeepCheck(lines, "completion.unknown_echoes.index.pages", pagesRead(player), DEEP_INDEX_PAGES);
        addDeepCheck(lines, "completion.unknown_echoes.index.biomes",
                visitedBiomes(player), EchoRealmBiomeCatalog.REQUIRED_COUNT);
        addDeepCheck(lines, "completion.unknown_echoes.index.mini_bosses",
                miniBossKills(player), MINI_BOSSES.size());
        addDeepCheck(lines, "completion.unknown_echoes.index.artifacts",
                artifactUpgradeSteps(player), ArtifactType.values().length * (ArtifactType.MAX_LEVEL - 1));
        if (lines.size() == beforeDeepChecks) {
            lines.add(Component.translatable("completion.unknown_echoes.index.ready")
                    .withStyle(ChatFormatting.GREEN));
        }
        return lines;
    }

    public static int completionScore(ServerPlayer player) {
        var journal = JournalManager.getData(player);
        int score = 0;
        score += Math.min(visitedBiomes(player) * 3, 30);
        score += Math.min(journal.getStructures().size() * 2, 40);
        score += Math.min(puzzleCompletions(player) * 3, 45);
        score += Math.min(miniBossKills(player) * 6, 48);
        score += Math.min(pagesRead(player), 80);
        score += Math.min(abilityEnhancementStages(player) * 5, 45);
        score += Math.min(artifactUnlocks(player) * 4, 12);
        score += Math.min(artifactUpgradeSteps(player) * 3, 18);
        return score;
    }

    private static boolean hasCoreAbilities(ServerPlayer player) {
        return EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                && EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);
    }

    private static boolean hasCoreBosses(ServerPlayer player) {
        return defeatedCount(player, MAIN_BOSSES) == MAIN_BOSSES.size();
    }

    private static int visitedBiomes(ServerPlayer player) {
        return EchoRealmBiomeCatalog.countKnown(JournalManager.getData(player).getBiomes());
    }

    private static int pagesRead(ServerPlayer player) {
        return Math.min(AncientPageCatalog.countReleasePages(JournalManager.getData(player).getPages()),
                JournalManager.KNOWN_PAGES);
    }

    private static int miniBossKills(ServerPlayer player) {
        return defeatedCount(player, MINI_BOSSES);
    }

    private static int defeatedCount(ServerPlayer player, List<ResourceLocation> bossIds) {
        int count = 0;
        for (ResourceLocation bossId : bossIds) {
            if (EchoAbilityManager.hasDefeatedBoss(player, bossId)) {
                count++;
            }
        }
        return count;
    }

    private static int puzzleCompletions(ServerPlayer player) {
        int count = 0;
        for (String key : EchoAbilityManager.getData(player).getActivatedMechanisms()) {
            if (isPuzzleKey(key)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPuzzleKey(String key) {
        for (String prefix : PUZZLE_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static int abilityEnhancementStages(ServerPlayer player) {
        int sum = 0;
        sum += Math.min(3, EchoResearchManager.getResearchLevel(player, EchoAbilityType.WIND_ECHO));
        sum += Math.min(3, EchoResearchManager.getResearchLevel(player, EchoAbilityType.TIDE_ECHO));
        sum += Math.min(3, EchoResearchManager.getResearchLevel(player, EchoAbilityType.TRUE_SIGHT_ECHO));
        return sum;
    }

    private static int artifactUnlocks(ServerPlayer player) {
        int count = 0;
        var data = ArtifactManager.getData(player);
        for (ArtifactType type : ArtifactType.values()) {
            if (data.isClaimed(type)) {
                count++;
            }
        }
        return count;
    }

    private static int artifactUpgradeSteps(ServerPlayer player) {
        int count = 0;
        var data = ArtifactManager.getData(player);
        for (ArtifactType type : ArtifactType.values()) {
            count += Math.max(0, data.getLevel(type) - 1);
        }
        return count;
    }

    private static boolean hasArchiveTerminal(ServerPlayer player) {
        return EchoAbilityManager.hasActivatedMechanism(player, ARCHIVE_TERMINAL_KEY);
    }

    private static boolean hasArchiveTrials(ServerPlayer player) {
        return ArchiveTrialProgress.hasCompletedAll(player);
    }

    public static boolean hasEchoIndexUnlocked(ServerPlayer player) {
        return EchoAbilityManager.hasActivatedMechanism(player, INDEX_UNLOCK_KEY);
    }

    public static boolean hasEchoRealmCore(ServerPlayer player) {
        return EchoAbilityManager.hasActivatedMechanism(player, T7_CORE_KEY);
    }

    public static boolean adminSetT7ClaimState(ServerPlayer player, String state, boolean grant) {
        String key = switch (state) {
            case "reward" -> T7_REWARD_KEY;
            case "core" -> T7_CORE_KEY;
            case "deep_clue" -> T7_CLUE_MAP_KEY;
            case "index" -> INDEX_UNLOCK_KEY;
            default -> null;
        };
        if (key == null) {
            return false;
        }
        if (grant) {
            EchoAbilityManager.activateMechanism(player, key);
            if (state.equals("reward")) {
                EchoAbilityManager.activateMechanism(player, INDEX_UNLOCK_KEY);
            }
        } else {
            EchoAbilityManager.deactivateMechanism(player, key);
        }
        EchoAbilityManager.syncToClient(player);
        JournalManager.syncJournal(player);
        return true;
    }

    private static boolean hasClaimedT7(ServerPlayer player) {
        return EchoAbilityManager.getData(player).getActivatedMechanisms().contains(T7_REWARD_KEY);
    }

    private static void claimT7Reward(ServerPlayer player) {
        EchoAbilityManager.activateMechanism(player, T7_REWARD_KEY);
        EchoAbilityManager.activateMechanism(player, INDEX_UNLOCK_KEY);
        claimSupplementalRewards(player);
        ModAdvancements.award(player, ModAdvancements.ECHO_REALM_GRADUATION);
        ModAdvancements.award(player, ModAdvancements.ECHO_REALM_COMPLETE);
        EchoAbilityManager.syncToClient(player);
        JournalManager.syncJournal(player);
        player.sendSystemMessage(Component.translatable("message.unknown_echoes.completion.reward"));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void claimIndexExpansionRewards(ServerPlayer player) {
        boolean changed = false;
        if (canClaimIndexExpansionOne(player) && !hasIndexExpansionOne(player)) {
            changed |= grantIndexReward(player, INDEX_EXPANSION_ONE_KEY,
                    List.of(new ItemStack(ModItems.ECHO_MARK.get(), 4),
                            new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 4),
                            new ItemStack(ModItems.ECHO_CORE.get(), 2)),
                    "message.unknown_echoes.index.expansion1");
        }
        if (canClaimIndexExpansionTwo(player) && !hasIndexExpansionTwo(player)) {
            changed |= grantIndexReward(player, INDEX_EXPANSION_TWO_KEY,
                    List.of(new ItemStack(ModItems.CLUE_MAP.get(), 1),
                            new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 6)),
                    "message.unknown_echoes.index.expansion2");
        }
        if (changed) {
            EchoAbilityManager.syncToClient(player);
            JournalManager.syncJournal(player);
        }
    }

    private static boolean canClaimIndexExpansionOne(ServerPlayer player) {
        return hasEchoIndexUnlocked(player)
                && pagesRead(player) >= INDEX_EXPANSION_ONE_PAGES
                && visitedBiomes(player) >= REQUIRED_BIOMES;
    }

    private static boolean canClaimIndexExpansionTwo(ServerPlayer player) {
        return hasIndexExpansionOne(player)
                && pagesRead(player) >= DEEP_INDEX_PAGES
                && completionScore(player) >= DEEP_INDEX_SCORE;
    }

    private static boolean hasIndexExpansionOne(ServerPlayer player) {
        return EchoAbilityManager.hasActivatedMechanism(player, INDEX_EXPANSION_ONE_KEY);
    }

    private static boolean hasIndexExpansionTwo(ServerPlayer player) {
        return EchoAbilityManager.hasActivatedMechanism(player, INDEX_EXPANSION_TWO_KEY);
    }

    private static boolean grantIndexReward(ServerPlayer player, String key,
                                            List<ItemStack> rewards, String messageKey) {
        if (!canFitAllRewards(player, rewards)) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.index.need_space"), true);
            return false;
        }
        EchoAbilityManager.activateMechanism(player, key);
        for (ItemStack stack : rewards) {
            player.getInventory().add(stack);
        }
        player.displayClientMessage(Component.translatable(messageKey), true);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 0.8F);
        return true;
    }

    private static String backfillTarget(ServerPlayer player) {
        if (pagesRead(player) < JournalManager.KNOWN_PAGES) {
            return "pages";
        }
        if (miniBossKills(player) < MINI_BOSSES.size()) {
            return "mini_bosses";
        }
        if (artifactUpgradeSteps(player) < ArtifactType.values().length * (ArtifactType.MAX_LEVEL - 1)) {
            return "artifacts";
        }
        if (visitedBiomes(player) < EchoRealmBiomeCatalog.REQUIRED_COUNT) {
            return "biomes";
        }
        return "ready";
    }

    private static void claimSupplementalRewards(ServerPlayer player) {
        boolean pending = false;
        if (!EchoAbilityManager.hasActivatedMechanism(player, T7_CORE_KEY)) {
            pending |= !giveSupplement(player, T7_CORE_KEY,
                    new ItemStack(ModItems.ECHO_REALM_CORE.get(), 1));
        }
        if (!EchoAbilityManager.hasActivatedMechanism(player, T7_INGOTS_KEY)) {
            pending |= !giveSupplement(player, T7_INGOTS_KEY,
                    new ItemStack(ModItems.ECHO_REALM_INGOT.get(), 4));
        }
        if (!EchoAbilityManager.hasActivatedMechanism(player, T7_CLUE_MAP_KEY)) {
            pending |= !giveSupplement(player, T7_CLUE_MAP_KEY,
                    new ItemStack(ModItems.CLUE_MAP.get(), 1));
        }
        if (pending) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.completion.need_space"), true);
        }
    }

    private static boolean giveSupplement(ServerPlayer player, String key, ItemStack stack) {
        if (!canFitEntireStack(player, stack)) {
            return false;
        }
        player.getInventory().add(stack);
        EchoAbilityManager.activateMechanism(player, key);
        return true;
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

    private static boolean canFitAllRewards(ServerPlayer player, List<ItemStack> rewards) {
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack slot : player.getInventory().items) {
            slots.add(slot.copy());
        }
        for (ItemStack reward : rewards) {
            if (!simulateInsert(slots, reward.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean simulateInsert(List<ItemStack> slots, ItemStack reward) {
        int remaining = reward.getCount();
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, reward)) {
                int move = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                if (move > 0) {
                    slot.grow(move);
                    remaining -= move;
                }
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            if (remaining <= 0) {
                return true;
            }
            if (slots.get(i).isEmpty()) {
                ItemStack placed = reward.copy();
                int move = Math.min(remaining, placed.getMaxStackSize());
                placed.setCount(move);
                slots.set(i, placed);
                remaining -= move;
            }
        }
        return remaining <= 0;
    }

    private static Component doneLine(String key, boolean done) {
        return Component.translatable(key, mark(done))
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component countLine(String key, int have, int need) {
        boolean done = have >= need;
        return Component.translatable(key, mark(done), have, need)
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component thresholdLine(String key, int have, int total, int need) {
        boolean done = have >= need;
        return Component.translatable(key, mark(done), have, total, need)
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static void addDeepCheck(List<Component> lines, String key, int have, int need) {
        if (have < need) {
            lines.add(Component.translatable(key, have, need).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component mark(boolean done) {
        return Component.translatable(done ? "completion.unknown_echoes.done" : "completion.unknown_echoes.todo");
    }
}
