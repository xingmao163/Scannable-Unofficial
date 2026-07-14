package com.starmao.scannable.datagen;

import com.starmao.scannable.Scannable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public final class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // ---- Scanner (shaped) ----
        shaped(RecipeCategory.TOOLS, com.starmao.scannable.common.item.Items.SCANNER.get())
                .pattern("i i")
                .pattern("brb")
                .pattern("gqg")
                .define('i', Tags.Items.INGOTS_IRON)
                .define('b', Items.IRON_BARS)
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('g', Tags.Items.INGOTS_GOLD)
                .define('q', Tags.Items.GEMS_QUARTZ)
                .group("scanner")
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .unlockedBy("has_gold", has(Tags.Items.INGOTS_GOLD))
                .save(output);

        // ---- Blank Module (shaped) ----
        shaped(RecipeCategory.MISC, com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
                .pattern("ggg")
                .pattern("crc")
                .pattern("cnc")
                .define('g', Tags.Items.DYES_GREEN)
                .define('c', Items.CLAY_BALL)
                .define('r', Tags.Items.DUSTS_GLOWSTONE)
                .define('n', Tags.Items.NUGGETS_GOLD)
                .group("blank_module")
                .unlockedBy("has_clay", has(Items.CLAY_BALL))
                .unlockedBy("has_glowstone", has(Tags.Items.DUSTS_GLOWSTONE))
                .save(output);

        // ---- Modules (shapeless: blank + thematic ingredient) ----
        shapelessModule("range_module", Tags.Items.ENDER_PEARLS);
        shapelessModule("fluid_module", Items.WATER_BUCKET);
        shapelessModule("friendly_entity_module", Tags.Items.LEATHERS);
        shapelessModule("hostile_entity_module", Tags.Items.BONES);
        shapelessModule("block_module", stoneTag());
        shapelessModule("entity_module", Items.LEAD);
        shapelessModule("item_module", Items.CHEST);
    }

    private void shapelessModule(final String itemName, final TagKey<Item> ingredient) {
        final var item = com.starmao.scannable.common.item.Items.ITEMS.getEntries().stream()
                .filter(h -> h.getId().getPath().equals(itemName))
                .findFirst()
                .orElseThrow();
        shapeless(RecipeCategory.MISC, item.get())
                .requires(com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(com.starmao.scannable.common.item.Items.BLANK_MODULE.get()))
                .save(output, itemName + "_via_blank");
    }

    private void shapelessModule(final String itemName, final net.minecraft.world.level.ItemLike ingredient) {
        final var item = com.starmao.scannable.common.item.Items.ITEMS.getEntries().stream()
                .filter(h -> h.getId().getPath().equals(itemName))
                .findFirst()
                .orElseThrow();
        shapeless(RecipeCategory.MISC, item.get())
                .requires(com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(com.starmao.scannable.common.item.Items.BLANK_MODULE.get()))
                .save(output, itemName + "_via_blank");
    }

    private static TagKey<Item> stoneTag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "stones"));
    }

    // ---- Runner (DataProvider wrapper) ---- //

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput output,
                      final CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries,
                                                       final RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Scannable recipes";
        }
    }
}
