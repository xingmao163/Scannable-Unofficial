package com.starmao.scannable.client.renderer;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.starmao.scannable.client.ScanManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Renders the fullscreen scan-reveal wave using the 26.1.2 GPU pipeline.
 * <p>
 * Samples the main depth buffer and reconstructs world position per-pixel to paint an
 * additive expanding spherical shell ("ping wave") onto whatever surface it crosses.
 */
public enum ScannerRenderer {
    INSTANCE;

    // --------------------------------------------------------------------- //

    private long currentStart;
    @Nullable private Vec3 currentCenter;

    // --------------------------------------------------------------------- //

    public void ping(final Vec3 pos) {
        currentStart = System.currentTimeMillis();
        currentCenter = pos;
    }


    public void render(final Matrix4fc modelViewMatrix, final CameraRenderState cameraRenderState) {
        render(modelViewMatrix, cameraRenderState.projectionMatrix);
    }

    /**
     * Draws the fullscreen scan-reveal wave for the active ping (if any). Called from the world
     * render hook with the camera view-rotation matrix; the wave is composited additively onto the
     * main color target by sampling its depth buffer and reconstructing world position per-pixel.
     */
    public void render(final Matrix4fc modelViewMatrix, final Matrix4fc projectionMatrix) {
        if (!shouldRender() || currentCenter == null) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        final RenderTarget target = mc.gameRenderer.mainRenderTarget();
        final GpuTextureView colorView = target.getColorTextureView();
        final GpuTextureView depthView = target.getDepthTextureView();
        if (colorView == null || depthView == null) {
            return;
        }

        // Force a generic 4x4 matrix inversion. In vanilla, the projection matrix might have
        // view bobbing applied to it via Matrix4f.mul(), which adds translations into the W-row
        // without clearing JOML's PROPERTY_PERSPECTIVE flag. If we use a direct .invert() here,
        // JOML's invertPerspective() optimization kicks in and completely destroys the W-row,
        // causing the unprojected Z coordinates to collapse into a vertical stripe artifact.
        // Copying element-by-element using set(float...) guarantees properties = 0.
        final Matrix4f invViewMatrix = new Matrix4f().set(
                modelViewMatrix.m00(), modelViewMatrix.m01(), modelViewMatrix.m02(), modelViewMatrix.m03(),
                modelViewMatrix.m10(), modelViewMatrix.m11(), modelViewMatrix.m12(), modelViewMatrix.m13(),
                modelViewMatrix.m20(), modelViewMatrix.m21(), modelViewMatrix.m22(), modelViewMatrix.m23(),
                modelViewMatrix.m30(), modelViewMatrix.m31(), modelViewMatrix.m32(), modelViewMatrix.m33()
        ).invert();
        final Matrix4f invProjMatrix = new Matrix4f().set(
                projectionMatrix.m00(), projectionMatrix.m01(), projectionMatrix.m02(), projectionMatrix.m03(),
                projectionMatrix.m10(), projectionMatrix.m11(), projectionMatrix.m12(), projectionMatrix.m13(),
                projectionMatrix.m20(), projectionMatrix.m21(), projectionMatrix.m22(), projectionMatrix.m23(),
                projectionMatrix.m30(), projectionMatrix.m31(), projectionMatrix.m32(), projectionMatrix.m33()
        ).invert();
        final Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
        final int adjustedDuration = ScanManager.computeScanGrowthDuration();
        final float radius = ScanManager.computeRadius(currentStart, adjustedDuration);

        final GpuDevice device = RenderSystem.getDevice();
        final CommandEncoder encoder = device.createCommandEncoder();

        // Build the per-frame std140 uniform block (invView, center, camera, radius).
        final int size = new Std140SizeCalculator().putMat4f().putMat4f().putVec4().putVec4().putVec4().get();
        final GpuBuffer uniformBuffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer data = Std140Builder.onStack(stack, size)
                    .putMat4f(invProjMatrix)
                    .putMat4f(invViewMatrix)
                    .putVec4((float) currentCenter.x, (float) currentCenter.y, (float) currentCenter.z, 0.0f)
                    .putVec4((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z, 0.0f)
                    .putVec4(radius, 0.0f, 0.0f, 0.0f)
                    .get();
            uniformBuffer = device.createBuffer(() -> "scannable scan_effect uniforms", GpuBuffer.USAGE_UNIFORM, data);
        }

        final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        try (RenderPass pass = encoder.createRenderPass(() -> "scannable scan_effect", colorView, java.util.Optional.empty())) {
            pass.setPipeline(com.mojang.blaze3d.systems.RenderSystem.getCompiledPipeline(ScanResultRenderType.SCAN_EFFECT_PIPELINE));
                        pass.setUniform("ScanInfo", uniformBuffer);
            pass.setUniform("Sampler0", depthView, sampler);
            pass.draw(3, 1, 0, 0);
        } finally {
            uniformBuffer.close();
        }
    }

    // --------------------------------------------------------------------- //

    private boolean shouldRender() {
        final int adjustedDuration = ScanManager.computeScanGrowthDuration();
        return currentStart > 0 && adjustedDuration > (int) (System.currentTimeMillis() - currentStart);
    }

    private ScannerRenderer() {}
}
