package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import cn.kurt6.unknown_echoes.block.wind.WindCurrentPlatformBlock;
import cn.kurt6.unknown_echoes.config.ClientConfig;
import cn.kurt6.unknown_echoes.event.WorldEchoWingsEvents;
import cn.kurt6.unknown_echoes.item.tool.AreaMiningSupport;
import cn.kurt6.unknown_echoes.network.DoubleJumpPayload;
import cn.kurt6.unknown_echoes.network.ToolSkillStatePayload;
import cn.kurt6.unknown_echoes.network.WorldEchoFlightPayload;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    private static boolean usedDoubleJump = false;
    private static boolean wasGrounded = true;
    private static int glideTicks = 0;
    private static final int WORLD_ECHO_FLIGHT_TOGGLE_WINDOW_TICKS = 7;
    private static boolean worldEchoFlightActive = false;
    private static int worldEchoFlightToggleTicks = 0;
    private static boolean echoToolSkillKeyActive = false;
    /** 用竖直速度区分"这一下是地面起跳"还是"已经被地形/气流带离地后的空中跳"。 */
    private static final double TAKEOFF_JUMP_VERTICAL_SPEED = 0.2D;
    /** 单 tick 内最多承认的跳跃按下次数，避免异常输入放大成连续切换。 */
    private static final int MAX_JUMP_PRESSES_PER_TICK = 4;
    /** 自行统计的跳跃按下边沿数：KeyMapping.consumeClick 会把 GLFW REPEAT 也算成点击。 */
    private static int jumpPressCount = 0;
    private static final int WIND_PLATFORM_TAKEOFF_CHECK_HEIGHT = 24;
    private static final int TRUE_SIGHT_CACHE_REFRESH_TICKS = 40;
    private static final int TRUE_SIGHT_CACHE_MOVE_DISTANCE = 8;
    private static final int TRUE_SIGHT_CACHE_LIMIT = 64;
    private static final int TRUE_SIGHT_PARTICLE_LIMIT = 6;
    private static List<BlockPos> trueSightHiddenCache = List.of();
    private static BlockPos trueSightCacheCenter = null;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> trueSightCacheDimension = null;
    private static int trueSightCacheRange = -1;

    public static boolean isDoubleJumpUsed() {
        return usedDoubleJump;
    }

    public static boolean isGliding() {
        return glideTicks > 0;
    }

    public static boolean isWorldEchoFlying() {
        return worldEchoFlightActive;
    }

    /** 服务端拒绝滑翔请求时清理表现并显示提示。 */
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
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.getOverlay() != null || mc.player == null) {
            return;
        }
        if (mc.options.keyJump.matches(event.getKey(), event.getScanCode())) {
            jumpPressCount = Math.min(jumpPressCount + 1, MAX_JUMP_PRESSES_PER_TICK);
        }
    }

    /** 跳跃键可被改绑到鼠标键；鼠标路径没有 REPEAT，但仍需并入同一计数来源。 */
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.getOverlay() != null || mc.player == null) {
            return;
        }
        if (mc.options.keyJump.matchesMouse(event.getButton())) {
            jumpPressCount = Math.min(jumpPressCount + 1, MAX_JUMP_PRESSES_PER_TICK);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        migrateLegacyArtifactKeyBinding();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            ClientBossMusic.clear(mc);
            usedDoubleJump = false;
            wasGrounded = true;
            glideTicks = 0;
            worldEchoFlightActive = false;
            worldEchoFlightToggleTicks = 0;
            echoToolSkillKeyActive = false;
            jumpPressCount = 0;
            drainVanillaJumpClicks(mc);
            return;
        }
        if (mc.isPaused()) {

            return;
        }

        handleOpenEchoOverviewKey(mc, player);
        handleWeaponSkillKey(mc, player);
        handleWeaponSecondarySkillKey(mc, player);
        handleGravityAttuneKey(mc, player);
        handleVoidStepKey(mc, player);
        handleItemActiveSkillKey(mc, player);
        handleItemAlternateSkillKey(mc, player);
        handleArtifactUseKey(mc, player);

        ClientBossMusic.tick(mc);
        tickTrueSightShimmer(player);
        tickTrueSightIllusionPulse(player);
        tickEchoToolSkillKey(mc, player);

        int jumpClicks = jumpPressCount;
        jumpPressCount = 0;
        drainVanillaJumpClicks(mc);
        if (tickWorldEchoFlight(player, jumpClicks)) {
            jumpClicks = 0;
        }
        boolean grounded = player.onGround() || player.isInWater() || player.isInLava()
                || player.getAbilities().flying || player.onClimbable();
        int airClicks = airClickCount(player, grounded, jumpClicks);
        if (grounded) {
            usedDoubleJump = false;
            glideTicks = 0;
        } else if (airClicks > 0 && !usedDoubleJump
                && !player.getAbilities().mayfly
                && !player.isFallFlying() && !player.isPassenger()
                && ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 1.1, 0.55, motion.z * 1.1);
            player.fallDistance = 0.0F;
            usedDoubleJump = true;
            PacketDistributor.sendToServer(DoubleJumpPayload.JUMP);
            if (ClientConfig.ENABLE_ECHO_PARTICLES.get()) {
                ClientEquipmentFx.wind(player, false);
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

            glideTicks = windGlideDuration()
                    + (cn.kurt6.unknown_echoes.ability.EchoArmorSets.hasListenerAbilityBonus(player) ? 20 : 0)
                    + (cn.kurt6.unknown_echoes.ability.EchoArmorSets.hasResonatorAbilityBonus(player) ? 10 : 0);
            PacketDistributor.sendToServer(DoubleJumpPayload.GLIDE_START);
        }
        tickGlide(player);
        wasGrounded = grounded;
    }

    private static void handleOpenEchoOverviewKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.OPEN_ECHO_OVERVIEW.consumeClick()) {
        if (mc.screen == null) {
            mc.setScreen(new cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen(
                    cn.kurt6.unknown_echoes.client.gui.overview.EchoOverviewScreen.ModulePage.OVERVIEW));
        }
        }
    }

    private static void handleWeaponSkillKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.WEAPON_SKILL.consumeClick()) {
        if (mc.screen != null || player.isSpectator()) {
            continue;
        }
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        if (main.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill
                && !player.getCooldowns().isOnCooldown(main.getItem())) {
            PacketDistributor.sendToServer(cn.kurt6.unknown_echoes.network.WeaponSkillPayload.MAIN);
        } else if (off.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill
                && !player.getCooldowns().isOnCooldown(off.getItem())) {
            PacketDistributor.sendToServer(cn.kurt6.unknown_echoes.network.WeaponSkillPayload.OFF);
        }
        }
    }

    private static void handleWeaponSecondarySkillKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.WEAPON_SECONDARY_SKILL.consumeClick()) {
        if (mc.screen != null || player.isSpectator()) {
            continue;
        }
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        if (main.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill skill
                && skill.hasSecondarySkill()
                && !player.getCooldowns().isOnCooldown(main.getItem())) {
            PacketDistributor.sendToServer(
                    cn.kurt6.unknown_echoes.network.SecondaryWeaponSkillPayload.MAIN);
        } else if (off.getItem() instanceof cn.kurt6.unknown_echoes.item.weapon.WeaponSkill skill
                && skill.hasSecondarySkill()
                && !player.getCooldowns().isOnCooldown(off.getItem())) {
            PacketDistributor.sendToServer(
                    cn.kurt6.unknown_echoes.network.SecondaryWeaponSkillPayload.OFF);
        }
        }
    }

    private static void handleGravityAttuneKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.GRAVITY_ATTUNE.consumeClick()) {
        if (mc.screen == null && !player.isSpectator()
                && ClientAbilityCache.hasAbility(EchoAbilityType.GRAVITY_ECHO)) {
            PacketDistributor.sendToServer(
                    cn.kurt6.unknown_echoes.network.EchoAbilityActionPayload.GRAVITY_ATTUNE);
        }
        }
    }

    private static void handleVoidStepKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.VOID_STEP.consumeClick()) {
        if (mc.screen == null && !player.isSpectator()
                && ClientAbilityCache.hasAbility(EchoAbilityType.VOID_ECHO)) {
            PacketDistributor.sendToServer(
                    cn.kurt6.unknown_echoes.network.EchoAbilityActionPayload.VOID_STEP);
        }
        }
    }

    private static void handleItemActiveSkillKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.ITEM_ACTIVE_SKILL.consumeClick()) {
        if (mc.screen != null || player.isSpectator()) {
            continue;
        }
        sendItemActiveSkill(player, false);
        }
    }

    private static void handleItemAlternateSkillKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.ITEM_ALTERNATE_SKILL.consumeClick()) {
        if (mc.screen != null || player.isSpectator()) {
            continue;
        }
        sendItemActiveSkill(player, true);
        }
    }

    private static void handleArtifactUseKey(Minecraft mc, LocalPlayer player) {
        while (ClientModEvents.ARTIFACT_USE.consumeClick()) {
        if (mc.screen != null || player.isSpectator()) {
            continue;
        }
        net.minecraft.world.InteractionHand hand = artifactHand(player);
        if (hand != null) {
            PacketDistributor.sendToServer(new cn.kurt6.unknown_echoes.network.ArtifactUsePayload(
                    hand == net.minecraft.world.InteractionHand.OFF_HAND, false));
        }
        }
    }

    /** 旧版神器键默认未绑定；只迁移一次，保留玩家之后的自定义按键选择。 */
    private static void migrateLegacyArtifactKeyBinding() {
        if (ClientConfig.ARTIFACT_USE_KEY_MIGRATED.get()) {
            return;
        }
        if (ClientModEvents.ARTIFACT_USE.isUnbound()) {
            ClientModEvents.ARTIFACT_USE.setKey(ClientModEvents.ARTIFACT_USE.getDefaultKey());
        }
        ClientConfig.ARTIFACT_USE_KEY_MIGRATED.set(true);
    }

    /** 原版跳跃只读 isDown，从不消费 clickCount；这里清空它，避免计数长期堆积。 */
    private static void drainVanillaJumpClicks(Minecraft mc) {
        if (mc.options == null) {
            return;
        }
        while (mc.options.keyJump.consumeClick()) {

        }
    }

    /** 技能键作为采集修饰键；只在状态变化时发包，避免每 tick 产生网络流量。 */
    private static void tickEchoToolSkillKey(Minecraft mc, LocalPlayer player) {
        boolean supportedTool = AreaMiningSupport.usesToolSkillKey(player.getMainHandItem());
        boolean active = mc.screen == null && player.isAlive() && !player.isSpectator()
                && supportedTool && ClientModEvents.ECHO_TOOL_SKILL.isDown();
        if (active == echoToolSkillKeyActive) {
            return;
        }
        echoToolSkillKeyActive = active;
        PacketDistributor.sendToServer(new ToolSkillStatePayload(active));
    }

    /** 双击跳跃切换翼装飞行；本地先预测，服务端随后校验胸甲与玩家状态。 */
    private static boolean tickWorldEchoFlight(LocalPlayer player, int jumpClicks) {
        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean equipped = chest.is(ModItems.WORLD_ECHO_WINGS.get());
        boolean wingsUsable = equipped;
        boolean customFlightAvailable = wingsUsable && !hasExternalFlightAccess(player)
                && player.isAlive() && !player.isSpectator() && !player.isPassenger();
        if (!customFlightAvailable) {
            worldEchoFlightToggleTicks = 0;
            if (worldEchoFlightActive) {
                setWorldEchoFlight(player, false, true);
            }
            return wingsUsable;
        }

        if (worldEchoFlightToggleTicks > 0) {
            worldEchoFlightToggleTicks--;
        }
        for (int i = 0; i < jumpClicks; i++) {
            if (worldEchoFlightToggleTicks > 0) {
                setWorldEchoFlight(player, !worldEchoFlightActive, true);
                worldEchoFlightToggleTicks = 0;
            } else {
                worldEchoFlightToggleTicks = WORLD_ECHO_FLIGHT_TOGGLE_WINDOW_TICKS;
            }
        }

        if (worldEchoFlightActive) {
            player.getAbilities().flying = true;
            player.fallDistance = 0.0F;
            glideTicks = 0;
        }
        return true;
    }

    private static boolean hasExternalFlightAccess(LocalPlayer player) {
        if (player.getAbilities().mayfly) {
            return true;
        }
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute == null) {
            return false;
        }
        return flightAttribute.getBaseValue() > 0.0D
                || flightAttribute.getModifiers().stream().anyMatch(modifier ->
                        !modifier.id().equals(WorldEchoWingsEvents.FLIGHT_ATTRIBUTE_MODIFIER_ID)
                                && modifier.amount() > 0.0D);
    }

    private static void setWorldEchoFlight(LocalPlayer player, boolean flying, boolean notifyServer) {
        worldEchoFlightActive = flying;
        if (flying) {
            player.getAbilities().flying = true;
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.42D), motion.z);
            player.fallDistance = 0.0F;
            glideTicks = 0;
        } else if (!hasExternalFlightAccess(player)) {
            player.getAbilities().flying = false;
        }
        if (notifyServer) {
            PacketDistributor.sendToServer(flying
                    ? WorldEchoFlightPayload.START
                    : WorldEchoFlightPayload.STOP);
        }
    }

    /** 接收服务端确认；拒绝起飞或强制收翼时撤销客户端预测。 */
    public static void onWorldEchoFlightState(UUID playerId, boolean flying) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.getUUID().equals(playerId)) {
            return;
        }
        if (flying) {
            worldEchoFlightActive = true;
            player.getAbilities().flying = true;
            player.fallDistance = 0.0F;
        } else {
            worldEchoFlightActive = false;
            worldEchoFlightToggleTicks = 0;
            if (!hasExternalFlightAccess(player)) {
                player.getAbilities().flying = false;
            }
        }
    }

    /** 主手优先派发物品主动技能；副功能只发给声明支持的物品。 */
    private static void sendItemActiveSkill(LocalPlayer player, boolean alternate) {
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        if (supportsItemSkill(main, alternate) && (alternate || !isArtifactItem(main))) {
            PacketDistributor.sendToServer(alternate
                    ? cn.kurt6.unknown_echoes.network.ItemActiveSkillPayload.MAIN_ALTERNATE
                    : cn.kurt6.unknown_echoes.network.ItemActiveSkillPayload.MAIN);
        } else if (supportsItemSkill(off, alternate) && (alternate || !isArtifactItem(off))) {
            PacketDistributor.sendToServer(alternate
                    ? cn.kurt6.unknown_echoes.network.ItemActiveSkillPayload.OFF_ALTERNATE
                    : cn.kurt6.unknown_echoes.network.ItemActiveSkillPayload.OFF);
        }
    }

    /** 神器主技能走独立可改绑键；副功能仍复用物品副功能键以兼容罗盘与锚点。 */
    private static boolean isArtifactItem(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof cn.kurt6.unknown_echoes.item.GenericArtifactItem
                || stack.is(ModItems.STORM_COMPASS.get());
    }

    private static boolean supportsItemSkill(net.minecraft.world.item.ItemStack stack,
                                             boolean alternate) {
        return stack.getItem() instanceof cn.kurt6.unknown_echoes.item.ItemActiveSkill skill
                && (!alternate || skill.hasAlternateItemSkill());
    }

    private static net.minecraft.world.InteractionHand artifactHand(LocalPlayer player) {
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            String id = stack.get(cn.kurt6.unknown_echoes.registry.ModDataComponents.ARTIFACT_ID.get());
            Integer serial = stack.get(cn.kurt6.unknown_echoes.registry.ModDataComponents.CREDENTIAL_SERIAL.get());
            String owner = stack.get(cn.kurt6.unknown_echoes.registry.ModDataComponents.ARTIFACT_OWNER.get());
            ClientArtifactCache.Entry entry = id == null ? null : ClientArtifactCache.get(id);
            if (entry != null && serial != null && serial == entry.serial()
                    && (owner == null || owner.isBlank() || player.getUUID().toString().equals(owner))) {
                return hand;
            }
        }
        return null;
    }

    private static int airClickCount(LocalPlayer player, boolean grounded, int jumpClicks) {
        if (jumpClicks <= 0) {
            return 0;
        }

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
        if (!player.isAlive() || player.isSpectator()
                || !ClientAbilityCache.hasAbility(EchoAbilityType.WIND_ECHO)
                || player.isInWater() || player.isInLava() || player.isFallFlying() || player.isPassenger()) {
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
            if (player.tickCount % 12 == 0) {
                ClientEquipmentFx.wind(player, true);
            }
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
        if (!ClientAbilityCache.hasAbility(EchoAbilityType.TRUE_SIGHT_ECHO)
                || !ClientConfig.ENABLE_ECHO_PARTICLES.get()) {
            clearTrueSightCache();
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        int range = trueSightShimmerRange();
        var random = player.getRandom();
        BlockPos center = player.blockPosition();
        if (shouldRefreshTrueSightCache(player, center, range)) {
            refreshTrueSightCache(player, center, range);
        }
        int particles = 0;
        for (BlockPos pos : trueSightHiddenCache) {
            if (player.level().getBlockState(pos).is(ModBlocks.HIDDEN_RUNE_BRICKS.get())
                    && random.nextFloat() < 0.35F) {
                player.level().addParticle(ParticleTypes.END_ROD,
                        pos.getX() + random.nextDouble(),
                        pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(),
                        0.0, 0.01, 0.0);
                if (++particles >= TRUE_SIGHT_PARTICLE_LIMIT) {
                    break;
                }
            }
        }
    }

    private static boolean shouldRefreshTrueSightCache(LocalPlayer player, BlockPos center, int range) {
        return trueSightCacheCenter == null
                || trueSightCacheRange != range
                || !player.level().dimension().equals(trueSightCacheDimension)
                || player.tickCount % TRUE_SIGHT_CACHE_REFRESH_TICKS == 0
                || trueSightCacheCenter.distManhattan(center) >= TRUE_SIGHT_CACHE_MOVE_DISTANCE;
    }

    private static void refreshTrueSightCache(LocalPlayer player, BlockPos center, int range) {
        List<BlockPos> found = new ArrayList<>();
        int matches = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-range, -5, -range), center.offset(range, 5, range))) {
            if (player.level().getBlockState(pos).is(ModBlocks.HIDDEN_RUNE_BRICKS.get())) {
                matches++;
                if (found.size() < TRUE_SIGHT_CACHE_LIMIT) {
                    found.add(pos.immutable());
                } else {
                    int replacement = player.getRandom().nextInt(matches);
                    if (replacement < TRUE_SIGHT_CACHE_LIMIT) {
                        found.set(replacement, pos.immutable());
                    }
                }
            }
        }
        trueSightHiddenCache = List.copyOf(found);
        trueSightCacheCenter = center.immutable();
        trueSightCacheDimension = player.level().dimension();
        trueSightCacheRange = range;
    }

    private static void clearTrueSightCache() {
        trueSightHiddenCache = List.of();
        trueSightCacheCenter = null;
        trueSightCacheDimension = null;
        trueSightCacheRange = -1;
    }

    /** 真视研究 1:幻象类敌人每 6 秒短暂显出镜边微光。 */
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
            ClientEquipmentFx.trueSight(entity);
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

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBossMusic.clear(Minecraft.getInstance());
        ClientAbilityCache.clear();
        ClientAbilityCooldownCache.clear();
        ClientArtifactCache.clear();
        ClientJournalCache.clear();
        ClientJourneyCache.clear();
        ClientStudyCache.clear();
        ClientChaseCache.clear();
        ClientTradeCache.clearSession();
        ClientWorldEchoFlightCache.clear();
        cn.kurt6.unknown_echoes.client.gui.EchoBossBarOverlay.clearCaches();
        usedDoubleJump = false;
        wasGrounded = true;
        glideTicks = 0;
        worldEchoFlightActive = false;
        worldEchoFlightToggleTicks = 0;
        echoToolSkillKeyActive = false;
        jumpPressCount = 0;
        clearTrueSightCache();
    }
}
