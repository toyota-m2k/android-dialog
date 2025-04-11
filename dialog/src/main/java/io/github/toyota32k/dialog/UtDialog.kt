@file:Suppress("FunctionName")

package io.github.toyota32k.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.toyota32k.utils.dp2px
import io.github.toyota32k.utils.getAttrColor
import io.github.toyota32k.utils.setLayoutHeight
import io.github.toyota32k.utils.setLayoutWidth
import io.github.toyota32k.utils.setMargin
import io.github.toyota32k.utils.withAlpha
import kotlin.math.max
import kotlin.math.min

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class UtDialog: UtDialogBase() {
    // region 動作/操作モード

    /**
     * bodyViewをスクロール可能にするかどうか。
     * trueにセットする場合は、sizingOptionを　COMPACT 以外にセットする。AUTO_SCROLLを推奨
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var scrollable: Boolean by bundle.booleanFalse

    /**
     * 画面外をタップしてダイアログを閉じるとき、Positive()扱いにするか？
     * true: positive扱い
     * false: negative扱い（デフォルト）
     */
    protected var positiveCancellable: Boolean by bundle.booleanFalse

    override fun onCancellableChanged(value: Boolean) {
        if (this::rootView.isInitialized) {
            applyGuardColor()
        }
    }

    /**
     * DialogFragmentが isCancelable というプロパティを持っていていることに気づいた。
     * ダイアログの場合、あっちのは使わないから、そっとオーバーライドしておく。
     */
    override fun setCancelable(cancelable: Boolean) {
        this.cancellable = cancelable
    }

    override fun isCancelable(): Boolean {
        return cancellable
    }


    /**
     * Drag&Dropによるダイアログ移動を許可するか？
     * true:許可する
     * false:許可しない（デフォルト）
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var draggable: Boolean by bundle.booleanFalse

    /**
     * ドラッグ中に上下方向の位置を画面内にクリップするか？
     * true: クリップする（デフォルト）
     * false:クリップしない
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var clipVerticalOnDrag: Boolean by bundle.booleanTrue

    /**
     * ドラッグ中に左右方向の位置を画面内にクリップするか？
     * true: クリップする（デフォルト）
     * false:クリップしない（とはいえ、操作できる程度にはクリップするよ。）
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var clipHorizontalOnDrag: Boolean by bundle.booleanTrue

    /**
     * ダイアログを開く/閉じるの動作にアニメション効果をつけるか？
     * true: つける
     * false:つけない
     */
    var animationEffect: Boolean by bundle.booleanTrue

    /**
     * ヘッダ（ok/cancelボタンやタイトル）無しにするか？
     * true: ヘッダなし
     * false: ヘッダあり（デフォルト）
     */
    var noHeader: Boolean by bundle.booleanFalse

    /**
     * フッタ（新UIのok/cancelボタンやタイトル）無しにするか？
     * true: フッタなし
     * false: フッタあり（デフォルト）
     */
    var noFooter: Boolean by bundle.booleanFalse


    /**
     * ビルトインボタン(ok/cancelなど）を非表示(BuiltInButton.NONE)にしたとき、そのボタンの領域をなくすか、見えないがそこにあるものとしてレンダリングするか？
     * （早い話、Goneにするか、Invisibleにするか）
     * true: ボタンの領域をなくす（Gone) ... ボタンの片方だけ表示すると、タイトルが左右に偏って表示されるので注意。
     * false: ボタンは見えなくても、そこにある感じにレンダリング(Invisible) : default
     */
    var invisibleBuiltInButton: Boolean by bundle.booleanTrue

    @Deprecated("use noInvisibleBuiltInButton instead")
    var noInvisibleHeaderButton: Boolean
        get() = !invisibleBuiltInButton
        set(v) { invisibleBuiltInButton = !v }

    /**
     * bodyContainerのマージン
     * ボディ部分だけのメッセージ的ダイアログを作る場合に、noHeader=true と組み合わせて使うことを想定
     * 上下左右を個別にカスタマイズするときは、onViewCreated()で、bodyContainerのマージンを直接操作する。
     * -1: デフォルト（8dp)
     * >=0: カスタムマージン(dp)
     */
    var bodyContainerMargin: Int by bundle.intMinusOne

    /**
     * UtDialogConfig.dialogMarginOnPortrait / dialogMarginOnLandscape によるマージン設定を無効化する場合は true をセットする。
     */
    var noDialogMargin: Boolean by bundle.booleanFalse

    /**
     * isDialog == true の場合に、StatusBar を非表示にして、全画面にダイアログを表示するか？
     * フラグメントモード(isDialog==false)の場合には無視される。
     * true にすると、
     * - StatusBar は非表示
     * - FLAG_LAYOUT_NO_LIMITS を設定して、rootView を全画面に表示
     * - LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES をセット（切り欠き部分にも表示する）
     *
     * NoActionBar系のスタイルを適用し、（プログラム的に）StatusBarを非表示にしたとき、
     * Activityの root view が、切り欠き（インカメラ）部分を含む、スクリーン全体に表示される。
     * フラグメントモードの場合は、Activity（のwindow）上に構築されるので、Activityの状態に応じた適切な領域にダイアログの rootView が配置されるが、
     * ダイアログモードの場合、Activityとは独立した window が作成されるが、StatusBar は表示された状態となり、切り欠き部分を避けた領域に rootViewが配置される。
     * 背景が透明なダイアログなら、あまり問題はないが、
     * 背景をguardView で隠すタイプ（cancellable == falseの場合など）は、Activityの一部が露出した感じの表示になって不格好となる。
     * その場合は、hideStatusBarOnDialogMode = true として、スクリーン全体を覆うよう指示することとした。
     *
     * Activity の window と同じ状態を Dialogのwindow に再現しようと、いろいろ試みたが、どうもうまくいかないので、プロパティで渡すことにした。
     * 将来よい方法が見つかれば。。。
     */
    var hideStatusBarOnDialogMode: Boolean by bundle.booleanWithDefault(UtDialogConfig.hideStatusBarOnDialogMode)

    // endregion

    // region ダイアログサイズ

    /*------------------------------------------------------------------------------------------------------------
        ダイアログのレンダリングに関する覚書

        FrameLayout(rootView)を全画面表示としておき、dialog_view (dialogView) を（プログラム的に）指定位置に配置する。
        dialog_view (dialogView) / body_container/body_scroller (bodyContainer) の layout_width / height は、
        WidthOption/HeightOption に従ってプログラム側から設定される。

        COMPACT
            dialogView        layout_width/height = wrap_content
            bodyContainer    layout_width/height = wrap_content
        FIXED
            dialogView        layout_width/height = wrap_content のまま
            bodyContainer    layout_width/height = widthHint/heightHintで与えられた値
            つまり、コンテント(bodyContainer)の幅を固定することで、dialogView の幅も固定される、という戦略。
        FULL
            dialogView        layout_width/height = match_parent
            bodyContainer    layout_width/height = 0dp (ConstraintLayout の layout_constraintStart_toStartOf/layout_constraintEnd_toEndOf =parent によってdialogViewのサイズと同じになる）
            FULLの場合に限り、dialogView側のサイズを match_parent にして、bodyContainerのサイズをゼロにすることで、内側のサイズを外側に合わせるレンダリングとなる。
            match_parent というOS的な仕掛けを使いたくて、この場合だけイレギュラーな方法になってしまったが、逆に、FIXED の方が、不自然なロジックにも見えるだろう。
            すべて外側から内側に向かって計算するロジックに揃えることもできたが、LIMIT / AUTO_SCROLL / CUSTOM などは、中身に合わせて、外側が伸び縮みする動作を目指したので、
            どうしても、内側から外側というロジックをベースにしたほうが都合がよかったのだ。
        LIMIT / AUTO_SCROLL / CUSTOM
            dialogView        layout_width/height = wrap_content のまま
            bodyContainer    layout_width/height = 計算値（updateDynamicWidth/Height)
            基本的な考え方はFIXEDと同じく、内側のサイズを規定することで、外側のサイズが決まる方式。
     *------------------------------------------------------------------------------------------------------------*/

    /**
     * 幅指定フラグ
     */
    enum class WidthFlag(val param: Int, val isDynamicSizing: Boolean) {
        COMPACT(WRAP_CONTENT, false),        // WRAP_CONTENT
        FULL(MATCH_PARENT, false),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT, false),          // bodyの幅を、widthHint で与えられる値に固定
        LIMIT(WRAP_CONTENT, true),          // FULLと同じだが、widthHintで与えられるサイズでクリップされる。
    }

    data class WidthOption(val flag: WidthFlag, val hint: Int) {
        companion object {
            val COMPACT = WidthOption(WidthFlag.COMPACT, 0)        // WRAP_CONTENT
            val FULL = WidthOption(WidthFlag.FULL, 0)              // フルスクリーンに対して、MATCH_PARENT
            fun FIXED(width: Int) = WidthOption(WidthFlag.FIXED, width)
            fun LIMIT(width: Int) = WidthOption(WidthFlag.LIMIT, width)
        }
    }

    /**
     * 高さ指定フラグ
     */
    enum class HeightFlag(val param: Int, val isDynamicSizing: Boolean) {
        COMPACT(WRAP_CONTENT, false),        // WRAP_CONTENT
        FULL(MATCH_PARENT, false),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT, false),          // bodyの高さを、heightHint で与えられる値に固定
        LIMIT(WRAP_CONTENT, true),           // FULLと同じだが、heightHintで与えられるサイズでクリップされる。
        AUTO_SCROLL(WRAP_CONTENT, true),    // MATCH_PARENTを最大値として、コンテントが収まる高さに自動調整。収まらない場合はスクロールする。（bodyには MATCH_PARENTを指定)
        CUSTOM(WRAP_CONTENT, true),         // AUTO_SCROLL 的な配置をサブクラスで実装する。その場合、calcCustomContainerHeight() をオーバーライドすること。
    }

    data class HeightOption(val flag: HeightFlag, val hint: Int) {
        companion object {
            val COMPACT = HeightOption(HeightFlag.COMPACT, 0)        // WRAP_CONTENT
            val FULL = HeightOption(HeightFlag.FULL, 0)              // フルスクリーンに対して、MATCH_PARENT
            val AUTO_SCROLL = HeightOption(HeightFlag.AUTO_SCROLL, 0)
            val CUSTOM = HeightOption(HeightFlag.CUSTOM, 0)
            fun FIXED(height: Int) = HeightOption(HeightFlag.FIXED, height)
            fun LIMIT(height: Int) = HeightOption(HeightFlag.LIMIT, height)
        }
    }

    private var widthFlag: WidthFlag by bundle.enum(WidthFlag.COMPACT)

    /**
     * widthOption = FIXED or LIMIT を指定したときに、ダイアログ幅(dp)を指定
     */
    private var widthHint: Int by bundle.intZero

    /**
     * 幅の決定方法を指定
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var widthOption: WidthOption
        get() = WidthOption(widthFlag, widthHint)
        set(v) {
            widthFlag = v.flag
            widthHint = v.hint
        }


    private var heightFlag: HeightFlag by bundle.enum(HeightFlag.COMPACT)

    /**
     * heightOption = FIXED の場合の、ダイアログ高さ(dp)を指定
     */
    private var heightHint: Int by bundle.intZero

    /**
     * 高さの決定方法を指定
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    var heightOption: HeightOption
        get() = HeightOption(heightFlag, heightHint)
        set(v) {
            heightFlag = v.flag
            heightHint = v.hint
        }

    /**
     * ダイアログの高さを指定して、高さ固定モードにする。
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    @Deprecated("use HeightOption.FIXED() instead")
    fun setFixedHeight(height: Int) {
        if (isViewInitialized) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        heightFlag = HeightFlag.FIXED
        heightHint = height
    }

    /**
     * ダイアログの高さを指定して、最大高さ指定付き可変高さモードにする。
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    @Deprecated("use HeightOption.LIMIT() instead")
    fun setLimitHeight(height: Int) {
        if (isViewInitialized) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        heightFlag = HeightFlag.LIMIT
        heightHint = height
    }

    /**
     * ダイアログの幅を指定して、幅固定モードにする。
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    @Deprecated("use WidthOption.FIXED() instead")
    fun setFixedWidth(width: Int) {
        if (isViewInitialized) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        widthFlag = WidthFlag.FIXED
        widthHint = width
    }

    /**
     * ダイアログの幅を指定して、最大幅指定付き可変幅モードにする。
     * createBodyView()より前（コンストラクタか、preCreateBodyView()）にセットする。
     */
    @Deprecated("use WidthOption.LIMIT() instead")
    fun setLimitWidth(width: Int) {
        if (isViewInitialized) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        widthFlag = WidthFlag.LIMIT
        widthHint = width
    }

    /**
     * heightOption = CUSTOM を設定する場合は、このメソッドをオーバーライドすること。
     * @param currentBodyHeight         現在のBodyビュー（createBodyViewが返したビュー）の高さ
     * @param currentContainerHeight    現在のコンテナ（Bodyの親）の高さ。マージンとか弄ってなければ currentBodyHeightと一致するはず。
     * @param maxContainerHeight        コンテナの高さの最大値（このサイズを超えないよう、Bodyの高さを更新すること）
     * @return コンテナの高さ（bodyではなく、containerの高さを返すこと）
     */
    protected open fun calcCustomContainerHeight(currentBodyHeight: Int, currentContainerHeight: Int, maxContainerHeight: Int): Int {
        error("calcCustomContainerHeight() must be overridden in subclass on setting 'heightOption==CUSTOM'")
    }

    /**
     * heightOption = CUSTOM の場合、bodyビュー (createBodyViewが返したビュー）の高さが変化したときに、
     * ダイアログサイズを再計算・更新するために、このメソッドを呼び出してください。
     */
    protected fun updateCustomHeight() {
        onRootViewSizeChanged()
    }

    // endregion

    // region ダイアログの表示位置

    /**
     * ダイアログ位置の指定フラグ
     */
    enum class GravityOption(val gravity: Int) {
        RIGHT_TOP(Gravity.END or Gravity.TOP),          // 右上（デフォルト）
        CENTER(Gravity.CENTER),                         // 画面中央（メッセージボックス的）
        LEFT_TOP(Gravity.START or Gravity.TOP),         // 左上...ほかの組み合わせも作ろうと思えば作れるが、俺は使わん。
        CUSTOM(Gravity.START or Gravity.TOP),           // customPositionX/customPositionY で座標（rootViewに対するローカル座標）を指定
    }

    /**
     * ダイアログの表示位置を指定
     */
    var gravityOption: GravityOption by bundle.enum(GravityOption.CENTER)

    /**
     * gravityOption == CUSTOM の場合のX座標（rootViewローカル座標）
     * D&D(draggable==true)中の座標保存にも使用。
     */
    var customPositionX: Float? by bundle.floatNullable

    /**
     * gravityOption == CUSTOM の場合のY座標（rootViewローカル座標）
     * D&D(draggable==true)中の座標保存にも使用。
     */
    var customPositionY: Float? by bundle.floatNullable

    // endregion

    // region ガードビュー（ダイアログの「画面外」）

    /**
     * ガードビュー（ダイアログの「画面外」）の背景の描画方法フラグ
     */
//    enum class GuardColorX(@ColorInt val color:Int) {
//        INVALID(Color.argb(0,0,0,0)),                       // 透明（無効値）
//        TRANSPARENT(Color.argb(0,0xFF,0xFF,0xFF)),          // 透明（通常、 cancellable == true のとき用）
//        DIM(Color.argb(0xB0,0,0,0)),                        // 黒っぽいやつ　（cancellable == false のとき用）
//        SEE_THROUGH(Color.argb(0xB0,0xFF, 0xFF, 0xFF)),     // 白っぽいやつ　（好みで）
//        SOLID_GRAY(Color.rgb(0xc1,0xc1,0xc1)),
//
//        THEME_DIM(Color.argb(0, 2,2,2)),                    // colorSurface の反対色で目立つように覆う（colorSurfaceが白なら黒っぽい/黒なら白っぽい色で覆う）
//        THEME_SEE_THROUGH(Color.argb(0, 3,3,3)),            // colorSurface と同じ色で、コントラストを落とすような感じ。
//    }
    data class GuardColor(@ColorInt val rawColor: Int, @AttrRes val dynamic: Int?, val dynamicAlpha: Int = 0xB0) {
        constructor(@ColorInt color: Int) : this(color, null)

        @ColorInt
        fun color(context: Context): Int {
            return if (dynamic != null) {
                context.theme.getAttrColor(dynamic, rawColor).withAlpha(dynamicAlpha)
            } else {
                rawColor
            }
        }

        val isValid: Boolean get() = rawColor != 0
        val isDynamic: Boolean get() = dynamic != null

        companion object {
            const val INVALID_COLOR = 0
            val INVALID = GuardColor(INVALID_COLOR)
            val TRANSPARENT = GuardColor(Color.argb(0, 0xFF, 0xFF, 0xFF))
            val DIM = GuardColor(Color.argb(0xB0, 0, 0, 0))
            val SEE_THROUGH = GuardColor(Color.argb(0xB0, 0xFF, 0xFF, 0xFF))
            val SOLID_GRAY = GuardColor(Color.rgb(0xc1, 0xc1, 0xc1))
            val THEME_DIM = GuardColor(Color.argb(0xB0, 0, 0, 0), R.attr.color_dlg_text)
            val THEME_SEE_THROUGH = GuardColor(Color.argb(0xB0, 0xFF, 0xFF, 0xFF), com.google.android.material.R.attr.colorSurface, 0xB0)
            fun CUSTOM(@ColorInt color: Int) = GuardColor(color)
        }
    }


    /**
     * ガードビューの色
     */
    // @ColorInt
    private var guardColorValue: Int by bundle.intNonnull(GuardColor.INVALID_COLOR)
    private var guardColorDynamic: Int? by bundle.intNullable
    private var guardColorDynamicAlpha: Int by bundle.intNonnull(0xB0)
    var guardColor: GuardColor
        get() = if (guardColorValue != GuardColor.INVALID_COLOR) GuardColor(guardColorValue, guardColorDynamic, guardColorDynamicAlpha) else GuardColor.INVALID
        set(v) {
            guardColorValue = v.rawColor
            guardColorDynamic = v.dynamic
            guardColorDynamicAlpha = v.dynamicAlpha
        }

    /**
     * ガードビューに色は設定されているか？
     */
    private val hasGuardColor: Boolean
        get() = guardColorValue != GuardColor.INVALID_COLOR

    /**
     * ボディガードビュー の色
     * 注：
     * ボディガードビュー : ダイアログ内のコントロール(Ok/Cancelなどを除く)の操作を禁止するためにかぶせるビュー
     * ガードビュー: ダイアログの外側の操作を禁止するために、Window全体を覆うビュー（== rootView)
     */
    // @ColorInt
    private var bodyGuardColorValue: Int by bundle.intNonnull(GuardColor.INVALID_COLOR)
    private var bodyGuardColorDynamic: Int? by bundle.intNullable
    private var bodyGuardColorDynamicAlpha: Int by bundle.intNonnull(0xB0)
    var bodyGuardColor: GuardColor
        get() = if (bodyGuardColorValue != GuardColor.INVALID_COLOR) GuardColor(bodyGuardColorValue, bodyGuardColorDynamic, bodyGuardColorDynamicAlpha) else UtDialogConfig.defaultBodyGuardColor
        set(v) {
            bodyGuardColorValue = v.rawColor
            bodyGuardColorDynamic = v.dynamic
            bodyGuardColorDynamicAlpha = v.dynamicAlpha
        }

//    @ColorInt
//    private fun Resources.Theme.getAttrColor(@AttrRes attr:Int, @ColorInt def:Int):Int {
//        val typedValue = TypedValue()
//        if(resolveAttribute(attr, typedValue, true)) {
//            return typedValue.data
//        } else {
//            return def
//        }
//    }
//
//    private fun isDark(@ColorInt color:Int) :Boolean {
//        val hsl = FloatArray(3)
//        ColorUtils.colorToHSL(color, hsl)
//        return hsl[2] < 0.5f
//    }
//    private fun autoDim(context:Context):Int {
//        return context.theme.getAttrColor(R.attr.color_dlg_text, 0).withAlpha(0xB0)
//    }
//    private fun autoSeeThrough(context:Context):Int {
//        return context.theme.getAttrColor(R.attr.color_dlg_bg, 0).withAlpha(0xB0)
//    }
//
//    @ColorInt
//    fun resolveColor(@ColorInt color:Int): Int  {
//        return when(color) {
//            GuardColor.THEME_DIM.color -> autoDim(context) //context.getColor(R.color.guard_dim)
//            GuardColor.THEME_SEE_THROUGH.color -> autoSeeThrough(context)   // context.getColor(R.color.guard_see_through)
//            else -> color
//        }
//    }

    /**
     * 実際に描画するガードビューの背景色を取得
     * 優先度
     * １．明示的に設定されている色
     * ２．画面外タップで閉じない（!cancellable)場合は、DIM
     * ３．画面外タップで閉じる(cancellable)なら、無色透明
     */
    @ColorInt
    private fun managedGuardColor(): Int {
        return when {
            UtDialogConfig.solidBackgroundOnPhone && isPhone -> GuardColor.SOLID_GRAY.rawColor
            hasGuardColor -> guardColor.color(context)
            cancellable -> UtDialogConfig.defaultGuardColorOfCancellableDialog.color(context)
            else -> UtDialogConfig.defaultGuardColor.color(context)
        }
    }

    /**
     * 親ダイアログの状態を考慮したガードビューの背景色を設定する
     */
    protected fun applyGuardColor() {
        val color = managedGuardColor()
        rootView.background = color.toDrawable()
    }

    // endregion

    // region フォーカス移動
    class FocusManager(withDialogButtons: Boolean) {
        private val rootFocusManager: UtFocusManager = UtFocusManager()
        private val bodyFocusManager: UtFocusManager? = if (withDialogButtons) {
            rootFocusManager.register(R.id.left_button, R.id.right_button)
            UtFocusManager().apply { rootFocusManager.appendChild(this) }
        } else {
            null
        }

        val root: UtFocusManager get() = rootFocusManager
        val body: UtFocusManager get() = bodyFocusManager ?: rootFocusManager

        fun attach(rootView: View, bodyView: View) {
            if (bodyFocusManager != null) {
                bodyFocusManager.attach(bodyView)
            }
            rootFocusManager.attach(bodyView)
        }

        private var initialFocus: Boolean = false
        fun reserveInitialFocus() {
            initialFocus = true
        }

        fun applyInitialFocus(fallbackView: () -> View) {
            if (initialFocus) {
                initialFocus = false
                if (!root.applyInitialFocus()) {
                    root.setInitialFocus(fallbackView().id)
                    root.applyInitialFocus()
                }
            }
        }
    }

    private var focusManager: FocusManager? = null

    fun enableFocusManagement(withDialogButtons: Boolean = true): UtFocusManager {
        val fm = focusManager ?: FocusManager(withDialogButtons).apply { focusManager = this }
        return fm.body
    }

    /**
     * ソフトウェアキーボードが表示された時に、フォーカスのあるEditTextが見えるよう
     * コンテンツを自動調整するかどうかを指定するフラグ
     * true: 自動調整する（デフォルト）
     * false: 自動調整しない
     */
    var adjustContentForKeyboard: Boolean by bundle.booleanWithDefault(UtDialogConfig.adjustContentForKeyboard)

    // endregion

    // region ダイアログの親子関係、表示/非表示

    /**
     * 子ダイアログを開くときに親ダイアログを隠すかどうかのフラグ
     */
    enum class ParentVisibilityOption {
        NONE,                       // 何もしない：表示しっぱなし
        HIDE_AND_SHOW,              // このダイアログを開くときに非表示にして、閉じるときに表示する
        HIDE_AND_SHOW_ON_NEGATIVE,  // onNegativeで閉じるときには、親を表示する。Positiveのときは非表示のまま
        HIDE_AND_SHOW_ON_POSITIVE,  // onPositiveで閉じるときには、親を表示する。Negativeのときは非表示のまま
        HIDE_AND_LEAVE_IT,          // このダイアログを開くときに非表示にして、あとは知らん
    }

    /**
     * 子ダイアログを開くときに親ダイアログの隠し方
     */
    var parentVisibilityOption by bundle.enum(ParentVisibilityOption.HIDE_AND_SHOW)

    /**
     * ダイアログの表示/非表示
     */
    var visible: Boolean
        get() = rootView.isVisible
        set(v) { rootView.visibility = if(v) View.VISIBLE else View.INVISIBLE }

    private val fadeInAnimation get() = UtFadeAnimation(true, UtDialogConfig.fadeInDuration)
    private val fadeOutAnimation get() = UtFadeAnimation(false, UtDialogConfig.fadeOutDuraton)

    fun fadeIn(completed: (() -> Unit)? = null) {
        if (!this::rootView.isInitialized) {
            completed?.invoke()         // onCreateViewでnullを返す（開かないでcancelされる）ダイアログの場合、ここに入ってくる
        } else if (animationEffect) {
            fadeInAnimation.start(rootView) {
                completed?.invoke()
            }
        } else {
            visible = true
            completed?.invoke()
        }
    }

    fun fadeOut(completed: (() -> Unit)? = null) {
        if (!this::rootView.isInitialized || !visible) {
            completed?.invoke()
        } else if (animationEffect) {
            fadeOutAnimation.start(rootView) {
                visible = false
                completed?.invoke()
            }
        } else {
            visible = false
            completed?.invoke()
        }
    }

    /**
     * ルートダイアログ（ダイアログチェーンの先頭）を取得
     */
    val rootDialog: UtDialog?
        get() = UtDialogHelper.rootDialog(requireActivity())

    /**
     * 親ダイアログを取得
     * 自身がルートならnullを返す。
     */
    val parentDialog: UtDialog?
        get() = UtDialogHelper.parentDialog(this)

    // endregion

    // region タイトルバー

    /**
     * タイトル
     */
    private var privateTitle: String? by bundle.stringNullable
    var title: String?
        get() = privateTitle
        set(v) = replaceTitle(v)

    /**
     * ダイアログ構築後に（動的に）ダイアログタイトルを変更する。
     */
    open fun replaceTitle(title: String?) {
        this.privateTitle = title
        if (this::titleView.isInitialized) {
            titleView.text = title
        }
    }

    /**
     * タイトルバーに表示するボタンのタイプ
     * 標準ボタンのポリシー
     * - タイトルの左はNegativeボタン、 右はPositiveボタンを配置する。
     * - 「閉じる」ボタンは、右（CLOSE：Positive）にも、左（CLOSE_LEFT:Negative）にも配置可能。
     * setLeftButton(), setRightButton()で、これら標準以外のボタンを作成することは可能だが、あまりポリシーから逸脱しないように。
     */
//    enum class BuiltInButtonType(val string:UtStandardString, val positive:Boolean, val blueColor:Boolean) {
//        OK(UtStandardString.OK, true, true),                // OK
//        DONE(UtStandardString.DONE, true, true),            // 完了
//        CLOSE(UtStandardString.CLOSE, true, true),          // 閉じる
//
//        CANCEL(UtStandardString.CANCEL, false, false),      // キャンセル
//        BACK(UtStandardString.BACK, false, false),          // 戻る
//        CLOSE_LEFT(UtStandardString.CLOSE, false, false),   // 閉じる
//
//        NONE(UtStandardString.NONE, false, false),          // ボタンなし
//    }

    data class ButtonType(val string: String?, val positive: Boolean) {
        constructor(@StringRes stringId: Int, positive: Boolean) : this(UtStandardString.getText(stringId), positive)

        companion object {
            val OK get() = ButtonType(UtStandardString.OK.id, true)
            val DONE get() = ButtonType(UtStandardString.DONE.id, true)
            val CLOSE get() = ButtonType(UtStandardString.CLOSE.id, true)
            val CANCEL get() = ButtonType(UtStandardString.CANCEL.id, false)
            val BACK get() = ButtonType(UtStandardString.BACK.id, false)
            val NEGATIVE_CLOSE get() = ButtonType(UtStandardString.CLOSE.id, false)
            val POSITIVE_BACK get() = ButtonType(UtStandardString.BACK.id, true)
            val NONE = ButtonType(null, false)

            fun CUSTOM(string: String, positive: Boolean) = ButtonType(string, positive)
            fun CUSTOM(@StringRes stringId: Int, positive: Boolean) = ButtonType(stringId, positive)
        }
    }

    var leftButtonType: ButtonType
        get() = ButtonType(leftButtonText, leftButtonPositive)
        set(v) = setLeftButton(v.string, v.positive)
    var rightButtonType: ButtonType
        get() = ButtonType(rightButtonText, rightButtonPositive)
        set(v) = setRightButton(v.string, v.positive)

    // 左ボタンのプロパティ (setLeftButton()で設定）
    private var leftButtonText: String? by bundle.stringNullable
    private var leftButtonPositive: Boolean by bundle.booleanFalse

    @Suppress("unused")
    val hasLeftButton: Boolean
        get() = leftButtonText != null

    // 右ボタンのプロパティ（setRightButton()で設定）
    private var rightButtonText: String? by bundle.stringNullable
    private var rightButtonPositive: Boolean by bundle.booleanFalse
    private var rightButtonBlue: Boolean by bundle.booleanFalse

    @Suppress("unused")
    val hasRightButton: Boolean
        get() = rightButtonText != null

    /**
     * 左ボタンのプロパティを細かく指定
     * @param id        ボタンキャプションの文字列リソースID
     * @param positive  true:Positiveボタン(タップしてonPositiveを発行）
     *                  false:Negativeボタン（タップしてonNegativeを発行）
     */
    private fun setLeftButton(string: String?, positive: Boolean = false) {
        leftButtonText = string
        leftButtonPositive = positive
        if (isViewInitialized) {
            updateLeftButton()
        }
    }


    /**
     * 右ボタンのプロパティを細かく指定
     * @param id        ボタンキャプションの文字列リソースID
     * @param positive  true:Positiveボタン(タップしてonPositiveを発行）
     *                  false:Negativeボタン（タップしてonNegativeを発行）
     */
    fun setRightButton(string: String?, positive: Boolean = false) {
        rightButtonText = string
        rightButtonPositive = positive
        if (isViewInitialized) {
            updateRightButton()
        }
    }

    /**
     * 右ボタンのプロパティをタイプで指定
     */
//    fun setRightButton(type: BuiltInButtonType) {
//        setRightButton(type.string.id, type.positive)
//    }

    private val themedContext: Context by lazy { ContextThemeWrapper(super.getContext(), UtDialogConfig.dialogTheme) }
    override fun getContext(): Context {
        return themedContext
    }

    /**
     * ボタンプロパティを、ビュー(Button)に反映する
     */
    private fun updateButton(button: Button, label: String, positive: Boolean) {
        activity?.apply {
            button.text = label
            if (button !is MaterialButton) {
                // legacy design
                if (positive) {
                    button.background = ContextCompat.getDrawable(context, R.drawable.legacy_dlg_button_bg_blue)
                    button.setTextColor(context.getColorStateList(R.color.legacy_dlg_button_text_blue))
                } else {
                    button.background = ContextCompat.getDrawable(context, R.drawable.legacy_dlg_button_bg_white)
                    button.setTextColor(context.getColorStateList(R.color.legacy_dlg_button_text_white))
                }
            }
        }
    }

    /**
     * 左ボタンのプロパティをビュー(Button)に反映
     */
    private fun updateLeftButton() {
        val label = leftButtonText
        if (label != null) {
            updateButton(leftButton, label, leftButtonPositive)
        } else {
            leftButton.visibility = if (invisibleBuiltInButton) View.INVISIBLE else View.GONE
        }
    }

    /**
     * 右ボタンのプロパティをビュー(Button)に反映
     */
    private fun updateRightButton() {
        val label = rightButtonText
        if (label != null) {
            updateButton(rightButton, label, rightButtonBlue)
        } else {
            rightButton.visibility = if (invisibleBuiltInButton) View.INVISIBLE else View.GONE
        }
    }

    // endregion

    // region デバイス情報

    /**
     * デバイスの向き
     */
    val orientation: Int
        get() = resources.configuration.orientation

    /**
     * デバイスは横置き（ランドスケープか）？
     */
    val isLandscape: Boolean
        get() = orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * デバイスは縦置き（ポートレート）か？
     */
    val isPortrait: Boolean
        get() = orientation == Configuration.ORIENTATION_PORTRAIT

    /**
     * デバイスはPhoneか？（600dp以下をPhoneと判定）
     */
    val isPhone: Boolean
        get() = resources.getBoolean(R.bool.under600dp)

    /**
     * デバイスはタブレットか？
     */
    val isTablet: Boolean
        get() = !isPhone

    // endregion

    // region ドラッグ＆ドロップ

    /**
     * 軸方向毎のドラッグ情報を保持するクラス
     */
    private class DragParam {
        private var dialogSize: Float = 0f
        private var screenSize: Float = 0f
        private var clip: Boolean = false
        private var minPos: Float = 0f
        private var maxPos: Float = 0f

        private var orgDialogPos: Float = 0f
        private var dragStartPos: Float = 0f

        fun setup(dialogSize: Float, screenSize: Float, clip: Boolean, minPos: Float, maxPos: Float) {
            this.dialogSize = dialogSize
            this.screenSize = screenSize
            this.clip = clip
            this.minPos = minPos
            this.maxPos = maxPos
        }

        fun start(dialogPos: Float, dragPos: Float) {
            dragStartPos = dragPos
            orgDialogPos = dialogPos
        }

        /**
         * ドラッグ後の位置を取得
         */
        fun getPosition(dragPos: Float): Float {
            val newPos = orgDialogPos + (dragPos - dragStartPos)
            return clipPosition(newPos)
        }

        /**
         * ダイアログの位置を画面内にクリップする
         */
        fun clipPosition(pos: Float): Float {
            return if (clip) {
                max(0f, min(pos, screenSize - dialogSize))
            } else {
                max(minPos, min(pos, screenSize - maxPos))
            }
        }
    }

    /**
     * ドラッグ情報クラス
     */
    inner class DragInfo {
        private var dragging: Boolean = false
        private val x = DragParam()
        private val y = DragParam()

        /**
         * サイズ情報を初期化
         */
        private fun setup() {
            val w = dialogView.width.toFloat()
            x.setup(w, rootView.width.toFloat(), clipHorizontalOnDrag, -w / 2f, w / 2f)
            y.setup(dialogView.height.toFloat(), rootView.height.toFloat(), clipVerticalOnDrag, 0f, requireContext().dp2px(50).toFloat())
        }

        /**
         * D&Dまたは、GravityOption.CUSTOMの場合に、ダイアログが画面（rootView)内に収まるよう、座標位置を補正する。
         */
        fun adjustPosition(xp: Float?, yp: Float?) {
            setup()
            if (xp != null) {
                dialogView.x = x.clipPosition(xp)
            }
            if (yp != null) {
                dialogView.y = y.clipPosition(yp)
            }
        }

        /**
         * ドラッグ開始
         */
        fun start(ev: MotionEvent) {
            setup()
            x.start(dialogView.x, ev.rawX)
            y.start(dialogView.y, ev.rawY)
            dragging = true
        }

        /**
         * ドラッグによる位置移動
         */
        fun move(ev: MotionEvent) {
            if (!dragging) return
            val x = x.getPosition(ev.rawX)
            dialogView.x = x
            customPositionX = x
            val y = y.getPosition(ev.rawY)
            dialogView.y = y
            customPositionY = y
        }

        /**
         * ドラッグ終了
         */
        fun cancel() {
            dragging = false
        }
    }

    /**
     * D&Dで移動したダイアログの位置を元に戻す。
     */
    private fun resetDialogPosition() {
        dialogView.translationX = 0f
        dialogView.translationY = 0f
        customPositionX = null
        customPositionY = null
    }

    /**
     * ドラッグ情報
     */
    private val dragInfo: DragInfo by lazy { DragInfo() }

    /**
     * ドラッグによるダイアログ移動に必要なタッチイベントのハンドラを登録する。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun enableDrag() {
        if (!draggable) return
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // スペシャルサービス仕様：ダブルタップで元の位置に戻してあげよう。
                resetDialogPosition()
                dragInfo.cancel()   // double tap の２回目タップで移動しないように。
                return true
            }
        })
        rootView.findViewById<View>(R.id.header).setOnTouchListener { _, ev ->
            if (!gestureDetector.onTouchEvent(ev)) {
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    dragInfo.start(ev)
                } else if (ev.action == MotionEvent.ACTION_MOVE) {
                    dragInfo.move(ev)
                }
            }
            true
        }
    }

    // endregion

    // region ビルトインビュー

    lateinit var titleView: TextView
        private set
    lateinit var leftButton: Button
        private set
    lateinit var rightButton: Button
        private set
    lateinit var progressRingOnTitleBar: ProgressBar
        private set

    lateinit var rootView: ViewGroup              // 全画面を覆う透過の背景となるダイアログのルート：
        protected set
    lateinit var dialogView: ViewGroup        // ダイアログ画面としてユーザーに見えるビュー。rootView上で位置、サイズを調整する。
        protected set
    lateinit var bodyContainer: ViewGroup          // bodyViewの入れ物
        private set
    lateinit var bodyView: View                      /* UtDialogを継承するサブクラス毎に作成されるダイアログの中身  (createBodyView()で構築されたビュー） */ private set
    lateinit var refContainerView: View              // コンテナ領域（ダイアログ領域ーヘッダー領域）にフィットするダミービュー
        private set
    lateinit var bodyGuardView: FrameLayout           // dialogContentへの操作をブロックするためのガードビュー
        private set
    lateinit var centerProgressRing: ProgressBar     // 中央に表示するプログレスリング：デフォルトでは非表示。bodyGuardView とともに visible にすることで表示される。
        private set

    // endregion

    // region レンダリング

    private fun applyDialogMargin(lp: MarginLayoutParams): MarginLayoutParams {
        if (noDialogMargin) return lp
        if (isLandscape) {
            UtDialogConfig.dialogMarginOnLandscape?.let { m ->
                lp.setMargins(context.dp2px(m.left), context.dp2px(m.top), context.dp2px(m.right), context.dp2px(m.bottom))
            }
        } else {
            UtDialogConfig.dialogMarginOnPortrait?.let { m ->
                lp.setMargins(context.dp2px(m.left), context.dp2px(m.top), context.dp2px(m.right), context.dp2px(m.bottom))
            }
        }
        return lp
    }

    /**
     * widthOption/heightOption/gravityOptionに従って、 bodyContainerのLayoutParamを設定する。
     * 構築時：onCreateDialog()から実行
     */
    private fun setupLayout() {
        val params = (dialogView.layoutParams as? FrameLayout.LayoutParams)?.apply {
            width = widthFlag.param
            height = heightFlag.param
            gravity = gravityOption.gravity
            applyDialogMargin(this)
        } ?: FrameLayout.LayoutParams(widthFlag.param, heightFlag.param, gravityOption.gravity)

        dialogView.layoutParams = params
        if (heightFlag == HeightFlag.FULL) {
            bodyContainer.setLayoutHeight(0)
        }
        if (widthFlag == WidthFlag.FULL) {
            bodyContainer.setLayoutWidth(0)
        }
        setupFixedSize()
        setupDynamicSize()
    }

    /**
     * widthOption/heightOptionで FIXEDが指定されているときに、
     * widthHint/heightHintで与えられたサイズに従ってLayoutParamsを設定する。
     */
    private fun setupFixedSize() {
        val fw = if (widthFlag == WidthFlag.FIXED) widthHint else null
        val fh = if (heightFlag == HeightFlag.FIXED) heightHint else null
        if (fw == null && fh == null) return

        val lp = bodyContainer.layoutParams ?: return
        if (fw != null) {
            lp.width = requireContext().dp2px(fw)
        }
        if (fh != null) {
            lp.height = requireContext().dp2px(fh)
        }
        bodyContainer.layoutParams = lp
    }

    /**
     * widthOption == LIMITまたは、heightOption==AUTO_SCROLL/CUSTOMの場合に、レイアウト更新のため、また、
     * draggable==true または、gravityOption == CUSTOM の場合は、位置補正（画面内にクリップ）のために、それぞれサイズ変更イベントをフックする。
     */
    private fun setupDynamicSize() {
        if (widthFlag.isDynamicSizing || heightFlag.isDynamicSizing || draggable || gravityOption == GravityOption.CUSTOM) {
            // デバイス回転などによるスクリーンサイズ変更を検出するため、ルートビューのサイズ変更を監視する。
            rootView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                if (or - ol != r - l || ob - ot != b - t) {
                    onRootViewSizeChanged()
                }
                adjustDialogPosition(customPositionX, customPositionY)
            }
            refContainerView.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ ->
                if (or - ol != r - l) {
//                    logger.debug("x:org ${dialogView.x} layoutChanged")
                    onContainerHeightChanged()
                }
            }
        }
        if (heightFlag == HeightFlag.AUTO_SCROLL) {
            // コンテンツ（bodyViewの中身）の増減によるbodyViewサイズの変更を監視する。
            bodyView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                if (or - ol != r - l || ob - ot != b - t) {
                    onBodyViewSizeChanged()
                }
            }
        }
    }

    /**
     * リサイズ時の高さ調整
     * （HeightOption.AUTO_SCROLL, HeightOption.CUSTOMのための処理）
     */
    private fun updateDynamicHeight(lp: ConstraintLayout.LayoutParams): Boolean {
        if (heightFlag.isDynamicSizing) {
            val winHeight = rootView.height
            if (winHeight == 0) return false
            val containerHeight = refContainerView.height
            val dlgHeight = dialogView.height + dialogView.marginTop + dialogView.marginBottom
            val bodyHeight = bodyView.height
            val maxContainerHeight = winHeight - (dlgHeight - containerHeight)

            val newContainerHeight = when (heightFlag) {
                HeightFlag.AUTO_SCROLL -> min(bodyHeight, maxContainerHeight)
                HeightFlag.LIMIT -> min(maxContainerHeight, requireContext().dp2px(heightHint))
                HeightFlag.CUSTOM -> calcCustomContainerHeight(bodyHeight, containerHeight, maxContainerHeight)
                else -> return false
            }

//            logger.info("window:${winHeight}, scroller:$scrHeight, dialogView:$dlgHeight, bodyHeight:$bodyHeight, maxScrHeight=$maxScrHeight, newScrHeight=$newScrHeight")
            if (lp.height != newContainerHeight) {
                lp.height = newContainerHeight
                return true
            }
        }
        return false
    }

    /**
     * リサイズ時の幅調整
     *　（WidthOption.LIMIT用の処理）
     * @param lp    bodyContainer の LayoutParams
     */
    private fun updateDynamicWidth(lp: ConstraintLayout.LayoutParams): Boolean {
        if (widthFlag == WidthFlag.LIMIT) {
            val winWidth = rootView.width
            if (winWidth == 0) return false
            val dlgMargin = dialogView.marginStart + dialogView.marginEnd
            val bodyMargin = lp.marginStart + lp.marginEnd // bodyContainer のマージン
            val maxCntWidth = winWidth - dlgMargin - bodyMargin
            val newCntWidth = min(maxCntWidth, requireContext().dp2px(widthHint))
            if (lp.width != newCntWidth) {
                lp.width = newCntWidth
                return true
            }
        }
        return false
    }

    /**
     * 画面（rootView)内に収まるよう補正して、ダイアログ位置を設定
     */
    private fun adjustDialogPosition(x: Float?, y: Float?) {
        if (x != null || y != null) {
            dragInfo.adjustPosition(x, y)
        }
    }

    /**
     * rootViewのサイズが変化したとき（≒デバイス回転）にレンダリング処理
     */
    private fun onRootViewSizeChanged() {
        val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
        val h = updateDynamicHeight(lp)
        val w = updateDynamicWidth(lp)
        if (h || w) {
            bodyContainer.layoutParams = lp
        }
    }

    private fun onContainerHeightChanged() {
        val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
        if (updateDynamicHeight(lp)) {
            bodyContainer.layoutParams = lp
        }
    }

    /**
     * bodyViewのサイズが変更したときの処理。
     * bodyViewの高さに合わせて、bodyContainerの高さを更新する。
     */
    private fun onBodyViewSizeChanged() {
        if (heightFlag == HeightFlag.AUTO_SCROLL) {
            val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
            if (updateDynamicHeight(lp)) {
                // bodyView のOnLayoutChangeListenerの中から、コンテナのサイズを変更しても、なんか１回ずつ無視されるので、ちょっと遅延する。
                Handler(Looper.getMainLooper()).post {
                    bodyContainer.layoutParams = lp
                }
            }
        }
    }

    // endregion

    // region ダイアログの構築

    val isViewInitialized: Boolean
        get() = this::rootView.isInitialized

    /**
     * ダイアログ構築前の処理
     * titleViewなど、UtDialog側のビューを構築するまえに行うべき初期化処理を行う。
     */
    abstract fun preCreateBodyView()

    /**
     * ダイアログビュー専用インフレーター
     * createBodyView()の中で、BodyViewを構築するとき、必ず Dialog#layoutInflater（this.dialog.layoutInflater)を使い、
     * LayoutInflater.layoutInflater()の第２引数(root)に、bodyContainer、第３引数（attachToRoot）にfalse を渡さないと正しく動作しない。
     * これを「規約」として利用者の責任に帰するのは酷なので、これらの引数を隠ぺいした専用インフレーターを渡すAPIとし、そのためのi/fを定義する。
     *
     */

    interface IViewInflater {
        fun inflate(@LayoutRes id: Int): View
        val layoutInflater: LayoutInflater
    }

    /**
     * ダイアログのビューを構築する
     * @param savedInstanceState    FragmentDialog.onCreateView に渡された状態復元用パラメータ
     * @param inflater              専用インフレーター
     */
    protected abstract fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View

    /**
     * IViewInflaterの実装クラス
     */
    private data class ViewInflater(override val layoutInflater: LayoutInflater, val bodyContainer: ViewGroup) : IViewInflater {
        override fun inflate(id: Int): View {
            return layoutInflater.inflate(id, bodyContainer, false)
        }
    }

    private lateinit var keyboardObserver: ISoftwareKeyboardObserver


    /**
     * isDialog == true の場合に呼ばれる。
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.dlg_style).apply {
            window?.let { window ->
                window.setBackgroundDrawable(GuardColor.TRANSPARENT.rawColor.toDrawable())

                if (isDialog && hideStatusBarOnDialogMode) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())

                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // ディスプレイの切り欠き部分 (ノッチなど) にもウィンドウを広げる
                        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

                // キーボードが表示されたら検出するリスナーを設定
                if(adjustContentForKeyboard) {
                    keyboardObserver = UtSoftwareKeyboardObserver.byGlobalLayout(this@UtDialog, requireActivity()).observe(::onSoftwareKeyboardChanged)
                }
            }
        }
    }

    /**
     * コンテントビュー生成処理
     */
    override fun onCreateView(orgInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        try {
            val inflater = orgInflater.cloneInContext(context)
            if (UtDialogConfig.solidBackgroundOnPhone && isPhone) {
                animationEffect = false
            }
            preCreateBodyView()
            rootView = inflater.inflate(UtDialogConfig.dialogFrameId, container, false) as FrameLayout
            (rootView as? UtRootFrameLayout)?.apply { ownerDialog = this@UtDialog }

            if (noHeader) {
                rootView.findViewById<View>(R.id.header).visibility = View.GONE
                rootView.findViewById<View>(R.id.separator).visibility = View.GONE
            }
            if (noFooter) {
                val footer = rootView.findViewById<View>(R.id.footer)
                if (footer != null) {
                    footer.visibility = View.GONE
                }
            }
            leftButton = rootView.findViewById(R.id.left_button)
            rightButton = rootView.findViewById(R.id.right_button)
            titleView = rootView.findViewById(R.id.dialog_title)
            progressRingOnTitleBar = rootView.findViewById(R.id.progress_on_title_bar)
            dialogView = rootView.findViewById(R.id.dialog_view)
            refContainerView = rootView.findViewById(R.id.ref_container_view)
            bodyGuardView = rootView.findViewById(R.id.body_guard_view)
            bodyGuardView.background = bodyGuardColor.color(context).toDrawable()
            centerProgressRing = rootView.findViewById(R.id.center_progress_ring)
            dialogView.isClickable = true   // これをセットしておかないと、ヘッダーなどのクリックで rootViewのonClickが呼ばれて、ダイアログが閉じてしまう。
            title?.let { titleView.text = it }
            if (heightFlag == HeightFlag.AUTO_SCROLL) {
                scrollable = true
            } else if (heightFlag == HeightFlag.COMPACT) {
                scrollable = false
            }
            bodyContainer = if (scrollable) {
                rootView.findViewById(R.id.body_scroller)
            } else {
                rootView.findViewById(R.id.body_container)
            }
            val margin = bodyContainerMargin
            if (margin >= 0) {
                bodyContainer.setMargin(margin, margin, margin, margin)
            }
            bodyContainer.visibility = View.VISIBLE
            leftButton.setOnClickListener(this::onLeftButtonTapped)
            rightButton.setOnClickListener(this::onRightButtonTapped)

            rootView.setOnClickListener(this@UtDialog::onBackgroundTapped)
//            rootView.isFocusableInTouchMode = true
//            rootView.setOnKeyListener(this@UtDialog::onBackgroundKeyListener)
//          画面外タップで閉じるかどうかにかかわらず、リスナーをセットする。そうしないと、ダイアログ外のビューで操作できてしまう。
//            if (lightCancelable) {
//                rootView.setOnClickListener(this@UtDialog::onBackgroundTapped)
//            }
            updateLeftButton()
            updateRightButton()
            bodyView = createBodyView(savedInstanceState, ViewInflater(inflater, bodyContainer))
            bodyContainer.addView(bodyView)
            focusManager?.attach(rootView, bodyView)
            setupLayout()
//            dlg?.setContentView(rootView)
            if (draggable) {
                enableDrag()
            }
            applyGuardColor()
            if (savedInstanceState == null) {
                // 新しくダイアログを開く
                // アニメーションして開くときは、初期状態を非表示にしておく。
                if (animationEffect) {
                    this.visible = false
                }
                // 初回フォーカスセットを予約
                focusManager?.reserveInitialFocus()
            } else {
                // 回転などによる状態復元
                if (parentVisibilityOption != ParentVisibilityOption.NONE) {
                    parentDialog?.visible = false
                }
            }

            if (!isDialog && adjustContentForKeyboard) {
                keyboardObserver = UtSoftwareKeyboardObserver.byWindowInsets(this, rootView).observe(::onSoftwareKeyboardChanged)
            }
            return rootView
        } catch (e: Throwable) {
            // View作り中に例外が出る原因は、主に２つ
            //  1. Viewの inflate に失敗    ... これは製品リリースまでに修正すること。
            //  2. Processが一度死んだため、ViewModelの取得に失敗 ... これは防ぎようがないので、dismiss して、こっそり終了しておく。
            logger.stackTrace(e)
            dismiss()
            notifyResult()  // onDismissも呼ばれないことがあるようだ。
            return null
        }
    }

    override fun onResume() {
        super.onResume()
        focusManager?.applyInitialFocus {
            if (rightButton.isVisible && rightButton.isEnabled) {
                rightButton
            } else {
                leftButton
            }
        }
    }

    // endregion

    // region イベント

    override fun internalCloseDialog() {
        fadeOut {
            super.internalCloseDialog()
        }
    }

    /**
     * ダイアログが表示されるときのイベントハンドラ
     */
    override fun onDialogOpening() {
        fadeIn()
        parentDialog?.let { parent ->
            if (!parent.status.finished) {   // parentのcompleteハンドラの中から別のダイアログを開く場合、parentのfadeOutが完了する前に、ここからfadeOutの追撃が行われ、completeハンドラがクリアされて親ダイアログが閉じられなくなってしまう
                // 子ダイアログが開いた後、親ダイアログが開いたソフトウェアキーボードが残ってしまうと嫌なので、明示的に閉じておく
                parent.hideSoftwareKeyboard()
                if (parentVisibilityOption != ParentVisibilityOption.NONE) {
                    parent.fadeOut()
                }
            }
        }
    }

    /**
     * InputMethodManagerインスタンスの取得
     */

    val immService: InputMethodManager?
        get() = try {
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        } catch (_: Throwable) {
            null
        }

    /**
     * ソフトウェアキーボードを非表示にする。
     */
    fun hideSoftwareKeyboard() {
        activity?.window?.decorView?.requestFocus()    // IMEの未確定ウィンドウが残るのを防ぐ（ダイアログ外のビューにフォーカスをセットする） requestFocusFromTouch()だと、Activity上のビューにフォーカスがセットされて黒くなるので注意！
        immService?.hideSoftInputFromWindow(rootView.windowToken, 0)
    }

    private fun getMaxScrollAmount(scrollableView: ViewGroup): Int {
        return when (scrollableView) {
            is ScrollView -> {
                val childHeight = scrollableView.getChildAt(0)?.height ?: 0
                max(0, childHeight - scrollableView.height - scrollableView.scrollY)
            }
            is RecyclerView -> {
                if (scrollableView.computeVerticalScrollRange() > 0) {
                    scrollableView.computeVerticalScrollRange() - scrollableView.computeVerticalScrollExtent() - scrollableView.computeVerticalScrollOffset()
                } else {
                    0
                }
            }
            is ListView -> {
                val lastVisiblePosition = scrollableView.lastVisiblePosition
                if (lastVisiblePosition >= 0 && lastVisiblePosition < scrollableView.count - 1) {
                    // まだスクロールできる
                    Int.MAX_VALUE / 2  // 正確な値は計算が難しいため、十分大きな値を返す
                } else {
                    0  // もうスクロールできない
                }
            }
            else -> 0
        }
    }
    /**
     * ダイアログモード(isDialog==true)の場合限定
     * ソフトウェアキーボードが開く/閉じるの場合の処理
     * デフォルトでは、rootView のパディングを調整する。
     */
    open fun onSoftwareKeyboardChanged(keyboardHeight: Int) {
//        if(open) {
//            rootView.setPadding(0, 0, 0, keyboardHeight)
//        } else {
//            rootView.setPadding(0, 0, 0, 0)
//        }
        if (keyboardHeight>0) {
            val focusedView = rootView.findFocus() ?: return

            // スクロール可能なコンテナを探す
            var scrollableParent: ViewGroup? = null
            var parent = focusedView.parent

            while (parent is ViewGroup) {
                if (parent is ScrollView || parent is RecyclerView || parent is ListView) {
                    scrollableParent = parent
                    break
                }
                parent = parent.parent
            }

            val rect = Rect()
            focusedView.getGlobalVisibleRect(rect)
            val screenHeight = rootView.height
            val bottomOffset = (rect.bottom + 20) - (screenHeight - keyboardHeight)
            var remainingOffset = bottomOffset

            if (bottomOffset > 0) {
                if (scrollableParent != null) {
                    // スクロール可能ならスクロールで対応
                    val scrollAmount = min(bottomOffset, getMaxScrollAmount(scrollableParent))
                    if (scrollAmount>0) {
                        when (scrollableParent) {
                            is ScrollView -> scrollableParent.smoothScrollBy(0, scrollAmount)
                            is RecyclerView -> scrollableParent.smoothScrollBy(0, scrollAmount)
                            is ListView -> scrollableParent.smoothScrollByOffset(scrollAmount)
                        }
                        remainingOffset -= scrollAmount
                    }
                }
                if (remainingOffset>0) {
                    // スクロール不可能ならrootViewを移動
                    rootView.animate()
                        .translationY(-remainingOffset.toFloat())
                        .setDuration(200)
                        .start()
                }
            }
        } else {
            if(rootView.translationY != 0f) {
                rootView.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    /**
     * ダイアログが閉じる前のイベントハンドラ
     */
    override fun onDialogClosing() {
        if(!isViewInitialized) {
            // onDialogOpening (≒ onViewCreated)が呼ばれずに onDialogClosing()が呼ばれた
            return
        }

        // Android7/8 でダイアログが閉じてもSoftware Keyboardが閉じない事例あり
        hideSoftwareKeyboard()

        // Chromebookで、HWキーボードの候補ウィンドウが残ってしまうのを防止
        // ここで requestFocusFromTouchを呼ぶと、ダイアログが閉じてから、へんなビューのレイヤー（ゾンビ的なやつ？）が親ダイアログの上に出現してしまう。
        // Layout Inspector で見たら、親ダイアログの FrameLayout が、子ビューの上に飛び出しているように見える。。。気持ち悪い。
        // これが原因であることを突き止めるのに、丸二日かかったぞ。
//        rootView.requestFocusFromTouch()

        // 親ダイアログの表示状態を復元
        val parent = parentDialog ?: return
        if(  parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW ||
            (parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE && status.negative) ||
            (parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW_ON_POSITIVE && status.positive)) {
            parent.fadeIn()
        }
    }

    /**
     * 背景（ガードビュー）がタップされたときのハンドラ
     */
    protected open fun onBackgroundTapped(view:View) {
        if(view==rootView && cancellable) {
            if(positiveCancellable) {
                onPositive()
            } else {
                onNegative()
            }
        }
    }

    /**
     * キーイベントハンドラ
     * これは、FragmentやDialogFragmentのメソッドではなく、オリジナルのやつ。UtMortalActivity#onKeyDown()ががんばって呼び出している。
     * デフォルトでは、BACK, CANCELでダイアログを閉じる。
     * @return  true:イベントを消費した / false:消費しなかった
     */
    open fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isDialog && keyCode==KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            cancel()
            return true
        }
        // フォーカス管理
        if(focusManager?.root?.handleTabEvent(keyCode, event) { rootView.findFocus() } == true) {
            return true
        }

        return false
    }

    // rootViewで、Backキーによるダイアログキャンセルを行い、
    // かつ、ダイアログ表示中に、親Activityがキーイベントを拾ってしまうのを防止できれば、と考えたが、
    // フォーカスがない状態では、onKeyListenerが呼ばれないので、役に立たなかった。残念。
//    protected open fun onBackgroundKeyListener(view:View, keyCode: Int, keyEvent: KeyEvent?): Boolean {
//        if(keyCode==KeyEvent.KEYCODE_BACK) {
//            if(!status.finished) {
//                cancel()
//            }
//        }
//        return true
//    }


    /**
     * 左ボタンがタップされたときのハンドラ
     */
    protected open fun onLeftButtonTapped(view:View) {
        if(leftButtonPositive) {
            onPositive()
        } else {
            onNegative()
        }
    }

    /**
     * 右ボタンがタップされたときのハンドラ
     */
    protected open fun onRightButtonTapped(view:View) {
        if(rightButtonPositive) {
            onPositive()
        } else {
            onNegative()
        }
    }

    /**
     * ネガティブボタンがタップされた。
     */
    protected open fun onNegative() {
        if(confirmToCompleteNegative()) {
            cancel()
        }
    }

    /**
     * ポジティブボタンがタップされた。
     */
    protected open fun onPositive() {
        if(confirmToCompletePositive()) {
            complete(IUtDialog.Status.POSITIVE)
        }
    }

    /**
     * OK/Done でダイアログを閉じてもよいか（必要な情報は揃ったか）？
     */
    protected open fun confirmToCompletePositive():Boolean {
        return true
    }

    /**
     * キャンセルしてもよいか？（普通はオーバーライド不要・・・通信中に閉じないようにするとか。。。)
     */
    protected open fun confirmToCompleteNegative():Boolean {
        return true
    }

    // endregion

}
