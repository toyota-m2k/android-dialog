package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.viewModelScope
import io.github.toyota32k.binder.clickBinding
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.sample.OptionActivity
import io.github.toyota32k.dialog.sample.databinding.DialogAutoScrollBinding
import io.github.toyota32k.dialog.sample.databinding.DialogOptionSampleBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.getViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OptionSampleDialog : UtDialogEx() {
    class OptionSampleDialogViewModel : UtDialogViewModel() {
        lateinit var settings: OptionActivity.OptionActivityViewModel
        fun setup(settings:OptionActivity.OptionActivityViewModel) {
            this.settings = settings
        }
        var count = 3

        val isBusy = MutableStateFlow(false)
        fun busy() {
            if(isBusy.value) return
            isBusy.value = true
            viewModelScope.launch {
                delay(3000)
                isBusy.value = false
            }
        }
    }

    private val viewModel by lazy { getViewModel<OptionSampleDialogViewModel>() }
    private lateinit var controls: DialogOptionSampleBinding

    override fun preCreateBodyView() {
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogOptionSampleBinding.inflate(inflater.layoutInflater)

        fun addItemToView(index:Int) {
            val view = TextView(requireContext()).apply {
                text = "Item - $index"
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            controls.root.addView(view, 1)
        }
        for(i in 0 until viewModel.count) {
            addItemToView(i+1)
        }
        return controls.apply {
            binder
                .clickBinding(addItemButton) {
                    viewModel.count++
                    addItemToView(viewModel.count)
                }
                .clickBinding(delItemButton) {
                    val view = controls.root.children.lastOrNull { it is TextView } ?: return@clickBinding
                    controls.root.removeView(view)
                    viewModel.count--
                }
                .clickBinding(closeButton) {
                    onPositive()
                }
                .clickBinding(busyButton) {
                    viewModel.busy()
                }
                .dialogBodyGuardViewVisibility(viewModel.isBusy, viewModel.settings.progressRingOnBodyGuardView)
        }.root
    }
}