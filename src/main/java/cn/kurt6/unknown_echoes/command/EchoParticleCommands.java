package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.particle.ParticleData;
import cn.kurt6.unknown_echoes.particle.ParticleFx;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

final class EchoParticleCommands {
    private EchoParticleCommands() { }

    static LiteralArgumentBuilder<CommandSourceStack> create() {
        var root = Commands.literal("particle").requires(source -> source.hasPermission(2));
        root.then(EchoTestTools.withPlayer(Commands.literal("inspect"),
                (ctx, player) -> inspect(ctx.getSource(), player)));
        root.then(Commands.literal("tier").then(EchoTestTools.withPlayer(
                Commands.argument("tier", IntegerArgumentType.integer(0, 5)),
                (ctx, player) -> edit(ctx.getSource(), player, "tier", null,
                        IntegerArgumentType.getInteger(ctx, "tier")))));
        root.then(EchoTestTools.withPlayer(Commands.literal("clear"),
                (ctx, player) -> edit(ctx.getSource(), player, "clear", null, 0)));
        root.then(Commands.literal("set").then(kinds().then(EchoTestTools.withPlayer(
                Commands.argument("count", IntegerArgumentType.integer(0, 50)),
                (ctx, player) -> edit(ctx.getSource(), player, "set", kind(ctx),
                        IntegerArgumentType.getInteger(ctx, "count"))))));
        root.then(Commands.literal("preset").then(EchoTestTools.withPlayer(kinds(),
                (ctx, player) -> edit(ctx.getSource(), player, "preset", kind(ctx), 50))));
        var shapes = kinds();
        for (String shape : new String[]{"ring", "line"}) {
            shapes.then(EchoTestTools.withPlayer(Commands.literal(shape),
                    (ctx, player) -> preview(ctx.getSource(), player, kind(ctx), shape)));
        }
        return root.then(Commands.literal("preview").then(shapes));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> kinds() {
        return Commands.argument("kind", StringArgumentType.word()).suggests((ctx, builder) ->
                SharedSuggestionProvider.suggest(Arrays.stream(ParticleKind.values()).map(ParticleKind::id), builder));
    }

    private static ParticleKind kind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "kind");
        for (ParticleKind kind : ParticleKind.values()) {
            if (kind.id().equals(id)) return kind;
        }
        throw new SimpleCommandExceptionType(Component.translatable(
                "command.unknown_echoes.particle.unknown")).create();
    }

    private static int preview(CommandSourceStack source, ServerPlayer player, ParticleKind kind, String shape) {
        Vec3 from = player.position().add(0, 1, 0);
        if (shape.equals("ring")) {
            ParticleFx.ring(player.serverLevel(), from, 3, kind);
        } else {
            ParticleFx.line(player.serverLevel(), from, from.add(player.getLookAngle().scale(8)), kind);
        }
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.particle.preview",
                player.getDisplayName(), Component.translatable(kind.key())), false);
        return 1;
    }

    static ParticleData changed(ParticleData before, String operation, ParticleKind kind, int value) {
        if (operation.equals("tier")) return new ParticleData(value, before.counts());
        if (operation.equals("clear")) return before.clear();
        var counts = new ArrayList<>(operation.equals("preset")
                ? Collections.nCopies(ParticleKind.values().length, 0) : before.counts());
        counts.set(kind.ordinal(), value);
        return new ParticleData(operation.equals("preset") ? 5 : before.tier(), counts);
    }

    private static int edit(CommandSourceStack source, ServerPlayer player, String operation,
                            ParticleKind kind, int value) {
        var stack = player.getMainHandItem();
        if (!ParticleRules.eligible(stack)) {
            source.sendFailure(Component.translatable("command.unknown_echoes.particle.weapon"));
            return 0;
        }
        ParticleData data;
        try {
            data = changed(ParticleRules.data(stack), operation, kind, value);
        } catch (IllegalArgumentException invalidCapacity) {
            source.sendFailure(Component.translatable("command.unknown_echoes.particle.capacity"));
            return 0;
        }
        stack.set(ModDataComponents.PARTICLES.get(), data);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return inspect(source, player);
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!ParticleRules.eligible(stack)) {
            source.sendFailure(Component.translatable("command.unknown_echoes.particle.weapon"));
            return 0;
        }
        var data = ParticleRules.data(stack);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.particle.summary",
                player.getDisplayName(), data.tier(), data.total(), data.capacity()), false);
        for (ParticleKind kind : ParticleKind.values()) {
            source.sendSuccess(() -> Component.translatable("command.unknown_echoes.particle.count",
                    Component.translatable(kind.key()), data.count(kind)), false);
        }
        return 1;
    }
}
