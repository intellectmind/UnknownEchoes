package cn.kurt6.unknown_echoes.entity.mob;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 保留原版鱼群行为，水桶交互不进入 AbstractFish 的捕捉入口。 */
public abstract class EchoSchoolingFish extends AbstractSchoolingFish {
    protected EchoSchoolingFish(EntityType<? extends AbstractSchoolingFish> type, Level level) {
        super(type, level);
    }

    @Override
    protected final InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public final ItemStack getBucketItemStack() {
        return ItemStack.EMPTY;
    }
}
