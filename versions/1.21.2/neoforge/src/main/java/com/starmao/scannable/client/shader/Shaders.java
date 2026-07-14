package com.starmao.scannable.client.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class Shaders {
    public static final ShaderProgram SCAN_EFFECT = new ShaderProgram(
            com.starmao.scannable.Scannable.id("core/scan_effect"),
            DefaultVertexFormat.POSITION_TEX,
            ShaderDefines.EMPTY);
    public static final ShaderProgram SCAN_RESULT = new ShaderProgram(
            com.starmao.scannable.Scannable.id("core/scan_result"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY);

    public static void onRegisterShaders(final RegisterShadersEvent event) {
        event.registerShader(SCAN_EFFECT);
        event.registerShader(SCAN_RESULT);
    }

    private Shaders() {
    }
}
