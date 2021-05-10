package io.github.toyota32k.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialog

class HogeDialog : UtDialog(), View.OnClickListener {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ほげダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightOption.AUTO_SCROLL
        setLimitWidth(700)
        cancellable = true
        scrollable = true

    }
    override fun createBodyView(savedInstanceState: Bundle?, inflater: LayoutInflater, rootView: ViewGroup): View {
        return inflater.inflate(R.layout.sample_hoge_dialog, rootView, false).apply {
            findViewById<Button>(R.id.first_button).setOnClickListener(this@HogeDialog)
        }
    }

    override fun onClick(v: View?) {
        FugaDialog().show(this, "fuga", IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW)
    }
}