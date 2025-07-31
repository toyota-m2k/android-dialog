package io.github.toyota32k.dialog.task

import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.UtDialogOwner
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
    val taskName: String
    val taskResult:Any?
    fun resumeTask(value:Any?)
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

suspend inline fun <reified T: FragmentActivity> IUtMortalInstanceSource.getActivity():T {
    return getOwnerOf(T::class.java).asActivity() as? T ?: throw java.lang.IllegalStateException("not target activity")
}

@Suppress("unused")
suspend inline fun <reified T:FragmentActivity, R> IUtMortalInstanceSource.withActivity(fn:(T)->R):R {
    return fn(getActivity<T>())
}

