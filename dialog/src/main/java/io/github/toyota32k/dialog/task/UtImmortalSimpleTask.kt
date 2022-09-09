package io.github.toyota32k.dialog.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * UtImmortalTaskBaseを継承しないで、ラムダだけ与えてちょいちょいと使う用の単純実装
 */
class UtImmortalSimpleTask(
    taskName:String,
    val callback:suspend UtImmortalSimpleTask.()->Boolean
) : UtImmortalTaskBase(taskName) {
    companion object {
        const val defTaskName = "UtImmortalSimpleTask"

        /**
         * やりっぱなしタスク
         */
        fun run(taskName: String = defTaskName, coroutineScope: CoroutineScope?=null, callback:suspend UtImmortalSimpleTask.()->Boolean) : Job {
            return UtImmortalSimpleTask(taskName, callback).fire(coroutineScope)
        }

        /**
         * タスクの結果(bool)を待つ
         */
        suspend fun runAsync(taskName: String = defTaskName, callback:suspend UtImmortalSimpleTask.()->Boolean):Boolean {
            return UtImmortalSimpleTask(taskName, callback).fireAsync()
        }

        /**
         * コールバックの結果を待つ
         */
        suspend fun <T> executeAsync(taskName:String=defTaskName, callback:suspend UtImmortalSimpleTask.()->T): T {
            data class TResult<T>(var value:T)
            var r:TResult<T>? = null
            UtImmortalSimpleTask(taskName) {
                r = TResult(callback())
                true
            }.fireAsync()
            return r!!.value
        }
    }

    override suspend fun execute(): Boolean {
        return callback()
    }
}