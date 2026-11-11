package com.zerostudio.idea.widget.editor.symbolinput.v2.model.legacy

import com.google.gson.annotations.SerializedName

/** Original V1 symbol-group JSON model. */
data class SymbolGroup(
    @SerializedName("n") var name: String = "",
    @SerializedName("d") var items: MutableList<SymbolItem> = mutableListOf()
)

/** Original V1 symbol-item JSON model. */
data class SymbolItem(
    @SerializedName("a") var shortAction: Int = 0,
    @SerializedName("b") var display: String = "",
    @SerializedName("c") var shortText: String? = null,
    @SerializedName("d") var longAction: Int? = null,
    @SerializedName("e") var longText: String? = null
)
