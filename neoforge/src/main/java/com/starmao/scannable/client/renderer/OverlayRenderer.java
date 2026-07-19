package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.common.config.Strings;
import com.starmao.scannable.common.item.ScannerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a second-screen progress indicator overlay while the player is charging a scan.
 * <p>Displays a pie-chart-style progress arc and a percentage label at the centre
 * of the screen when the player holds right-click with a scanner. Only renders
 * when the held item is a {@link com.starmao.scannable.common.item.ScannerItem}.
 */
public final class OverlayRenderer {
