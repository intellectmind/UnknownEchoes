package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.fx.PlayerSoundFeedback;
import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.block.RealmHabitatChimeBlock;
import cn.kurt6.unknown_echoes.ecology.RealmLivingTrailManager;
import cn.kurt6.unknown_echoes.ecology.RealmChimeLookup;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.worldevent.WorldEventManager;
import cn.kurt6.unknown_echoes.worldevent.WorldEventType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 三域主动生态观测工具。玩家需要先激活栖鸣台，再在同一生境内观察植物与既有生灵。 */
public final class RealmFieldLensItem extends Item {
    public static final int CHIME_RADIUS = 12;
    public static final double RESIDENT_RADIUS = 10.0D;
    private static final double RESIDENT_RADIUS_SQR = RESIDENT_RADIUS * RESIDENT_RADIUS;
    public static final int OBSERVATION_COOLDOWN_TICKS = 200;
    public static final int RESIDENT_RECOVERY_TICKS = 1200;
    public static final int TRACE_RADIUS = 20;
    public static final int TRACE_VERTICAL_RADIUS = 6;
    public static final int TRACE_COOLDOWN_TICKS = 40;
    public static final int EVENT_TRACE_RADIUS = 64;
    public static final int EVENT_TRACE_COOLDOWN_TICKS = 40;

    private static final int CHIME_VERTICAL_RADIUS = 6;
    private static final int FAILURE_COOLDOWN_TICKS = 20;
    private static final String RESIDENT_READY_AT_KEY = "ue_field_lens_ready_at";
    private static final TagKey<Block> WAYFARER_FLORA = floraTag(
            "realm_field_lens_wayfarer_flora");
    private static final TagKey<Block> CONFLUENCE_FLORA = floraTag(
            "realm_field_lens_confluence_flora");
    private static final TagKey<Block> INTERLAYER_FLORA = floraTag(
            "realm_field_lens_interlayer_flora");
    private static final TagKey<Block> WAYFARER_FORAGE = floraTag(
            "realm_field_lens_wayfarer_forage");
    private static final TagKey<Block> CONFLUENCE_FORAGE = floraTag(
            "realm_field_lens_confluence_forage");
    private static final TagKey<Block> INTERLAYER_FORAGE = floraTag(
            "realm_field_lens_interlayer_forage");

    public RealmFieldLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.ECHO_METAL_INGOT.get())
                || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (serverPlayer.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.resting"), true);
            return InteractionResultHolder.consume(stack);
        }

        WorldEventManager.WorldEventSignal travelSignal = WorldEventManager.findNearestTravelEvent(
                serverLevel, serverPlayer.blockPosition(), EVENT_TRACE_RADIUS);
        WorldEventManager.WorldEventSignal ambientSignal = WorldEventManager.findNearestAmbientEvent(
                serverLevel, serverPlayer.blockPosition(), EVENT_TRACE_RADIUS);
        WorldEventManager.WorldEventSignal signal = nearestSignal(serverPlayer.blockPosition(),
                travelSignal, ambientSignal);
        if (signal == null) {
            serverPlayer.getCooldowns().addCooldown(this, 20);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.event_trace.none"), true);
            PlayerSoundFeedback.deny(serverPlayer);
            return InteractionResultHolder.consume(stack);
        }

        serverPlayer.getCooldowns().addCooldown(this, EVENT_TRACE_COOLDOWN_TICKS);
        stack.hurtAndBreak(1, serverPlayer, serverPlayer.getSlotForHand(hand));
        presentEventTrace(serverLevel, serverPlayer, signal);
        serverPlayer.displayClientMessage(Component.translatable(
                "message.unknown_echoes.realm_field_lens.event_trace."
                        + signal.type().getId()), true);
        serverPlayer.swing(hand, true);
        // 耐久或声音回调可能替换手持物；返回旧空栈会让原版收尾清掉新物品。
        return InteractionResultHolder.consume(serverPlayer.getItemInHand(hand));
    }

    private static WorldEventManager.WorldEventSignal nearestSignal(
            BlockPos origin,
            WorldEventManager.WorldEventSignal left,
            WorldEventManager.WorldEventSignal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.center().distSqr(origin) <= right.center().distSqr(origin)
                ? left : right;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level rawLevel = context.getLevel();
        BlockPos floraPos = context.getClickedPos();
        RealmHabitatChimeBlock.Mode mode = RealmHabitatChimeBlock.modeForDimension(rawLevel);
        if (mode == null || !rawLevel.getBlockState(floraPos).is(floraTag(mode))) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm_field_lens.invalid_flora"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (rawLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(rawLevel instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)
                || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.resting"), true);
            return InteractionResult.SUCCESS;
        }

        BlockPos chimePos = findActiveChime(level, floraPos, mode);
        if (chimePos == null) {
            player.getCooldowns().addCooldown(this, FAILURE_COOLDOWN_TICKS);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.chime_required"), true);
            return InteractionResult.SUCCESS;
        }
        ResidentSelection selection = findResident(level, floraPos, mode);
        PathfinderMob resident = selection.resident();
        if (resident == null) {
            player.getCooldowns().addCooldown(this, FAILURE_COOLDOWN_TICKS);
            player.displayClientMessage(Component.translatable(
                    selection.hasEligibleResident()
                            ? "message.unknown_echoes.realm_field_lens.resident_recovering"
                            : "message.unknown_echoes.realm_field_lens.resident_required"), true);
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        ItemStack reward = rewardFor(mode, resident);
        player.getCooldowns().addCooldown(this, OBSERVATION_COOLDOWN_TICKS);
        resident.getPersistentData().putLong(RESIDENT_READY_AT_KEY,
                safeDeadline(level.getGameTime(), RESIDENT_RECOVERY_TICKS));
        stack.hurtAndBreak(1, player, player.getSlotForHand(context.getHand()));
        give(player, reward);
        presentObservation(level, floraPos, chimePos, resident);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (!(target instanceof PathfinderMob resident)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm_field_lens.invalid_resident"), true);
            }
            return InteractionResult.SUCCESS;
        }
        RealmHabitatChimeBlock.Mode mode = RealmHabitatChimeBlock.modeForDimension(player.level());
        if (mode == null || !isEligibleResident(resident, mode)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.unknown_echoes.realm_field_lens.invalid_resident"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel level)
                || !(player instanceof ServerPlayer serverPlayer)
                || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.resting"), true);
            return InteractionResult.SUCCESS;
        }

        BlockPos forageTarget = findNearestForageTarget(level, resident.blockPosition(), mode);
        if (forageTarget == null) {
            serverPlayer.getCooldowns().addCooldown(this, 20);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.no_forage"), true);
            return InteractionResult.SUCCESS;
        }
        resident.getLookControl().setLookAt(forageTarget.getX() + 0.5D,
                forageTarget.getY() + 0.5D, forageTarget.getZ() + 0.5D);
        if (!resident.getNavigation().moveTo(forageTarget.getX() + 0.5D,
                forageTarget.getY(), forageTarget.getZ() + 0.5D, trailSpeed(mode))) {
            serverPlayer.getCooldowns().addCooldown(this, 20);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.realm_field_lens.path_blocked"), true);
            return InteractionResult.SUCCESS;
        }

        if (!isCurrentTrailContext(level, serverPlayer, resident, forageTarget, mode)) {
            return InteractionResult.SUCCESS;
        }
        serverPlayer.getCooldowns().addCooldown(this, TRACE_COOLDOWN_TICKS);
        stack.hurtAndBreak(1, serverPlayer, serverPlayer.getSlotForHand(hand));
        // 耐久成就或损坏回调可能已移除生灵，不能重新登记离开世界的实体。
        if (!isCurrentTrailContext(level, serverPlayer, resident, forageTarget, mode)) {
            return InteractionResult.SUCCESS;
        }
        RealmLivingTrailManager.startTrail(level, serverPlayer, resident, forageTarget, mode);
        presentLivingTrail(level, resident, forageTarget, mode);
        return InteractionResult.SUCCESS;
    }

    private static boolean isEligibleResident(PathfinderMob resident,
                                              RealmHabitatChimeBlock.Mode mode) {
        return resident.isAlive() && !resident.isRemoved() && resident.getTarget() == null
                && !resident.isLeashed() && !resident.isPassenger() && !resident.isVehicle()
                && RealmHabitatChimeBlock.matchesMode(resident, mode)
                && (mode != RealmHabitatChimeBlock.Mode.WAYFARER
                || resident instanceof Animal);
    }

    private static boolean isCurrentTrailContext(ServerLevel level, ServerPlayer player,
                                                  PathfinderMob resident, BlockPos forageTarget,
                                                  RealmHabitatChimeBlock.Mode mode) {
        return player.level() == level && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && resident.level() == level && isEligibleResident(resident, mode)
                && RealmHabitatChimeBlock.modeForDimension(level) == mode
                && level.hasChunkAt(forageTarget)
                && isForageTarget(level.getBlockState(forageTarget), mode);
    }

    private static BlockPos findNearestForageTarget(ServerLevel level, BlockPos origin,
                                                    RealmHabitatChimeBlock.Mode mode) {
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - TRACE_VERTICAL_RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + TRACE_VERTICAL_RADIUS);
        if (minY > maxY) return null;
        int minChunkX = (origin.getX() - TRACE_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + TRACE_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - TRACE_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + TRACE_RADIUS) >> 4;
        int minSectionY = minY >> 4;
        int maxSectionY = maxY >> 4;
        int chunkCountZ = maxChunkZ - minChunkZ + 1;
        int sectionCount = maxSectionY - minSectionY + 1;
        LevelChunkSection[] candidates = new LevelChunkSection[
                (maxChunkX - minChunkX + 1) * chunkCountZ * sectionCount];
        TagKey<Block> tag = forageTag(mode);
        java.util.function.Predicate<BlockState> matches = state -> state.is(tag);
        boolean hasCandidates = false;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    var section = chunk.getSection(chunk.getSectionIndex(sectionY << 4));
                    if (!section.maybeHas(matches)) continue;
                    int index = ((chunkX - minChunkX) * chunkCountZ + chunkZ - minChunkZ)
                            * sectionCount + sectionY - minSectionY;
                    candidates[index] = section;
                    hasCandidates = true;
                }
            }
        }
        if (!hasCandidates) return null;

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos cursor : BlockPos.withinManhattan(origin,
                TRACE_RADIUS, TRACE_VERTICAL_RADIUS, TRACE_RADIUS)) {
            if (nearest != null) {
                int depth = Math.abs(cursor.getX() - origin.getX())
                        + Math.abs(cursor.getY() - origin.getY()) + Math.abs(cursor.getZ() - origin.getZ());
                // 后续曼哈顿层的欧氏距离平方至少为 depth² / 3，同距仍保留原版先后顺序。
                if (depth * depth > 3.0D * nearestDistance) break;
            }
            int y = cursor.getY();
            if (y < minY || y > maxY) continue;
            double distance = cursor.distSqr(origin);
            if (distance < 16.0D || distance >= nearestDistance) continue;
            int index = (((cursor.getX() >> 4) - minChunkX) * chunkCountZ
                    + (cursor.getZ() >> 4) - minChunkZ) * sectionCount + (y >> 4) - minSectionY;
            LevelChunkSection section = candidates[index];
            if (section == null
                    || !section.getBlockState(cursor.getX() & 15, y & 15, cursor.getZ() & 15).is(tag)) continue;
            nearestDistance = distance;
            nearest = cursor.immutable();
        }
        return nearest;
    }

    public static double trailSpeed(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> 1.05D;
            case CONFLUENCE -> 0.95D;
            case INTERLAYER -> 0.8D;
        };
    }

    private static void presentLivingTrail(ServerLevel level, PathfinderMob resident,
                                           BlockPos target, RealmHabitatChimeBlock.Mode mode) {
        Vec3 start = resident.position().add(0.0D, resident.getBbHeight() * 0.65D, 0.0D);
        Vec3 destination = Vec3.atCenterOf(target).add(0.0D, 0.45D, 0.0D);
        Vec3 delta = destination.subtract(start);
        double length = Math.min(delta.length(), 14.0D);
        if (length > 0.1D) {
            Vec3 direction = delta.normalize();
            for (double distance = 0.0D; distance <= length; distance += 0.8D) {
                Vec3 point = start.add(direction.scale(distance));
                level.sendParticles(trailParticle(mode), point.x, point.y, point.z,
                        1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                destination.x, destination.y, destination.z,
                8, 0.25D, 0.2D, 0.25D, 0.015D);
        level.playSound(null, resident.blockPosition(), ModSounds.ITEM_FIELD_LENS_INSPECT.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.45F, mode == RealmHabitatChimeBlock.Mode.INTERLAYER
                        ? 0.7F : 1.45F);
    }

    private static net.minecraft.core.particles.ParticleOptions trailParticle(
            RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> ParticleTypes.WAX_ON;
            case CONFLUENCE -> ParticleTypes.GLOW;
            case INTERLAYER -> ParticleTypes.SCULK_SOUL;
        };
    }

    private static void presentEventTrace(ServerLevel level, ServerPlayer player,
                                          WorldEventManager.WorldEventSignal signal) {
        Vec3 start = player.getEyePosition().add(0.0D, -0.2D, 0.0D);
        Vec3 destination = Vec3.atCenterOf(signal.center()).add(0.0D, 0.6D, 0.0D);
        Vec3 delta = destination.subtract(start);
        double length = Math.min(delta.length(), 14.0D);
        if (length > 0.1D) {
            Vec3 direction = delta.normalize();
            for (double distance = 0.8D; distance <= length; distance += 0.8D) {
                Vec3 point = start.add(direction.scale(distance));
                level.sendParticles(eventTraceParticle(signal.type()),
                        point.x, point.y, point.z,
                        1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }
        level.playSound(null, player.blockPosition(), eventTraceSound(signal.type()),
                SoundSource.PLAYERS, 0.7F, eventTracePitch(signal.type()));
    }

    private static net.minecraft.core.particles.ParticleOptions eventTraceParticle(
            WorldEventType type) {
        return switch (type) {
            case FAR_BELL_PROCESSION -> ParticleTypes.WAX_ON;
            case MEMORY_FIREFLY_TIDE -> ParticleTypes.GLOW;
            case INTERLAYER_RESPITE -> ParticleTypes.SCULK_SOUL;
            case METEOR_AFTERGLOW -> ParticleTypes.END_ROD;
            case ECHO_STORM -> ParticleTypes.ELECTRIC_SPARK;
            case STRATA_AFTERSHOCK -> ParticleTypes.WHITE_ASH;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static net.minecraft.sounds.SoundEvent eventTraceSound(WorldEventType type) {
        return switch (type) {
            case FAR_BELL_PROCESSION -> SoundEvents.BELL_BLOCK;
            case MEMORY_FIREFLY_TIDE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case INTERLAYER_RESPITE -> SoundEvents.RESPAWN_ANCHOR_AMBIENT;
            case METEOR_AFTERGLOW -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case ECHO_STORM -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
            case STRATA_AFTERSHOCK -> SoundEvents.SCULK_CLICKING;
            default -> SoundEvents.SPYGLASS_USE;
        };
    }

    private static float eventTracePitch(WorldEventType type) {
        return switch (type) {
            case FAR_BELL_PROCESSION -> 1.25F;
            case MEMORY_FIREFLY_TIDE -> 1.55F;
            case INTERLAYER_RESPITE -> 0.72F;
            case METEOR_AFTERGLOW -> 1.7F;
            case ECHO_STORM -> 0.85F;
            case STRATA_AFTERSHOCK -> 0.55F;
            default -> 1.0F;
        };
    }

    private static BlockPos findActiveChime(ServerLevel level, BlockPos origin,
                                            RealmHabitatChimeBlock.Mode mode) {
        return RealmChimeLookup.findActive(level, origin, mode, CHIME_RADIUS, CHIME_VERTICAL_RADIUS);
    }

    private static ResidentSelection findResident(ServerLevel level, BlockPos floraPos,
                                                  RealmHabitatChimeBlock.Mode mode) {
        Vec3 center = Vec3.atCenterOf(floraPos);
        List<PathfinderMob> residents = level.getEntitiesOfClass(PathfinderMob.class,
                new AABB(floraPos).inflate(RESIDENT_RADIUS),
                mob -> isEligibleResident(mob, mode)
                        && mob.distanceToSqr(center) <= RESIDENT_RADIUS_SQR);
        PathfinderMob nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        long now = level.getGameTime();
        for (PathfinderMob resident : residents) {
            double distance = resident.distanceToSqr(center);
            if (distance >= nearestDistance || !isResidentReady(resident, now)) {
                continue;
            }
            nearest = resident;
            nearestDistance = distance;
        }
        return new ResidentSelection(nearest, !residents.isEmpty());
    }

    private record ResidentSelection(PathfinderMob resident, boolean hasEligibleResident) {
    }

    private static boolean isResidentReady(PathfinderMob resident, long now) {
        long readyAt = resident.getPersistentData().getLong(RESIDENT_READY_AT_KEY);
        long maximumReadyAt = safeDeadline(now, RESIDENT_RECOVERY_TICKS);
        if (readyAt > maximumReadyAt) {
            resident.getPersistentData().putLong(RESIDENT_READY_AT_KEY, maximumReadyAt);
            readyAt = maximumReadyAt;
        }
        return readyAt <= now;
    }

    private static long safeDeadline(long now, int duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    private static ItemStack rewardFor(RealmHabitatChimeBlock.Mode mode,
                                       PathfinderMob resident) {
        return switch (mode) {
            case WAYFARER -> new ItemStack(ModItems.ECHO_DUST.get(), 2);
            case CONFLUENCE -> {
                if (resident.getType() == ModEntities.CRYSTAL_CHIME_SPIRIT.get()
                        || resident.getType() == ModEntities.CRYSTAL_FEATHER_BIRD.get()) {
                    yield new ItemStack(ModItems.CRYSTAL_SONG_SHARD.get(), 2);
                }
                if (resident.getType() == ModEntities.GLOW_RABBIT.get()) {
                    yield new ItemStack(ModItems.GLOW_HIDE.get(), 2);
                }
                if (resident.getType() == ModEntities.MOSS_BACK_TURTLE.get()) {
                    yield new ItemStack(ModItems.SILENCE_MOSS.get(), 2);
                }
                if (resident.getType() == ModEntities.WIND_SAC_MOTH.get()) {
                    yield new ItemStack(ModItems.WIND_SAC_MEMBRANE.get(), 2);
                }
                if (resident.getType() == ModEntities.BROKEN_BELL_CROW.get()) {
                    yield new ItemStack(ModItems.BROKEN_BELL_GEAR.get(), 2);
                }
                yield new ItemStack(ModItems.ECHO_FIBER.get(), 2);
            }
            case INTERLAYER -> {
                if (resident.getType() == ModEntities.FOLDPAGE_BUG.get()) {
                    yield new ItemStack(ModItems.PALIMPSEST_LEAF.get());
                }
                if (resident.getType() == ModEntities.ASHBACK_GRAZER.get()) {
                    yield new ItemStack(ModItems.ASHBACK_MOSS_MEAT.get());
                }
                if (resident.getType() == ModEntities.ASHVEIL_STALKER.get()) {
                    yield new ItemStack(ModItems.DEEP_ORE_SCRAP.get());
                }
                if (resident.getType() == ModEntities.FOLDED_MANTIS.get()) {
                    yield new ItemStack(ModItems.FOLDED_CARAPACE.get());
                }
                if (resident.getType() == ModEntities.GRAVITY_RAM.get()) {
                    yield new ItemStack(ModItems.SPACE_ANCHOR_SHARD.get());
                }
                if (resident.getType() == ModEntities.HUSHLIGHT_JELLY.get()) {
                    yield new ItemStack(ModItems.HUSHLIGHT_GEL.get());
                }
                if (resident.getType() == ModEntities.RIFT_GLIDER.get()) {
                    yield new ItemStack(ModItems.SPACE_ANCHOR_SHARD.get());
                }
                yield new ItemStack(ModItems.SPACE_ANCHOR_SHARD.get());
            }
        };
    }

    private static void presentObservation(ServerLevel level, BlockPos floraPos,
                                           BlockPos chimePos, PathfinderMob resident) {
        Vec3 flora = Vec3.atCenterOf(floraPos).add(0.0D, 0.45D, 0.0D);
        Vec3 chime = Vec3.atCenterOf(chimePos).add(0.0D, 0.55D, 0.0D);
        Vec3 creature = resident.position().add(0.0D, resident.getBbHeight() * 0.6D, 0.0D);
        sendTrace(level, flora, chime, ParticleTypes.WAX_ON);
        sendTrace(level, creature, flora, ParticleTypes.GLOW);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                flora.x, flora.y, flora.z, 14, 0.35D, 0.25D, 0.35D, 0.02D);
        level.playSound(null, floraPos, ModSounds.ITEM_FIELD_LENS_INSPECT.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        level.playSound(null, chimePos, ModSounds.ITEM_FIELD_LENS_CONFIRM.get(),
                SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    private static void sendTrace(ServerLevel level, Vec3 from, Vec3 to,
                                  net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 delta = to.subtract(from);
        for (int step = 0; step <= 10; step++) {
            Vec3 point = from.add(delta.scale(step / 10.0D));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private static TagKey<Block> floraTag(String id) {
        return TagKey.create(Registries.BLOCK, UnknownEchoes.id(id));
    }

    private static TagKey<Block> floraTag(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> WAYFARER_FLORA;
            case CONFLUENCE -> CONFLUENCE_FLORA;
            case INTERLAYER -> INTERLAYER_FLORA;
        };
    }

    private static TagKey<Block> forageTag(RealmHabitatChimeBlock.Mode mode) {
        return switch (mode) {
            case WAYFARER -> WAYFARER_FORAGE;
            case CONFLUENCE -> CONFLUENCE_FORAGE;
            case INTERLAYER -> INTERLAYER_FORAGE;
        };
    }

    public static boolean isForageTarget(BlockState state,
                                         RealmHabitatChimeBlock.Mode mode) {
        return state.is(forageTag(mode));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.realm_field_lens.observe")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.realm_field_lens.trail")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.realm_field_lens.anomaly")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.realm_field_lens.reward")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.realm_field_lens.cooldown")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
