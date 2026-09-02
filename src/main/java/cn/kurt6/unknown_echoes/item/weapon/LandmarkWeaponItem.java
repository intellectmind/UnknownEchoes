package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.item.tool.DeepAlloyTier;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** 两座深层大型地标的专属双战技武器。全部命中、耗能和冷却均由服务端结算。 */
public final class LandmarkWeaponItem extends EchoMeleeWeaponItem implements WeaponSkill {
    private static final String CODEX_MARKS_KEY = "ue_codex_marks";
    private static final String CODEX_MARK_OWNER = "ue_codex_mark_owner";
    private static final String CODEX_MARK_UNTIL = "ue_codex_mark_until";
    private static final int MAX_ACTIVE_CODEX_MARKS = 64;

    private final Kind kind;
    private final String tooltipKey;

    public LandmarkWeaponItem(Kind kind, double damage, double speed,
                              String tooltipKey, Properties properties) {
        super(DeepAlloyTier.INSTANCE, damage, speed, tooltipKey, properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        boolean matchesThemedMaterial = switch (kind) {
            case PALIMPSEST_SABER -> repairCandidate.is(ModItems.PALIMPSEST_LEAF.get());
            case ORRERY_LANCE -> repairCandidate.is(ModItems.ORRERY_PRISM_CORE.get());
        };
        return matchesThemedMaterial || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean activateSkill(ServerLevel level, ServerPlayer player,
                                 ItemStack stack, InteractionHand hand) {
        return activate(level, player, hand, false);
    }

    @Override
    public boolean hasSecondarySkill() {
        return true;
    }

    @Override
    public boolean activateSecondarySkill(ServerLevel level, ServerPlayer player,
                                          ItemStack stack, InteractionHand hand) {
        return activate(level, player, hand, true);
    }

    private boolean activate(ServerLevel level, ServerPlayer player,
                             InteractionHand hand, boolean secondary) {
        int energyCost = energyCost(secondary);
        int cooldown = cooldownTicks(secondary);
        String cooldownKey = "ue_" + tooltipKey + (secondary ? "_secondary_until" : "_primary_until");
        long now = player.server.overworld().getGameTime();
        long readyAt = WeaponSkillSupport.normalizeCooldownDeadline(
                player, cooldownKey, now, cooldown);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                    "message.unknown_echoes.landmark_gear.cooldown", seconds), true);
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, energyCost)) {
            return false;
        }

        player.getPersistentData().putLong(cooldownKey,
                WeaponSkillSupport.safeDeadline(now, cooldown));
        if (kind == Kind.PALIMPSEST_SABER) {
            if (secondary) {
                replayAnnotations(level, player, now);
            } else {
                inscribeTargets(level, player, now);
            }
        } else if (secondary) {
            suspendOrbit(level, player);
        } else {
            pierceOrbit(level, player);
        }
        player.swing(hand, true);
        return true;
    }

    private int energyCost(boolean secondary) {
        if (kind == Kind.PALIMPSEST_SABER) {
            return secondary ? 22 : 10;
        }
        return secondary ? 26 : 12;
    }

    private int cooldownTicks(boolean secondary) {
        if (kind == Kind.PALIMPSEST_SABER) {
            return secondary ? 140 : 40;
        }
        return secondary ? 170 : 50;
    }

    private void inscribeTargets(ServerLevel level, ServerPlayer player, long now) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.PALIMPSEST_SABER;
        List<LivingEntity> targets = coneTargets(level, player, 12.0D, 0.45D, 6);
        for (LivingEntity target : targets) {
            if (!hurtWithSkill(player, target, 15.0F)) {
                continue;
            }
            addAnnotationMark(target, player, WeaponSkillSupport.safeDeadline(now, 200L), now);
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        60, 0, true, false, true));
            }
            WeaponFx.hitMark(level, target, fx);
        }
        WeaponFx.slashArc(level, player, fx, 12.0D, WeaponFx.halfAngleOf(0.45D));
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.9F, 1.35F);
    }

    private void replayAnnotations(ServerLevel level, ServerPlayer player, long now) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(9.0D),
                        target -> isHostileTarget(player, target)
                                && player.distanceToSqr(target) <= 81.0D
                                && player.hasLineOfSight(target))
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(10)
                .toList();
        for (LivingEntity target : targets) {
            boolean marked = hasAnnotationMark(target, player, now);
            if (!hurtWithSkill(player, target, marked ? 28.0F : 10.0F)) {
                continue;
            }
            if (marked) {
                clearAnnotationMark(target, player);
            }
            // 标记目标伤害翻近三倍,必须用重击标记区分:同一种标记玩家读不出差别。
            if (marked) {
                WeaponFx.heavyHitMark(level, target, WeaponFxTheme.PALIMPSEST_SABER);
                level.sendParticles(ParticleTypes.FLASH,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            } else {
                WeaponFx.hitMark(level, target, WeaponFxTheme.PALIMPSEST_SABER);
            }
        }
        WeaponFx.groundRing(level, player, WeaponFxTheme.PALIMPSEST_SABER, 9.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.2F, 0.7F);
    }

    private static void addAnnotationMark(LivingEntity target, ServerPlayer player,
                                          long expiresAt, long now) {
        CompoundTag data = target.getPersistentData();
        CompoundTag marks = data.getCompound(CODEX_MARKS_KEY);
        migrateLegacyMark(data, marks, now);
        pruneExpiredMarks(marks, now);
        String playerId = player.getStringUUID();
        pruneOldestMarks(marks, playerId,
                marks.contains(playerId, Tag.TAG_LONG)
                        ? MAX_ACTIVE_CODEX_MARKS : MAX_ACTIVE_CODEX_MARKS - 1);
        marks.putLong(playerId, expiresAt);
        data.put(CODEX_MARKS_KEY, marks);
    }

    private static boolean hasAnnotationMark(LivingEntity target, ServerPlayer player, long now) {
        CompoundTag data = target.getPersistentData();
        CompoundTag marks = data.getCompound(CODEX_MARKS_KEY);
        migrateLegacyMark(data, marks, now);
        pruneExpiredMarks(marks, now);
        if (marks.isEmpty()) {
            data.remove(CODEX_MARKS_KEY);
            return false;
        }
        data.put(CODEX_MARKS_KEY, marks);
        String playerId = player.getStringUUID();
        return marks.contains(playerId, Tag.TAG_LONG) && marks.getLong(playerId) >= now;
    }

    private static void clearAnnotationMark(LivingEntity target, ServerPlayer player) {
        CompoundTag data = target.getPersistentData();
        CompoundTag marks = data.getCompound(CODEX_MARKS_KEY);
        marks.remove(player.getStringUUID());
        if (marks.isEmpty()) {
            data.remove(CODEX_MARKS_KEY);
        } else {
            data.put(CODEX_MARKS_KEY, marks);
        }
    }

    private static void migrateLegacyMark(CompoundTag data, CompoundTag marks, long now) {
        if (data.contains(CODEX_MARK_UNTIL, Tag.TAG_LONG)) {
            String owner = data.getString(CODEX_MARK_OWNER);
            long expiresAt = data.getLong(CODEX_MARK_UNTIL);
            if (!owner.isEmpty() && expiresAt >= now) {
                marks.putLong(owner, expiresAt);
            }
        }
        data.remove(CODEX_MARK_OWNER);
        data.remove(CODEX_MARK_UNTIL);
    }

    private static void pruneExpiredMarks(CompoundTag marks, long now) {
        for (String playerId : List.copyOf(marks.getAllKeys())) {
            if (!marks.contains(playerId, Tag.TAG_LONG) || marks.getLong(playerId) < now) {
                marks.remove(playerId);
            }
        }
    }

    private static void pruneOldestMarks(CompoundTag marks, String protectedPlayerId,
                                         int maxCount) {
        int excess = marks.getAllKeys().size() - maxCount;
        if (excess <= 0) {
            return;
        }
        List<String> oldestFirst = marks.getAllKeys().stream()
                .sorted(Comparator.comparingLong(marks::getLong))
                .toList();
        for (String playerId : oldestFirst) {
            if (excess <= 0) {
                break;
            }
            if (!playerId.equals(protectedPlayerId)) {
                marks.remove(playerId);
                excess--;
            }
        }
    }

    private void pierceOrbit(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.ORRERY_LANCE;
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double reach = clippedReach(level, player, eye, direction, 18.0D);
        for (LivingEntity target : lineTargets(level, player, eye, direction, reach, 1.1D, 6)) {
            if (hurtWithSkill(player, target, 17.0F)) {
                WeaponFx.hitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                            80, 0, true, false, true));
                }
            }
        }
        WeaponFx.pierceBeam(level, player, fx, eye, direction, reach);
        player.setDeltaMovement(direction.x * 1.15D,
                Math.max(0.08D, direction.y * 0.85D + 0.08D),
                direction.z * 1.15D);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS, 1.0F, 1.15F);
    }

    private void suspendOrbit(ServerLevel level, ServerPlayer player) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(9.0D),
                        target -> isHostileTarget(player, target)
                                && player.distanceToSqr(target) <= 81.0D
                                && player.hasLineOfSight(target))
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(10)
                .toList();
        for (LivingEntity target : targets) {
            if (!hurtWithSkill(player, target, 22.0F)) {
                continue;
            }
            if (!isBoss(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                        35, 0, true, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        80, 1, true, false, true));
            }
            // 目标被抬离地面,标记要向上延伸而不是只画腰环,否则读不出"被吊起来"。
            WeaponFx.hitMark(level, target, WeaponFxTheme.ORRERY_LANCE);
            WeaponFx.dashTrail(level, target.position(), new Vec3(0.0D, 1.0D, 0.0D),
                    target.getBbHeight() + 1.2D, WeaponFxTheme.ORRERY_LANCE);
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                100, 0, true, false, true));
        WeaponFx.groundRing(level, player, WeaponFxTheme.ORRERY_LANCE, 9.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.15F, 0.75F);
    }

    private static List<LivingEntity> coneTargets(ServerLevel level, ServerPlayer player,
                                                   double range, double minDot, int limit) {
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(range),
                        target -> isHostileTarget(player, target))
                .stream()
                .filter(target -> {
                    Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
                    return offset.lengthSqr() <= range * range
                            && offset.normalize().dot(look) >= minDot
                            && player.hasLineOfSight(target);
                })
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(limit)
                .toList();
    }

    private static double clippedReach(ServerLevel level, ServerPlayer player,
                                       Vec3 origin, Vec3 direction, double range) {
        BlockHitResult block = level.clip(new ClipContext(origin, origin.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return block.getType() == HitResult.Type.MISS
                ? range
                : origin.distanceTo(block.getLocation());
    }

    /** 薄封装:命中几何归 WeaponSkillSupport.lineTargets 持有,不要在这里复制一份。 */
    private static List<LivingEntity> lineTargets(ServerLevel level, ServerPlayer player,
                                                  Vec3 origin, Vec3 direction, double reach,
                                                  double width, int limit) {
        return WeaponSkillSupport.lineTargets(level, player, reach, width, limit);
    }


    private static boolean isHostileTarget(ServerPlayer player, LivingEntity target) {
        return WeaponSkillSupport.isHostile(player, target);
    }


    private static boolean hurtWithSkill(ServerPlayer player, LivingEntity target, float damage) {
        return WeaponSkillSupport.hurt(player, target, damage);
    }


    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".primary",
                        Component.keybind("key.unknown_echoes.weapon_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".secondary",
                        Component.keybind("key.unknown_echoes.weapon_secondary_skill"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".detail")
                .withStyle(ChatFormatting.GRAY));
    }

    public enum Kind {
        PALIMPSEST_SABER,
        ORRERY_LANCE
    }
}
