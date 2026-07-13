package com.starmao.scannable.api;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.Closeable;

public interface ScanResult extends Closeable {
    Vec3 getPosition();

    @Nullable
    AABB getRenderBounds();

    @Override
    default void close() {
    }
}
