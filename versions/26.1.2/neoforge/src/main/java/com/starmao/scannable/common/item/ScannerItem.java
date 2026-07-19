package com.starmao.scannable.common.item;

import com.starmao.scannable.Scannable;
import com.starmao.scannable.client.ScanManager;
import com.starmao.scannable.client.audio.SoundManager;
import com.starmao.scannable.common.config.Constants;
import com.starmao.scannable.common.config.ModConfig;
import com.starmao.scannable.common.config.Strings;
import com.starmao.scannable.common.container.ScannerContainerMenu;
import com.starmao.scannable.common.energy.ItemEnergyStorage;
import com.starmao.scannable.common.inventory.ScannerContainer;
import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.network.data.ItemScanResultData;
import com.starmao.scannable.common.network.message.S2CItemScanResult;
import com.starmao.scannable.common.scanning.ChargingScannerModule;
import com.starmao.scannable.common.scanning.ItemScannerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public final class ScannerItem extends ModItem {
    public static boolean isScanner(ItemStack stack) {
        return stack.getItem() instanceof ScannerItem;
    }

    public ScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        // Energy bar info
        ItemEnergyStorage.of(stack).ifPresent(energy ->
                tooltip.accept(Strings.energyStorage(energy.getEnergyStored(), energy.getMaxEnergyStored())));

        // — Installed modules overview —
        ScannerContainer container = ScannerContainer.of(stack);

        // Active modules list
        Container activeModules = container.getActiveModules();
        boolean hasActive = false;
        for (int i = 0; i < activeModules.getContainerSize(); i++) {
            ItemStack module = activeModules.getItem(i);
            if (module.isEmpty()) continue;
            if (!hasActive) {
                tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tooltip.scannable_unofficial.scanner.active_modules")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
                hasActive = true;
            }
            int cost = ModuleHelper.getEnergyCost(module);
            Component name = module.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            if (cost > 0) {
                tooltip.accept(Component.literal(" ")
                        .append(name)
                        .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(String.valueOf(cost)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" FE)").withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                tooltip.accept(Component.literal(" ").append(name));
            }
        }

        // Inactive (stored) modules list
        Container inactiveModules = container.getInactiveModules();
        boolean hasInactive = false;
        for (int i = 0; i < inactiveModules.getContainerSize(); i++) {
            ItemStack module = inactiveModules.getItem(i);
            if (module.isEmpty()) continue;
            if (!hasInactive) {
                tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tooltip.scannable_unofficial.scanner.inactive_modules")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
                hasInactive = true;
            }
            tooltip.accept(Component.literal(" ")
                    .append(module.getHoverName().copy().withStyle(ChatFormatting.DARK_GRAY)));
        }

        // Total cost per scan summary
        if (hasActive) {
            int totalCost = 0;
            for (int i = 0; i < activeModules.getContainerSize(); i++) {
                totalCost += ModuleHelper.getEnergyCost(activeModules.getItem(i));
            }
            tooltip.accept(Component.empty());
            tooltip.accept(Strings.totalEnergyCost(totalCost));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ModConfig.SCANNER_USE_ENERGY.get();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) (getRelativeEnergy(stack) * MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(getRelativeEnergy(stack) / 3f, 1, 1);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return stack.getHoverName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new ScannerContainerMenu(id, inv, hand, ScannerContainer.of(stack));
                    }
                }, buf -> buf.writeEnum(hand));
            }
        } else {
            List<ItemStack> modules = new ArrayList<>();
            if (!collectModules(stack, modules)) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Strings.MESSAGE_NO_SCAN_MODULES);
                }
                player.getCooldowns().addCooldown(stack, 10);
                return InteractionResult.FAIL;
            }

            if (!tryConsumeEnergy(player, stack, modules, true)) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Strings.MESSAGE_NOT_ENOUGH_ENERGY);
                }
                player.getCooldowns().addCooldown(stack, 10);
                return InteractionResult.FAIL;
            }

            player.startUsingItem(hand);
            if (level.isClientSide()) {
                // Client-side: only scan with non-item modules (range, entity, block, etc.)
                // Item scanner results arrive from the server after the scan completes.
                final List<ItemStack> nonItemModules = new ArrayList<>();
                for (final ItemStack m : modules) {
                    if (!(m.getItem() instanceof ConfigurableItemScannerModuleItem)) {
                        nonItemModules.add(m);
                    }
                }
                if (!nonItemModules.isEmpty()) {
                    ScanManager.beginScan(player, nonItemModules);
                }
                SoundManager.playChargingSound();
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Constants.SCAN_DURATION_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        super.onUseTick(level, entity, stack, count);
        if (entity.level().isClientSide()) {
            ScanManager.updateScan(entity, false);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) {
            ScanManager.cancelScan();
            SoundManager.stopChargingSound();
        }
        super.releaseUsing(stack, level, entity, timeLeft);
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }

        List<ItemStack> modules = new ArrayList<>();
        if (!collectModules(stack, modules)) {
            return stack;
        }

        boolean hasEnergy = tryConsumeEnergy(player, stack, modules, false);

        if (level.isClientSide()) {
            finishScanClient(player, stack, modules, hasEnergy);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            finishScanServer(serverPlayer, stack, level);
        }

        player.getCooldowns().addCooldown(stack, Constants.SCAN_COOLDOWN_TICKS);
        return stack;
    }

    /**
     * Client-side scan finalisation: plays sounds and updates the scan renderer.
     */
    private static void finishScanClient(final LivingEntity entity, final ItemStack stack,
                                         final List<ItemStack> modules, final boolean hasEnergy) {
        SoundManager.stopChargingSound();
        if (hasEnergy) {
            ScanManager.updateScan(entity, true);
            SoundManager.playActivateSound();
        } else {
            ScanManager.cancelScan();
        }
    }

    /**
     * Server-side scan finalisation: reads the item scanner module configuration,
     * executes the scan via {@link ItemScannerService}, and sends results back
     * to the requesting client.
     */
    private static void finishScanServer(final ServerPlayer player, final ItemStack stack, final Level level) {
        final ScannerContainer scannerContainer = ScannerContainer.of(stack);
        final var activeModules = scannerContainer.getActiveModules();

        List<Identifier> targetItemIds = List.of();
        for (int slot = 0; slot < activeModules.getContainerSize(); slot++) {
            final ItemStack module = activeModules.getItem(slot);
            if (module.isEmpty()) continue;
            if (module.getItem() instanceof ConfigurableItemScannerModuleItem moduleItem) {
                targetItemIds = moduleItem.getIds(module);
                break;
            }
        }

        if (!targetItemIds.isEmpty()) {
            final Vec3 center = player.position();
            final int scanRadius = 64;
            final List<ItemScanResultData> results = ItemScannerService.scan(
                    level, center, scanRadius, targetItemIds);

            if (ModConfig.DEBUG_LOG_ITEM_SCANNER.get()) {
                Scannable.LOGGER.info("[ScannerItem] Server scan: {} result(s)", results.size());
            }

            if (!results.isEmpty()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        player, new S2CItemScanResult(center, results));
            }
        }
    }

    // ---- Energy ---- //

    private static float getRelativeEnergy(ItemStack stack) {
        return ItemEnergyStorage.of(stack)
                .map(storage -> storage.getEnergyStored() / (float) storage.getMaxEnergyStored())
                .orElse(0f);
    }

    private static boolean tryConsumeEnergy(Player player, ItemStack scanner, List<ItemStack> modules, boolean simulate) {
        if (!ModConfig.SCANNER_USE_ENERGY.get()) return true;
        if (player.isCreative()) return true;

        Optional<ItemEnergyStorage> energyStorage = ItemEnergyStorage.of(scanner);
        if (energyStorage.isEmpty()) return false;

        long totalCost = 0;
        for (ItemStack module : modules) {
            totalCost += ModuleHelper.getEnergyCost(module);
        }

        long extracted = energyStorage.get().extractEnergy(totalCost, simulate);
        return extracted >= totalCost;
    }

    // ---- Module collection ---- //

    private static boolean collectModules(ItemStack scanner, List<ItemStack> modules) {
        ScannerContainer container = ScannerContainer.of(scanner);
        Container activeModules = container.getActiveModules();
        boolean hasScannerModules = false;
        for (int slot = 0; slot < activeModules.getContainerSize(); slot++) {
        ItemStack module = activeModules.getItem(slot);
        if (module.isEmpty()) continue;
        modules.add(module);
        hasScannerModules |= ModuleHelper.hasResultProvider(module);
    }
    return hasScannerModules;
}


    // ---- Charging module tick ---- //

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slotId) {
        super.inventoryTick(stack, level, entity, slotId);
        if (!(entity instanceof Player player)) return;
        if (!ModConfig.SCANNER_USE_ENERGY.get()) return;

        ScannerContainer container = ScannerContainer.of(stack);
        var activeModules = container.getActiveModules();
        int chargerCount = 0;
        for (int i = 0; i < activeModules.getContainerSize(); i++) {
            var module = activeModules.getItem(i);
            if (module.isEmpty()) continue;
            if (ModuleHelper.getModule(module)
                    .filter(m -> m instanceof ChargingScannerModule)
                    .isPresent()) {
                chargerCount++;
            }
        }
        // Stacking: more charging modules = faster charging
        if (chargerCount == 0) return;

        long currentTick = level.getGameTime();
        int interval = ModConfig.CHARGER_MODULE_INTERVAL.get();
        // Recharge directly — bypass external charging gate so the module
        // works even when allowExternalCharging is false.
        int amount = ModConfig.CHARGER_MODULE_ENERGY_PER_PULSE.get() * chargerCount;
        int capacity = ModConfig.SCANNER_ENERGY_CAPACITY.get();
        int current = stack.getOrDefault(ModDataComponents.SCANNER_ENERGY.get(), 0);
        int newEnergy = Math.min(capacity, current + amount);
        stack.set(ModDataComponents.SCANNER_ENERGY.get(), newEnergy);
        stack.set(ModDataComponents.LAST_CHARGE_TICK.get(), currentTick);
    }

}
