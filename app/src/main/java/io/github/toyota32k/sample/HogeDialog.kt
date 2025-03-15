package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.*

class HogeDialog : UtDialog(), View.OnClickListener,
    IUtDialogHost {
    init {
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        title="ほげダイアログ"
        gravityOption = GravityOption.RIGHT_TOP
        heightOption = HeightFlag.AUTO_SCROLL
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
            findViewById<Button>(R.id.third_button).setOnClickListener { leftButton.isEnabled = !leftButton.isEnabled }
            findViewById<Button>(R.id.forth_button).setOnClickListener { rightButton.isEnabled = !rightButton.isEnabled }
            findViewById<Button>(R.id.fifth_button).setOnClickListener { progressRingOnTitleBar.visibility = if(progressRingOnTitleBar.visibility==View.VISIBLE) View.INVISIBLE else View.VISIBLE }
        }
    }

    override fun onClick(v: View?) {
        if(v?.id == R.id.first_button) {
            FugaDialog().apply{parentVisibilityOption= ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE}.show(this.requireActivity(), "fuga")
        } else {
            PiyoDialog().apply{parentVisibilityOption= ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE}.show(this.requireActivity(), "piyo")
        }
    }

    private fun onFugaDialogCompleted(dlg: IUtDialog) {
        if(dlg.status.ok) {
            complete()
        }
    }
    private fun onPiyoDialogCompleted(dlg: IUtDialog) {
        if(dlg.status.ok) {
            complete()
        }
    }

    private val dialogHostManager = UtDialogHostManager()
    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return dialogHostManager[tag]
    }
}