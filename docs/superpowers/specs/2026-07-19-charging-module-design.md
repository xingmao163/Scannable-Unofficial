# 充能模块 (Charging Module) 设计规格

## 概述

为 Scannable-Unofficial 添加一个 **充能模块**，安装在扫描器后每隔一段时间自动补充能量。

## 类型

简单被动模块（类似 Range 模块），无需 GUI 配置。

## 行为

- 模块自身 **不消耗能量**（`getEnergyCost() = 0`）
- 模块 **不产生扫描结果**（`hasResultProvider() = false`）
- 装上后自动生效，无需玩家操作
- **仅服务端执行**充能逻辑

## 配置（ModConfig）

| 键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `charging.interval` | int（tick） | `100` | 每次充能间隔，20 tick = 1 秒 |
| `charging.amount` | int（FE） | `10` | 每次充能补充的能量 |

## 堆叠规则

**禁止堆叠**：扫描器内安装多个充能模块不会叠加效果。装 1 个和装 N 个效果完全相同——只要检测到至少 1 个充能模块存在，即按固定速率充能。

## 技术实现

### Tick 机制

在 `ScannerItem.inventoryTick()` 中实现：

1. 检查 `level.isClientSide()` —— 仅服务端执行
2. 从扫描器的 `ScannerContainer` 获取活跃模块列表
3. 遍历模块，检查是否有 `ChargingScannerModule.INSTANCE`
4. 如果没有充能模块，跳过
5. 从 data component 读取 `lastChargeTick`
6. 计算 `currentGameTime - lastChargeTick ≥ interval`
7. 如果满足条件，调用 `ItemEnergyStorage.receiveEnergy(amount, false)`
8. 更新 `lastChargeTick` 为当前 game time

### 数据组件

新增 `LAST_CHARGE_TICK: DataComponentType<Long>` 记录上次充能的 game time。

### 涉及文件

**新增：**

| 文件 | 说明 |
|------|------|
| `ChargingScannerModule.java` | 枚举单例，实现 `ScannerModule` |
| 语言条目 | 各语言文件中的名称和描述 |

**修改：**

| 文件 | 变更 |
|------|------|
| `Items.java` | 注册 `charger_module` 物品 |
| `ModConfig.java` | 添加 `charging.interval` 和 `charging.amount` |
| `ModDataComponents.java` | 添加 `LAST_CHARGE_TICK` |
| `ScannerItem.java` | 覆写 `inventoryTick` |
| `en_us.json` / `zh_cn.json` 等 | 名称、描述、配置项翻译 |

### 不需要

- ❌ GUI / 菜单
- ❌ 网络包
- ❌ ScanResultProvider
- ❌ 客户端代码
- ❌ 配置文件 GUI
