package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class FugaDialog : UtDialog() {
    override fun preCreateBodyView() {
        title="ふがダイアログ"
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.COMPACT
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        gravityOption = GravityOption.RIGHT_TOP
        cancellable = false
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