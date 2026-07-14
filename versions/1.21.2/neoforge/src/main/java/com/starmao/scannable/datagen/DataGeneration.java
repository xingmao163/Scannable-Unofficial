package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for all data generation (item models, recipes, tags, languages).
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
        final var existingFileHelper = event.getExistingFileHelper();

        // -- Server-side providers --
        generator.addProvider(event.includeServer(), new ModRecipeProvider.Runner(output, lookupProvider));
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(output, lookupProvider, existingFileHelper));

        // -- Client-side providers --
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output, Scannable.MOD_ID, "en_us"));
        generator.addProvider(event.includeClient(),
                new ModChineseLanguageProvider(output, Scannable.MOD_ID, "zh_cn"));
    }

    private DataGeneration() {
    }
}
