package io.github.toyota32k.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.*

class HogeDialog : UtDialog(), View.OnClickListener, IUtDialogHost {
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
        return inflater.inflate(R.layout.sample_hoge_dialog).apply {
            findViewById<Button>(R.id.first_button).setOnClickListener(this@HogeDialog)
        }
    }

    override fun onClick(v: View?) {
        dialogHostManager["fuga"] = this::onFugaDialogCompleted
        FugaDialog().show(this, "fuga", IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW)
    }

    fun onFugaDialogCompleted(dlg:IUtDialog) {
        dialogHostManager["fuga"] = null
        if(dlg.status.ok) {
            complete()
        }
    }

    val dialogHostManager = UtDialogHostManager()
    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return dialogHostManager[tag]
    }
}