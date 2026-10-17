package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.particle.ParticleEnchantingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, UnknownEchoes.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ParticleEnchantingMenu>> PARTICLE_ENCHANTING =
            MENUS.register("particle_enchanting", () -> IMenuTypeExtension.create(
                    (id, inventory, buf) -> new ParticleEnchantingMenu(id, inventory)));

    private ModMenus() { }
}
