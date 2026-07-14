package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.common.item.Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public final class ModItemTagProvider extends TagsProvider<Item> {

    public ModItemTagProvider(final PackOutput output,
                               final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider, Scannable.MOD_ID);
    }

    @Override
    protected void addTags(final HolderLookup.Provider lookupProvider) {
        tag(com.starmao.scannable.common.tags.ItemTags.MODULES)
                .add(Items.BLANK_MODULE.getKey())
                .add(Items.RANGE_MODULE.getKey())
                .add(Items.FLUID_MODULE.getKey())
                .add(Items.FRIENDLY_ENTITY_MODULE.getKey())
                .add(Items.HOSTILE_ENTITY_MODULE.getKey())
                .add(Items.BLOCK_MODULE.getKey())
                .add(Items.ENTITY_MODULE.getKey())
                .add(Items.ITEM_MODULE.getKey());
    }

    @Override
    public String getName() {
        return "Scannable Item Tags";
    }
}
