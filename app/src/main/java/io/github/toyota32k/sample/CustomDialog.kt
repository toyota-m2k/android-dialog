package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import io.github.toyota32k.R
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.ListViewBinding
import io.github.toyota32k.binder.command.Command
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.utils.setLayoutHeight

class CustomDialog : UtDialog() {
    override fun preCreateBodyView() {
        title="Custom Dialog"
        heightOption= HeightOption.CUSTOM
        widthOption = WidthOption.COMPACT
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
    }

//    private class ListAdapter(val context: Context): ListViewAdapter<String>() {
//        override fun createItemView(parent: ViewGroup?): View {
//            return TextView(context)
//        }
//
//        override fun updateItemView(itemView: View, position: Int) {
//            (itemView as TextView).text = this[position]
//        }
//    }

    var count:Int = 0

    lateinit var listView: ListView
    val binder = Binder()
    val observableList = ObservableList<String>()   // 本当はViewModelに実装しないといけないんだが。

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_fill_dialog).apply {
            listView = findViewById(R.id.list_view)

            binder.register (
                ListViewBinding.create(listView, observableList, R.layout.string_list_item) { _, view, text->
                    view.findViewById<TextView>(R.id.text_view).text = text
                },
                Command().connectAndBind(this@CustomDialog, findViewById(R.id.add_item_button)) {
                    count++
                    observableList.add("Item - $count")
                    updateCustomHeight()
                },
                Command().connectAndBind(this@CustomDialog, findViewById(R.id.del_item_button)) {
                    observableList.removeLastOrNull()
                    updateCustomHeight()
                },
                Command().connectAndBind(this@CustomDialog, findViewById(R.id.sub_dialog_button)) {
                    SubCompactDialog.open(isDialog)
                },
            )
        }
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
        val calculatedLvHeight = listView.calcFixedContentHeight()
        val remainHeight = currentBodyHeight-listView.height    // == listviewを除く、その他のパーツの高さ合計
        val maxLvHeight = maxContainerHeight - remainHeight     // listViewの最大高さ
        return if(calculatedLvHeight>=maxLvHeight) {
            // リストビューの中身が、最大高さを越える --> 最大高さを採用
            listView.setLayoutHeight(maxLvHeight)
            maxContainerHeight
        } else {
            // リストビューの中身が、最大高さより小さい --> リストビューの中身のサイズを採用
            listView.setLayoutHeight(calculatedLvHeight)
            calculatedLvHeight + remainHeight
        }
    }

    fun ListView.calcFixedContentHeight():Int {
        val listAdapter = adapter ?: return 0
        if(count==0) return 0
        val listItem = listAdapter.getView(0, null, this)
        listItem.measure(0, 0)
        val itemHeight = listItem.measuredHeight
        return itemHeight * count + dividerHeight * (count-1)
    }
}