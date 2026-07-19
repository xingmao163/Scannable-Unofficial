package com.starmao.scannable.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for a scan result provider.
 * Collects scan results over multiple ticks and renders them.
 */
public interface ScanResultProvider {

    /**
     * Initialises the provider for a new scan cycle.
     * <p>Called once when the scan begins. Implementations should capture
     * the scan parameters and prepare internal state for subsequent
     * {@link #computeScanResults} and {@link #collectScanResults} calls.
     *
     * @param player   the player performing the scan
     * @param modules  the scanner module item stacks installed
     * @param center   the world position where the scan originates
     * @param radius   the maximum scan radius in blocks
     * @param scanTicks the total duration of the scan in ticks
     */
    void initialize(Player player, Collection<ItemStack> modules, Vec3 center, float radius, int scanTicks);

    /**
     * Computes the set of scan results.
     * <p>Called each tick during the scan's active phase. Implementations
     * should perform the actual world queries (block state checks, entity
     * lookups, capability queries) and accumulate results internally.
     */
    void computeScanResults();

    /**
     * Passes accumulated scan results to the consumer for rendering.
     * <p>Called after {@link #computeScanResults} each tick. The provider
     * should feed every active result to the callback; expired or stale
     * results should be omitted.
     *
     * @param level    the world / block getter to resolve positions against
     * @param callback consumer that receives each visible scan result
     */
    void collectScanResults(BlockGetter level, Consumer<ScanResult> callback);

    /**
     * Renders the scan results in the given context.
     * <p>Called every frame while scan results are active. The provider
     * receives the list of results accumulated during the scan tick and
     * should draw in-world markers (boxes, icons) or GUI elements as
     * appropriate for the {@link ScanResultRenderContext}.
     *
     * @param context       the rendering context (world-space or GUI)
     * @param bufferSource  the buffer source for vertex data
     * @param poseStack     the current transformation matrix stack
     * @param renderInfo    the camera / rendering info
     * @param partialTicks  the frame's partial tick for interpolation
     * @param results       the scan results to render
     */
    void render(ScanResultRenderContext context, MultiBufferSource bufferSource, PoseStack poseStack,
                Camera renderInfo, float partialTicks, List<ScanResult> results);

    /**
     * Resets the provider to its initial state.
     * <p>Called when a scan ends or is interrupted. Implementations should
     * clear any cached results, release resources, and prepare for the
     * next scan cycle.
     */
    void reset();
}
