package cn.kurt6.unknown_echoes.item.weapon;

import cn.kurt6.unknown_echoes.entity.boss.AbyssWatcher;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 真视短刃(V0.6B 三件武器之一,V0.6C 强化):伤害 6.5 / 攻速 3.2(V3.2/V3.3 上调,原设定基线 4.5/2.0;近乎无冷却的匕首)。
 * 武器特性只随武器/材料线存在,不读取玩家能力研究:
 * - 裂镜连斩:4 秒内连续命中同一目标叠"镜痕",第 3 层破碎并造成额外伤害。
 * - 击中镜像类分身时短暂暴露真身方向(真身发光 3 秒 + 指向粒子),冷却 10 秒。
 * - 对幻象类目标的通用弱点识破由真视能力在 CommonEvents 中结算,不绑定本武器。
 */
public class TrueSightBladeItem extends SwordItem {

    /** 暴露真身冷却(tick):设定基线 10 秒。 */
    private static final int REVEAL_COOLDOWN_TICKS = 200;
    private static final double REVEAL_SEARCH_RADIUS = 48.0;

    /** 镜痕(V0.6C 连斩):叠层窗口与爆发伤害。存目标 persistentData,服务端权威。 */
    private static final String MARK_COUNT_KEY = "ue_mirror_mark_count";
    private static final String MARK_TIME_KEY = "ue_mirror_mark_time";
    private final int markWindowTicks;
    private final float shatterDamage;

    public TrueSightBladeItem(Properties properties) {
        this(6.5, 2.0, 80, 4.0F, properties);
    }

    public TrueSightBladeItem(double damage, double speed, int markWindowTicks,
                              float shatterDamage, Properties properties) {
        super(EchoMetalTier.INSTANCE, properties.attributes(WeaponAttributes.melee(damage, speed)));
        this.markWindowTicks = markWindowTicks;
        this.shatterDamage = shatterDamage;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.skill")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.detail")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.unknown_echoes.true_sight_blade.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public void applyDamageEventEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        this.tickMirrorMark(level, player, target);
        if (!player.getCooldowns().isOnCooldown(this)) {
            LivingEntity real = findRealBody(level, target);
            if (real != null) {
                player.getCooldowns().addCooldown(this, REVEAL_COOLDOWN_TICKS);
                int glowTicks = cn.kurt6.unknown_echoes.ability.EchoArmorSets
                        .hasListenerClueBonus(player) ? 120 : 60;
                real.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0, false, false, false));
                spawnRevealTrail(level, player, real);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
                player.displayClientMessage(
                        Component.translatable("message.unknown_echoes.true_sight_blade.reveal"), true);
            }
        }
    }

    /**
     * 裂镜连斩:4 秒窗口内连续命中同一目标叠镜痕(目标身上浮现裂镜微光),
     * 第 3 层破碎——额外爆发伤害(幻象类双倍)+ 短暂发光 + 碎镜环。
     * 层数存目标 persistentData(服务端),超窗自动重计。
     */
    private void tickMirrorMark(ServerLevel level, ServerPlayer player, LivingEntity target) {
        var data = target.getPersistentData();
        long now = level.getGameTime();
        int count = data.getInt(MARK_COUNT_KEY);
        long last = data.getLong(MARK_TIME_KEY);
        count = (now - last > markWindowTicks) ? 1 : count + 1;
        data.putLong(MARK_TIME_KEY, now);
        if (count < 3) {
            data.putInt(MARK_COUNT_KEY, count);
            // 叠层提示:目标身上的裂镜微光逐层变密
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    count * 3, 0.3, 0.4, 0.3, 0.02);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F + count * 0.3F);
            return;
        }
        // 第 3 层:镜痕破碎
        data.putInt(MARK_COUNT_KEY, 0);
        boolean illusion = target.getType().is(ModEntityTags.ILLUSION_MOBS);
        WeaponEffectGuard.run(player,
                () -> target.hurt(player.damageSources().playerAttack(player),
                        illusion ? shatterDamage * 2.0F : shatterDamage));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2 * i / 18;
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX() + Math.cos(angle) * 1.1,
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ() + Math.sin(angle) * 1.1, 1, 0.02, 0.1, 0.02, 0.0);
        }
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                14, 0.4, 0.4, 0.4, 0.15);
        level.playSound(null, target.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.1F, 1.5F);
    }

    /** 命中的是分身/假身时,返回场上的真身;否则 null。 */
    private static LivingEntity findRealBody(ServerLevel level, LivingEntity hit) {
        if (hit instanceof MirrorGuardian guardian && guardian.isIllusion()) {
            for (MirrorGuardian real : level.getEntitiesOfClass(MirrorGuardian.class,
                    hit.getBoundingBox().inflate(REVEAL_SEARCH_RADIUS),
                    g -> g.isAlive() && !g.isIllusion())) {
                return real;
            }
        }
        if (hit instanceof AbyssWatcher watcher && watcher.isClone()) {
            for (AbyssWatcher real : level.getEntitiesOfClass(AbyssWatcher.class,
                    hit.getBoundingBox().inflate(REVEAL_SEARCH_RADIUS),
                    w -> w.isAlive() && !w.isClone())) {
                return real;
            }
        }
        return null;
    }

    /** 从玩家眼前向真身铺一道短粒子轨迹(方向提示,不给精确坐标 UI)。 */
    private static void spawnRevealTrail(ServerLevel level, ServerPlayer player, LivingEntity real) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = real.position().add(0, real.getBbHeight() * 0.5, 0).subtract(from).normalize();
        for (int i = 1; i <= 10; i++) {
            Vec3 p = from.add(dir.scale(0.7 * i));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
