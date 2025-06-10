package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import androidx.lifecycle.viewModelScope
import io.github.toyota32k.binder.combinatorialVisibilityBinding
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.binder.recyclerViewBindingEx
import io.github.toyota32k.binder.textBinding
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.sample.OptionActivity
import io.github.toyota32k.dialog.sample.databinding.DialogOptionSampleBinding
import io.github.toyota32k.dialog.sample.databinding.ItemDialogOptionBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.getViewModel
import io.github.toyota32k.utils.lifecycle.asConstantLiveData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OptionSampleDialog : UtDialogEx() {
    data class Entry(val label: String, val value: String)
    private fun ObservableList<Entry>.put(label: String, value: String): ObservableList<Entry> {
        add(Entry(label, value))
        return this
    }
    class OptionSampleDialogViewModel : UtDialogViewModel() {
        lateinit var systemEntries: List<Entry>
        val entries = ObservableList<Entry>()
        fun setup(vm: OptionActivity.OptionActivityViewModel) {
            settings = vm
            systemEntries = listOf(
                Entry("statusBar", "${vm.showStatusBar.value}"),
                Entry("actionBar", "${vm.showActionBar.value}"),
                Entry("edgeToEdge", "${vm.edgeToEdgeEnabled.value}"),
                Entry("fitSystemWindows", "${vm.fitSystemWindows.value}"),
                Entry("dialogMode", "${vm.isDialogMode.value}"),
                Entry("portraitMargin", "${vm.portraitMarginInfo.value}"),
                Entry("landscapeMargin", "${vm.landscapeMarginInfo.value}"),
            )
        }

        val isBusy = MutableStateFlow(false)
        fun busy() {
            if (isBusy.value) return
            isBusy.value = true
            viewModelScope.launch {
                delay(3000)
                isBusy.value = false
            }
        }
        val commandBusy = LiteUnitCommand(::busy)
        lateinit var settings: OptionActivity.OptionActivityViewModel
    }

    private val viewModel by lazy { getViewModel<OptionSampleDialogViewModel>() }
    private lateinit var controls: DialogOptionSampleBinding

    override fun preCreateBodyView() {
        viewModel.entries.apply {
            clear()
            addAll(viewModel.systemEntries)
            put("-", "")
            put("isDialog", "$isDialog")
            if(!isDialog) {
                put("systemBarOption", "$systemBarOptionOnFragmentMode")
            } else {
                put("hideStatusBar", "$hideStatusBarOnDialogMode")
            }
            put("adjustContentMode", "$adjustContentForKeyboard")
            put("adjustContentsStrategy", "$adjustContentsStrategy")

            put("-", "")

            put("widthOption", "$widthOption")
            put("heightOption", "$heightOption")
            put("gravityOption", "$gravityOption")

            put("-", "")

            put("draggable", "$draggable")
            put("scrollable", "$scrollable")
            put("cancellable", "$cancellable")
            put("noHeader", "$noHeader")
            put("noFooter", "$noFooter")

            put("-", "")

            put("guardColor", "$guardColor")
            put("bodyGuardColor", "$bodyGuardColor")

            put("-", "")

            put("positiveCancellable", "$positiveCancellable")
            put("clipVerticalOnDrag", "$clipVerticalOnDrag")
            put("clipHorizontalOnDrag", "$clipHorizontalOnDrag")
            put("animationEffect", "$animationEffect")
            put("invisibleBuiltInButton", "$invisibleBuiltInButton")
            put("noDialogMargin", "$noDialogMargin")
            put("bodyContainerMargin", "$bodyContainerMargin")
        }
        enableFocusManagement().autoRegister()
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogOptionSampleBinding.inflate(inflater.layoutInflater)
            .apply {
                binder
                    .dialogBodyGuardViewVisibility(viewModel.isBusy, viewModel.settings.progressRingOnBodyGuardView)
                    .conditional(viewModel.settings.progressRingOnHeader.value) {
                        dialogProgressRingOnTitleTitleBarVisibility(viewModel.isBusy)
                    }
                    .bindCommand(viewModel.commandBusy, busyButton)
                    .recyclerViewBindingEx<Entry, ItemDialogOptionBinding>(recyclerView) {
                        list(viewModel.entries)
                        fixedSize(false)
                        inflate { parent-> ItemDialogOptionBinding.inflate(inflater.layoutInflater, parent, false) }
                        bindView { c, itemBinder, _, item ->
                            itemBinder
                                .owner(owner)
                                .combinatorialVisibilityBinding((item.label == "-").asConstantLiveData()) {
                                    straightGone(c.optionLabel,c.optionValue)
                                    inverseGone(c.separator)
                                }
                                .textBinding(c.optionLabel, item.label.asConstantLiveData())
                                .textBinding(c.optionValue, item.value.asConstantLiveData())
                        }
                    }
//                    .recyclerViewBinding(recyclerView, viewModel.entries, itemViewLayoutId=R.layout.item_dialog_option, fixedSize = false) {_, view, item->
//                        if(item.label != "-") {
//                            view.findViewById<TextView>(R.id.option_label).apply {
//                                visibility = View.VISIBLE
//                                text = item.label
//                            }
//                            view.findViewById<TextView>(R.id.option_value).apply {
//                                visibility = View.VISIBLE
//                                text = item.value
//                            }
//                            view.findViewById<View>(R.id.separator).visibility = View.GONE
//                        } else {
//                            view.findViewById<TextView>(R.id.option_label).visibility = View.GONE
//                            view.findViewById<TextView>(R.id.option_value).visibility = View.GONE
//                            view.findViewById<View>(R.id.separator).visibility = View.VISIBLE
//                        }
//                    }
            }
        return controls.root
    }
}