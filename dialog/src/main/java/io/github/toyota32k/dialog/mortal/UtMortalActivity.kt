package io.github.toyota32k.dialog.mortal

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import io.github.toyota32k.dialog.*
import io.github.toyota32k.dialog.task.UtImmortalTaskManager

/**
 * ImmortalTask と協調動作するActivityの基本実装
 * ベースActivityクラスの変更が困難、または、AppCompatActivity以外から派生する場合は、このクラスの実装を参考に、実装を追加してください。
 *
 */
abstract class UtMortalActivity private constructor(
    val mortalActivityCore: UtMortalTaskKeeper) : AppCompatActivity(), IUtDialogHost by mortalActivityCore {
    constructor() : this(UtMortalTaskKeeper())

    /**
     * タスクの結果を受け取るハンドラ
     * Activityがタスクの結果を知る必要がある場合は onCreate() でハンドラをセットする
     */
//    protected open fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {}
    protected var immortalTaskResultHandler: ((taskInfo: UtImmortalTaskManager.ITaskInfo)->Unit)? = null

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    override fun onResume() {
        super.onResume()
        mortalActivityCore.onResume(this)
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    override fun onPause() {
        super.onPause()
        mortalActivityCore.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mortalActivityCore.onDestroy(this)
    }

    /**
     * UtMortalActivityを継承するActivityは、onKeyDownを直接オーバーライドしないで、必要なら、handleKeyEventをオーバーライドする。
     *
     */
    open fun handleKeyEvent(keyCode: Int, event: KeyEvent?):Boolean {
        return false
    }

    /**
     * KeyDownイベントハンドラ（オーバーライド禁止）
     * - ダイアログ表示中なら、ダイアログにイベントを渡す。
     * - ダイアログ表示中でなければ、handleKeyEvent()を呼び出す。
     * - handleKeyEvent()がfalseを返したら、親クラス(FragmentActivity）の onKeyDownを呼ぶ。
     */
    final override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (mortalActivityCore.onKeyDown(this, keyCode, event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    open val logger = UtImmortalTaskManager.logger
}