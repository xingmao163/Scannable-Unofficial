package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates item model JSON files for all scanner items.
 *
 * <p>All items use {@code item/generated} with a matching texture in
 * the {@code item/} texture directory (already hand-authored).
 */
public final class ModItemModelProvider extends ItemModelProvider {

    private static final String[] ITEMS = {
            "scanner",
            "blank_module",
            "range_module",
            "fluid_module",
            "friendly_entity_module",
            "hostile_entity_module",
            "entity_module",
            "block_module",
            "item_module",
    };

    public ModItemModelProvider(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, Scannable.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (final String item : ITEMS) {
            singleTexture(item, mcLoc("item/generated"), "layer0", modLoc("item/" + item));
        }
    }
}
