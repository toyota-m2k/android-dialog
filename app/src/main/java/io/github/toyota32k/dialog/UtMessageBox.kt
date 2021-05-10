@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.R

open class UtMessageBox : UtDialogBase(), DialogInterface.OnClickListener {
    var title:String? by UtDialogArgumentDelegate()
    var message:String? by UtDialogArgumentDelegate()
    var okLabel:String? by UtDialogArgumentDelegate()
    var cancelLabel:String? by UtDialogArgumentDelegate()
    var otherLabel:String? by UtDialogArgumentDelegate()

    protected open fun getAlertBuilder():AlertDialog.Builder {
        val builder = AlertDialog.Builder(requireContext())
        title?.let { builder.setTitle(it) }
        message?.let { builder.setMessage(it) }
        okLabel?.let { builder.setPositiveButton(it, this) }
        cancelLabel?.let { builder.setNegativeButton(it, this) }
        otherLabel?.let { builder.setNeutralButton(it, this) }
        return builder
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return getAlertBuilder().create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when(which) {
            DialogInterface.BUTTON_POSITIVE -> { complete(IUtDialog.Status.POSITIVE) }
            DialogInterface.BUTTON_NEUTRAL  -> { complete(IUtDialog.Status.NEUTRAL) }
            DialogInterface.BUTTON_NEGATIVE -> { complete(IUtDialog.Status.NEGATIVE) }
            else -> {}
        }
    }

    companion object {
        /**
         * アクティビティから呼び出すOKボタンだけの確認メッセージ
         */
        fun confirm(activity: FragmentActivity, tag:String?, title:String?, message:String?, okLabel:String=activity.getString(
            R.string.ok
        )) {
            UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = okLabel
                show(activity, tag)
            }
        }
        fun confirm(fragment: Fragment, tag:String, title:String?, message:String?, okLabel:String=fragment.getString(
            R.string.ok
        )) {
            UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = okLabel
                show(fragment, tag)
            }
        }

        fun okCancel(activity: FragmentActivity, tag:String, title:String?, message:String?, okLabel:String=activity.getString(
            R.string.ok
        ), cancelLabel:String=activity.getString(R.string.cancel)) {
            UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = okLabel
                this.cancelLabel = cancelLabel
                show(activity,tag)
            }
        }

        fun okCancel(fragment: Fragment, tag:String, title:String?, message:String?, okLabel:String=fragment.getString(
            R.string.ok
        ), cancelLabel:String=fragment.getString(R.string.cancel)) {
            UtMessageBox().apply {
                UtMessageBox().apply {
                    this.title = title
                    this.message = message
                    this.okLabel = okLabel
                    this.cancelLabel = cancelLabel
                    show(fragment,tag)
                }
            }
        }

        fun yesNo(activity: FragmentActivity, tag:String, title:String?, message:String?, yesLabel:String=activity.getString(
            R.string.yes
        ), noLabel:String=activity.getString(R.string.no)) {
            okCancel(activity,tag,title,message,yesLabel,noLabel)
        }

        fun yesNo(fragment: Fragment, tag:String, title:String?, message:String?, yesLabel:String=fragment.getString(
            R.string.yes
        ), noLabel:String=fragment.getString(R.string.no)) {
            okCancel(fragment,tag,title,message,yesLabel,noLabel)
        }
    }
}
