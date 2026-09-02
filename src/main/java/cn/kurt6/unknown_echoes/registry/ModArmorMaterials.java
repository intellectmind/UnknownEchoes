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

/** 件数效果不在材质里,见 ability/EchoArmorSets(服务端判定)。 */
public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, UnknownEchoes.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXPLORER =
            ARMOR_MATERIALS.register("explorer", () -> new ArmorMaterial(
                    defense(2, 6, 5, 2),
                    14,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ModItems.ECHO_FIBER.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("explorer"))),
                    0.5F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LISTENER =
            ARMOR_MATERIALS.register("listener", () -> new ArmorMaterial(
                    defense(3, 7, 6, 3),
                    18,
                    SoundEvents.ARMOR_EQUIP_CHAIN,
                    () -> Ingredient.of(ModItems.ECHO_METAL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("listener"))),
                    2.0F,
                    0.0F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHO_TRAVELER =
            ARMOR_MATERIALS.register("echo_traveler", () -> new ArmorMaterial(
                    defense(4, 9, 7, 4),
                    18,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.ECHO_METAL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("echo_traveler"))),
                    3.25F,
                    0.10F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WIND_WALKER =
            ARMOR_MATERIALS.register("wind_walker", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.WIND_RUNE_SHARD.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("wind_walker"))),
                    3.75F,
                    0.10F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TIDE_STALKER =
            ARMOR_MATERIALS.register("tide_stalker", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.MIRROR_LAKE_SHARD.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("tide_stalker"))),
                    3.75F,
                    0.10F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRUE_SIGHT_SHADOW =
            ARMOR_MATERIALS.register("true_sight_shadow", () -> new ArmorMaterial(
                    defense(4, 9, 8, 4),
                    20,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.ILLUSION_DUST.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("true_sight_shadow"))),
                    3.75F,
                    0.10F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SILENT_WATCH =
            ARMOR_MATERIALS.register("silent_watch", () -> new ArmorMaterial(
                    defense(4, 10, 8, 4),
                    22,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(ModItems.SILENCE_MOSS.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("silent_watch"))),
                    4.0F,
                    0.10F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ECHO_OATH =
            ARMOR_MATERIALS.register("echo_oath", () -> new ArmorMaterial(
                    defense(5, 10, 8, 5),
                    25,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.ECHO_REALM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("echo_oath"))),
                    4.5F,
                    0.125F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ABYSS_WALKER =
            ARMOR_MATERIALS.register("abyss_walker", () -> new ArmorMaterial(
                    defense(5, 11, 9, 5),
                    25,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.DEEP_ALLOY_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("abyss_walker"))),
                    5.0F,
                    0.15F));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RESONATOR =
            ARMOR_MATERIALS.register("resonator", () -> new ArmorMaterial(
                    defense(6, 12, 10, 6),
                    26,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.RESONANCE_ALLOY_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(UnknownEchoes.id("resonator"))),
                    6.0F,
                    0.20F));

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
