package io.github.toyota32k.dialog.mortal

import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import io.github.toyota32k.dialog.*
import io.github.toyota32k.dialog.task.UtImmortalTaskManager

/**
 * ImmortalTask と協調動作するActivityの基本実装
 * ベースActivityクラスの変更が困難、または、AppCompatActivity以外から派生する場合は、このクラスの実装を参考に、実装を追加してください。
 */
abstract class UtMortalActivity(
    protected val mortalTaskKeeper: UtMortalTaskKeeper = UtMortalTaskKeeper())
    : AppCompatActivity()
    , IUtDialogHost by mortalTaskKeeper {

    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    override fun onResume() {
        super.onResume()
        mortalTaskKeeper.onResume(this)
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    override fun onPause() {
        super.onPause()
        mortalTaskKeeper.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mortalTaskKeeper.onDestroy(this)
    }

    /**
     * UtMortalActivityを継承するActivityは、onKeyDownを直接オーバーライドしないで、
     * 必要なら、handleKeyEventをオーバーライドしてください。
     *
     * 自分自身のキーイベント処理(mortalActivityCore.onKeyDown()) と、
     * super.onKeyDown() の間に、派生クラスの onKeyDown() をはさむ必要があったので、このような構成にした。
     */
    open fun handleKeyEvent(keyCode: Int, event: KeyEvent?):Boolean {
        return false
    }

    /**
     * KeyDownイベントハンドラ
     * - ダイアログ表示中なら、ダイアログにイベントを渡す。
     * - ダイアログ表示中でなければ、handleKeyEvent()を呼び出す。
     * - handleKeyEvent()がfalseを返したら、親クラス(FragmentActivity）の onKeyDownを呼ぶ。
     */
    final override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        logger.info("$keyCode ${event?:"null"}")
        return handleKeyEvent(keyCode, event)
               || super.onKeyDown(keyCode, event)
    }

    final override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        logger.info("$event")
        if (mortalTaskKeeper.handleKeyEvent(this, event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    open val logger = UtImmortalTaskManager.logger
}