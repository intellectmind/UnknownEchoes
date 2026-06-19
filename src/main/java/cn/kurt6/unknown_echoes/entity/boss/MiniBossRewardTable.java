package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * 第一维度 Mini Boss 结算表。首杀关键材料和残页走个人安全发放,重复挑战只给普通材料。
 */
final class MiniBossRewardTable {
    private MiniBossRewardTable() {
    }

    static void grantStormWeaver(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.STORM_SPINDLE, 1, 23, ModItems.WIND_ENHANCE_CORE,
                ModItems.CRYSTAL_FEATHER, ModItems.WINDWOVEN_THREAD, ModItems.WIND_ENHANCE_CORE));
    }

    static void grantTideLanternKeeper(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.BROKEN_LANTERN_LENS, 1, 43, ModItems.TIDE_UPGRADE_CORE,
                ModItems.TIDE_PEARL, ModItems.MIRROR_TIDE_ALLOY_PLATE, ModItems.TIDE_UPGRADE_CORE));
    }

    static void grantMirrorDustButler(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.MIRROR_STAFF_CORE, 1, 56, ModItems.TRUE_SIGHT_UPGRADE_CORE,
                ModItems.POLISHED_MIRROR_DUST, ModItems.DECOY_SHARD, ModItems.TRUE_SIGHT_UPGRADE_CORE));
    }

    static void grantSilentPriest(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.SILENT_CANDLE_WICK, 1, 70, ModItems.ATTUNED_ECHO_GEM,
                ModItems.SILENT_RESIN, ModItems.MOSS_SHELL_PLATE, null));
    }

    static void grantCrystalSongkeeper(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.SONGKEEPER_FORK, 1, 80, ModItems.ATTUNED_ECHO_GEM,
                ModItems.RESONANT_CRYSTAL_NOTE, ModItems.CRYSTAL_RESONANCE_ROD, null));
    }

    static void grantBrokenBellKeeper(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.BELL_CLAPPER, 1, 92, ModItems.ATTUNED_ECHO_GEM,
                ModItems.RUSTED_TIME_GEAR, ModItems.BROKEN_CLOCKWORK_PLATE, null));
    }

    static void grantDreamBloomKeeper(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.DREAM_MIST_VINE, 2, 86, ModItems.ATTUNED_ECHO_GEM,
                ModItems.DREAM_NECTAR, ModItems.ECHO_AMBER, null));
    }

    static void grantLostRecorderChief(MiniBossEntity boss, ServerPlayer player, boolean firstKill) {
        grant(boss, player, firstKill, new Spec(
                ModItems.ARCHIVE_BINDING, 1, 104, ModItems.ATTUNED_ECHO_GEM,
                ModItems.ECHO_INDEX, ModItems.RECORD_TRACING_PAPER, null));
    }

    private static void grant(MiniBossEntity boss, ServerPlayer player, boolean firstKill, Spec spec) {
        if (firstKill) {
            boss.giveFirstKillItem(player, new ItemStack(spec.firstKillMaterial().get(), spec.firstKillCount()));
            boss.giveFirstKillItem(player, ancientPage(spec.pageId()));
            boss.giveFirstKillItem(player, new ItemStack(spec.primaryReward().get()));
            boss.giveFirstKillItem(player, new ItemStack(ModItems.TRAVELER_BADGE.get()));
            return;
        }

        MiniBossEntity.giveItem(player, new ItemStack(spec.regionMaterial().get(), randomBetween(boss, 3, 6)));
        MiniBossEntity.giveItem(player, new ItemStack(spec.equipmentMaterial().get(), randomBetween(boss, 1, 3)));
        if (spec.abilityCore() != null && roll(boss, 0.25F)) {
            MiniBossEntity.giveItem(player, new ItemStack(spec.abilityCore().get()));
        }
        if (roll(boss, 0.20F)) {
            MiniBossEntity.giveItem(player, new ItemStack(ModItems.ATTUNED_ECHO_GEM.get()));
        }
        if (roll(boss, 0.35F)) {
            MiniBossEntity.giveItem(player, new ItemStack(ModItems.TRAVELER_BADGE.get()));
        }
        if (roll(boss, 0.20F)) {
            MiniBossEntity.giveItem(player, ancientPage(spec.pageId()));
        }
        if (roll(boss, 0.08F)) {
            MiniBossEntity.giveItem(player, new ItemStack(ModItems.ECHO_MARK.get()));
        }
    }

    private static ItemStack ancientPage(int pageId) {
        ItemStack stack = new ItemStack(ModItems.ANCIENT_PAGE.get());
        stack.set(ModDataComponents.PAGE_ID.get(), pageId);
        return stack;
    }

    private static int randomBetween(MiniBossEntity boss, int min, int max) {
        return min + boss.getRandom().nextInt(max - min + 1);
    }

    private static boolean roll(MiniBossEntity boss, float chance) {
        return boss.getRandom().nextFloat() < chance;
    }

    private record Spec(Supplier<? extends ItemLike> firstKillMaterial,
                        int firstKillCount,
                        int pageId,
                        Supplier<? extends Item> primaryReward,
                        Supplier<? extends ItemLike> regionMaterial,
                        Supplier<? extends ItemLike> equipmentMaterial,
                        Supplier<? extends Item> abilityCore) {
    }
}
