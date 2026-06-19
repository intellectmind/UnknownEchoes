package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.ability.EchoAbilityData;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.beacon.EchoBeaconBlock;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;

/**
 * /unknownechoes 命令:
 * journal —— 查看自己的探索日志(所有玩家可用)
 * ability grant|revoke|list、realm tp —— 调试用(权限等级 2)
 */
public class EchoCommands {

    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(EchoAbilityType.values()).map(EchoAbilityType::getId), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unknownechoes")
                .then(Commands.literal("journal")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            for (Component line : JournalManager.buildSummary(player)) {
                                player.sendSystemMessage(line);
                            }
                            return 1;
                        }))
                .then(Commands.literal("ability")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests(ABILITY_SUGGESTIONS)
                                                .executes(ctx -> grantAbility(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "ability"), true)))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests(ABILITY_SUGGESTIONS)
                                                .executes(ctx -> grantAbility(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "ability"), false)))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> listAbilities(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"))))))
                .then(Commands.literal("realm")
                        .then(Commands.literal("tp")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    EchoBeaconBlock.teleportToRealm(player);
                                    return 1;
                                }))));
    }

    private static int grantAbility(CommandSourceStack source, Collection<ServerPlayer> targets,
                                    String abilityId, boolean grant) {
        EchoAbilityType ability = EchoAbilityType.byId(abilityId);
        if (ability == null) {
            source.sendFailure(Component.literal("Unknown ability: " + abilityId));
            return 0;
        }
        for (ServerPlayer player : targets) {
            if (grant) {
                EchoAbilityManager.unlockAbility(player, ability);
            } else {
                EchoAbilityManager.revokeAbility(player, ability);
            }
        }
        source.sendSuccess(() -> Component.literal((grant ? "Granted " : "Revoked ")
                + ability.getId() + " for " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int listAbilities(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityData data = EchoAbilityManager.getData(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                + " abilities=" + data.getUnlockedAbilities()
                + " beacons=" + data.getActivatedBeacons()
                + " bosses=" + data.getDefeatedBosses()
                + " dimensions=" + data.getUnlockedDimensions()), false);
        return 1;
    }
}
