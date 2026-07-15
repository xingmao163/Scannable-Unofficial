package com.starmao.scannable.client.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.starmao.scannable.client.ScanManager;
import com.starmao.scannable.client.shader.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public enum ScannerRenderer {
    INSTANCE;

    private DepthOnlyRenderTarget mainCameraDepth = new DepthOnlyRenderTarget("scannable_depth", MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);
    private long currentStart;
    private Vec3 currentCenter;

    private GpuBuffer cachedQuadBuffer;
    private int cachedBufferWidth = -1;
    private int cachedBufferHeight = -1;

    private int pendingBufferWidth = -1;
    private int pendingBufferHeight = -1;

    public void ping(Vec3 pos) {
        currentStart = System.currentTimeMillis();
        currentCenter = pos;
    }

    public void tick() {
        if (pendingBufferWidth > 0 && pendingBufferHeight > 0) {
            createQuadBuffer(pendingBufferWidth, pendingBufferHeight);
            pendingBufferWidth = -1;
            pendingBufferHeight = -1;
        }
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
        mainCameraDepth.copyDepthFrom(mainRenderTarget);
    }

    private void renderEffect(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        var device = RenderSystem.getDevice();
        if (device == null) return;

        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        var depthTex = mainCameraDepth.getDepthTexture();
        if (depthTex == null) return;

        int w = mainTarget.width;
        int h = mainTarget.height;

        if (cachedQuadBuffer == null || cachedBufferWidth != w || cachedBufferHeight != h) {
            pendingBufferWidth = w;
            pendingBufferHeight = h;
            return;
        }

        Matrix4f invertedViewMatrix = new Matrix4f(viewMatrix);
        invertedViewMatrix.invert();

        Matrix4f invertedProjectionMatrix = new Matrix4f(projectionMatrix);
        invertedProjectionMatrix.invert();

        var cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        int adjustedDuration = ScanManager.computeScanGrowthDuration();
        float radius = ScanManager.computeRadius(currentStart, (float) adjustedDuration);

        var encoder = device.createCommandEncoder();
        if (encoder == null) return;

        var colorTex = mainTarget.getColorTexture();
        if (colorTex == null) return;

        try (var pass = encoder.createRenderPass(colorTex, java.util.OptionalInt.empty(), depthTex, java.util.OptionalDouble.empty())) {
            pass.setPipeline(Shaders.SCAN_EFFECT);

            pass.bindSampler("depthTex", depthTex);
            pass.setUniform("center", (float) currentCenter.x(), (float) currentCenter.y(), (float) currentCenter.z());
            pass.setUniform("invViewMat", invertedViewMatrix);
            pass.setUniform("invProjMat", invertedProjectionMatrix);
            pass.setUniform("pos", (float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
            pass.setUniform("radius", radius);

            pass.setVertexBuffer(0, cachedQuadBuffer);
            pass.draw(4, 1);
        }
    }

    private void createQuadBuffer(int width, int height) {
        var device = RenderSystem.getDevice();
        if (device == null) return;

        if (cachedQuadBuffer != null) {
            cachedQuadBuffer.close();
            cachedQuadBuffer = null;
        }

        int vertexSize = Float.BYTES * 5;
        ByteBuffer vertexData = ByteBuffer.allocate(vertexSize * 4).order(java.nio.ByteOrder.nativeOrder());
        vertexData.putFloat(0).putFloat(0).putFloat(0).putFloat(0).putFloat(0)
                .putFloat(width).putFloat(0).putFloat(0).putFloat(1).putFloat(0)
                .putFloat(width).putFloat(height).putFloat(0).putFloat(1).putFloat(1)
                .putFloat(0).putFloat(height).putFloat(0).putFloat(0).putFloat(1);
        vertexData.flip();

        cachedQuadBuffer = device.createBuffer(
                () -> "scanner_quad",
                BufferType.VERTICES,
                BufferUsage.STATIC_WRITE,
                vertexData);
        cachedBufferWidth = width;
        cachedBufferHeight = height;
    }

    public static final class DepthOnlyRenderTarget extends TextureTarget {
        public DepthOnlyRenderTarget(String name, int width, int height) {
            super(name, width, height, true);
        }

        @Override
        public void createBuffers(int width, int height) {
            super.createBuffers(width, height);

        }

        public com.mojang.blaze3d.textures.GpuTexture getDepthTexture() {
            return this.depthTexture;
        }
    }
}
