package cn.kurt6.unknown_echoes.registry;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.recipe.ComponentPreservingShapelessRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, UnknownEchoes.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, ComponentPreservingShapelessRecipe.Serializer>
            COMPONENT_PRESERVING_SHAPELESS = RECIPE_SERIALIZERS.register(
                    "component_preserving_shapeless",
                    ComponentPreservingShapelessRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }
}
