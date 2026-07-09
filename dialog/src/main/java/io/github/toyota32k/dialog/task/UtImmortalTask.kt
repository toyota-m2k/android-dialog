package io.github.toyota32k.dialog.task

import kotlinx.coroutines.Job

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
class UtImmortalTask (
    taskName:String = DEF_TASK_NAME,
    allowSequential:Boolean = false
    ) : UtImmortalTaskBase(taskName,null, allowSequential) {
    override fun toString(): String {
        return "UtImmortalTask($taskName)"
    }

    fun launchTask(callback: suspend UtImmortalTask.() -> Unit):Job {
        return fire { callback() }
    }
    suspend fun awaitTask(callback: suspend UtImmortalTask.() -> Unit) {
        launchTask(callback).join()
    }
    suspend fun <T> awaitTaskResult(callback: suspend UtImmortalTask.() -> T):T {
        return fireAsync { callback() }
    }

    suspend fun <T> awaitTaskResult(default:T, callback: suspend UtImmortalTask.() -> T):T {
        return try {
            fireAsync { callback() }
        } catch (e: Exception) {
            default
        }
    }

    fun subTask(allowSequential: Boolean=this.allowSequential): UtImmortalTask {
        return UtImmortalTask("$taskName#${nextSubTaskId()}", allowSequential)
    }

    companion object {
        private const val DEF_TASK_NAME = "UtImmortalTask.Default"

//        fun launchTask(taskName: String, callback: suspend UtImmortalTask.() -> Unit):Job {
//            return UtImmortalTask(taskName).launchTask(callback)
//        }
//        suspend fun awaitTask(taskName: String, callback: suspend UtImmortalTask.() -> Unit) {
//            UtImmortalTask(taskName).awaitTask(callback)
//        }
//        suspend fun <T> awaitTaskResult(taskName: String, callback: suspend UtImmortalTask.() -> T):T {
//            return UtImmortalTask(taskName).awaitTaskResult(callback)
//        }
//
//        suspend fun <T> awaitTaskResult(taskName: String, default:T, callback: suspend UtImmortalTask.() -> T):T {
//            return UtImmortalTask(taskName).awaitTaskResult(default, callback)
//        }
    }
}


