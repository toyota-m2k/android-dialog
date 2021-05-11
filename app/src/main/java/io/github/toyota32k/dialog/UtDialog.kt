package io.github.toyota32k.dialog

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.github.toyota32k.R
import io.github.toyota32k.utils.dp2px
import kotlin.math.min

@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class UtDialog : UtDialogBase() {
    /**
     * タイトル： createBodyView()より前（コンストラクタ、または、onCreateあたり）にセットしておく
     * ダイアログ表示後に動的にタイトルを変える場合は、replaceTitle()を呼ぶ。
     */
    var title:String? by bundle.stringNullable

    /**
     * bodyViewをスクロール可能にするかどうか。
     * trueにセットする場合は、sizingOptionを　COMPACT 以外にセットする。AUTO_HEIGHT|FIXED_HEIGHTを推奨
     */
    var scrollable:Boolean by bundle.booleanFalse
    var cancellable:Boolean by bundle.booleanTrue

    @Suppress("unused")
    enum class WidthOption(val param:Int) {
        COMPACT(WRAP_CONTENT),        // WRAP_CONTENT
        FULL(MATCH_PARENT),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT),          // bodyの幅を、widthHint で与えられる値に固定
        LIMIT(WRAP_CONTENT),          // MATCH_PARENT を最大値として、widthHint で指定されたサイズを超えないように調整される
    }
    @Suppress("unused")
    enum class HeightOption(val param:Int) {
        COMPACT(WRAP_CONTENT),        // WRAP_CONTENT
        FULL(MATCH_PARENT),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT),          // bodyの高さを、heightHint で与えられる値に固定
        AUTO_SCROLL(WRAP_CONTENT),    // MATCH_PARENTを最大値として、コンテントが収まる高さに自動調整。収まらない場合はスクロールする。（bodyには MATCH_PARENTを指定)
    }

    var widthOption: WidthOption by bundle.enum(WidthOption.COMPACT)
    var heightOption: HeightOption by bundle.enum(HeightOption.COMPACT)
    var widthHint:Int by bundle.intZero
    var heightHint:Int by bundle.intZero

    @Suppress("unused")
    fun setFixedHeight(height:Int) {
        if(dialog!=null) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        heightOption = HeightOption.FIXED
        heightHint = height
    }

    @Suppress("unused")
    fun setFixedWidth(width:Int) {
        if(dialog!=null) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        widthOption = WidthOption.FIXED
        widthHint = width
    }

    @Suppress("unused")
    fun setLimitWidth(width:Int) {
        if(dialog!=null) {
            throw IllegalStateException("dialog rendering information must be set before preCreateBodyView")
        }
        widthOption = WidthOption.LIMIT
        widthHint = width
    }

    @Suppress("unused")
    enum class GravityOption(val gravity:Int) {
        RIGHT_TOP(Gravity.END or Gravity.TOP),      // 右上（デフォルト）
        CENTER(Gravity.CENTER),         // 画面中央（メッセージボックス的）
        LEFT_TOP(Gravity.START or Gravity.TOP),       // これいるか？
        CUSTOM(Gravity.NO_GRAVITY);         // todo: requestPosition()で指定するとか、なんか方法を考える
    }
    var gravityOption: GravityOption by bundle.enum(GravityOption.RIGHT_TOP)

    /**
     * ダイアログの「画面外」の背景
     */
    enum class GuardColor(@ColorInt val color:Int) {
        INVALID(Color.argb(0,0,0,0)),                       // 透明（無効値）
        TRANSPARENT(Color.argb(0,0xFF,0xFF,0xFF)),          // 透明（通常、 cancellable == true のとき用）
        DIM(Color.argb(0x40,0,0,0)),                        // 黒っぽいやつ　（cancellable == false のとき用）
        @Suppress("unused")
        SEE_THROUGH(Color.argb(0x40,0xFF, 0xFF, 0xFF));     // 白っぽいやつ　（好みで）
    }

    @Suppress("MemberVisibilityCanBePrivate")
    var guardColor:Int by bundle.intNonnull(GuardColor.INVALID.color)
    private val hasGuardColor:Boolean
        get() = guardColor!= GuardColor.INVALID.color

    @ColorInt
    private fun managedGuardColor():Int {
        return when {
            hasGuardColor -> guardColor
            !cancellable-> GuardColor.DIM.color
            else-> GuardColor.TRANSPARENT.color
        }
    }

    @Suppress("unused")
    enum class BuiltInButtonType(@StringRes val id:Int, val positive:Boolean, val blueColor:Boolean) {
        OK(R.string.ok, true, true),
        DONE(R.string.done, true, true),
        CLOSE(R.string.close, true, true),

        CANCEL(R.string.cancel, false, false),
        BACK(R.string.back, false, false),
        CLOSE_LEFT(R.string.close, false, false),
    }

    private var leftButtonText:Int by bundle.intZero
    private var leftButtonPositive:Boolean by bundle.booleanFalse
    private var leftButtonBlue:Boolean by bundle.booleanFalse
    @Suppress("unused")
    val hasLeftButton:Boolean
        get() = leftButtonText > 0

    private var rightButtonText:Int by bundle.intZero
    private var rightButtonPositive:Boolean by bundle.booleanFalse
    private var rightButtonBlue:Boolean by bundle.booleanFalse
    @Suppress("unused")
    val hasRightButton:Boolean
        get() = rightButtonText > 0

    fun setLeftButton(@StringRes id:Int, positive: Boolean=false, blue:Boolean=positive) {
        leftButtonText = id
        leftButtonPositive = positive
        leftButtonBlue = blue
    }

    fun setLeftButton(type: BuiltInButtonType) {
        setLeftButton(type.id, type.positive, type.blueColor)
        if(dialog!=null) {
            updateLeftButton()
        }
    }

    fun setRightButton(@StringRes id:Int, positive: Boolean=false, blue:Boolean=positive) {
        rightButtonText = id
        rightButtonPositive = positive
        rightButtonBlue = blue
        if(dialog!=null) {
            updateRightButton()
        }
    }

    fun setRightButton(type: BuiltInButtonType) {
        setRightButton(type.id, type.positive)
    }

    private fun updateButton(button:Button, @StringRes id:Int, blue:Boolean) {
        activity?.apply {
            button.text = getText(id)
            if(blue) {
                button.background = ContextCompat.getDrawable(this, R.drawable.dlg_button_bg_blue)
                button.setTextColor(getColor(R.color.dlg_blue_button_text))
            } else {
                button.background = ContextCompat.getDrawable(this, R.drawable.dlg_button_bg_white)
                button.setTextColor(getColor(R.color.dlg_white_button_text))
            }
        }
    }

    private fun updateLeftButton() {
        val id = leftButtonText
        if(id!=0) {
            updateButton(leftButton, id, leftButtonBlue)
        } else {
            leftButton.visibility = View.GONE
        }
    }
    private fun updateRightButton() {
        val id = rightButtonText
        if(id!=0) {
            updateButton(rightButton, id, rightButtonBlue)
        } else {
            rightButton.visibility = View.GONE
        }
    }


    val orientation:Int
        get() = resources.configuration.orientation
    val isLandscape:Boolean
        get() = orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPortrait:Boolean
        get() = orientation == Configuration.ORIENTATION_PORTRAIT
    val isPhone:Boolean
        get() = resources.getBoolean(R.bool.under600dp)
    val isTablet:Boolean
        get() = !isPhone

    open fun preCreateBodyView() {
    }

    abstract fun createBodyView(savedInstanceState:Bundle?, inflater: LayoutInflater, rootView:ViewGroup): View

    lateinit var titleView:TextView
    lateinit var leftButton: Button
    lateinit var rightButton: Button

    lateinit var rootView: FrameLayout              // 全画面を覆う透過の背景となるダイアログのルート：
    lateinit var dialogView:ConstraintLayout        // ダイアログ画面としてユーザーに見えるビュー。rootView上で位置、サイズを調整する。
    lateinit var bodyContainer:FrameLayout          // bodyViewの入れ物
    lateinit var bodyView:View                      // UtDialogを継承するサブクラス毎に作成されるダイアログの中身

    fun replaceTitle(title:String) {
        this.title = title
        if(dialog!=null) {
            titleView.text = title
        }
    }

//    private fun setDialogSize(w:Int, h:Int) {
//        rootView.layoutParams = rootView.layoutParams?.apply {
//            this.width = w
//            this.height = h
//        } ?: ViewGroup.LayoutParams(w,h)
//    }
//    private fun setDialogGravity(gravity:Int) {
//        dialogView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT,gravity)
//    }

    private fun setupLayout() {
        dialogView.layoutParams = FrameLayout.LayoutParams(widthOption.param, heightOption.param, gravityOption.gravity)
        setupFixedSize()
        setupDynamicSize()
    }

    private fun setupFixedSize() {
        val fw = if(widthOption== WidthOption.FIXED) widthHint else null
        val fh = if(heightOption== HeightOption.FIXED) heightHint else null
        if(fw==null && fh==null) return

        val lp = bodyContainer.layoutParams ?: return
        if(fw!=null) {
            lp.width = requireContext().dp2px(fw)
        }
        if(fh!=null) {
            lp.height = requireContext().dp2px(fh)
        }
        bodyContainer.layoutParams = lp
    }

    private fun setupDynamicSize() {
        if(widthOption== WidthOption.LIMIT ||heightOption== HeightOption.AUTO_SCROLL) {
            rootView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> onRootViewSizeChanged() }
        }
        if(heightOption== HeightOption.AUTO_SCROLL) {
            bodyView.addOnLayoutChangeListener { _, _,_,_,_, _, _, _, _ ->
                onBodyViewSizeChanged()
            }
        }
    }

    private fun updateBodyContainerHeightOnAutoScroll(lp:ConstraintLayout.LayoutParams) : Boolean {
        if(heightOption== HeightOption.AUTO_SCROLL) {
            val scroller = bodyContainer as ScrollView
            val winHeight = rootView.height
            val scrHeight = scroller.height
            val dlgHeight = dialogView.height
            val bodyHeight = bodyView.height

            val maxScrHeight = winHeight - (dlgHeight - scrHeight)
            val newScrHeight = min(bodyHeight, maxScrHeight)

//            logger.info("window:${winHeight}, scroller:$scrHeight, dialogView:$dlgHeight, bodyHeight:$bodyHeight, maxScrHeight=$maxScrHeight, newScrHeight=$newScrHeight")
            if(lp.height != newScrHeight) {
                lp.height = newScrHeight
                return true
            }
        }
        return false
    }

    private fun updateBodyContainerWidthOnLimitOption(lp:ConstraintLayout.LayoutParams) : Boolean {
        if(widthOption== WidthOption.LIMIT) {
            val winWidth = rootView.width
            val dlgWidth = dialogView.width
            val cntWidth = bodyContainer.width
            val maxCntWidth = winWidth - (dlgWidth-cntWidth)
            val newCntWidth = min(maxCntWidth, requireContext().dp2px(widthHint))
            if(lp.width!=newCntWidth) {
                lp.width = newCntWidth
                return true
            }
        }
        return false
    }

    private fun onRootViewSizeChanged() {
//        logger.info("W=$newWidth, H=$newHeight (${requireContext().px2dp(newWidth)},${requireContext().px2dp(newHeight)})")
//        logger.info("dialogView.height=${dialogView.height} bodyContainer.height=${bodyContainer.height}")
        val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
        val h = updateBodyContainerHeightOnAutoScroll(lp)
        val w = updateBodyContainerWidthOnLimitOption(lp)
        if(h||w) {
            bodyContainer.layoutParams = lp
        }
    }

    private fun onBodyViewSizeChanged() {
//        logger.info("W=$newWidth, H=$newHeight (${requireContext().px2dp(newWidth)},${requireContext().px2dp(newHeight)})")
        if (heightOption == HeightOption.AUTO_SCROLL) {
            val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
            if(updateBodyContainerHeightOnAutoScroll(lp)) {
                bodyContainer.layoutParams = lp
            }
        }
    }

//    private fun autoHeight():Int {
//        return MATCH_PARENT // todo
//    }
//    private fun fixedHeight(): Int {
//        if(fixedBodyHeight>0) {
//            val lp = bodyContainer.layoutParams
//            if(lp!=null) {
//                lp.height = requireContext().dp2px(fixedBodyHeight)
//                bodyContainer.layoutParams = lp
//            } else {
//                bodyContainer.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, requireContext().dp2px(fixedBodyHeight))
//            }
//            return WRAP_CONTENT
//        } else {
//            return MATCH_PARENT
//        }
//    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Dialogの第２引数でスタイルを渡したら、（そのスタイルの指定やダイアログ側のルートコンテナのlayout指定に関わらず）常に全画面のダイアログが開く。
        return Dialog(requireContext(), R.style.dlg_style).also { dlg ->
            preCreateBodyView()
            // ダイアログの背景を透過させる。
            // ダイアログテーマとかdialog_frameのルートコンテナの背景を透過させても効果がないので注意。
            dlg.window?.setBackgroundDrawable(ColorDrawable(managedGuardColor()))
            rootView = View.inflate(requireContext(), R.layout.dialog_frame, null) as FrameLayout
//            rootView = dlg.layoutInflater.inflate(R.layout.dialog_frame, null) as FrameLayout
            leftButton = rootView.findViewById(R.id.left_button)
            rightButton = rootView.findViewById(R.id.right_button)
            titleView = rootView.findViewById(R.id.dialog_title)
            dialogView = rootView.findViewById(R.id.dialog_view)
            title?.let { titleView.text = it }
            if(heightOption== HeightOption.AUTO_SCROLL) {
                scrollable  = true
            }
            bodyContainer = if (scrollable) {
                rootView.findViewById(R.id.body_scroller)
            } else {
                rootView.findViewById(R.id.body_container)
            }
            bodyContainer.visibility = View.VISIBLE
            leftButton.setOnClickListener(this::onLeftButtonTapped)
            rightButton.setOnClickListener(this::onRightButtonTapped)
            if(cancellable) {
                rootView.setOnClickListener(this@UtDialog::onBackgroundTapped)
                dialogView.setOnClickListener(this@UtDialog::onBackgroundTapped)
            }
            updateLeftButton()
            updateRightButton()
            bodyView = createBodyView(savedInstanceState, dlg.layoutInflater, bodyContainer)
            if(bodyContainer==bodyView) {
                bodyView = bodyContainer.getChildAt(0)
            } else if(bodyContainer.childCount==0) {
                bodyContainer.addView(bodyView)
            }
            setupLayout()
            dlg.setContentView(rootView)
        }
    }

    protected open fun onBackgroundTapped(view:View) {
        if(view==rootView && cancellable) {
            onNegative()
        }
    }

    protected open fun onLeftButtonTapped(view:View) {
        if(leftButtonPositive) {
            onPositive()
        } else {
            onNegative()
        }
    }
    protected open fun onRightButtonTapped(view:View) {
        if(rightButtonPositive) {
            onPositive()
        } else {
            onNegative()
        }
    }

    protected open fun onNegative() {
        if(confirmToCompleteNegative()) {
            cancel()
        }
    }

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

}