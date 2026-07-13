package com.starmao.scannable.common.item;

import com.starmao.scannable.common.container.BlockModuleContainerMenu;
import com.starmao.scannable.common.scanning.ConfigurableBlockScannerModule;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class ConfigurableBlockScannerModuleItem extends ConfigurableModuleItem<Block> {
    public ConfigurableBlockScannerModuleItem() {
        super(ConfigurableBlockScannerModule.INSTANCE,
                (id, inv, hand) -> new BlockModuleContainerMenu(id, inv, hand));
    }

    @Override
    protected DataComponentType<List<ResourceLocation>> getComponent() {
        return ModDataComponents.BLOCKS.get();
    }

    @Override
    protected Registry<Block> getRegistry() {
        return BuiltInRegistries.BLOCK;
    }

    @Override
    protected Component getDisplayName(final Block block) {
        return block.getName();
    }

    @Override
    protected Component getListCaption() {
        return Component.translatable("tooltip.scannable_unofficial.scanner.block_module.list").withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockState state = level.getBlockState(context.getClickedPos());
        final Block block = state.getBlock();
        if (block == Blocks.AIR) return InteractionResult.PASS;

        final Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (addValue(player.getItemInHand(context.getHand()), block)) {
            player.swing(context.getHand());
            player.getInventory().setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
