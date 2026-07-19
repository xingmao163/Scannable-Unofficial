package com.starmao.scannable.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

import static com.starmao.scannable.common.item.Items.*;

/**
 * Generates all vanilla recipes for scanner items.
 * <p>Produces shaped and shapeless recipes for the scanner and every module.
 * Run via {@code runData} — output goes to {@code src/generated/resources/}.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(final RecipeOutput exporter) {
        // Scanner
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SCANNER.get())
                .pattern("S")
                .pattern("I")
                .pattern("R")
                .define('S', Tags.Items.GLASS_BLOCKS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        // Modules
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, RANGE_MODULE.get())
                .requires(Tags.Items.ENDER_PEARLS)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, FLUID_MODULE.get())
                .requires(Items.BUCKET)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, FRIENDLY_ENTITY_MODULE.get())
                .requires(Items.EGG)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, HOSTILE_ENTITY_MODULE.get())
                .requires(Items.ROTTEN_FLESH)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, BLOCK_MODULE.get())
                .requires(Tags.Items.COBBLESTONES)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ENTITY_MODULE.get())
                .requires(Items.SPAWNER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ITEM_MODULE.get())
                .requires(Items.CHEST)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(exporter);
    }
}
