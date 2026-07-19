package com.starmao.scannable.common.scanning.filter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.TamableAnimal;

import java.util.function.Predicate;

/**
 * Scan filter singleton that matches friendly / passive entities.
 * <p>An entity is considered friendly if it is an instance of
 * {@link Animal}, {@link TamableAnimal}, or {@link AbstractVillager}.
 * Used by the friendly entity scanner module.
 */
public enum FriendlyEntityScanFilter implements Predicate<Entity> {
