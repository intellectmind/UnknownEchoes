package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.item.weapon.WeaponEffectGuard;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public record ParticleAttack(ResourceLocation weapon, ParticleData data, UUID owner, UUID attack, float shadowBonus) {
    private static final String KEY = "ue_particle_shot";
    private static final String HITS = "ue_particle_hits";
    private static final ThreadLocal<ParticleAttack> SHOT = new ThreadLocal<>();

    public static ParticleAttack capture(ServerPlayer player, ItemStack weapon) {
        ParticleData data = ParticleRules.data(weapon);
        if (data.total() == 0 || WeaponEffectGuard.isActive(player)) return null;
        float bonus = ParticleEffects.consumeShadow(player, data.count(ParticleKind.SHADOW));
        UUID attack = UUID.randomUUID();
        if (bonus > 0) ParticleShadowClaims.grant(player.getPersistentData(), attack);
        return new ParticleAttack(BuiltInRegistries.ITEM.getKey(weapon.getItem()), data, player.getUUID(), attack, bonus);
    }

    public static void shoot(net.minecraft.world.entity.LivingEntity shooter, ItemStack weapon, Runnable fire) {
        ParticleAttack previous = SHOT.get();
        ParticleAttack snapshot = shooter instanceof ServerPlayer player ? capture(player, weapon) : null;
        if (snapshot == null) SHOT.remove(); else SHOT.set(snapshot);
        try { fire.run(); }
        finally { if (previous == null) SHOT.remove(); else SHOT.set(previous); }
    }

    public static void launched(AbstractArrow arrow) {
        ParticleAttack snapshot = SHOT.get();
        if (snapshot != null && !arrow.getPersistentData().contains(KEY)
                && arrow.getOwner() instanceof ServerPlayer player && snapshot.owner.equals(player.getUUID()))
            arrow.getPersistentData().put(KEY, snapshot.save());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("weapon", weapon.toString());
        tag.putInt("tier", data.tier());
        tag.putIntArray("counts", data.counts().stream().mapToInt(Integer::intValue).toArray());
        tag.putUUID("owner", owner);
        tag.putUUID("attack", attack);
        tag.putFloat("shadow", shadowBonus);
        return tag;
    }

    public static ParticleAttack projectile(AbstractArrow arrow, ServerPlayer player) {
        if (!arrow.getPersistentData().contains(KEY, Tag.TAG_COMPOUND) || arrow.getOwner() != player) return null;
        return load(arrow.getPersistentData().getCompound(KEY), player.getUUID());
    }

    public static ParticleAttack load(CompoundTag tag, UUID owner) {
        try {
            if (!tag.contains("weapon", Tag.TAG_STRING) || !tag.contains("tier", Tag.TAG_INT)
                    || !tag.contains("counts", Tag.TAG_INT_ARRAY) || !tag.contains("shadow", Tag.TAG_FLOAT)) return null;
            ResourceLocation weapon = ResourceLocation.tryParse(tag.getString("weapon"));
            if (weapon == null || !weapon.getNamespace().equals(UnknownEchoes.MODID) || !ParticleRules.WEAPONS.contains(weapon.getPath())
                    || !tag.hasUUID("owner") || !tag.hasUUID("attack") || !tag.getUUID("owner").equals(owner)) return null;
            float bonus = tag.getFloat("shadow");
            ParticleData data = new ParticleData(tag.getInt("tier"), Arrays.stream(tag.getIntArray("counts")).boxed().toList());
            int shadow = data.count(ParticleKind.SHADOW);
            if (data.total() == 0 || !(bonus == 0 || bonus == 0.15F && shadow >= 30 || bonus == 0.6F && shadow >= 35)) return null;
            return new ParticleAttack(weapon, data, owner, tag.getUUID("attack"), bonus);
        } catch (IllegalArgumentException ignored) { return null; }
    }

    public static boolean seen(AbstractArrow arrow, UUID target) {
        CompoundTag tag = arrow.getPersistentData();
        if (!tag.contains(HITS)) return false;
        if (!tag.contains(HITS, Tag.TAG_LIST)) return true;
        ListTag list = (ListTag) tag.get(HITS);
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_INT_ARRAY) return true;
        if (list.size() >= 128) return true;
        for (int i = 0; i < list.size(); i++) if (list.getIntArray(i).length != 4) return true;
        return list.contains(NbtUtils.createUUID(target));
    }

    public static void mark(AbstractArrow arrow, UUID target) {
        if (seen(arrow, target)) return;
        ListTag list = arrow.getPersistentData().getList(HITS, Tag.TAG_INT_ARRAY);
        list.add(NbtUtils.createUUID(target));
        arrow.getPersistentData().put(HITS, list);
    }

    public float availableShadow(ServerPlayer player) {
        return owner.equals(player.getUUID()) && ParticleShadowClaims.available(player.getPersistentData(), attack) ? shadowBonus : 0;
    }

    public void claimShadow(ServerPlayer player) {
        if (shadowBonus > 0 && owner.equals(player.getUUID())) ParticleShadowClaims.claim(player.getPersistentData(), attack);
    }
}
