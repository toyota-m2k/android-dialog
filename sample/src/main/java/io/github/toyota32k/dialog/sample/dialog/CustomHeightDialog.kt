package io.github.toyota32k.dialog.sample.dialog

import android.os.Bundle
import android.view.View
import android.widget.ListView
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
import io.github.toyota32k.utils.asConstantLiveData
import io.github.toyota32k.utils.setLayoutHeight

class CustomHeightDialog : UtDialogEx() {
    class CustomHeightDialogViewModel : UtDialogViewModel() {
        val observableList = ObservableList<String>()   // 本当はViewModelに実装しないといけないんだが。
    }

    override fun preCreateBodyView() {
        title="Custom Dialog"
        heightOption = HeightOption.CUSTOM
        widthOption = WidthOption.LIMIT(400)
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    lateinit var controls: DialogFullHeightBinding  // レイアウトはFullHeightDialogと同じ
    val viewModel by lazy { getViewModel<CustomHeightDialogViewModel>() }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogFullHeightBinding.inflate(inflater.layoutInflater)
        binder
            .owner(this)
            .listViewBinding(controls.listView, viewModel.observableList, R.layout.item_string_list) {
                    listBinder, view, text->
                listBinder.textBinding(this@CustomHeightDialog, view.findViewById<TextView>(R.id.text_view), text.asConstantLiveData())
            }
            .clickBinding(controls.addItemButton) {
                viewModel.observableList.add("Item - ${viewModel.observableList.size+1}")
                updateCustomHeight()
            }
            .clickBinding(controls.delItemButton) {
                viewModel.observableList.removeLastOrNull()
                updateCustomHeight()
            }
        return controls.root
    }

    /**
     * リストビュー内のアイテムが少ないときは、できるだけコンパクトに、
     * アイテムが増えてきたら、画面いっぱいまで拡張し、それ以降はスクロールさせる。
     */
    override fun calcCustomContainerHeight(
        currentBodyHeight: Int,
        currentContainerHeight: Int,
        maxContainerHeight: Int
    ): Int {
        val calculatedLvHeight = controls.listView.calcFixedContentHeight()
        val remainHeight = currentBodyHeight-controls.listView.height    // == listviewを除く、その他のパーツの高さ合計
        val maxLvHeight = maxContainerHeight - remainHeight     // listViewの最大高さ
        return if(calculatedLvHeight>=maxLvHeight) {
            // リストビューの中身が、最大高さを越える --> 最大高さを採用
            controls.listView.setLayoutHeight(maxLvHeight)
            maxContainerHeight
        } else {
            // リストビューの中身が、最大高さより小さい --> リストビューの中身のサイズを採用
            controls.listView.setLayoutHeight(calculatedLvHeight)
            calculatedLvHeight + remainHeight
        }
    }

    private fun ListView.calcFixedContentHeight():Int {
        val listAdapter = adapter ?: return 0
        if(count==0) return 0
        val listItem = listAdapter.getView(0, null, this)
        listItem.measure(0, 0)
        val itemHeight = listItem.measuredHeight
        return itemHeight * count + dividerHeight * (count-1)
    }
}