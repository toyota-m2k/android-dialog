@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.content.Context
import androidx.annotation.ColorInt

/**
 * アプリ内で共通のダイアログ動作に関する設定をここにまとめます。
 */
object UtDialogConfig {
    /**
     * UtStandardString.setContext()を忘れると OK/Cancel などの文字が表示されなくなるので、ここにも初期化用のメソッドを置いておこう。
     */
    fun setup(context: Context, table:IUtStringTable?=null) {
        UtStandardString.setContext(context, table)
    }

    /**
     * デフォルトで isDialogをtrueにするかどうか？
     * true: ダイアログモード (新しいwindowを生成して配置）
     * false: フラグメントモード (ActivityのWindow上に配置）
     */
    var showInDialogModeAsDefault = false

    /**
     * Edge-to-Edge を有効にするか？
     * API35 ではデフォルトになるらしい。
     */
    var edgeToEdgeEnabled = true

    /**
     * UtDialog.show()の動作指定フラグ
     * true: UtDialog#show()で、FragmentManager#executePendingTransactions()を呼ぶ
     * false: FragmentManagerのスケジューリングに任せる。
     */
    enum class ShowDialogMode {
        Commit,         // use FragmentTransaction#commit()
        CommitNow,      // use FragmentTransaction#commitNow()
        Immediately,    // use FragmentTransaction$commit() & FragmentManager#executePendingTransactions()
    }
    var showDialogImmediately:ShowDialogMode = ShowDialogMode.Immediately

    /**
     * Phone の場合、全画面を灰色で塗りつぶす（背景のビューを隠す）
     * サブダイアログに切り替わるときに、一瞬、後ろが透けて見えるのがブサイク、という意見があるので。
     * true にすると、UtDialog.isPhone==true のとき、ダイアログの背景をGuardColor.SOLID_GRAY にする。
     */
    var solidBackgroundOnPhone:Boolean = true

    /**
     * ダイアログの外側のウィンドウを覆うガードビューの色
     */
    @ColorInt
    var defaultGuardColor:Int = UtDialog.GuardColor.THEME_DIM.color

    /**
     * ダイアログの外側をタップして閉じるタイプのダイアログのガードビューの色
     */
    @ColorInt
    var defaultGuardColorOfCancellableDialog:Int = UtDialog.GuardColor.TRANSPARENT.color

    /**
     * ダイアログがビジーの時にボディビューを覆うボディガードビューの色
     */
    @ColorInt
    var defaultBodyGuardColor:Int = UtDialog.GuardColor.THEME_SEE_THROUGH.color

    /**
     * ダイアログのスタイル
     */
    var dialogTheme: Int = R.style.UtDialogTheme

    var fadeInDuration:Long = 300L
    var fadeOutDuraton:Long = 400L
}