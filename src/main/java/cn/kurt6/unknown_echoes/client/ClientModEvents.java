package cn.kurt6.unknown_echoes.client;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.client.model.EchoArmorModel;
import cn.kurt6.unknown_echoes.client.render.AbyssWatcherRenderer;
import cn.kurt6.unknown_echoes.client.gui.ArtifactHudOverlay;
import cn.kurt6.unknown_echoes.client.render.BrokenBellKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.CrystalSongkeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.DreamBloomKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.EchoProjectionRenderer;
import cn.kurt6.unknown_echoes.client.render.LostRecorderChiefRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorGuardianRenderer;
import cn.kurt6.unknown_echoes.client.render.CrystalFeatherBirdRenderer;
import cn.kurt6.unknown_echoes.client.render.SilentWalkerRenderer;
import cn.kurt6.unknown_echoes.client.render.EchoWandererRenderer;
import cn.kurt6.unknown_echoes.client.render.ForgottenColossusRenderer;
import cn.kurt6.unknown_echoes.client.render.SilentPriestRenderer;
import cn.kurt6.unknown_echoes.client.render.StormWeaverRenderer;
import cn.kurt6.unknown_echoes.client.render.TideBoltRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorDustButlerRenderer;
import cn.kurt6.unknown_echoes.client.render.MirrorDustDecoyRenderer;
import cn.kurt6.unknown_echoes.client.render.TideLanternKeeperRenderer;
import cn.kurt6.unknown_echoes.client.render.TideWaterShadeRenderer;
import cn.kurt6.unknown_echoes.registry.ModEntities;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = UnknownEchoes.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    private static final IClientItemExtensions ECHO_ARMOR_EXTENSIONS = new IClientItemExtensions() {
        private final EchoArmorModel<LivingEntity> inner = EchoArmorModel.inner();
        private final EchoArmorModel<LivingEntity> outer = EchoArmorModel.outer();

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                       EquipmentSlot equipmentSlot,
                                                       HumanoidModel<?> original) {
            return equipmentSlot == EquipmentSlot.LEGS ? this.inner : this.outer;
        }
    };

    /** 打开回响总览(默认 K)。 */
    public static final KeyMapping OPEN_ECHO_OVERVIEW = new KeyMapping(
            "key.unknown_echoes.echo_overview", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.unknown_echoes");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ECHO_OVERVIEW);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                UnknownEchoes.id("artifact_hud"), ArtifactHudOverlay::render);
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
                ModItems.ECHO_OATH_LEGGINGS.get(), ModItems.ECHO_OATH_BOOTS.get());
    }

    /** 潮汐弩拉弦/装填模型谓词(V0.6C 视觉重做):自定义弩不继承原版 crossbow 的 ItemProperties。 */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowPredicates(ModItems.ECHO_LONGBOW.get());
            registerBowPredicates(ModItems.DREAM_BLOOM_BOW.get());
            registerTideCrossbowPredicates(ModItems.TIDE_CROSSBOW.get());
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

    private static void registerTideCrossbowPredicates(Item item) {
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
        event.registerEntityRenderer(ModEntities.ECHO_WANDERER.get(), EchoWandererRenderer::new);
        event.registerEntityRenderer(ModEntities.FORGOTTEN_COLOSSUS.get(), ForgottenColossusRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSS_WATCHER.get(), AbyssWatcherRenderer::new);
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
        // V0.6F 普通环境生物
        event.registerEntityRenderer(ModEntities.ECHO_DEER.get(),
                cn.kurt6.unknown_echoes.client.render.EchoDeerRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOW_RABBIT.get(),
                cn.kurt6.unknown_echoes.client.render.GlowRabbitRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR_TAIL_FISH.get(),
                cn.kurt6.unknown_echoes.client.render.MirrorTailFishRenderer::new);
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
    }
}
