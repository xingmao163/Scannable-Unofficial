package com.starmao.scannable.api;

/**
 * Identifies the rendering context in which scan results are being drawn.
 * <p>Used by {@link ScanResultProvider#render} to adapt rendering behaviour
 * (e.g. world-space markers vs. GUI overlay elements).
 */
public enum ScanResultRenderContext {
    /** Results are rendered in 3D world space, typically as coloured boxes or icons over blocks/entities. */
    WORLD,
    /** Results are rendered in screen-space, e.g. as icons within a GUI overlay. */
    GUI
}
