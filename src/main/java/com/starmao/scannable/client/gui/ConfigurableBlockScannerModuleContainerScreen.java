package com.starmao.scannable.client.gui;

import com.starmao.scannable.common.container.BlockModuleContainerMenu;
import com.starmao.scannable.common.item.ConfigurableBlockScannerModuleItem;
import com.starmao.scannable.common.network.Network;
import com.starmao.scannable.common.network.message.SetConfiguredModuleItemAtMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Screen for configuring a block scanner module. */
public class ConfigurableBlockScannerModuleContainerScreen
        extends AbstractConfigurableScannerModuleContainerScreen<BlockModuleContainerMenu, Block> {

    public ConfigurableBlockScannerModuleContainerScreen(BlockModuleContainerMenu container,
                                                          Inventory inventory, Component title) {
        super(container, inventory, title,
                Component.translatable("gui.scannable_unofficial.scanner.block_module.list"));
    }

    @Override
    protected List<Block> getConfiguredItems(ItemStack stack) {
        if (stack.getItem() instanceof ConfigurableBlockScannerModuleItem item) {
            return item.getValues(stack);
        }
        return List.of();
    }

    @Override
    protected Component getItemName(Block block) {
        return block.getName();
    }

    /**
     * Draws the configured block's item icon into its slot.
     *
     * <p><b>Pass the slot-relative {@code x}/{@code y} unchanged — do not add
     * {@code leftPos}/{@code topPos}.</b> {@code AbstractContainerScreen} already translates the
     * extractor's pose by {@code (leftPos, topPos)} before calling {@code extractLabels}, and
     * {@code fakeItem} captures that pose into its {@code GuiItemRenderState}
     * ({@code new Matrix3x2f(this.pose)}). Adding the offset here applies it a second time and
     * pushes the icon a full panel-width off screen, so a configured filter appears to have no
     * icon at all. This matches vanilla, which draws slot contents with
     * {@code graphics.fakeItem(itemStack, slot.x, slot.y, seed)} — also un-offset.
     *
     * <p>Contrast {@link ConfigurableEntityScannerModuleContainerScreen}, where the offset
     * <em>is</em> required: {@code GuiGraphicsExtractor#entity} does not capture the pose.
     */
    @Override
    protected void renderConfiguredItem(GuiGraphicsExtractor graphics, Block block, int x, int y) {
        graphics.fakeItem(new ItemStack(block.asItem()), x, y);
    }

    @Override
    protected void configureItemAt(ItemStack stack, int slot, ItemStack value) {
        Block block = Block.byItem(value.getItem());
        if (block != Blocks.AIR) {
            BuiltInRegistries.BLOCK.getResourceKey(block).ifPresent(key ->
                Network.sendToServer(new SetConfiguredModuleItemAtMessage(menu.containerId, slot, key.identifier())));
        }
    }
}
