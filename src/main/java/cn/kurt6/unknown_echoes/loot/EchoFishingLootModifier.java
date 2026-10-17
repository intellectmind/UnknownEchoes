package cn.kurt6.unknown_echoes.loot;

import cn.kurt6.unknown_echoes.ability.EchoPermission;
import cn.kurt6.unknown_echoes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EchoFishingLootModifier extends LootModifier {
    public static final MapCodec<EchoFishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).and(ResourceLocation.CODEC.fieldOf("bonus_table")
                    .forGetter(modifier -> modifier.bonusTable)).apply(instance, EchoFishingLootModifier::new));

    private final ResourceLocation bonusTable;

    public EchoFishingLootModifier(LootItemCondition[] conditions, ResourceLocation bonusTable) {
        super(conditions);
        this.bonusTable = bonusTable;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (!context.getQueriedLootTableId().equals(BuiltInLootTables.FISHING.location())
                || !(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)
                || !(hook.getPlayerOwner() instanceof ServerPlayer)
                || tool == null || !tool.is(ModItems.ECHO_FISHING_ROD.get())
                || !context.getLevel().dimension().location().equals(EchoPermission.ECHO_REALM_ID)
                || generatedLoot.stream().noneMatch(stack -> !stack.isEmpty())) {
            return generatedLoot;
        }
        LootTable table = context.getLevel().getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, this.bonusTable));
        // 补充表共享本次收杆上下文，不可再次触发全局修改器或提前向背包发奖。
        table.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), stack -> {
            if (!stack.isEmpty()) {
                generatedLoot.add(stack);
            }
        }));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
