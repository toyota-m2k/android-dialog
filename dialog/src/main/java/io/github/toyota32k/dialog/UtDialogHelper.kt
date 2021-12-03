@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

object UtDialogHelper {
    /**
     * leafからルートに向かって、ダイアログチェーン列挙する
     * (leafも含む）
     */
    fun dialogChainToParent(leaf: Fragment) = sequence<IUtDialog> {
        var dlg: Fragment? = leaf
        while(dlg!=null) {
            if(dlg is IUtDialog) {
                yield(dlg)
            }
            dlg = dlg.parentFragment
        }
    }
    fun dialogChainToParent(leaf: IUtDialog) :Sequence<IUtDialog> {
        return UtDialogHelper.dialogChainToParent(leaf.asFragment)
    }

    fun parentDialog(fragment: Fragment):IUtDialog? {
        return dialogChainToParent(fragment).find { it.asFragment!==fragment }
    }

    fun parentDialog(dialog: IUtDialog):IUtDialog? {
        return parentDialog(dialog.asFragment)
    }

    /**
     * fragmentManagerに属するダイアログを列挙する
     */
    fun dialogChildren(fm: FragmentManager) : List<IUtDialog> {
        return fm.fragments.mapNotNull { it as? IUtDialog }
    }

    /**
     * parentの子ダイアログを列挙する
     */
    fun dialogChildren(parent: IUtDialog):List<IUtDialog> {
        return dialogChildren(parent.asFragment.childFragmentManager)
    }

    fun dialogDescendants(fm:FragmentManager):List<IUtDialog> {
        val children = dialogChildren(fm)
        return children + children.flatMap { dialogDescendants(it.asFragment.childFragmentManager) }
    }

    fun dialogDescendants(parent: FragmentActivity):List<IUtDialog> {
        return dialogDescendants(parent.supportFragmentManager)
    }

    fun dialogDescendants(parent: IUtDialog):List<IUtDialog> {
        return dialogDescendants(parent.asFragment.childFragmentManager)
    }


    /**
     * ダイアログチェーンの先頭（ルートのダイアログ）を取得
     */
    fun dialogRoot(leaf: IUtDialog): IUtDialog {
        return dialogChainToParent(leaf).last()
    }

    /**
     * parentで与えられたダイアログと、その子ダイアログをキャンセルする
     */
    fun cancelChildren(parent: IUtDialog) {
        for(c in dialogChildren(parent)) {
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

    fun findChildDialog(fm:FragmentManager, tag:String) : IUtDialog? {
        val list = dialogDescendants(fm)
        return list.find { it.asFragment.tag == tag }
    }

    fun findChildDialog(activity: FragmentActivity, tag:String): IUtDialog? {
        return findChildDialog(activity.supportFragmentManager, tag)
    }
    fun findChildDialog(fragment: Fragment, tag:String): IUtDialog? {
        return findChildDialog(fragment.childFragmentManager, tag)
    }

    fun findChildDialog(owner: UtDialogOwner, tag:String):IUtDialog? {
        return when(owner.lifecycleOwner) {
            is FragmentActivity -> findChildDialog(owner.lifecycleOwner, tag)
            is Fragment         -> findChildDialog(owner.lifecycleOwner, tag)
            else -> null
        }
    }

    /**
     * 現在アクティブなダイアログがあれば取得する。
     * このメソッドは、１つの親（Activity or Fragment, UtDialog）は、最大１つの子ダイアログを持つ直鎖を構成することを前提としており、
     * これが分岐する（１つの親から２つ以上の子ダイアログを同時に表示する）ことは想定しない。
     */
    fun currentDialog(fm: FragmentManager):Sequence<IUtDialog> = sequence<IUtDialog> {
        var leaf :IUtDialog? = null
        val children = dialogChildren(fm)
        for (d in children) {
            val descendant = dialogChildren(d.asFragment.childFragmentManager)
            if(descendant.isEmpty()) {
                yield(d)
            } else {
                yieldAll(currentDialog(d.asFragment.childFragmentManager))
            }
        }
    }
}