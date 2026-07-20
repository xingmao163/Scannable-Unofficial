package com.starmao.scannable.api;

import com.starmao.scannable.Scannable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe registry for scan result providers.
 *
 * <p>Scanning modules in the {@code common} package need access to
 * {@link ScanResultProvider} instances without importing client-only
 * classes. This registry bridges the gap — client code populates it
 * during initialization, and modules read from it safely on both sides.
 *
 * <h3>For third-party addons</h3>
 * <p>To register a custom scan result provider:
 * <pre>{@code
 * // Wrap the provider in a Supplier to prevent classloading on dedicated servers:
 * ScanResultProviderRegistry.register("mymod:custom", () -> new MyScanResultProvider());
 *
 * // Check before registering to avoid conflicts:
 * if (!ScanResultProviderRegistry.isRegistered("mymod:custom")) {
 *     ScanResultProviderRegistry.register("mymod:custom", () -> new MyScanResultProvider());
 * }
 * }</pre>
 * <p>The provider class ({@code MyScanResultProvider}) MUST implement
 * {@link ScanResultProvider} and be kept in client-side code
 * (annotated with {@link net.neoforged.api.distmarker.OnlyIn @OnlyIn(Dist.CLIENT)}).
 *
 * <p>Existing providers can be discovered via {@link #getProviderNames()} and {@link #getAll()}.
 * Built-in provider names are {@value #BLOCKS}, {@value #ENTITIES}, {@value #ITEMS}.
 *
 * @see ScanResultProvider
 */
public final class ScanResultProviderRegistry {

    // ---- Well-known provider names ---- //

    /** Provider name for block scan results. */
    public static final String BLOCKS = "blocks";
    /** Provider name for entity scan results. */
    public static final String ENTITIES = "entities";
    /** Provider name for item scan results (server-driven). */
    public static final String ITEMS = "items";
    private static final Map<String, Supplier<ScanResultProvider>> PROVIDERS = new ConcurrentHashMap<>();

    /**
     * Register a provider {@code Supplier} so it can be looked up by name.
     * <p>The supplier pattern avoids classloading the provider on a dedicated
     * server where the provider's implementation classes may not exist.
     * <p>If a provider with the same name is already registered, a warning is
     * logged and the new entry <em>replaces</em> the old one.
     *
     * @param name     the provider name (use a namespaced name like {@code "mymod:custom"}
     *                 for third-party addons to avoid collisions)
     * @param provider a supplier that creates or returns the provider instance;
     *                 should return {@code null} if called on a dedicated server
     */
    public static void register(final String name, final Supplier<ScanResultProvider> provider) {
        final Supplier<ScanResultProvider> previous = PROVIDERS.put(name, provider);
        if (previous != null) {
            Scannable.LOGGER.warn("[ScanResultProviderRegistry] Replaced existing provider for '{}'. " +
                    "If you are a third-party addon, use a namespaced name to avoid conflicts.", name);
        }
    }

    /**
     * Check whether a provider with the given name is already registered.
     *
     * @param name the provider name to check
     * @return {@code true} if a provider is registered under that name
     */
    public static boolean isRegistered(final String name) {
        return PROVIDERS.containsKey(name);
    }

    /**
     * Get a registered provider by name.
     *
     * @param name the provider name
     * @return the provider instance, or {@code null} if the name is unknown or
     *         if called on a dedicated server where the supplier may be absent
     */
    public static ScanResultProvider get(final String name) {
        final Supplier<ScanResultProvider> supplier = PROVIDERS.get(name);
        return supplier != null ? supplier.get() : null;
    }

    /**
     * {@return the set of all registered provider names, for third-party discovery}
     */
    public static Set<String> getProviderNames() {
        return Collections.unmodifiableSet(PROVIDERS.keySet());
    }

    /**
     * {@return an unmodifiable view of all registered name-to-provider mappings,
     * for third-party discovery}
     */
    public static Map<String, ScanResultProvider> getAll() {
        final Map<String, ScanResultProvider> snapshot = new HashMap<>();
        for (final Map.Entry<String, Supplier<ScanResultProvider>> entry : PROVIDERS.entrySet()) {
            final ScanResultProvider provider = entry.getValue().get();
            if (provider != null) {
                snapshot.put(entry.getKey(), provider);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private ScanResultProviderRegistry() {}
}
