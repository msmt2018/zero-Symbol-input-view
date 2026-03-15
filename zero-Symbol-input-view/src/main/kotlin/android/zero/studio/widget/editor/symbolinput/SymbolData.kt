package android.zero.studio.widget.editor.symbolinput

import com.google.gson.annotations.SerializedName


// 符号分组数据模型
data class SymbolGroup(
    @SerializedName("n") var name: String = "",
    @SerializedName("d") var items: MutableList<SymbolItem> = mutableListOf()
)

// 具体符号属性数据模型
data class SymbolItem(
    @SerializedName("a") var shortAction: Int = 0,
    @SerializedName("b") var display: String = "",
    @SerializedName("c") var shortText: String? = null,
    @SerializedName("d") var longAction: Int? = null,
    @SerializedName("e") var longText: String? = null
)
