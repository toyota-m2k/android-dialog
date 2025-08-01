package io.github.toyota32k.dialog.task

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.show
import io.github.toyota32k.logger.UtLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ImmortalTask の基本実装
 * @param taskName  タスクを一意に識別する名前
 * @param parentContext 親タスクのタスクコンテキスト（ルートタスクならnull）
 *                      parentContextを指定すると、親タスクのコルーチンスコープで実行される。
 *                      ViewModelも、親タスクのライフサイクル内で動作する。
 * @param allowSequential true:同名のタスクが実行中なら、それが終わるのを待って実行 / false:同名のタスクが実行中ならエラー
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class UtImmortalTaskBase(
    final override val taskName: String,
    val parentContext:IUtImmortalTaskContext? = null,
    val allowSequential:Boolean = false) : IUtImmortalTask {

//    private var continuation:Continuation<Any?>? = null
    // ダイアログの終了待ち用 continuation ... ネストできるようにスタックとする（push/pop）
    private val continuationStack = mutableListOf<Continuation<Any?>>()
    private fun pushContinuation(c:Continuation<Any?>) {
        continuationStack.add(c)
    }
    private fun popContinuation():Continuation<Any?>? {
        return continuationStack.removeLastOrNull()
    }

    override val immortalTaskContext =  UtImmortalTaskContext(taskName, parentContext)

    /**
     * ダイアログのcomplete待ち用
     */
    override fun resumeTask(value: Any?) {
        popContinuation()?.resume(value)
    }

    /**
     * タスク終了時にリソース解放が必要ならオーバーライドする
     */
    override fun close() {
        immortalTaskContext.close()
    }

    /**
     * タスクの中身を実装する。
     * この suspendメソッドは、タスクが完了するまで待機する。
     * つまり、このメソッドが応答を返すとタスクは終了する。
     */
    protected abstract suspend fun execute(): Boolean

    /**
     * bool 以外の結果を返す場合は、このプロパティをオーバーライドする。
     */
    override val taskResult:Any? = null

    val isRunning:Boolean get() = UtImmortalTaskManager.isRunning(taskName)

    /**
     * タスクを開始する
     */
    fun fire(coroutineScope: CoroutineScope?=null) : Job {
        logger.debug()
        return (coroutineScope ?: UtImmortalTaskManager.immortalTaskScope).launch {
            try {
                fireAsync()
            } catch(e:Throwable) {
                logger.stackTrace(e, "ImmortalTask:$taskName")
            } finally {
                logger.verbose("ImmortalTask:$taskName finished")
            }
        }
    }

    suspend fun fireAsync():Boolean {
        logger.debug(taskName)
        return UtImmortalTaskManager.beginTask(allowSequential, this) {
            withContext(immortalCoroutineScope.coroutineContext) {
                execute()
            }
        }
    }

    suspend fun <T> withOwner(fn: suspend (UtDialogOwner)->T):T {
        return UtImmortalTaskManager.mortalInstanceSource.withOwner { owner ->
            fn(owner)
        }
    }
    suspend fun <T> withOwner(clazz: Class<*>, fn: suspend (UtDialogOwner)->T):T {
        return UtImmortalTaskManager.mortalInstanceSource.withOwner(clazz) { owner ->
            fn(owner)
        }
    }
    suspend fun <T> withOwner(ownerChooser: (LifecycleOwner) -> Boolean, fn: suspend (UtDialogOwner)->T):T {
        return UtImmortalTaskManager.mortalInstanceSource.withOwner(ownerChooser) { owner ->
            fn(owner)
        }
    }
    suspend inline fun <reified T:FragmentActivity, R> withActivity(fn: (T)->R):R {
        return UtImmortalTaskManager.mortalInstanceSource.withActivity<T,R>(fn)
    }

    /**
     * タスク内からダイアログを表示し、complete()までsuspendする。
     */
    suspend fun <D> showDialog(tag:String, dialogSource:(UtDialogOwner)-> D) : D where D:IUtDialog {
        return internalShowDialog(tag, takeOwner = UtImmortalTaskManager.mortalInstanceSource::getOwner, dialogSource = dialogSource)
    }

    /**
     * showDialogの簡略版
     */
    suspend inline fun <reified D> showDialog(dlg:D):D where D:IUtDialog {
        return showDialog(dlg.javaClass.name) { dlg }
    }

    /**
     * オーナークラス（アクティビティ）を指定して（指定されたクラスのアクティビティが表示されるのを待って）ダイアログを表示
     */
    suspend fun <D> showDialog(tag:String, ownerClass:Class<*>, dialogSource:(UtDialogOwner)->D):D where D:IUtDialog {
        return internalShowDialog(tag, takeOwner = { UtImmortalTaskManager.mortalInstanceSource.getOwnerOf(ownerClass) }, dialogSource = dialogSource)
    }

    /**
     * オーナー（アクティビティ）を指定して（Chooserで選択されるアクティビティが表示されるのを待って）ダイアログを表示
     */
    suspend fun <D> showDialog(tag:String, ownerChooser:(LifecycleOwner)->Boolean, dialogSource:(UtDialogOwner)->D):D where D:IUtDialog {
        return internalShowDialog(tag, takeOwner = { UtImmortalTaskManager.mortalInstanceSource.getOwnerBy(ownerChooser) }, dialogSource = dialogSource)
    }

    private suspend fun <D> internalShowDialog(tag:String, takeOwner:suspend ()->UtDialogOwner, dialogSource:(UtDialogOwner)->D):D where D:IUtDialog {
        val running = UtImmortalTaskManager.taskOf(taskName)
        if(running == null || running.task != this) {
            throw IllegalStateException("task($taskName) is not running")
        }
        logger.debug("dialog opening...")
        @Suppress("UNCHECKED_CAST")
        val r = withContext(UtImmortalTaskManager.immortalTaskScope.coroutineContext) {
            val owner = takeOwner()
            suspendCoroutine {
                pushContinuation(it)
                dialogSource(owner).apply { immortalTaskName = taskName }.show(owner, tag)
            }
        } as D
        logger.debug("dialog closed")
        return r
    }

    companion object {
        val logger: UtLog = UtImmortalTaskManager.logger
    }
}

