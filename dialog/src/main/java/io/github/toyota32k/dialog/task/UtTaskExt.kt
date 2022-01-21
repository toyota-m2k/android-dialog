package io.github.toyota32k.dialog.task

import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.UtMessageBox
import io.github.toyota32k.dialog.UtStandardString

/**
 * ImmortalTask内からメッセージボックスを表示するための拡張関数
 */
suspend fun UtImmortalTaskBase.showConfirmMessageBox(title:String?, message:String?, okLabel:String= UtStandardString.OK.text) {
    showDialog("internalConfirm") { UtMessageBox.createForConfirm(title,message,okLabel) }
}

suspend fun UtImmortalTaskBase.showOkCancelMessageBox(title:String?, message:String?, okLabel:String= UtStandardString.OK.text, cancelLabel:String= UtStandardString.CANCEL.text) : Boolean {
    return showDialog("internalOkCancel") { UtMessageBox.createForOkCancel(title,message,okLabel, cancelLabel) }.status.ok
}

suspend fun UtImmortalTaskBase.showYesNoMessageBox(title:String?, message:String?, yesLabel:String= UtStandardString.YES.text, noLabel:String= UtStandardString.NO.text) : Boolean {
    return showDialog("internalYesNo") { UtMessageBox.createForYesNo(title,message,yesLabel,noLabel) }.status.yes
}

/**
 * ImmortalTask内からアクティビティを取得するための拡張関数
 */
suspend fun UtImmortalTaskBase.getActivity():FragmentActivity? {
    return UtImmortalTaskManager.mortalInstanceSource.getOwner().asActivity()
}