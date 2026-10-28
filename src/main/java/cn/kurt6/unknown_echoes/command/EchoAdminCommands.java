package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityData;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.artifact.ArtifactData;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.block.beacon.DeepEchoTeleporter;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlock;
import cn.kurt6.unknown_echoes.block.deep.WorldResonanceAltarBlockEntity;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EchoAdminCommands {
    private static final int WORLD_ECHO_ALTAR_SEARCH_RADIUS = 16;

    private static final SuggestionProvider<CommandSourceStack> PROGRESS_SUGGESTIONS =
            (context, builder) -> {
                List<String> suggestions = new ArrayList<>();
                for (EchoAbilityType ability : EchoAbilityType.values()) {
                    suggestions.add("ability:" + ability.getId());
                }
                for (EchoResearchLine line : EchoResearchLine.values()) {
                    suggestions.add("research:" + line.getId() + ":1");
                }
                for (ArtifactType artifact : ArtifactType.values()) {
                    suggestions.add("artifact:" + artifact.getId());
                }
                suggestions.add("boss:forgotten_colossus");
                suggestions.add("boss:abyss_watcher");
                suggestions.add("boss:mirror_guardian");
                suggestions.add("boss:storm_weaver");
                suggestions.add("boss:silent_priest");
                suggestions.add("boss:tide_lantern_keeper");
                suggestions.add("boss:mirror_dust_butler");
                suggestions.add("boss:void_prophet");
                suggestions.add("boss:rift_warden");
                suggestions.add("dimension:unknown_echoes:echo_realm");
                suggestions.add("dimension:unknown_echoes:deep_echoes");
                suggestions.add("mechanism:deep:void_route_ready");
                suggestions.add("t7:reward");
                suggestions.add("t7:core");
                suggestions.add("t7:deep_clue");
                suggestions.add("t7:index");
                return SharedSuggestionProvider.suggest(suggestions, builder);
            };

    public static LiteralArgumentBuilder<CommandSourceStack> progress() {
        return Commands.literal("progress")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("query")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> query(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("grant")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("progress", StringArgumentType.greedyString())
                                        .suggests(PROGRESS_SUGGESTIONS)
                                        .executes(ctx -> modify(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                StringArgumentType.getString(ctx, "progress"), true)))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("progress", StringArgumentType.greedyString())
                                        .suggests(PROGRESS_SUGGESTIONS)
                                        .executes(ctx -> modify(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                StringArgumentType.getString(ctx, "progress"), false)))))
                .then(Commands.literal("repair")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> repair(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> worldEchoTest() {
        return Commands.literal("world_echo")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> activateWorldEcho(ctx.getSource(),
                        ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> activateWorldEcho(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"))));
    }

    private static int query(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityData ability = EchoAbilityManager.getData(target);
        ArtifactData artifacts = ArtifactManager.getData(target);
        var journal = JournalManager.getData(target);
        String name = target.getGameProfile().getName();

        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.header", name), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.abilities",
                ability.getUnlockedAbilities()), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.beacons",
                ability.getActivatedBeacons()), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.bosses",
                ability.getDefeatedBosses()), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.dimensions",
                ability.getUnlockedDimensions()), false);
        StringBuilder research = new StringBuilder();
        for (EchoResearchLine line : EchoResearchLine.values()) {
            research.append(line.getId()).append('=')
                    .append(EchoResearchManager.getResearchLevel(target, line)).append(' ');
        }
        String researchLine = research.toString();
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.research",
                researchLine), false);
        StringBuilder artifactLine = new StringBuilder();
        for (ArtifactType type : ArtifactType.values()) {
            if (artifacts.isClaimed(type)) {
                artifactLine.append(type.getId()).append("(lv").append(artifacts.getLevel(type))
                        .append(",tuning=").append(artifacts.getTuning(type).isEmpty() ? "-" : artifacts.getTuning(type))
                        .append(",serial=").append(artifacts.getSerial(type)).append(") ");
            }
        }
        String artifactsOut = artifactLine.isEmpty() ? "(none)" : artifactLine.toString();
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.artifacts",
                artifactsOut), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.energy",
                ArtifactManager.getEnergy(target), ArtifactManager.getMaxEnergy(target)), false);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.inspect.journal",
                journal.getStructures().size(), journal.getBiomes().size(), journal.getMobs().size(),
                journal.getPages().size(), journal.getClues().size()), false);
        return 1;
    }

    private static int modify(CommandSourceStack source, ServerPlayer target, String progress, boolean grant)
            throws CommandSyntaxException {
        progress = progress.strip();
        if (!progress.isEmpty() && StringReader.isQuotedStringStart(progress.charAt(0))) {
            StringReader reader = new StringReader(progress);
            progress = reader.readString();
            if (reader.canRead()) {
                source.sendFailure(Component.translatable("command.unknown_echoes.admin.progress_format"));
                return 0;
            }
        } else if (progress.chars().anyMatch(Character::isWhitespace)) {
            source.sendFailure(Component.translatable("command.unknown_echoes.admin.progress_format"));
            return 0;
        }
        return modifyProgress(source, target, progress, grant);
    }

    private static int modifyProgress(CommandSourceStack source, ServerPlayer target, String progress, boolean grant) {
        String[] parts = progress.split(":", 2);
        if (parts.length < 2) {
            source.sendFailure(Component.translatable("command.unknown_echoes.admin.progress_format"));
            return 0;
        }
        String category = parts[0];
        String value = parts[1];
        boolean ok = switch (category) {
            case "ability" -> {
                EchoAbilityType ability = EchoAbilityType.byId(value);
                if (ability == null) {
                    yield false;
                }
                if (grant) {
                    yield EchoAbilityManager.unlockAbility(target, ability);
                } else {
                    EchoAbilityManager.revokeAbility(target, ability);
                    yield true;
                }
            }
            case "boss" -> {
                ResourceLocation id = value.contains(":")
                        ? ResourceLocation.tryParse(value) : UnknownEchoes.id(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    yield EchoAbilityManager.markBossDefeated(target, id);
                } else {
                    EchoAbilityManager.revokeBossDefeated(target, id);
                    yield true;
                }
            }
            case "beacon" -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    yield EchoAbilityManager.activateBeacon(target, id);
                } else {
                    EchoAbilityManager.revokeBeacon(target, id);
                    yield true;
                }
            }
            case "dimension" -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    yield EchoAbilityManager.unlockDimension(target, id);
                } else {
                    EchoAbilityManager.revokeDimension(target, id);
                    yield true;
                }
            }
            case "artifact" -> {
                ArtifactType type = ArtifactType.byId(value);
                if (type == null) {
                    yield false;
                }
                if (grant) {
                    yield ArtifactManager.adminGrant(target, type);
                } else {
                    ArtifactManager.adminRevoke(target, type);
                }
                yield true;
            }
            case "research" -> {

                String[] sub = value.split(":", 2);
                EchoResearchLine line = EchoResearchLine.byId(sub[0]);
                if (line == null || !grant) {
                    yield false;
                }
                int amount = 1;
                if (sub.length == 2) {
                    try {
                        amount = Integer.parseInt(sub[1]);
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                EchoResearchManager.addResearchPoints(target, line, amount);
                yield true;
            }
            case "mechanism" -> {
                boolean changed;
                if (grant) {
                    changed = EchoAbilityManager.activateMechanism(target, value);
                } else {
                    EchoAbilityManager.deactivateMechanism(target, value);
                    changed = true;
                }

                if (changed) {
                    EchoAbilityManager.syncToClient(target);
                }
                yield changed;
            }
            case "t7" -> {
                yield EchoCompletionManager.adminSetT7ClaimState(target, value, grant);
            }
            default -> false;
        };
        if (!ok) {
            source.sendFailure(Component.translatable(grant
                    ? "command.unknown_echoes.admin.grant_failed"
                    : "command.unknown_echoes.admin.revoke_failed", progress));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(grant
                ? "command.unknown_echoes.admin.granted"
                : "command.unknown_echoes.admin.revoked", progress, target.getGameProfile().getName()), true);
        return 1;
    }

    /** repair:全量重推同步包 + 配方解锁核对 + 能量惰性结算,修复卡档的客户端状态。 */
    private static int repair(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityManager.syncToClient(target);
        JournalManager.syncJournal(target);
        ArtifactManager.syncToClient(target);
        source.sendSuccess(() -> Component.translatable("command.unknown_echoes.admin.repaired",
                target.getGameProfile().getName()), true);
        return 1;
    }

    /** 测试入口：直接填满目标附近祭坛的五种共鸣，但仍走正式召唤仪式与 Boss 绑定流程。 */
    private static int activateWorldEcho(CommandSourceStack source, ServerPlayer target) {
        if (target.isSpectator()) {
            source.sendFailure(Component.translatable(
                    "command.unknown_echoes.admin.world_echo.spectator"));
            return 0;
        }
        if (!target.level().dimension().equals(DeepEchoTeleporter.DEEP_ECHOES)) {
            source.sendFailure(Component.translatable(
                    "command.unknown_echoes.admin.world_echo.wrong_dimension"));
            return 0;
        }

        ServerLevel level = target.serverLevel();
        BlockPos center = BlockPos.containing(target.position());
        BlockPos altarPos = findNearestWorldEchoAltar(level, center);
        if (altarPos == null) {
            source.sendFailure(Component.translatable(
                    "command.unknown_echoes.admin.world_echo.no_altar",
                    WORLD_ECHO_ALTAR_SEARCH_RADIUS));
            return 0;
        }

        WorldResonanceAltarBlockEntity altar = WorldResonanceAltarBlock.getOrCreateAltar(
                level, altarPos, level.getBlockState(altarPos));
        if (altar == null || !altar.beginAdminTestStarting(level, target.getUUID())) {
            source.sendFailure(Component.translatable(
                    "command.unknown_echoes.admin.world_echo.busy"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.unknown_echoes.admin.world_echo.started", altarPos.toShortString()), true);
        return 1;
    }

    @Nullable
    private static BlockPos findNearestWorldEchoAltar(ServerLevel level, BlockPos center) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = WORLD_ECHO_ALTAR_SEARCH_RADIUS;
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        for (BlockPos mutable : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(mutable).is(ModBlocks.WORLD_RESONANCE_ALTAR.get())) {
                continue;
            }
            BlockPos candidate = mutable.immutable();
            double dx = candidate.getX() - center.getX();
            double dy = candidate.getY() - center.getY();
            double dz = candidate.getZ() - center.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
