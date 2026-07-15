package com.starmao.scannable.api;

/**
 * Texture constants for the Scannable mod.
 * In NeoForge 26.1.2+, ResourceLocation has been moved or refactored.
 * This class maintains API compatibility using String-based identifiers.
 */
public final class ModTextures {
    private static final String MOD_ID = "scannable_unofficial";

    // Use String identifiers instead of ResourceLocation for compatibility
    public static final String ICON_INFO = MOD_ID + ":textures/gui/overlay/info.png";
    public static final String ICON_WARNING = MOD_ID + ":textures/gui/overlay/warning.png";
    public static final String SCANNER_PROGRESS = MOD_ID + ":textures/gui/overlay/scanner_progress.png";

    private ModTextures() {
    }
}
