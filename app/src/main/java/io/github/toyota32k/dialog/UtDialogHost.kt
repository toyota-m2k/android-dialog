@file:Suppress("unused")

package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * ダイアログの処理が終わったときに、その結果（ダイアログインスタンス）を返すためのi/f
 * Activity / Fragment / ViewModel などで継承・実装する。
 */
interface IUtDialogResultReceptor {
    fun onDialogResult(caller: IUtDialog)
}

/**
 * タグ(Fragment#tag)をキーに、IUtDialogResultReceptor を返すための i/f
 * Activity / Fragment で継承・実装する
 */
interface IUtDialogHost {
    fun queryDialogResultReceptor(tag:String): IUtDialogResultReceptor?
}

/**
 * IUtDialogHostの実装
 * - Activity / Fragment(Dialog) / ViewModel などのフィールドとしてインスタンス化する。
 * - ViewModel.uiDialogHostManager は、UtDialogから直接参照できないので、ActivityまたはFragmentから参照できるようにしておく。
 * - 集中管理（１つのインスタンスで管理）する場合
 *   - 最もライフサイクルの長い ViewModelに配置
 *   - 必要なら、Activity/FragmentのonCreateあたりでaddReceptor, onDestroyあたりでremoveReceptorする。
 *   - Activityまたは、FragmentでIUtDialogHostを継承し、queryDialogResultReceptor()で、ViewModel.uiDialogHostManager.queryDialogResultReceptor() を返すようにする。
 * - 分散管理（Activity/Fragment/ViewModelにそれぞれインスタンスを配置）する場合
 *  - それぞれのライフサイクルでそれぞれのインスタンスを管理
 *  - ViewModelにUtDialogHostManagerを配置するときは、Activity/FragmentのUtDialogHostManagerを addChildHost()しておき、Activity#queryDialogResultRecepterから、これを参照するようにするのがよい。
 *  - この場合、onDestroyでremoveChildHost()しておかないとActivityインスタンスがリークするので注意
 */
class UtDialogHostManager: IUtDialogHost {
    data class ReceptorWrapper(val fn:(caller: IUtDialog)->Unit): IUtDialogResultReceptor {
        override fun onDialogResult(caller: IUtDialog) {
            fn(caller)
        }
    }

    private val receptorMap = mutableMapOf<String, IUtDialogResultReceptor>()
    private val hostList = mutableListOf<IUtDialogHost>()

    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        var r = receptorMap[tag]
        if(r!=null) {
            return r
        }
        for(h in hostList) {
            r = h.queryDialogResultReceptor(tag)
            if(r!=null) {
                return r
            }
        }
        return null
    }

    operator fun set(tag:String, r: IUtDialogResultReceptor?) {
        if(r!=null) {
            receptorMap[tag] = r
        } else {
            receptorMap.remove(tag)
        }
    }

    operator fun set(tag:String, fn:(IUtDialog)->Unit) {
        receptorMap[tag] = ReceptorWrapper(fn)
    }

    operator fun get(tag:String):IUtDialogResultReceptor? {
        return queryDialogResultReceptor(tag)
    }

    fun setReceptor(tag:String, r: IUtDialogResultReceptor) {
        this[tag] = r
    }

    fun setReceptor(tag:String, fn:(IUtDialog)->Unit) {
        this[tag] = fn
    }

    // uuidを使ってTagを自動発行とするのは、実装をシンプルにするよいアイデアだと思われたが、
    // 発行されたtagをどこに覚えておくか、ActivityやFragmentでは当然ダメで、
    // ViewModelさえ再作成される可能性があり、なら、savedStateHandlerを使うのか？となっていくと、逆に複雑になってしまう。
    // 却下。
    // 代わりに、ReceptorDelegate を使って プロパティ名をタグにする作戦で攻めてみよう。

//    private fun generateTag() : String {
//        var uuid:String
//        do {
//            uuid = UUID.randomUUID().toString()
//        } while(receptorMap.containsKey(uuid))
//        return uuid
//    }
//
//    fun addReceptor(r: IUtDialogResultReceptor) : String {
//        return generateTag().also {
//            this[it] = r
//        }
//    }
//
//    fun addReceptor(fn:(IUtDialog)->Unit):String {
//        return addReceptor(ReceptorWrapper(fn))
//    }


    fun removeReceptor(tag:String) {
        receptorMap.remove(tag)
    }

    fun addChildHost(host: IUtDialogHost) {
        hostList.add(host)
    }

    fun removeChildHost(host: IUtDialogHost) {
        hostList.remove(host)
    }

    fun clear() {
        receptorMap.clear()
        hostList.clear()
    }

    interface ISubmission {
        val dialog:IUtDialog
        val clientData:Any?
    }
    inner class ReceptorImpl(private val tag:String, val submit:(ISubmission)->Unit) : IUtDialogResultReceptor, ISubmission {
        init {
            setReceptor(this.tag, this)
        }

        private var dialogRef:WeakReference<IUtDialog>? = null
        override val dialog: IUtDialog
            get() = dialogRef?.get()!!
        override var clientData:Any?
            get() = dialogRef?.get()?.asFragment?.arguments?.get(tag)
            set(v) { dialogRef?.get()?.ensureArguments()?.put(tag,v)}

        override fun onDialogResult(caller: IUtDialog) {
            dialogRef = WeakReference(caller)
            submit(this)
        }

        fun showDialog(activity: FragmentActivity, creator:(ReceptorImpl)->IUtDialog) {
            creator(this).apply{
                attachDialog(this, null)
                show(activity, tag)
            }
        }
        fun showDialog(fragment: Fragment, creator:(ReceptorImpl)->IUtDialog) {
            creator(this).apply{
                attachDialog(this, null)
                show(fragment, tag)
            }
        }
        fun showDialog(activity: FragmentActivity, clientData:Any?, creator:(ReceptorImpl)->IUtDialog) {
            creator(this).apply{
                attachDialog(this, clientData)
                show(activity, tag)
            }
        }
        fun showDialog(fragment: Fragment, clientData:Any?, creator:(ReceptorImpl)->IUtDialog) {
            creator(this).apply{
                attachDialog(this, clientData)
                show(fragment, tag)
            }
        }

        fun attachDialog(dlg:IUtDialog, clientData:Any?) {
            dialogRef = WeakReference(dlg)
            if(clientData!=null) {
                this.clientData = clientData
            }
        }

        fun dispose() {
            removeReceptor(tag)
        }
    }

    // 委譲プロパティを使って、プロパティ名をタグにもつReceptorを生成する作戦
    // ... うまくいった！と思ったが、なんと、プロパティにアクセスされるまで、getValue()が呼ばれない。。。当たり前といえば当たり前。
    // getValue()が呼ばれないと、dialogHostManager に登録されないので、ダイアログ再構築後、ダイアログのcompleteから呼ばれるときに、登録がない、ということが起きる。
    // 企画倒れ。

//    inner class ReceptorDelegate(val submit: (ISubmission) -> Unit):ReadOnlyProperty<Any,ReceptorImpl> {
//        override operator fun getValue(thisRef: Any, property: KProperty<*>): ReceptorImpl {
//            return ReceptorImpl(property.name, submit)
//        }
//    }
//
//    fun delegate(submit: (ISubmission) -> Unit):ReadOnlyProperty<Any,ReceptorImpl> {
//        return ReceptorDelegate(submit)
//    }

//    fun createReceptor(onComplete:(IUtDialog, clientData:Any?)->Unit) : ReceptorImpl {
//        return ReceptorImpl(onComplete)
//    }
//    fun createReceptor(onComplete:(IUtDialog)->Unit) : ReceptorImpl {
//        return ReceptorImpl { dlg, _-> onComplete(dlg) }
//    }

    // ブサイクだが、これしかないか。。。


    fun register(tag:String, submit: (ISubmission) -> Unit) : ReceptorImpl {
        return ReceptorImpl(tag,submit)
    }
    fun find(tag:String):ReceptorImpl? {
        return receptorMap[tag] as? ReceptorImpl
    }
}