package com.starmao.scannable.common.util;

import com.starmao.scannable.api.ClientScanHandler;

import javax.annotation.Nullable;

/**
 * Thread-safe accessor for the client-side scan handler.
 * <p>
 * On a dedicated server the handler is never set and remains {@code null},
 * so common code can safely call {@link #getHandler()} and null-check
 * without loading any client-only classes.
 */
public final class ClientAccessor {

    @Nullable
    private static volatile ClientScanHandler handler;

    private ClientAccessor() {
    }

    /**
     * Set the client handler (called during client-side initialisation only).
     */
    public static void setHandler(@Nullable ClientScanHandler handler) {
        ClientAccessor.handler = handler;
    }

    /**
     * Get the current handler, or {@code null} on a dedicated server.
     */
    @Nullable
    public static ClientScanHandler getHandler() {
        return handler;
    }
}
