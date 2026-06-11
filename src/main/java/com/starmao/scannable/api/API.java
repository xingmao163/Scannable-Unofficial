package com.starmao.scannable.api;

import com.starmao.scannable.Scannable;
import net.minecraft.resources.ResourceLocation;

public final class API {
    public static final ResourceLocation ICON_INFO = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "textures/gui/overlay/info.png");
    public static final ResourceLocation ICON_WARNING = ResourceLocation.fromNamespaceAndPath(Scannable.MOD_ID, "textures/gui/overlay/warning.png");

    private API() {
    }
}
