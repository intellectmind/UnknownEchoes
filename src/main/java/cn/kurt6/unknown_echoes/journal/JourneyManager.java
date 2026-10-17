package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.network.JourneySyncPayload;
import cn.kurt6.unknown_echoes.world.DeepEchoesBiomeCatalog;
import cn.kurt6.unknown_echoes.world.EchoRealmBiomeCatalog;
import cn.kurt6.unknown_echoes.world.wonder.WorldWonderType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

public final class JourneyManager {
    private JourneyManager() { }

    public static List<Integer> values(ServerPlayer player) {
        var journal = JournalManager.getData(player);
        var ability = EchoAbilityManager.getData(player);
        var records = ability.getActivatedMechanisms();
        var puzzles = JourneyPuzzleRecords.count(records);
        int structures = (int) player.server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .keySet().stream().filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)
                        && journal.getStructures().contains(id.toString())).count();
        int mobs = (int) BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> id.getNamespace().equals(UnknownEchoes.MODID)
                        && journal.getMobs().contains(id.toString())).count();
        int biomes = DeepEchoesProgressCatalog.countKnown(journal.getBiomes(), EchoRealmBiomeCatalog.IDS)
                + DeepEchoesProgressCatalog.countKnown(journal.getBiomes(), DeepEchoesBiomeCatalog.IDS);
        int abilities = (int) Arrays.stream(EchoAbilityType.values())
                .filter(type -> ability.getUnlockedAbilities().contains(type.getId())).count();
        int revisits = (int) records.stream().filter(key -> key.startsWith("research_revisit:")).count();
        int wonders = (int) Arrays.stream(WorldWonderType.values())
                .filter(type -> journal.getStructures().contains(UnknownEchoes.id(type.getId()).toString())).count();
        return List.of(structures, AncientPageCatalog.countReleasePages(journal.getPages()), biomes, mobs,
                puzzles.sites(), puzzles.kinds(), abilities, revisits, wonders, puzzles.workshops(),
                DeepEchoesProgressCatalog.countKnown(journal.getBiomes(), DeepEchoesProgressCatalog.BIOME_IDS),
                DeepEchoesProgressCatalog.countKnown(journal.getStructures(), DeepEchoesProgressCatalog.STRUCTURE_IDS),
                DeepEchoesProgressCatalog.countPages(journal.getPages()));
    }

    public static int claimedMask(ServerPlayer player) {
        int mask = 0;
        for (int index = 0; index < JourneyCatalog.CHAPTERS.size(); index++) {
            if (EchoAbilityManager.hasActivatedMechanism(player, JourneyCatalog.CHAPTERS.get(index).claimKey())) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    public static void sync(ServerPlayer player) {
        List<Integer> values = values(player);
        ModAdvancements.checkJourneyAdvancements(player, values);
        int claimed = claimedMask(player);
        int ready = JourneyCatalog.readyMask(values, claimed);
        boolean notify = false;
        if (player.isAlive() && !player.isSpectator()) {
            for (int index = 0; index < JourneyCatalog.CHAPTERS.size(); index++) {
                if ((ready & (1 << index)) != 0) {
                    String key = JourneyCatalog.CHAPTERS.get(index).noticeKey();
                    if (!EchoAbilityManager.hasActivatedMechanism(player, key)) {
                        notify |= EchoAbilityManager.activateMechanism(player, key);
                    }
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new JourneySyncPayload(values, claimed));
        if (notify) player.displayClientMessage(Component.translatable("journey.unknown_echoes.ready")
                .withStyle(ChatFormatting.GOLD), false);
    }

    public static void recordPuzzle(ServerPlayer player, BlockPos pos, String kind) {
        if (!player.isAlive() || player.isSpectator()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 144.0D) return;
        var correction = JourneyPuzzleRecords.sequenceCorrection(
                EchoAbilityManager.getData(player).getActivatedMechanisms(),
                player.level().dimension().location().toString(), pos.asLong(), kind);
        if (!correction.isEmpty()) {
            if (EchoAbilityManager.completeReservedMechanisms(player, correction)) sync(player);
            return;
        }
        String key = JourneyPuzzleRecords.nextKey(EchoAbilityManager.getData(player).getActivatedMechanisms(),
                player.level().dimension().location().toString(), pos.asLong(), kind);
        if (key != null && EchoAbilityManager.activateMechanism(player, key)) sync(player);
    }

    public static void claim(ServerPlayer player, int index) {
        var chapter = JourneyCatalog.chapter(index);
        if (chapter == null || !player.isAlive() || player.isSpectator()) return;
        if (EchoAbilityManager.hasActivatedMechanism(player, chapter.claimKey()) || !chapter.complete(values(player))) {
            sync(player);
            return;
        }
        List<ItemStack> rewards = chapter.rewards().stream()
                .map(reward -> new ItemStack(BuiltInRegistries.ITEM.get(UnknownEchoes.id(reward.item())), reward.count()))
                .toList();
        if (!RewardCapacity.canFitAll(player, rewards)) {
            player.displayClientMessage(Component.translatable("journey.unknown_echoes.need_space"), true);
            return;
        }
        if (!EchoAbilityManager.activateMechanisms(player, () -> {
            for (ItemStack reward : rewards) player.getInventory().add(reward);
        }, chapter.claimKey())) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.progress_record.full"), true);
            return;
        }
        player.playNotifySound(ModSounds.FEEDBACK_JOURNEY_REWARD.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("journey.unknown_echoes.received",
                Component.translatable("journey.unknown_echoes.chapter." + chapter.id())), true);
        sync(player);
    }
}
