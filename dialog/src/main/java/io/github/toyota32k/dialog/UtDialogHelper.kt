@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
        return try {
            val list = allDialogs(dialog.asFragment.requireActivity())
            val index = list.indexOf(dialog)
            if (index <= 0) {
                null
            } else {
                list[index - 1]
            }
        } catch(e:Throwable) {
            logger.stackTrace(e)
            null
        }
    }

    fun parentDialogHost(dialog:IUtDialog): IUtDialogHost? {
        return try {
            val list = allDialogsAndMessageBoxes(dialog.asFragment.requireActivity())
            val index = list.indexOf(dialog)
            if (index <= 0) {
                null
            } else {
                list[index - 1] as? IUtDialogHost
            }
        } catch(e:Throwable) {
            logger.stackTrace(e)
            null
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
     * refuge()で引きはがしたダイアログを、Activityに戻したり、そのまま閉じたりする i/f
     */
    interface IRefuge {
        fun dismiss()
        fun restore(activity: FragmentActivity)
    }
    interface IDialogRefuge {
        fun dismiss()
        fun restore(transaction: FragmentTransaction)
    }

    private class Refuges(val list: List<IDialogRefuge>): IRefuge {
        override fun dismiss() {
            list.forEach { it.dismiss() }
        }

        override fun restore(activity: FragmentActivity) {
            activity.supportFragmentManager
                .beginTransaction()
                .apply {
                    list.forEach { it.restore(this) }
                }
                .commit()
            activity.supportFragmentManager.executePendingTransactions()
        }
    }

    /**
     * ダイアログをActivityから引きはがして退避する。
     *
     * Activityで、setContentView()によってビューの構成を変更するとき（例：onConfigurationChangedでレイアウトを変更する、など）、
     * ダイアログを表示したまま（Activityのビューにaddした状態のまま）setContentView()を呼び出すと、ダイアログが消えてしまう。
     * これを防ぐために、ダイアログを一時的にActivityから引きはがして、Activityのビュー構成を変更した後、IRefuge#restore()で元に戻す。
     *
     * このメソッドがIRefugeインスタンスを返した場合は、必ず、restore()または、dismiss()を呼び出すこと。（--> タスクがリークし、同名のタスクが実行できなくなる。）
     */
    fun refugeAll(activity: FragmentActivity): IRefuge? {
        val transaction = activity.supportFragmentManager.beginTransaction()
        val list = allDialogsAndMessageBoxes(activity).reverse().mapNotNull { (it as? UtDialog)?.refuge(transaction) }.reverse().toList()
        if (list.isEmpty()) {
            return null
        } else {
            transaction.commit()
            activity.supportFragmentManager.executePendingTransactions()
            return Refuges(list)
        }
    }


    /**
     * ActivityがFinishするとき(onDestroyでisFinishing==trueのとき）に、
     * まだ開いたままになっているダイアログ（＝ImmortalTaskで待ち合わせているダイアログ）を強制的に閉じて notifyResultを発行することにより、
     * 待ち合わせタスクを解放する。
     */
    fun forceCloseAllDialogs(activity: FragmentActivity) {
        try {
            val list = allDialogsAndMessageBoxes(activity)
            for (d in list.reverse()) {
                d.forceDismiss()
            }
        } catch(e:Throwable) {
            // ダイアログを表示した状態でアプリを終了すると例外が出るケースがある
            logger.stackTrace(Log.INFO, e)
        }
    }


    fun childrenOf(dialog:UtDialog) : List<UtDialog> {
        return try {
            val list = allDialogs(dialog.requireActivity())
            val current = list.indexOf(dialog)
            if (current >= 0) {
                list.drop(current + 1)
            } else {
                emptyList()
            }
        } catch(e:Throwable) {
            logger.stackTrace(e)
            emptyList()
        }
    }

    /**
     * タグからダイアログを検索
     */
    fun findDialog(fragmentManager: FragmentManager, tag: String): UtDialog? {
//        val f1 = fragmentManager.findFragmentByTag(tag)
//        val f2 = fragmentManager.fragments.mapNotNull { if (it.tag == tag) it as? UtDialog else null }.firstOrNull()
//        logger.debug("findDialog:1=${f1!=null}, 2=${f2!=null}")
//        return fragmentManager.fragments.mapNotNull { if (it.tag == tag) it as? UtDialog else null }.firstOrNull()
        return fragmentManager.fragments.firstNotNullOfOrNull { if (it.tag == tag) it as? UtDialog else null }
    }

    fun findDialog(activity: FragmentActivity, tag: String): UtDialog? {
        return findDialog(activity.supportFragmentManager, tag)
    }

    fun findDialog(owner: UtDialogOwner, tag: String): IUtDialog? {
        return when (owner.lifecycleOwner) {
            is FragmentActivity -> findDialog(owner.lifecycleOwner, tag)
            is Fragment -> try { findDialog(owner.lifecycleOwner.requireActivity(), tag) } catch(e:Throwable) { logger.stackTrace(e); null }
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