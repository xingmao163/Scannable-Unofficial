# 1.21.3 Notes

- Inherited all overrides from 1.21.2, as NeoForge groups 1.21.2 and 1.21.3 under the same API version (docs.neoforged.net/docs/1.21.3/).
- The `RenderLevelStageEvent#getModelViewMatrix()` identity matrix issue from 1.21.2 persists in 1.21.3, requiring the same ScanManager override that reconstructs the camera rotation matrix.
- All other 1.21.2 client, item, JEI, recipe, and shader overrides carry over unchanged.
- Parchment mappings differ (2024.11.10 for 1.21.3 vs 2024.11.17 for 1.21.1 baseline), but the same override source compiles cleanly against `neo_version = 21.3.96`.

Files in this directory are exact copies from `versions/1.21.2/`. If any are later consolidated back to the baseline, both 1.21.2 and 1.21.3 should be updated in lockstep.
