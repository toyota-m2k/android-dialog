package io.github.toyota32k.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHelper

class FugaDialog : UtDialog(), View.OnClickListener {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ふがダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.COMPACT
        cancellable = true
        scrollable = false
    }
    override fun createBodyView(
        savedInstanceState: Bundle?,
        inflater: IViewInflater
    ): View {
        return inflater.inflate(R.layout.sample_fuga_dialog).apply {
            findViewById<Button>(R.id.first_button).setOnClickListener(this@FugaDialog)
        }
    }

    override fun onClick(v: View?) {
        UtDialogHelper.cancelAllDialogs(requireActivity())
    }
}