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
object UtImmortalTask {
    private const val DEF_TASK_NAME = "UtImmortalTask.Default"
    private val logger = UtImmortalTaskManager.logger

    // やりっぱなしタスク：待たない
    fun launchTask(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> Unit):Job {
        return UtImmortalTaskImpl(taskName, null, allowSequential).launchTask(callback)
    }
    fun launchTask(taskName:String, callback: suspend IUtImmortalTask.() -> Unit):Job {
        return launchTask(taskName, false, callback)
    }
    fun launchTask(callback: suspend IUtImmortalTask.() -> Unit):Job {
        return launchTask(DEF_TASK_NAME, false, callback)
    }

    /**
     * 待つ（戻り値なし＆例外をスローする）
     */
    suspend fun awaitTask(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> Unit) {
        UtImmortalTaskImpl(taskName, null, allowSequential).awaitTask(callback)
    }
    suspend fun awaitTask(taskName:String, callback: suspend IUtImmortalTask.() -> Unit) {
        awaitTask(taskName, false, callback)
    }
    suspend fun awaitTask(callback: suspend IUtImmortalTask.() -> Unit) {
        awaitTask(DEF_TASK_NAME, false, callback)
    }

    /**
     * 待つ（戻り値なし＆例外をスローしない）
     */
    suspend fun awaitTaskCatching(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> Unit) {
        launchTask(taskName, allowSequential, callback).join()
    }
    suspend fun awaitTaskCatching(taskName:String, callback: suspend IUtImmortalTask.() -> Unit) {
        awaitTaskCatching(taskName, false, callback)
    }
    suspend fun awaitTaskCatching(callback: suspend IUtImmortalTask.() -> Unit) {
        awaitTaskCatching(DEF_TASK_NAME, false, callback)
    }

    /**
     * 戻り値を待つ
     * エラーが発生したら例外をスロー
     */
    suspend fun <T> awaitTaskResult(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> T):T {
        return UtImmortalTaskImpl(taskName, null, allowSequential).awaitTaskResult(callback)
    }
    suspend fun <T> awaitTaskResult(taskName:String, callback: suspend IUtImmortalTask.() -> T):T {
        return awaitTaskResult(taskName, false, callback)
    }
    suspend fun <T> awaitTaskResult(callback: suspend IUtImmortalTask.() -> T):T {
        return awaitTaskResult(DEF_TASK_NAME, false, callback)
    }

    /**
     * 戻り値を待つ
     * エラーが発生したらデフォルト値を返す
     */
    suspend fun <T> awaitTaskResultCatching(taskName:String, default:T, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> T):T {
        return UtImmortalTaskImpl(taskName, null, allowSequential).awaitTaskResultCatching(default, callback)
    }

    suspend fun <T> awaitTaskResultCatching(taskName:String, default:T, callback: suspend IUtImmortalTask.() -> T):T {
        return awaitTaskResultCatching(taskName, default, false, callback)
    }
    suspend fun <T> awaitTaskResultCatching(default:T, callback: suspend IUtImmortalTask.() -> T):T {
        return awaitTaskResultCatching(DEF_TASK_NAME, default, false, callback)
    }
}


