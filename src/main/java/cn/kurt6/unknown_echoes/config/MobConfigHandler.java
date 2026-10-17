package cn.kurt6.unknown_echoes.config;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.entity.boss.RechallengeBoundBoss;
import cn.kurt6.unknown_echoes.entity.boss.DeepRechallengeBoundBoss;
import cn.kurt6.unknown_echoes.entity.boss.MiniBossEntity;
import cn.kurt6.unknown_echoes.entity.boss.WorldEcho;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = UnknownEchoes.MODID)
public final class MobConfigHandler {
    private MobConfigHandler() { }

    public static boolean isOrdinaryMob(LivingEntity entity) {
        return entity instanceof Mob && !(entity instanceof RechallengeBoundBoss)
                && !(entity instanceof DeepRechallengeBoundBoss)
                && !(entity instanceof MiniBossEntity) && !(entity instanceof WorldEcho)
                && entity.getType().getCategory() != MobCategory.MISC
                && UnknownEchoes.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    public static boolean isMonster(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL
                || event.getSpawnType() != MobSpawnType.NATURAL
                && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION
                || !isOrdinaryMob(event.getEntity())) return;
        var mob = event.getEntity();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        double chance = isMonster(mob) ? ServerConfig.MONSTER_SPAWN_CHANCE.get()
                : ServerConfig.CREATURE_SPAWN_CHANCE.get();
        // 默认值不消耗随机数，避免改变原有生成序列；这里只否决，不强制通过原版检查。
        if (ServerConfig.DISABLED_NATURAL_SPAWNS.get().contains(id)
                || chance <= 0.0D || chance < 1.0D && mob.getRandom().nextDouble() >= chance) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity().level().isClientSide || !isOrdinaryMob(event.getEntity())) return;
        double scaled = event.getDroppedExperience() * ServerConfig.MOB_EXPERIENCE_MULTIPLIER.get();
        event.setDroppedExperience((int) Math.clamp(Math.round(scaled), 0L, Integer.MAX_VALUE));
    }
}
