package cn.kurt6.unknown_echoes.journal;

import net.minecraft.core.BlockPos;

/**
 * 探索线索(V0.6D):玩家个人日志中的"遗迹方位记忆"。
 * 来源:发现结构时自动落账(visited=true)、观测站核心检索/线索地图(visited=false)。
 * 风暴罗盘只读取这些个人线索指向,不做全图结构扫描(12.2 技术边界)。
 * 序列化为单行字符串存入 ExplorationJournalData.clues。
 */
public record ExplorationClue(String structureId, String dimension, BlockPos pos, boolean visited) {

    public String encode() {
        return structureId + "|" + dimension + "|"
                + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + (visited ? 1 : 0);
    }

    /** 解析失败(旧档/手改数据)返回 null,调用方跳过即可。 */
    public static ExplorationClue decode(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 6) {
            return null;
        }
        try {
            return new ExplorationClue(parts[0], parts[1],
                    new BlockPos(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4])),
                    "1".equals(parts[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public ExplorationClue asVisited() {
        return new ExplorationClue(structureId, dimension, pos, true);
    }
}
