package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.SequencedMap;

/**
 * Factory for {@link MultiBufferSource.BufferSource} instances that are safe for
 * rendering <em>items</em>.
 *
 * <p><strong>Why this exists.</strong> A {@code BufferSource} keeps at most one live
 * "shared" batch at a time: {@code BufferSource#getBuffer} ends the previous shared
 * RenderType as soon as a different non-fixed type is requested. Item rendering with
 * the enchantment glint asks for <em>two</em> RenderTypes at once —
 * {@code ItemRenderer.getFoilBufferDirect} builds a {@code VertexMultiConsumer.Double}
 * from {@code RenderType.glint()} plus the item's own RenderType. With the empty
 * {@code fixedBuffers} table produced by {@code MultiBufferSource.immediate(...)},
 * fetching the second type immediately ends — and permanently kills — the glint
 * batch, while the already-created multi-consumer still holds a reference to it
 * ({@code BufferBuilder#build} clears its building flag unconditionally). The next
 * vertex write then throws {@code IllegalStateException: Not building!}.
 *
 * <p>Vanilla avoids this by registering the glint RenderTypes in {@code RenderBuffers}'
 * fixed table, so they travel their own dedicated buffers and never disturb the shared
 * batch. {@link #create()} reproduces exactly that, which makes the result safe for
 * {@code ItemRenderer} / {@code ItemInHandRenderer} work.
 *
 * <p>The returned instance may be reused across frames: {@code ByteBufferBuilder} only
 * advances an offset on {@code build()} and the memory is handed back once the resulting
 * mesh is released — vanilla {@code RenderBuffers} keeps its sources for the lifetime of
 * the client in the same way.
 */
public final class ScanRenderBuffers {

    /** Buffer size vanilla uses for the glint shader types. */
    private static final int GLINT_BUFFER_SIZE = 1536;

    /**
     * Creates a buffer source whose fixed table covers the enchantment-glint render
     * types, mirroring {@code RenderBuffers}' setup.
     *
     * <p>Every non-glint render type (item models, entity models, ...) still travels the
     * shared buffer, so the batching behaviour for those stays identical to vanilla.
     *
     * <p>Only the glint types present in <em>all</em> supported 1.21.x versions are
     * registered: {@code RenderType.entityGlintDirect()} exists in 1.21.1 but was removed
     * in later 1.21.x, and it is not needed here anyway — item rendering (first-person
     * hands, GUI item icons) resolves to {@code glint()} / {@code glintTranslucent()}.
     *
     * @return a fresh, item-render-safe buffer source
     */
    public static MultiBufferSource.BufferSource create() {
        final SequencedMap<RenderType, ByteBufferBuilder> fixed = new Object2ObjectLinkedOpenHashMap<>();
        fixed.put(RenderType.glint(), new ByteBufferBuilder(GLINT_BUFFER_SIZE));
        fixed.put(RenderType.glintTranslucent(), new ByteBufferBuilder(GLINT_BUFFER_SIZE));
        fixed.put(RenderType.entityGlint(), new ByteBufferBuilder(GLINT_BUFFER_SIZE));
        fixed.put(RenderType.armorEntityGlint(), new ByteBufferBuilder(GLINT_BUFFER_SIZE));
        return MultiBufferSource.immediateWithBuffers(fixed, new ByteBufferBuilder(GLINT_BUFFER_SIZE));
    }

    private ScanRenderBuffers() {}
}
