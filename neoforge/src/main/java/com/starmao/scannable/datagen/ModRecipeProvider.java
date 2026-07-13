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

/**
 * Generates all mod recipes.
 *
 * <p>The scanner itself is crafted via a shaped pattern. Every module
 * is a shapeless upgrade from a blank module plus one thematic ingredient
 * (item or tag).
 */
public final class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(final PackOutput output,
                             final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(final RecipeOutput output) {
        // ---- Scanner (shaped) ----
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS,
                        com.starmao.scannable.common.item.Items.SCANNER.get())
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
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
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
        shapelessModule(output, "range_module", Tags.Items.ENDER_PEARLS);
        shapelessModule(output, "fluid_module", Items.WATER_BUCKET);
        shapelessModule(output, "friendly_entity_module", Tags.Items.LEATHERS);
        shapelessModule(output, "hostile_entity_module", Tags.Items.BONES);
        shapelessModule(output, "block_module", stoneTag());
        shapelessModule(output, "entity_module", Items.LEAD);
        shapelessModule(output, "item_module", Items.CHEST);
    }

    private void shapelessModule(final RecipeOutput output,
                                 final String itemName,
                                 final TagKey<Item> ingredient) {
        final var item = com.starmao.scannable.common.item.Items.ITEMS.getEntries().stream()
                .filter(h -> h.getId().getPath().equals(itemName))
                .findFirst()
                .orElseThrow();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item.get())
                .requires(com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(com.starmao.scannable.common.item.Items.BLANK_MODULE.get()))
                .save(output, Scannable.id(itemName + "_via_blank"));
    }

    private void shapelessModule(final RecipeOutput output,
                                 final String itemName,
                                 final net.minecraft.world.level.ItemLike ingredient) {
        final var item = com.starmao.scannable.common.item.Items.ITEMS.getEntries().stream()
                .filter(h -> h.getId().getPath().equals(itemName))
                .findFirst()
                .orElseThrow();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item.get())
                .requires(com.starmao.scannable.common.item.Items.BLANK_MODULE.get())
                .requires(ingredient)
                .group("scanner_module")
                .unlockedBy("has_blank_module",
                        has(com.starmao.scannable.common.item.Items.BLANK_MODULE.get()))
                .save(output, Scannable.id(itemName + "_via_blank"));
    }

    /**
     * Returns the {@code c:stones} item tag.
     * In NeoForge 21.1.x there is no {@code Tags.Items.STONE} constant,
     * so we create the key directly to match the original recipe data.
     */
    private static TagKey<Item> stoneTag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "stones"));
    }
}
