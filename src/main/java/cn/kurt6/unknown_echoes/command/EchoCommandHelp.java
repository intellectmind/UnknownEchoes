package cn.kurt6.unknown_echoes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;

final class EchoCommandHelp {
    private static final String ROOT = "unknownechoes";

    private EchoCommandHelp() { }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                         List<LiteralArgumentBuilder<CommandSourceStack>> groups) {
        List<String> topics = groups.stream().map(LiteralArgumentBuilder::getLiteral).toList();
        var root = Commands.literal(ROOT)
                .executes(ctx -> overview(ctx.getSource(), dispatcher, topics));
        var help = Commands.literal("help")
                .executes(ctx -> overview(ctx.getSource(), dispatcher, topics));
        for (var group : groups) {
            String topic = group.getLiteral();
            if (group.getCommand() == null) {
                group.executes(ctx -> usage(ctx.getSource(), dispatcher, topic));
            }
            root.then(group);
            help.then(Commands.literal(topic).requires(group.getRequirement())
                    .executes(ctx -> usage(ctx.getSource(), dispatcher, topic)));
        }
        root.then(help);
        var node = root.build();

        var admin = alias("admin", node.getChild("progress"));
        admin.then(alias("activate_world_echo", node.getChild("test").getChild("world_echo")));
        node.addChild(admin.build());
        node.addChild(Commands.literal("realm").requires(source -> source.hasPermission(2))
                .executes(ctx -> usage(ctx.getSource(), dispatcher, "tp"))
                .then(alias("tp", node.getChild("tp").getChild("realm"))).build());
        node.addChild(Commands.literal("deep").requires(source -> source.hasPermission(2))
                .executes(ctx -> usage(ctx.getSource(), dispatcher, "test"))
                .then(alias("tp", node.getChild("tp").getChild("deep")))
                .then(alias("prepare", node.getChild("test").getChild("prepare"))).build());
        dispatcher.getRoot().addChild(node);
        for (String name : List.of("ue", "echoes")) {
            dispatcher.register(Commands.literal(name).executes(node.getCommand()).redirect(node));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> alias(String name,
                                                                    CommandNode<CommandSourceStack> target) {
        // 复制执行器与子节点，兼容不带后续参数的叶命令；只 redirect 会丢失叶执行器。
        var alias = Commands.literal(name).requires(target.getRequirement());
        if (target.getCommand() != null) {
            alias.executes(target.getCommand());
        }
        for (var child : target.getChildren()) {
            alias.then(child);
        }
        return alias;
    }

    private static int overview(CommandSourceStack source, CommandDispatcher<CommandSourceStack> dispatcher,
                                List<String> topics) {
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.help.title")
                .withStyle(ChatFormatting.GOLD), false);
        var root = dispatcher.getRoot().getChild(ROOT);
        for (String topic : topics) {
            if (!root.getChild(topic).canUse(source)) {
                continue;
            }
            source.sendSuccess(() -> Component.translatable("command.unknown_echoes.help.entry",
                    commandLink("/" + ROOT + " " + topic, "/" + ROOT + " help " + topic),
                    Component.translatable("command.unknown_echoes.help." + topic)), false);
        }
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.help.footer"), false);
        return 1;
    }

    private static int usage(CommandSourceStack source, CommandDispatcher<CommandSourceStack> dispatcher,
                             String topic) {
        var group = dispatcher.getRoot().getChild(ROOT).getChild(topic);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.help.entry",
                Component.literal("/" + ROOT + " " + topic).withStyle(ChatFormatting.GOLD),
                Component.translatable("command.unknown_echoes.help." + topic)), false);
        for (String suffix : dispatcher.getAllUsage(group, source, true)) {
            if (suffix.isEmpty() && !group.getChildren().isEmpty()) {
                continue;
            }
            String command = "/" + ROOT + " " + topic + (suffix.isEmpty() ? "" : " " + suffix);
            int argument = command.indexOf('<');
            String suggestion = argument < 0 ? command : command.substring(0, argument);
            source.sendSuccess(() -> commandLink(command, suggestion), false);
        }
        return 1;
    }

    private static Component commandLink(String text, String suggestion) {
        return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("command.unknown_echoes.help.click"))));
    }
}
