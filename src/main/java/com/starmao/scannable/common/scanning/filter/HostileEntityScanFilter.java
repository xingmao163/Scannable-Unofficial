package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;

import java.util.function.Predicate;

public enum HostileEntityScanFilter implements Predicate<Entity> {
    INSTANCE;

    @Override
    public boolean test(Entity entity) {
        return entity instanceof Monster || entity instanceof Enemy;
    }
}
