package com.starmao.scannable.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

import static com.starmao.scannable.common.item.Items.*;

/**
 * Generates all mod recipes.
 *
 * <p>The scanner itself is crafted via a shaped pattern. Every module
 * is a shapeless upgrade from a blank module plus one thematic ingredient
 * (item or tag).
 */
public final class ModRecipeProvider extends RecipeProvider {
    private ModRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // ---- Scanner (shaped) ----
        shaped(RecipeCategory.TOOLS, SCANNER.get())
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
        shaped(RecipeCategory.MISC, BLANK_MODULE.get())
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
        module(RANGE_MODULE.get(), Tags.Items.ENDER_PEARLS);
        module(FLUID_MODULE.get(), Items.WATER_BUCKET);
        module(FRIENDLY_ENTITY_MODULE.get(), Tags.Items.LEATHERS);
        module(HOSTILE_ENTITY_MODULE.get(), Tags.Items.BONES);
        module(BLOCK_MODULE.get(), Tags.Items.STONES);
        module(ENTITY_MODULE.get(), Items.LEAD);
        module(ITEM_MODULE.get(), Items.CHEST);
        module(CHARGER_MODULE.get(), Items.LIGHTNING_ROD);
    }

    private void module(final Item item, final TagKey<Item> ingredient) {
        shapeless(RecipeCategory.MISC, item)
                .requires(BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(BLANK_MODULE.get()))
                .save(output);
    }

    private void module(final Item item, final Item ingredient) {
        shapeless(RecipeCategory.MISC, item)
                .requires(BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(BLANK_MODULE.get()))
                .save(output);
    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Scannable Recipes";
        }
    }
}
