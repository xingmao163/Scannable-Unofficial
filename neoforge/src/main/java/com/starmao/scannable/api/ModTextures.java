package com.starmao.scannable.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Central registry of texture paths used by the scanner GUI and overlay rendering.
 * <p>All textures are loaded from {@code assets/scannable_unofficial/textures/gui/}.
 * This is a utility class with a private constructor — never instantiated.
 */
public final class ModTextures {
    private static final String MOD_ID = "scannable_unofficial";

    /** Icon displayed for informational scan results. */
    public static final ResourceLocation ICON_INFO = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/info.png");
    /** Icon displayed for warning-level scan results. */
    public static final ResourceLocation ICON_WARNING = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/warning.png");
    /** Texture used to render the scanner charge / progress indicator overlay. */
    public static final ResourceLocation SCANNER_PROGRESS = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/overlay/scanner_progress.png");

    private ModTextures() {
    }
}
