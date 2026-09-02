package cn.kurt6.unknown_echoes.recipe;

import cn.kurt6.unknown_echoes.registry.ModRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/** 无序装备升级配方。保留基底装备的玩家组件，同时沿用目标物品的固有组件。 */
public final class ComponentPreservingShapelessRecipe implements CraftingRecipe {
    private static final int MAX_ADDITIONS = 8;

    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient base;
    private final List<Ingredient> additions;
    private final NonNullList<Ingredient> allIngredients;
    private final ItemStack result;

    public ComponentPreservingShapelessRecipe(String group, CraftingBookCategory category,
                                              Ingredient base, List<Ingredient> additions,
                                              ItemStack result) {
        this.group = group;
        this.category = category;
        this.base = base;
        this.additions = List.copyOf(additions);
        this.allIngredients = NonNullList.create();
        this.allIngredients.add(base);
        this.allIngredients.addAll(additions);
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != this.allIngredients.size()) {
            return false;
        }
        List<ItemStack> supplied = new ArrayList<>(input.ingredientCount());
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                supplied.add(stack);
            }
        }
        return RecipeMatcher.findMatches(supplied, this.allIngredients) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (this.base.test(stack)) {
                ItemStack upgraded = stack.transmuteCopy(this.result.getItem(), this.result.getCount());
                upgraded.applyComponents(this.result.getComponentsPatch());
                preserveDurabilityRatio(stack, upgraded);
                return upgraded;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void preserveDurabilityRatio(ItemStack base, ItemStack upgraded) {
        if (!base.has(DataComponents.MAX_DAMAGE) || !base.has(DataComponents.DAMAGE)
                || !upgraded.has(DataComponents.MAX_DAMAGE) || !upgraded.has(DataComponents.DAMAGE)) {
            return;
        }
        int baseMaxDamage = base.getMaxDamage();
        int upgradedMaxDamage = upgraded.getMaxDamage();
        if (baseMaxDamage <= 0 || upgradedMaxDamage <= 0) {
            return;
        }
        double damageRatio = (double) base.getDamageValue() / baseMaxDamage;
        int upgradedDamage = (int) Math.round(damageRatio * upgradedMaxDamage);
        upgraded.setDamageValue(Math.clamp(upgradedDamage, 0, upgradedMaxDamage - 1));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.allIngredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.allIngredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.COMPONENT_PRESERVING_SHAPELESS.get();
    }

    private static DataResult<List<Ingredient>> validateAdditions(List<Ingredient> additions) {
        if (additions.isEmpty()) {
            return DataResult.error(() -> "Component-preserving recipe requires at least one addition");
        }
        if (additions.size() > MAX_ADDITIONS) {
            return DataResult.error(() -> "Too many additions for component-preserving recipe; maximum is "
                    + MAX_ADDITIONS);
        }
        return DataResult.success(additions);
    }

    public static final class Serializer implements RecipeSerializer<ComponentPreservingShapelessRecipe> {
        private static final MapCodec<ComponentPreservingShapelessRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "")
                                .forGetter(recipe -> recipe.group),
                        CraftingBookCategory.CODEC.fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(recipe -> recipe.category),
                        Ingredient.CODEC_NONEMPTY.fieldOf("base")
                                .forGetter(recipe -> recipe.base),
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                                .flatXmap(ComponentPreservingShapelessRecipe::validateAdditions,
                                        DataResult::success)
                                .forGetter(recipe -> recipe.additions),
                        ItemStack.STRICT_CODEC.fieldOf("result")
                                .forGetter(recipe -> recipe.result)
                ).apply(instance, ComponentPreservingShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ComponentPreservingShapelessRecipe>
                STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<ComponentPreservingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ComponentPreservingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ComponentPreservingShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int additionCount = buffer.readVarInt();
            if (additionCount < 1 || additionCount > MAX_ADDITIONS) {
                throw new IllegalArgumentException(
                        "Invalid component-preserving recipe addition count: " + additionCount);
            }
            List<Ingredient> additions = new ArrayList<>(additionCount);
            for (int i = 0; i < additionCount; i++) {
                additions.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new ComponentPreservingShapelessRecipe(group, category, base, additions, result);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer,
                                      ComponentPreservingShapelessRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.base);
            buffer.writeVarInt(recipe.additions.size());
            for (Ingredient addition : recipe.additions) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, addition);
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }
}
