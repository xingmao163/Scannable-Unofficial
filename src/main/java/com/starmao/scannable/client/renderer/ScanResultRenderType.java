package com.starmao.scannable.client.renderer;

import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.starmao.scannable.Scannable;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Render pipelines and types for the scanner effect on 26.3.
 */
public final class ScanResultRenderType {
    // -- Fullscreen scan wave -------------------------------------------------

    
    public static final BindGroupLayout SCAN_INFO_LAYOUT = BindGroupLayout.builder()
            .withUniform("ScanInfo", UniformType.UNIFORM_BUFFER)
            .build();

    public static final RenderPipeline SCAN_EFFECT_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_effect"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Scannable.id("core/scan_effect"))
            // Note: Custom uniforms/samplers might need custom BindGroupLayouts in RenderPearl
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withBindGroupLayout(SCAN_INFO_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();

    // -- Scan result boxes ----------------------------------------------------

    public static final RenderPipeline RESULT_BOX_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_result"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    public static final RenderType RESULT_BOX_TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_result",
            RenderSetup.builder(RESULT_BOX_PIPELINE).createRenderSetup());

    // -- Shimmer overlay ------------------------------------------------------

    public static final RenderPipeline SHIMMER_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_shimmer"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader(Scannable.id("core/scan_result"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    public static final RenderType SHIMMER_TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_shimmer",
            RenderSetup.builder(SHIMMER_PIPELINE).createRenderSetup());

    // -- Label background + icon ------------------------------------------------

    public static final RenderPipeline TYPE_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_label"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    public static final RenderType TYPE = RenderType.create(
            Scannable.MOD_ID + ":scan_label",
            RenderSetup.builder(TYPE_PIPELINE).createRenderSetup());

    public static final RenderPipeline ICON_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_icon"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    private static final Map<Identifier, RenderType> ICON_TYPES = new HashMap<>();

    public static final RenderPipeline SCAN_PROGRESS_PIPELINE = RenderPipeline.builder()
            .withLocation(Scannable.id("pipeline/scan_progress"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();

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
