package com.starmao.scannable.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for all data generation (item models, recipes, tags).
 *
 * <p>Registered as a mod event listener from the main mod class.
 * Triggered by the {@code runData} Gradle task. Output is written to
 * {@code src/generated/resources/} — files there are auto-generated and should
 * not be hand-edited. Existing hand-written resources under {@code src/main/resources/}
 * take precedence as they are passed via {@code --existing}.
 */
public final class DataGeneration {

    public static void onGatherData(final GatherDataEvent event) {
        final var generator = event.getGenerator();
        final var output = generator.getPackOutput();
        final var lookupProvider = event.getLookupProvider();

        // -- Server-side providers --
        generator.addProvider(event.includeServer(), new ModRecipeProvider.Runner(output, lookupProvider));
    }

    private DataGeneration() {
    }
}
