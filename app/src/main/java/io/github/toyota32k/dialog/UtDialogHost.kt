@file:Suppress("unused")

package io.github.toyota32k.dialog

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

    private val receiverMap = mutableMapOf<String, IUtDialogResultReceptor>()
    private val hostList = mutableListOf<IUtDialogHost>()

    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        var r = receiverMap[tag]
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
            receiverMap[tag] = r
        } else {
            receiverMap.remove(tag)
        }
    }

    operator fun set(tag:String, fn:(IUtDialog)->Unit) {
        receiverMap[tag] = ReceptorWrapper(fn)
    }

    operator fun get(tag:String):IUtDialogResultReceptor? {
        return queryDialogResultReceptor(tag)
    }

    fun addReceptor(tag:String, r: IUtDialogResultReceptor) {
        this[tag] = r
    }

    fun addReceptor(tag:String, fn:(IUtDialog)->Unit) {
        this[tag] = fn
    }

    fun removeReceptor(tag:String) {
        receiverMap.remove(tag)
    }

    fun addChildHost(host: IUtDialogHost) {
        hostList.add(host)
    }

    fun removeChildHost(host: IUtDialogHost) {
        hostList.remove(host)
    }

    fun clear() {
        receiverMap.clear()
        hostList.clear()
    }

}