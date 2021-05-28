package io.github.toyota32k.sample

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import io.github.toyota32k.R
import io.github.toyota32k.bindit.list.ListViewAdapter
import io.github.toyota32k.dialog.UtDialog

class FillDialog : io.github.toyota32k.dialog.UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.FULL
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    private class ListAdapter(val context: Context): ListViewAdapter<String>() {
        override fun createItemView(parent: ViewGroup?): View {
            return TextView(context)
        }

        override fun updateItemView(itemView: View, position: Int) {
            (itemView as TextView).text = this[position]
        }
    }

    var count:Int = 0

    lateinit var listView: ListView

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_fill_dialog).apply {
            listView = findViewById(R.id.list_view)
            listView.adapter = ListAdapter(requireContext())

            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                count++
                (listView.adapter as ListAdapter).add("Item - $count")
            }
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                (listView.adapter as ListAdapter).removeLastOrNull()
            }
        }
    }
}