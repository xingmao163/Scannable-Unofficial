package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ItemModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableItemScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Screen for configuring an item scanner module's target items.
 * <p>Displays the configured item list and allows adding new items
 * by clicking an empty slot with the desired item in the player's cursor.
 */
public class ConfigurableItemScannerModuleContainerScreen
