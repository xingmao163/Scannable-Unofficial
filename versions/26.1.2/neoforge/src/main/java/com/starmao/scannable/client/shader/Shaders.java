package com.starmao.scannable.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starmao.scannable.Scannable;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shader definitions for the scanner effect.
 * Stub for 26.1.2 — shader system pending rewrite to new RenderPipeline API.
 */
public final class Shaders {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shaders.class);

    public static void initialize() {
        LOGGER.debug("Shader definitions initialized (26.1.2 stub)");
    }

    private Shaders() {}
}
