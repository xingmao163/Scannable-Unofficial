# 1.21.2 Notes

- Added a `ScanManager` override because `RenderLevelStageEvent#getModelViewMatrix()` is identity in 1.21.2 during the scan render path, while 1.21.1 provided the expected camera rotation matrix.
- The override reconstructs the camera rotation matrix from `Minecraft.getInstance().gameRenderer.getMainCamera().rotation()` so frustum culling and scan marker rendering keep matching the active camera.
- This cannot be moved back to the shared baseline until the 1.21.2+ render matrix behavior is aligned with the 1.21.1 path.
