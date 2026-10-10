package com.starmao.scannable.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;

/** Manages scanner audio feedback. */
public final class SoundManager {
    private static final SoundEvent SCANNER_CHARGE = SoundEvent.createVariableRangeEvent(
            com.starmao.scannable.Scannable.id("scanner_charge"));
    private static final SoundEvent SCANNER_ACTIVATE = SoundEvent.createVariableRangeEvent(
            com.starmao.scannable.Scannable.id("scanner_activate"));

    @Nullable
    private static SimpleSoundInstance currentChargingSound;

    @SuppressWarnings("null")
    public static void playChargingSound() {
        currentChargingSound = SimpleSoundInstance.forUI(SCANNER_CHARGE, 1);
        Minecraft.getInstance().getSoundManager().play(currentChargingSound);
    }

    @SuppressWarnings("null")
    public static void stopChargingSound() {
        if (currentChargingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentChargingSound);
            currentChargingSound = null;
        }
    }

    public static void playActivateSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SCANNER_ACTIVATE, 1));
    }

    private SoundManager() {
    }
}
