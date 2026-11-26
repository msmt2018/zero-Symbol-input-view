# 16 — Compose testing and release checks

Test domain models, text edit transactions, action execution, codecs, repositories and migrations independently of UI. Test drawer anchors, drag/settle/fling and nested-scroll policies as core state logic. Compose UI tests should cover semantics, clicks, long press, page selection and state restoration.

CI release gates:
- Compose debug/release variants build.
- Compose unit tests pass.
- Instrumentation test sources compile and relevant UI tests run.
- Source/artifact/runtime audits reject legacy View and page-lifecycle dependencies.
- Do not disable checks or exclude real dependencies to make a boundary check pass.

Troubleshooting:
- Stale UI: verify the repository snapshot is emitted into Compose state.
- Pager snaps back: use stable page IDs and one selected-page source of truth.
- Extension not refreshed: verify registry revision and observer handle.
- Repeated writes during recomposition: move side effects to event callbacks or controlled effects.
- Back does not dismiss: the host owns system Back dispatch; the container does not register Activity BackHandler.

Compose is not literally lifecycle-free: Composition, recomposition, remember scopes and effect cancellation exist. The target is independence from traditional Android View and Activity/Fragment page lifecycle.