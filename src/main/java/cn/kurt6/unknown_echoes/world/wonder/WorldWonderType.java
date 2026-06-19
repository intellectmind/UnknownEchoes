package cn.kurt6.unknown_echoes.world.wonder;

/**
 * 第一维度世界奇观稳定 ID。当前版本先提供定义层与数据包入口,
 * 大型结构落地时必须复用这些 ID,避免线索、Wiki、完成度和存档记录漂移。
 */
public enum WorldWonderType {
    ECHO_WORLD_TREE("echo_world_tree", "echo_forest", 1),
    ETERNAL_ECHO_LIGHTHOUSE("eternal_echo_lighthouse", "mirror_lake", 1),
    MIRROR_SEA("mirror_sea", "mirror_lake", 2),
    INVERTED_MOUNTAINS("inverted_mountains", "floating_isles", 2),
    SKY_RIFT("sky_rift", "floating_isles", 2),
    SILENT_GREAT_BOAT("silent_great_boat", "silent_swamp", 3),
    BROKEN_BELL_TOWER("broken_bell_tower", "broken_bell_wastes", 3);

    private final String id;
    private final String region;
    private final int priority;

    WorldWonderType(String id, String region, int priority) {
        this.id = id;
        this.region = region;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public String getRegion() {
        return region;
    }

    public int getPriority() {
        return priority;
    }

    public static WorldWonderType byId(String id) {
        for (WorldWonderType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
