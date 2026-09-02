package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 风暴罗盘：记述派检索终端的凭据物品。 */
public class StormCompassItem extends Item implements ItemActiveSkill {

    public StormCompassItem(Properties properties) {
        super(properties);
    }

    /** 飞行由「使用神器」键触发；检索指针走「物品副功能」键；右键不释放。 */
    @Override
    public boolean activateItemSkill(ServerLevel level, ServerPlayer player,
                                     ItemStack stack, InteractionHand hand) {
        useOnServer(player, stack, hand, false);
        return true;
    }

    @Override
    public boolean hasAlternateItemSkill() {
        return true;
    }

    @Override
    public boolean activateAlternateItemSkill(ServerLevel level, ServerPlayer player,
                                              ItemStack stack, InteractionHand hand) {
        useOnServer(player, stack, hand, true);
        return true;
    }

    private void useOnServer(ServerPlayer player, ItemStack stack, InteractionHand hand,
                             boolean locateClue) {
        ArtifactType type = ArtifactType.STORM_COMPASS;
        if (!ArtifactManager.isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return;
        }
        if (!ArtifactManager.validateCredential(player, type, stack)) {

            actionbar(player, "message.unknown_echoes.artifact.not_yours");
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.5F);
            return;
        }
        if (ArtifactManager.isOnCooldown(player, type)) {
            actionbar(player, "message.unknown_echoes.storm_compass.cooldown");
            return;
        }
        if (!locateClue) {
            ArtifactEffectManager.use(player, type, hand);
            return;
        }

        if (!ArtifactManager.spendEnergy(player,
                ArtifactEffectManager.getEnergyCost(player, type))) {
            return;
        }

        String tuning = ArtifactManager.getData(player).getTuning(type);
        ExplorationClue target = pickTarget(player, tuning);
        boolean windflow = ArtifactType.STORM_COMPASS.getHiddenWord().equals(tuning);
        if (windflow && player.level() instanceof ServerLevel serverLevel) {
            pointToWindflowPlatform(player, serverLevel);
        }
        boolean windflowAirborne = windflow && !player.onGround();
        if (windflowAirborne && !isAirborne(player)) {
            windflowAirborne = false;
        }
        if (target == null) {
            actionbar(player, "message.unknown_echoes.storm_compass.no_clue");
            if (!windflowAirborne) {
                ArtifactManager.startCooldown(player, type, 3);
            }
            return;
        }

        pointTo(player, target);

        if (!windflowAirborne) {
            ArtifactManager.startCooldown(player, type,
                    ArtifactEffectManager.getCooldownSeconds(player, type));
        }
    }

    private boolean isAirborne(ServerPlayer player) {
        return !player.isInWater() && !player.isInLava() && !player.onClimbable()
                && !player.isPassenger();
    }

    private void pointToWindflowPlatform(ServerPlayer player, ServerLevel level) {
        BlockPos platform = cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity
                .nearestPlatform(level, player.blockPosition());
        if (platform == null) {
            return;
        }
        double pdx = platform.getX() + 0.5 - player.getX();
        double pdz = platform.getZ() + 0.5 - player.getZ();
        double pdist = Math.sqrt(pdx * pdx + pdz * pdz);
        if (pdist <= 4) {
            return;
        }
        String distKey = pdist < 48 ? "near" : pdist < 160 ? "mid" : "far";
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.storm_compass.windflow",
                Component.translatable("direction.unknown_echoes."
                        + octant(pdx / pdist, pdz / pdist)),
                Component.translatable("message.unknown_echoes.storm_compass.dist."
                        + distKey)), false);
    }

    /** 按调谐词条挑选目标线索:寻迹=未踏入优先;归途=已踏入(回访)优先;未调谐=最近。 */
    private ExplorationClue pickTarget(ServerPlayer player, String tuning) {
        String dim = player.level().dimension().location().toString();
        BlockPos origin = player.blockPosition();
        ExplorationClue preferred = null;
        ExplorationClue fallback = null;
        double preferredDist = Double.MAX_VALUE;
        double fallbackDist = Double.MAX_VALUE;
        boolean preferUnvisited = !"homeward".equals(tuning);
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (!clue.dimension().equals(dim)) {
                continue;
            }
            double dx = (double) clue.pos().getX() - origin.getX();
            double dz = (double) clue.pos().getZ() - origin.getZ();
            double horizontalDistSqr = dx * dx + dz * dz;
            if (horizontalDistSqr < 24 * 24) {
                continue;
            }
            double dist = clue.pos().distSqr(origin);
            boolean isPreferred = clue.visited() != preferUnvisited;
            if (isPreferred && dist < preferredDist) {
                preferred = clue;
                preferredDist = dist;
            } else if (!isPreferred && dist < fallbackDist) {
                fallback = clue;
                fallbackDist = dist;
            }
        }
        return preferred != null ? preferred : fallback;
    }

    /** 指向演出:服务端粒子射线 + 含蓄方向文案(八方位 + 模糊距离,不给坐标)。 */
    private void pointTo(ServerPlayer player, ExplorationClue target) {
        double dx = target.pos().getX() + 0.5 - player.getX();
        double dz = target.pos().getZ() + 0.5 - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double nx = dx / dist;
        double nz = dz / dist;

        if (player.level() instanceof ServerLevel level) {
            // 指针略微上扬:纯水平的束在第一人称里被准星压住,读不出指向哪边。
            Vec3 origin = new Vec3(player.getX(), player.getEyeY() - 0.2D, player.getZ());
            ArtifactEffectManager.spawnCompassPointer(level, origin,
                    new Vec3(nx, 0.06D, nz), 10.0D);
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);

        String direction = octant(nx, nz);
        String distKey = dist < 200 ? "near" : dist < 800 ? "mid" : "far";
        ResourceLocation structureId = ResourceLocation.tryParse(target.structureId());
        Component structureName = structureId == null
                ? Component.translatable("artifact.unknown_echoes.storm_compass.unknown_ruin")
                : Component.translatable("structure.unknown_echoes." + structureId.getPath());
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.storm_compass.point",
                structureName,
                Component.translatable("direction.unknown_echoes." + direction),
                Component.translatable("message.unknown_echoes.storm_compass.dist." + distKey)), false);
    }

    /** 水平向量 → 八方位键(n/ne/e/se/s/sw/w/nw)。 */
    private static String octant(double nx, double nz) {
        double angle = Math.toDegrees(Math.atan2(nx, -nz));
        if (angle < 0) {
            angle += 360;
        }
        String[] dirs = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return dirs[(int) Math.floor(((angle + 22.5) % 360) / 45)];
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        ArtifactTooltip.add(tooltip, ArtifactType.STORM_COMPASS);
    }
}
