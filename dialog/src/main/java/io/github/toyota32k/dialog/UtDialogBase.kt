package io.github.toyota32k.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.logger.UtLog
import io.github.toyota32k.utils.android.hideActionBar
import io.github.toyota32k.utils.android.hideStatusBar
import io.github.toyota32k.utils.android.isActionBarVisible
import io.github.toyota32k.utils.android.isStatusBarVisible
import io.github.toyota32k.utils.android.showActionBar
import io.github.toyota32k.utils.android.showStatusBar
import java.lang.ref.WeakReference

/**
 * UtDialog / UtMessageBox 共通の基底クラス
 */
abstract class UtDialogBase : DialogFragment(), IUtDialog {
    val bundle = UtBundleDelegate { ensureArguments() }

    private fun ensureArguments(): Bundle {
        return arguments ?: Bundle().apply { arguments = this }
    }

    /**
     * ダイアログの表示モード
     *
     * true: ダイアログモード
     *    DialogFragment#show() で表示。ダイアログ用に新しい Windowが構築される。
     *    Activityの上に、独立したwindow を重ねる構成となるため、動作が安定していたので、従来はデフォルトはこちらにしていたが、
     *    edge-to-edge が標準になると、Activityの状態（NoActionBar + statusBar非表示の場合など）と整合をとるのが難しくなってきたので、
     *    v4世代では、フラグメントモードをデフォルトに変更。
     * false: フラグメントモード
     *    FragmentManager のトランザクションで表示する。ActivityのWindow上に構築される。
     *
     *
     */
    var isDialog : Boolean by bundle.booleanWithDefault(UtDialogConfig.showInDialogModeAsDefault)
    val isFragment: Boolean get() = !isDialog

    enum class SystemBarOptionOnFragmentMode {
        NONE,   // 何もしない (NoActionBar ならこれでok)
//        DODGE,  // SystemBarを避ける
        HIDE,   // SystemBarを隠す
        STRICT, // ActivityのContentView領域だけを使い、それ以外には一切手出ししない（Activityの layout のルートコンテナに id 必須）
    }

    /**
     * フラグメントモードの場合に、setOnApplyWindowInsetsListenerを呼び出して、insets の調整を行うかどうか。
     */
//    private val edgeToEdgeEnabled get() = systemBarOptionOnFragmentMode == SystemBarOptionOnFragmentMode.DODGE
    private val hideSystemBarOnFragmentMode get() = systemBarOptionOnFragmentMode == SystemBarOptionOnFragmentMode.HIDE
    private val strictSystemBarMode get() = systemBarOptionOnFragmentMode == SystemBarOptionOnFragmentMode.STRICT

    /**
     * フラグメントモードの場合に、StatusBar / ActionBar をどう扱うか。
     */
    var systemBarOptionOnFragmentMode : SystemBarOptionOnFragmentMode by bundle.enum(def=UtDialogConfig.systemBarOptionOnFragmentMode)



    private var dialogHost: WeakReference<IUtDialogHost>? = null

    final override var status: IUtDialog.Status = IUtDialog.Status.UNKNOWN
    final override var immortalTaskName: String? by bundle.stringNullable
    final override val asFragment: DialogFragment
        get() = this
    final override var doNotResumeTask: Boolean by bundle.booleanFalse

    /**
     * ダイアログ外をタップしてキャンセル可能にするか？
     * true:キャンセル可能（デフォルト）
     * false:キャンセル不可
     */
    private var lightCancelable:Boolean by bundle.booleanTrue
    final override var cancellable:Boolean
        get() = lightCancelable
        set(c) {
            if(lightCancelable != c) {
                lightCancelable = c
                onCancellableChanged(c)
            }
        }
    protected open fun onCancellableChanged(value:Boolean) {
        isCancelable = value
    }

    private var originalStatusBarVisibility:Boolean? by bundle.booleanNullable
    private var originalActionBarVisibility:Boolean? by bundle.booleanNullable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IUtDialogHost) {
            dialogHost = WeakReference(context)
        }
        if(!isDialog && hideSystemBarOnFragmentMode) {
            val activity = context as? AppCompatActivity
            if(activity!=null) {
                originalStatusBarVisibility = activity.isStatusBarVisible()
                originalActionBarVisibility = activity.isActionBarVisible()
                if(originalStatusBarVisibility==true) {
                    activity.hideStatusBar()
                }
                if(originalActionBarVisibility==true) {
                    activity.hideActionBar()
                }
            }
        }
    }

    /**
     * ダイアログが開く
     */
    protected open fun onDialogOpening() {
    }

    /**
     * ダイアログが閉じる
     */
    protected open fun onDialogClosing() {
    }

    protected open fun onDialogClosed() {
    }

    private fun getActionBarHeight():Int {
        val activity = requireActivity() as? AppCompatActivity ?: return 0
        return if (activity.supportActionBar?.isShowing == true) {
            val styledAttributes = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
            val actionBarHeight = styledAttributes.getDimensionPixelSize(0, 0)
            styledAttributes.recycle()
            actionBarHeight
        } else {
            0
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewDestroyed = false
        if(savedInstanceState==null) {
            onDialogOpening()
        }
        // edgeToEdgeEnabled（DODGE）モードは廃止。
        // 苦労して調整したが、STRICTモードで、より効果的な「よけ」が達成できたので、泣く泣く削除。
//        if(!isDialog && edgeToEdgeEnabled) {
//            // システムバーを避けるためのマージン設定
//            fun applySystemBarsInsets(view:View, systemBarsInsets:Insets) {
//                val params = view.layoutParams as ViewGroup.MarginLayoutParams
//                params.topMargin = systemBarsInsets.top
//                params.bottomMargin = systemBarsInsets.bottom
//                view.layoutParams = params
//            }
//
//            // 当初は、setOnApplyWindowInsetsListener のコールバックで System Bars の insets を取得していたが、
//            // ダイアログはActivityが起動した状態から表示されるので、デバイスを回転するなどの操作が行われるまで、
//            // 最初の１回目のコールバックが発生せず、表示が不正（ActionBarの下にダイアログが潜る）になる。
//            // ViewCompat.getRootWindowInsets(view) で Insets を取得することにしたが、このメソッドではなぜか
//            // ActionBar のサイズが含まれず、StatusBarのサイズだけになってしまう。
//            // 仕方がないから、自力で ActionBar のサイズを計算する、getActionBarHeight() を実装し、
//            // getRootWindowInsets()の insets.top これを加算して使用することとした。
//
////            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
////                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
////                applySystemBarsInsets(v, systemBarsInsets)
////                WindowInsetsCompat.Builder(insets).setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE).build()
////            }
//
//            view.post {
//                val insets = ViewCompat.getRootWindowInsets(view)
//                if(insets!=null) {
//                    val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//                    applySystemBarsInsets(view, Insets.add(systemBarsInsets, Insets.of(0, getActionBarHeight(), 0, 0)))
//                }
//            }
    }

    // dismiss()を呼んでから、viewがdestroyされるまで、少し時間があり、
    // リソースを解放するための onDialogClose()を呼ぶタイミングとしては、dismiss()後のonDestroyView()が適当だ。
    // デバイス回転などによるView再構築のための onDestroyView()と区別するため、
    // dismiss で dialogClosed フラグを立て、onDestroyViewで、dialogClosed == trueなら、onDialogClosed()を呼ぶことにする。
    // ビューが破棄された状態（onDestroyView()が呼ばれて、onViewCreated()が呼ばれる前）に dismiss()されるケースも考慮し、
    // 初期状態は viewDestroyed は true で開始する。
    private var viewDestroyed = true
        private set(v) {
            if(v!=field) {
                field = v
                if(v && dialogClosed) {
                    onDialogClosed()
                }
            }
        }
    private var dialogClosed = false
        private set(v) {
            if(v && !field) {
                field = true
                if(viewDestroyed) {
                    onDialogClosed()
                }
            }
        }


    override fun onDestroyView() {
        super.onDestroyView()
        viewDestroyed = true
    }

    override fun onDetach() {
        super.onDetach()
        dialogHost = null
        if(!isDialog && hideSystemBarOnFragmentMode) {
            val activity = requireActivity() as? AppCompatActivity
            if(activity!=null) {
                if(originalStatusBarVisibility==true) {
                    originalStatusBarVisibility = null
                    activity.showStatusBar()
                }
                if(originalActionBarVisibility==true) {
                    originalActionBarVisibility = null
                    activity.showActionBar()
                }
            }
        }
    }

//    override fun onDismiss(dialog: DialogInterface) {
//        super.onDismiss(dialog)
//        // cancelやcompleteをすり抜けるケースがあると困るので。。。
//        onDialogClosing()
//    }

    private fun queryResultReceptor(): IUtDialogResultReceptor? {
        val tag = this.tag ?: return null

        return UtDialogHelper.parentDialogHost(this)?.queryDialogResultReceptor(tag)
                ?: dialogHost?.get()?.queryDialogResultReceptor(tag)
    }

    /**
     * FragmentDialog#onCancel
     * dialog.cancel() 時にシステムから呼び出される。
     * UtDialogでは、Backボタンで戻るようなケースに呼び出されることがあるが、これは、UtDialogの管理外の操作となり、アニメーションは行わない。
     */
    override fun onCancel(dialog: DialogInterface) {
        logger.debug()
        setFinishingStatus(IUtDialog.Status.NEGATIVE)
        super.onCancel(dialog)
    }

    /**
     * AlertDialog系のダイアログ（UtMessageBox/UtSelectionBox) は、デバイスを回転したときも onDismiss が呼ばれるため、
     * 無条件に、notifyResult()を呼んでしまうと メッセージボックスが表示されているのにタスクは終了している、という想定外の状態になってしまう。
     * これを回避するため、回転などによりメッセージボックスが（dismiss後に）復活するケースでは、それに先立ち onSaveInstanceState() が呼ばれることを利用し、
     * このタイミングで willBeBackSoonフラグ を立て、このフラグが立っている場合は、onDismissで notifyResult()を呼ばないようにする。
     */
    private var willBeBackSoon:Boolean = false
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        willBeBackSoon = true
    }

    protected open val isAlertDialog:Boolean
        get() = this is DialogInterface.OnClickListener

    /**
     * FragmentDialog#onDismiss
     * ダイアログが閉じる時に、システムから呼び出される。
     */
    override fun onDismiss(dialog: DialogInterface) {
        logger.debug()
        if( !isAlertDialog || !willBeBackSoon ) {   // 戻ってこない場合だけ
            setFinishingStatus(IUtDialog.Status.NEGATIVE)
        }
        super.onDismiss(dialog)
        if(isDialog) {
            dialogClosed = true
        }

        if(isDialog && !willBeBackSoon) {
            // AlertDialog系のダイアログ（UtMessageBox/UtSelectionBox）は画面外タップで閉じると notifyResult()が呼ばれないので、ここで呼んでおく。
            // ただし、回転の場合も呼ばれるので、これを除外するため、willBeBackSoon フラグをチェックする
            // ダイアログモードの場合で、HWキーボードのESCキー押下で閉じる場合も、AlertDialog同様、notifyResultが呼ばれないことが判明。isDialog==true ならこれを呼ぶ。
            notifyResult()
        }
        willBeBackSoon = false
    }

    /**
     * OK/Doneなどによる正常終了時に呼び出される
     */
    protected open fun onComplete() {
        logger.debug("$this")
    }

    /**
     * キャンセル時に呼び出される
     */
    protected open fun onCancel() {
        logger.debug("$this")
    }

    /**
     * ダイアログの終了をタスクやdialogHostに通知する
     */
    private var notified:Boolean = false        // ふつうはstateで守られるが、MessageBox (Dialog) の場合、画面外タップで閉じるときなどに notifyResult を呼ぶ必要があったので、念のためもう一段のチェックを入れる。
    protected fun notifyResult() {
        if(notified) return
        notified = true
        val task = immortalTaskName?.let { UtImmortalTaskManager.taskOf(it) }?.task
        if(task!=null && !doNotResumeTask) {
            task.resumeTask(this)
        } else {
            queryResultReceptor()?.onDialogResult(this)
        }
    }

    /**
     * ダイアログを閉じる前に、必要な処理をまとめて行うメソッド
     */
    private fun setFinishingStatus(status:IUtDialog.Status):Boolean {
        if (!status.finished) {
            throw IllegalStateException("${status}: finishing status is required.")
        }
        return if (!this.status.finished) {
            this.status = status
            try {
                onDialogClosing()
            } catch (e:Throwable) {
                logger.error(e)
            }
            if(!status.negative) {
                onComplete()
            } else {
                onCancel()
            }
            true
        } else false
    }

    /**
     * ダイアログを閉じる処理の本体
     * fade in/out のようなアニメーションを実装する場合に、サブクラスでオーバーライドする。
     */
    protected open fun internalCloseDialog() {
        dismiss()
        if(!isDialog) {
            // fragment mode の場合、dismiss()しても onDismiss()が呼ばれない。
            dialogClosed = true
        }
        notifyResult()
    }

    override fun forceDismiss() {
        if(!status.finished) {
            status = IUtDialog.Status.NEGATIVE
        }
        dismissAllowingStateLoss()
        if(!isDialog) {
            // fragment mode の場合、dismiss()しても onDismiss()が呼ばれない。
            dialogClosed = true
        }
        notifyResult()
    }

    /**
     * OK/Doneボタンなどから呼び出す
     */
    override fun complete(status: IUtDialog.Status) {
        if(setFinishingStatus(status)) {
            internalCloseDialog()
        }
    }

    /**
     * キャンセルボタンなどから明示的にキャンセルする場合に呼び出す。
     * AlertDialogなどは、それ自身がCancelをサポートしているので、これを呼び出す必要はないはず。
     * setCanceledOnTouchOutside(true)なDialogなら、画面外タップでキャンセルされると思う。
     */
    override fun cancel() {
        complete(IUtDialog.Status.NEGATIVE)
    }

    private fun getActivityContentViewId(activity:FragmentActivity):Int {
        if(strictSystemBarMode) {
            val container = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup
            if (container != null && container.id != View.NO_ID) {
                return container.id
            }
            logger.error("No container found.")
        }
        return android.R.id.content
    }

    /**
     * ダイアログを表示する
     */
    override fun show(activity:FragmentActivity, tag:String?) {
        if(tag!=null && UtDialogHelper.findDialog(activity, tag) !=null) {
            logger.error("Dialog ($tag) is already exists.")
            return
        }

        if(isDialog) {
            super.show(activity.supportFragmentManager, tag)
        } else {
            val containerId = getActivityContentViewId(activity)
            activity.supportFragmentManager.apply {
                beginTransaction()
                .add(containerId, this@UtDialogBase, tag)
//              .addToBackStack(null)     // スタックには積まず、UtMortalDialog経由で自力で何とかする。
                .apply {
                    if(UtDialogConfig.showDialogImmediately==UtDialogConfig.ShowDialogMode.CommitNow) {
                        commitNow()	// これを使うとonDialogClosed()の中から showDialog()を呼ぶケースで、fragmentManagerが例外を投げるようなので注意。
                    } else {
                        commit()
                    }
                }
                if(UtDialogConfig.showDialogImmediately==UtDialogConfig.ShowDialogMode.Immediately) {
                    executePendingTransactions()
                }
            }
        }
    }

    companion object {
        val logger = UtLog("DLG", null, "io.github.toyota32k.dialog.")
    }

}

