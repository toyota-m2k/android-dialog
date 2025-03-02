package io.github.toyota32k.dialog.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * UtImmortalTaskBaseを継承しないで、ラムダだけ与えてちょいちょいと使う用の単純実装
 */
@Suppress("unused")
@Deprecated("use UtImmortalTask")
class UtImmortalSimpleTask(
    taskName:String,
    allowSequential:Boolean,
    val callback:suspend UtImmortalSimpleTask.()->Boolean
) : UtImmortalTaskBase(taskName, allowSequential = allowSequential) {
    constructor(taskName: String, callback:suspend UtImmortalSimpleTask.()->Boolean) : this(taskName, false, callback)

    companion object {
        private const val DEF_TASK_NAME = "UtImmortalSimpleTask"

        /**
         * やりっぱなしタスク
         */
        @Deprecated("use UtImmortalTask.launch")
        fun run(taskName: String = DEF_TASK_NAME, coroutineScope: CoroutineScope?=null, callback:suspend UtImmortalSimpleTask.()->Boolean) : Job {
            return UtImmortalSimpleTask(taskName, callback).fire(coroutineScope)
        }

        /**
         * タスクの結果(bool)を待つ
         */
        @Deprecated("use UtImmortalTask.awaitResult")
        suspend fun runAsync(taskName: String = DEF_TASK_NAME, callback:suspend UtImmortalSimpleTask.()->Boolean):Boolean {
            return UtImmortalSimpleTask(taskName, callback).fireAsync()
        }

        /**
         * コールバックの結果を待つ
         */
        @Deprecated("use UtImmortalTask.awaitResult")
        suspend fun <T> executeAsync(taskName:String=DEF_TASK_NAME, callback:suspend UtImmortalSimpleTask.()->T): T {
            data class TResult<T>(var value:T)
            var r:TResult<T>? = null
            UtImmortalSimpleTask(taskName, allowSequential = true) {
                r = TResult(callback())
                true
            }.fireAsync()
            return r!!.value
        }

        @Deprecated("use UtImmortalTask.awaitResult")
        suspend fun <T> executeAsync(taskName:String=DEF_TASK_NAME, allowSequential: Boolean=false, defResult:T, callback:suspend UtImmortalSimpleTask.()->T):T {
            var r = defResult
            UtImmortalSimpleTask(taskName, allowSequential = allowSequential) {
                r = callback()
                true
            }.fireAsync()
            return r
        }
    }

    override suspend fun execute(): Boolean {
        return callback()
    }
}