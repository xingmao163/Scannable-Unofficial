package com.starmao.scannable.client.scanning;

import com.starmao.scannable.api.API;
import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScanResultProviderRegistry;
import com.starmao.scannable.client.scanning.ScanResultProviderItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for scan result providers.
 */
public final class ScanResultProviders {
    private static final Map<String, ScanResultProvider> PROVIDERS = new HashMap<>();

    public static final ProviderHolder<ScanResultProviderBlock> BLOCKS = new ProviderHolder<>("blocks", new ScanResultProviderBlock());
    public static final ProviderHolder<ScanResultProviderEntity> ENTITIES = new ProviderHolder<>("entities", new ScanResultProviderEntity());
    public static final ProviderHolder<ScanResultProviderItem> ITEMS = new ProviderHolder<>("items", new ScanResultProviderItem());

    public static void initialize() {
        PROVIDERS.put("blocks", BLOCKS.get());
        PROVIDERS.put("entities", ENTITIES.get());
        PROVIDERS.put("items", ITEMS.get());

        // Populate the cross-platform registry so common scanning modules
        // can access providers without importing client-only classes.
        ScanResultProviderRegistry.register("blocks", BLOCKS::get);
        ScanResultProviderRegistry.register("entities", ENTITIES::get);
        ScanResultProviderRegistry.register("items", ITEMS::get);
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
