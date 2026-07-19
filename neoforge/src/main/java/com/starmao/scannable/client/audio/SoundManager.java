package com.starmao.scannable.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;

/**
 * Manages scanner audio feedback — charge-up loop and activation sounds.
 * <p>All methods are static utility calls; the class is never instantiated.
 * Sound instances are managed via Minecraft's {@link net.minecraft.client.sounds.SoundManager}.
 */
public final class SoundManager {
