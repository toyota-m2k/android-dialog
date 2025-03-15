package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import io.github.toyota32k.binder.editTextBinding
import io.github.toyota32k.binder.enableBinding
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.sample.databinding.DialogCompactBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.getViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CompactDialog : UtDialogEx() {
    class CompactDialogViewModel : UtDialogViewModel() {
        val yourName = MutableStateFlow("")
    }

    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption=HeightOption.COMPACT
        widthOption=WidthOption.LIMIT(400)
        gravityOption = GravityOption.CENTER
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        cancellable = false
        draggable = true
        enableFocusManagement().autoRegister()
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        return controls.root
    }
}
