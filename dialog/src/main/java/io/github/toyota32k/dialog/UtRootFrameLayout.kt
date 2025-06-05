package io.github.toyota32k.dialog

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.FrameLayout
import io.github.toyota32k.utils.WeakReferenceDelegate

class UtRootFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var ownerDialog by WeakReferenceDelegate<UtDialog>()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dialog = ownerDialog
        if (dialog!=null && event.action == KeyEvent.ACTION_DOWN) {
            logger.debug { "key event consumed by dialog: ${event.keyCode} (${event}) : ${dialog.javaClass.simpleName}" }
            if (dialog.onKeyDown(event.keyCode, event)) {
                // ダイアログがイベントを処理した
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        val logger = UtDialogBase.logger
    }
}
