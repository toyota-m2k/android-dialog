package io.github.toyota32k.dialog.task

import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.show
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ImmortalTask の基本実装
 * @param taskName  タスクを一意に識別する名前
 * @param parentContext 親タスクのタスクコンテキスト（ルートタスクならnull）
 * parentContextを指定すると、親タスクのスコープ&タスク名で実行される。
 */
@Suppress("unused")
abstract class UtImmortalTaskBase(taskName: String, val parentContext:IUtImmortalTaskContext? = null) : IUtImmortalTask {

    protected var continuation:Continuation<Any?>? = null

    private val rawTaskContext = parentContext ?: UtImmortalTaskContext(taskName)
    override var immortalTaskContext: IUtImmortalTaskContext = rawTaskContext
    override val taskName = rawTaskContext.taskName

    /**
     * ダイアログのcomplete待ち用
     */
    override fun resumeTask(value: Any?) {
        continuation?.resume(value)
        continuation = null
    }

    /**
     * タスク終了時にリソース解放が必要ならオーバーライドする
     */
    override fun close() {
        if(rawTaskContext!==parentContext) {
            (rawTaskContext as UtImmortalTaskContext).close()
        }
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

    /**
     * タスクを開始する
     */
    fun fire(coroutineScope: CoroutineScope?=null) {
        logger.debug()
        (coroutineScope ?: UtImmortalTaskManager.immortalTaskScope).launch {
            fireAsync()
        }
    }

    suspend fun fireAsync():Boolean {
        logger.debug()
        UtImmortalTaskManager.attachTask(this@UtImmortalTaskBase)
        val result = try {
            logger.debug("to executed...")
            withContext(immortalCoroutineScope.coroutineContext) {
                execute()
            }
        } catch(e:Throwable) {
            logger.stackTrace(e, "ImmortalTask:$taskName")
            false
        }
        UtImmortalTaskManager.detachTask(this@UtImmortalTaskBase, result)
        close()
        return result
    }

    protected suspend fun <T> withOwner(fn: suspend (UtDialogOwner)->T):T {
        return UtImmortalTaskManager.mortalInstanceSource.withOwner { owner ->
            fn(owner)
        }
    }

    /**
     * タスク内からダイアログを表示し、complete()までsuspendする。
     */
    @Suppress("UNCHECKED_CAST")
    protected suspend fun <D> showDialog(tag:String, dialogSource:(UtDialogOwner)-> D) : D where D:IUtDialog {
        val running = UtImmortalTaskManager.taskOf(taskName)
        if(running == null || running.task != this) {
            throw IllegalStateException("task($taskName) is not running")
        }
        logger.debug("dialog opening...")
        val r = withContext(UtImmortalTaskManager.immortalTaskScope.coroutineContext) {
            withOwner { owner->
                suspendCoroutine<Any?> {
                    continuation = it
                    dialogSource(owner).apply { immortalTaskName = taskName }.show(owner, tag)
                } as D
            }
        }
        logger.debug("dialog closed")
        return r
    }

    companion object {
        val logger: UtLog = UtImmortalTaskManager.logger
    }
}