package com.starmao.scannable.common.scanning;

import com.starmao.scannable.common.config.ConfigParsers;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.common.scanning.filter.IgnoredBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Scanner module that detects rare ore blocks (diamond, emerald, netherite, quartz, etc.).
 *
 * <p>By default, any block tagged with the configured top-level ore tag
 * ({@link ServerConfig#RARE_ORE_TOP_TAG}, default {@code c:ores}) is considered
 * a rare ore — unless it is already caught by the
 * {@link CommonOresBlockScannerModule} (common ores) or is in the
 * {@link IgnoredBlocks} list. Players may also add extra block IDs via
 * {@link ServerConfig#RARE_ORE_BLOCKS} and extra tags via
 * {@link ServerConfig#RARE_ORE_TAGS}.
 *
 * <p>The filter is lazily built and cached.
 *
 * <p>Singleton — use {@link #INSTANCE}.
 */
public final class RareOresBlockScannerModule extends AbstractOreBlockScannerModule {
    public static final RareOresBlockScannerModule INSTANCE = new RareOresBlockScannerModule();

    private RareOresBlockScannerModule() {}

    /** Clears the cached filter so it is rebuilt on the next scan. */
    public static void clearCache() {
        INSTANCE.clearFilter();
    }

    @Override
    protected int getEnergyCostConfig() {
        return ServerConfig.SCANNER_ENERGY_COST_ORE_RARE.get();
    }

    @Override
    protected float getRangeModifierConfig() {
        return ServerConfig.SCANNER_RANGE_MODIFIER_ORE_RARE.get().floatValue();
    }

    @Override
    protected List<? extends String> getBlockConfig() {
        return ServerConfig.RARE_ORE_BLOCKS.get();
    }

    @Override
    protected List<? extends String> getTagConfig() {
        return ServerConfig.RARE_ORE_TAGS.get();
    }

    @Override
    protected void addExtraFilters(final List<Predicate<BlockState>> filters) {
        // Implicit rule: anything in the top-level ore tag that is neither
        // common nor ignored counts as rare.
        final TagKey<Block> topLevelOreTag = ConfigParsers.parseBlockTag(ServerConfig.RARE_ORE_TOP_TAG.get());
        if (topLevelOreTag != null) {
            filters.add(state -> !IgnoredBlocks.contains(state)
                    && state.is(topLevelOreTag)
                    && !CommonOresBlockScannerModule.INSTANCE.getFilter(ItemStack.EMPTY).test(state));
        }
    }
}
