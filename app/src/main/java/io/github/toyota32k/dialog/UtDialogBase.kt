@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.github.toyota32k.BuildConfig
import io.github.toyota32k.utils.UtLog
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

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

class BundleDelegate(val bundle:Bundle) {
    inline operator fun <reified T> getValue(thisRef: BundleDelegate, property: KProperty<*>): T? {
        return thisRef.bundle[property.name] as T?
    }
}

interface IUtDialog {
    enum class Status(val index:Int) {
        UNKNOWN(0),
        POSITIVE(DialogInterface.BUTTON_POSITIVE),
        NEGATIVE(DialogInterface.BUTTON_NEGATIVE),
        NEUTRAL(DialogInterface.BUTTON_NEUTRAL);

        val finished : Boolean
            get() = this != UNKNOWN

        val negative: Boolean
            get() = this == NEGATIVE
        val cancel: Boolean
            get() = this == NEGATIVE
        val no: Boolean
            get() = this == NEGATIVE
        val positive: Boolean
            get() = this == POSITIVE
        val ok: Boolean
            get() = this == POSITIVE
        val yes: Boolean
            get() = this == POSITIVE
        val neutral: Boolean
            get() = this == NEUTRAL
    }
    val status: Status
//    fun show(parent:FragmentActivity, tag:String=this.javaClass.name)
//    fun show(parent: Fragment, tag:String=this.javaClass.name)
    fun cancel()
    fun cancelChildren()
}

abstract class UtDialogBase : DialogFragment(), IUtDialog {
    private var dialogHost: WeakReference<IUtDialogHost>? = null

    override var status: IUtDialog.Status = IUtDialog.Status.UNKNOWN

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is IUtDialogHost) {
            dialogHost = WeakReference(context)
        }
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
    }

    private fun queryResultReceptor(): IUtDialogResultReceptor? {
        val tag = this.tag ?: return null
        return (parentFragment as? IUtDialogHost)?.queryDialogResultReceptor(tag) ?: dialogHost?.get()?.queryDialogResultReceptor(tag)
    }

    private fun notifyResult() {
        queryResultReceptor()?.onDialogResult(this)
    }

    /**
     * OK/Doneなどによる正常終了時に呼び出される
     */
    protected open fun onComplete() {
        notifyResult()
    }

    /**
     * キャンセル時に呼び出される
     */
    protected open fun onCancel() {
        if(!status.finished) {
            status = IUtDialog.Status.NEGATIVE
            notifyResult()
        }
    }

    /**
     * ok/cancelに関わらず、ダイアログが閉じるときに呼び出される。
     */
    protected open fun onClosed() {
    }

    /**
     * OK/Doneボタンなどから呼び出す
     */
    open fun complete(status: IUtDialog.Status) {
        if (BuildConfig.DEBUG && !status.finished) {
            error("Assertion failed")
        }
        if (!this.status.finished) {
            this.status = status
            onComplete()
            dismiss()
        }
    }

    /**
     * キャンセルボタンなどから明示的にキャンセルする場合に呼び出す。
     * AlertDialogなどは、それ自身がCancelをサポートしているので、これを呼び出す必要はないはず。
     * setCanceledOnTouchOutside(true)なDialogなら、画面外タップでキャンセルされると思う。
     */
    override fun cancel() {
        dialog?.cancel()
    }

    override fun cancelChildren() {
        cancelAllDialogs(childFragmentManager)
        cancel()    //自分自身を閉じる
    }

    open fun show(parent:FragmentActivity, tag:String?) {
        super.show(parent.supportFragmentManager, tag)
    }

    open fun show(parent: Fragment, tag:String?) {
        super.show(parent.childFragmentManager, tag)
    }

    companion object {
        fun cancelAllDialogs(fm:FragmentManager) {
            for(f in fm.fragments) {
                if(f is IUtDialog) {
                    f.cancelChildren()
                }
            }
        }

        fun cancelAllDialogs(owner:Fragment) {
            cancelAllDialogs(owner.childFragmentManager)
        }

        fun cancelAllDialogs(owner:FragmentActivity) {
            cancelAllDialogs(owner.supportFragmentManager)
        }

        val logger = UtLog("DLG")
    }

}

open class UtGenericDialog(val createDialogCallback: (context: Context, savedInstanceState:Bundle?)-> Dialog) : UtDialogBase() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return createDialogCallback(requireContext(), savedInstanceState)
    }
}

