package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.artifact.ArtifactManager;
import cn.kurt6.unknown_echoes.item.tool.DeepAlloyTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/** 世界回响体专属双战技武器，全部目标、能量和冷却判定均在服务端执行。 */
public class WorldEchoWeaponItem extends EchoMeleeWeaponItem implements WeaponSkill {
    private static final int GREAT_PRIMARY_COST = 18;
    private static final int GREAT_SECONDARY_COST = 36;
    private static final int SPEAR_PRIMARY_COST = 16;
    private static final int SPEAR_SECONDARY_COST = 40;
    private static final int GREAT_PRIMARY_COOLDOWN = 80;
    private static final int GREAT_SECONDARY_COOLDOWN = 240;
    private static final int SPEAR_PRIMARY_COOLDOWN = 60;
    private static final int SPEAR_SECONDARY_COOLDOWN = 280;

    private final Kind kind;
    private final String tooltipKey;

    public WorldEchoWeaponItem(Kind kind, double damage, double speed,
                               String tooltipKey, Properties properties) {
        super(DeepAlloyTier.INSTANCE, damage, speed, tooltipKey, properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
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
                    "message.unknown_echoes.world_echo_weapon.cooldown", seconds), true);
            return false;
        }
        if (!ArtifactManager.spendEnergy(player, energyCost)) {
            return false;
        }
        player.getPersistentData().putLong(cooldownKey,
                WeaponSkillSupport.safeDeadline(now, cooldown));
        if (kind == Kind.WORLD_RIFT_GREATSWORD) {
            if (secondary) {
                worldCollapse(level, player);
            } else {
                worldRend(level, player);
            }
        } else if (secondary) {
            fivefoldJudgment(level, player);
        } else {
            starPierce(level, player);
        }
        player.swing(hand, true);
        return true;
    }

    private int energyCost(boolean secondary) {
        if (kind == Kind.WORLD_RIFT_GREATSWORD) {
            return secondary ? GREAT_SECONDARY_COST : GREAT_PRIMARY_COST;
        }
        return secondary ? SPEAR_SECONDARY_COST : SPEAR_PRIMARY_COST;
    }

    private int cooldownTicks(boolean secondary) {
        if (kind == Kind.WORLD_RIFT_GREATSWORD) {
            return secondary ? GREAT_SECONDARY_COOLDOWN : GREAT_PRIMARY_COOLDOWN;
        }
        return secondary ? SPEAR_SECONDARY_COOLDOWN : SPEAR_PRIMARY_COOLDOWN;
    }

    private void worldRend(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.WORLD_RIFT_GREATSWORD;
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double reach = clippedReach(level, player, eye, direction, 18.0D);
        List<LivingEntity> targets = lineTargets(level, player, eye, direction, reach, 1.5D, 8);
        for (LivingEntity target : targets) {
            boolean hurt = hurtWithSkill(player, target, 20.0F);
            if (hurt) {
                WeaponFx.hitMark(level, target, fx);
                if (!isBoss(target)) {
                    target.knockback(0.45D, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
            }
        }
        WeaponFx.pierceBeam(level, player, fx, eye, direction, reach);
        Vec3 impact = eye.add(direction.scale(reach));
        WeaponFx.releaseBurst(level, impact, fx, 1.2D);
        // 末端可能超出可听距离；施法者只收近身反馈，旁观者仍听末端定位声。
        level.playSound(player, impact.x, impact.y, impact.z,
                ModSounds.WEAPON_WORLD_RIFT_GREATSWORD_PRIMARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.playNotifySound(ModSounds.WEAPON_WORLD_RIFT_GREATSWORD_PRIMARY.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void worldCollapse(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.WORLD_RIFT_GREATSWORD;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(9.0D),
                        target -> isHostileTarget(player, target)
                                && player.distanceToSqr(target) <= 81.0D)
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(12)
                .toList();
        for (LivingEntity target : targets) {
            boolean hurt = hurtWithSkill(player, target, 28.0F);
            if (!hurt) {
                continue;
            }
            WeaponFx.pullTether(level, player, target, fx);
            if (!isBoss(target)) {
                Vec3 pull = player.position().add(0.0D, 0.8D, 0.0D).subtract(target.position());
                if (pull.lengthSqr() > 1.0E-4D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.8D)));
                    target.hurtMarked = true;
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        60, 1, true, false, true));
            }
        }
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        WeaponFx.groundRing(level, player, fx, 9.0D);
        WeaponFx.releaseBurst(level, center, fx, 1.35D);
        level.playSound(null, player.blockPosition(),
                ModSounds.WEAPON_WORLD_RIFT_GREATSWORD_SECONDARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void starPierce(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.FIVEFOLD_STAR_SPEAR;
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double reach = clippedReach(level, player, eye, direction, 24.0D);
        for (LivingEntity target : lineTargets(level, player, eye, direction, reach, 1.0D, 6)) {
            if (hurtWithSkill(player, target, 18.0F)) {
                WeaponFx.hitMark(level, target, fx);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        80, 0, true, false, true));
            }
        }
        WeaponFx.pierceBeam(level, player, fx, eye, direction, reach);
        Vec3 dash = new Vec3(direction.x * 1.6D,
                Math.max(0.12D, direction.y * 1.3D + 0.12D),
                direction.z * 1.6D);
        WeaponFx.dashTrail(level, player.position().add(0.0D, 0.8D, 0.0D), dash, 2.4D, fx);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        level.playSound(null, player.blockPosition(),
                ModSounds.WEAPON_FIVEFOLD_STAR_SPEAR_PRIMARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void fivefoldJudgment(ServerLevel level, ServerPlayer player) {
        WeaponFxTheme.Theme fx = WeaponFxTheme.FIVEFOLD_STAR_SPEAR;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(20.0D),
                        target -> isHostileTarget(player, target)
                                && player.distanceToSqr(target) <= 400.0D
                                && player.hasLineOfSight(target))
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(5)
                .toList();
        for (LivingEntity target : targets) {
            if (!hurtWithSkill(player, target, 32.0F)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    120, 0, true, false, true));
            double y = target.getY() + target.getBbHeight() * 0.55D;
            // 判决是"从天落下"而不是"从玩家射出":降柱起点取目标头顶,不连回玩家。
            WeaponFx.dashTrail(level, new Vec3(target.getX(), y + 4.5D, target.getZ()),
                    new Vec3(0.0D, -1.0D, 0.0D), 4.5D, fx);
            WeaponFx.heavyHitMark(level, target, fx);
        }
        level.playSound(null, player.blockPosition(),
                ModSounds.WEAPON_FIVEFOLD_STAR_SPEAR_SECONDARY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static double clippedReach(ServerLevel level, ServerPlayer player,
                                       Vec3 origin, Vec3 direction, double range) {
        Vec3 end = origin.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(origin, end,
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
        WORLD_RIFT_GREATSWORD,
        FIVEFOLD_STAR_SPEAR
    }
}
