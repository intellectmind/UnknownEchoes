package cn.kurt6.unknown_echoes.journal;

import net.minecraft.core.BlockPos;

public record ExplorationClue(String structureId, String dimension, BlockPos pos, boolean visited) {

    public String encode() {
        return structureId + "|" + dimension + "|"
                + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + (visited ? 1 : 0);
    }

    /** 解析失败返回 null,调用方跳过即可。 */
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
