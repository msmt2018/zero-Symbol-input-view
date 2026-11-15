# 05 — Repository, Management and Migration

## Repository contract

`SymbolGroupRepository` provides `loadGroups()`, `saveGroups(groups)`, `isLegacyMigrationComplete()` and `markLegacyMigrationComplete()`. A successful save should mean a subsequent load observes the saved snapshot. The default SharedPreferences implementation uses the V2 preference namespace and versioned JSON.

## SharedPreferences repository

The default repository stores groups under the V2 key `groups_json_v2` and migration state under `legacy_migration_complete`. It can normalize supported older formats. If decoding fails, it attempts to preserve the damaged JSON in a backup key before returning an empty list. This is recovery behavior, not proof that the original data was repaired; notify users and provide export/reset options.

It does not write back to the V1 `advanced_symbol_prefs` namespace.

## SymbolGroupManager

The manager is a command model above the repository. A successful command validates a new snapshot, persists it once, then publishes the new in-memory snapshot. On write failure, the previous snapshot remains.

Operations include:
- reload and replace all groups;
- add, rename, duplicate, remove and reorder groups;
- add, update, remove, move and copy symbols;
- batch remove/move/copy and reorder by stable IDs.

Invalid IDs, missing groups/items and invalid indices are rejected according to each method contract. ID generators can be injected for tests; generated IDs must be unique. The manager retries a generated ID a bounded number of times and then fails rather than looping forever.

## Defaults and codecs

- `SymbolDrawerDefaults`: creates the default group set.
- `SymbolGroupStorageCodec`: encodes/decodes persisted group data.
- `SymbolGroupBundleCodec`: encodes/decodes import/export bundles.
- Import must validate the complete payload before replacing current data.

## V1 migration

Migration is read-only against legacy preferences and writes to the independent V2 repository. It should run only when the V2 destination is empty and migration is not already complete. Unknown legacy action IDs must not be guessed or mapped to unrelated actions. Keep V1 IDs and payload semantics stable.

## Custom repository guidance

Use a transaction when persisting the complete snapshot. Return defensive snapshots, avoid exposing mutable database entities, and make write failures observable. Test empty stores, corrupt JSON, interrupted writes, already-completed migration and non-empty V2 destinations.
