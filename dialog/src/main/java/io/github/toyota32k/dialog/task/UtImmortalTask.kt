package io.github.toyota32k.dialog.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ImmortalTask は、タスク毎に UtImmortalTaskBase から派生したタスククラスを用意する前提で設計しましたが、
 * 実際に使ってみると、execute() をオーバーライドする以外に特別な処理を実装することがないので、
 * それを外部からラムダで渡す UtImmortalSimpleTask クラスを実装しました。
 * しかし、そのラムダが Boolean型を返すことを前提に実装してしまったため、
 * 使う側が、不必要に trueを返す無駄な実装が要求されることになっていました。
 * 長い間我慢して使っていたのですが、v5への移行に際し、この問題の改善を図ります。
 * - 待ち合わせしない（値を返さない）タスク実行 (launchTask) には、Unit を返す コールバック関数を渡す。
 * - 戻り値を必要としないが待ち合わせをするタスクの実行 (awaitTask) にも、Unit を返す コールバック関数を渡す。
 * - 戻り値（T型）を待ち合わせるタスクの実行 (awaitTaskResult) には、T型の戻り値を返す コールバック関数を渡す。
 * - awaitResultは、callbackでエラーが発生すると例外をスローするが、デフォルト値（defValue:T）をを渡すと、エラーが発生しても例外はスローしないで、defValueを返す。
 */
class UtImmortalTask<T>(
    taskName:String,
    allowSequential:Boolean,
    private val callback:suspend UtImmortalTask<T>.()->T
    ) : UtImmortalTaskBase(taskName, allowSequential = allowSequential) {
        constructor(taskName: String, callback:suspend UtImmortalTask<T>.()->T) : this(taskName, false,  callback)

    var result:T? = null
    override suspend fun execute(): Boolean {
        return try {
            result = callback()
            true
        } catch(e:Exception) {
            logger.stackTrace(e, "ImmortalTask:$taskName")
            false
        }
    }

    override fun toString(): String {
        return "UtImmortalTask($taskName)"
    }

    companion object {
        private const val DEF_TASK_NAME = "UtImmortalTask.Default"
        /**
         * やりっぱなしタスク
         */
        fun launchTask(taskName: String = DEF_TASK_NAME, coroutineScope: CoroutineScope?=null, allowSequential: Boolean=false, callback:suspend UtImmortalTaskBase.()->Unit) : Job {
            return UtImmortalTask(taskName, allowSequential, callback).fire(coroutineScope)
        }

        /**
         * タスクの終了を待つ (launchしたJobをJoin()するだけ）
         * OKメッセージボックスを表示して閉じるまで待つ、とか。
         */
        suspend fun awaitTask(taskName: String = DEF_TASK_NAME, coroutineScope: CoroutineScope?=null, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->Unit) {
            return launchTask(taskName, coroutineScope, allowSequential, callback).join()
        }

        /**
         * 結果を待つ
         * エラーが発生したら、例外をスローする。
         */
        suspend fun <T> awaitTaskResult(taskName: String = DEF_TASK_NAME, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->T) : T {
            return UtImmortalTask(taskName, allowSequential, callback).run {
                if (fireAsync()) {
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                } else {
                    throw IllegalStateException("Task failed")
                }
            }
        }
        /**
         * 結果を待つ
         * エラーが発生したら、defValue を返す
         */
        suspend fun <T> awaitTaskResult(defValue:T, taskName: String = DEF_TASK_NAME, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->T) : T {
            return UtImmortalTask(taskName, allowSequential, callback).run {
                if (fireAsync()) {
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                } else {
                    defValue
                }
            }
        }
    }

}

fun UtImmortalTaskBase.launchSubTask(fn:suspend UtImmortalTaskBase.()->Unit):Job {
    return this.immortalCoroutineScope.launch {
        fn()
    }
}

suspend fun UtImmortalTaskBase.awaitSubTask(fn:suspend UtImmortalTaskBase.()->Unit) {
    launchSubTask(fn).join()
}

suspend fun <T> UtImmortalTaskBase.awaitSubTaskResult(fn:suspend UtImmortalTaskBase.()->T):T {
    return withContext(this.immortalCoroutineScope.coroutineContext) {
        fn()
    }
}

fun IUtImmortalTaskContext.toRunningTask():UtImmortalTaskBase? {
    val taskInfo = UtImmortalTaskManager.taskOf(this.taskName)
    return if (taskInfo?.state == UtImmortalTaskState.RUNNING) {
        taskInfo.task as? UtImmortalTaskBase
    } else null
}

/**
 * 実行中のUtImmortalTaskBase上で、サブタスクを開始する
 * （やりっぱなし）
 */
fun IUtImmortalTaskContext.launchSubTask(fn:suspend UtImmortalTaskBase.()->Unit):Job {
    val task = toRunningTask() ?: throw IllegalStateException("cannot launch sub-task on ${this.taskName}")
    return task.launchSubTask(fn)
}

/**
 * 実行中のUtImmortalTaskBase上で、サブタスクを開始する
 * （終了を待つ）
 */
suspend fun IUtImmortalTaskContext.awaitSubTask(fn:suspend UtImmortalTaskBase.()->Unit) {
    return launchSubTask(fn).join()
}

/**
 * 実行中のUtImmortalTaskBase上で、サブタスクを開始する
 * （サブタスクの結果を待つ）
 */
suspend fun <T> IUtImmortalTaskContext.awaitSubTaskResult(fn:suspend UtImmortalTaskBase.()->T):T {
    val task = toRunningTask() ?: throw IllegalStateException("cannot launch sub-task on ${this.taskName}")
    return task.awaitSubTaskResult(fn)
}


