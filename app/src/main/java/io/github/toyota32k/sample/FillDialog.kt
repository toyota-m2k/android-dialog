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

class FillDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.FULL
//        guardColor = Color.argb(0xD0, 0xFF, 0xFF, 0xFF)
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

//    private class ListAdapter(val context: Context): ListViewAdapter<String>() {
//        override fun createItemView(parent: ViewGroup): View {
//            return TextView(context)
//        }
//
//        override fun updateItemView(itemView: View, position: Int) {
//            (itemView as TextView).text = this[position]
//        }
//    }

    private var count:Int = 0

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
                Command().connectAndBind(this@FillDialog, findViewById(R.id.add_item_button)) {
                    count++
                    observableList.add("Item - $count")
                },
                Command().connectAndBind(this@FillDialog, findViewById(R.id.del_item_button)) {
                    observableList.removeLastOrNull()
                },
                Command().connectAndBind(this@FillDialog, findViewById(R.id.sub_dialog_button)) {
                    SubCompactDialog.open()
                },
            )
        }
    }
}