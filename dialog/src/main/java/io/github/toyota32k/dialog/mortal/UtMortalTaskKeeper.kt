package io.github.toyota32k.dialog.mortal

import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.IUtDialogHost
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHelper
import io.github.toyota32k.dialog.UtDialogHostManager
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.dialog.toDialogOwner
import io.github.toyota32k.utils.lifecycle.LifecycleOwnerHolder
import io.github.toyota32k.utils.lifecycle.LifecycleReference
import java.lang.ref.WeakReference

interface IUtKeyEventDispatcher {
    fun handleKeyEvent(keyCode: Int, event: KeyEvent?):Boolean
}

/**
 * Mortal Activity と Immortal Task の連携をサポートするための最小クラス
 * dynamic reserve task ( == UtImmortalSimpleTask ) しか使わない通常の構成ではこれを使う。
 * static reserve task (タスクの結果を Activity で受け取る）を使う構成の場合は、UtMortalStaticTaskKeeper を使う。
 */
open class UtMortalTaskKeeper(
    private val dialogHostManager: UtDialogHostManager) : IUtDialogHost by dialogHostManager {
    constructor() : this(UtDialogHostManager())

    private var ownerRef:WeakReference<FragmentActivity>? = null
    protected var ownerActivity: FragmentActivity?
        get() = ownerRef?.get()
        set(value) { if (value!=null) ownerRef=WeakReference(value) else ownerRef = null }

    fun attach(activity: FragmentActivity) {
        ownerActivity = activity
    }

    protected fun <T> withActivity(fn:(FragmentActivity)->T):T? {
        return ownerActivity?.run { fn(this) }
    }

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    open fun onResume() {
        // ImmortalTask に接続する
        withActivity { activity->
            UtImmortalTaskManager.registerOwner(activity.toDialogOwner())
        }
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    open fun onPause() {
        withActivity { activity->
            UtImmortalTaskManager.unregisterOwner(activity.toDialogOwner())
        }
    }

    open fun onDestroy() {
        // タスクはActivityをまたいで生存可能とするが、ダイアログ（UI）はActivityの子なので閉じざるを得ない。
        // 普通、ダイアログが閉じるのを待ってActivityをfinish()すればよいはずなので、たぶん困ることはないはず。
        withActivity { activity->
            if (activity.isFinishing) {
                UtDialogHelper.forceCloseAllDialogs(activity)
            }
        }
    }

    /**
     * キーイベントの処理
     * - ダイアログ表示中なら、ダイアログにイベントを渡す。
     * - ダイアログがイベントを処理しなければ、
     *
     * このように、キーイベントはとてもデリケートで、テキトーに処理されると困るので、UtDialogを利用するアクティビティでは、
     * onKeyDown / dispatchKeyEventをオーバーライドしないで、IUtKeyEventDispatcher を実装し、handleKeyEvent()メソッドで
     * キーイベントをハンドルするようにしてください。
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        logger.verbose { "onKeyDown: $keyCode (${event})" }
        if (event==null) return false
        val activity = ownerActivity ?: return false
        val currentDialog: UtDialog? = UtDialogHelper.currentDialog(activity)
        if (currentDialog != null) {
            logger.debug { "key event to dialog (isFragment=${currentDialog.isFragment}: ${event.keyCode} (${event}) : ${currentDialog.javaClass.simpleName}" }
            if (currentDialog.handleKeyEvent(event)) {
                // ダイアログがイベントを処理した
                logger.debug { "key event consumed by dialog: ${event.keyCode} (${event}) : ${currentDialog.javaClass.simpleName}" }
                return true
            }

            logger.debug { "key event pass through activity: ${event.keyCode} (${event}) : ${currentDialog.javaClass.simpleName}" }
            return false
        } else if (activity is IUtKeyEventDispatcher) {
            return activity.handleKeyEvent(keyCode, event)
        } else {
            return false
        }
    }

    open val logger = UtImmortalTaskManager.logger
}