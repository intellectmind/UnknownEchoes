package cn.kurt6.unknown_echoes.particle;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

final class ParticleExperience {
    private ParticleExperience() { }

    record Change(int level, float progress, int total, int nextLevel, int points, int delta) {
        boolean unchanged(ServerPlayer player) {
            return player.experienceLevel == level && player.experienceProgress == progress && player.totalExperience == total;
        }

        boolean approve(ServerPlayer player) {
            if (!unchanged(player)) return false;
            if (delta == 0) return true;
            var pointsEvent = new PlayerXpEvent.XpChange(player, delta);
            if (NeoForge.EVENT_BUS.post(pointsEvent).isCanceled() || pointsEvent.getAmount() != delta || !unchanged(player)) return false;
            if (nextLevel != level) {
                var levelEvent = new PlayerXpEvent.LevelChange(player, nextLevel - level);
                if (NeoForge.EVENT_BUS.post(levelEvent).isCanceled() || levelEvent.getLevels() != nextLevel - level) return false;
            }
            return unchanged(player);
        }

        void apply(ServerPlayer player) {
            if (delta == 0) return;
            // 事件已在扣费前校验；此处不能再次触发可取消事件，打断物品与经验的统一结算。
            player.setExperienceLevels(nextLevel);
            player.setExperiencePoints(points);
            player.totalExperience = (int) Math.clamp((long) total + delta, 0, Integer.MAX_VALUE);
            player.increaseScore(delta);
        }
    }

    static Change prepare(ServerPlayer player, int delta) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        if (level < 0 || !Float.isFinite(progress) || progress < 0 || progress >= 1) return null;
        int needed = needed(level);
        if (needed <= 0 || needed != player.getXpNeededForNextLevel()) return null;
        long points = (long) Math.round(progress * needed) + delta;
        int nextLevel = level;
        while (points < 0 && nextLevel > 0) points += needed(--nextLevel);
        if (points < 0) return null;
        while (points >= needed(nextLevel)) {
            int size = needed(nextLevel);
            if (size <= 0) return null;
            points -= size;
            nextLevel++;
        }
        if (needed(nextLevel) <= 0) return null;
        return new Change(level, progress, player.totalExperience, nextLevel, (int) points, delta);
    }

    private static int needed(int level) {
        long amount = level >= 30 ? 9L * level - 158 : level >= 15 ? 5L * level - 38 : 2L * level + 7;
        return amount > Integer.MAX_VALUE ? -1 : (int) amount;
    }
}
