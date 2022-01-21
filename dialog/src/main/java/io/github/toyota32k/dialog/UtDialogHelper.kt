@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.github.toyota32k.utils.reverse

object UtDialogHelper {
    val logger get() = UtDialogBase.logger
    /**
     * スタックの先頭から、すべてのダイアログ(UtDialog)を列挙
     */
    fun allDialogs(activity: FragmentActivity): List<UtDialog> {
        return activity.supportFragmentManager.fragments.mapNotNull { it as? UtDialog }
    }

    /**
     * スタックの先頭から、すべてのダイアログ（UtDialog)と、メッセージボックス(IUtDialog)を列挙する。
     */
    fun allDialogsAndMessageBoxes(activity: FragmentActivity): List<IUtDialog> {
        return activity.supportFragmentManager.fragments.mapNotNull { it as? IUtDialog }
    }

    /**
     * dialogの親（スタックの一つ前）を取得
     */
    fun parentDialog(dialog: UtDialog): UtDialog? {
        val list = allDialogs(dialog.asFragment.requireActivity())
        val index = list.indexOf(dialog)
        return if (index <= 0) {
            null
        } else {
            list[index - 1]
        }
    }

    /**
     * ルートダイアログ（ダイアログスタックの先頭）を取得
     */
    fun rootDialog(activity: FragmentActivity): UtDialog? {
        return allDialogs(activity).firstOrNull()
    }

    /**
     * 現在（スタックの一番上）のダイアログ
     */
    fun currentDialog(activity: FragmentActivity):UtDialog? {
        return allDialogs(activity).lastOrNull()
    }

    /**
     * 外部から、現在のダイアログを閉じる。
     * DialogFragment#show()で（ダイアログとして）表示されたダイアログは、何もしなくても Backボタンで閉じるが、
     * FragmentTransaction#add/commit で表示したダイアログは、自力で閉じる必要があるので、Activity#onBackPressまたは、onKeyDownイベントから、このメソッドを呼び出すこと。
     * UtMortalActivityには、onBackPressハンドラに、実装ずみだが、それらをオーバーライドする場合は要注意。
     */
    fun cancelCurrentDialog(activity: FragmentActivity) : Boolean {
        val dlg = currentDialog(activity)
        if(dlg!=null && !dlg.isDialog) {
            dlg.cancel()
            return true
        }
        return false
    }

    /**
     * activityに属するダイアログをすべてキャンセルする。
     */
    fun cancelAllDialogs(activity: FragmentActivity) {
        val list = allDialogsAndMessageBoxes(activity)
        for (d in list.reverse()) {
            d.cancel()
        }
    }

    /**
     * タグからダイアログを検索
     */
    fun findDialog(fragmentManager: FragmentManager, tag: String): UtDialog? {
//        val f1 = fragmentManager.findFragmentByTag(tag)
//        val f2 = fragmentManager.fragments.mapNotNull { if (it.tag == tag) it as? UtDialog else null }.firstOrNull()
//        logger.debug("findDialog:1=${f1!=null}, 2=${f2!=null}")
        return fragmentManager.fragments.mapNotNull { if (it.tag == tag) it as? UtDialog else null }.firstOrNull()
    }

    fun findDialog(activity: FragmentActivity, tag: String): UtDialog? {
        return findDialog(activity.supportFragmentManager, tag)
    }

    fun findDialog(owner: UtDialogOwner, tag: String): IUtDialog? {
        return when (owner.lifecycleOwner) {
            is FragmentActivity -> findDialog(owner.lifecycleOwner, tag)
            is Fragment -> findDialog(owner.lifecycleOwner.requireActivity(), tag)
            else -> null
        }
    }

    /**
     * ダイアログ表示中か？
     */
    fun isDialogShown(activity: FragmentActivity): Boolean {
        return activity.supportFragmentManager.fragments.firstOrNull { it is IUtDialog } != null
    }
}