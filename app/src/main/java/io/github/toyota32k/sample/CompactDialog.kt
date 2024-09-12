package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.EditText
import io.github.toyota32k.R
import io.github.toyota32k.dialog.UtDialog

class CompactDialog : UtDialog() {
    init {
        title="Compact Dialog"
        heightOption=HeightOption.COMPACT
        widthOption=WidthOption.COMPACT
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    // 呼び出し元から、結果（このダイアログだと入力された名前）を取り出せるようにするためのプロパティ
    var name:String? = null

    /**
     * ダイアログの中身 (bodyView)を作成して返す。
     */
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_compact_dialog)
    }

    /**
     * Doneボタンがタップされたときの処理
     * 呼び出し元から結果が参照できるように、入力された内容をプロパティとして取り出しておく。
     */
    override fun onPositive() {
        name = dialog?.findViewById<EditText>(R.id.name_input)?.text?.toString() ?: ""
        super.onPositive()
    }
}