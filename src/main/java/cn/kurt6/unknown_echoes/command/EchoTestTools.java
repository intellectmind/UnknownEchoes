package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.DeepEchoAbilityHandler;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.item.tool.LandmarkPickaxeItem;
import cn.kurt6.unknown_echoes.item.weapon.WeaponSkill;
import cn.kurt6.unknown_echoes.particle.ParticleKind;
import cn.kurt6.unknown_echoes.particle.ParticleRules;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class EchoTestTools {
    private EchoTestTools() { }

    @FunctionalInterface
    interface PlayerAction {
        int run(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException;
    }

    static <T extends ArgumentBuilder<CommandSourceStack, T>> T withPlayer(T node, PlayerAction action) {
        return node.executes(ctx -> action.run(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> action.run(ctx, EntityArgument.getPlayer(ctx, "target"))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        for (String action : new String[]{"heal", "energy", "cooldowns"}) {
            root.then(withPlayer(Commands.literal(action),
                    (ctx, player) -> execute(ctx, player, action)));
        }
        var research = Commands.literal("research");
        for (EchoResearchLine line : EchoResearchLine.values()) {
            research.then(Commands.literal(line.getId()).then(withPlayer(
                    Commands.argument("points", IntegerArgumentType.integer(1, 4)),
                    (ctx, player) -> research(ctx, player, line, IntegerArgumentType.getInteger(ctx, "points")))));
        }
        root.then(research);
        return root.then(Commands.literal("kit").then(withPlayer(Commands.literal("particle"),
                (ctx, player) -> execute(ctx, player, "kit"))));
    }

    private static int research(CommandContext<CommandSourceStack> ctx, ServerPlayer player,
                                EchoResearchLine line, int points) {
        EchoResearchManager.addResearchPoints(player, line, points);
        return done(ctx, player, "research");
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String action) {
        if (action.equals("heal")) {
            if (!player.isAlive()) {
                ctx.getSource().sendFailure(Component.translatable("command.unknown_echoes.test.heal_dead"));
                return 0;
            }
            player.setHealth(player.getMaxHealth());
            player.getFoodData().eat(20, 1.0F);
            player.clearFire();
        } else if (action.equals("energy")) {
            ArtifactManager.refillEnergy(player, ArtifactManager.getMaxEnergy(player));
        } else if (action.equals("cooldowns")) {
            resetItemCooldowns(player);
            for (ArtifactType type : ArtifactType.values()) {
                ArtifactManager.getData(player).clearCooldown(type);
            }
            DeepEchoAbilityHandler.resetTestCooldowns(player);
            ArtifactManager.syncToClient(player);
        } else if (action.equals("kit")) {
            for (ParticleKind kind : ParticleKind.values()) give(player, new ItemStack(kind.material(), 50));
            for (int tier = 1; tier <= 5; tier++) {
                for (var cost : ParticleRules.upgradeCost(tier)) give(player, item(cost.item(), cost.count()));
            }
            give(player, item("echo_broadsword", 1));
            give(player, item("particle_enchanting_table", 1));
            give(player, item("particle_core", 50));
            give(player, item("purifying_vial", 16));
            player.setExperienceLevels(Math.max(player.experienceLevel, 200));
        }
        return done(ctx, player, action);
    }

    private static void resetItemCooldowns(ServerPlayer player) {
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(UnknownEchoes.MODID)) continue;
            if (player.getCooldowns().isOnCooldown(item)) player.getCooldowns().removeCooldown(item);
            if (item instanceof WeaponSkill) {
                for (String slot : new String[]{"primary", "secondary"}) {
                    player.getPersistentData().remove("ue_weapon_skill_" + id.getPath() + "_" + slot + "_until");
                    player.getPersistentData().remove("ue_" + id.getPath() + "_" + slot + "_until");
                }
            } else if (item instanceof LandmarkPickaxeItem) {
                player.getPersistentData().remove("ue_" + id.getPath() + "_active_until");
            }
        }
    }

    private static ItemStack item(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(UnknownEchoes.id(id)), count);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack, false);
    }

    private static int done(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String action) {
        ctx.getSource().sendSuccess(() -> Component.translatable("command.unknown_echoes.test." + action,
                player.getDisplayName()), true);
        return 1;
    }
}
