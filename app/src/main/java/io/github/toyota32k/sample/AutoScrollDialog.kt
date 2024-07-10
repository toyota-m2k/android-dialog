package io.github.toyota32k.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class AutoScrollDialog : UtDialog() {
    init {
        title="Auto Scroll Test"
        setLimitWidth(400)
        heightOption=HeightOption.AUTO_SCROLL
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    var count:Int = 0
    @SuppressLint("SetTextI18n")
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_auto_scroll_dialog).apply {
            // Add Item ボタンタップで、アイテムを追加
            findViewById<Button>(R.id.add_item_button).setOnClickListener {
                val view = TextView(requireContext()).apply {
                    count++
                    text = "Item $count"
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                (bodyView as LinearLayout).addView(view)
            }
            // Delete Item ボタンタップで、アイテムを削除
            findViewById<Button>(R.id.del_item_button).setOnClickListener {
                val view = (bodyView as LinearLayout).children.last()
                if(view is TextView) {
                    (bodyView as LinearLayout).removeView(view)
                }
            }
        }
    }
}