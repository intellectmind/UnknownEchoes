package cn.kurt6.unknown_echoes.block.tide;

import cn.kurt6.unknown_echoes.ability.EchoAbilityManager;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 水下符文:沉没圣殿与镜湖湖底的可读碑文(潮汐回响权限链)。
 * - 无潮汐回响:右键只看到模糊水纹(服务端判定,水下呼吸 Mod 绕不过)。
 * - 有潮汐回响:读出铭文(按坐标确定 4 组文本之一),服务于线索与回访。
 * - Boss 机制(V0.5):深渊观测者战斗中会"侵蚀"圣殿符文(CORRUPTED=true,变暗+黑泡),
 *   任何玩家右键即可净化(战斗机制不查权限)——全部净化后观测者短暂破防。
 *   守卫拦截(V1.7):符文 5 格内有存活黑潮拟影时净化失败,需先击杀或引开守卫。
 */
public class TideRuneBlock extends Block {
    public static final MapCodec<TideRuneBlock> CODEC = simpleCodec(TideRuneBlock::new);
    /** 被深渊观测者侵蚀:由 Boss 写入,玩家净化;只在 Boss 战期间出现。 */
    public static final BooleanProperty CORRUPTED = BooleanProperty.create("corrupted");
    /** 覆盖符文簇半径 + Boss 游荡半径,避免 Boss 走到场地边缘时净化通知漏发。 */
    private static final double WATCHER_NOTIFY_RANGE = 64.0D;

    /** 铭文文本组数(lang 键 rune.unknown_echoes.tide.<0..N-1>)。 */
    public static final int INSCRIPTION_COUNT = 4;

    public TideRuneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CORRUPTED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORRUPTED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // Boss 机制优先:净化被侵蚀的符文(不查能力权限——这正是获取潮汐回响的试炼)
        if (state.getValue(CORRUPTED)) {
            // 黑潮守卫:符文旁有存活拟影时净化失败,先击杀或引开守卫(V1.7 净化压力战)
            if (level instanceof ServerLevel serverLevel
                    && AbyssWatcher.isRuneGuarded(serverLevel, pos)) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.04);
                level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                        SoundSource.BLOCKS, 1.0F, 0.5F);
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.tide_rune.guarded"), true);
                return InteractionResult.CONSUME;
            }
            level.setBlock(pos, state.setValue(CORRUPTED, Boolean.FALSE), 3);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.5F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.05);
                notifyNearbyWatcher(serverLevel, pos, player);
            }
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune.purified"), true);
            return InteractionResult.CONSUME;
        }
        if (!EchoPermission.canUseEchoMechanism(player, EchoAbilityType.TIDE_ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.unknown_echoes.tide_rune.blurred"), true);
            return InteractionResult.CONSUME;
        }
        // 按坐标稳定取一组铭文,同一块符文每次读到同样的内容
        int index = Math.floorMod(pos.hashCode(), INSCRIPTION_COUNT);
        player.displayClientMessage(Component.translatable("rune.unknown_echoes.tide.title")
                .withStyle(ChatFormatting.DARK_AQUA), false);
        player.displayClientMessage(Component.translatable("rune.unknown_echoes.tide." + index), false);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F);
        // 回访点研究来源(V0.6B):每块符文首次读取 +1 潮汐研究点(个人绑定,按坐标去重)
        if (player instanceof ServerPlayer serverPlayer) {
            String key = "research_revisit:tide_rune:" + level.dimension().location() + ":" + pos.asLong();
            if (!EchoAbilityManager.hasActivatedMechanism(serverPlayer, key)) {
                EchoAbilityManager.activateMechanism(serverPlayer, key);
                EchoAbilityManager.addResearchPoints(serverPlayer, EchoAbilityType.TIDE_ECHO, 1);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.unknown_echoes.research.insight"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** 净化后通知附近的深渊观测者重新检查破防条件。 */
    private static void notifyNearbyWatcher(ServerLevel level, BlockPos pos, Player purifier) {
        for (AbyssWatcher watcher : level.getEntitiesOfClass(AbyssWatcher.class,
                new net.minecraft.world.phys.AABB(pos).inflate(WATCHER_NOTIFY_RANGE))) {
            if (!watcher.isClone()) {
                watcher.onRunePurified(purifier);
            }
        }
    }
}
