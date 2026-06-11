package com.starmao.scannable.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server-safe registry for scan result providers.
 *
 * <p>Scanning modules in the {@code common} package need access to
 * {@link ScanResultProvider} instances without importing client-only
 * classes. This registry bridges the gap — client code populates it
 * during initialization, and modules read from it safely on both sides.
 */
public final class ScanResultProviderRegistry {
    private static final Map<String, Supplier<ScanResultProvider>> PROVIDERS = new HashMap<>();

    /**
     * Register a provider {@code Supplier} so it can be looked up by name.
     * The supplier pattern avoids classloading the provider on a dedicated
     * server where the provider's implementation classes may not exist.
     */
    public static void register(final String name, final Supplier<ScanResultProvider> provider) {
        PROVIDERS.put(name, provider);
    }

    /**
     * Get a registered provider. Returns {@code null} if the name is unknown
     * or if called on a dedicated server where the supplier may be absent.
     */
    public static ScanResultProvider get(final String name) {
        final Supplier<ScanResultProvider> supplier = PROVIDERS.get(name);
        return supplier != null ? supplier.get() : null;
    }

    private ScanResultProviderRegistry() {}
}
