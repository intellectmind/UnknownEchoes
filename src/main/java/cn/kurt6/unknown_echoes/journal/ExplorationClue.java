package cn.kurt6.unknown_echoes.journal;

import net.minecraft.core.BlockPos;

public record ExplorationClue(String structureId, String dimension, BlockPos pos, boolean visited) {

    public String encode() {
        return structureId + "|" + dimension + "|"
                + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + (visited ? 1 : 0);
    }

    // 解析失败返回 null，调用方跳过即可。
    public static ExplorationClue decode(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length != 6 || !("0".equals(parts[5]) || "1".equals(parts[5]))) {
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

    // 抵达状态变化不改变目的地身份。
    public boolean sameSite(ExplorationClue other) {
        return other != null && structureId.equals(other.structureId)
                && dimension.equals(other.dimension) && pos.equals(other.pos);
    }
}
