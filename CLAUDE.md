# 项目规则
- **停止维护版本** - 本项目已停止维护1.21.2、1.21.3、1.21.4版本，不要调整这些版本

## 文件架构
- **改目录结构/新增构建目标前先读 `ARCHITECTURE.md`** — 那里写明了多版本覆盖层的合并顺序、版本别名、以及 26.x 覆盖层中"占位遮蔽类不可删"的规则。
- 主源码树在 `neoforge/src/main/`（= 1.21.1 基线 + 全版本共享代码），版本覆盖层在 `versions/`；版本号与依赖版本的唯一真相来源是 `gradle/versions.gradle` 的 `supportedVersions`。
- 仓库根目录不应出现 `bin/`、`net/`、`assets/`、`META-INF/` 等本地残留；`.gitignore` 中这类规则**必须带前导斜杠**，否则会误伤 `versions/*/…/resources/assets/` 下的真实资源。

## 文件读取
1. **`smart_read` 优先于 `Read`** — 大文件用 `smart_read`（默认只输出符号轮廓），需要看具体函数时用 `focus` 展开。
2. **`Agent` 隔离搜索** — 独立的查找（查注册 ID、搜 API 用法）交给 Agent 工具，结论带回主会话即可，不膨胀上下文。
3. **`semantic_search` > `Grep` + `Read`** — 找"类似实现"时语义搜索直接定位到相关函数/类，避免 grep 遍历+逐个读文件。
4. **高频小改** → `smart_read` 定位 + `Edit` 修改。
5. **查文档** → `Agent` + `WebFetch`，不污染主上下文。

## 语言规范
- **所有回复必须使用中文**（思考过程、分析、回复均使用中文）

## Git 规范
- **禁止自动提交** - 不要自动执行 `git commit` 或 `git push`
- **禁止自动创建分支** - 不要自动执行 `git checkout -b` 或创建新分支
- 所有 Git 操作必须由用户显式要求并确认后执行
