# 13 — Public packages and editor-neutral actions

The Compose artifact separates editor-neutral contracts from Compose UI. Text document edits, action IDs, action handlers and execution hosts belong to Kotlin APIs that do not require an Activity, Fragment, View or LifecycleOwner.

The text-document contract uses UTF-16 offsets to match Kotlin/Java string indexing. Validate edit ranges and conflicts before mutation; apply multi-edits in descending offset order and preserve selection/caret metadata according to the transaction contract.

Extension registry snapshots and revision notifications are data APIs. Consumers explicitly own and close observer handles; there is no automatic Fragment view lifecycle binding.

Kotlin `typealias` facade declarations are Kotlin source aliases only. They do not create Java-visible classes. Java ABI compatibility requires concrete public facade declarations and Java compilation tests.