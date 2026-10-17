package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.model.EchoArmorModel;
import cn.kurt6.unknown_echoes.client.model.ThemedEchoArmorModel;
import cn.kurt6.unknown_echoes.client.render.AbyssWatcherRenderer;
import cn.kurt6.unknown_echoes.client.render.AshScribeRenderer;
import cn.kurt6.unknown_echoes.client.gui.EchoStatusHudOverlay;
import cn.kurt6.unknown_echoes.client.render.BrokenBellKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.CrystalSongkeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.DeepEcologyRenderer;
import cn.kurt6.unknown_echoes.client.render.DreamBloomKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.EchoProjectionRenderer;
import cn.kurt6.unknown_echoes.client.render.LostRecorderChiefRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorGuardianRenderer;
import cn.kurt6.unknown_echoes.client.render.RiftWardenRenderer;
import cn.kurt6.unknown_echoes.client.render.CrystalFeatherBirdRenderer;
import cn.kurt6.unknown_echoes.client.render.SilentWalkerRenderer;
import cn.kurt6.unknown_echoes.client.render.EchoWandererRenderer;
import cn.kurt6.unknown_echoes.client.render.EndingRecordTableRenderer;
import cn.kurt6.unknown_echoes.client.render.ForgottenColossusRenderer;
import cn.kurt6.unknown_echoes.client.render.InvertedGatekeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.SilentPriestRenderer;
import cn.kurt6.unknown_echoes.client.render.StormWeaverRenderer;
import cn.kurt6.unknown_echoes.client.render.TideBoltRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorDustButlerRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorDustDecoyRenderer;
import cn.kurt6.unknown_echoes.client.render.TideLanternKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.TideWaterShadeRenderer;
import cn.kurt6.unknown_echoes.client.render.VoidProphetRenderer;
import cn.kurt6.unknown_echoes.client.render.WorldEchoRenderer;
import cn.kurt6.unknown_echoes.client.render.WorldEchoWingsLayer;
import cn.kurt6.unknown_echoes.client.render.WorldResonanceAltarRenderer;
import cn.kurt6.unknown_echoes.registry.ModBlockEntities;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModArmorMaterials;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    private static final IClientItemExtensions ECHO_ARMOR_EXTENSIONS = new IClientItemExtensions() {
        private final Map<EquipmentSlot, EchoArmorModel<LivingEntity>> abyssModels =
                createAbyssArmorModels();
        private final Map<ThemedEchoArmorModel.Style, Map<EquipmentSlot, ThemedEchoArmorModel<LivingEntity>>>
                themedModels = createThemedArmorModels();

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                       EquipmentSlot equipmentSlot,
                                                       HumanoidModel<?> original) {
            ThemedEchoArmorModel.Style style = themedStyle(itemStack);
            if (style == null) {
                return this.abyssModels.get(equipmentSlot);
            }
            return this.themedModels.get(style).get(equipmentSlot);
        }
    };

    private static Map<EquipmentSlot, EchoArmorModel<LivingEntity>> createAbyssArmorModels() {
        EnumMap<EquipmentSlot, EchoArmorModel<LivingEntity>> result =
                new EnumMap<>(EquipmentSlot.class);
        result.put(EquipmentSlot.HEAD, EchoArmorModel.create(EquipmentSlot.HEAD));
        result.put(EquipmentSlot.CHEST, EchoArmorModel.create(EquipmentSlot.CHEST));
        result.put(EquipmentSlot.LEGS, EchoArmorModel.create(EquipmentSlot.LEGS));
        result.put(EquipmentSlot.FEET, EchoArmorModel.create(EquipmentSlot.FEET));
        return result;
    }

    private static Map<ThemedEchoArmorModel.Style, Map<EquipmentSlot, ThemedEchoArmorModel<LivingEntity>>>
    createThemedArmorModels() {
        EnumMap<ThemedEchoArmorModel.Style, Map<EquipmentSlot, ThemedEchoArmorModel<LivingEntity>>> result =
                new EnumMap<>(ThemedEchoArmorModel.Style.class);
        for (ThemedEchoArmorModel.Style style : ThemedEchoArmorModel.Style.values()) {
            EnumMap<EquipmentSlot, ThemedEchoArmorModel<LivingEntity>> slots =
                    new EnumMap<>(EquipmentSlot.class);
            slots.put(EquipmentSlot.HEAD, ThemedEchoArmorModel.create(style, EquipmentSlot.HEAD));
            slots.put(EquipmentSlot.CHEST, ThemedEchoArmorModel.create(style, EquipmentSlot.CHEST));
            slots.put(EquipmentSlot.LEGS, ThemedEchoArmorModel.create(style, EquipmentSlot.LEGS));
            slots.put(EquipmentSlot.FEET, ThemedEchoArmorModel.create(style, EquipmentSlot.FEET));
            result.put(style, slots);
        }
        return result;
    }

    private static ThemedEchoArmorModel.Style themedStyle(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) {
            return null;
        }
        if (armor.getMaterial().is(ModArmorMaterials.EXPLORER.getKey())) {
            return ThemedEchoArmorModel.Style.EXPLORER;
        }
        if (armor.getMaterial().is(ModArmorMaterials.ECHO_TRAVELER.getKey())) {
            return ThemedEchoArmorModel.Style.ECHO_TRAVELER;
        }
        if (armor.getMaterial().is(ModArmorMaterials.LISTENER.getKey())) {
            return ThemedEchoArmorModel.Style.LISTENER;
        }
        if (armor.getMaterial().is(ModArmorMaterials.WIND_WALKER.getKey())) {
            return ThemedEchoArmorModel.Style.WIND_WALKER;
        }
        if (armor.getMaterial().is(ModArmorMaterials.TIDE_STALKER.getKey())) {
            return ThemedEchoArmorModel.Style.TIDE_STALKER;
        }
        if (armor.getMaterial().is(ModArmorMaterials.TRUE_SIGHT_SHADOW.getKey())) {
            return ThemedEchoArmorModel.Style.TRUE_SIGHT_SHADOW;
        }
        if (armor.getMaterial().is(ModArmorMaterials.SILENT_WATCH.getKey())) {
            return ThemedEchoArmorModel.Style.SILENT_WATCH;
        }
        if (armor.getMaterial().is(ModArmorMaterials.ECHO_OATH.getKey())) {
            return ThemedEchoArmorModel.Style.ECHO_OATH;
        }
        if (armor.getMaterial().is(ModArmorMaterials.RESONATOR.getKey())) {
            return ThemedEchoArmorModel.Style.RESONATOR;
        }
        return null;
    }

    public static final KeyMapping OPEN_ECHO_OVERVIEW = new KeyMapping(
            "key.unknown_echoes.echo_overview", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.unknown_echoes");

    public static final KeyMapping WEAPON_SKILL = new KeyMapping(
            "key.unknown_echoes.weapon_skill", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.unknown_echoes");

    /** 双战技武器的副战技(默认 X,可改键)，不占用右键交互。 */
    public static final KeyMapping WEAPON_SECONDARY_SKILL = new KeyMapping(
            "key.unknown_echoes.weapon_secondary_skill", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X, "key.categories.unknown_echoes");

    public static final KeyMapping ECHO_TOOL_SKILL = new KeyMapping(
            "key.unknown_echoes.echo_tool_skill", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z, "key.categories.unknown_echoes");

    public static final KeyMapping GRAVITY_ATTUNE = new KeyMapping(
            "key.unknown_echoes.gravity_attune", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.unknown_echoes");

    public static final KeyMapping VOID_STEP = new KeyMapping(
            "key.unknown_echoes.void_step", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.unknown_echoes");

    public static final KeyMapping ARTIFACT_USE = new KeyMapping(
            "key.unknown_echoes.artifact_use", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U, "key.categories.unknown_echoes");

    /** 手持物品主动技能(默认 C)：不占用右键交互。 */
    public static final KeyMapping ITEM_ACTIVE_SKILL = new KeyMapping(
            "key.unknown_echoes.item_active_skill", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, "key.categories.unknown_echoes");

    /** 手持物品副功能(默认 B)：不占用 Shift+右键。 */
    public static final KeyMapping ITEM_ALTERNATE_SKILL = new KeyMapping(
            "key.unknown_echoes.item_alternate_skill", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, "key.categories.unknown_echoes");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ECHO_OVERVIEW);
        event.register(WEAPON_SKILL);
        event.register(WEAPON_SECONDARY_SKILL);
        event.register(ECHO_TOOL_SKILL);
        event.register(GRAVITY_ATTUNE);
        event.register(VOID_STEP);
        event.register(ARTIFACT_USE);
        event.register(ITEM_ACTIVE_SKILL);
        event.register(ITEM_ALTERNATE_SKILL);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                UnknownEchoes.id("echo_status_hud"), EchoStatusHudOverlay::render);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ECHO_ARMOR_EXTENSIONS,
                ModItems.EXPLORER_HELMET.get(), ModItems.EXPLORER_CHESTPLATE.get(),
                ModItems.EXPLORER_LEGGINGS.get(), ModItems.EXPLORER_BOOTS.get(),
                ModItems.LISTENER_HELMET.get(), ModItems.LISTENER_CHESTPLATE.get(),
                ModItems.LISTENER_LEGGINGS.get(), ModItems.LISTENER_BOOTS.get(),
                ModItems.ECHO_TRAVELER_HELMET.get(), ModItems.ECHO_TRAVELER_CHESTPLATE.get(),
                ModItems.ECHO_TRAVELER_LEGGINGS.get(), ModItems.ECHO_TRAVELER_BOOTS.get(),
                ModItems.WIND_WALKER_HELMET.get(), ModItems.WIND_WALKER_CHESTPLATE.get(),
                ModItems.WIND_WALKER_LEGGINGS.get(), ModItems.WIND_WALKER_BOOTS.get(),
                ModItems.TIDE_STALKER_HELMET.get(), ModItems.TIDE_STALKER_CHESTPLATE.get(),
                ModItems.TIDE_STALKER_LEGGINGS.get(), ModItems.TIDE_STALKER_BOOTS.get(),
                ModItems.TRUE_SIGHT_SHADOW_HELMET.get(), ModItems.TRUE_SIGHT_SHADOW_CHESTPLATE.get(),
                ModItems.TRUE_SIGHT_SHADOW_LEGGINGS.get(), ModItems.TRUE_SIGHT_SHADOW_BOOTS.get(),
                ModItems.SILENT_WATCH_HELMET.get(), ModItems.SILENT_WATCH_CHESTPLATE.get(),
                ModItems.SILENT_WATCH_LEGGINGS.get(), ModItems.SILENT_WATCH_BOOTS.get(),
                ModItems.ECHO_OATH_HELMET.get(), ModItems.ECHO_OATH_CHESTPLATE.get(),
                ModItems.ECHO_OATH_LEGGINGS.get(), ModItems.ECHO_OATH_BOOTS.get(),
                ModItems.ABYSS_WALKER_HELMET.get(), ModItems.ABYSS_WALKER_CHESTPLATE.get(),
                ModItems.ABYSS_WALKER_LEGGINGS.get(), ModItems.ABYSS_WALKER_BOOTS.get(),
                ModItems.RESONATOR_HELMET.get(), ModItems.RESONATOR_CHESTPLATE.get(),
                ModItems.RESONATOR_LEGGINGS.get(), ModItems.RESONATOR_BOOTS.get(),
                ModItems.WORLD_ECHO_WINGS.get());
    }

    /** Boss 与 Mini Boss 刷怪蛋是手绘贴图:NeoForge 在 HIGHEST 给全部 DeferredSpawnEggItem 注册双色染色,这里在 LOWEST 覆盖为不染色。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> -1,
                ModItems.FORGOTTEN_COLOSSUS_SPAWN_EGG.get(), ModItems.ABYSS_WATCHER_SPAWN_EGG.get(),
                ModItems.MIRROR_GUARDIAN_SPAWN_EGG.get(), ModItems.VOID_PROPHET_SPAWN_EGG.get(),
                ModItems.RIFT_WARDEN_SPAWN_EGG.get(), ModItems.WORLD_ECHO_SPAWN_EGG.get(),
                ModItems.STORM_WEAVER_SPAWN_EGG.get(), ModItems.TIDE_LANTERN_KEEPER_SPAWN_EGG.get(),
                ModItems.MIRROR_DUST_BUTLER_SPAWN_EGG.get(), ModItems.SILENT_PRIEST_SPAWN_EGG.get(),
                ModItems.CRYSTAL_SONGKEEPER_SPAWN_EGG.get(), ModItems.BROKEN_BELL_KEEPER_SPAWN_EGG.get(),
                ModItems.DREAM_BLOOM_KEEPER_SPAWN_EGG.get(), ModItems.LOST_RECORDER_CHIEF_SPAWN_EGG.get(),
                ModItems.INVERTED_GATEKEEPER_SPAWN_EGG.get(), ModItems.ASH_SCRIBE_SPAWN_EGG.get());
    }

    /** 潮汐弩拉弦/装填模型谓词( 视觉重做):自定义弩不继承原版 crossbow 的 ItemProperties。 */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowPredicates(ModItems.ECHO_LONGBOW.get());
            registerBowPredicates(ModItems.DREAM_BLOOM_BOW.get());
            registerBowPredicates(ModItems.GRAVITY_INVERSION_BOW.get());
            registerCrossbowPredicates(ModItems.TIDE_CROSSBOW.get());
            registerCrossbowPredicates(ModItems.ASH_SCRIPT_CROSSBOW.get());
        });
    }

    private static void registerBowPredicates(Item item) {
        ItemProperties.register(item,
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                });
        ItemProperties.register(item,
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                                ? 1.0F : 0.0F);
    }

    private static void registerCrossbowPredicates(Item item) {
        ItemProperties.register(item,
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || CrossbowItem.isCharged(stack)) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                            / (float) CrossbowItem.getChargeDuration(stack, entity);
                });
        ItemProperties.register(item,
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                                && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(item,
                ResourceLocation.withDefaultNamespace("charged"),
                (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MOSSBACK_HEDGEHOG.get(),
                context -> new DeepEcologyRenderer<>(context, "mossback_hedgehog", 0.325F));
        event.registerEntityRenderer(ModEntities.EMBERCLAW_SCORPION.get(),
                context -> new DeepEcologyRenderer<>(context, "emberclaw_scorpion", 0.450F));
        event.registerEntityRenderer(ModEntities.LANTERN_SEAHORSE.get(),
                context -> new DeepEcologyRenderer<>(context, "lantern_seahorse", 0.225F));
        event.registerEntityRenderer(ModEntities.REEFJAW_SHARK.get(),
                context -> new DeepEcologyRenderer<>(context, "reefjaw_shark", 0.650F));
        event.registerEntityRenderer(ModEntities.SLATEHORN_BOAR.get(),
                context -> new DeepEcologyRenderer<>(context, "slatehorn_boar", 0.475F));
        event.registerEntityRenderer(ModEntities.HOLLOW_REED_SERPENT.get(),
                context -> new DeepEcologyRenderer<>(context, "hollow_reed_serpent", 0.375F));
        event.registerEntityRenderer(ModEntities.RIFTFANG_EEL.get(),
                context -> new DeepEcologyRenderer<>(context, "riftfang_eel", 0.400F));
        event.registerEntityRenderer(ModEntities.NEEDLEFIN_LIONFISH.get(),
                context -> new DeepEcologyRenderer<>(context, "needlefin_lionfish", 0.475F));
        event.registerEntityRenderer(ModEntities.MOSSPLATE_HOUND.get(),
                context -> new DeepEcologyRenderer<>(context, "mossplate_hound", 0.4F));
        event.registerEntityRenderer(ModEntities.CAIRN_SHELL_BRUTE.get(),
                context -> new DeepEcologyRenderer<>(context, "cairn_shell_brute", 0.45F));
        event.registerEntityRenderer(ModEntities.SPORE_CROWNED_WRETCH.get(),
                context -> new DeepEcologyRenderer<>(context, "spore_crowned_wretch", 0.225F));
        event.registerEntityRenderer(ModEntities.SPORE_PELLET.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.REED_MASK_RAIDER.get(),
                context -> new DeepEcologyRenderer<>(context, "reed_mask_raider", 0.4F));
        event.registerEntityRenderer(ModEntities.BROKEN_BELL_AXEMAN.get(),
                context -> new DeepEcologyRenderer<>(context, "broken_bell_axeman", 0.4F));
        event.registerEntityRenderer(ModEntities.RIFT_PAGE_STALKER.get(),
                context -> new DeepEcologyRenderer<>(context, "rift_page_stalker", 0.4F));
        event.registerEntityRenderer(ModEntities.MOSSFANG_TOAD.get(),
                context -> new DeepEcologyRenderer<>(context, "mossfang_toad", 0.475F));
        event.registerEntityRenderer(ModEntities.AMBERBACK_BEETLE.get(),
                context -> new DeepEcologyRenderer<>(context, "amberback_beetle", 0.450F));
        event.registerEntityRenderer(ModEntities.LANTERNJAW_ANGLER.get(),
                context -> new DeepEcologyRenderer<>(context, "lanternjaw_angler", 0.475F));
        event.registerEntityRenderer(ModEntities.SAWTOOTH_PRAWN.get(),
                context -> new DeepEcologyRenderer<>(context, "sawtooth_prawn", 0.550F));
        event.registerEntityRenderer(ModEntities.SAILTAIL_MINNOW.get(),
                cn.kurt6.unknown_echoes.client.render.SailtailMinnowRenderer::new);
        event.registerEntityRenderer(ModEntities.MOSS_SAIL_SNAIL.get(),
                cn.kurt6.unknown_echoes.client.render.MossSailSnailRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_SPINE_URCHIN.get(),
                cn.kurt6.unknown_echoes.client.render.CrystalSpineUrchinRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_SAIL_SERPENT.get(),
                cn.kurt6.unknown_echoes.client.render.AshSailSerpentRenderer::new);
        event.registerEntityRenderer(ModEntities.PETALWING_BUTTERFLY.get(),
                cn.kurt6.unknown_echoes.client.render.PetalwingButterflyRenderer::new);
        event.registerEntityRenderer(ModEntities.MOSSLIGHT_FIREFLY.get(),
                cn.kurt6.unknown_echoes.client.render.MosslightFireflyRenderer::new);
        event.registerEntityRenderer(ModEntities.BELL_NEEDLE_WASP.get(),
                cn.kurt6.unknown_echoes.client.render.BellNeedleWaspRenderer::new);
        event.registerEntityRenderer(ModEntities.SHARDWING_SKATE.get(),
                cn.kurt6.unknown_echoes.client.render.ShardwingSkateRenderer::new);

        event.registerBlockEntityRenderer(ModBlockEntities.ENDING_RECORD_TABLE.get(),
                EndingRecordTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORLD_RESONANCE_ALTAR.get(),
                WorldResonanceAltarRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO_WANDERER.get(), EchoWandererRenderer::new);
        event.registerEntityRenderer(ModEntities.FORGOTTEN_COLOSSUS.get(), ForgottenColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSS_WATCHER.get(), AbyssWatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_PROPHET.get(), VoidProphetRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT_WARDEN.get(), RiftWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.WORLD_ECHO.get(), WorldEchoRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_GUARDIAN.get(), MirrorGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.SILENT_WALKER.get(), SilentWalkerRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_FEATHER_BIRD.get(), CrystalFeatherBirdRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO_PROJECTION.get(), EchoProjectionRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_WEAVER.get(), StormWeaverRenderer::new);
        event.registerEntityRenderer(ModEntities.SILENT_PRIEST.get(), SilentPriestRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDE_BOLT.get(), TideBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDE_LANTERN_KEEPER.get(), TideLanternKeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDE_WATER_SHADE.get(), TideWaterShadeRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_DUST_BUTLER.get(), MirrorDustButlerRenderer::new);
        event.registerEntityRenderer(ModEntities.LOST_TRAVELER.get(),
                cn.kurt6.unknown_echoes.client.render.LostTravelerRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO_DEER.get(),
                cn.kurt6.unknown_echoes.client.render.EchoDeerRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOW_RABBIT.get(),
                cn.kurt6.unknown_echoes.client.render.GlowRabbitRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_TAIL_FISH.get(),
                cn.kurt6.unknown_echoes.client.render.MirrorTailFishRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO_CUTTLEFISH.get(),
                context -> new DeepEcologyRenderer<>(context, "echo_cuttlefish", 0.28F, -0.0523D));
        event.registerEntityRenderer(ModEntities.PEARLBACK_RAY.get(),
                context -> new DeepEcologyRenderer<>(context, "pearlback_ray", 0.45F, -0.1938D));
        event.registerEntityRenderer(ModEntities.MOSS_BACK_TURTLE.get(),
                cn.kurt6.unknown_echoes.client.render.MossBackTurtleRenderer::new);
        event.registerEntityRenderer(ModEntities.DREAMING_DEER.get(),
                cn.kurt6.unknown_echoes.client.render.DreamingDeerRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_CHIME_SPIRIT.get(),
                cn.kurt6.unknown_echoes.client.render.CrystalChimeSpiritRenderer::new);
        event.registerEntityRenderer(ModEntities.BROKEN_BELL_GUARD.get(),
                cn.kurt6.unknown_echoes.client.render.BrokenBellGuardRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_NOISE_WISP.get(),
                cn.kurt6.unknown_echoes.client.render.CrystalNoiseWispRenderer::new);
        event.registerEntityRenderer(ModEntities.BROKEN_BELL_CROW.get(),
                cn.kurt6.unknown_echoes.client.render.BrokenBellCrowRenderer::new);
        event.registerEntityRenderer(ModEntities.LOST_RECORDER.get(),
                cn.kurt6.unknown_echoes.client.render.LostRecorderRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_SAC_MOTH.get(),
                cn.kurt6.unknown_echoes.client.render.WindSacMothRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_DRIFT_JELLY.get(),
                cn.kurt6.unknown_echoes.client.render.WindDriftJellyRenderer::new);
        event.registerEntityRenderer(ModEntities.REEDWING_DRAGONFLY.get(),
                cn.kurt6.unknown_echoes.client.render.ReedwingDragonflyRenderer::new);
        event.registerEntityRenderer(ModEntities.GALE_SHRIKE.get(),
                cn.kurt6.unknown_echoes.client.render.GaleShrikeRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_LANTERN_BAT.get(),
                cn.kurt6.unknown_echoes.client.render.AshLanternBatRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_ERODED_SENTINEL.get(),
                cn.kurt6.unknown_echoes.client.render.WindErodedSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.ECHO_REMNANT.get(),
                cn.kurt6.unknown_echoes.client.render.EchoRemnantRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_SHADOW.get(),
                cn.kurt6.unknown_echoes.client.render.WaterShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_DUST_DECOY.get(), MirrorDustDecoyRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_SONGKEEPER.get(), CrystalSongkeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.BROKEN_BELL_KEEPER.get(), BrokenBellKeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.DREAM_BLOOM_KEEPER.get(), DreamBloomKeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.LOST_RECORDER_CHIEF.get(), LostRecorderChiefRenderer::new);
        event.registerEntityRenderer(ModEntities.INVERTED_GATEKEEPER.get(), InvertedGatekeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_SCRIBE.get(), AshScribeRenderer::new);

        event.registerEntityRenderer(ModEntities.FOLDPAGE_BUG.get(),
                cn.kurt6.unknown_echoes.client.render.FoldpageBugRenderer::new);
        event.registerEntityRenderer(ModEntities.ANCHOR_SHELL_BEAST.get(),
                cn.kurt6.unknown_echoes.client.render.AnchorShellBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSS_LURKER.get(),
                cn.kurt6.unknown_echoes.client.render.AbyssLurkerRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_WANDERER.get(),
                cn.kurt6.unknown_echoes.client.render.VoidWandererRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT_GLIDER.get(),
                context -> new DeepEcologyRenderer<>(context, "rift_glider", 0.72F, -0.4688D));
        event.registerEntityRenderer(ModEntities.ASHBACK_GRAZER.get(),
                context -> new DeepEcologyRenderer<>(context, "ashback_grazer", 0.7F));
        event.registerEntityRenderer(ModEntities.HUSHLIGHT_JELLY.get(),
                context -> new DeepEcologyRenderer<>(context, "hushlight_jelly", 0.52F));
        event.registerEntityRenderer(ModEntities.FOLDED_MANTIS.get(),
                context -> new DeepEcologyRenderer<>(context, "folded_mantis", 0.58F, -0.0669D));
        event.registerEntityRenderer(ModEntities.GRAVITY_RAM.get(),
                context -> new DeepEcologyRenderer<>(context, "gravity_ram", 0.76F));
        event.registerEntityRenderer(ModEntities.ASHVEIL_STALKER.get(),
                context -> new DeepEcologyRenderer<>(context, "ashveil_stalker", 0.58F));
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (var model : EndingRecordTableRenderer.KINETIC_MODELS) {
            event.register(model);
        }
        for (var model : WorldResonanceAltarRenderer.KINETIC_MODELS) {
            event.register(model);
        }
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new WorldEchoWingsLayer(renderer));
            }
        }
    }
}
