package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.worldevent.WorldEventManager;
import cn.kurt6.unknown_echoes.worldevent.WorldEventType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RealmDisasterCommands {
    private RealmDisasterCommands() { }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("disaster").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(context -> {
                    boolean started = WorldEventManager.startDisaster(context.getSource().getPlayerOrException());
                    context.getSource().sendSuccess(() -> Component.translatable(
                            "message.unknown_echoes.disaster." + (started ? "started" : "unavailable")), false);
                    return started ? 1 : 0;
                }))
                .then(Commands.literal("test").executes(context -> test(context.getSource(),
                                context.getSource().getLevel().dimension().location().equals(EchoPermission.DEEP_ECHOES_ID)
                                        ? WorldEventType.STRATA_RUPTURE : WorldEventType.RIFT_STORM, 60))
                        .then(testType("rift_storm", WorldEventType.RIFT_STORM))
                        .then(testType("strata_rupture", WorldEventType.STRATA_RUPTURE)))
                .then(Commands.literal("stop").executes(context -> {
                    int stopped = WorldEventManager.stopDisasters(context.getSource().getLevel());
                    context.getSource().sendSuccess(() -> Component.translatable(
                            "message.unknown_echoes.disaster.stopped", stopped), false);
                    return stopped;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> testType(String name, WorldEventType type) {
        return Commands.literal(name).executes(context -> test(context.getSource(), type, 60))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(20, 300))
                        .executes(context -> test(context.getSource(), type, IntegerArgumentType.getInteger(context, "seconds"))));
    }

    private static int test(CommandSourceStack source, WorldEventType type, int seconds) throws CommandSyntaxException {
        var result = WorldEventManager.testDisaster(source.getPlayerOrException(), type, seconds);
        if (result == WorldEventManager.DisasterTestResult.STARTED) {
            source.sendSuccess(() -> Component.translatable("message.unknown_echoes.disaster.test_started", seconds), false);
            return 1;
        }
        source.sendFailure(Component.translatable("message.unknown_echoes.disaster.test_" + result.name().toLowerCase(java.util.Locale.ROOT)));
        return 0;
    }
}
