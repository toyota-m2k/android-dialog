@file:Suppress("unused")

package io.github.toyota32k.dialog

import io.github.toyota32k.dialog.task.UtImmortalSimpleTask
import io.github.toyota32k.dialog.task.UtImmortalTask
import kotlinx.coroutines.Job

/**
 * UtImmortalTaskを起動＋ダイアログを表示して、結果をコールバック（onResult)で受け取る。
 * ダイアログの結果はonResultの引数、dlg.status などを確認。
 */
fun <D:IUtDialog> D.launchOnTask(tag:String, onResult:(dlg:D)->Unit): Job {
    val dlg = this
    return UtImmortalTask.launch(tag) {
        showDialog(tag) { dlg }
        onResult(dlg)
    }
}

/**
 * UtImmortalTaskを起動＋ダイアログを表示して、結果をコールバック（onResult)で受け取り、結果を返す。
 * ダイアログの結果はonResultの引数、dlg.status などを確認。
 * onResultの戻り値を返すので、連続した処理を書くときに使えるんじゃなかろうか。
 */
suspend fun <D:IUtDialog,T> D.awaitOnTask(tag:String, defaultResult:T, onResult:(dlg:D)->T):T {
    val dlg = this
    return UtImmortalTask.awaitResult(defaultResult, tag) {
        showDialog(tag) { dlg }
        onResult(dlg)
    }
}

