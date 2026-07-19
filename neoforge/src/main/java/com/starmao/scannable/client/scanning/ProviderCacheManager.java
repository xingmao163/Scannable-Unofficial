package com.starmao.scannable.client.scanning;

/**
 * Utility that resets all scan result provider caches, typically triggered
 * on config reload so that colour mappings and filter caches are re-baked.
 * <p>Only a single {@link #clearCache()} method — all logic is delegated
 * to the individual provider reset methods.
 */
public final class ProviderCacheManager {
