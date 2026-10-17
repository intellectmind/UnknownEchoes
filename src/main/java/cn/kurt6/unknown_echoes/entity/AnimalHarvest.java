package cn.kurt6.unknown_echoes.entity;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class AnimalHarvest {
    private AnimalHarvest() {
    }

    public static boolean canInteract(Level level, Animal animal, Player player) {
        return animal.level() == level && player.level() == level
                && animal.isAlive() && player.isAlive() && !player.isSpectator()
                && player.canInteractWithEntity(animal, 1.0D);
    }

    public static boolean canHarvest(Level level, Animal animal, Player player) {
        return !animal.isBaby() && canInteract(level, animal, player);
    }
}
