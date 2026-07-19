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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * Renders the scanner's expanding ring / wave effect using a custom shader.
 * <p>Grabs the scene depth buffer and renders a full-screen effect that
 * expands outward from the scan origin, creating a visual "scan wave"
 * that grows over {@link com.starmao.scannable.client.ScanManager#computeScanGrowthDuration()}.
 * <p>Singleton enum — only one such renderer exists per client.
 */
public enum ScannerRenderer {
