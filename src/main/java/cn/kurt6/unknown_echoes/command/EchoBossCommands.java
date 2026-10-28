package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.entity.boss.BossTestControl;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Locale;

final class EchoBossCommands {
    private EchoBossCommands() { }

    static LiteralArgumentBuilder<CommandSourceStack> create() {
        var root = Commands.literal("boss").requires(source -> source.hasPermission(2));
        for (String action : new String[]{"inspect", "next", "open"}) {
            root.then(Commands.literal(action)
                    .executes(ctx -> run(ctx.getSource(), nearest(ctx.getSource()), action))
                    .then(Commands.argument("boss", EntityArgument.entity())
                            .executes(ctx -> run(ctx.getSource(), EntityArgument.getEntity(ctx, "boss"), action))));
        }
        return root;
    }

    private static Entity nearest(CommandSourceStack source) {
        return source.getLevel().getEntitiesOfClass(LivingEntity.class,
                        AABB.ofSize(source.getPosition(), 128, 128, 128),
                        entity -> entity.isAlive() && entity instanceof BossTestControl control
                                && control.acceptsTestControl()
                                && entity.position().distanceToSqr(source.getPosition()) <= 64 * 64)
                .stream().min(Comparator.comparingDouble(entity ->
                        entity.position().distanceToSqr(source.getPosition()))).orElse(null);
    }

    private static int run(CommandSourceStack source, Entity entity, String action) {
        if (!(entity instanceof LivingEntity boss) || !(boss instanceof BossTestControl control)
                || !boss.isAlive() || boss.isRemoved() || !(boss.level() instanceof ServerLevel)
                || !control.acceptsTestControl()) {
            source.sendFailure(Component.translatable("command.unknown_echoes.boss.missing"));
            return 0;
        }
        boolean changed = switch (action) {
            case "next" -> control.advanceTestPhase();
            case "open" -> control.openTestWindow();
            default -> true;
        };
        if (!changed) {
            source.sendFailure(Component.translatable("command.unknown_echoes.boss.unavailable"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.boss.state",
                boss.getDisplayName(), number(boss.getHealth()), number(boss.getMaxHealth()),
                number(control.phaseDamageFloor())), !action.equals("inspect"));
        return 1;
    }

    private static String number(float value) { return String.format(Locale.ROOT, "%.1f", value); }
}
