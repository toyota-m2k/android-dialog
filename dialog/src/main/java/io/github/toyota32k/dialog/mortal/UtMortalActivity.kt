package io.github.toyota32k.dialog.mortal

import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.*
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.Listeners

typealias IUtRootViewInsetsListener = Listeners.IListener<Insets>

/**
 * ImmortalTask と協調動作するActivityの基本実装
 * ベースActivityクラスの変更が困難、または、AppCompatActivity以外から派生する場合は、このクラスの実装を参考に、
 * 実装を追加してください。
 */
abstract class UtMortalActivity(
    protected val mortalTaskKeeper: UtMortalTaskKeeper = UtMortalTaskKeeper())
    : AppCompatActivity()
    , IUtDialogHost by mortalTaskKeeper, IUtKeyEventDispatcher {

    init {
        mortalTaskKeeper.attach(this)
    }
    /**
     * Activity が前面に上がる時点で、reserveTask()を呼び出して、タスクテーブルに登録しておく。
     */
    override fun onResume() {
        super.onResume()
        mortalTaskKeeper.onResume()
    }

    /**
     * Activity が　finish()するときに disposeTask()する。
     */
    override fun onPause() {
        super.onPause()
        mortalTaskKeeper.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mortalTaskKeeper.onDestroy()
        mRootViewInsetsListeners.dispose()
    }

    /**
     * UtMortalActivityを継承するActivityは、onKeyDownを直接オーバーライドしないで、
     * 必要なら、handleKeyEventをオーバーライドしてください。
     *
     * 自分自身のキーイベント処理(mortalActivityCore.onKeyDown()) と、
     * super.onKeyDown() の間に、派生クラスの onKeyDown() をはさむ必要があったので、このような構成にした。
     *
     * IKeyEventDispatcher i/f
     */
    override fun handleKeyEvent(keyCode: Int, event: KeyEvent?):Boolean {
        return false
    }

    /**
     * KeyDownイベントハンドラ
     * - ダイアログ表示中なら、ダイアログにイベントを渡す。
     * - ダイアログ表示中でなければ、handleKeyEvent()を呼び出す。
     * - handleKeyEvent()がfalseを返したら、親クラス(FragmentActivity）の onKeyDownを呼ぶ。
     */
    final override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        logger.verbose("$keyCode ${event?:"null"}")
        if (mortalTaskKeeper.onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    final override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event)
    }

    private var mRootViewInsetsListeners = Listeners<Insets>()
    private var mLastInsets: Insets? = null

    /**
     * RootViewInsetsリスナーを取得または設定する。
     */
    fun addRootViewInsetsListener(owner: LifecycleOwner, listener: IUtRootViewInsetsListener): IDisposable {
        mLastInsets?.let {
            // 既にInsetsが適用されている場合は、即座にリスナーを呼び出す
            listener.onChanged(it)
        }
        return mRootViewInsetsListeners.add(owner, listener)
    }

    /**
     * WindowInsetsリスナーをrootViewに設定し、適宜rootViewにInsetsを適用する。
     * @param rootView ルートビュー
     * @param getTargetInsetsZones 適用するシステムゾーンを返すラムダ
     */
    protected fun setupWindowInsetsListener(rootView: View, getTargetInsetsZones: ()-> Int) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val targetSystemZone  = getTargetInsetsZones()
            val all = UtDialogConfig.SystemZone.calcInsets(insets, targetSystemZone)
            rootView.setPadding(all.left, all.top, all.right, all.bottom)
            mRootViewInsetsListeners.invoke(all)
            mLastInsets = all
            insets
        }
    }

    /**
     * WindowInsetsリスナーをrootViewに設定し、適宜rootViewにInsetsを適用する。
     * @param rootView ルートビュー
     * @param targetInsetsZones 適用するシステムゾーン（UtDialogConfig.SystemZone の値）
     */
    protected fun setupWindowInsetsListener(rootView: View, targetInsetsZones: Int = UtDialogConfig.SystemZone.NORMAL) {
        setupWindowInsetsListener(rootView) { targetInsetsZones }
    }

    companion object {
        /**
         * 現在のWindowInsetsを元に、rootViewにInsetsを適用する。
         * @param rootView ルートビュー
         * @param targetSystemZone 適用するシステムゾーン（UtDialogConfig.SystemZone の値）
         */
        fun applyCurrentWindowInsetsToRootView(rootView: View, targetSystemZone:Int = UtDialogConfig.SystemZone.NORMAL) {
            val insets = ViewCompat.getRootWindowInsets(rootView) ?: return // SDK 20以下なら nullが返るが、その場合は処理不要
            val all = UtDialogConfig.SystemZone.calcInsets(insets, targetSystemZone)
            rootView.setPadding(all.left, all.top, all.right, all.bottom)
        }
    }

    open val logger = UtImmortalTaskManager.logger
}