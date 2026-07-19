package com.starmao.scannable.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for all data generation (item models, recipes, tags).
 *
 * <p>Registered as a mod event listener from the main mod class.
 * Triggered by the {@code runData} Gradle task. Output is written to
 * {@code src/generated/resources/} — files there are auto-generated and should
 * not be hand-edited.
 *
 * <p>In 26.1.2, data generation uses event subclasses {@link GatherDataEvent.Server}
 * and {@link GatherDataEvent.Client} with the {@code createProvider(Factory)} API.
 */
public final class DataGeneration {

    public static void gatherServerData(final GatherDataEvent.Server event) {
        event.createProvider(ModRecipeProvider.Runner::new);
    }



    private DataGeneration() {
    }
}
