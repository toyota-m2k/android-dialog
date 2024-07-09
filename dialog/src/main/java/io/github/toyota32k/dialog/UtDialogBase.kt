@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.utils.UtLog
import java.lang.ref.WeakReference

/**
 * Activityでダイアログの結果を受け取る場合に継承すべきi/f
 * フラグメントで結果を受け取る場合ViewModel、または、FragmentManager.setFragmentResult()を使う。
 * Activityでも、ViewModelを使ってもよいが、onCreateでイベントリスナーの再接続が必要となることを考えると、INtDialogHostを実装した方が楽だと思う。
 */
//inline fun <reified T> IUtDialogHost.toLoader():IUtDialogHostLoader? where T:ViewModel {
//    val vm = this as? ViewModel ?: return null
//    return UtViewModelDialogHostLoader(ViewModel::class.java)
//}

//interface IUtDialogHostLoader {
//    fun load(activity: FragmentActivity) :IUtDialogHost?
//    fun serialize():String
//}
//
//class UtViewModelDialogHostLoader<T>(private val clazz:Class<T>) : IUtDialogHostLoader where T:ViewModel, T:IUtDialogHost {
//    override fun load(activity: FragmentActivity): IUtDialogHost {
//        return ViewModelProvider(activity, SavedStateViewModelFactory(activity.application, activity)).get(clazz)
//    }
//
//    override fun serialize(): String {
//        return clazz.name
//    }
//
//    companion object {
//        fun <T> deserialize(ser:String):UtViewModelDialogHostLoader<T> where T:ViewModel, T:IUtDialogHost{
//            val clz = Class.forName(ser)
//
//            return UtViewModelDialogHostLoader<clz.componentType>(clz)
//        }
//    }
//}

//class BundleDelegate(val bundle:Bundle) {
//    inline operator fun <reified T> getValue(thisRef: BundleDelegate, property: KProperty<*>): T? {
//        return thisRef.bundle[property.name] as T?
//    }
//}

abstract class UtDialogBase(
    val isDialog:Boolean=true       // true:ダイアログモード（MessageBox類）/ false:フラグメントモード(UtDialog)
    ) : DialogFragment(), IUtDialog {
    val bundle = UtBundleDelegate { ensureArguments() }

    final override fun ensureArguments(): Bundle {
        return arguments ?: Bundle().apply { arguments = this }
    }

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


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IUtDialogHost) {
            dialogHost = WeakReference(context)
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewDestroyed = false
        if(savedInstanceState==null) {
            onDialogOpening()
        }
        if(!isDialog) {
            // Apply window insets to avoid overlapping with system navigation bar
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val params = v.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = systemBarsInsets.top
                params.bottomMargin = systemBarsInsets.bottom
                v.layoutParams = params
                WindowInsetsCompat.Builder(insets).setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE).build()
            }
        }
    }

    // dismiss()を呼んでから、viewがdestroyされるまで、少し時間があり、
    // リソースを解放するための onDialogClose()を呼ぶタイミングとしては、dismiss()後のonDestroyView()が適当だ。
    // デバイス回転などによるView再構築のための onDestroyView()と区別するため、
    // dismiss で dialogClosed フラグを立て、onDestroyViewで、dialogClosed == trueなら、onDialogClosed()を呼ぶことにする。
    // ビューが破棄された状態（onDestroyView()が呼ばれて、onViewCreated()が呼ばれる前）に dismiss()されるケースも考慮し、
    // 初期状態は viewDestroyed は true で開始する。
    protected var viewDestroyed = true
        private set(v) {
            if(v!=field) {
                field = v
                if(v && dialogClosed) {
                    onDialogClosed()
                }
            }
        }
    protected var dialogClosed = false
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

    /**
     * ダイアログを表示する
     */
    override fun show(activity:FragmentActivity, tag:String?) {
        if(tag!=null && UtDialogHelper.findDialog(activity, tag) !=null) return

        if(isDialog) {
            super.show(activity.supportFragmentManager, tag)
        } else {
            activity.supportFragmentManager.apply {
                beginTransaction()
                .add(android.R.id.content, this@UtDialogBase, tag)
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

