package io.github.toyota32k.dialog.mortal

import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.IUtDialogHost
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHelper
import io.github.toyota32k.dialog.UtDialogHostManager
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.dialog.toDialogOwner

/**
 * Mortal Activity と Immortal Task の連携をサポートするための最小クラス
 * dynamic reserve task ( == UtImmortalSimpleTask ) しか使わない通常の構成ではこれを使う。
 * static reserve task (タスクの結果を Activity で受け取る）を使う構成の場合は、UtMortalStaticTaskKeeper を使う。
 */
open class UtMortalTaskKeeper(
    private val dialogHostManager: UtDialogHostManager) : IUtDialogHost by dialogHostManager {
    constructor() : this(UtDialogHostManager())

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    open fun onResume(activity: FragmentActivity) {
        // ImmortalTask に接続する
        UtImmortalTaskManager.registerOwner(activity.toDialogOwner())
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    open fun onPause(activity: FragmentActivity) {
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
    fun handleKeyEvent(activity: FragmentActivity, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false   // DOWNのみ処理
        val currentDialog: UtDialog? = UtDialogHelper.currentDialog(activity)
        if (currentDialog != null) {
            logger.debug { "key event consumed by dialog: ${event.keyCode} (${event}) : ${currentDialog.javaClass.simpleName}" }
            if (currentDialog.handleKeyEvent(event)) {
                // ダイアログがイベントを処理した
                return true
            }
            if (!currentDialog.isDialog) {
                // フラグメントモードの場合は、ダイアログでイベントを処理しなくても、消費したことにする（ダイアログの後ろで、Activityが操作されてしまうのを防止）
                return true
            }
        }
        return false
    }

    open val logger = UtImmortalTaskManager.logger
}