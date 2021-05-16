package io.github.toyota32k.task

import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.show
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ImmortalTask の基本実装
 */
@Suppress("unused")
abstract class UtImmortalTaskBase(override val taskName: String) : IUtImmortalTask {

    private var continuation:Continuation<Any?>? = null
    private var dialogOwnerTicket:Any? = null

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

    }

    /**
     * タスクの中身を実装する。
     * この suspendメソッドは、タスクが完了するまで待機する。
     * つまり、このメソッドが応答を返すとタスクは終了する。
     */
    protected abstract suspend fun execute(): Boolean

    /**
     * bool以外の結果を返す場合は、このプロパティをオーバーライドする。
     */
    override val taskResult:Any? = null

    /**
     * タスクを開始する
     */
    open fun fire() {
        logger.debug()
        UtImmortalTaskManager.attachTask(this)
        UtImmortalTaskManager.immortalTaskScope.launch {
            val result = try {
                logger.debug("to executed...")
                execute()
            } catch(e:Throwable) {
                logger.stackTrace(e, "ImmortalTask:$taskName")
                false
            }
            UtImmortalTaskManager.detachTask(this@UtImmortalTaskBase, result)
        }
    }

    /**
     * タスク内からダイアログを表示し、complete()までsuspendする。
     */
    protected suspend fun showDialog(tag:String, dialogSource:()-> IUtDialog) : IUtDialog? {
        val running = UtImmortalTaskManager.taskOf(taskName)
        if(running == null || running.task != this) {
            throw IllegalStateException("task($taskName) is not running")
        }
        logger.debug("dialog opening...")
        val r = withContext<IUtDialog?>(UtImmortalTaskManager.immortalTaskScope.coroutineContext) {
            UtImmortalTaskManager.mortalInstanceSource.withOwner(dialogOwnerTicket) { ticket, owner->
                dialogOwnerTicket = ticket
                suspendCoroutine<Any?> {
                    continuation = it
                    dialogSource().apply { immortalTaskName = taskName }.show(owner, tag)
                } as IUtDialog
            }
        }
        dialogOwnerTicket = null
        logger.debug("dialog closed")
        return r
    }

    companion object {
        val logger:UtLog = UtImmortalTaskManager.logger
    }
}