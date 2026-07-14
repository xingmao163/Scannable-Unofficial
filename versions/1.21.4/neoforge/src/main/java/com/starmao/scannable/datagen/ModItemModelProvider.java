package com.starmao.scannable.datagen;

import com.google.gson.JsonObject;
import com.starmao.scannable.Scannable;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ModItemModelProvider implements DataProvider {
    private final PackOutput output;

    private static final String[] MODULE_ITEMS = {
            "blank_module",
            "range_module",
            "fluid_module",
            "friendly_entity_module",
            "hostile_entity_module",
            "entity_module",
            "block_module",
            "item_module",
    };

    public ModItemModelProvider(final PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        var assetRoot = output.getOutputFolder().resolve("assets/" + Scannable.MOD_ID);
        var modelsPath = assetRoot.resolve("models/item/");
        var itemsPath = assetRoot.resolve("items/");

        var futures = new ArrayList<CompletableFuture<?>>();

        // Scanner (single layer)
        {
            var json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            var textures = new JsonObject();
            textures.addProperty("layer0", Scannable.MOD_ID + ":item/scanner");
            json.add("textures", textures);
            futures.add(DataProvider.saveStable(cache, json, modelsPath.resolve("scanner.json")));
            futures.add(DataProvider.saveStable(cache, makeItemRef("scannable_unofficial:item/scanner"), itemsPath.resolve("scanner.json")));
        }

        // Modules (layered: blank + slot + icon)
        for (final String item : MODULE_ITEMS) {
            var json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            var textures = new JsonObject();
            textures.addProperty("layer0", Scannable.MOD_ID + ":item/blank_module");
            textures.addProperty("layer1", Scannable.MOD_ID + ":item/module_slot");
            textures.addProperty("layer2", Scannable.MOD_ID + ":item/" + item);
            json.add("textures", textures);
            futures.add(DataProvider.saveStable(cache, json, modelsPath.resolve(item + ".json")));
            futures.add(DataProvider.saveStable(cache, makeItemRef(Scannable.MOD_ID + ":item/" + item), itemsPath.resolve(item + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
    }

    private static JsonObject makeItemRef(final String modelId) {
        var model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelId);
        var root = new JsonObject();
        root.add("model", model);
        return root;
    }

    @Override
    public String getName() {
        return "Item Models: " + Scannable.MOD_ID;
    }
}
