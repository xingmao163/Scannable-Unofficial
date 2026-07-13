package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.TamableAnimal;

import java.util.function.Predicate;

public enum FriendlyEntityScanFilter implements Predicate<Entity> {
    INSTANCE;

    @Override
    public boolean test(Entity entity) {
        return entity instanceof Animal
                || entity instanceof TamableAnimal
                || entity instanceof AbstractVillager;
    }
}
