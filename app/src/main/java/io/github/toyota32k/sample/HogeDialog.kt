package io.github.toyota32k.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class HogeDialog : UtDialog() {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ほげダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightOption.COMPACT
        setLimitWidth(700)
        cancellable = true
        scrollable = false

    }
    override fun createBodyView(savedInstanceState: Bundle?, inflater: LayoutInflater, rootView: ViewGroup): View {
        return inflater.inflate(R.layout.sample_hoge_dialog, rootView, false)
    }
}