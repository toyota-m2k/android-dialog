package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class FillDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.FULL
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    private inner class ListAdapter: BaseAdapter() {
        val items:MutableList<String> = mutableListOf()

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return (convertView as? TextView ?: TextView(requireContext())).also { it.text = items[position] }
        }
        fun add(item:String) {
            items.add(item)
            notifyDataSetChanged()
        }
        fun remove() {
            if(items.size==0) return
            items.removeLast()
            notifyDataSetChanged()
        }
    }

    var count:Int = 0

    lateinit var listView: ListView
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_fill_dialog).apply {
            listView = findViewById(R.id.list_view)
            listView.adapter = ListAdapter()

            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                count++
                (listView.adapter as ListAdapter).add("Item - $count")
            }
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                (listView.adapter as ListAdapter).remove()
            }
        }
    }
}