package cn.kurt6.unknown_echoes.particle;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.advancement.ModAdvancements;
import cn.kurt6.unknown_echoes.registry.ModBlocks;
import cn.kurt6.unknown_echoes.registry.ModDataComponents;
import cn.kurt6.unknown_echoes.registry.ModItems;
import cn.kurt6.unknown_echoes.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParticleEnchantingMenu extends AbstractContainerMenu {
    private final SimpleContainer station = new SimpleContainer(2);
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final DataSlot result = DataSlot.standalone();
    private int lastActionTick;
    private boolean hasAction;

    public ParticleEnchantingMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL); }

    public ParticleEnchantingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.PARTICLE_ENCHANTING.get(), id);
        this.access = access;
        this.inventory = inventory;
        addSlot(new Slot(station, 0, ParticleEnchantingLayout.WEAPON_X, ParticleEnchantingLayout.WEAPON_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return ParticleRules.eligible(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(station, 1, ParticleEnchantingLayout.BOTTLE_X, ParticleEnchantingLayout.BOTTLE_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, ParticleEnchantingLayout.INVENTORY_X + col * 18, ParticleEnchantingLayout.INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, ParticleEnchantingLayout.INVENTORY_X + col * 18, ParticleEnchantingLayout.HOTBAR_Y));
        addDataSlot(result);
    }

    public ItemStack weapon() { return station.getItem(0); }
    public int result() { return result.get(); }

    public Map<Item, Integer> costs(int action) {
        ParticleData data = ParticleRules.data(weapon());
        Map<Item, Integer> costs = new LinkedHashMap<>();
        if (action == 0 && data.tier() < 5) {
            for (var cost : ParticleRules.upgradeCost(data.tier() + 1))
                costs.put(BuiltInRegistries.ITEM.get(UnknownEchoes.id(cost.item())), cost.count());
        } else if (action >= 1 && action <= 16) {
            costs.put(action > 8 ? ModItems.PARTICLE_CORE.get() : ParticleKind.values()[action - 1].material(), 1);
        } else if (action >= 17 && action <= 25) costs.put(ModItems.PURIFYING_VIAL.get(), 1);
        return costs;
    }

    public int count(Item item) {
        int count = 0;
        for (ItemStack stack : inventory.items) if (stack.is(item)) count += stack.getCount();
        for (ItemStack stack : inventory.offhand) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    public static int experience(Player player) {
        return (int) Math.min(Integer.MAX_VALUE, (long) ParticleRules.experienceAtLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    public int rejection(Player player, int action) {
        if (!ParticleRules.eligible(weapon()) || action < 0 || action > 25) return 1;
        ParticleData data = ParticleRules.data(weapon());
        if (action == 0) {
            if (data.tier() == 5) return 2;
            if (player.experienceLevel < ParticleRules.level(data.tier() + 1)) return 3;
        } else if (action <= 16) {
            if (data.total() >= data.capacity()) return 2;
            int count = data.count(ParticleKind.values()[(action - 1) % 8]);
            if (experience(player) < ParticleRules.injectionCost(count)) return 4;
        } else {
            if (action == 25 ? data.total() == 0 : data.count(ParticleKind.values()[action - 17]) == 0) return 5;
            ItemStack bottles = station.getItem(1);
            if (!bottles.isEmpty() && (!bottles.is(Items.GLASS_BOTTLE) || bottles.getCount() >= bottles.getMaxStackSize())) return 6;
        }
        for (var cost : costs(action).entrySet()) if (count(cost.getKey()) < cost.getValue()) return 7;
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (!(player instanceof ServerPlayer server) || player != inventory.player || player.containerMenu != this || !stillValid(player)) return false;
        // 请求间隔按服务器处理 tick 计；无符号差值允许 int 计数器回绕。
        int now = server.server.getTickCount();
        if (hasAction && Integer.toUnsignedLong(now - lastActionTick) < 4L) return false;
        lastActionTick = now;
        hasAction = true;
        int rejection = rejection(player, action);
        result.set(rejection);
        if (rejection != 0) { broadcastChanges(); return false; }
        ParticleData data = ParticleRules.data(weapon());
        ParticleOperation operation = ParticleOperation.plan(data, action, server.getRandom().nextInt(100));
        ItemStack original = weapon(), snapshot = original.copy();
        ParticleExperience.Change experience = ParticleExperience.prepare(server, operation.experienceDelta());
        if (experience == null || !experience.approve(server) || player.containerMenu != this || !stillValid(player)
                || weapon() != original || !ItemStack.isSameItemSameComponents(weapon(), snapshot) || !experience.unchanged(server)) {
            if (player.containerMenu == this && stillValid(player)) { result.set(10); broadcastChanges(); }
            return false;
        }
        rejection = rejection(player, action);
        if (rejection != 0) { result.set(rejection); broadcastChanges(); return false; }
        for (var cost : costs(action).entrySet()) consume(cost.getKey(), cost.getValue());
        experience.apply(server);
        weapon().set(ModDataComponents.PARTICLES.get(), operation.result());
        if (operation.returnsBottle()) {
            if (station.getItem(1).isEmpty()) station.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
            else station.getItem(1).grow(1);
        }
        result.set(operation.success() ? 8 : 9);
        station.setChanged();
        inventory.setChanged();
        if (operation.success()) ModAdvancements.checkParticleAdvancements(server, weapon());
        if (player.containerMenu != this || !stillValid(player)) return true;
        access.execute((level, pos) -> ParticleFx.operation(server, pos, operation.result(), action, operation.success()));
        broadcastChanges();
        return true;
    }

    private void consume(Item item, int count) {
        for (var group : java.util.List.of(inventory.items, inventory.offhand)) for (ItemStack stack : group) {
            if (!stack.is(item)) continue;
            int taken = Math.min(count, stack.getCount());
            stack.shrink(taken);
            count -= taken;
            if (count == 0) return;
        }
        throw new IllegalStateException("Particle payment changed during transaction");
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isRemoved()
                && (!(player instanceof ServerPlayer server) || !server.hasDisconnected())
                && access.evaluate((level, pos) -> player.level() == level && level.hasChunkAt(pos)
                && player.distanceToSqr(pos.getCenter()) <= 64
                && level.getBlockState(pos).is(ModBlocks.PARTICLE_ENCHANTING_TABLE.get()), player.level().isClientSide);
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, station);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ParticleRules.eligible(stack) && !slots.getFirst().hasItem()) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
