# Compose migration architecture

The module was copied from `zero-symbol-input-view-v2` as an exact Git-blob baseline (182 files).
The new Compose surface is being migrated into independent Compose packages.

## Module boundaries

- `model`, `action`, `data`: typed symbol/action contracts, JSON codecs and persistence compatibility.
- `compose`: public symbol drawer composable and saveable state.
- `compose.manager`, `compose.settings`: Material 3 manager/settings surfaces.
- `com.zerostudio.idea.widget.drawer.compose`: editor-neutral generic bottom-drawer API.
- Existing View-based files in this copied baseline are transitional and are not the Compose public UI. Their View-bound extension contracts are scheduled for replacement by composable slots/renderers.

## Layout policy

The symbol drawer uses a drag handle for vertical expansion and HorizontalPager for horizontal group paging. Indicator reveal is tied to expansion progress. Symbol cells are virtualized by LazyVerticalGrid. Grid column count and symbol cell geometry come from the existing validated settings model. The generic drawer API owns bottom anchoring, drag/settle and content slots but does not depend on an editor object.

Responsive policy will be applied using available Compose constraints: compact widths use a bottom-docked drawer and adaptive symbol density; medium/expanded widths can select bottom or side docking. The API must not assume a phone-sized fixed width.

## Validation gates

1. Preserve V1 JSON import and V2 versioned JSON round-trip.
2. Keep action execution editor-neutral and callback-driven.
3. Preserve stable group selection across data updates.
4. Test drag expansion separately from horizontal pager gestures.
5. Test full symbol labels without ellipsis or forced line truncation.
6. Verify accessibility semantics, touch targets, reduced motion and state restoration.
7. Remove transitional View UI dependencies only after the Compose equivalents and tests cover every public feature.
