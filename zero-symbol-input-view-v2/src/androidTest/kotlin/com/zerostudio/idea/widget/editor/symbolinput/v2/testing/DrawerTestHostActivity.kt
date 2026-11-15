package com.zerostudio.idea.widget.editor.symbolinput.v2.testing

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

class DrawerTestHostActivity : Activity() {
    lateinit var content: FrameLayout
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = FrameLayout(this)
        setContentView(content)
    }
}
