package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import io.github.toyota32k.binder.clickBinding
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.sample.R

class ThemeColorDialog : UtDialogEx() {
    override fun preCreateBodyView() {
        noHeader = true
        noFooter = true
        widthOption = WidthOption.FULL
        heightOption = HeightOption.FULL
        scrollable = true
        cancellable = true
        guardColor = UtDialog.GuardColor.THEME_DIM
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return  inflater.inflate(R.layout.dialog_theme_colors).also { view ->
            binder.clickBinding(view) { onPositive() }
        }
    }
}