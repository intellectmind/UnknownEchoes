package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 第一维度通用近战武器实现:正式武器共享基础数值和服务端命中特效。 */
public class EchoMeleeWeaponItem extends SwordItem {
    private static final String BROADSWORD_TARGET_KEY = "ue_broadsword_target";
    private static final String BROADSWORD_COUNT_KEY = "ue_broadsword_count";
    private static final String BROADSWORD_TIME_KEY = "ue_broadsword_time";
    private static final String HAMMER_TIME_KEY = "ue_bell_hammer_time";
    /** 攻击瞬间的蓄力值:Player.attack 在 hurt 之前就重置蓄力条,事件里现读恒为 0,由 CommonEvents 预先记录。 */
    public static final String ATTACK_CHARGE_KEY = "ue_attack_charge";

    private final String tooltipKey;

    public EchoMeleeWeaponItem(double damage, double speed, String tooltipKey, Properties properties) {
        super(EchoMetalTier.INSTANCE, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".skill")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.weapon." + tooltipKey + ".lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** 返回并入本次伤害事件的追加伤害(>0 时由 CommonEvents setAmount 落账):
     *  事件期间目标 invulnerableTime/lastHurt 已置位,嵌套 hurt 会被无敌帧吞掉。 */
    public float applyDamageEventEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        return switch (tooltipKey) {
            case "echo_broadsword" -> applyBroadsword(level, player, target);
            case "echo_battleaxe" -> applyBattleaxe(player, target);
            case "silence_scythe" -> applySilenceScythe(level, target);
            case "crystal_song_staff" -> applyCrystalSongStaff(level, player, target);
            case "broken_bell_hammer" -> applyBellHammer(level, player, target);
            case "echo_oathblade" -> applyOathblade(target);
            default -> 0.0F;
        };
    }

    private float applyBroadsword(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy)) {
            return 0.0F;
        }
        var data = player.getPersistentData();
        String targetId = target.getUUID().toString();
        long now = level.getGameTime();
        long last = data.getLong(BROADSWORD_TIME_KEY);
        int previousCount = Math.max(0, Math.min(2, data.getInt(BROADSWORD_COUNT_KEY)));
        boolean continues = targetId.equals(data.getString(BROADSWORD_TARGET_KEY))
                && last >= 0L && last <= now && now - last <= 100L;
        int count = continues ? previousCount + 1 : 1;
        data.putString(BROADSWORD_TARGET_KEY, targetId);
        data.putLong(BROADSWORD_TIME_KEY, now);
        if (count < 3) {
            data.putInt(BROADSWORD_COUNT_KEY, count);
            return 0.0F;
        }
        data.putInt(BROADSWORD_COUNT_KEY, 0);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, true));
        level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 1, 0, 0, 0, 0);
        return 3.0F;
    }

    private float applyBattleaxe(ServerPlayer player, LivingEntity target) {
        if (!isStrongAttack(player) || isBossLike(target) || !(target instanceof Enemy)) {
            return 0.0F;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0, true, false, true));
        return 0.0F;
    }

    private float applySilenceScythe(ServerLevel level, LivingEntity target) {
        if (isBossLike(target) || !(target instanceof Enemy)) {
            return 0.0F;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 4, 0.25, 0.25, 0.25, 0.01);
        return 0.0F;
    }

    private float applyBellHammer(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!isStrongAttack(player) || !(target instanceof Enemy)) {
            return 0.0F;
        }
        var data = target.getPersistentData();
        String timeKey = HAMMER_TIME_KEY + "_" + player.getUUID();
        long now = level.getGameTime();
        long last = data.getLong(timeKey);
        if (data.contains(timeKey, Tag.TAG_LONG)
                && last >= 0L && last <= now && now - last <= 60L) {
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.7F);
            data.remove(timeKey);
            return isBossLike(target) ? 2.0F : 5.0F;
        }
        data.putLong(timeKey, now);
        return 0.0F;
    }

    private float applyCrystalSongStaff(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBossLike(target)) {
            return 0.0F;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
        level.sendParticles(ParticleTypes.NOTE, target.getX(), target.getY() + target.getBbHeight(),
                target.getZ(), 6, 0.4, 0.2, 0.4, 0.01);
        level.playSound(null, target.blockPosition(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.6F);
        return 0.0F;
    }

    private float applyOathblade(LivingEntity target) {
        // LivingIncomingDamageEvent 晚于 Boss 自身的阶段钳制，事件层不得再给 Boss 追加伤害。
        if (target.getType().is(ModEntityTags.ECHO_BOSSES)) {
            return 0.0F;
        }
        return target.getType().is(ModEntityTags.ECHO_REALM_HOSTILES) ? 2.0F : 0.0F;
    }

    private static boolean isBossLike(LivingEntity target) {
        return target.getType().is(ModEntityTags.ECHO_BOSSES);
    }

    private static boolean isStrongAttack(ServerPlayer player) {
        return player.getPersistentData().getFloat(ATTACK_CHARGE_KEY) >= 0.9F;
    }
}
