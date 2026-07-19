package com.starmao.scannable.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.starmao.scannable.common.container.EntityModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableEntityScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen for configuring an entity scanner module's target entities.
 * <p>Displays the configured entity type list with an in-GUI entity preview
 * rendered using the entity's model. Entities are configured via spawn eggs
 * or JEI ghost-drag.
 */
public class ConfigurableEntityScannerModuleContainerScreen
