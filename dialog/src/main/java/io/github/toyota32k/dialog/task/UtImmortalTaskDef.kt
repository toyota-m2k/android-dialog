package io.github.toyota32k.dialog.task

import java.io.Closeable

/**
 * 不死身タスクの状態
 */
enum class UtImmortalTaskState(val finished:Boolean) {
    INITIAL(false),
    RUNNING(false),
    COMPLETED(true),
    ERROR(true),
}

/**
 * 不死身タスクのi/f
 */
interface IUtImmortalTask : Closeable {
    val taskName: String
    val taskResult:Any?
    fun resumeTask(value:Any?)
}

/**
 * ライフサイクルオブジェクト（死んだり生き返ったりするオブジェクト:Activity/Fragment）を取得するための i/f
 */
interface IUiMortalInstanceSource {
    suspend fun <T> withOwner(ticket:Any?=null, fn: suspend (Any, io.github.toyota32k.dialog.UtDialogOwner)->T):T
    suspend fun <T> withOwner(clazzSpecified:Class<*>, ticket:Any?, fn: suspend (Any, io.github.toyota32k.dialog.UtDialogOwner)->T):T
}