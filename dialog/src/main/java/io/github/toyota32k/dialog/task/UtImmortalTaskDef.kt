package io.github.toyota32k.dialog.task

import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.mortal.UtMortalActivity
import kotlinx.coroutines.Job
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
interface IUtImmortalTask : Closeable, IUtImmortalTaskContextSource {
    // internals
    val taskName: String
    fun resumeTask(tag: String, value: Any?)

    /**
     * オーナー取得
     */
    suspend fun <T> withOwner(fn: suspend (UtDialogOwner) -> T): T
    suspend fun <T> withOwner(clazz: Class<*>, fn: suspend (UtDialogOwner) -> T): T
    suspend fun <T> withOwner(ownerChooser: (LifecycleOwner) -> Boolean, fn: suspend (UtDialogOwner) -> T): T

    /**
     * タスク内からダイアログを表示し、complete()までsuspendする。
     */
    suspend fun <D> showDialog(dlg: D): D where D : IUtDialog
    suspend fun <D> showDialog(tag: String, dialogSource: (UtDialogOwner) -> D): D where D : IUtDialog

    /**
     * オーナークラス（アクティビティ）を指定して（指定されたクラスのアクティビティが表示されるのを待って）ダイアログを表示
     */
    suspend fun <D> showDialog(tag: String, ownerClass: Class<*>, dialogSource: (UtDialogOwner) -> D): D where D : IUtDialog

    /**
     * オーナー（アクティビティ）を指定して（Chooserで選択されるアクティビティが表示されるのを待って）ダイアログを表示
     */
    suspend fun <D> showDialog(tag: String, ownerChooser: (LifecycleOwner) -> Boolean, dialogSource: (UtDialogOwner) -> D): D where D : IUtDialog

    fun subTask(): IUtImmortalTaskExecutable
}

interface IUtImmortalTaskExecutable {
    fun launchTask(callback: suspend IUtImmortalTask.() -> Unit): Job
    suspend fun awaitTask(callback: suspend IUtImmortalTask.() -> Unit)
    suspend fun awaitTaskCatching(callback: suspend IUtImmortalTask.() -> Unit)
    suspend fun <T> awaitTaskResult(callback: suspend IUtImmortalTask.() -> T): T
    suspend fun <T> awaitTaskResultCatching(default:T, callback: suspend IUtImmortalTask.() -> T):T
}
/**
 * ライフサイクルオブジェクト（死んだり生き返ったりするオブジェクト:Activity/Fragment）を取得するための i/f
 */
interface IUtMortalInstanceSource {
    suspend fun getOwner() : UtDialogOwner
    suspend fun getOwnerOf(clazz:Class<*>) : UtDialogOwner
    suspend fun getOwnerBy(filter:(LifecycleOwner)->Boolean):UtDialogOwner

    @MainThread
    fun getOwnerOrNull(): UtDialogOwner?
}

suspend inline fun <T> IUtMortalInstanceSource.withOwner(fn:(UtDialogOwner)->T):T {
    return fn(getOwner())
}

suspend inline fun <T> IUtMortalInstanceSource.withOwner(clazz:Class<*>, fn:(UtDialogOwner)->T):T {
    return fn(getOwnerOf(clazz))
}

suspend inline fun <T> IUtMortalInstanceSource.withOwner(noinline ownerChooser:(LifecycleOwner)->Boolean, fn:(UtDialogOwner)->T):T {
    return fn(getOwnerBy(ownerChooser))
}

suspend fun IUtMortalInstanceSource.getOwnerAsActivity(): UtMortalActivity {
    return getOwnerOf(UtMortalActivity::class.java).asActivity() as? UtMortalActivity ?: throw java.lang.IllegalStateException("not target activity")
}

suspend fun <R> IUtMortalInstanceSource.withMortalActivity(fn:(UtMortalActivity)->R):R {
    return fn(getOwnerAsActivity())
}

suspend inline fun <reified T: FragmentActivity> IUtMortalInstanceSource.getActivity():T {
    return getOwnerOf(T::class.java).asActivity() as? T ?: throw java.lang.IllegalStateException("not target activity")
}

@Suppress("unused")
suspend inline fun <reified T:FragmentActivity, R> IUtMortalInstanceSource.withActivity(fn:(T)->R):R {
    return fn(getActivity<T>())
}

