package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.*

class HogeDialog : io.github.toyota32k.dialog.UtDialog(), View.OnClickListener,
    io.github.toyota32k.dialog.IUtDialogHost {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ほげダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightOption.AUTO_SCROLL
        setLimitWidth(700)
        cancellable = true
        scrollable = true

    }
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        dialogHostManager["fuga"] = this::onFugaDialogCompleted
        dialogHostManager["piyo"] = this::onPiyoDialogCompleted
        return inflater.inflate(R.layout.sample_hoge_dialog).apply {
            findViewById<Button>(R.id.first_button).setOnClickListener(this@HogeDialog)
            findViewById<Button>(R.id.second_button).setOnClickListener(this@HogeDialog)
        }
    }

    override fun onClick(v: View?) {
        if(v?.id == R.id.first_button) {
            FugaDialog().apply{parentVisibilityOption= io.github.toyota32k.dialog.IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE}.show(this, "fuga")
        } else {
            PiyoDialog().apply{parentVisibilityOption= io.github.toyota32k.dialog.IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE}.show(this, "piyo")
        }
    }

    fun onFugaDialogCompleted(dlg: io.github.toyota32k.dialog.IUtDialog) {
        if(dlg.status.ok) {
            complete()
        }
    }
    fun onPiyoDialogCompleted(dlg: io.github.toyota32k.dialog.IUtDialog) {
        if(dlg.status.ok) {
            complete()
        }
    }

    val dialogHostManager = io.github.toyota32k.dialog.UtDialogHostManager()
    override fun queryDialogResultReceptor(tag: String): io.github.toyota32k.dialog.IUtDialogResultReceptor? {
        return dialogHostManager[tag]
    }
}