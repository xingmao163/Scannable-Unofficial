package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side registry and holder for scan result provider instances.
 * <p>Populated during client setup — creates singleton providers for blocks,
 * entities, and items, registers them locally and in the cross-platform
 * {@link ScanResultProviderRegistry} so common modules can access them
 * without client-only class imports.
 */
public final class ScanResultProviders {
    private static final Map<String, ScanResultProvider> PROVIDERS = new HashMap<>();

    public static final ProviderHolder<ScanResultProviderBlock> BLOCKS = new ProviderHolder<>("blocks", new ScanResultProviderBlock());
    public static final ProviderHolder<ScanResultProviderEntity> ENTITIES = new ProviderHolder<>("entities", new ScanResultProviderEntity());
    public static final ProviderHolder<ScanResultProviderItem> ITEMS = new ProviderHolder<>("items", new ScanResultProviderItem());

    public static void initialize() {
        PROVIDERS.put(ScanResultProviderRegistry.BLOCKS, BLOCKS.get());
        PROVIDERS.put(ScanResultProviderRegistry.ENTITIES, ENTITIES.get());
        PROVIDERS.put(ScanResultProviderRegistry.ITEMS, ITEMS.get());

        // Populate the cross-platform registry so common scanning modules
        // can access providers without importing client-only classes.
        ScanResultProviderRegistry.register(ScanResultProviderRegistry.BLOCKS, BLOCKS::get);
        ScanResultProviderRegistry.register(ScanResultProviderRegistry.ENTITIES, ENTITIES::get);
        ScanResultProviderRegistry.register(ScanResultProviderRegistry.ITEMS, ITEMS::get);
    }

    public static ScanResultProvider get(String name) {
        return PROVIDERS.get(name);
    }

    public static final class ProviderHolder<T extends ScanResultProvider> {
        private final T provider;

        private ProviderHolder(String name, T provider) {
            this.provider = provider;
        }

        public T get() {
            return provider;
        }
    }

    private ScanResultProviders() {
    }
}
