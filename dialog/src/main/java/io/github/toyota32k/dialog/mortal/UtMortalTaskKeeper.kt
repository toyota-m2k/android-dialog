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
     * フラグメントモードのダイアログを表示中にカーソルキーなどを操作すると、ダイアログの下のActivityが操作されてしまう事案が発生。
     * これを回避するため、フラグメントモードのダイアログ表示中は、onKeyDownイベントをActivityに回さないようにする。
     * この処理を dispatchKeyEvent()でやってしまうと、キーイベントがダイアログにも伝わらなくなって操作不能に陥るため、
     * onKeyDownに実装している。
     *
     * このように、キーイベントはとてもデリケートで、テキトーに処理されると困るので、UtDialogを利用するアクティビティでは、
     * onKeyDown / dispatchKeyEventをオーバーライドしないで、IUtKeyEventDispatcher を実装し、handleKeyEvent()メソッドで
     * キーイベントをハンドルするようにしてください。
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return withActivity { activity->
            if (UtDialogHelper.currentDialog(activity)?.isFragment == true) {
                // フラグメントモードのダイアログが表示されているときは、Activityのキーイベントハンドラを呼び出さないようにする。
                // そうしないとダイアログ表示中に、下のアクティビティがキー操作できてしまう。
                true
            } else if (activity is IUtKeyEventDispatcher) {
                activity.handleKeyEvent(keyCode, event)
            } else {
                false
            }
        } == true
    }

    /**
     * ダイアログへ確実にキーイベントを送るため、Activity.dispatchKeyEventを利用する。
     *
     * onKeyDownは、ダイアログ上のEditTextなどがフォーカスを持っていると呼ばれないことがあり、
     * TABによるフォーカス移動が動作しないことがあった。
     *
     * @return true ダイアログがイベントを消費した
     *         false 消費しなかった （super.dispatchKeyEventを呼んでください。）
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false   // DOWNのみ処理
        return withActivity { activity ->
            val currentDialog: UtDialog? = UtDialogHelper.currentDialog(activity)
            if (currentDialog != null) {
                logger.debug { "key event consumed by dialog: ${event.keyCode} (${event}) : ${currentDialog.javaClass.simpleName}" }
                if (currentDialog.handleKeyEvent(event)) {
                    // ダイアログがイベントを処理した
                    return@withActivity true
                }
            }
            false
        } == true
    }

    open val logger = UtImmortalTaskManager.logger
}