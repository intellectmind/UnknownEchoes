package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.artifact.ArtifactType;
import cn.kurt6.unknown_echoes.config.ServerConfig;
import cn.kurt6.unknown_echoes.journal.ExplorationClue;
import cn.kurt6.unknown_echoes.journal.JournalManager;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 风暴罗盘(V0.6D 首件神器,12.3):记述派检索终端的凭据物品。
 * 物品只是凭据——等级/调谐/序号全部读玩家 ArtifactData;序号不符(他人/旧凭据)不可用。
 * 指向只读玩家个人日志线索(已发现遗迹 + 检索/线索地图写入的方位记忆),
 * 不做全图扫描,不显示精确坐标,只给八方位 + 模糊距离(12.2/12.3 限制)。
 */
public class StormCompassItem extends Item {

    public StormCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        useOnServer(serverPlayer, stack);
        return InteractionResultHolder.consume(stack);
    }

    private void useOnServer(ServerPlayer player, ItemStack stack) {
        ArtifactType type = ArtifactType.STORM_COMPASS;
        if (!ArtifactManager.isEnabled(type)) {
            actionbar(player, "message.unknown_echoes.artifact.disabled");
            return;
        }
        if (!ArtifactManager.validateCredential(player, type, stack)) {
            // 含蓄文案:他人/旧凭据(12.2)
            actionbar(player, "message.unknown_echoes.artifact.not_yours");
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.5F);
            return;
        }
        if (ArtifactManager.isOnCooldown(player, type)) {
            actionbar(player, "message.unknown_echoes.storm_compass.cooldown");
            return;
        }
        if (!player.isShiftKeyDown()) {
            ArtifactEffectManager.use(player, type);
            return;
        }

        String tuning = ArtifactManager.getData(player).getTuning(type);
        ExplorationClue target = pickTarget(player, tuning);
        if (target == null) {
            actionbar(player, "message.unknown_echoes.storm_compass.no_clue");
            ArtifactManager.startCooldown(player, type, 3);
            return;
        }

        pointTo(player, target);

        // 冷却:基础值随等级小幅缩短(2 级小幅强化,12.4);
        // 隐藏词条"风流":滑翔/空中查看不进入冷却(指针稳定),并额外指示最近风流平台
        // (V0.6E 平台落地后补全;只查已加载区块的平台注册表,符合 12.3 限制)
        boolean windflow = ArtifactType.STORM_COMPASS.getHiddenWord().equals(tuning);
        if (windflow && player.level() instanceof ServerLevel serverLevel) {
            BlockPos platform = cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlockEntity
                    .nearestPlatform(serverLevel, player.blockPosition());
            if (platform != null) {
                double pdx = platform.getX() + 0.5 - player.getX();
                double pdz = platform.getZ() + 0.5 - player.getZ();
                double pdist = Math.sqrt(pdx * pdx + pdz * pdz);
                if (pdist > 4) {
                    String distKey = pdist < 48 ? "near" : pdist < 160 ? "mid" : "far";
                    player.displayClientMessage(Component.translatable(
                            "message.unknown_echoes.storm_compass.windflow",
                            Component.translatable("direction.unknown_echoes."
                                    + octant(pdx / pdist, pdz / pdist)),
                            Component.translatable("message.unknown_echoes.storm_compass.dist."
                                    + distKey)), false);
                }
            }
        }
        boolean windflowAirborne = windflow && !player.onGround();
        if (!windflowAirborne) {
            int level = ArtifactManager.getData(player).getLevel(type);
            int base = ServerConfig.STORM_COMPASS_COOLDOWN_SECONDS.get();
            ArtifactManager.startCooldown(player, type,
                    Math.max(1, (int) (base * (1.0 - 0.15 * (level - 1)))));
        }
    }

    /** 按调谐词条挑选目标线索:寻迹=未踏入优先;归途=已踏入(回访)优先;未调谐=最近。 */
    private ExplorationClue pickTarget(ServerPlayer player, String tuning) {
        String dim = player.level().dimension().location().toString();
        BlockPos origin = player.blockPosition();
        ExplorationClue preferred = null;
        ExplorationClue fallback = null;
        double preferredDist = Double.MAX_VALUE;
        double fallbackDist = Double.MAX_VALUE;
        boolean preferUnvisited = !"homeward".equals(tuning); // seek/windflow/未调谐 → 探新优先
        for (ExplorationClue clue : JournalManager.getClues(player)) {
            if (!clue.dimension().equals(dim)) {
                continue;
            }
            double dist = clue.pos().distSqr(origin);
            if (dist < 24 * 24) {
                continue; // 已经在跟前的不指
            }
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
            double eyeY = player.getEyeY() - 0.2;
            for (int i = 2; i <= 12; i++) {
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX() + nx * i * 0.9, eyeY + i * 0.05, player.getZ() + nz * i * 0.9,
                        1, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), eyeY, player.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
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
        double angle = Math.toDegrees(Math.atan2(nx, -nz)); // 0=北,顺时针
        if (angle < 0) {
            angle += 360;
        }
        String[] dirs = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return dirs[(int) Math.floor(((angle + 22.5) % 360) / 45)];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        Integer serial = stack.get(ModDataComponents.CREDENTIAL_SERIAL.get());
        if (serial != null) {
            tooltip.add(Component.translatable("tooltip.unknown_echoes.artifact.serial", serial)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.unknown_echoes.storm_compass.hint")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }

    private static void actionbar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
