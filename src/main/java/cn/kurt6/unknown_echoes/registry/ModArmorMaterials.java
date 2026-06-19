package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * V0.6F 套装盔甲材质(11.3):
 * - 探索者:早期套装,皮革级偏上;材料 回响纤维/回响碎片/回响石砖,无 Boss 前置。
 * - 聆听者:中期套装,锁链~铁级;材料 镜湖碎片/晶羽/回响金属。
 * - 区域 Boss 套:裸防略高于下界合金,强度主要来自场景效果与能力联动。
 * 件数效果不在材质里,见 ability/EchoArmorSets(服务端判定)。
 * 深渊行者(V0.8 深层材料线)与共鸣者(V1.0 终局材料线)按二十三章版本规划后置。
 */
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, UnknownEchoes.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXPLORER =
            ARMOR_MATERIALS.register("explorer", () -> new ArmorMaterial(
                    defense(1, 4, 3, 2),
                    14,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ModItems.ECHO_FIBER.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("explorer"))),
                    0.0F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LISTENER =
            ARMOR_MATERIALS.register("listener", () -> new ArmorMaterial(
                    defense(2, 5, 4, 2),
                    18,
                    SoundEvents.ARMOR_EQUIP_CHAIN,
                    () -> Ingredient.of(ModItems.ECHO_METAL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("listener"))),
                    0.5F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHO_TRAVELER =
            ARMOR_MATERIALS.register("echo_traveler", () -> new ArmorMaterial(
                    defense(4, 9, 7, 4),
                    18,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.ECHO_METAL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("echo_traveler"))),
                    1.5F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WIND_WALKER =
            ARMOR_MATERIALS.register("wind_walker", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.WIND_RUNE_SHARD.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("wind_walker"))),
                    3.1F,
                    0.05F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TIDE_STALKER =
            ARMOR_MATERIALS.register("tide_stalker", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.MIRROR_LAKE_SHARD.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("tide_stalker"))),
                    3.1F,
                    0.05F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRUE_SIGHT_SHADOW =
            ARMOR_MATERIALS.register("true_sight_shadow", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.ILLUSION_DUST.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("true_sight_shadow"))),
                    3.1F,
                    0.05F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SILENT_WATCH =
            ARMOR_MATERIALS.register("silent_watch", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    22,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.SILENCE_MOSS.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("silent_watch"))),
                    3.2F,
                    0.05F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHO_OATH =
            ARMOR_MATERIALS.register("echo_oath", () -> new ArmorMaterial(
                    defense(5, 10, 9, 5),
                    24,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.ECHO_REALM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("echo_oath"))),
                    3.5F,
                    0.08F));

    private static Map<ArmorItem.Type, Integer> defense(int boots, int chest, int leggings, int helmet) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, chest);
        return map;
    }
}
