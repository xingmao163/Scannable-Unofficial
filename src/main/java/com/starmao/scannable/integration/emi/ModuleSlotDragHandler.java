package com.starmao.scannable.integration.emi;

import com.starmao.scannable.client.gui.AbstractConfigurableScannerModuleContainerScreen;
import com.starmao.scannable.common.config.ServerConfig;
import com.starmao.scannable.client.gui.ConfigurableBlockScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableEntityScannerModuleContainerScreen;
import com.starmao.scannable.client.gui.ConfigurableItemScannerModuleContainerScreen;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModuleSlotDragHandler implements EmiDragDropHandler<Screen> {

    private static final int SLOTS_ORIGIN_X = 62;
    private static final int SLOTS_ORIGIN_Y = 20;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COUNT = 5;

    @Override
    public boolean dropStack(Screen screen, EmiIngredient ingredient, int mouseX, int mouseY) {
        if (!ServerConfig.HOOK_ALLOW_EMI.get()) return false;

        if (!(screen instanceof AbstractConfigurableScannerModuleContainerScreen<?, ?> cfgScreen)) {
            return false;
        }

        int relX = mouseX - cfgScreen.getGuiLeft();
        int relY = mouseY - cfgScreen.getGuiTop();

        int slot = getSlotAt(relX, relY);
        if (slot < 0 || slot >= SLOT_COUNT) return false;

        var stacks = ingredient.getEmiStacks();
        if (stacks.isEmpty()) return false;
        EmiStack emiStack = stacks.get(0);

        ItemStack itemStack = emiStack.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) return false;

        int containerId = cfgScreen.getMenu().containerId;

        if (screen instanceof ConfigurableBlockScannerModuleContainerScreen) {
            Block block = Block.byItem(itemStack.getItem());
            if (block == Blocks.AIR) return false;
            var key = BuiltInRegistries.BLOCK.getResourceKey(block);
            if (key.isPresent()) {
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(containerId, slot, key.get().location()));
                return true;
            }
        } else if (screen instanceof ConfigurableEntityScannerModuleContainerScreen) {
            if (itemStack.getItem() instanceof SpawnEggItem spawnEgg) {
                var type = spawnEgg.getType(itemStack);
                var key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(type);
                if (key.isPresent()) {
                    Network.sendToServer(new SetConfiguredModuleItemAtMessage(containerId, slot, key.get().location()));
                    return true;
                }
            }
        } else if (screen instanceof ConfigurableItemScannerModuleContainerScreen) {
            var key = BuiltInRegistries.ITEM.getResourceKey(itemStack.getItem());
            if (key.isPresent()) {
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(containerId, slot, key.get().location()));
                return true;
            }
        }

        return false;
    }


    private static int getSlotAt(int relX, int relY) {
        int col = (relX - SLOTS_ORIGIN_X) / SLOT_SIZE;
        if (col < 0 || col >= SLOT_COUNT) return -1;
        int row = (relY - SLOTS_ORIGIN_Y) / SLOT_SIZE;
        if (row != 0) return -1;
        return col;
    }
}
