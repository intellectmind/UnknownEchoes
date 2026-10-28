package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.artifact.ArtifactUseGuard;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.journal.JournalNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
        if (!isCurrentInput(level, player, stack, hand)) {
            return false;
        }
        useOnServer(level, player, stack, hand, false);
        return true;
    }

    @Override
    public boolean hasAlternateItemSkill() {
        return true;
    }

    @Override
    public boolean activateAlternateItemSkill(ServerLevel level, ServerPlayer player,
                                              ItemStack stack, InteractionHand hand) {
        if (!isCurrentInput(level, player, stack, hand)) {
            return false;
        }
        return ArtifactUseGuard.run(player, ArtifactType.STORM_COMPASS, () -> {
            useOnServer(level, player, stack, hand, true);
            return true;
        });
    }

    private void useOnServer(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand,
                             boolean locateClue) {
        ArtifactType type = ArtifactType.STORM_COMPASS;
        if (!ArtifactManager.isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return;
        }
        if (!ArtifactManager.validateCredential(player, type, stack)) {

            actionbar(player, "message.unknown_echoes.artifact.not_yours");
            PlayerSoundFeedback.deny(player);
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

        String tuning = ArtifactManager.getData(player).getTuning(type);
        JournalNavigation.Result navigation = JournalNavigation.resolve(JournalManager.getClues(player),
                JournalManager.getData(player).getDestination(), player.level().dimension().location().toString(),
                player.blockPosition(), tuning);
        if (navigation.status() == JournalNavigation.Status.OTHER_DIMENSION) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.storm_compass.other_dimension",
                    JournalNavigation.structureName(navigation.clue()),
                    JournalNavigation.dimensionName(navigation.clue().dimension())), true);
            return;
        }
        if (navigation.status() == JournalNavigation.Status.NEARBY) {
            player.displayClientMessage(Component.translatable("journal.unknown_echoes.clue.nearby",
                    JournalNavigation.structureName(navigation.clue())), true);
            return;
        }
        ExplorationClue target = navigation.clue();
        boolean windflow = ArtifactType.STORM_COMPASS.getHiddenWord().equals(tuning);
        BlockPos platform = windflow && player.level() instanceof ServerLevel serverLevel
                ? findWindflowPlatform(player, serverLevel) : null;
        if (target == null && platform == null) {
            actionbar(player, "message.unknown_echoes.storm_compass.no_clue");
            return;
        }
        boolean windflowAirborne = windflow && !player.onGround();
        if (windflowAirborne && !isAirborne(player)) {
            windflowAirborne = false;
        }
        Vec3 origin = new Vec3(player.getX(), player.getEyeY() - 0.2D, player.getZ());
        if (!windflowAirborne) {
            ArtifactManager.reserveCooldown(player, type, target == null ? 3
                    : ArtifactEffectManager.getCooldownSeconds(player, type));
        }
        if (!ArtifactManager.spendEnergy(player,
                ArtifactEffectManager.getEnergyCost(player, type))) {
            if (!windflowAirborne) {
                ArtifactManager.getData(player).clearCooldown(type);
            }
            return;
        }
        try {
            if (!canContinue(level, player, stack, hand)) {
                return;
            }
            if (platform != null) {
                pointToWindflowPlatform(player, platform, origin);
            }
            if (target != null && canContinue(level, player, stack, hand)) {
                pointTo(level, player, stack, hand, target, origin);
            }
        } finally {
            // 永续核心能量未变化时支付不发包，仍需同步这次预留的冷却。
            if (!windflowAirborne) {
                ArtifactManager.syncToClient(player);
            }
        }
    }

    private boolean isCurrentInput(ServerLevel level, ServerPlayer player,
                                    ItemStack stack, InteractionHand hand) {
        return player.level() == level && player.isAlive() && !player.isSpectator()
                && !player.hasDisconnected() && stack.is(this) && player.getItemInHand(hand) == stack;
    }

    private boolean canContinue(ServerLevel level, ServerPlayer player,
                                ItemStack stack, InteractionHand hand) {
        return isCurrentInput(level, player, stack, hand)
                && ArtifactManager.isEnabled(ArtifactType.STORM_COMPASS)
                && ArtifactManager.validateCredential(player, ArtifactType.STORM_COMPASS, stack);
    }

    private boolean isAirborne(ServerPlayer player) {
        return !player.isInWater() && !player.isInLava() && !player.onClimbable()
                && !player.isPassenger();
    }

    private BlockPos findWindflowPlatform(ServerPlayer player, ServerLevel level) {
        BlockPos platform = cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity
                .nearestPlatform(level, player.blockPosition());
        if (platform == null) {
            return null;
        }
        double pdx = platform.getX() + 0.5 - player.getX();
        double pdz = platform.getZ() + 0.5 - player.getZ();
        return pdx * pdx + pdz * pdz > 4 * 4 ? platform : null;
    }

    private void pointToWindflowPlatform(ServerPlayer player, BlockPos platform, Vec3 origin) {
        double pdx = platform.getX() + 0.5 - origin.x;
        double pdz = platform.getZ() + 0.5 - origin.z;
        double pdist = Math.sqrt(pdx * pdx + pdz * pdz);
        String distKey = pdist < 48 ? "near" : pdist < 160 ? "mid" : "far";
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.storm_compass.windflow",
                Component.translatable("direction.unknown_echoes."
                        + octant(pdx / pdist, pdz / pdist)),
                Component.translatable("message.unknown_echoes.storm_compass.dist."
                        + distKey)), false);
    }

    /** 指向演出:服务端粒子射线 + 含蓄方向文案(八方位 + 模糊距离,不给坐标)。 */
    private void pointTo(ServerLevel level, ServerPlayer player, ItemStack stack,
                         InteractionHand hand, ExplorationClue target, Vec3 origin) {
        double dx = target.pos().getX() + 0.5 - origin.x;
        double dz = target.pos().getZ() + 0.5 - origin.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double nx = dx / dist;
        double nz = dz / dist;

        ArtifactEffectManager.spawnCompassPointer(level, origin,
                new Vec3(nx, 0.06D, nz), 10.0D);
        player.playNotifySound(ModSounds.ITEM_STORM_COMPASS_LOCATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!canContinue(level, player, stack, hand)) {
            return;
        }

        String direction = octant(nx, nz);
        String distKey = dist < 200 ? "near" : dist < 800 ? "mid" : "far";
        player.displayClientMessage(Component.translatable(
                "message.unknown_echoes.storm_compass.point",
                JournalNavigation.structureName(target),
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
