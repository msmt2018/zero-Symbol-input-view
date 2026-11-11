package com.zerostudio.idea.widget.editor.symbolinput.v2.data

/** Action IDs and ordering copied from V1 res/values/arrays.xml. */
object LegacySymbolActionCatalog {
    val values = intArrayOf(
        0, 3, 4, 7, 8, 9, 11, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56
    )
    val labels = listOf(
        "插入文本", "删除行", "清空行", "转为大写", "转为小写", "增加缩进 (Tab)",
        "注释/取消注释", "到行首", "到行尾", "光标左移", "光标右移", "光标上移",
        "光标下移", "打开快捷功能栏管理", "到文件开头", "到文件结尾", "全选",
        "复制", "剪切", "粘贴", "格式化代码", "复制行", "剪切行", "重复行",
        "替换行", "回车键", "Tab 键", "Shift + Tab",
        "F1 键", "F2 键", "F3 键", "F4 键", "F5 键", "F6 键",
        "F7 键", "F8 键", "F9 键", "F10 键", "F11 键", "F12 键",
        "Ctrl + C", "Ctrl + X", "Ctrl + V", "撤销", "重做"
    )
    init { check(values.size == labels.size) }
}
