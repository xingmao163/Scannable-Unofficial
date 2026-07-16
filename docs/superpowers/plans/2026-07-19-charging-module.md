# 充能模块 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 subagent-driven-development（推荐）或 executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 Scannable-Unofficial 添加充能模块，安装后每 100 ticks 自动为扫描器补充 10 FE。

**架构：** 简单被动模块（`ScannerModule` 枚举单例），通过 `ScannerItem.inventoryTick` 在服务端 tick 中检测模块并调用 `ItemEnergyStorage.receiveEnergy`。充能间隔记录在扫描器的 `LAST_CHARGE_TICK` data component 中。

**技术栈：** NeoForge 1.21.x, ModDevGradle, Java 21

---

### 任务 1：添加充能配置项

**修改：** `neoforge/src/main/java/com/starmao/scannable/common/config/ModConfig.java`

- [ ] **步骤 1：在 ModConfig 中添加充能配置**

在 `ModConfig.java` 中 `// ---- Range Modifiers ----` 区块之后、`// ---- Ignored ----` 之前添加两个新配置项：

```java
// ---- Charging ----

public static final ModConfigSpec.IntValue CHARGING_INTERVAL = BUILDER
        .comment("Tick interval between charging module energy transfers (20 ticks = 1 second).")
        .defineInRange("charging.interval", 100, 1, 72000);

public static final ModConfigSpec.IntValue CHARGING_AMOUNT = BUILDER
        .comment("Amount of energy restored per charging interval.")
        .defineInRange("charging.amount", 10, 1, 10000);
```

- [ ] **步骤 2：验证编译**

```bash
./gradlew :neoforge:compileJava
```
预期：BUILD SUCCESSFUL

---

### 任务 2：添加充能时间数据组件

**修改：** `neoforge/src/main/java/com/starmao/scannable/common/item/ModDataComponents.java`

- [ ] **步骤 1：在 ModDataComponents 中添加 LAST_CHARGE_TICK**

在 `LOCKED` 注册之后新增：

```java
public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LAST_CHARGE_TICK =
        REGISTER.register("last_charge_tick", () -> DataComponentType.<Long>builder()
                .persistent(Codec.LONG)
                .networkSynchronized(ByteBufCodecs.VAR_LONG)
                .build());
```

需要在文件头添加 `import com.mojang.serialization.Codec;`（如果尚未导入）和 `import net.minecraft.network.codec.ByteBufCodecs;`。

- [ ] **步骤 2：验证编译**

```bash
./gradlew :neoforge:compileJava
```
预期：BUILD SUCCESSFUL

---

### 任务 3：创建充能模块枚举

**创建：** `neoforge/src/main/java/com/starmao/scannable/common/scanning/ChargingScannerModule.java`

- [ ] **步骤 1：编写 ChargingScannerModule**

```java
package com.starmao.scannable.common.scanning;

import com.starmao.scannable.api.ScanResultProvider;
import com.starmao.scannable.api.ScannerModule;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Charging module — does not scan. Instead, it periodically restores energy
 * to the scanner while installed in an active slot.
 *
 * <p>Energy restoration is handled server-side in {@link
 * com.starmao.scannable.common.item.ScannerItem#inventoryTick}.
 * Multiple charging modules do <em>not</em> stack.
 */
public enum ChargingScannerModule implements ScannerModule {
    INSTANCE;

    @Override
    public int getEnergyCost(ItemStack module) {
        return 0; // does not consume energy
    }

    @Override
    public boolean hasResultProvider() {
        return false;
    }

    @Nullable
    @Override
    public ScanResultProvider getResultProvider() {
        return null;
    }
}
```

- [ ] **步骤 2：确保包目录存在**

`neoforge/src/main/java/com/starmao/scannable/common/scanning/` 已存在（有其他模块在）。

- [ ] **步骤 3：验证编译**

```bash
./gradlew :neoforge:compileJava
```
预期：BUILD SUCCESSFUL

---

### 任务 4：注册充能模块物品

**修改：** `neoforge/src/main/java/com/starmao/scannable/common/item/Items.java`

- [ ] **步骤 1：在 Items.java 中添加注册**

在 `ITEM_MODULE` 注册之后添加：

```java
public static final DeferredItem<ScannerModuleItem> CHARGER_MODULE = ITEMS.register("charger_module",
        () -> new ScannerModuleItem(ChargingScannerModule.INSTANCE));
```

确保文件头已导入 `ChargingScannerModule`（在 `com.starmao.scannable.common.scanning.*` 通配导入中已覆盖）。

- [ ] **步骤 2：验证编译**

```bash
./gradlew :neoforge:compileJava
```
预期：BUILD SUCCESSFUL

---

### 任务 5：在 ScannerItem 中实现充能 tick

**修改：** `neoforge/src/main/java/com/starmao/scannable/common/item/ScannerItem.java`

- [ ] **步骤 1：添加 inventoryTick 覆写**

在 `ScannerItem` 类中 `// ---- Energy ----` 静态方法块之前添加 `inventoryTick` 覆写：

```java
@Override
public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
    super.inventoryTick(stack, level, entity, slotId, isSelected);
    // Only charge on the server side
    if (level.isClientSide()) return;
    if (!(entity instanceof Player player)) return;
    if (!ModConfig.SCANNER_USE_ENERGY.get()) return;

    // Check if a charging module is installed in an active slot
    ScannerContainer container = ScannerContainer.of(stack);
    Container activeModules = container.getActiveModules();
    boolean hasCharger = false;
    for (int i = 0; i < activeModules.getContainerSize(); i++) {
        ItemStack module = activeModules.getItem(i);
        if (module.isEmpty()) continue;
        if (ModuleHelper.getModule(module)
                .filter(m -> m instanceof ChargingScannerModule)
                .isPresent()) {
            hasCharger = true;
            break;
        }
    }
    if (!hasCharger) return;

    // Read last charge tick
    long lastTick = stack.getOrDefault(ModDataComponents.LAST_CHARGE_TICK.get(), 0L);
    long currentTick = level.getGameTime();
    int interval = ModConfig.CHARGING_INTERVAL.get();
    if (currentTick - lastTick < interval) return;

    // Recharge
    ItemEnergyStorage.of(stack).ifPresent(energy -> {
        int amount = ModConfig.CHARGING_AMOUNT.get();
        energy.receiveEnergy(amount, false);
        stack.set(ModDataComponents.LAST_CHARGE_TICK.get(), currentTick);
    });
}
```

需要在文件头添加必要的 import：
- `net.minecraft.world.entity.Entity`
- `net.minecraft.world.entity.player.Player`
- `net.minecraft.world.Container`
- `com.starmao.scannable.common.scanning.ChargingScannerModule`

- [ ] **步骤 2：验证编译**

```bash
./gradlew :neoforge:compileJava
```
预期：BUILD SUCCESSFUL

---

### 任务 6：添加语言翻译

**修改：** 各语言 JSON 文件

- [ ] **步骤 1：在 en_us.json 中添加条目**

`neoforge/src/main/resources/assets/scannable_unofficial/lang/en_us.json`：

```json
"item.scannable_unofficial.charger_module": "Charging Module",
"item.scannable_unofficial.charger_module.desc": "Periodically recharges the scanner",
"scannable_unofficial.configuration.chargingInterval": "Charging Interval",
"scannable_unofficial.configuration.chargingAmount": "Charging Amount"
```

- [ ] **步骤 2：在 zh_cn.json 中添加条目**

`neoforge/src/main/resources/assets/scannable_unofficial/lang/zh_cn.json`：

```json
"item.scannable_unofficial.charger_module": "充能模块",
"item.scannable_unofficial.charger_module.desc": "每隔一段时间自动为扫描器补充能量",
"scannable_unofficial.configuration.chargingInterval": "充能间隔",
"scannable_unofficial.configuration.chargingAmount": "单次充能量"
```

- [ ] **步骤 3：验证构建**

```bash
./gradlew :neoforge:build
```
预期：BUILD SUCCESSFUL

---

### 任务 7：运行测试验证

- [ ] **步骤 1：启动客户端验证功能**

```bash
./gradlew :neoforge:client
```

1. 创造模式物品栏中确认出现"充能模块"
2. 将充能模块安装到扫描器活跃槽位
3. 观察能量条是否自动上涨（每 100 tick +10 FE）
4. 安装多个充能模块，确认效果不叠加
