# 1.21.4 Notes

NeoForge 21.4.157 introduced breaking API changes that required significant overrides beyond the inherited 1.21.3 set:

## NeoForge API removals
- `net.neoforged.neoforge.client.model.generators.ItemModelProvider` — **removed** from the NeoForge jar. Replaced with a manual `DataProvider` that writes JSON directly.
- `net.neoforged.neoforge.common.data.ExistingFileHelper` — **removed**. No replacement needed for tags or models.
- `GatherDataEvent.includeServer()` / `includeClient()` — **removed**. Event now has `Server` and `Client` subclasses dispatched separately.

## Build script changes (shared `neoforge/build.gradle`)
- `data` run type renamed to `clientData` + `serverData` in 1.21.4.
- Conditional logic added: versions ≤1.21.3 use the old `data` run, 1.21.4 uses `clientData`/`serverData`.

## Override files (7 new files)
- `Scannable.java` — register two GatherDataEvent listeners (Server + Client) instead of one.
- `DataGeneration.java` — split into `onGatherDataServer` / `onGatherDataClient`.
- `ModItemModelProvider.java` — rewritten as plain `DataProvider` (JSON output).
- `ModItemTagProvider.java` — removed `ExistingFileHelper` parameter.
- `ConfigurableEntityScannerModuleContainerScreen.java` — `SpawnEggItem.getType()` now requires `HolderLookup.Provider`.
- `integration/jei/EntityModuleGhostHandler.java` — same SpawnEggItem fix (JEI 20.0.0.4).
- JEI integration stubs from 1.21.3 removed; baseline JEI plugin compiles against `jei-1.21.4-neoforge:20.0.0.4`.

## Versions
- Parchment: `2025.03.23` for `1.21.4` (not `2024.11.10` as previously configured).
- JEI: `20.0.0.4` (not `21.5.0.8` as previously configured).
- NeoForge: `21.4.157`.
