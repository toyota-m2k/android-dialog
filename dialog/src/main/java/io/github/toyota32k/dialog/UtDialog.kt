package io.github.toyota32k.dialog

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.github.toyota32k.utils.dp2px
import io.github.toyota32k.utils.setLayoutHeight
import io.github.toyota32k.utils.setLayoutWidth
import kotlin.math.max
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
     * trueにセットする場合は、sizingOptionを　COMPACT 以外にセットする。AUTO_SCROLLを推奨
     */
    var scrollable:Boolean by bundle.booleanFalse
    var cancellable:Boolean by bundle.booleanTrue

    @Suppress("unused")
    enum class WidthOption(val param:Int) {
        COMPACT(WRAP_CONTENT),        // WRAP_CONTENT
        FULL(MATCH_PARENT),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT),          // bodyの幅を、widthHint で与えられる値に固定
        LIMIT(WRAP_CONTENT),          // フルスクリーンを最大値として、widthHint で指定されたサイズを超えないように調整される
    }
    @Suppress("unused")
    enum class HeightOption(val param:Int) {
        COMPACT(WRAP_CONTENT),        // WRAP_CONTENT
        FULL(MATCH_PARENT),           // フルスクリーンに対して、MATCH_PARENT
        FIXED(WRAP_CONTENT),          // bodyの高さを、heightHint で与えられる値に固定
        AUTO_SCROLL(WRAP_CONTENT),    // MATCH_PARENTを最大値として、コンテントが収まる高さに自動調整。収まらない場合はスクロールする。（bodyには MATCH_PARENTを指定)
        CUSTOM(WRAP_CONTENT),         // AUTO_SCROLL 的な配置をサブクラスで実装する。その場合、calcCustomContainerHeight() をオーバーライドすること。
    }

    var widthOption: WidthOption by bundle.enum(WidthOption.COMPACT)
    var heightOption: HeightOption by bundle.enum(HeightOption.COMPACT)
    var widthHint:Int by bundle.intZero
    var heightHint:Int by bundle.intZero

    /**
     * 子ダイアログを開くときに親ダイアログを隠すかどうか
     */
    enum class ParentVisibilityOption {
        NONE,                   // 何もしない：表示しっぱなし
        HIDE_AND_SHOW,          // このダイアログを開くときに非表示にして、閉じるときに表示する
        HIDE_AND_SHOW_ON_NEGATIVE,  // onNegativeで閉じるときには、親を表示する。Positiveのときは非表示のまま
        HIDE_AND_SHOW_ON_POSITIVE,  // onPositiveで閉じるときには、親を表示する。Negativeのときは非表示のまま
        HIDE_AND_LEAVE_IT       // このダイアログを開くときに非表示にして、あとは知らん
    }
    var parentVisibilityOption by bundle.enum(ParentVisibilityOption.HIDE_AND_SHOW)
    var visible:Boolean
        get() = dialogView.visibility == View.VISIBLE
        set(v) { dialogView.visibility = if(v) View.VISIBLE else View.INVISIBLE }



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
//        CUSTOM(Gravity.NO_GRAVITY);         // requestPosition()で指定するとか、なんか方法を考える
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

//    fun parentDialog() : UtDialog? {
//        return UtDialogHelper.dialogChainToParent(this).filter { it!=this@UtDialog && it is UtDialog }.firstOrNull() as UtDialog?
//    }

    @ColorInt
    private fun managedGuardColor():Int {
        return when {
            hasGuardColor -> guardColor
            !cancellable-> GuardColor.DIM.color
            else-> GuardColor.TRANSPARENT.color
        }
    }

    @Suppress("unused")
    enum class BuiltInButtonType(val string:UtStandardString, val positive:Boolean, val blueColor:Boolean) {
        OK(UtStandardString.OK, true, true),
        DONE(UtStandardString.DONE, true, true),
        CLOSE(UtStandardString.CLOSE, true, true),

        CANCEL(UtStandardString.CANCEL, false, false),
        BACK(UtStandardString.BACK, false, false),
        CLOSE_LEFT(UtStandardString.CLOSE, false, false),
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
        setLeftButton(type.string.id, type.positive, type.blueColor)
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
        setRightButton(type.string.id, type.positive)
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

    interface IViewInflater {
        val layoutInflater: LayoutInflater
        val bodyContainer:ViewGroup
        fun inflate(@LayoutRes id:Int):View
    }
    protected abstract fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View

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
        if(heightOption== HeightOption.FULL) {
            bodyContainer.setLayoutHeight(0)
        }
        if(widthOption== WidthOption.FULL) {
            bodyContainer.setLayoutWidth(0)
        }
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
        if(widthOption== WidthOption.LIMIT || heightOption== HeightOption.AUTO_SCROLL || heightOption== HeightOption.CUSTOM) {
            rootView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                if (or - ol != r - l || ob - ot != b - t) {
                    onRootViewSizeChanged()
                }
            }
        }
        if(heightOption== HeightOption.AUTO_SCROLL) {
            bodyView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                if (or - ol != r - l || ob - ot != b - t) {
                    onBodyViewSizeChanged()
                }
            }
        }
    }

    /**
     * heightOption = CUSTOM に設定したときは、このメソッドをオーバーライドすること。
     * @param currentBodyHeight     現在のBodyビュー（createBodyViewが返したビュー）の高さ
     * @param currentContainerHeight    現在のコンテナ（Bodyの親）の高さ。マージンとか弄ってなければ currentBodyHeightと一致するはず。
     * @param maxContainerHeight        コンテナの高さの最大値（このサイズを超えないよう、Bodyの高さを更新すること）
     * @return コンテナの高さ（bodyではなく、containerの高さを返すこと）
     */
    protected open fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int {
        error("must be implemented in subclass on setting 'heightOption==CUSTOM'")
    }

    /**
     * heightOption = CUSTOM でbodyビュー (createBodyViewが返したビュー）の高さが変化したときに、このメソッドを呼んでダイアログサイズを更新する。
     */
    protected fun updateCustomHeight() {
        onRootViewSizeChanged()
    }

    private fun updateDynamicHeight(lp:ConstraintLayout.LayoutParams) : Boolean {
        if(heightOption== HeightOption.AUTO_SCROLL ||heightOption== HeightOption.CUSTOM) {
            val winHeight = screenSize.height
            if(winHeight==0) return false
            val containerHeight = bodyContainer.height
            val dlgHeight = dialogView.height
            val bodyHeight = bodyView.height
            val maxContainerHeight = winHeight - (dlgHeight - containerHeight)

            val newContainerHeight = if(heightOption== HeightOption.AUTO_SCROLL) {
                min(bodyHeight, maxContainerHeight)
            } else {
                calcCustomContainerHeight(bodyHeight,containerHeight,maxContainerHeight)
            }

//            logger.info("window:${winHeight}, scroller:$scrHeight, dialogView:$dlgHeight, bodyHeight:$bodyHeight, maxScrHeight=$maxScrHeight, newScrHeight=$newScrHeight")
            if(lp.height != newContainerHeight) {
                lp.height = newContainerHeight
                return true
            }
        }
        return false
    }

    private fun updateDynamicWidth(lp:ConstraintLayout.LayoutParams) : Boolean {
        if(widthOption== WidthOption.LIMIT) {
            val winWidth = screenSize.width
            if(winWidth==0) return false
            val dlgWidth = dialogView.width
            val cntWidth = dlgWidth - lp.marginStart - lp.marginEnd // bodyContainer.width
            val maxCntWidth = winWidth - (dlgWidth-cntWidth)
            val newCntWidth = min(maxCntWidth, requireContext().dp2px(widthHint))
            if(lp.width!=newCntWidth) {
                lp.width = newCntWidth
                return true
            }
        }
        return false
    }

    data class MutableSize(var width:Int, var height:Int) {
        val longer:Int
            get() = max(width,height)
        val shorter:Int
            get() = min(width, height)
    }
    val screenSize = MutableSize(0,0)

    private fun onRootViewSizeChanged() {
//        logger.info("W=$newWidth, H=$newHeight (${requireContext().px2dp(newWidth)},${requireContext().px2dp(newHeight)})")
//        logger.info("dialogView.height=${dialogView.height} bodyContainer.height=${bodyContainer.height}")
        screenSize.width = rootView.width
        screenSize.height = rootView.height
        val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
        val h = updateDynamicHeight(lp)
        val w = updateDynamicWidth(lp)
        if(h||w) {
            bodyContainer.layoutParams = lp
        }
    }

    private fun onBodyViewSizeChanged() {
//        logger.info("W=$newWidth, H=$newHeight (${requireContext().px2dp(newWidth)},${requireContext().px2dp(newHeight)})")
        if (heightOption == HeightOption.AUTO_SCROLL) {
            val lp = bodyContainer.layoutParams as ConstraintLayout.LayoutParams
            if(updateDynamicHeight(lp)) {
                // bodyView のOnLayoutChangeListenerの中から、コンテナのサイズを変更しても、なんか１回ずつ無視されるので、ちょっと遅延する。
                Handler(Looper.getMainLooper()).post {
                    bodyContainer.layoutParams = lp
                }
            }
        }
    }

//    private fun autoHeight():Int {
//        return MATCH_PARENT
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

    data class ViewInflater(val dlg:Dialog, override val bodyContainer:ViewGroup): IViewInflater {
        override val layoutInflater get() = dlg.layoutInflater

        override fun inflate(id: Int): View {
            return dlg.layoutInflater.inflate(id, bodyContainer, false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Dialogの第２引数でスタイルを渡したら、（そのスタイルの指定やダイアログ側のルートコンテナのlayout指定に関わらず）常に全画面のダイアログが開く。
        return Dialog(requireContext(), R.style.dlg_style).also { dlg ->
            try {
                preCreateBodyView()
                // ダイアログの背景を透過させる。
                // ダイアログテーマとかdialog_frameのルートコンテナの背景を透過させても効果がないので注意。
                dlg.window?.setBackgroundDrawable(ColorDrawable(GuardColor.TRANSPARENT.color))
                rootView = View.inflate(requireContext(), R.layout.dialog_frame, null) as FrameLayout
//            rootView = dlg.layoutInflater.inflate(R.layout.dialog_frame, null) as FrameLayout
                leftButton = rootView.findViewById(R.id.left_button)
                rightButton = rootView.findViewById(R.id.right_button)
                titleView = rootView.findViewById(R.id.dialog_title)
                dialogView = rootView.findViewById(R.id.dialog_view)
                title?.let { titleView.text = it }
                if (heightOption == HeightOption.AUTO_SCROLL) {
                    scrollable = true
                } else if (heightOption == HeightOption.CUSTOM) {
                    scrollable = false
                }
                bodyContainer = if (scrollable) {
                    rootView.findViewById(R.id.body_scroller)
                } else {
                    rootView.findViewById(R.id.body_container)
                }
                bodyContainer.visibility = View.VISIBLE
                leftButton.setOnClickListener(this::onLeftButtonTapped)
                rightButton.setOnClickListener(this::onRightButtonTapped)
                if (cancellable) {
                    rootView.setOnClickListener(this@UtDialog::onBackgroundTapped)
                    dialogView.setOnClickListener(this@UtDialog::onBackgroundTapped)
                }
                updateLeftButton()
                updateRightButton()
                bodyView = createBodyView(savedInstanceState, ViewInflater(dlg, bodyContainer))
                bodyContainer.addView(bodyView)
                setupLayout()
                dlg.setContentView(rootView)
            } catch(e:Throwable) {
                // View作り中のエラーは、デフォルトでログに出る間もなく死んでしまうようなので、キャッチして出力する。throwし直すから死ぬけど。
                logger.stackTrace(e)
                throw e
            }
        }
    }

    val rootDialog : UtDialog
        get() {
            var dlg:UtDialog = this
            var fragment: Fragment = dlg
            while(true) {
                fragment = fragment.parentFragment ?: break
                if(fragment is UtDialog) {
                    dlg = fragment
                }
            }
            return dlg
        }

    val parentDialog : UtDialog?
        get() {
            var fragment: Fragment? = this.parentFragment
            while(fragment!=null) {
                if(fragment is UtDialog) {
                    return fragment
                }
                fragment = fragment.parentFragment
            }
            return null
        }

    protected fun applyGuardColor() {
        val color = managedGuardColor()
        if(Color.alpha(color)!=0) {
            rootDialog.dialog?.window?.setBackgroundDrawable(ColorDrawable(color))
        } else {
            val parent = parentDialog
            if(parent!=null) {
                parent.applyGuardColor()
            } else {
                dialog?.window?.setBackgroundDrawable(ColorDrawable(color))
            }
        }
    }

    override fun onDialogOpening() {
        applyGuardColor()
        val parent = parentDialog ?: return
        if (parentVisibilityOption != ParentVisibilityOption.NONE) {
            parent.visible = false
        }
    }

    override fun onDialogClosing() {
        val parent = parentDialog ?: return
        if(  parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW ||
            (parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW_ON_NEGATIVE && status.negative) ||
            (parentVisibilityOption==ParentVisibilityOption.HIDE_AND_SHOW_ON_POSITIVE && status.positive)) {
            parent.visible = true
        }
        parent.applyGuardColor()
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