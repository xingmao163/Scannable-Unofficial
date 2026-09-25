package com.starmao.scannable.client.renderer;

import com.starmao.scannable.Scannable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

/**
 * Hand depth rendering. Stub for 26.1.2 — GPU pipeline rewrite pending.
 * TODO: Reimplement using new SubmitNodeCollector pipeline.
 */
public final class HandDepthRenderer {

    public static void writeHandDepth(final float partialTicks) {
        final Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()
                || mc.options.hideGui
                || mc.gameMode.getPlayerMode() == GameType.SPECTATOR
                || mc.player == null
                || (mc.getCameraEntity() instanceof LivingEntity living && living.isSleeping())) {
            return;
        }
        // TODO: Reimplement using 26.1 GPU pipeline
    }

    private HandDepthRenderer() {}
}
