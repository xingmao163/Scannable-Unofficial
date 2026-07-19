package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;

import java.util.function.Predicate;

/**
 * Scan filter singleton that matches hostile / enemy entities.
 * <p>An entity is considered hostile if it is an instance of
 * {@link Monster} or {@link Enemy}. Used by the hostile entity scanner module.
 */
public enum HostileEntityScanFilter implements Predicate<Entity> {
