package cn.kurt6.unknown_echoes.mixin;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 熔炼分栈需要保留的物品实体状态；原生 copy 不携带这些字段。 */
@Mixin(ItemEntity.class)
public interface ItemDropAccessor {
    @Accessor("pickupDelay")
    int unknownEchoes$getPickupDelay();

    @Nullable
    @Accessor("thrower")
    UUID unknownEchoes$getThrower();

    @Accessor("thrower")
    void unknownEchoes$setThrower(@Nullable UUID thrower);

    @Accessor("health")
    int unknownEchoes$getHealth();

    @Accessor("health")
    void unknownEchoes$setHealth(int health);
}
