package com.starmao.scannable.common.inventory;

import com.starmao.scannable.common.item.ModuleHelper;
import com.starmao.scannable.common.item.ModDataComponents;
import com.starmao.scannable.common.item.ScannerItem;
import com.starmao.scannable.common.item.ScannerModuleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class ScannerContainer extends SimpleContainer {
    private static final int ACTIVE_MODULE_COUNT = 3;
    private static final int INACTIVE_MODULE_COUNT = 6;
    private static final int TOTAL_MODULE_COUNT = ACTIVE_MODULE_COUNT + INACTIVE_MODULE_COUNT;

    private final ItemStack container;

    public ScannerContainer(ItemStack container) {
        super(TOTAL_MODULE_COUNT);
        this.container = container;

        ItemContainerContents contents = container.get(ModDataComponents.SCANNER_MODULES.get());
        if (contents != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            contents.copyInto(items);
            for (int slot = 0; slot < getContainerSize(); slot++) {
                setItem(slot, items.get(slot));
            }
        }
    }

    public static ScannerContainer of(ItemStack container) {
        if (container.getItem() instanceof ScannerItem) {
            return new ScannerContainer(container);
        }
        return new ScannerContainer(new ItemStack(container.getItem()));
    }

    public ContainerSlice getActiveModules() {
        return new ContainerSlice(this, 0, ACTIVE_MODULE_COUNT);
    }

    public ContainerSlice getInactiveModules() {
        return new ContainerSlice(this, ACTIVE_MODULE_COUNT, INACTIVE_MODULE_COUNT);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (canPlaceItem(i, itemStack)) {
            super.setItem(i, itemStack);
        }
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack stack) {
        return isModule(stack) && super.canPlaceItem(i, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        List<ItemStack> items = new ArrayList<>(getContainerSize());
        for (int slot = 0; slot < getContainerSize(); slot++) {
            items.add(getItem(slot));
        }
        this.container.set(ModDataComponents.SCANNER_MODULES.get(), ItemContainerContents.fromItems(items));
    }

    @Override
    public ItemStack addItem(ItemStack stack) {
        if (canAddItem(stack)) {
            return super.addItem(stack);
        } else {
            return stack;
        }
    }

    @Override
    public boolean canAddItem(ItemStack stack) {
        return isModule(stack) && super.canAddItem(stack);
    }

    private boolean isModule(ItemStack stack) {
        if (stack.getItem() instanceof ScannerModuleItem) return true;
        return ModuleHelper.getModule(stack).isPresent();
    }
}
