package com.starmao.scannable.common.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.minecraft.ChatFormatting.*;

public final class Strings {
    public static final Component MESSAGE_NO_SCAN_MODULES = Component.translatable("message.scannable_unofficial.scanner.no_modules").withStyle(RED);
    public static final Component MESSAGE_NOT_ENOUGH_ENERGY = Component.translatable("message.scannable_unofficial.scanner.no_energy").withStyle(RED);
    public static final Component MESSAGE_NO_FREE_SLOTS = Component.translatable("message.scannable_unofficial.scanner.no_free_slots");

    public static Component energyStorage(long stored, long capacity) {
        MutableComponent storedText = Component.literal(String.valueOf(stored)).withStyle(GREEN);
        MutableComponent capacityText = Component.literal(String.valueOf(capacity)).withStyle(GREEN);
        return Component.translatable("tooltip.scannable_unofficial.scanner.energy", storedText, capacityText).withStyle(GRAY);
    }

    public static Component energyUsage(int value) {
        MutableComponent energyText = Component.literal(String.valueOf(value)).withStyle(GREEN);
        return Component.translatable("tooltip.scannable_unofficial.scanner_module.energy", energyText).withStyle(GRAY);
    }

    public static Component totalEnergyCost(int value) {
        MutableComponent energyText = Component.literal(String.valueOf(value)).withStyle(GREEN);
        return Component.translatable("tooltip.scannable_unofficial.scanner.total_energy_cost", energyText).withStyle(GRAY);
    }

    public static Component progress(int value) {
        return Component.translatable("gui.scannable_unofficial.scanner.progress", value);
    }

    private Strings() {
    }
}
