package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 共鸣信物仪式统一入口(V0.6E,设计文档 5.1.1)。
 * 权威判定永远是服务端个人双记录(机关完成 + Boss 击败);信物只是仪式载体:
 * - 机关完成 → 写个人"仪式记录" + 发信物(凭据归属同神器序号方案,他人/旧信物无效);
 * - 信物丢失不卡进度:回到机关核心处免费复领(序号自增,旧信物作废);
 * - 祭坛吸收时消耗信物,双记录齐全才写入能力(红线 #2:信物不是权威状态)。
 */
public class ResonanceTokenManager {

    /** 个人仪式记录键:该玩家完成过此能力的核心机关(不绑定坐标——机关本体判定仍按坐标)。 */
    public static String ritualKey(EchoAbilityType ability) {
        return "resonance_ritual:" + ability.getId();
    }

    public static boolean hasRitualRecord(ServerPlayer player, EchoAbilityType ability) {
        return EchoAbilityManager.hasActivatedMechanism(player, ritualKey(ability));
    }

    @Nullable
    public static Item tokenItem(EchoAbilityType ability) {
        return switch (ability) {
            case WIND_ECHO -> ModItems.WIND_RESONANCE_TOKEN.get();
            case TIDE_ECHO -> ModItems.TIDE_RESONANCE_TOKEN.get();
            case TRUE_SIGHT_ECHO -> ModItems.TRUE_SIGHT_RESONANCE_TOKEN.get();
            default -> null;
        };
    }

    /** 机关完成时调用:写仪式记录并发放信物(已有能力的玩家只补记录,不再发信物)。 */
    public static void grantRitual(ServerPlayer player, EchoAbilityType ability) {
        boolean firstTime = !hasRitualRecord(player, ability);
        if (firstTime) {
            EchoAbilityManager.activateMechanism(player, ritualKey(ability));
            EchoAbilityManager.syncToClient(player);
        }
        if (EchoAbilityManager.hasAbility(player, ability)) {
            return;
        }
        if (firstTime || !hasValidToken(player, ability)) {
            issueToken(player, ability);
            player.sendSystemMessage(Component.translatable(
                    "message.unknown_echoes.token.granted",
                    Component.translatable("item.unknown_echoes." + tokenId(ability))));
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /**
     * 机关核心处免费复领(5.1.1:"机关认得你,重新递出了信物")。
     * 仅在仪式记录在册、能力未解锁、身上没有效信物时发放。
     *
     * @return true = 本次发放了信物(调用方应结束本次交互)
     */
    public static boolean reissueIfMissing(ServerPlayer player, EchoAbilityType ability) {
        if (!hasRitualRecord(player, ability)
                || EchoAbilityManager.hasAbility(player, ability)
                || hasValidToken(player, ability)) {
            return false;
        }
        issueToken(player, ability);
        player.displayClientMessage(
                Component.translatable("message.unknown_echoes.token.reissued"), true);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.3F);
        return true;
    }

    /** 信物校验:能力 id 与凭据序号都要与玩家数据一致(他人信物/旧信物不通过)。 */
    public static boolean isValidToken(ServerPlayer player, EchoAbilityType ability, ItemStack stack) {
        Item item = tokenItem(ability);
        if (item == null || !stack.is(item)) {
            return false;
        }
        String id = stack.get(ModDataComponents.TOKEN_ABILITY.get());
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        return ability.getId().equals(id) && serial != null
                && serial == EchoAbilityManager.getData(player).getTokenSerial(ability);
    }

    /** 背包(含副手)里是否持有效信物。 */
    public static boolean hasValidToken(ServerPlayer player, EchoAbilityType ability) {
        return findToken(player, ability) != null;
    }

    /** 祭坛吸收:消耗一枚有效信物。调用前先确认 hasValidToken。 */
    public static boolean consumeToken(ServerPlayer player, EchoAbilityType ability) {
        ItemStack token = findToken(player, ability);
        if (token == null) {
            return false;
        }
        token.shrink(1);
        return true;
    }

    @Nullable
    private static ItemStack findToken(ServerPlayer player, EchoAbilityType ability) {
        ItemStack offhand = player.getOffhandItem();
        if (isValidToken(player, ability, offhand)) {
            return offhand;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isValidToken(player, ability, stack)) {
                return stack;
            }
        }
        return null;
    }

    private static void issueToken(ServerPlayer player, EchoAbilityType ability) {
        Item item = tokenItem(ability);
        if (item == null) {
            return;
        }
        int serial = EchoAbilityManager.getData(player).nextTokenSerial(ability);
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.TOKEN_ABILITY.get(), ability.getId());
        stack.set(ModDataComponents.CREDENTIAL_SERIAL.get(), serial);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static String tokenId(EchoAbilityType ability) {
        return switch (ability) {
            case WIND_ECHO -> "wind_resonance_token";
            case TIDE_ECHO -> "tide_resonance_token";
            case TRUE_SIGHT_ECHO -> "true_sight_resonance_token";
            default -> "wind_resonance_token";
        };
    }
}
