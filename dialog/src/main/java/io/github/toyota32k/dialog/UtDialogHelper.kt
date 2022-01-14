@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.utils.reverse

object UtDialogHelper {
    /**
     * スタックの先頭から、すべてのダイアログ(UtDialog)を列挙
     */
    fun allDialogs(activity: FragmentActivity) : List<UtDialog> {
        return activity.supportFragmentManager.fragments.mapNotNull { it as? UtDialog }
    }

    /**
     * スタックの先頭から、すべてのダイアログ（UtDialog)と、メッセージボックス(IUtDialog)を列挙する。
     */
    fun allDialogsAndMessageBoxes(activity: FragmentActivity) : List<IUtDialog> {
        return activity.supportFragmentManager.fragments.mapNotNull { it as? IUtDialog }
    }

    /**
     * dialogの親（スタックの一つ前）を取得
     */
    fun parentDialog(dialog: UtDialog):UtDialog? {
        val list = allDialogs(dialog.asFragment.requireActivity())
        val index = list.indexOf(dialog)
        return if(index<=0) {
            null
        } else {
            list[index-1]
        }
    }

    /**
     * ルートダイアログ（ダイアログスタックの先頭）を取得
     */
    fun rootDialog(activity:FragmentActivity): UtDialog? {
        return allDialogs(activity).firstOrNull()
    }

    /**
     * activityに属するダイアログをすべてキャンセルする。
     */
    fun cancelAllDialogs(activity:FragmentActivity) {
        val list = allDialogsAndMessageBoxes(activity)
        for(d in list.reverse()) {
            d.cancel()
        }
    }

    /**
     * タグからダイアログを検索
     */
    fun findDialog(activity:FragmentActivity, tag:String):UtDialog? {
        return activity.supportFragmentManager.fragments.mapNotNull { if(it.tag==tag) it as? UtDialog else null }.firstOrNull()
    }

    fun findDialog(owner: UtDialogOwner, tag:String):IUtDialog? {
        return when(owner.lifecycleOwner) {
            is FragmentActivity -> findDialog(owner.lifecycleOwner, tag)
            is Fragment         -> findDialog(owner.lifecycleOwner.requireActivity(), tag)
            else -> null
        }
    }
}