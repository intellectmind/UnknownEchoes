package cn.kurt6.unknown_echoes.command;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityData;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.artifact.ArtifactData;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.journal.EchoCompletionManager;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.research.EchoResearchLine;
import cn.kurt6.unknown_echoes.research.EchoResearchManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * /echoes admin query|grant|revoke|repair <玩家> [<进度项>](V0.6D,权限等级 2)。
 * 多人服个人回响进度的查询与修复入口。只操作既有数据写入口
 * (EchoAbilityManager / ArtifactManager / JournalManager),不直接写客户端状态。
 * 进度项格式:ability:<id> / boss:<path> / beacon:<ns:path> / dimension:<ns:path>
 *           / artifact:<id> / research:<line>:<点数> / mechanism:<key> / t7:<state>
 */
public class EchoAdminCommands {

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
                suggestions.add("dimension:unknown_echoes:echo_realm");
                suggestions.add("t7:reward");
                suggestions.add("t7:core");
                suggestions.add("t7:deep_clue");
                suggestions.add("t7:index");
                return SharedSuggestionProvider.suggest(suggestions, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echoes")
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("query")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> query(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("progress", StringArgumentType.string())
                                                .suggests(PROGRESS_SUGGESTIONS)
                                                .executes(ctx -> modify(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "progress"), true)))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("progress", StringArgumentType.string())
                                                .suggests(PROGRESS_SUGGESTIONS)
                                                .executes(ctx -> modify(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "progress"), false)))))
                        .then(Commands.literal("repair")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> repair(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target")))))));
    }

    /** query:汇总该玩家全部个人回响进度(能力/信标/Boss/维度/研究/神器/能量/日志)。 */
    private static int query(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityData ability = EchoAbilityManager.getData(target);
        ArtifactData artifacts = ArtifactManager.getData(target);
        var journal = JournalManager.getData(target);
        String name = target.getGameProfile().getName();

        source.sendSuccess(() -> Component.literal("== " + name + " =="), false);
        source.sendSuccess(() -> Component.literal("abilities=" + ability.getUnlockedAbilities()), false);
        source.sendSuccess(() -> Component.literal("beacons=" + ability.getActivatedBeacons()), false);
        source.sendSuccess(() -> Component.literal("bosses=" + ability.getDefeatedBosses()), false);
        source.sendSuccess(() -> Component.literal("dimensions=" + ability.getUnlockedDimensions()), false);
        StringBuilder research = new StringBuilder();
        for (EchoResearchLine line : EchoResearchLine.values()) {
            research.append(line.getId()).append('=')
                    .append(EchoResearchManager.getResearchLevel(target, line)).append(' ');
        }
        String researchLine = research.toString();
        source.sendSuccess(() -> Component.literal("research: " + researchLine), false);
        StringBuilder artifactLine = new StringBuilder();
        for (ArtifactType type : ArtifactType.values()) {
            if (artifacts.isClaimed(type)) {
                artifactLine.append(type.getId()).append("(lv").append(artifacts.getLevel(type))
                        .append(",tuning=").append(artifacts.getTuning(type).isEmpty() ? "-" : artifacts.getTuning(type))
                        .append(",serial=").append(artifacts.getSerial(type)).append(") ");
            }
        }
        String artifactsOut = artifactLine.isEmpty() ? "(none)" : artifactLine.toString();
        source.sendSuccess(() -> Component.literal("artifacts: " + artifactsOut), false);
        source.sendSuccess(() -> Component.literal("energy=" + ArtifactManager.getEnergy(target)
                + "/" + ArtifactManager.getMaxEnergy()), false);
        source.sendSuccess(() -> Component.literal("journal: structures=" + journal.getStructures().size()
                + " biomes=" + journal.getBiomes().size() + " mobs=" + journal.getMobs().size()
                + " pages=" + journal.getPages().size() + " clues=" + journal.getClues().size()), false);
        return 1;
    }

    /** grant/revoke:解析进度项并调用对应 Manager 写入口。 */
    private static int modify(CommandSourceStack source, ServerPlayer target, String progress, boolean grant) {
        String[] parts = progress.split(":", 2);
        if (parts.length < 2) {
            source.sendFailure(Component.literal("进度项格式: <category>:<value>,见 tab 补全"));
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
                    EchoAbilityManager.unlockAbility(target, ability);
                } else {
                    EchoAbilityManager.revokeAbility(target, ability);
                }
                yield true;
            }
            case "boss" -> {
                ResourceLocation id = value.contains(":")
                        ? ResourceLocation.tryParse(value) : UnknownEchoes.id(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    EchoAbilityManager.markBossDefeated(target, id);
                } else {
                    EchoAbilityManager.revokeBossDefeated(target, id);
                }
                yield true;
            }
            case "beacon" -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    EchoAbilityManager.activateBeacon(target, id);
                } else {
                    EchoAbilityManager.revokeBeacon(target, id);
                }
                yield true;
            }
            case "dimension" -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null) {
                    yield false;
                }
                if (grant) {
                    EchoAbilityManager.unlockDimension(target, id);
                } else {
                    EchoAbilityManager.revokeDimension(target, id);
                }
                yield true;
            }
            case "artifact" -> {
                ArtifactType type = ArtifactType.byId(value);
                if (type == null) {
                    yield false;
                }
                if (grant) {
                    ArtifactManager.adminGrant(target, type);
                } else {
                    ArtifactManager.adminRevoke(target, type);
                }
                yield true;
            }
            case "research" -> {
                // research:<line>:<点数>;revoke 不支持负向回收(残页来源无法区分),提示改用 query 核对
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
                if (grant) {
                    EchoAbilityManager.activateMechanism(target, value);
                    yield true;
                }
                EchoAbilityManager.deactivateMechanism(target, value);
                yield true;
            }
            case "t7" -> {
                yield EchoCompletionManager.adminSetT7ClaimState(target, value, grant);
            }
            default -> false;
        };
        if (!ok) {
            source.sendFailure(Component.literal("无法" + (grant ? "授予" : "撤销") + ": " + progress));
            return 0;
        }
        source.sendSuccess(() -> Component.literal((grant ? "已授予 " : "已撤销 ") + progress
                + " → " + target.getGameProfile().getName()), true);
        return 1;
    }

    /** repair:全量重推同步包 + 配方解锁核对 + 能量惰性结算,修复卡档的客户端状态。 */
    private static int repair(CommandSourceStack source, ServerPlayer target) {
        EchoAbilityManager.syncToClient(target); // 内部顺带核对配方解锁链
        JournalManager.syncJournal(target);
        ArtifactManager.syncToClient(target);    // 内部完成能量惰性结算
        source.sendSuccess(() -> Component.literal("已重新同步 "
                + target.getGameProfile().getName() + " 的全部回响进度"), true);
        return 1;
    }
}
