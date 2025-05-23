package io.github.toyota32k.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class UtMessageBox : UtDialogBase(), DialogInterface.OnClickListener {
    var title:String? by bundle.stringNullable
    var message:String? by bundle.stringNullable
    var okLabel:String? by bundle.stringNullable
    var cancelLabel:String? by bundle.stringNullable
    var otherLabel:String? by bundle.stringNullable
    var selectedByButton:Boolean = false
        private set
    init {
        isDialog = true
    }

    protected open fun createAlertBuilder():AlertDialog.Builder {
        return AlertDialog.Builder(requireContext())
    }
    protected open fun getAlertBuilder():AlertDialog.Builder {
        val builder = createAlertBuilder()
        title?.let { builder.setTitle(it) }
        message?.let { builder.setMessage(it) }
        okLabel?.let { builder.setPositiveButton(it, this) }
        cancelLabel?.let { builder.setNegativeButton(it, this) }
        otherLabel?.let { builder.setNeutralButton(it, this) }
        return builder
    }

    open fun preCreateDialog() {
        // サブクラスで title や message などを初期化するなら、このタイミングがおススメ
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = cancellable
        return getAlertBuilder().create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        selectedByButton = true
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
        fun createForConfirm(title:String?, message:String?, okLabel:String= UtStandardString.OK.text) : UtMessageBox {
            return UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = okLabel
            }
        }

        fun createForOkCancel(title:String?, message:String?, okLabel:String= UtStandardString.OK.text, cancelLabel:String= UtStandardString.CANCEL.text) : UtMessageBox {
            return UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = okLabel
                this.cancelLabel = cancelLabel
            }
        }

        fun createForYesNo(title:String?, message:String?, yesLabel:String= UtStandardString.YES.text, noLabel:String= UtStandardString.NO.text) : UtMessageBox {
            return createForOkCancel(title,message,yesLabel,noLabel)
        }

        fun createForThreeChoices(title:String?, message:String?, positiveLabel:String, neutralLabel:String, negativeLabel:String) : UtMessageBox {
            return UtMessageBox().apply {
                this.title = title
                this.message = message
                this.okLabel = positiveLabel
                this.cancelLabel = negativeLabel
                this.otherLabel = neutralLabel
            }
        }

//        fun openToNotify(title:String?, message:String?, okLabel:String= UtStandardString.OK.text) {
//            UtImmortalTask.launchTask {
//                showConfirmMessageBox(title, message, okLabel)
//            }
//        }
//
//        /**
//         * 確認ダイアログを表示
//         * ユーザーが OK ボタンを押すのを待つ。
//         */
//        suspend fun openToConfirm(title:String?, message:String?, okLabel:String= UtStandardString.OK.text) {
//            UtImmortalTask.awaitTask<Unit> {
//                showConfirmMessageBox(title, message, okLabel)
//            }
//        }
//
//        suspend fun openForOkCancel(title:String?, message:String?, okLabel:String= UtStandardString.OK.text, cancelLabel:String= UtStandardString.CANCEL.text) : Boolean {
//            return UtImmortalTask.awaitTaskResult<Boolean> {
//                showOkCancelMessageBox(title, message, okLabel, cancelLabel)
//            }
//        }
//        suspend fun openForYesNo(title:String?, message:String?, yesLabel:String= UtStandardString.YES.text, noLabel:String= UtStandardString.NO.text) : Boolean {
//            return UtImmortalTask.awaitTaskResult<Boolean> {
//                showYesNoMessageBox(title, message, yesLabel, noLabel)
//            }
//        }
//        suspend fun openForThreeChoices(title:String?, message:String?, positiveLabel:String, neutralLabel:String, negativeLabel:String) : IUtDialog.Status {
//            return UtImmortalTask.awaitTaskResult<IUtDialog.Status> {
//                showThreeChoicesMessageBox(title, message, positiveLabel, neutralLabel, negativeLabel)
//            }
//        }
    }
}
