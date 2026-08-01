package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.ability.EchoAbilityData;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.block.beacon.EchoBeaconBlock;
import cn.kurt6.unknown_echoes.block.deep.DeepRouteNodeBlock;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
 * ability grant|revoke|list、artifact grant、realm tp、deep tp —— 调试用(权限等级 2)
 */
public class EchoCommands {

    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(EchoAbilityType.values()).map(EchoAbilityType::getId), builder);

    private static final SuggestionProvider<CommandSourceStack> ARTIFACT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(ArtifactType.values()).map(ArtifactType::getId), builder);

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
                .then(Commands.literal("artifact")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("artifact", StringArgumentType.word())
                                                .suggests(ARTIFACT_SUGGESTIONS)
                                                .executes(ctx -> grantArtifact(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        StringArgumentType.getString(ctx, "artifact")))))))
                .then(Commands.literal("realm")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("tp")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                     EchoBeaconBlock.teleportToRealm(player);
                                     return 1;
                                 })))
                .then(Commands.literal("deep")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("tp")
                                .executes(ctx -> teleportToDeepEchoes(ctx.getSource())))
                        .then(Commands.literal("prepare")
                                .then(Commands.literal("void_prophet")
                                        .executes(ctx -> prepareVoidProphet(ctx.getSource(),
                                                java.util.List.of(ctx.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> prepareVoidProphet(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets")))))
                                .then(Commands.literal("rift_warden")
                                        .executes(ctx -> prepareRiftWarden(ctx.getSource(),
                                                java.util.List.of(ctx.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> prepareRiftWarden(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"))))))));
    }

    private static int grantAbility(CommandSourceStack source, Collection<ServerPlayer> targets,
                                    String abilityId, boolean grant) {
        EchoAbilityType ability = EchoAbilityType.byId(abilityId);
        if (ability == null) {
            source.sendFailure(Component.translatable("command.unknown_echoes.ability.unknown", abilityId));
            return 0;
        }
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (grant) {
                if (EchoAbilityManager.unlockAbility(player, ability)) {
                    changed++;
                }
            } else {
                EchoAbilityManager.revokeAbility(player, ability);
                changed++;
            }
        }
        if (changed == 0) {
            return 0;
        }
        int successCount = changed;
        source.sendSuccess(() -> Component.translatable(grant
                ? "command.unknown_echoes.ability.granted"
                : "command.unknown_echoes.ability.revoked",
                Component.translatable("ability.unknown_echoes." + ability.getId()), successCount), true);
        return changed;
    }

    private static int grantArtifact(CommandSourceStack source, Collection<ServerPlayer> targets,
                                     String artifactId) {
        ArtifactType artifact = ArtifactType.byId(artifactId);
        if (artifact == null) {
            source.sendFailure(Component.translatable("command.unknown_echoes.artifact.unknown", artifactId));
            return 0;
        }
        int granted = 0;
        for (ServerPlayer player : targets) {
            if (ArtifactManager.adminGrant(player, artifact)) {
                granted++;
            }
        }
        if (granted == 0) {
            source.sendFailure(Component.translatable("command.unknown_echoes.artifact.grant_failed",
                    Component.translatable("artifact.unknown_echoes." + artifact.getId())));
            return 0;
        }
        int successCount = granted;
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.artifact.granted",
                Component.translatable("artifact.unknown_echoes." + artifact.getId()),
                successCount, targets.size()), true);
        return granted;
    }

    private static int listAbilities(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityData data = EchoAbilityManager.getData(target);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.ability.list",
                target.getGameProfile().getName(), data.getUnlockedAbilities(), data.getActivatedBeacons(),
                data.getDefeatedBosses(), data.getUnlockedDimensions()), false);
        return 1;
    }

    private static int teleportToDeepEchoes(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!DeepEchoTeleporter.teleportForTesting(player)) {
            source.sendFailure(Component.translatable("command.unknown_echoes.deep_tp.failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.deep_tp.success"), false);
        return 1;
    }

    /** 测试入口：补齐裂隙看守者场地前置，但保留三座稳定器与祭坛仪式。 */
    private static int prepareRiftWarden(CommandSourceStack source,
                                          Collection<ServerPlayer> targets) {
        int prepared = 0;
        for (ServerPlayer player : targets) {
            if (EchoAbilityManager.activateMechanism(player, EchoPermission.VOID_ROUTE_READY_KEY)
                    && EchoAbilityManager.unlockAbility(player, EchoAbilityType.GRAVITY_ECHO)) {
                EchoAbilityManager.syncToClient(player);
                prepared++;
            }
        }
        if (prepared == 0) {
            return 0;
        }
        int successCount = prepared;
        source.sendSuccess(() -> Component.translatable(
                "command.unknown_echoes.deep_prepare.rift_warden", successCount), true);
        return prepared;
    }

    /** 测试入口：补齐虚空先知入口前置，但保留三座复合校准节点与祭坛仪式。 */
    private static int prepareVoidProphet(CommandSourceStack source,
                                           Collection<ServerPlayer> targets) {
        int prepared = 0;
        for (ServerPlayer player : targets) {
            if (DeepRouteNodeBlock.prepareVoidProphetForTesting(player)) {
                EchoAbilityManager.unlockAbility(player, EchoAbilityType.WIND_ECHO);
                EchoAbilityManager.unlockAbility(player, EchoAbilityType.TIDE_ECHO);
                EchoAbilityManager.unlockAbility(player, EchoAbilityType.TRUE_SIGHT_ECHO);
                EchoAbilityManager.syncToClient(player);
                prepared++;
            }
        }
        if (prepared == 0) {
            return 0;
        }
        int successCount = prepared;
        source.sendSuccess(() -> Component.translatable(
                "command.unknown_echoes.deep_prepare.void_prophet", successCount), true);
        return prepared;
    }
}
