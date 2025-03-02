package io.github.toyota32k.dialog.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * ImmortalTask は、タスク毎に UtImmortalTaskBase から派生したタスククラスを用意する前提で設計したが、
 * 実際に使ってみると、execute() をオーバーライドする以外に特別な処理を実装することがないので、
 * それを外部からラムダで渡す UtImmortalSimpleTask クラスを実装したが、そのラムダが Boolean型を返すことを前提に実装してしまったため、
 * 使う側が、不必要に　trueを返す、などの無駄な実装が要求されることになっていた。長い間我慢して使っていたが、v5への移行に際し、この問題の改善を図る。
 * - 待ち合わせしない（値を返さない）タスク実行 (launch) には、Unit を返す コールバック関数を渡す。
 * - 戻り値を必要としないが待ち合わせをするタスクの実行 (await) にも、Unit を返す コールバック関数を渡す。
 * - 戻り値（T型）を待ち合わせるタスクの実行 (awaitResult) には、T型の戻り値を返す コールバック関数を渡す。
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
            false
        }
    }

    companion object {
        private const val DEF_TASK_NAME = "UtImmortalTask.Default"
        /**
         * やりっぱなしタスク
         */
        fun launch(taskName: String = DEF_TASK_NAME, coroutineScope: CoroutineScope?=null, allowSequential: Boolean=false, callback:suspend UtImmortalTaskBase.()->Unit) : Job {
            return UtImmortalTask(taskName, allowSequential, callback).fire(coroutineScope)
        }

        /**
         * タスクの終了を待つ (launchしたJobをJoin()するだけ）
         */
        suspend fun <T> await(taskName: String = DEF_TASK_NAME, coroutineScope: CoroutineScope?=null, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->Unit) : Unit {
            return launch(taskName, coroutineScope, allowSequential, callback).join()
        }

        /**
         * 結果を待つ
         * エラーが発生したら、例外をスローする。
         */
        suspend fun <T> awaitResult(taskName: String = DEF_TASK_NAME, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->T) : T {
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
        suspend fun <T> awaitResult(defValue:T, taskName: String = DEF_TASK_NAME, allowSequential:Boolean = false, callback:suspend UtImmortalTaskBase.()->T) : T {
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