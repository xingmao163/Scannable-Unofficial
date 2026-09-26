package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starmao.scannable.Scannable;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Render pipelines and types for the scanner effect on 26.1.2.
 * <p>
 * Defined in code via {@link RenderPipeline#builder()} — no .json shader
 * descriptors needed. Shader GLSL sources are in
 * {@code assets/scannable_unofficial/shaders/core/}.
 */
public final class ScanResultRenderType {
    // -- Fullscreen scan wave -------------------------------------------------

    /**
     * Fullscreen scan-reveal effect: samples the main depth buffer and additively paints the
     * expanding spherical wave. No vertex buffer (core/screenquad generates the fullscreen triangle
     * from gl_VertexID); no depth test/write; additive blend. Drawn via a manual RenderPass in
     * ScannerRenderer so it can bind the depth texture + a per-frame uniform buffer.
     */
    public static final RenderPipeline SCAN_EFFECT_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_effect"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Scannable.id("core/scan_effect"))
            .withSampler("DepthSampler")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ScanInfo", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .build();

    // -- Scan result boxes ----------------------------------------------------

    /**
     * Filled, translucent, two-sided, through-wall box for result highlights.
     * Uses position_color shaders (POSITION_COLOR / QUADS / translucent blend).
     */
    public static final RenderPipeline RESULT_BOX_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_result"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderType RESULT_BOX_TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_result",
            RenderSetup.builder(RESULT_BOX_PIPELINE).createRenderSetup());

    // -- Shimmer overlay ------------------------------------------------------

    /**
     * Animated per-box shimmer (additive, through walls): the result-box fill, drawn with the
     * scan_result shader (scrolling scanlines + pulse + edge glow). Uses vanilla
     * core/position_tex_color vertex shader; the fragment reads GameTime from the Globals UBO.
     */
    public static final RenderPipeline SHIMMER_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_shimmer"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader(Scannable.id("core/scan_result"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderType SHIMMER_TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_shimmer",
            RenderSetup.builder(SHIMMER_PIPELINE).createRenderSetup());

    // -- Label background + icon ------------------------------------------------

    /**
     * Translucent, no-depth quad for label backgrounds (e.g. the dark bar behind
     * a result's name label). Built on vanilla's translucent POSITION_COLOR snippet.
     */
    public static final RenderPipeline TYPE_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_label"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    public static final RenderType TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_label",
            RenderSetup.builder(TYPE_PIPELINE).createRenderSetup());

    /**
     * Textured, translucent, two-sided, through-wall pipeline for the billboarded result icons.
     * <p>
     * Mirrors vanilla's {@code GUI_TEXTURED_SNIPPET}
     * ({@code RenderPipelines.GUI_TEXTURED_SNIPPET}) — same shaders, sampler, blend and vertex
     * format — plus the NO_DEPTH_TEST state so icons show through walls.
     * <p>
     * <b>The {@code DynamicTransforms} and {@code Projection} uniforms are mandatory.</b>
     * {@code core/position_tex_color} reads {@code ModelViewMat}/{@code ProjMat} and multiplies
     * the sampled texel by {@code ColorModulator}, all of which live in those two UBOs. Declaring
     * the sampler without them leaves {@code ColorModulator} at its zero default, so
     * {@code texture(...) * vertexColor * ColorModulator} evaluates to fully transparent and the
     * icon silently disappears — with no error, only the build-time warning
     * "Found unknown and unsupported uniform DynamicTransforms in ...:pipeline/scan_icon".
     */
    public static final RenderPipeline ICON_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_icon"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final Map<Identifier, RenderType> ICON_TYPES = new HashMap<>();

    /**
     * Returns a RenderType bound to the given icon texture, cached per-texture.
     *
     * <p><b>Never pass {@code null}.</b> A null texture becomes a null resource
     * location in the {@link RenderSetup}, and flushing that batch throws
     * {@code NullPointerException: Uploading texture} from
     * {@code ReloadableTexture.doLoad} — deep inside {@code endBatch()}, far from
     * the offending call. Callers that may not have an icon (e.g. the block
     * provider, which renders a text-only label) must skip the icon quad
     * entirely instead of asking for a type here.
     */
    public static RenderType icon(final Identifier texture) {
        if (texture == null) {
            throw new IllegalArgumentException(
                    "ScanResultRenderType.icon() requires a non-null texture; "
                            + "skip drawing the icon instead of passing null");
        }
        return ICON_TYPES.computeIfAbsent(texture, tex -> RenderType.create(
                Scannable.MOD_ID + ":scan_icon/" + tex,
                RenderSetup.builder(ICON_PIPELINE).withTexture("Sampler0", tex).createRenderSetup()));
    }

    private ScanResultRenderType() {}
}
