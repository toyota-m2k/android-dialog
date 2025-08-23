@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.window.OnBackInvokedDispatcher
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import io.github.toyota32k.dialog.UtDialog.KeyboardAdjustMode
import io.github.toyota32k.dialog.UtDialog.KeyboardAdjustStrategy

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
     * UtDialog#isDialog のデフォルト値
     * true: ダイアログモード (新しいwindowを生成して配置）
     * false: フラグメントモード (ActivityのWindow上に配置）
     */
    var showInDialogModeAsDefault = true

    object SystemZone {
        val SYSTEM_BARS:Int = WindowInsetsCompat.Type.systemBars()
        val IME:Int = WindowInsetsCompat.Type.ime()
        val CUTOUT:Int = WindowInsetsCompat.Type.displayCutout()

        // combinations
        val NORMAL:Int = SYSTEM_BARS or CUTOUT
        val ALL:Int = SYSTEM_BARS or IME or CUTOUT

        fun calcInsets(insets:WindowInsetsCompat,zones:Int): Insets {
            var all = Insets.NONE
            if ((zones and SYSTEM_BARS) == SYSTEM_BARS) {
                all = insets.getInsets(SYSTEM_BARS)
            }
            if ((zones and IME) == IME) {
                all = Insets.max(all, insets.getInsets(IME))
            }
            if ((zones and CUTOUT) == CUTOUT) {
                all = Insets.max(all, insets.getInsets(CUTOUT))
            }
            return all
        }
    }

    enum class SystemZoneOption(val value:Int) {
        NONE(0),                    // 何もしない
        FIT_TO_ACTIVITY(1),         // ActivityのWindowに合わせる
        HIDE_ACTION_BAR(2),         // ActionBarを非表示にしてできるだけ全画面に表示（cutoutもよけない）
        CUSTOM_INSETS(3),           // systemZoneFlags に従う
        ;
        companion object {
            fun of(value:Int):SystemZoneOption {
                return entries.firstOrNull { it.value == value } ?: NONE
            }
        }
    }

    var systemZoneOption: SystemZoneOption = SystemZoneOption.FIT_TO_ACTIVITY
    var systemZoneFlags:Int = SystemZone.NORMAL

    /**
     * ソフトウェアキーボードが表示された時に、フォーカスのあるEditTextが見えるよう
     * コンテンツを自動調整するかどうかを指定するフラグ
     * true: 自動調整する（デフォルト）
     * false: 自動調整しない
     */
    var adjustContentForKeyboard:KeyboardAdjustMode = KeyboardAdjustMode.NONE

    /**
     * KeyboardAdjustMode.NONE以外の場合に、どうやってコンテンツを自動調整するか。
     */
    var adjustContentsStrategy: KeyboardAdjustStrategy = KeyboardAdjustStrategy.PAN

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
    var solidBackgroundOnPhone:Boolean = false

    /**
     * ダイアログの外側のウィンドウを覆うガードビューの色
     */
    var defaultGuardColor:UtDialog.GuardColor = UtDialog.GuardColor.THEME_DIM

    /**
     * ダイアログの外側をタップして閉じるタイプ(cancellable=true)のダイアログのガードビューの色
     */
    var defaultGuardColorOfCancellableDialog:UtDialog.GuardColor = UtDialog.GuardColor.TRANSPARENT

    /**
     * ダイアログがビジーの時にボディビューを覆うボディガードビューの色
     */
    var defaultBodyGuardColor:UtDialog.GuardColor = UtDialog.GuardColor.THEME_SEE_THROUGH

    /**
     * ダイアログのスタイル
     */
    @StyleRes
    var dialogTheme: Int = R.style.UtDialogTheme

    /**
     * ダイアログフレームレイアウトのリソースID
     * R.layout.dialog_frame は Material3 専用
     * Material2 (Theme.MaterialComponents) の場合は、R.layout.dialog_frame_legacy を使う。
     */
    @LayoutRes
    var dialogFrameId: Int = R.layout.dialog_frame

    /**
     * 旧バージョン互換モード
     */
    fun useLegacyTheme() {
        dialogFrameId = R.layout.dialog_frame_legacy
        dialogMarginOnPortrait = null
        dialogMarginOnLandscape = null
    }

    /**
     * フェードイン/アウトアニメーションの遷移時間
     */
    var fadeInDuration:Long = 300L
    var fadeOutDuraton:Long = 400L

    /**
     * rootViewに対するdialogViewのマージン
     * Width/HeightOption FULL/LIMIT/AUTO_SCROLL/CUSTOM を指定したときの最大サイズ決定に使用する。
     * null を指定すればマージンなし。個別には、UtDialog#noDialogMargin で無効化可能。
     */
    var dialogMarginOnPortrait: Rect? = Rect(20, 40, 20, 40)
    var dialogMarginOnLandscape: Rect? = Rect(40, 20, 40, 20)

    /**
     * onBackInvokerDispatcherのプライオリティの基準値 (デフォルト値：OnBackInvokedDispatcher.PRIORITY_DEFAULT）
     */
    var baseBackInvokedDispatcherPriority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { OnBackInvokedDispatcher.PRIORITY_OVERLAY } else { 0 } // Default Priority
}