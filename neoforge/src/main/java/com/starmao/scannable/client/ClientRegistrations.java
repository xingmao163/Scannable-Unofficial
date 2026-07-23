package com.starmao.scannable.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only registrations that must NOT be referenced from common code
 * at the bytecode level (avoids {@link net.neoforged.fml.common.asm.RuntimeDistCleaner} errors).
 * <p>
 * Each method is annotated {@link OnlyIn @OnlyIn(Dist.CLIENT)} so the
 * RuntimeDistCleaner strips the method body on a dedicated server,
 * removing all client-only class references from the bytecode.
 * The class itself is intentionally <strong>not</strong> annotated
 * {@code @OnlyIn(Dist.CLIENT)} so that common code can reference it
 * inside a {@code FMLEnvironment.dist.isClient()} guard without
 * triggering a dist-load violation.
 */
public final class ClientRegistrations {

    @OnlyIn(Dist.CLIENT)
    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (mc, screen) ->
                        new ConfigurationScreen(modContainer, screen)
        );
    }

    private ClientRegistrations() {
    }
}
