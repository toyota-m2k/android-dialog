@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.launch
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

abstract class UtDialogBase(val isDialog:Boolean=true) : DialogFragment(), IUtDialog {
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
        if(savedInstanceState==null) {
            onDialogOpening()
        }
    }

    override fun onDetach() {
        super.onDetach()
        dialogHost = null
    }

    override fun onCancel(dialog: DialogInterface) {
        if(!status.finished) {
            onCancel()
        }
        super.onCancel(dialog)
    }

//    override fun onDismiss(dialog: DialogInterface) {
//        super.onDismiss(dialog)
//        // cancelやcompleteをすり抜けるケースがあると困るので。。。
//        onDialogClosing()
//    }

    private fun queryResultReceptor(): IUtDialogResultReceptor? {
        val tag = this.tag ?: return null
        return (parentFragment as? IUtDialogHost)?.queryDialogResultReceptor(tag) ?: dialogHost?.get()?.queryDialogResultReceptor(tag)
    }

    /**
     * フェードアウトアニメーションの終了を待つための dismiss
     * UtDialogで実装する。
     */
    open suspend fun dismissAsync() {
        dismiss()
    }

    private fun notifyResult(dismiss:Boolean) {
        val task = immortalTaskName?.let { UtImmortalTaskManager.taskOf(it) }?.task

        if(dismiss) {
            if(task!=null) {
                task.immortalCoroutineScope.launch {
                    dismissAsync()
                    if(!doNotResumeTask) {
                        task.resumeTask(this@UtDialogBase)
                    }
                    onDialogClosed()
                }
                return
            } else {
                dismiss()
            }
        }

        if(task!=null && !doNotResumeTask) {
            task.resumeTask(this)
        } else {
            queryResultReceptor()?.onDialogResult(this)
        }
        onDialogClosed()
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
        if(!status.finished) {
            logger.debug("$this")
            status = IUtDialog.Status.NEGATIVE
            onDialogClosing()
            notifyResult(dismiss=false)
        }
    }

    /**
     * ok/cancelに関わらず、ダイアログが閉じるときに呼び出される。
     */
//    protected open fun onClosed() {
//        logger.debug("$this")
//    }

    /**
     * OK/Doneボタンなどから呼び出す
     */
    override fun complete(status: IUtDialog.Status) {
        if (!status.finished) {
            throw IllegalStateException("complete must finish dialog.")
        }
        if (!this.status.finished) {
            this.status = status
            onDialogClosing()
            if(!status.negative) {
                onComplete()
            } else {
                onCancel()
            }
            notifyResult(dismiss = true)
        }
    }

    /**
     * キャンセルボタンなどから明示的にキャンセルする場合に呼び出す。
     * AlertDialogなどは、それ自身がCancelをサポートしているので、これを呼び出す必要はないはず。
     * setCanceledOnTouchOutside(true)なDialogなら、画面外タップでキャンセルされると思う。
     */
    override fun cancel() {
        if(isDialog) {
            dialog?.cancel()
        } else {
            complete(IUtDialog.Status.NEGATIVE)
        }
//        if(!status.finished) {
//            status = IUtDialog.Status.NEGATIVE
//            onDialogClosing()
//            notifyResult()
//            dialog?.cancel()
//            onCancel()  // dialog.cancel()を呼んだら自動的にonCancelが呼ばれるのかと思っていたが、よばれないので明示的に呼ぶ
//        }
    }

    override fun show(activity:FragmentActivity, tag:String?) {
        if(tag!=null && UtDialogHelper.findDialog(activity, tag) !=null) return

        if(isDialog) {
            super.show(activity.supportFragmentManager, tag)
        } else {
            activity.supportFragmentManager.apply {
                beginTransaction()
                .add(android.R.id.content, this@UtDialogBase, tag)
//                .addToBackStack(null)     // スタックには積まず、UtMortalDialog経由で自力で何とかする。
                .commit()
                if(UtDialogConfig.showDialogImmediately) {
                    executePendingTransactions()
                }
            }
        }
    }

    companion object {
        val logger = UtLog("DLG", null, "com.metamoji.")
    }

}

//open class UtGenericDialog(val createDialogCallback: (context: Context, savedInstanceState:Bundle?)-> Dialog) : UtDialogBase() {
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return createDialogCallback(requireContext(), savedInstanceState)
//    }
//}

