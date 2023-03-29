package io.github.toyota32k.dialog

import android.app.AlertDialog
import android.view.View

class UtAlertDialog : UtMessageBox() {
    private var fnCreateView:((UtAlertDialog)->View)? = null
    private var fnCreateBuilder:((UtAlertDialog)->AlertDialog.Builder)? = null

    fun viewCreator(fn:(UtAlertDialog)->View):UtAlertDialog {
        fnCreateView = fn
        return this
    }
    fun builderCreator(fn:(UtAlertDialog)->AlertDialog.Builder):UtAlertDialog {
        fnCreateBuilder = fn
        return this
    }

    override fun createAlertBuilder(): AlertDialog.Builder {
        return fnCreateBuilder?.invoke(this) ?: super.getAlertBuilder()
    }

    override fun getAlertBuilder(): AlertDialog.Builder {
        val builder = super.getAlertBuilder()
        fnCreateView?.apply {
            builder.setView(invoke(this@UtAlertDialog))
        }
        return builder
    }
}