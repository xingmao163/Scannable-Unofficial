package com.starmao.scannable.client.shader;

/**
 * Shader definitions for the scanner effect.
 *
 * <p><b>Dead code on 26.1.2 — safe to delete once the version source trees diverge.</b>
 * On 1.21.1 this class owned the two {@code ShaderInstance}s loaded from
 * {@code shaders/core/*.json} and was referenced from {@code ScannerClientSetup},
 * {@code ScannerRenderer} and both result providers. The 26.1 render pipeline replaces
 * GLSL descriptors with {@code RenderPipeline} objects built in code, so all of that now
 * lives in {@link com.starmao.scannable.client.renderer.ScanResultRenderType} and nothing
 * calls this class.
 *
 * <p>It is kept only so the shared client setup file keeps compiling identically on both
 * branches — see the branch model in ARCHITECTURE.md. Do not add new callers.
 *
 * @deprecated superseded by {@link com.starmao.scannable.client.renderer.ScanResultRenderType}
 */
@Deprecated
public final class Shaders {

    private Shaders() {}
}
