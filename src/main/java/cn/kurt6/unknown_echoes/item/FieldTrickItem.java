package cn.kurt6.unknown_echoes.item;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.fx.CombatFx;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class FieldTrickItem extends Item {
    public enum Kind {
        DECOY("echo_decoy", 200),
        BURST("chime_burst", 240),
        TRACE("seeker_spore", 160);

        final String id;
        final int cooldown;

        Kind(String id, int cooldown) {
            this.id = id;
            this.cooldown = cooldown;
        }
    }

    private static final TagKey<Block> TRACEABLE = TagKey.create(
            Registries.BLOCK, UnknownEchoes.id("trick_traceable_resources"));
    private static final int TRACE_RADIUS = 16;
    private static final double BURST_RADIUS = 5.0D;
    private static final int BURST_TARGETS = 8;
    private static final float BURST_DAMAGE = 6.0F;
    private final Kind kind;

    public FieldTrickItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)
                || !player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(this)
                || !stack.is(this) || serverPlayer.level() != server) {
            return InteractionResultHolder.fail(stack);
        }
        // 伤害、击退、目标切换和声音都可能触发同步回调，先预留道具与冷却。
        ItemStack payment = player.getAbilities().instabuild ? ItemStack.EMPTY : stack.split(1);
        player.getCooldowns().addCooldown(this, kind.cooldown);
        boolean applied = switch (kind) {
            case DECOY -> placeDecoy(server, serverPlayer);
            case BURST -> burst(server, serverPlayer);
            case TRACE -> trace(server, serverPlayer);
        };
        if (!applied) {
            refund(serverPlayer, hand, stack, payment);
            player.getCooldowns().removeCooldown(this);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        if (burstSource(server, serverPlayer)) {
            player.swing(hand, true);
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    private static void refund(ServerPlayer player, InteractionHand hand,
                               ItemStack original, ItemStack payment) {
        if (payment.isEmpty()) return;
        ItemStack held = player.getItemInHand(hand);
        if (held == original && held.isEmpty()) {
            player.setItemInHand(hand, payment);
        } else if (held == original && ItemStack.isSameItemSameComponents(held, payment)
                && held.getCount() + payment.getCount() <= held.getMaxStackSize()) {
            held.grow(payment.getCount());
        } else if (!player.getInventory().add(payment)) {
            player.drop(payment, false);
        }
    }

    private boolean placeDecoy(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = clipDecoy(level, player, eye, eye.add(player.getLookAngle().scale(12.0D)));
        if (!burstSource(level, player)) return false;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP) {
            return fail(player, "message.unknown_echoes.field_trick.ground");
        }
        Vec3 center = hit.getLocation().add(0, 0.1D, 0);
        if (!FieldTrickLure.apply(level, player, center)) {
            return burstSource(level, player) && fail(player, "message.unknown_echoes.field_trick.no_enemy");
        }
        if (!burstSource(level, player)) return true;
        CombatFx.emit(level, Shape.SIGIL, center, Vec3.ZERO,
                0.65D, 0, 0, 0xFBE8B1, 0xB48BCD, 24, false);
        if (!burstSource(level, player)) return true;
        level.playSound(null, center.x, center.y, center.z,
                ModSounds.ITEM_FIELD_TRICK_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static BlockHitResult clipDecoy(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        // 原版射线会向两端外推；先检查整个实际读取足印，不能等命中后再查区块。
        Vec3 low = start.lerp(end, -1.0e-7D);
        Vec3 high = end.lerp(start, -1.0e-7D);
        BlockPos from = BlockPos.containing(new Vec3(Math.min(low.x, high.x), Math.min(low.y, high.y), Math.min(low.z, high.z)));
        BlockPos to = BlockPos.containing(new Vec3(Math.max(low.x, high.x), Math.max(low.y, high.y), Math.max(low.z, high.z)));
        if (to.getY() < level.getMinBuildHeight() || from.getY() >= level.getMaxBuildHeight()) return null;
        for (int chunkX = from.getX() >> 4; chunkX <= to.getX() >> 4; chunkX++) {
            for (int chunkZ = from.getZ() >> 4; chunkZ <= to.getZ() >> 4; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return null;
            }
        }
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private boolean burst(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(BURST_RADIUS),
                target -> burstTarget(level, player, target, center)).stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center))).toList();
        int applied = 0;
        for (LivingEntity target : targets) {
            if (!burstSource(level, player)) break;
            if (!burstTarget(level, player, target, center)) continue;
            if (applied >= BURST_TARGETS) break;
            double dx = center.x - target.getX();
            double dz = center.z - target.getZ();
            if (dx * dx + dz * dz < 0.001D) {
                dx = -look.x;
                dz = -look.z;
            }
            applied++;
            target.hurt(level.damageSources().playerAttack(player), BURST_DAMAGE);
            if (!burstSource(level, player)) break;
            if (!burstTarget(level, player, target, center)) continue;
            target.knockback(1.25D, dx, dz);
            if (!burstSource(level, player)) break;
            if (!burstTarget(level, player, target, center)) continue;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        if (applied == 0) {
            return burstSource(level, player) && fail(player, "message.unknown_echoes.field_trick.no_target");
        }
        if (!burstSource(level, player)) return true;
        CombatFx.emit(level, Shape.RING, center.add(0, 0.2D, 0), Vec3.ZERO,
                BURST_RADIUS, 0, 0, 0xFFF0CB, 0xDB936E, 16, false);
        if (burstSource(level, player)) {
            level.playSound(null, BlockPos.containing(center), SoundEvents.BELL_RESONATE,
                    SoundSource.PLAYERS, 0.8F, 1.45F);
        }
        return true;
    }

    private static boolean burstSource(ServerLevel level, ServerPlayer player) {
        return player.level() == level && player.isAlive() && !player.isSpectator();
    }

    private static boolean burstTarget(ServerLevel level, ServerPlayer player, LivingEntity target, Vec3 center) {
        return target != player && target.level() == level && target.isAlive() && !target.isSpectator()
                && target.distanceToSqr(center) <= BURST_RADIUS * BURST_RADIUS;
    }

    private boolean trace(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        Vec3 start = player.getEyePosition().add(0, -0.3D, 0);
        BlockPos nearest = findTraceable(level, origin);
        if (!burstSource(level, player)) return true;
        if (nearest == null) {
            player.displayClientMessage(Component.translatable("message.unknown_echoes.field_trick.no_resource"), true);
            if (!burstSource(level, player)) return true;
            level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.7F, 1.6F);
            return true;
        }
        Vec3 destination = Vec3.atCenterOf(nearest);
        Vec3 offset = destination.subtract(start);
        CombatFx.emit(level, Shape.TRACE, start, offset.normalize(),
                0.12D, offset.length(), 0, 0xF0FFC4, 0x7FCAA2, 30, false);
        if (!burstSource(level, player)) return true;
        CombatFx.emit(level, Shape.SIGIL, destination, Vec3.ZERO,
                0.65D, 0, 0, 0xF0FFC4, 0x7FCAA2, 30, false);
        if (!burstSource(level, player)) return true;
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.7F, 1.6F);
        if (!burstSource(level, player)) return true;
        player.displayClientMessage(Component.translatable("message.unknown_echoes.field_trick.trail",
                nearest.getX(), nearest.getY(), nearest.getZ()), true);
        return true;
    }

    private static BlockPos findTraceable(ServerLevel level, BlockPos origin) {
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - TRACE_RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + TRACE_RADIUS);
        if (minY > maxY) return null;
        int minChunkX = (origin.getX() - TRACE_RADIUS) >> 4;
        int maxChunkX = (origin.getX() + TRACE_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - TRACE_RADIUS) >> 4;
        int maxChunkZ = (origin.getZ() + TRACE_RADIUS) >> 4;
        int minSectionY = minY >> 4;
        int sectionCount = (maxY >> 4) - minSectionY + 1;
        int chunkCountZ = maxChunkZ - minChunkZ + 1;
        LevelChunkSection[] sections = new LevelChunkSection[(maxChunkX - minChunkX + 1) * chunkCountZ * sectionCount];
        java.util.function.Predicate<BlockState> matches = state -> state.is(TRACEABLE);
        boolean hasCandidates = false;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (int sectionY = minSectionY; sectionY <= maxY >> 4; sectionY++) {
                    var section = chunk.getSection(chunk.getSectionIndex(sectionY << 4));
                    if (!section.maybeHas(matches)) continue;
                    int index = ((chunkX - minChunkX) * chunkCountZ + chunkZ - minChunkZ) * sectionCount + sectionY - minSectionY;
                    sections[index] = section;
                    hasCandidates = true;
                }
            }
        }
        if (!hasCandidates) return null;
        BlockPos nearest = null;
        double best = TRACE_RADIUS * TRACE_RADIUS + 1.0D;
        for (BlockPos candidate : BlockPos.betweenClosed(new BlockPos(origin.getX() - TRACE_RADIUS, minY, origin.getZ() - TRACE_RADIUS),
                new BlockPos(origin.getX() + TRACE_RADIUS, maxY, origin.getZ() + TRACE_RADIUS))) {
            double distance = candidate.distSqr(origin);
            if (distance >= best || distance > TRACE_RADIUS * TRACE_RADIUS || !level.isInWorldBounds(candidate)) continue;
            int y = candidate.getY();
            int index = (((candidate.getX() >> 4) - minChunkX) * chunkCountZ + (candidate.getZ() >> 4) - minChunkZ)
                    * sectionCount + (y >> 4) - minSectionY;
            LevelChunkSection section = sections[index];
            if (section != null && section.getBlockState(candidate.getX() & 15, y & 15, candidate.getZ() & 15).is(TRACEABLE)) {
                nearest = candidate.immutable();
                best = distance;
            }
        }
        return nearest;
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes." + kind.id + ".effect")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(kind == Kind.TRACE
                        ? "tooltip.unknown_echoes.seeker_spore.cost" : "tooltip.unknown_echoes.field_trick.cost",
                        kind.cooldown / 20)
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
