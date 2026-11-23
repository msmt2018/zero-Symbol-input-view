# 06 — Compose settings

`SymbolDrawerSettings` is an immutable settings snapshot. Use `copy(...)` to create updates.

## Persistence

`SymbolDrawerSettingsRepository` provides `load()`, `save(settings)`, `reset()` and `hasStoredSettings()`. `SharedPreferencesSymbolDrawerSettingsRepository` is the Android persistence implementation and should be created outside Composable bodies.

`SymbolDrawerSettingsCodec` provides `encode`, `decode` and strict backup decoding via `decodeStrict`. Strict decoding rejects unknown/missing fields and invalid schema versions rather than silently discarding data.

## Compose screen

```kotlin
@Composable
fun SymbolDrawerSettingsScreen(
    settings: SymbolDrawerSettings,
    onSettingsChange: (SymbolDrawerSettings) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null
)
```

The screen edits local Compose state and emits immutable snapshots. The host persists successful changes and applies them to the drawer. The screen does not accept Context, Activity, Fragment, View or LifecycleOwner.