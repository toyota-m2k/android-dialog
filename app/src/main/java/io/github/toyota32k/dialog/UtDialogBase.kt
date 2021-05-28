@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.BuildConfig
import io.github.toyota32k.task.UtImmortalTaskManager
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
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

abstract class UtDialogBase : DialogFragment(), IUtDialog {
    val bundle = UtBundleDelegate { ensureArguments() }

    final override fun ensureArguments():Bundle {
        return arguments ?: Bundle().apply { arguments = this }
    }

    private var dialogHost: WeakReference<IUtDialogHost>? = null

    final override var status: IUtDialog.Status = IUtDialog.Status.UNKNOWN
    final override var immortalTaskName: String? by bundle.stringNullable
    final override var visible: Boolean
        get() = dialog?.isShowing ?: false
        set(v) { dialog?.apply { if(v) show() else hide() } }
    final override val asFragment: DialogFragment
        get() = this

    override var parentVisibilityOption by bundle.enum(IUtDialog.ParentVisibilityOption.NONE) //UtDialogArgumentGenericDelegate { IUtDialog.ParentVisibilityOption.safeValueOf(it, IUtDialog.ParentVisibilityOption.NONE) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is IUtDialogHost) {
            dialogHost = WeakReference(context)
        }
    }

    /**
     * 子ダイアログが開いたときに呼び出される。
     * 子ダイアログのparentVisibilityOptionに従って、親ダイアログを非表示にする。
     */
     open fun onChildDialogOpened(child:UtDialogBase) {
         if (child.parentVisibilityOption != IUtDialog.ParentVisibilityOption.NONE) {
             if (visible) {
                 visible = false
             }
         }
     }

    /**
     * 子ダイアログが閉じたときに呼び出される。
     * 子ダイアログのparentVisibilityOptionに従って、親ダイアログを表示する。
     */
    open fun onChildDialogClosing(child:UtDialogBase) {
        if(!visible) {
            if (child.parentVisibilityOption == IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW ||
                (child.parentVisibilityOption == IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW_ON_POSITIVE && child.status.positive) ||
                (child.parentVisibilityOption == IUtDialog.ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE && !child.status.positive)
            ) {
                visible = true
            }
        }
    }

    /**
     * ダイアログが開く
     */
    private suspend fun onDialogOpening() {
        val parent = parentFragment as? UtDialogBase ?: return
        delay(100)          // 子ダイアログ(==this)が表示されてから親を閉じるため、ウェイトを入れる
        parent.onChildDialogOpened(this@UtDialogBase)
    }

    /**
     * ダイアログが閉じる
     */
    private suspend fun onDialogClosing() {
        val parent = parentFragment as? UtDialogBase
        parent?.onChildDialogClosing(this)
        delay(100)
    }


    override fun onStart() {
        super.onStart()
        MainScope().launch { onDialogOpening() }
    }

    override fun onDetach() {
        super.onDetach()
        dialogHost = null
    }

    override fun onCancel(dialog: DialogInterface) {
        if(!status.finished) {
            status = IUtDialog.Status.NEGATIVE
        }
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClosed()
        // cancelやcompleteをすり抜けるケースがあると困るので。。。
        MainScope().launch { onDialogClosing() }
    }

    private fun queryResultReceptor(): IUtDialogResultReceptor? {
        val tag = this.tag ?: return null
        return (parentFragment as? IUtDialogHost)?.queryDialogResultReceptor(tag) ?: dialogHost?.get()?.queryDialogResultReceptor(tag)
    }

    private fun notifyResult() {
        immortalTaskName?.let { UtImmortalTaskManager.taskOf(it) }?.task?.resumeTask(this)
            ?:queryResultReceptor()?.onDialogResult(this)
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
        if(!status.finished) {
            status = IUtDialog.Status.NEGATIVE
            notifyResult()
        }
    }

    /**
     * ok/cancelに関わらず、ダイアログが閉じるときに呼び出される。
     */
    protected open fun onClosed() {
        logger.debug("$this")
    }

    /**
     * OK/Doneボタンなどから呼び出す
     */
    override fun complete(status: IUtDialog.Status) {
        if (BuildConfig.DEBUG && !status.finished) {
            error("Assertion failed")
        }
        if (!this.status.finished) {
            this.status = status
            MainScope().launch {
                onDialogClosing()
                onComplete()
                notifyResult()
                dismiss()
            }
        }
    }

    /**
     * キャンセルボタンなどから明示的にキャンセルする場合に呼び出す。
     * AlertDialogなどは、それ自身がCancelをサポートしているので、これを呼び出す必要はないはず。
     * setCanceledOnTouchOutside(true)なDialogなら、画面外タップでキャンセルされると思う。
     */
    override fun cancel() {
        if(!status.finished) {
            status = IUtDialog.Status.NEGATIVE
            MainScope().launch {
                onDialogClosing()
                notifyResult()
                dialog?.cancel()
                onCancel()  // dialog.cancel()を呼んだら自動的にonCancelが呼ばれるのかと思っていたが、よばれないので明示的に呼ぶ
            }
        }
    }

    override fun show(activity:FragmentActivity, tag:String?) {
        if(tag!=null && UtDialogHelper.findChildDialog(activity,tag)!=null) return
        super.show(activity.supportFragmentManager, tag)
    }

    override fun show(fragment: Fragment, tag:String?) {
        if(tag!=null && UtDialogHelper.findChildDialog(fragment,tag)!=null) return
        super.show(fragment.childFragmentManager, tag)
    }

    companion object {
        val logger = UtLog("DLG")
    }

}

//open class UtGenericDialog(val createDialogCallback: (context: Context, savedInstanceState:Bundle?)-> Dialog) : UtDialogBase() {
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return createDialogCallback(requireContext(), savedInstanceState)
//    }
//}

