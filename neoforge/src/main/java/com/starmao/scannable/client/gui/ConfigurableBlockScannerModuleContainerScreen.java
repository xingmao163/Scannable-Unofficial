package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.BlockModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableBlockScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Screen for configuring a block scanner module's target blocks.
 * <p>Displays the configured block list and allows adding new blocks
 * by clicking an empty slot with a block item in the player's cursor.
 */
public class ConfigurableBlockScannerModuleContainerScreen
