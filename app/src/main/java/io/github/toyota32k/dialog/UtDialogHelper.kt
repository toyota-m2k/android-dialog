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
        var dlg: Fragment? = leaf.asFragment.parentFragment
        while(dlg!=null) {
            if(dlg is IUtDialog) {
                yield(dlg)
            }
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
     * parentで与えられたダイアログと、その子ダイアログをキャンセルする
     */
    fun cancelChildren(parent:IUtDialog) {
        for(c in dialogChildren(parent)) {
            c.parentVisibilityOption = IUtDialog.ParentVisibilityOption.NONE    // 閉じるダイアログ（親）が表示されてから閉じるのはブサイクなので非表示のまま閉じる
            cancelChildren(c)
        }
        parent.cancel()
    }

    /**
     * activityに属するダイアログをすべてキャンセルする。
     */
    fun cancelAllDialogs(activity:FragmentActivity) {
        for(c in dialogChildren(activity.supportFragmentManager)) {
            cancelChildren(c)
        }
    }

    fun findChildDialog(activity: FragmentActivity, tag:String):IUtDialog? {
        return activity.supportFragmentManager.findFragmentByTag(tag) as? IUtDialog
    }
    fun findChildDialog(fragment: Fragment, tag:String):IUtDialog? {
        return fragment.childFragmentManager.findFragmentByTag(tag) as? IUtDialog
    }
}