package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.utils.setLayoutHeight

class CustomDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.CUSTOM
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    private inner class ListAdapter:BaseAdapter() {
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
                updateCustomHeight()
            }
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                (listView.adapter as ListAdapter).remove()
                updateCustomHeight()
            }
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