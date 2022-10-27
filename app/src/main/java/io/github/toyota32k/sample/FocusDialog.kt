package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class FocusDialog : UtDialog() {
    init {
        title = "Focus Management"
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        setLimitWidth(400)
        heightOption=HeightOption.COMPACT
//        enableFocusManagement().clear().register(R.id.input_1, R.id.input_2, R.id.input_3, R.id.input_4).setInitialFocus(R.id.input_2)
        enableFocusManagement().autoRegister().setInitialFocus(R.id.input_2).setCustomEditorAction()
    }
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_focus_dialog)
    }
}