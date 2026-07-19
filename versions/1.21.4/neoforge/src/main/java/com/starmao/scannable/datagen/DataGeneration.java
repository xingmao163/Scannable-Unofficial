package com.starmao.scannable.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class DataGeneration {
    public static void onGatherDataServer(final GatherDataEvent.Server event) {
        var output = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();
        event.addProvider(new ModRecipeProvider.Runner(output, lookupProvider));
    }



    private DataGeneration() {
    }
}
