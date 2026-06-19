package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
import cn.kurt6.unknown_echoes.entity.boss.ForgottenColossus;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.entity.boss.MirrorGuardian;
import cn.kurt6.unknown_echoes.registry.ModEntityTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

    public void applyDamageEventEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        switch (tooltipKey) {
            case "echo_broadsword" -> applyBroadsword(level, player, target);
            case "echo_battleaxe" -> applyBattleaxe(target);
            case "silence_scythe" -> applySilenceScythe(level, target);
            case "crystal_song_staff" -> applyCrystalSongStaff(level, player, target);
            case "broken_bell_hammer" -> applyBellHammer(level, player, target);
            case "echo_oathblade" -> applyOathblade(player, target);
            default -> {
            }
        }
    }

    private void applyBroadsword(ServerLevel level, ServerPlayer player, LivingEntity target) {
        var data = player.getPersistentData();
        String targetId = target.getUUID().toString();
        long now = level.getGameTime();
        int count = targetId.equals(data.getString(BROADSWORD_TARGET_KEY))
                && now - data.getLong(BROADSWORD_TIME_KEY) <= 100 ? data.getInt(BROADSWORD_COUNT_KEY) + 1 : 1;
        data.putString(BROADSWORD_TARGET_KEY, targetId);
        data.putLong(BROADSWORD_TIME_KEY, now);
        if (count < 3) {
            data.putInt(BROADSWORD_COUNT_KEY, count);
            return;
        }
        data.putInt(BROADSWORD_COUNT_KEY, 0);
        WeaponEffectGuard.run(player,
                () -> target.hurt(player.damageSources().playerAttack(player), 3.0F));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, true));
        level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 1, 0, 0, 0, 0);
    }

    private void applyBattleaxe(LivingEntity target) {
        if (isBossLike(target) || !(target instanceof Enemy)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0, true, false, true));
    }

    private void applySilenceScythe(ServerLevel level, LivingEntity target) {
        if (isBossLike(target) || !(target instanceof Enemy)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 4, 0.25, 0.25, 0.25, 0.01);
    }

    private void applyBellHammer(ServerLevel level, ServerPlayer player, LivingEntity target) {
        var data = target.getPersistentData();
        long now = level.getGameTime();
        if (now - data.getLong(HAMMER_TIME_KEY) <= 60) {
            WeaponEffectGuard.run(player,
                    () -> target.hurt(player.damageSources().playerAttack(player),
                            isBossLike(target) ? 2.0F : 5.0F));
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.7F);
            data.putLong(HAMMER_TIME_KEY, 0L);
        } else {
            data.putLong(HAMMER_TIME_KEY, now);
        }
    }

    private void applyCrystalSongStaff(ServerLevel level, ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Enemy) || isBossLike(target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, true, false, true));
        level.sendParticles(ParticleTypes.NOTE, target.getX(), target.getY() + target.getBbHeight(),
                target.getZ(), 6, 0.4, 0.2, 0.4, 0.01);
        level.playSound(null, target.blockPosition(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    private void applyOathblade(ServerPlayer player, LivingEntity target) {
        if (!target.getType().is(ModEntityTags.ECHO_REALM_HOSTILES)) {
            return;
        }
        WeaponEffectGuard.run(player,
                () -> target.hurt(player.damageSources().playerAttack(player),
                        isBossLike(target) ? 1.0F : 2.0F));
    }

    private static boolean isBossLike(LivingEntity target) {
        return target instanceof MiniBossEntity
                || target instanceof ForgottenColossus
                || target instanceof AbyssWatcher
                || target instanceof MirrorGuardian;
    }
}
