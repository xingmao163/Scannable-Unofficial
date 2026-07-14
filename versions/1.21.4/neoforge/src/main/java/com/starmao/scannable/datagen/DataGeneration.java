package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class DataGeneration {

    public static void onGatherDataServer(final GatherDataEvent.Server event) {
        var output = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();
        event.addProvider(new ModRecipeProvider.Runner(output, lookupProvider));
        event.addProvider(new ModItemTagProvider(output, lookupProvider));
    }

    public static void onGatherDataClient(final GatherDataEvent.Client event) {
        var output = event.getGenerator().getPackOutput();
        event.addProvider(new ModItemModelProvider(output));
        event.addProvider(new ModLanguageProvider(output, Scannable.MOD_ID, "en_us"));
        event.addProvider(new ModChineseLanguageProvider(output, Scannable.MOD_ID, "zh_cn"));
    }

    private DataGeneration() {
    }
}
