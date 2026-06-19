package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * 配方解锁链(V0.6B,11.7 既定规则落地):
 * 能力研究不再驱动武器配方;武器进度只由图纸、材料与合成线承担。
 * 这里仅保留非武器的便利配方书提示,避免把能力升级误做成武器升级树。
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
                    ANY_ABILITY)
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
}
