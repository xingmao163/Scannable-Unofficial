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

/** Manages custom GL shaders for the scanner effect. */
public final class Shaders implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shaders.class);
    private static final Shaders INSTANCE = new Shaders();
    private static final List<ShaderReference> SHADERS = new ArrayList<>();

    @Nullable public static ShaderInstance scanEffectShader;
    @Nullable public static ShaderInstance scanResultShader;

    public static void initialize() {
        addShader("scan_effect", DefaultVertexFormat.POSITION_TEX, shader -> scanEffectShader = shader);
        addShader("scan_result", DefaultVertexFormat.POSITION_TEX_COLOR, shader -> scanResultShader = shader);
        loadAndListenToReload();
    }

    @Nullable
    public static ShaderInstance getScanEffectShader() {
        return scanEffectShader;
    }

    @Nullable
    public static ShaderInstance getScanResultShader() {
        return scanResultShader;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        reloadShaders(manager);
    }

    private static void loadAndListenToReload() {
        Minecraft.getInstance().submitAsync(() -> {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            INSTANCE.onResourceManagerReload(manager);
            if (manager instanceof ReloadableResourceManager reloadableManager) {
                reloadableManager.registerReloadListener(INSTANCE);
            }
        });
    }

    private static void reloadShaders(ResourceProvider provider) {
        RenderSystem.assertOnRenderThread();
        SHADERS.forEach(reference -> reference.reload(provider));
    }

    private static void addShader(String name, VertexFormat format, Consumer<ShaderInstance> reloadAction) {
        SHADERS.add(new ShaderReference(name, format, reloadAction));
    }

    private static final class ShaderReference {
        private final String name;
        private final VertexFormat format;
        private final Consumer<ShaderInstance> reloadAction;
        @Nullable private ShaderInstance shader;

        ShaderReference(String name, VertexFormat format, Consumer<ShaderInstance> reloadAction) {
            this.name = name;
            this.format = format;
            this.reloadAction = reloadAction;
        }

        void reload(ResourceProvider provider) {
            if (shader != null) {
                shader.close();
                shader = null;
            }

            try {
                shader = new ShaderInstance(
                        location -> provider.getResource(
                                ResourceLocation.fromNamespaceAndPath("minecraft", location.getPath()))
                                .or(() -> provider.getResource(
                                        ResourceLocation.fromNamespaceAndPath(com.starmao.scannable.Scannable.MOD_ID, location.getPath()))),
                        name, format);
            } catch (Exception e) {
                LOGGER.error("Failed to load shader: {}", name, e);
            }

            reloadAction.accept(shader);
        }
    }

    private Shaders() {
    }
}
