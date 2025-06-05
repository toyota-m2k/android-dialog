package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.github.toyota32k.binder.clickBinding
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.binder.listViewBinding
import io.github.toyota32k.binder.textBinding
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.sample.R
import io.github.toyota32k.dialog.sample.databinding.DialogFullHeightBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.getViewModel
import io.github.toyota32k.utils.lifecycle.asConstantLiveData

class FullHeightDialog : UtDialogEx() {
    class FullHeightDialogViewModel : UtDialogViewModel() {
        val observableList = ObservableList<String>()   // 本当はViewModelに実装しないといけないんだが。
    }

    override fun preCreateBodyView() {
        title="Full Height"
        heightOption = HeightOption.FULL
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        draggable = true
    }

    lateinit var controls: DialogFullHeightBinding
    val viewModel by lazy { getViewModel<FullHeightDialogViewModel>() }


    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogFullHeightBinding.inflate(inflater.layoutInflater)
        binder
            .owner(this)
            .listViewBinding(controls.listView, viewModel.observableList, R.layout.item_string_list) {
                listBinder, view, text->
                listBinder.textBinding(this@FullHeightDialog, view.findViewById<TextView>(R.id.text_view), text.asConstantLiveData())
            }
            .clickBinding(controls.addItemButton) {
                viewModel.observableList.add("Item - ${viewModel.observableList.size+1}")
            }
            .clickBinding(controls.delItemButton) {
                viewModel.observableList.removeLastOrNull()
            }
        return controls.root
    }
}