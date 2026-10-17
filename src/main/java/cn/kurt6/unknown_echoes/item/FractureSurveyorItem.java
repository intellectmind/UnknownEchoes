package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.journal.ClueLocator;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 只读取已加载区块的三域巨构测绘仪，不生成区块、不显示精确坐标。 */
public final class FractureSurveyorItem extends Item implements ItemActiveSkill {
    private static final String TARGET_KEY_PREFIX = "ue_fracture_surveyor_target_";
    private static final int ENERGY_COST = 4;
    private static final int COOLDOWN_TICKS = 60;
    private static final List<ResourceLocation> OVERWORLD_TARGETS = List.of(
            UnknownEchoes.id("far_echo_cartography_court"),
            UnknownEchoes.id("far_bell_wayfarer_harbor"));
    private static final List<ResourceLocation> ECHO_REALM_TARGETS = List.of(
            UnknownEchoes.id("chorus_confluence_court"),
            UnknownEchoes.id("prismatic_canopy_conservatory"));
    private static final List<ResourceLocation> DEEP_TARGETS = List.of(
            UnknownEchoes.id("resonance_foundry"),
            UnknownEchoes.id("gravity_crown_observatory"),
            UnknownEchoes.id("voidscar_bastion"),
            UnknownEchoes.id("shattered_codex_vault"),
            UnknownEchoes.id("weightless_orrery_court"),
            UnknownEchoes.id("hushed_cathedral"),
            UnknownEchoes.id("riftglass_citadel"),
            UnknownEchoes.id("folded_celestial_archive"),
            UnknownEchoes.id("inverted_pendulum_palace"),
            UnknownEchoes.id("riftworn_memorial_court"),
            UnknownEchoes.id("palimpsest_ecology_dome"),
            UnknownEchoes.id("interlayer_expedition_ring"));
    private static final String[] DIRECTIONS = {
            "east", "southeast", "south", "southwest",
            "west", "northwest", "north", "northeast"
    };

    public FractureSurveyorItem(Properties properties) {
        super(properties);
    }

    /** 测绘由「物品主动技能」键触发；换目标走「物品副功能」键。 */
    @Override
    public boolean activateItemSkill(ServerLevel level, ServerPlayer player,
                                     ItemStack stack, InteractionHand hand) {
        if (!canActivate(level, player, stack, hand)) {
            return false;
        }
        return survey(level, player, stack, hand);
    }

    @Override
    public boolean hasAlternateItemSkill() {
        return true;
    }

    @Override
    public boolean activateAlternateItemSkill(ServerLevel serverLevel, ServerPlayer serverPlayer,
                                              ItemStack stack, InteractionHand hand) {
        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return false;
        }
        List<ResourceLocation> targets = targetsFor(serverLevel);
        if (targets.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.fracture_surveyor.dormant"), true);
            return false;
        }
        String selectionKey = selectionKeyFor(serverLevel);
        int selected = Math.floorMod(
                serverPlayer.getPersistentData().getInt(selectionKey) + 1, targets.size());
        serverPlayer.getPersistentData().putInt(selectionKey, selected);
        serverPlayer.displayClientMessage(Component.translatable(
                "message.unknown_echoes.fracture_surveyor.selected",
                structureName(targets.get(selected))), true);
        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }
        serverLevel.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.65F,
                0.85F + selected * 0.05F);
        return true;
    }

    private boolean survey(ServerLevel serverLevel, ServerPlayer serverPlayer,
                           ItemStack stack, InteractionHand hand) {
        List<ResourceLocation> targets = targetsFor(serverLevel);
        if (targets.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.fracture_surveyor.dormant"), true);
            return false;
        }

        String selectionKey = selectionKeyFor(serverLevel);
        int selected = Math.floorMod(
                serverPlayer.getPersistentData().getInt(selectionKey), targets.size());
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        ResourceLocation targetId = targets.get(selected);
        ClueLocator.LoadedStructureTarget located = ClueLocator.findNearestLoadedStructure(
                serverLevel, targets, targetId, serverPlayer.blockPosition());
        if (located == null) {
            serverPlayer.getCooldowns().addCooldown(this, 20);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.fracture_surveyor.no_echo",
                    structureName(targetId)), true);
            if (canActivate(serverLevel, serverPlayer, stack, hand)) {
                PlayerSoundFeedback.deny(serverPlayer);
            }
            return false;
        }
        if (!targetId.equals(located.id())) {
            targetId = located.id();
            serverPlayer.getPersistentData().putInt(selectionKey, targets.indexOf(targetId));
        }
        BlockPos target = located.pos();
        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!ArtifactManager.spendEnergy(serverPlayer, ENERGY_COST)) {
            serverPlayer.getCooldowns().removeCooldown(this);
            return false;
        }
        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }
        boolean recorded = JournalManager.addClue(serverPlayer, new ExplorationClue(
                targetId.toString(), serverLevel.dimension().location().toString(), target, false));
        if (recorded) {
            JournalManager.syncJournal(serverPlayer);
        }
        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }

        double dx = target.getX() - serverPlayer.getX();
        double dz = target.getZ() - serverPlayer.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        int directionIndex = Math.floorMod(
                (int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4.0D)), 8);
        String range = distance < 192.0D ? "near" : distance < 512.0D ? "mid" : "far";
        serverPlayer.displayClientMessage(Component.translatable(
                "message.unknown_echoes.fracture_surveyor.result",
                structureName(targetId),
                Component.translatable("direction.unknown_echoes." + DIRECTIONS[directionIndex]),
                Component.translatable("range.unknown_echoes." + range)), true);
        if (!canActivate(serverLevel, serverPlayer, stack, hand)) {
            return true;
        }

        Vec3 direction = new Vec3(dx, 0.0D, dz).normalize();
        Vec3 origin = serverPlayer.position().add(0.0D, 1.2D, 0.0D);
        for (int step = 1; step <= 12; step++) {
            Vec3 point = origin.add(direction.scale(step * 0.65D));
            serverLevel.sendParticles(step % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y + Math.sin(step * 0.7D) * 0.18D, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        serverLevel.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, 1.35F);
        if (canActivate(serverLevel, serverPlayer, stack, hand)) {
            serverPlayer.swing(hand, true);
        }
        return true;
    }

    private boolean canActivate(ServerLevel level, ServerPlayer player,
                                ItemStack stack, InteractionHand hand) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && stack.is(this) && player.getItemInHand(hand) == stack;
    }

    private static List<ResourceLocation> targetsFor(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        if (dimensionId.equals(Level.OVERWORLD.location())) {
            return OVERWORLD_TARGETS;
        }
        if (dimensionId.equals(EchoPermission.ECHO_REALM_ID)) {
            return ECHO_REALM_TARGETS;
        }
        if (dimensionId.equals(EchoPermission.DEEP_ECHOES_ID)) {
            return DEEP_TARGETS;
        }
        return List.of();
    }

    private static String selectionKeyFor(ServerLevel level) {
        ResourceLocation dimensionId = level.dimension().location();
        return TARGET_KEY_PREFIX + dimensionId.getNamespace() + "_" + dimensionId.getPath();
    }

    private static Component structureName(ResourceLocation id) {
        return Component.translatable("structure.unknown_echoes." + id.getPath());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.fracture_surveyor.mode",
                        Component.keybind("key.unknown_echoes.item_alternate_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.fracture_surveyor.scan",
                        Component.keybind("key.unknown_echoes.item_active_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.fracture_surveyor.detail")
                .withStyle(ChatFormatting.GRAY));
    }
}
