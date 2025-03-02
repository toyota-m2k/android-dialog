package io.github.toyota32k.dialog.mortal

import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.utils.Disposer

/**
 * タスクの結果を受け取るハンドラ i/f
 * Activityがタスクの結果を知る必要がある場合は UtMortalStaticTaskKeeper を使うとともに、
 * Activity に UtImmortalTaskResultReceiver を実装する。
*/
interface IUtImmortalTaskResultReceiver {
    /**
     * Immortal Task が終了したときに結果を受け取るハンドラ
     */
    fun onImmortalTaskResult(task:UtImmortalTaskManager.ITaskInfo)
    /**
     * タスク名のテーブル
     */
    val staticImmortalTaskList:Array<String>
    /**
     * Activity終了時にタスクをdisposeするかどうかを返す。
     * 子アクティビティで開始したタスクを親アクティビティで処理する場合など、Activityをまたがるタスクに対しては false を返す。
     */
    fun queryDisposeTaskOnFinishActivity(name:String):Boolean {
        return true
    }
}

/**
 * static reserve task (タスクの結果を Activity で受け取る）を使う構成用
 */
abstract class UtMortalStaticTaskKeeper : UtMortalTaskKeeper() {
    private val observersDisposer = Disposer()

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    override fun onResume(activity: FragmentActivity) {
        // ImmortalTask に接続する
        super.onResume(activity)
        val resultReceiver = activity as? IUtImmortalTaskResultReceiver
        if(resultReceiver != null) {
            for (taskName in resultReceiver.staticImmortalTaskList) {
                val task = UtImmortalTaskManager.reserveTask(taskName)
                observersDisposer.register(task.registerStateObserver(activity) { state ->
                    if (state.finished) {
                        // val task = UtImmortalTaskManager.taskOf(taskName) ?: return@registerStateObserver
                        resultReceiver.onImmortalTaskResult(task)
                    }
                })
            }
        }
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    override fun onPause(activity: FragmentActivity) {
        val resultReceiver = activity as? IUtImmortalTaskResultReceiver
        if(resultReceiver != null) {
            for (name in resultReceiver.staticImmortalTaskList) {
                // デフォルトの動作では、Activityが完全終了(finish)するときに ImmortalTask も削除する。
                // アクティビティが終了してもタスクの動作を継続する場合は、queryDisposeTaskOnFinishActivity をオーバーライドして false を返す。
                if (activity.isFinishing && resultReceiver.queryDisposeTaskOnFinishActivity(name)) {
                    UtImmortalTaskManager.disposeTask(name)
                }
            }
        }
        observersDisposer.reset()
        super.onPause(activity)
    }

}