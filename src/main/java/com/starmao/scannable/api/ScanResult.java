package com.starmao.scannable.api;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.Closeable;

/**
 * Represents a single scan result — a detected object in the world.
 * <p>Each result carries the position of the detected object and optionally
 * a bounding box for rendering a highlight around it. Implementations are
 * {@link AutoCloseable} so that transient resources (e.g. client-side geometry)
 * can be released when the result expires.
 *
 * @see ScanResultProvider#collectScanResults
 */
public interface ScanResult extends Closeable {

    /** @return the world position of the detected object */
    Vec3 getPosition();

    /**
     * The bounding box used for in-world highlight rendering.
     *
     * @return a non-null AABB if the object should render a highlight box,
     *         or {@code null} to skip box rendering entirely
     */
    @Nullable
    AABB getRenderBounds();

    /**
     * Releases any transient resources held by this result.
     * <p>Default implementation is a no-op; override to clean up
     * client-side allocations when the scan result expires.
     */
    @Override
    default void close() {
    }
}
