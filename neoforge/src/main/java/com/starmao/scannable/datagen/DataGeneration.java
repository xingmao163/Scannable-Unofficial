package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for automatic data generation (recipes, models, tags, languages).
 * <p>Registered on the mod event bus. Run via {@code ./gradlew :neoforge:runData}.
 *
 * @see ModRecipeProvider
 */
public final class DataGeneration {
    public static void initialize(final IEventBus modEventBus) {
        modEventBus.addListener(DataGeneration::onGatherData);
    }

    private static void onGatherData(final GatherDataEvent event) {
        final var generator = event.getGenerator();
        final var output = generator.getPackOutput();
        final var lookupProvider = event.getLookupProvider();
        final var existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookupProvider));
    }

    private DataGeneration() {}
}
