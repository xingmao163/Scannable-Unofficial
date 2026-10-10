# Scannable Unofficial - Changelog & Pull Request Details

## Overview
This is a massive update that ports the mod to **Minecraft 1.21.4 (NeoForge 26.3)**, fully adapting it to Mojang's new `RenderPearl` graphics engine. It also fixes several critical rendering bugs caused by the transition, introduces a highly requested configuration option for label placement, and adds complete Russian localization.

---

## 🚀 1. Porting to Minecraft 1.21.4 (NeoForge 26.3)

### Build System Fixes (Gradle)
* **NeoForge ModDev Bump:** Updated `net.neoforged.moddev` from `2.0.76` to `2.0.148` in `libs.versions.toml` to fix the `cannot override contents()` decompilation crash on Java 25.
* **DataGen Cleanup:** Removed the empty, legacy `data { ... }` run configuration from `build.gradle`, as NeoForge 1.21+ strictly requires separated `clientData` and `serverData` runs, which caused `Trying to prepare unknown run: data` errors.

### RenderPearl Engine Adaptation
Mojang replaced `Blaze3D` with `RenderPearl`, which required a complete rewrite of the mod's rendering pipeline (resolving ~100 compilation errors).
* **Migrated to `SubmitNodeCollector`:** The old `MultiBufferSource.immediate()` (which allowed manual buffer building and `endBatch()`) was removed from Minecraft. Completely rewrote all rendering methods in providers (e.g., `ScanResultProviderBlock`) to use the new callback system: `bufferSource.submitCustomGeometry(poseStack, renderType, (pose, fill) -> { ... })`.
* **Updated `RenderType` & Shaders:** Migrated `RenderPipeline`, `BlendFunction`, `ColorTargetState`, and `CompareOp` to the new `com.mojang.renderpearl.api.pipeline.*` packages in `ScanResultRenderType.java`. Replaced deprecated `.withUniform(...)` calls with `.withBindGroupLayout(BindGroupLayouts.PROJECTION)`.
* **Event Hook Changes:** `RenderLevelStageEvent.AfterLevel` no longer provides access to geometry submission. Migrated block and item highlight rendering (`ScanManager.renderLevel`) to the new `SubmitCustomGeometryEvent`. The fullscreen "scan wave" effect remains in `AfterLevel` as it requires direct `RenderPass` access.
* **Camera & RenderTarget:** Updated `mc.getMainRenderTarget()` to `mc.gameRenderer.mainRenderTarget()` and `getMainCamera()` to `mainCamera()` across `ScannerRenderer.java`. Updated `PoseStack` transformations (replaced `mulPose` with `rotate`).

---

## 🛠️ 2. Critical Rendering Bug Fixes

### Fixed the "Infinite Vertical Stripe" Artifact
* **The Bug:** After porting to `SubmitNodeCollector`, the text background and scan icons would stretch vertically across the entire screen. This happened because the mutable `PoseStack` object was passed into the deferred execution lambda. By the time the game drew the vertices at the end of the frame, the `PoseStack` had been reset, destroying the scale and translation coordinates.
* **The Fix:** Rewrote `drawQuad` and `drawTexturedQuad` in `AbstractScanResultProvider.java` to accept the pre-captured, immutable `PoseStack.Pose` directly, guaranteeing vertices are drawn exactly as intended at submission time.

### Fixed Icon and Background Z-Fighting
* **The Bug:** The scan icon was often hidden behind the semi-transparent black background of the text label because both were drawn at the exact same depth (`Z = 0`) with depth-testing disabled.
* **The Fix:** Applied a depth bias to the icon's geometry in `AbstractScanResultProvider.java`, shifting it closer to the camera (`Z = -0.01f`). Since `SubmitNodeCollector` sorts translucent overlays by camera distance, this mathematically guarantees the icon is always drawn *on top* of the background.

### Hardened JOML Matrix Inversion against View Bobbing
* **The Bug:** When view bobbing is active, vanilla Minecraft applies affine translations to the camera's projection matrix without clearing JOML's internal `PROPERTY_PERSPECTIVE` flag. When the scanner attempted to `.invert()` this matrix, JOML would take an optimization fast-path that ignored the bobbing translation, scrambling the unprojected coordinates and breaking the shader effect.
* **The Fix:** Replaced direct matrix cloning (`new Matrix4f(src)`) with element-by-element assignment (`new Matrix4f().set(...)`) in `ScannerRenderer.java`. This safely strips all hidden flags, forcing JOML to use the generic and mathematically correct inversion algorithm.

---

## ✨ 3. Features & Quality of Life

### Added "Hover Text At Block" Configuration Toggle
* Added `HOVER_TEXT_AT_BLOCK` setting to `ClientConfig.java` and integrated it into `ScanResultProviderBlock.java`.
* **Behavior:** When enabled (True), looking at any block in a highlighted cluster will draw the tooltip label exactly over *that specific block*. When disabled (False), the label remains anchored to the geometric center of the cluster.

---

## 🌍 4. Localization

* **Fixed `en_us.json` Typos:** Corrected a misspelled translation key (`scannable_unoffcial` -> `scannable_unofficial`) and removed an accidental trailing dot that caused the config menu to display raw translation keys.
* **Added Full Russian Localization (`ru_ru.json`):** Added a complete, context-aware Russian translation file covering all 180+ keys in the mod (items, tooltips, configuration menus, and debug settings).
