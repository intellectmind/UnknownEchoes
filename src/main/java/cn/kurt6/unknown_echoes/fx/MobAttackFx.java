package cn.kurt6.unknown_echoes.fx;

import cn.kurt6.unknown_echoes.registry.ModSounds;
import cn.kurt6.unknown_echoes.network.CombatFxPayload.Shape;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MobAttackFx {
    private MobAttackFx() {
    }

    private static String id(Mob mob) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key.getNamespace().equals("unknown_echoes") ? key.getPath() : "";
    }

    public static int color(Mob mob) {
        return switch (id(mob)) {
            case "ash_lantern_bat" -> 0xD9B46D;
            case "gale_shrike" -> 0xA6C9DA;
            case "ash_sail_serpent" -> 0xC5A18B;
            case "bell_needle_wasp" -> 0xD3BA70;
            case "reed_mask_raider" -> 0xA0AF7C;
            case "broken_bell_axeman" -> 0xCCAA68;
            case "rift_page_stalker" -> 0xAF92C4;
            case "shardwing_skate" -> 0xB6DBEE;
            case "crystal_spine_urchin" -> 0x92E1BA;
            default -> -1;
        };
    }

    public static float glow(int phase, float age) {
        if (phase == 1) {
            return 0.94F + 0.06F * (float) Math.sin(age * 0.65F);
        }
        return phase == 3 ? 0.90F : 1.0F;
    }

    public static void present(Mob mob, int phase) {
        int color = color(mob);
        String entity = id(mob);
        if (!(mob.level() instanceof ServerLevel level) || color < 0 || !mob.isAlive()
                || mob.isNoAi() || (phase != 1 && phase != 2)
                || entity.equals("shardwing_skate") || entity.equals("crystal_spine_urchin")) {
            return;
        }
        Vec3 forward = mob.getViewVector(1.0F);
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 center = mob.getBoundingBox().getCenter().add(forward.scale(mob.getBbWidth() * 0.35D));
        if (!mob.isInvisible()) {
            CombatFx.emit(level, phase == 1 ? Shape.ORBIT : Shape.BURST, center, forward,
                    Math.min(.8, mob.getBbWidth() * .45), .2, 0,
                    color, (color & 0xFEFEFE) >> 1, phase == 1 ? 10 : 8, false);
        }
        var dust = new DustParticleOptions(new Vector3f((color >> 16 & 255) / 255.0F,
                (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F), phase == 1 ? 0.65F : 0.85F);
        // 这是贴身动作提示，不画成伤害范围；每次起手或出手最多六个采样点。
        int count = mob.isInvisible() ? 0 : phase == 1 ? 4 : 6;
        for (int i = 0; i < count; i++) {
            double t = i / (double) (count - 1);
            Vec3 point = center.add(side.scale((t - 0.5D) * Math.min(0.8D, mob.getBbWidth())))
                    .add(0, Math.sin(t * Math.PI) * (phase == 1 ? 0.15D : 0.28D), 0);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        if (phase == 1) {
            switch (id(mob)) {
                case "reed_mask_raider" -> mob.playSound(ModSounds.REED_MASK_RAIDER_PREPARE.get(), 0.7F, 1.0F);
                case "broken_bell_axeman" -> mob.playSound(ModSounds.BROKEN_BELL_AXEMAN_PREPARE.get(), 0.8F, 1.0F);
                case "rift_page_stalker" -> mob.playSound(ModSounds.RIFT_PAGE_STALKER_PREPARE.get(), 0.7F, 1.0F);
            }
        } else {
            switch (id(mob)) {
                case "ash_lantern_bat" -> mob.playSound(ModSounds.ASH_LANTERN_BAT_STRIKE.get(), 0.65F, 1.0F);
                case "gale_shrike" -> mob.playSound(ModSounds.GALE_SHRIKE_STRIKE.get(), 0.65F, 1.0F);
                case "ash_sail_serpent" -> mob.playSound(ModSounds.ASH_SAIL_SERPENT_STRIKE.get(), 0.7F, 1.0F);
                case "bell_needle_wasp" -> mob.playSound(ModSounds.BELL_NEEDLE_WASP_STRIKE.get(), 0.65F, 1.0F);
                case "reed_mask_raider" -> mob.playSound(ModSounds.REED_MASK_RAIDER_STRIKE.get(), 0.7F, 1.0F);
                case "broken_bell_axeman" -> mob.playSound(ModSounds.BROKEN_BELL_AXEMAN_STRIKE.get(), 0.8F, 1.0F);
                case "rift_page_stalker" -> mob.playSound(ModSounds.RIFT_PAGE_STALKER_STRIKE.get(), 0.7F, 1.0F);
            }
        }
    }
}
