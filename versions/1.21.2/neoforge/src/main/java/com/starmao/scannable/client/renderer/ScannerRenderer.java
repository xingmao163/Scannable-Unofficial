package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starmao.scannable.client.ScanManager;
import com.starmao.scannable.client.shader.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import com.mojang.blaze3d.ProjectionType;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/** Renders the scan wave effect using custom shaders. */
public enum ScannerRenderer {
    INSTANCE;

    private DepthOnlyRenderTarget mainCameraDepth = new DepthOnlyRenderTarget(MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);
    private long currentStart;
    private Vec3 currentCenter;

    public void ping(Vec3 pos) {
        currentStart = System.currentTimeMillis();
        currentCenter = pos;
    }

    public static void render(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        INSTANCE.doRender(viewMatrix, projectionMatrix);
    }

    private void doRender(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        if (shouldRender()) {
            grabDepthBuffer();
            renderEffect(viewMatrix, projectionMatrix);
        }
    }

    private boolean shouldRender() {
        int adjustedDuration = ScanManager.computeScanGrowthDuration();
        return currentStart > 0 && adjustedDuration > (int) (System.currentTimeMillis() - currentStart);
    }

    private void grabDepthBuffer() {
        RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.width != mainCameraDepth.width || mainRenderTarget.height != mainCameraDepth.height) {
            mainCameraDepth.resize(mainRenderTarget.width, mainRenderTarget.height);
        }
        mainCameraDepth = copyBufferSettings(mainRenderTarget, mainCameraDepth);
        mainCameraDepth.copyDepthFrom(mainRenderTarget);
        mainRenderTarget.bindWrite(false);
    }

    private void renderEffect(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        ShaderProgram shader = Shaders.getScanEffectShader();
        if (shader == null) return;

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        blit(target, viewMatrix, projectionMatrix);
    }

    private void blit(RenderTarget target, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        int width = target.width;
        int height = target.height;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        ShaderProgram shaderDef = Shaders.getScanEffectShader();
        if (shaderDef == null) return;
        CompiledShaderProgram oldShader = RenderSystem.setShader(shaderDef);

        // Compute uniforms now that the shader is active
        CompiledShaderProgram activeShader = RenderSystem.getShader();
        if (activeShader != null) {
            Matrix4f invertedViewMatrix = new Matrix4f(viewMatrix);
            invertedViewMatrix.invert();

            Matrix4f invertedProjectionMatrix = new Matrix4f(projectionMatrix);
            invertedProjectionMatrix.invert();

            Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            int adjustedDuration = ScanManager.computeScanGrowthDuration();
            float radiusAmount = ScanManager.computeRadius(currentStart, (float) adjustedDuration);

            activeShader.safeGetUniform("depthTex").set(mainCameraDepth.getDepthTextureId());
            activeShader.safeGetUniform("center").set(currentCenter.toVector3f());
            activeShader.safeGetUniform("invViewMat").set(invertedViewMatrix);
            activeShader.safeGetUniform("invProjMat").set(invertedProjectionMatrix);
            activeShader.safeGetUniform("pos").set(cameraPosition.toVector3f());
            activeShader.safeGetUniform("radius").set(radiusAmount);
        }

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, 0, height, 1, 100), ProjectionType.ORTHOGRAPHIC);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(0, 0, -50).setUv(0, 0);
        buffer.addVertex(width, 0, -50).setUv(1, 0);
        buffer.addVertex(width, height, -50).setUv(1, 1);
        buffer.addVertex(0, height, -50).setUv(0, 1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.setShader(oldShader);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }


    public static final class DepthOnlyRenderTarget extends TextureTarget {
        public DepthOnlyRenderTarget(int width, int height) {
            super(width, height, true);
        }

        public void createBuffers(int width, int height) {
            super.createBuffers(width, height);
            if (colorTextureId > -1) {
                if (frameBufferId > -1) {
                    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
                    glDrawBuffer(GL_NONE);
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                }
                TextureUtil.releaseTextureId(this.colorTextureId);
                this.colorTextureId = -1;
            }
        }
    }

    private static DepthOnlyRenderTarget copyBufferSettings(RenderTarget mainRenderTarget, DepthOnlyRenderTarget depthRenderTarget) {
        return depthRenderTarget;
    }
}
