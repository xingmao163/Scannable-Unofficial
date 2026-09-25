package com.starmao.scannable.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starmao.scannable.Scannable;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Manages custom GL shader definitions for the scanner effect.
 *
 * <p>In Minecraft 1.21.2, {@link ShaderProgram} is a record definition that
 * references a shader loaded by the {@code ShaderManager}. The actual compilation
 * and lifecycle is handled by the game's shader system. Custom shaders must have
 * their {@code .json}, {@code .vsh}, and {@code .fsh} files in
 * {@code assets/&lt;modid&gt;/shaders/core/}.</p>
 */
public final class Shaders {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shaders.class);

    /** Publicly accessible shader definition for the scan result shader. */
    public static final ShaderProgram SCAN_RESULT;

    @Nullable
    private static ShaderProgram scanEffectShader;

    @Nullable
    private static ShaderProgram scanResultShader;

    static {
        SCAN_RESULT = createShader("scan_result", DefaultVertexFormat.POSITION_TEX_COLOR);
        scanResultShader = SCAN_RESULT;
        scanEffectShader = createShader("scan_effect", DefaultVertexFormat.POSITION_TEX);
    }

    private static ShaderProgram createShader(String name, VertexFormat format) {
        return new ShaderProgram(
                ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, name),
                format,
                ShaderDefines.EMPTY
        );
    }

    /**
     * No-op initialization. Shader definitions are initialized in the static
     * initializer. The game's {@code ShaderManager} handles actual compilation
     * and reloading of GL shaders.
     */
    public static void initialize() {
        LOGGER.debug("Shader definitions initialized: scan_effect, scan_result");
    }

    @Nullable
    public static ShaderProgram getScanEffectShader() {
        return scanEffectShader;
    }

    @Nullable
    public static ShaderProgram getScanResultShader() {
        return scanResultShader;
    }

    private Shaders() {
    }
}
