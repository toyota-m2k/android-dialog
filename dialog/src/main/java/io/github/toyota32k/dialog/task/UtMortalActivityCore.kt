package io.github.toyota32k.dialog.task

import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.IUtDialogHost
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHelper
import io.github.toyota32k.dialog.UtDialogHostManager
import io.github.toyota32k.dialog.toDialogOwner
import io.github.toyota32k.utils.Disposer

open class UtMortalActivityCore(
    private val dialogHostManager: UtDialogHostManager) : IUtDialogHost by dialogHostManager {
    constructor() : this(UtDialogHostManager())

    /**
     * タスク名のテーブル
     */
    protected open val immortalTaskNameList:Array<String> = emptyArray()

    /**
     * タスクの結果を受け取るハンドラ
     * Activityがタスクの結果を知る必要がある場合はオーバーライドする。
     * 放置でよければ、オーバーライド不要。
     */
//    protected open fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {}

    /**
     * Activity終了時にタスクをdisposeするかどうかを返す。
     * 子アクティビティで開始したタスクを親アクティビティで処理する場合など、Activityをまたがるタスクに対しては false を返す。
     */
    protected open fun queryDisposeTaskOnFinishActivity(name:String):Boolean {
        return true
    }

    private val observersDisposer = Disposer()

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    open fun onResume(activity: FragmentActivity, notifyImmortalTaskResult:((UtImmortalTaskManager.ITaskInfo) -> Unit)?) {
        // ImmortalTask に接続する
        UtImmortalTaskManager.registerOwner(activity.toDialogOwner())
        if(notifyImmortalTaskResult != null) {
            for (taskName in immortalTaskNameList) {
                val task = UtImmortalTaskManager.reserveTask(taskName)
                observersDisposer.register(task.registerStateObserver(activity) { state ->
                    if (state.finished) {
                        // val task = UtImmortalTaskManager.taskOf(taskName) ?: return@registerStateObserver
                        notifyImmortalTaskResult(task)
                    }
                })
            }
        }
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    open fun onPause(activity: FragmentActivity) {
        for (name in immortalTaskNameList) {
            // デフォルトの動作では、Activityが完全終了(finish)するときに ImmortalTask も削除する。
            // アクティビティが終了してもタスクの動作を継続する場合は、queryDisposeTaskOnFinishActivity をオーバーライドして false を返す。
            if (activity.isFinishing && queryDisposeTaskOnFinishActivity(name)) {
                UtImmortalTaskManager.disposeTask(name)
            }
        }
        observersDisposer.reset()
        UtImmortalTaskManager.unregisterOwner(activity.toDialogOwner())
    }

    open fun onDestroy(activity: FragmentActivity) {
        // タスクはActivityをまたいで生存可能とするが、ダイアログ（UI）はActivityの子なので閉じざるを得ない。
        // 普通、ダイアログが閉じるのを待ってActivityをfinish()すればよいはずなので、たぶん困ることはないはず。
        if(activity.isFinishing) {
            UtDialogHelper.forceCloseAllDialogs(activity)
        }
    }

    /**
     * KeyDownイベントハンドラ（オーバーライド禁止）
     * - ダイアログ表示中なら、ダイアログにイベントを渡す。
     * - ダイアログ表示中でなければ、handleKeyEvent()を呼び出す。
     * @return true ダイアログがイベントを消費した
     *         false 消費しなかった （Activity側のキーイベント処理を実行してください）
     */
    fun onKeyDown(activity: FragmentActivity, keyCode: Int, event: KeyEvent?): Boolean {
        val currentDialog: UtDialog? = UtDialogHelper.currentDialog(activity)
        if (currentDialog != null) {
            logger.debug { "key event consumed by dialog: $keyCode (${event}) : ${currentDialog.javaClass.simpleName}" }
            if(currentDialog.onKeyDown(keyCode, event)) {
                // ダイアログがイベントを処理した
                return true
            }
            if(!currentDialog.isDialog) {
                // フラグメントモードの場合は、ダイアログでイベントを処理しなくても、消費したことにする（ダイアログの後ろで、Activityが操作されてしまうのを防止）
                return true
            }
        }
        return false
    }

//    /**
//     * ImmortalTask の状態変化を受け取るハンドラ
//     * - 終了ステータス以外は無視。
//     * - 終了ステータスの場合は、notifyImmortalTaskResult()を呼ぶ
//     */
//    private fun onImmortalTaskStateChanged(taskName:String, state:UtImmortalTaskState, notifyImmortalTaskResult:((UtImmortalTaskManager.ITaskInfo) -> Unit)) {
//        if(state.finished) {
//            val task = UtImmortalTaskManager.taskOf(taskName) ?: return
//            notifyImmortalTaskResult(task)
//        }
//    }

    open val logger = UtImmortalTaskManager.logger
}