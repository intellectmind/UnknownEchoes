package cn.kurt6.unknown_echoes.entity.boss;

import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/** Mini Boss 结算表。每场战斗只在 Boss 位置生成一套掉落。 */
final class MiniBossRewardTable {
    private MiniBossRewardTable() {
    }

    static void grantStormWeaver(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.STORM_SPINDLE, 1, 23, ModItems.WIND_ENHANCE_CORE,
                ModItems.CRYSTAL_FEATHER, ModItems.WINDWOVEN_THREAD, ModItems.WIND_ENHANCE_CORE));
        grantEchoDust(boss);
        if (roll(boss, 0.60F)) {
            drop(boss, new ItemStack(boss.getRandom().nextInt(5) < 3
                    ? ModItems.CRYSTAL_FEATHER.get() : ModItems.WIND_RUNE_SHARD.get()));
        }
        if (firstKill) {
            drop(boss, new ItemStack(ModItems.STORM_COMPASS_PART.get(), 2));
        } else if (roll(boss, 0.20F)) {
            drop(boss, new ItemStack(ModItems.STORM_COMPASS_PART.get()));
        }
    }

    static void grantTideLanternKeeper(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.BROKEN_LANTERN_LENS, 1, 43, ModItems.TIDE_UPGRADE_CORE,
                ModItems.TIDE_PEARL, ModItems.MIRROR_TIDE_ALLOY_PLATE, ModItems.TIDE_UPGRADE_CORE));
        grantEchoDust(boss);
        if (roll(boss, 0.60F)) {
            int choice = boss.getRandom().nextInt(6);
            if (choice < 3) {
                drop(boss, new ItemStack(ModItems.TIDE_RUNE_FRAGMENT.get(),
                        randomBetween(boss, 1, 2)));
            } else if (choice == 3) {
                drop(boss, new ItemStack(ModItems.TIDE_PEARL.get()));
            } else {
                drop(boss, new ItemStack(ModItems.MIRROR_LAKE_SHARD.get()));
            }
        }
    }

    static void grantMirrorDustButler(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.MIRROR_STAFF_CORE, 1, 56, ModItems.TRUE_SIGHT_UPGRADE_CORE,
                ModItems.POLISHED_MIRROR_DUST, ModItems.DECOY_SHARD, ModItems.TRUE_SIGHT_UPGRADE_CORE));
        if (firstKill) {
            drop(boss, new ItemStack(ModItems.DECOY_SHARD.get(), 2));
        }
        grantEchoDust(boss);
        if (roll(boss, 0.60F)) {
            if (boss.getRandom().nextInt(5) < 3) {
                drop(boss, new ItemStack(ModItems.ILLUSION_DUST.get(), randomBetween(boss, 1, 2)));
            } else {
                drop(boss, new ItemStack(ModItems.MIRROR_LAKE_SHARD.get()));
            }
        }
    }

    static void grantSilentPriest(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.SILENT_CANDLE_WICK, 1, 70, ModItems.ATTUNED_ECHO_GEM,
                ModItems.SILENT_RESIN, ModItems.MOSS_SHELL_PLATE, null));
        grantEchoDust(boss);
        if (roll(boss, 0.60F)) {
            if (boss.getRandom().nextInt(5) < 3) {
                drop(boss, new ItemStack(ModItems.SILENCE_MOSS.get(), randomBetween(boss, 1, 2)));
            } else {
                drop(boss, new ItemStack(ModItems.SILENT_POD.get()));
            }
        }
    }

    static void grantCrystalSongkeeper(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.SONGKEEPER_FORK, 1, 80, ModItems.ATTUNED_ECHO_GEM,
                ModItems.RESONANT_CRYSTAL_NOTE, ModItems.CRYSTAL_RESONANCE_ROD, null));
        grantEchoDust(boss);
        if (roll(boss, 0.55F)) {
            drop(boss, new ItemStack(ModItems.RESONANT_CRYSTAL_NOTE.get()));
        }
    }

    static void grantBrokenBellKeeper(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.BELL_CLAPPER, 1, 92, ModItems.ATTUNED_ECHO_GEM,
                ModItems.RUSTED_TIME_GEAR, ModItems.BROKEN_CLOCKWORK_PLATE, null));
        grantEchoDust(boss);
        if (roll(boss, 0.55F)) {
            drop(boss, new ItemStack(ModItems.BROKEN_BELL_GEAR.get()));
        }
    }

    static void grantDreamBloomKeeper(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.DREAM_MIST_VINE, 2, 86, ModItems.ATTUNED_ECHO_GEM,
                ModItems.DREAM_NECTAR, ModItems.ECHO_AMBER, null));
        drop(boss, new ItemStack(ModItems.DREAM_NECTAR.get(), randomBetween(boss, 1, 3)));
        if (roll(boss, 0.35F)) {
            drop(boss, new ItemStack(ModItems.ECHO_AMBER.get()));
        }
    }

    static void grantLostRecorderChief(MiniBossEntity boss, boolean firstKill) {
        grant(boss, firstKill, new Spec(
                ModItems.ARCHIVE_BINDING, 1, 104, ModItems.ATTUNED_ECHO_GEM,
                ModItems.BLANK_ECHO_PAGE, ModItems.RECORD_TRACING_PAPER, null));
        drop(boss, new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), randomBetween(boss, 1, 3)));
    }

    static void grantInvertedGatekeeper(MiniBossEntity boss, boolean firstKill) {
        if (firstKill) {
            drop(boss, new ItemStack(ModItems.SPACE_ANCHOR_SHARD.get(), 2));
            drop(boss, new ItemStack(ModItems.INVERTED_BOW_LIMB.get(), 2));
            drop(boss, ancientPage(146));
            drop(boss, new ItemStack(ModItems.GRAVITY_RESEARCH_RUBBING.get()));
        } else {
            drop(boss, new ItemStack(ModItems.SPACE_ANCHOR_SHARD.get(), randomBetween(boss, 1, 2)));
            drop(boss, new ItemStack(ModItems.INVERTED_BOW_LIMB.get(), randomBetween(boss, 1, 2)));
            grantDeepRepeatExtras(boss, 146);
        }
        drop(boss, new ItemStack(ModItems.DEEP_ORE_SCRAP.get(), randomBetween(boss, 2, 5)));
    }

    static void grantAshScribe(MiniBossEntity boss, boolean firstKill) {
        if (firstKill) {
            drop(boss, new ItemStack(ModItems.LAST_RECORD_FRAGMENT.get()));
            drop(boss, new ItemStack(ModItems.ASH_SCRIPT_MECHANISM.get(), 2));
            drop(boss, ancientPage(167));
            drop(boss, new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), 2));
        } else {
            drop(boss, new ItemStack(ModItems.RECORD_TRACING_PAPER.get(), randomBetween(boss, 1, 3)));
            drop(boss, new ItemStack(ModItems.ASH_SCRIPT_MECHANISM.get(), randomBetween(boss, 1, 2)));
            grantDeepRepeatExtras(boss, 167);
        }
        drop(boss, new ItemStack(ModItems.ECHO_INK_PAGE.get(), randomBetween(boss, 2, 4)));
    }

    private static void grantDeepRepeatExtras(MiniBossEntity boss, int pageId) {
        if (roll(boss, 0.30F)) {
            drop(boss, new ItemStack(ModItems.ATTUNED_ECHO_GEM.get()));
        }
        if (roll(boss, 0.20F)) {
            drop(boss, ancientPage(pageId));
        }
        if (roll(boss, 0.15F)) {
            drop(boss, new ItemStack(ModItems.ECHO_MARK.get()));
        }
    }

    private static void grant(MiniBossEntity boss, boolean firstKill, Spec spec) {
        if (firstKill) {
            drop(boss, new ItemStack(spec.firstKillMaterial().get(), spec.firstKillCount()));
            drop(boss, ancientPage(spec.pageId()));
            drop(boss, new ItemStack(spec.primaryReward().get()));
            drop(boss, new ItemStack(ModItems.TRAVELER_BADGE.get()));
            return;
        }

        drop(boss, new ItemStack(spec.regionMaterial().get(), randomBetween(boss, 3, 6)));
        drop(boss, new ItemStack(spec.equipmentMaterial().get(), randomBetween(boss, 1, 3)));
        if (spec.abilityCore() != null && roll(boss, 0.35F)) {
            drop(boss, new ItemStack(spec.abilityCore().get()));
        }
        if (roll(boss, 0.30F)) {
            drop(boss, new ItemStack(ModItems.ATTUNED_ECHO_GEM.get()));
        }
        if (roll(boss, 0.50F)) {
            drop(boss, new ItemStack(ModItems.TRAVELER_BADGE.get()));
        }
        if (roll(boss, 0.20F)) {
            drop(boss, ancientPage(spec.pageId()));
        }
        if (roll(boss, 0.15F)) {
            drop(boss, new ItemStack(ModItems.ECHO_MARK.get()));
        }
    }

    private static void grantEchoDust(MiniBossEntity boss) {
        drop(boss, new ItemStack(ModItems.ECHO_DUST.get(), randomBetween(boss, 1, 3)));
    }

    private static void drop(MiniBossEntity boss, ItemStack stack) {
        BossMaterialRewards.drop(boss, stack);
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
