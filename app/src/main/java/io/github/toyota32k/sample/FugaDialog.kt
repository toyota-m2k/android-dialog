package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHelper

class FugaDialog : UtDialog() {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ふがダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.COMPACT
        cancellable = false
        scrollable = false
        draggable = true
    }
    override fun createBodyView(
        savedInstanceState: Bundle?,
        inflater: IViewInflater
    ): View {
        return inflater.inflate(R.layout.sample_fuga_dialog).apply {
        }
    }

}