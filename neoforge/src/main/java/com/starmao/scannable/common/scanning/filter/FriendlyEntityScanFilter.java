package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/** Matches friendly entities (animals, villagers) based on their mob category. */
public enum FriendlyEntityScanFilter implements Predicate<Entity> {
    INSTANCE;

    @Override
    public boolean test(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof Player)
                && entity.getType().getCategory().isFriendly();
    }
}
