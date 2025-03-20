package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class FocusDialog : UtDialog() {
    override fun preCreateBodyView() {
        title = "Focus Management"
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        enableFocusManagement(true).autoRegister().setInitialFocus(R.id.input_2).setCustomEditorAction()
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_focus_dialog)
    }
}