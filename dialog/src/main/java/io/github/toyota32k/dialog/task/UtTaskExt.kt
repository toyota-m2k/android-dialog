@file:Suppress("unused")

package io.github.toyota32k.dialog.task

import android.app.Application
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtMessageBox
import io.github.toyota32k.dialog.UtMultiSelectionBox
import io.github.toyota32k.dialog.UtRadioSelectionBox
import io.github.toyota32k.dialog.UtSingleSelectionBox
import io.github.toyota32k.dialog.UtStandardString
import io.github.toyota32k.utils.android.getStringOrNull

private fun UtImmortalTaskBase.id2strOrNull(@StringRes id:Int):String? {
    if(id==0) return null
    return getStringOrNull(id)
}
private fun UtImmortalTaskBase.id2str(@StringRes id:Int):String {
    if(id==0) return throw IllegalStateException("id==0")
    return getStringOrNull(id) ?: throw IllegalStateException("no string: $id")
}
/**
 * 確認メッセージボックスを表示
 */
suspend fun UtImmortalTaskBase.showConfirmMessageBox(title:String?, message:String?, okLabel:String= UtStandardString.OK.text, cancellable:Boolean=true) {
    showDialog("internalConfirm") { UtMessageBox.createForConfirm(title,message,okLabel,cancellable) }
}
suspend fun UtImmortalTaskBase.showConfirmMessageBox(title:Int, message:Int, cancellable: Boolean=true) {
    showConfirmMessageBox(id2strOrNull(title),id2strOrNull(message),cancellable=cancellable)
}

suspend fun UtImmortalTaskBase.showOkCancelMessageBox(title:String?, message:String?, okLabel:String= UtStandardString.OK.text, cancelLabel:String= UtStandardString.CANCEL.text, cancellable: Boolean=true) : Boolean {
    return showDialog("internalOkCancel") { UtMessageBox.createForOkCancel(title,message,okLabel, cancelLabel, cancellable) }.status.ok
}
suspend fun UtImmortalTaskBase.showOkCancelMessageBox(title:Int, message:Int, cancellable: Boolean=true):Boolean {
    return showOkCancelMessageBox(id2strOrNull(title),id2strOrNull(message), cancellable=cancellable)
}
suspend fun UtImmortalTaskBase.showYesNoMessageBox(title:String?, message:String?, yesLabel:String= UtStandardString.YES.text, noLabel:String= UtStandardString.NO.text, cancellable: Boolean=false) : Boolean {
    return showDialog("internalYesNo") { UtMessageBox.createForYesNo(title,message,yesLabel,noLabel,cancellable) }.status.yes
}
suspend fun UtImmortalTaskBase.showYesNoMessageBox(title:Int, message:Int, cancellable: Boolean=false):Boolean {
    return showYesNoMessageBox(id2strOrNull(title),id2strOrNull(message),cancellable=cancellable)
}

suspend fun UtImmortalTaskBase.showThreeChoicesMessageBox(title:String?, message:String?, positiveLabel:String, neutralLabel:String, negativeLabel:String, cancellable: Boolean=false) : IUtDialog.Status {
    return showDialog("internalPositiveNeutralNegative") { UtMessageBox.createForThreeChoices(title,message,positiveLabel,neutralLabel,negativeLabel,cancellable) }.status
}
suspend fun UtImmortalTaskBase.showThreeChoicesMessageBox(title:Int, message:Int, positiveLabel:Int, neutralLabel:Int, negativeLabel:Int, cancellable: Boolean=true):IUtDialog.Status {
    return showThreeChoicesMessageBox(id2strOrNull(title),id2strOrNull(message),id2str(positiveLabel),id2str(neutralLabel),id2str(negativeLabel),cancellable)
}

suspend fun UtImmortalTaskBase.showSingleSelectionBox(title:String?, items:Array<String>, cancellable:Boolean=false) : Int {
    return showDialog("internalSingleSelection") { UtSingleSelectionBox.create(title, items, cancellable) }.selectedIndex
}

suspend fun UtImmortalTaskBase.showRadioSelectionBox(title:String?, items:Array<String>, initialSelection:Int, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=UtStandardString.CANCEL.text, cancellable:Boolean=true) : Int {
    return showDialog("internalRadioSelection") { UtRadioSelectionBox.create(title, items, initialSelection, okLabel, cancelLabel, cancellable) }.selectedIndex
}

suspend fun UtImmortalTaskBase.showMultiSelectionBox(title:String?, items:Array<String>, initialSelections:BooleanArray?, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=UtStandardString.CANCEL.text, cancellable:Boolean=true) : BooleanArray {
    return showDialog("internalMultiSelection") { UtMultiSelectionBox.create(title, items, initialSelections, okLabel, cancelLabel, cancellable) }.selectionFlags
}

/**
 * ImmortalTask内からアクティビティを取得するための拡張関数
 */
@Suppress("UnusedReceiverParameter")
suspend fun UtImmortalTaskBase.getActivity():FragmentActivity? {
    return UtImmortalTaskManager.mortalInstanceSource.getOwner().asActivity()
}

/**
 * UtDialogの immortalTask を取得
 */
val UtDialog.immortalTask:IUtImmortalTask get() {
    val taskName = immortalTaskName ?: throw IllegalStateException("no task name")
    return UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no such task: $taskName")
}

/**
 * UtDialog の immortalTaskContext を取得
 */
val UtDialog.immortalTaskContext: IUtImmortalTaskContext get() {
    val taskName = immortalTaskName ?: throw IllegalStateException("no task name")
    return UtImmortalTaskManager.taskOf(taskName)?.task?.immortalTaskContext ?: throw IllegalStateException("no such task: $taskName")
}

/**
 * ImmortalTask から Application を取得
 */
@Suppress("UnusedReceiverParameter")
val IUtImmortalTask.application : Application get() {
    return UtImmortalTaskManager.application
}

/**
 * ImmortalTask内からはいつでも リソース文字列 が取得できる
 */
fun IUtImmortalTask.getStringOrNull(@StringRes id:Int):String? {
    return application.getStringOrNull(id)
}
fun IUtImmortalTask.getStringOrDefault(@StringRes id:Int, default:String):String {
    return application.getStringOrNull(id) ?: default
}

/**
 * ちょっと悪ノリ気味。。。ViewModelからも application を取得できてしまう。
 * AndroidViewModel はもう不要。
 */
@Suppress("UnusedReceiverParameter")
val IUtImmortalTaskMutableContextSource.application : Application get() {
    return UtImmortalTaskManager.application
}

//inline fun <reified VM:ViewModel> IUtImmortalTaskContext.getViewModel():VM  {
//    return ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())[VM::class.java]
//}
//
//inline fun <reified VM:AndroidViewModel> IUtImmortalTaskContext.getViewModel(application: Application):VM  {
//    return ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[VM::class.java]
//}
