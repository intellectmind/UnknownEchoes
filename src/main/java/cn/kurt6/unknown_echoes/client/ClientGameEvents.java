package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.network.DoubleJumpPayload;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 风之回响体验能力:二段跳(客户端表现)。
 * 服务端通过 DoubleJumpPayload 校验能力并重置摔落距离;
 * 权限判定(风门等)始终在服务端,与本类无关。
 */
@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    private static boolean usedDoubleJump = false;
    private static boolean wasGrounded = true;
    /** 风之第三跳滑翔剩余 tick(>0 = 滑翔中)。 */
    private static int glideTicks = 0;
    /** 用竖直速度区分"这一下是地面起跳"还是"已经被地形/气流带离地后的空中跳"。 */
    private static final double TAKEOFF_JUMP_VERTICAL_SPEED = 0.2D;
    private static final int WIND_PLATFORM_TAKEOFF_CHECK_HEIGHT = 24;
    private static BossMusicSound bossMusic = null;
    private static int bossMusicScanCooldown = 0;

    /** HUD 表现层查询。 */
    public static boolean isDoubleJumpUsed() {
        return usedDoubleJump;
    }

    public static boolean isGliding() {
        return glideTicks > 0;
    }

    /** 服务端拒绝滑翔(V0.6E 能量/冷却/状态校验):取消滑翔表现并显示含蓄提示。 */
    public static void onGlideDenied(String reason) {
        glideTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.unknown_echoes.glide.deny." + reason), true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            usedDoubleJump = false;
            wasGrounded = true;
            glideTicks = 0;
            return;
        }

        // 回响总览(5.8 统一入口壳):只保留 K 作为 GUI 总入口,子页由界面内导航进入。
        while (ClientModEvents.OPEN_ECHO_OVERVIEW.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen(
                        cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.ModulePage.OVERVIEW));
            }
        }

        tickBossMusic(mc, player);
        tickTrueSightShimmer(player);
        tickTrueSightIllusionPulse(player);

        // 二段跳:用 consumeClick(按键事件计数)而不是 isDown 逐 tick 采样,
        // 否则快速双击时第二下落在两个 tick 之间会被漏掉(实测发生过)。
        int jumpClicks = 0;
        while (mc.options.keyJump.consumeClick()) {
            jumpClicks++;
        }
        boolean grounded = player.onGround() || player.isInWater() || player.isInLava()
                || player.getAbilities().flying || player.onClimbable();
        int airClicks = airClickCount(player, grounded, jumpClicks);
        if (grounded) {
            usedDoubleJump = false;
            glideTicks = 0;
        } else if (airClicks > 0 && !usedDoubleJump
                && !player.getAbilities().mayfly      // 避免与创造飞行双击跳冲突
                && !player.isFallFlying() && !player.isPassenger()
                && ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 1.1, 0.55, motion.z * 1.1);
            player.fallDistance = 0.0F;
            usedDoubleJump = true;
            PacketDistributor.sendToServer(DoubleJumpPayload.JUMP);
            if (ClientConfig.ENABLE_ECHO_PARTICLES.get()) {
                for (int i = 0; i < 8; i++) {
                    player.level().addParticle(ParticleTypes.CLOUD,
                            player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                            player.getY(),
                            player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.6,
                            0.0, -0.05, 0.0);
                }
            }
        } else if (airClicks > 0 && usedDoubleJump && glideTicks <= 0
                && ClientConfig.WIND_THIRD_JUMP_GLIDE.get()
                && !player.getAbilities().mayfly
                && !player.isFallFlying() && !player.isPassenger()
                && ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)
                && ClientAbilityCache.getResearchLevel(EchoAbilityType.WIND_ECHO) >= 1) {
            // 第三跳:短距离滑翔。研究等级延长滑翔时间(风之研究强化);
            // 聆听者 4 件再 +20 tick(11.3,客户端读自己装备做手感,服务端权威同步加成)。
            glideTicks = windGlideDuration()
                    + (cn.kurt6.unknown_echoes.ability.EchoArmorSets.hasListenerAbilityBonus(player) ? 20 : 0);
            PacketDistributor.sendToServer(DoubleJumpPayload.GLIDE_START);
        }
        tickGlide(player);
        wasGrounded = grounded;
    }

    private static int airClickCount(LocalPlayer player, boolean grounded, int jumpClicks) {
        if (jumpClicks <= 0) {
            return 0;
        }
        // 本事件在玩家 tick 之后触发:地面起跳那一 tick 玩家已离地,第一下点击属于起跳本身。
        // 但走下边缘、被风流平台抬起后再按跳,也会出现 wasGrounded=true 且本 tick 离地;
        // 此时若竖直速度不是一次正常跳跃的上冲,就应把这一下算作空中跳。
        boolean justJumpedFromGround = wasGrounded && !grounded
                && player.getDeltaMovement().y > TAKEOFF_JUMP_VERTICAL_SPEED
                && !isInActiveWindCurrentColumn(player);
        return justJumpedFromGround ? Math.max(0, jumpClicks - 1) : jumpClicks;
    }

    private static boolean isInActiveWindCurrentColumn(LocalPlayer player) {
        BlockPos base = player.blockPosition();
        int minY = Math.max(player.level().getMinBuildHeight(),
                base.getY() - WIND_PLATFORM_TAKEOFF_CHECK_HEIGHT);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = base.getY(); y >= minY; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(base.getX() + dx, y, base.getZ() + dz);
                    var state = player.level().getBlockState(cursor);
                    if (!state.is(ModBlocks.WIND_CURRENT_PLATFORM.get())
                            || !state.getValue(WindCurrentPlatformBlock.ACTIVE)) {
                        continue;
                    }
                    if (player.getX() >= cursor.getX() - 0.75D
                            && player.getX() <= cursor.getX() + 1.75D
                            && player.getZ() >= cursor.getZ() - 0.75D
                            && player.getZ() <= cursor.getZ() + 1.75D) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 滑翔表现:限制下落速度、轻微保持前进;摔落距离由服务端在 DoubleJumpPayload 中重置。 */
    private static void tickGlide(LocalPlayer player) {
        if (glideTicks <= 0) {
            return;
        }
        if (player.isInWater() || player.isInLava() || player.isFallFlying() || player.isPassenger()) {
            glideTicks = 0;
            return;
        }
        glideTicks--;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < -0.08) {
            player.setDeltaMovement(motion.x * 1.02, -0.08, motion.z * 1.02);
        }
        player.fallDistance = 0.0F;
        if (player.tickCount % 10 == 0) {
            PacketDistributor.sendToServer(DoubleJumpPayload.GLIDE_HOLD);
        }
        if (ClientConfig.ENABLE_ECHO_PARTICLES.get() && player.tickCount % 4 == 0) {
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                    player.getY() + 0.2,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                    0.0, -0.02, 0.0);
        }
    }

    private static int windGlideDuration() {
        int research = ClientAbilityCache.getResearchLevel(EchoAbilityType.WIND_ECHO);
        return 40 + (research >= 2 ? 20 : 0);
    }

    /** 真视回响体验层:基础只给近距隐纹微光;研究 1/2 提升感知效率,不解锁新权限。 */
    private static void tickTrueSightShimmer(LocalPlayer player) {
        if (player.tickCount % 10 != 0
                || !ClientAbilityCache.hasAbility(EchoAbilityType.TRUE_SIGHT_ECHO)
                || !ClientConfig.ENABLE_ECHO_PARTICLES.get()) {
            return;
        }
        int range = trueSightShimmerRange();
        var random = player.getRandom();
        net.minecraft.core.BlockPos center = player.blockPosition();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-range, -5, -range), center.offset(range, 5, range))) {
            if (player.level().getBlockState(pos).is(
                    cn.kurt6.unknown_echoes.registry.ModBlocks.HIDDEN_RUNE_BRICKS.get())
                    && random.nextFloat() < 0.35F) {
                player.level().addParticle(ParticleTypes.END_ROD,
                        pos.getX() + random.nextDouble(),
                        pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(),
                        0.0, 0.01, 0.0);
            }
        }
    }

    /** 真视研究 1:幻象类敌人每 6 秒短暂显出镜边微光。
     *  只对客户端已同步实体打粒子,不携带机关答案或真假坐标数据。 */
    private static void tickTrueSightIllusionPulse(LocalPlayer player) {
        if (!ClientAbilityCache.hasAbility(EchoAbilityType.TRUE_SIGHT_ECHO)
                || !ClientConfig.ENABLE_ECHO_PARTICLES.get()) {
            return;
        }
        int research = ClientAbilityCache.getResearchLevel(EchoAbilityType.TRUE_SIGHT_ECHO);
        if (research < 1) {
            return;
        }
        int phase = player.tickCount % 120;
        if (phase >= 10 || phase % 2 != 0) {
            return;
        }
        double range = research >= 2 ? 16.0D : 10.0D;
        var random = player.getRandom();
        int marked = 0;
        for (var entity : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity.isAlive() && entity.getType().is(ModEntityTags.ILLUSION_MOBS))) {
            if (entity == player) {
                continue;
            }
            for (int i = 0; i < 3; i++) {
                player.level().addParticle(ParticleTypes.END_ROD,
                        entity.getX() + (random.nextDouble() - 0.5D) * entity.getBbWidth(),
                        entity.getY() + entity.getBbHeight() * (0.25D + random.nextDouble() * 0.65D),
                        entity.getZ() + (random.nextDouble() - 0.5D) * entity.getBbWidth(),
                        0.0D, 0.015D, 0.0D);
            }
            if (++marked >= 6) {
                return;
            }
        }
    }

    private static int trueSightShimmerRange() {
        int research = ClientAbilityCache.getResearchLevel(EchoAbilityType.TRUE_SIGHT_ECHO);
        if (research >= 2) {
            return 16;
        }
        return research >= 1 ? 10 : 6;
    }

    /** 靠近存活的主线 Boss 时播放对应 Boss BGM,远离/Boss 死亡由音轨自身淡出。 */
    private static void tickBossMusic(Minecraft mc, LocalPlayer player) {
        if (bossMusic != null && !mc.getSoundManager().isActive(bossMusic)) {
            bossMusic = null;
        }
        // 静默领域(沉默祭司):领域内音乐被刻意压低/掐断——表现与机制一致(10.4.2)
        boolean silenceDomain = !player.level().getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.SilentPriest.class,
                player.getBoundingBox().inflate(cn.kurt6.unknown_echoes.entity.boss.SilentPriest.DOMAIN_RADIUS),
                priest -> priest.isAlive() && priest.isDomainActive()).isEmpty();
        if (silenceDomain) {
            mc.getMusicManager().stopPlaying();
            if (bossMusic != null) {
                mc.getSoundManager().stop(bossMusic);
                bossMusic = null;
            }
            return;
        }
        if (bossMusic != null) {
            // Boss 战期间持续压制原版背景音乐:原版 MusicManager 会在战斗中随机插曲,
            // 只在开播时停一次不够,必须每 tick 掐掉(无曲目播放时本调用是空操作)
            mc.getMusicManager().stopPlaying();
        }
        if (--bossMusicScanCooldown > 0 || bossMusic != null
                || !ClientConfig.PLAY_ECHO_AMBIENT_SOUNDS.get()) {
            return;
        }
        bossMusicScanCooldown = 20;
        var area = player.getBoundingBox().inflate(48.0D);
        var colossi = player.level().getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus.class, area,
                boss -> boss.isAlive());
        if (!colossi.isEmpty()) {
            startBossMusic(mc, new BossMusicSound(colossi.get(0),
                    cn.kurt6.unknown_echoes.registry.ModSounds.COLOSSUS_MUSIC.get()));
            return;
        }
        var watchers = player.level().getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher.class, area,
                boss -> boss.isAlive() && !boss.isClone());
        if (!watchers.isEmpty()) {
            startBossMusic(mc, new BossMusicSound(watchers.get(0),
                    cn.kurt6.unknown_echoes.registry.ModSounds.ABYSS_WATCHER_MUSIC.get()));
            return;
        }
        var guardians = player.level().getEntitiesOfClass(
                cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian.class, area,
                boss -> boss.isAlive() && !boss.isIllusion());
        if (!guardians.isEmpty()) {
            startBossMusic(mc, new BossMusicSound(guardians.get(0),
                    ModSounds.MIRROR_GUARDIAN_MUSIC.get()));
            return;
        }
        // Mini Boss 不再播放专属战斗 BGM:仅主线 Boss(遗忘巨像 / 深渊观测者 / 镜像守护者)有战斗音乐,
        // 守护者只保留场地音效与原版氛围音(用户要求:只有 Boss 有战斗 BGM)。
    }

    private static void startBossMusic(Minecraft mc, BossMusicSound sound) {
        bossMusic = sound;
        mc.getMusicManager().stopPlaying();
        mc.getSoundManager().play(bossMusic);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAbilityCache.clear();
        ClientArtifactCache.clear();
        ClientJournalCache.clear();
        usedDoubleJump = false;
        wasGrounded = true;
        glideTicks = 0;
        bossMusic = null;
    }
}
