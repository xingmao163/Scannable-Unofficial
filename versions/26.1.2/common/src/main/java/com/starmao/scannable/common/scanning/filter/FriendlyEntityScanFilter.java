package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;

import java.util.function.Predicate;

/**
 * Friendly entity filter that matches animals and tamable entities.
 * In NeoForge 26.1.2+, AbstractVillager class has been removed.
 */
public enum FriendlyEntityScanFilter implements Predicate<Entity> {
    INSTANCE;

    @Override
    public boolean test(Entity entity) {
        // AbstractVillager removed in 26.1.2; keep Animal and TamableAnimal checks
        return entity instanceof Animal
                || entity instanceof TamableAnimal;
    }
}
