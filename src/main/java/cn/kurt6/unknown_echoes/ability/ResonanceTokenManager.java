package cn.kurt6.unknown_echoes.ability;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.item.InventoryPayment;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class ResonanceTokenManager {
    private static final String PENDING_TOKEN_RETURN_PREFIX = "ue_pending_resonance_token_";

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
            case GRAVITY_ECHO -> ModItems.GRAVITY_RESONANCE_TOKEN.get();
            case VOID_ECHO -> ModItems.VOID_RESONANCE_TOKEN.get();
            default -> null;
        };
    }

    /** 机关完成时调用:写仪式记录并发放信物(已有能力的玩家只补记录,不再发信物)。 */
    public static boolean grantRitual(ServerPlayer player, EchoAbilityType ability,
                                      String... completionKeys) {
        boolean firstTime = !hasRitualRecord(player, ability);
        String[] records = new String[completionKeys.length + 1];
        records[0] = ritualKey(ability);
        System.arraycopy(completionKeys, 0, records, 1, completionKeys.length);
        boolean[] tokenResult = new boolean[2];
        if (!EchoAbilityManager.activateMechanisms(player, () -> {
            if (!EchoAbilityManager.hasAbility(player, ability) && !hasValidToken(player, ability)) {
                tokenResult[0] = true;
                tokenResult[1] = issueToken(player, ability);
            }
        }, records)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.progress_record.full"), true);
            return false;
        }
        if (firstTime || completionKeys.length > 0) {
            EchoAbilityManager.syncToClient(player);
        }
        if (tokenResult[0]) {
            if (tokenResult[1]) {
                player.sendSystemMessage(Component.translatable(
                        "message.unknown_echoes.token.granted",
                        Component.translatable("item.unknown_echoes." + tokenId(ability))));
                player.playNotifySound(ModSounds.FEEDBACK_TOKEN_GRANTED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                player.displayClientMessage(
                        Component.translatable(tokenIssueFailureKey(player, ability)), true);
            }
        }
        return true;
    }

    public static boolean reissueIfMissing(ServerPlayer player, EchoAbilityType ability) {
        if (!hasRitualRecord(player, ability)
                || EchoAbilityManager.hasAbility(player, ability)
                || hasValidToken(player, ability)) {
            return false;
        }
        if (!issueToken(player, ability)) {
            player.displayClientMessage(
                    Component.translatable(tokenIssueFailureKey(player, ability)), true);
            return false;
        }
        player.sendSystemMessage(Component.translatable(
                "message.unknown_echoes.token.reissued",
                Component.translatable("item.unknown_echoes." + tokenId(ability))));
        player.playNotifySound(ModSounds.FEEDBACK_TOKEN_GRANTED.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        return true;
    }

    /** 信物校验:能力 id 与凭据序号都要与玩家数据一致(他人信物/旧信物不通过)。 */
    public static boolean isValidToken(ServerPlayer player, EchoAbilityType ability, ItemStack stack) {
        if (!hasRitualRecord(player, ability)) {
            return false;
        }
        Item item = tokenItem(ability);
        if (item == null || !stack.is(item)) {
            return false;
        }
        String id = stack.get(ModDataComponents.TOKEN_ABILITY.get());
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        int currentSerial = EchoAbilityManager.getData(player).getTokenSerial(ability);
        if (!ability.getId().equals(id) || serial == null
                || currentSerial <= 0 || serial != currentSerial) {
            return false;
        }
        String owner = stack.get(ModDataComponents.TOKEN_OWNER.get());
        if (owner == null || owner.isBlank()) {
            stack.set(ModDataComponents.TOKEN_OWNER.get(), player.getUUID().toString());
            return true;
        }
        return owner.equals(player.getUUID().toString());
    }

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

    /** 当前玩家正在持有的有效共鸣信物。旧序号/他人信物可正常丢弃清理。 */
    public static boolean isCurrentToken(ServerPlayer player, ItemStack stack) {
        EchoAbilityType ability = abilityFromTokenItem(stack);
        return ability != null && isValidToken(player, ability, stack);
    }

    /** 把绑定信物放回玩家背包或副手。用于丢弃/死亡保护;落点判据见 InventoryPayment.placeBound。 */
    public static boolean returnTokenToInventory(ServerPlayer player, ItemStack stack) {
        return InventoryPayment.placeBound(player, stack);
    }


    /** 丢弃事件已先移除物品；先记录待归还状态，再尝试立即放回背包。 */
    public static boolean returnTossedToken(ServerPlayer player, ItemStack stack) {
        EchoAbilityType ability = abilityFromTokenItem(stack);
        if (ability == null || !isValidToken(player, ability, stack)) {
            return false;
        }
        String key = pendingTokenReturnKey(ability);
        player.getPersistentData().putBoolean(key, true);
        if (!returnTokenToInventory(player, stack.copyWithCount(1))) {
            return false;
        }
        player.getPersistentData().remove(key);
        return true;
    }

    /** 死亡掉落移除前记录待归还类型；信物可由个人仪式记录和当前序号重建。 */
    public static void queueTokenReturn(ServerPlayer player, ItemStack stack) {
        EchoAbilityType ability = abilityFromTokenItem(stack);
        if (ability != null && isValidToken(player, ability, stack)) {
            player.getPersistentData().putBoolean(pendingTokenReturnKey(ability), true);
        }
    }

    /** 登录、重生及背包腾位后重试；返回 true 表示仍有信物等待空位。 */
    public static boolean restorePendingTokenReturns(ServerPlayer player) {
        boolean remaining = false;
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            String key = pendingTokenReturnKey(ability);
            if (!player.getPersistentData().getBoolean(key)) {
                continue;
            }
            if (!hasRitualRecord(player, ability)
                    || EchoAbilityManager.getData(player).getTokenSerial(ability) <= 0) {
                player.getPersistentData().remove(key);
                continue;
            }
            normalizeTokens(player, ability);
            if (hasValidToken(player, ability)) {
                player.getPersistentData().remove(key);
                continue;
            }
            Item item = tokenItem(ability);
            if (item != null && returnTokenToInventory(player, makeToken(player, ability, item))) {
                player.getPersistentData().remove(key);
            } else {
                remaining = true;
            }
        }
        return remaining;
    }

    /** NeoForge persistent data 不自动跨死亡克隆，待归还标记必须显式复制。 */
    public static void copyPendingTokenReturns(Player original, Player replacement) {
        for (EchoAbilityType ability : EchoAbilityType.values()) {
            String key = pendingTokenReturnKey(ability);
            if (original.getPersistentData().getBoolean(key)) {
                replacement.getPersistentData().putBoolean(key, true);
            }
        }
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

    private static boolean issueToken(ServerPlayer player, EchoAbilityType ability) {
        Item item = tokenItem(ability);
        if (item == null) {
            return false;
        }
        normalizeTokens(player, ability);
        if (!hasTokenSpace(player)) {
            return false;
        }
        int serial = EchoAbilityManager.getData(player).nextTokenSerial(ability);
        if (serial <= 0) {
            return false;
        }
        return returnTokenToInventory(player, makeToken(player, ability, item, serial));
    }

    private static String tokenIssueFailureKey(ServerPlayer player, EchoAbilityType ability) {
        return EchoAbilityManager.getData(player).getTokenSerial(ability) == Integer.MAX_VALUE
                ? "message.unknown_echoes.token.serial_exhausted"
                : "message.unknown_echoes.token.need_space";
    }

    private static ItemStack makeToken(ServerPlayer player, EchoAbilityType ability, Item item) {
        return makeToken(player, ability, item,
                EchoAbilityManager.getData(player).getTokenSerial(ability));
    }

    private static ItemStack makeToken(ServerPlayer player, EchoAbilityType ability, Item item, int serial) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.TOKEN_ABILITY.get(), ability.getId());
        stack.set(ModDataComponents.CREDENTIAL_SERIAL.get(), serial);
        stack.set(ModDataComponents.TOKEN_OWNER.get(), player.getUUID().toString());
        return stack;
    }

    private static String pendingTokenReturnKey(EchoAbilityType ability) {
        return PENDING_TOKEN_RETURN_PREFIX + ability.getId();
    }

    private static boolean hasTokenSpace(ServerPlayer player) {
        return InventoryPayment.hasBoundItemSpace(player);
    }


    private static void normalizeTokens(ServerPlayer player, EchoAbilityType ability) {
        boolean foundValid = normalizeTokenList(player, ability, player.getInventory().items, false);
        normalizeTokenList(player, ability, player.getInventory().offhand, foundValid);
        player.getInventory().setChanged();
    }

    private static boolean normalizeTokenList(ServerPlayer player, EchoAbilityType ability,
                                              List<ItemStack> stacks, boolean foundValid) {
        Item item = tokenItem(ability);
        if (item == null) {
            return foundValid;
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.is(item)) {
                continue;
            }
            boolean valid = isValidToken(player, ability, stack);
            if (valid && !foundValid) {
                foundValid = true;
            } else {
                stacks.set(i, ItemStack.EMPTY);
            }
        }
        return foundValid;
    }

    @Nullable
    private static EchoAbilityType abilityFromTokenItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof cn.kurt6.unknown_echoes.item.ResonanceTokenItem token)) {
            return null;
        }
        return token.getAbility();
    }

    private static String tokenId(EchoAbilityType ability) {
        return switch (ability) {
            case WIND_ECHO -> "wind_resonance_token";
            case TIDE_ECHO -> "tide_resonance_token";
            case TRUE_SIGHT_ECHO -> "true_sight_resonance_token";
            case GRAVITY_ECHO -> "gravity_resonance_token";
            case VOID_ECHO -> "void_resonance_token";
            default -> "wind_resonance_token";
        };
    }
}
