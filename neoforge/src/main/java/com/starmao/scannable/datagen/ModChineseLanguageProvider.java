package com.starmao.scannable.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates {@code assets/scannable_unofficial/lang/zh_cn.json}.
 */
public final class ModChineseLanguageProvider extends LanguageProvider {

    public ModChineseLanguageProvider(final PackOutput output, final String modId, final String locale) {
        super(output, modId, locale);
    }

    @Override
    protected void addTranslations() {
        // Creative tab
        add("itemGroup.scannable_unofficial", "扫描仪");

        // Items
        add("item.scannable_unofficial.scanner", "扫描仪");
        add("item.scannable_unofficial.range_module", "范围模块");
        add("item.scannable_unofficial.fluid_module", "流体模块");
        add("item.scannable_unofficial.friendly_entity_module", "友好生物模块");
        add("item.scannable_unofficial.hostile_entity_module", "敌对生物模块");
        add("item.scannable_unofficial.entity_module", "实体模块");
        add("item.scannable_unofficial.block_module", "方块模块");
        add("item.scannable_unofficial.item_module", "物品模块");
        add("item.scannable_unofficial.blank_module", "空白模块");

        // Item descriptions
        add("item.scannable_unofficial.range_module.desc", "将扫描半径增加50%%");
        add("item.scannable_unofficial.fluid_module.desc", "检测流体方块，如水和熔岩");
        add("item.scannable_unofficial.friendly_entity_module.desc", "检测友好生物，如动物和村民");
        add("item.scannable_unofficial.hostile_entity_module.desc", "检测敌对生物，如怪物");
        add("item.scannable_unofficial.entity_module.desc", "右键实体以配置要检测的特定实体类型");
        add("item.scannable_unofficial.block_module.desc", "右键方块以配置要检测的特定方块");
        add("item.scannable_unofficial.blank_module.desc", "一个没有功能的占位模块");
        add("item.scannable_unofficial.item_module.desc", "扫描容器中已配置的物品");

        // Container titles
        add("container.scannable_unofficial.scanner", "扫描仪");
        add("container.scannable_unofficial.block_module", "方块模块");
        add("container.scannable_unofficial.entity_module", "实体模块");

        // GUI labels
        add("gui.scannable_unofficial.scanner.active_modules", "模块");
        add("gui.scannable_unofficial.scanner.inactive_modules", "未激活");
        add("gui.scannable_unofficial.scanner.progress", "%s%%");
        add("gui.scannable_unofficial.scanner.overlay.distance", "%s (%sm)");
        add("gui.scannable_unofficial.scanner.block_module.list", "方块");
        add("gui.scannable_unofficial.scanner.entity_module.list", "实体");
        add("gui.scannable_unofficial.scanner.item_module.list", "物品");

        // Tooltips
        add("tooltip.scannable_unofficial.scanner.energy", "能量: %s / %s FE");
        add("tooltip.scannable_unofficial.scanner_module.energy", "能量消耗: %s FE");
        add("tooltip.scannable_unofficial.scanner.total_energy_cost", "总计: %s FE/次扫描");
        add("tooltip.scannable_unofficial.scanner.active_modules", "已激活模块");
        add("tooltip.scannable_unofficial.scanner.inactive_modules", "未激活");
        add("tooltip.scannable_unofficial.scanner.block_module.list", "已选方块:");
        add("tooltip.scannable_unofficial.scanner.entity_module.list", "已选实体:");
        add("tooltip.scannable_unofficial.scanner.item_module.list", "已选物品:");

        // Messages
        add("message.scannable_unofficial.scanner.no_target_items", "§c物品模块未配置任何物品！右键打开配置");
        add("message.scannable_unofficial.scanner.no_modules", "未安装扫描模块！");
        add("message.scannable_unofficial.scanner.no_energy", "能量不足！");
        add("message.scannable_unofficial.scanner.no_free_slots", "没有空槽位！");

        // Configuration screen — ModConfig (scannable_unofficial-server.toml)
        add("scannable_unofficial.configuration.debug", "调试");
        add("scannable_unofficial.configuration.debug.logItemScanner", "物品扫描器日志");
        add("scannable_unofficial.configuration.scanner", "扫描仪");
        add("scannable_unofficial.configuration.scanner.useEnergy", "消耗能量");
        add("scannable_unofficial.configuration.scanner.energyCapacity", "能量容量");
        add("scannable_unofficial.configuration.scanner.baseScanRadius", "基础扫描半径");
        add("scannable_unofficial.configuration.scanner.resultStayDuration", "结果持续时长");
        add("scannable_unofficial.configuration.energy", "能量消耗");
        add("scannable_unofficial.configuration.energy.range", "范围模块");
        add("scannable_unofficial.configuration.energy.fluid", "流体模块");
        add("scannable_unofficial.configuration.energy.friendly", "友好生物模块");
        add("scannable_unofficial.configuration.energy.hostile", "敌对生物模块");
        add("scannable_unofficial.configuration.energy.block", "方块模块");
        add("scannable_unofficial.configuration.energy.entity", "实体模块");
        add("scannable_unofficial.configuration.energy.item", "物品模块");
        add("scannable_unofficial.configuration.range", "范围修正");
        add("scannable_unofficial.configuration.range.range", "范围模块修正");
        add("scannable_unofficial.configuration.range.fluid", "流体模块修正");
        add("scannable_unofficial.configuration.range.block", "方块模块修正");
        add("scannable_unofficial.configuration.fluids", "流体");
        add("scannable_unofficial.configuration.fluids.ignoredTags", "忽略的流体标签");
        add("scannable_unofficial.configuration.ignored", "忽略方块");
        add("scannable_unofficial.configuration.ignored.blocks", "忽略的方块注册名");
        add("scannable_unofficial.configuration.ignored.blockTags", "忽略的方块标签");

        // Configuration screen — ClientConfig (scannable_unofficial-client.toml)
        add("scannable_unofficial.configuration.colors", "颜色");
        add("scannable_unofficial.configuration.colors.blocksColors", "方块颜色");
        add("scannable_unofficial.configuration.colors.blockTagsColors", "方块标签颜色");
        add("scannable_unofficial.configuration.colors.fluidsColors", "流体颜色");
        add("scannable_unofficial.configuration.colors.fluidTagsColors", "流体标签颜色");
        add("scannable_unofficial.configuration.misc", "杂项");
        add("scannable_unofficial.configuration.misc.hideBrokenBlocks", "隐藏已破坏方块");
        add("scannable_unofficial.configuration.misc.itemScanColor", "物品扫描颜色");
    }
}
