@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

object UtDialogHelper {
    /**
     * leafからルートに向かって、ダイアログチェーン列挙する
     */
    fun dialogChainToParent(leaf:IUtDialog) = sequence<IUtDialog> {
        var dlg: Fragment? = leaf.asFragment
        while(dlg!=null && dlg is IUtDialog) {
            yield(dlg)
            dlg = dlg.parentFragment
        }
    }

    /**
     * fragmentManagerに属するダイアログを列挙する
     */
    fun dialogChildren(fm: FragmentManager) = sequence<IUtDialog> {
        // return fm.fragments.filterIsInstanceTo<IUtDialog,MutableList<IUtDialog>>(mutableListOf<IUtDialog>())
        //return fm.fragments.filter{ it is IUtDialog }.map { it as IUtDialog }
        for(f in fm.fragments) {
            if(f is IUtDialog) {
                yield(f)
            }
        }
    }

    /**
     * parentの子ダイアログを列挙する
     */
    fun dialogChildren(parent:IUtDialog):Sequence<IUtDialog> {
        return dialogChildren(parent.asFragment.childFragmentManager)
    }

    /**
     * ダイアログチェーンの先頭（ルートのダイアログ）を取得
     */
    fun dialogRoot(leaf:IUtDialog):IUtDialog {
        return dialogChainToParent(leaf).last()
    }

    /**
     * parentで与えられたダイアログの子ダイアログを（末端から）すべてキャンセルする。
     * parent自身はキャンセルしない。
     */
    fun cancelChildren(parent:IUtDialog) {
        for(c in dialogChildren(parent)) {
            cancelChildren(c)
            c.cancel()
        }
    }

    /**
     * activityに属するダイアログをすべてキャンセルする。
     */
    fun cancelAllDialogs(activity:FragmentActivity) {
        for(c in dialogChildren(activity.supportFragmentManager)) {
            cancelChildren(c)
            c.cancel()
        }
    }
}