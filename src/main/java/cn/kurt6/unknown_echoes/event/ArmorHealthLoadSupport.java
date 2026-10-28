package cn.kurt6.unknown_echoes.event;

import cn.kurt6.unknown_echoes.ability.EchoArmorSets;
import cn.kurt6.unknown_echoes.artifact.ArtifactEffectManager;
import cn.kurt6.unknown_echoes.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHealthLoadSupport {
    private ArmorHealthLoadSupport() {
    }

    /** 只在玩家 NBT 读取结束、装备与神器附件均已加载后还原瞬态生命容量。 */
    public static void afterLoad(ServerPlayer player, CompoundTag compound) {
        float health = player.getHealth();
        float previousMax = player.getMaxHealth();
        if (!restoreCapacities(player)) {
            return;
        }
        // 原生在装备读取前钳制 Health；只补回落在旧上限处的存档值，不能按新上限回满。
        float savedHealth = compound.getFloat("Health");
        if (compound.contains("Health", Tag.TAG_ANY_NUMERIC) && Float.isFinite(savedHealth)
                && health == previousMax && savedHealth > health) {
            player.setHealth(savedHealth);
        }
    }

    public record CopiedHealth(float requested, float maximum, float applied, Level level,
                               float absorption, boolean hadFullSet) {
    }

    /** Clone 监听器已复制附件与永久加成后，补回原生复制阶段遗漏的容量和生命。 */
    public static void afterClone(ServerPlayer player, CopiedHealth copied, boolean wasDeath,
                                  float absorptionBeforeClone) {
        if (copied == null || player.level() != copied.level() || player.hasDisconnected()) {
            return;
        }
        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        if (!restoreCapacities(player) || player.hasDisconnected() || health != copied.applied()
                || !Float.isFinite(copied.requested()) || !Float.isFinite(copied.maximum())) {
            return;
        }
        if (wasDeath && copied.requested() == copied.maximum() && health == copied.maximum()) {
            player.setHealth(player.getMaxHealth());
        } else if (!wasDeath && health == copied.maximum() && copied.requested() > health) {
            player.setHealth(copied.requested());
        }
        // 末地返回只补回原四件套累积且尚未恢复的护盾，不叠加原生药水补盾或覆盖 Clone 改动。
        if (!wasDeath && copied.hadFullSet() && EchoArmorSets.resonatorPieces(player) >= 4
                && Float.isFinite(copied.absorption()) && copied.absorption() > absorption
                && absorption == absorptionBeforeClone && player.getAbsorptionAmount() == absorption) {
            player.setAbsorptionAmount(copied.absorption());
        }
    }

    private static boolean restoreCapacities(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        int pieces = EchoArmorSets.resonatorPieces(player);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wings = chest.is(ModItems.WORLD_ECHO_WINGS.get());
        var level = player.level();
        float health = player.getHealth();
        List<AttributeModifier> wingHealth = new ArrayList<>();
        if (wings) {
            chest.forEachModifier(EquipmentSlot.CHEST, (attribute, modifier) -> {
                if (attribute.is(Attributes.MAX_HEALTH)) {
                    wingHealth.add(modifier);
                }
            });
        }
        if (!player.isAlive() || player.isSpectator() || player.level() != level
                || player.getHealth() != health || EchoArmorSets.resonatorPieces(player) != pieces
                || player.getItemBySlot(EquipmentSlot.CHEST) != chest) {
            return false;
        }

        ResonatorArmorEvents.applySetAttributes(player, pieces);
        ArtifactEffectManager.refreshTideHealth(player);
        var maximum = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximum != null) {
            for (AttributeModifier modifier : wingHealth) {
                maximum.removeModifier(modifier.id());
                maximum.addTransientModifier(modifier);
            }
        }
        return true;
    }
}
