package com.starmao.scannable.client.renderer;

/**
 * Placeholder for the 1.21.1 {@code ScanRenderBuffers} helper.
 *
 * <p>26.1 replaced the buffer-source based rendering pipeline with the new
 * {@code SubmitNodeCollector} pipeline, so {@code MultiBufferSource} based helpers do not
 * apply to this version. The scanner's hand-depth pass is itself a stub here (see
 * {@link HandDepthRenderer}), so nothing on 26.1.2 needs an item-safe buffer source.
 *
 * <p>This class exists only to shadow the 1.21.1 implementation while the version source
 * sets are merged; it must not be removed, or the 1.21.1 implementation would be compiled
 * against 26.1 APIs and fail.
 */
public final class ScanRenderBuffers {

    private ScanRenderBuffers() {}
}
