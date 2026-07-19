package com.starmao.scannable.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the loading and lifecycle of custom GL shaders for the scanner effect.
 * <p>Registers two shaders:
 * <ul>
 *   <li>{@code scan_effect} — full-screen wave effect using {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#POSITION_TEX}</li>
 *   <li>{@code scan_result} — block highlight rendering using {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#POSITION_TEX_COLOR}</li>
 * </ul>
 * <p>Implements {@link ResourceManagerReloadListener} so shaders are reloaded
 * whenever resources are reloaded (F3+T).
 */
public final class Shaders implements ResourceManagerReloadListener {
