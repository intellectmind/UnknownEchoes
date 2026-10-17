package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

public final class ParticleRepair {
    private ParticleRepair() { }

    public static boolean conflicts(ItemStack first, ItemStack second) {
        return first.is(second.getItem()) && ParticleRules.data(first).tier() > 0
                && ParticleRules.data(second).tier() > 0;
    }

    public static ItemStack preserve(ItemStack result, ItemStack first, ItemStack second) {
        if (conflicts(first, second)) return ItemStack.EMPTY;
        if (!ParticleRules.eligible(result)) return result;
        ParticleData data = ParticleRules.data(first);
        if (data.tier() == 0) {
            if (!first.isEmpty() && !first.is(second.getItem())) return result;
            data = ParticleRules.data(second);
            if (!result.is(second.getItem())) return result;
        } else if (!result.is(first.getItem())) return result;
        if (data.tier() == 0 || ParticleRules.data(result).equals(data)) return result;
        // 事件提供的结果可能直接引用输入，写组件前必须复制。
        ItemStack preserved = result.copy();
        preserved.set(ModDataComponents.PARTICLES.get(), data);
        return preserved;
    }

    public static ItemStack craftingResult(ItemStack result, CraftingInput input) {
        ItemStack first = ItemStack.EMPTY, second = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (first.isEmpty()) first = stack;
            else if (second.isEmpty()) second = stack;
            else return result;
        }
        return preserve(result, first, second);
    }
}
