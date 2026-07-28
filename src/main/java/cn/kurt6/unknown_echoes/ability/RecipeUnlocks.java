package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * 配方解锁链(V0.6B,11.7 既定规则落地):
 * 能力研究不再驱动武器配方;武器进度只由图纸、材料与合成线承担。
 * 配方书只负责显示；深层装备/武器按材料链解锁，不把能力等级当作合成权限。
 * 当前实现基线:配方书可见性解锁;硬性合成封锁仅在 doLimitedCrafting 规则开启时由原版生效。
 */
public final class RecipeUnlocks {

    private record Entry(String unlockKey, List<ResourceLocation> recipeIds,
                         java.util.function.Predicate<ServerPlayer> condition) {}

    /** 任一回响能力已解锁(聆听者套装的中期门槛,11.3 材料线 V0.5-V0.6)。 */
    private static final java.util.function.Predicate<ServerPlayer> ANY_ABILITY =
            player -> EchoAbilityManager.hasAbility(player, EchoAbilityType.WIND_ECHO)
                    || EchoAbilityManager.hasAbility(player, EchoAbilityType.TIDE_ECHO)
                    || EchoAbilityManager.hasAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);

    private static final List<Entry> ENTRIES = List.of(
            // 聆听者套装(V0.6F,11.3):四件随任一能力解锁,一次性提示
            new Entry("listener_set", List.of(
                    UnknownEchoes.id("listener_helmet"), UnknownEchoes.id("listener_chestplate"),
                    UnknownEchoes.id("listener_leggings"), UnknownEchoes.id("listener_boots")),
                    ANY_ABILITY),
            new Entry("deep_gravity_equipment", List.of(
                    UnknownEchoes.id("deep_alloy_ingot"),
                    UnknownEchoes.id("abyss_walker_helmet"), UnknownEchoes.id("abyss_walker_chestplate"),
                    UnknownEchoes.id("abyss_walker_leggings"), UnknownEchoes.id("abyss_walker_boots")),
                    player -> hasInInventory(player, ModItems.DEEP_ORE_SCRAP.get())
                            && hasInInventory(player, ModItems.SPACE_ANCHOR_SHARD.get())
                            && hasInInventory(player, ModItems.FOLDED_CARAPACE.get())),
            new Entry("gravity_hammer", List.of(UnknownEchoes.id("gravity_hammer")),
                    player -> hasInInventory(player, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(player, ModItems.GRAVITY_CORE_FRAGMENT.get())),
            new Entry("rift_dagger", List.of(UnknownEchoes.id("rift_dagger")),
                    player -> hasInInventory(player, ModItems.DEEP_ALLOY_INGOT.get())
                            && hasInInventory(player, ModItems.VOID_RIFT_SHARD.get()))
    );

    private RecipeUnlocks() {
    }

    /** 能力/研究进度变化后核对一次;条件满足即解锁,已解锁的静默跳过。 */
    public static void checkAndUnlock(ServerPlayer player) {
        for (Entry entry : ENTRIES) {
            String mechanismKey = "recipe_unlock:" + entry.unlockKey();
            if (EchoAbilityManager.hasActivatedMechanism(player, mechanismKey)
                    || !entry.condition().test(player)) {
                continue;
            }
            List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders = entry.recipeIds().stream()
                    .flatMap(id -> player.server.getRecipeManager().byKey(id).stream())
                    .collect(java.util.stream.Collectors.toList());
            if (holders.isEmpty()) {
                continue;
            }
            EchoAbilityManager.activateMechanism(player, mechanismKey);
            player.awardRecipes(holders);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_IN, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.recipe_unlocked." + entry.unlockKey()));
        }
    }

    private static boolean hasInInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().contains(item.getDefaultInstance());
    }
}
