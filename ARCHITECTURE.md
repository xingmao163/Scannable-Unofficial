# 文件架构说明

本项目采用**按 Minecraft 版本拆分独立分支**的方式维护多版本。本文档说明分支职责、
目录结构与版本配置约定。**切分支工作或新增版本分支前请先读完第 1、2 节。**

## 1. 分支模型

| 分支 | 构建目标 | Java | 说明 |
|---|---|---|---|
| `main` | 1.21.1 | 21 | 活跃开发主线 |
| `1.21.2-1.21.3` | 1.21.2（同时服务 1.21.3） | 21 | 已停止维护 |
| `1.21.4` | 1.21.4 | 21 | 已停止维护 |
| `26.1.2` | 26.1.2 | 25 | 开发中，部分功能仍为空桩 |

> 分支名一律用**连字符**而不是斜杠分隔多版本（`1.21.2-1.21.3` 而非 `1.21.2/1.21.3`）。
> git 的引用存储不允许同名路径既是文件又是目录：只要 `refs/heads/1.21.2` 存在，
> `refs/heads/1.21.2/3` 就会以 `cannot lock ref` 失败，两者无法共存。

**每个分支都是自包含的**：`src/main/` 里就是该版本的完整源码树，
不含任何针对其它版本的覆盖层，也不依赖别的分支才能编译。

### 为什么不再用"单分支 + 版本覆盖层"

历史上所有版本混在 `main` 上，靠 `build.gradle` 的 `mergeNeoforgeSources` /
`mergeNeoforgeResources` 把「主树 + `versions/shared` + `versions/<版本>`」三层合并后编译。
该方案有两个已实际造成损失的缺陷：

1. **没有任何关卡能发现某个版本坏了。** `main` 编译通过只证明 1.21.1 通过。
   实测 1.21.2 与 1.21.4 自 `4264163`（升级 1.3.0）起就编译失败 ——
   `BlockCacheScanFilter` 改用了 `BuiltInRegistries.BLOCK.getTag()`，而该方法在
   1.21.2 的映射中已被移除；26.1.2 则自 `6b3418a`（发布 v1.3.1）起从未编译通过
   （`ScannerItem.java` 缺一个大括号 + `ScanManager` 缺一个方法），
   而 `build.yml` 的矩阵里根本没有 26.1.2，所以无人察觉。
2. **改动需人工同步多份副本。** 实测 20 个文件存在 3~4 份副本（含 `Scannable.java`、
   `Items.java`、`ScannerItem.java`、`ModItem.java`、`ModDataComponents.java`、
   全部 `Configurable*ModuleItem`、两个 GUI 屏），且已出现真实漂移。

拆分后每个分支有**自己的 CI 构建**，任何版本坏掉会立刻暴露。

## 2. 目录职责

```
<任意分支>/
├── build.gradle              ★ 各分支逐字节一致，不含任何版本条件判断
├── gradle.properties         ★ 各分支的唯一版本差异所在（含 java_version）
├── settings.gradle           仓库名 + 插件仓库（各分支一致）
├── gradle/
│   ├── versions.gradle       仅 sharedRepositories（Maven 仓库列表）
│   ├── libs.versions.toml    版本目录（目前只有 moddev 插件版本）
│   └── wrapper/              Gradle Wrapper
├── src/main/
│   ├── java/                 该版本的完整实现源码
│   ├── resources/            assets / data（配方、标签、进度、着色器）
│   ├── templates/            neoforge.mods.toml 模板（占位符替换后生成）
│   └── generated/            DataGen 输出（首次运行 runData 后出现）
├── .github/workflows/build.yml  本分支的单版本构建（各分支仅 java-version 不同）
├── .gitattributes            行尾符规范化（分支间 cherry-pick/merge 依赖它）
├── ARCHITECTURE.md           本文档
├── CLAUDE.md                 项目规则（给 AI 协作者）
└── README.md                 面向用户的功能说明
```

顶层不应出现 `bin/`、`net/`、`assets/`、`META-INF/` 这类目录：它们是本地 IDE 输出或
反编译参考副本，已在 `.gitignore` 中按**根目录锚定**（`/bin/`、`/net/`、`/assets/`、
`/META-INF/`、`/.mcsrc1211/`）排除。**这些规则必须带前导斜杠** —— 无斜杠的 `assets/`
会匹配任意层级，曾把 `versions/26.1.2/.../resources/assets/` 整棵树（12 个
`items/*.json` + 2 个 `shaders/*.fsh`）静默排除在版本控制之外。

## 3. 版本配置约定（重要）

**版本差异只允许出现在 `gradle.properties`。** 这是硬约定，原因是各分支的
`build.gradle` 必须保持逐字节一致，否则将来修构建脚本时无法跨分支无冲突合并。

需要改版本号、NeoForge 版本、依赖版本时，只改本分支的 `gradle.properties`：

| 键 | 作用 |
|---|---|
| `java_version` | Java 工具链版本（26.x 用 25，1.21.x 用 21） |
| `minecraft_version` | 同时决定构建产物名 `Scannable_Unofficial-Neo<版本>` |
| `minecraft_version_range` / `neo_version_range` / `loader_version_range` | `neoforge.mods.toml` 里的依赖范围 |
| `neo_version` | NeoForge 版本 |
| `parchment_*` | Parchment 映射（26.x 留空） |
| `mod_version` | 模组版本号 |
| `jei_version` / `emi_version` | 留空则该集成包整体不参与编译 |

`build.gradle` 里的 `exclude` 规则依赖 `jei_version` / `emi_version` 是否为空：
留空会排除 `com/starmao/scannable/integration/{jei,emi}/**`。

## 4. 新增一个版本分支

1. 从**结构最接近**的现有分支切出，不要从 `main` 切（`main` 是 1.21.1 基线）：
   `git checkout -b <新版本> <最接近的分支>`
2. 改 `gradle.properties`：`java_version`、`minecraft_version`、`neo_version`、
   `parchment_*`、`mod_version`，以及三个 `*_range`。
3. 改 `.github/workflows/build.yml` 的 `java-version`，使其与 `java_version` 一致；
   并把新分支名加进 `on.push.branches` / `on.pull_request.branches` 列表。
4. 逐个处理该版本移除/改名的 API —— 用 `javap` 核对真实签名，不要凭记忆改：
   已解包的映射 jar 在 `build/moddev/artifacts/*-merged.jar`，例如
   `javap -cp <jar> net.minecraft.core.Registry`。
5. **必须让该分支独立编译通过再合并。** 这是拆分分支的全部意义所在。

> 已知的典型 API 迁移：`Registry#getTag(TagKey)` 在 1.21.2 被移除，替代品是
> `getTagOrEmpty(TagKey)`，返回值从 `Optional<HolderSet.Named<T>>` 变成
> `Iterable<Holder<T>>`（tag 不存在时空迭代）。

## 5. 跨分支同步代码

共享代码是**源码级拷贝**，没有共享模块，因此靠 git 自身能力同步：

- 单个提交：`git cherry-pick <sha>`。路径与目录布局在所有分支上一致，所以
  cherry-pick 能干净落地，前提是这两个分支的 `build.gradle` 没有分叉。
- 持续同步：`git merge <源分支>`；不要用孤立分支（orphan），共享祖先必须保留。
- **`build.gradle` 一旦在某个分支上被改得与其它分支不同，就破坏了这条通道** ——
  这是第 3 节那条硬约定的由来。

## 6. 各分支当前状态备注

- `main`（1.21.1）：活跃开发主线。
- `1.21.2-1.21.3`：已停止维护，同时服务 1.21.2 与 1.21.3。
  `BlockCacheScanFilter` 的 API 迁移已完成（见第 4 节备注）。
- `1.21.4`：已停止维护，同上。
- `26.1.2`：物品扫描结果注入仍是**空桩** —— 见
  `ScanManager.setServerItemResults` 与 `S2CItemScanResult` 的 TODO；
  着色器系统也仍是 stub（`Shaders.java`），等待改写为新的 RenderPipeline API。
  该分支的 7 个编译警告来自 NeoForge 26.x 弃用 `IItemHandler` 系接口，属预期。
