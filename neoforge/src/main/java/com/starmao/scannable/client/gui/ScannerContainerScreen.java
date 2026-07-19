package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.ScannerContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The main scanner inventory screen, showing active and inactive module slots.
 * <p>Uses a small custom GUI with 3 active module slots and 6 inventory slots.
 * Prevents the scanner item itself from being moved into its own slots.
 */
public class ScannerContainerScreen extends AbstractContainerScreen<ScannerContainerMenu> {
