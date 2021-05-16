package io.github.toyota32k.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

interface IUtDialog {
    /**
     * ダイアログの状態
     */
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

    /**
     * 子ダイアログを開くときに親ダイアログを隠すかどうか
     */
    enum class ParentVisibilityOption {
        NONE,                   // 何もしない：表示しっぱなし
        HIDE_AND_SHOW,          // このダイアログを開くときに非表示にして、閉じるときに表示する
        HIDE_AND_LEAVE_IT       // このダイアログを開くときに非表示にして、あとは知らん
    }
    var parentVisibilityOption:ParentVisibilityOption

    var immortalTaskName:String?

    /**
     * ダイアログの表示/非表示
     */
    var visible:Boolean

    /**
     * ダイアログを表示する
     */
    fun show(activity: FragmentActivity, tag:String?)
    fun show(fragment: Fragment, tag:String?)

    /**
     * ダイアログ状態をセットして閉じる
     */
    fun complete(status: Status= Status.POSITIVE)

    /**
     * ダイアログを（キャンセルして）閉じる
     */
    fun cancel()

    fun ensureArguments(): Bundle

    val asFragment: DialogFragment
}

