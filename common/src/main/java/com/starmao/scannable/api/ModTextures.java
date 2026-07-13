package com.starmao.scannable.api;

import net.minecraft.resources.ResourceLocation;

public final class ModTextures {
    private static final String MOD_ID = "scannable_unofficial";

    public static final ResourceLocation ICON_INFO = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/info.png");
    public static final ResourceLocation ICON_WARNING = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/warning.png");
    public static final ResourceLocation SCANNER_PROGRESS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/scanner_progress.png");

    private ModTextures() {
    }
}
