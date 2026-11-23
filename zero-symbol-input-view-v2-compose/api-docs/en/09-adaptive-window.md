# 09 — Adaptive window API

Package: `...symbolinput.v2.adaptive`.

`AdaptiveWindowObserver` derives a `WindowAdaptiveInfo` snapshot from `BoxWithConstraints`, current density/layout direction and Compose WindowInsets. Recomposition updates the snapshot as layout constraints change. It does not register ViewTree or Activity listeners.

`WindowInsetsMode.SUBTRACT_SYSTEM_INSETS` subtracts safe-drawing insets. `VIEW_BOUNDS_ALREADY_EXCLUDE_INSETS` is a historical enum name: in the Compose API, callers should select it only when the supplied layout bounds already exclude insets; no View is accepted.

The snapshot contains width/height in dp, density, insets and caller-supplied folding-feature data. Available dimensions subtract insets and clamp at zero. Width classes are COMPACT (<600dp), MEDIUM (600–839dp) and EXPANDED (>=840dp).

`AdaptiveLayoutResolver`, `AdaptiveGridResolver` and docking resolvers remain pure policy/geometry APIs. Folding-feature input is supplied by the host; the observer does not install WindowManager or Activity listeners.