# ダイアログのオプション

var scrollable:Boolean 
    デフォルト: false
var positiveCancellable:Boolean
    デフォルト: false
    画面外をタップしてダイアログを閉じるとき、Positive()扱いにするか？
var draggable:Boolean
    デフォルト: false
var clipVerticalOnDrag:Boolean
    デフォルト: false
var clipHorizontalOnDrag:Boolean
    デフォルト: false
var animationEffect:Boolean
    デフォルト: true
var noHeader:Boolean
    デフォルト: false
    標準のタイトルバー（旧UIならok/cancelボタンを含む）を表示しない。
var noFooter:Boolean
    デフォルト：false
    ボタンバー（ok/cancelボタンを表示するエリア）を表示しない。
var noInvisibleHeaderButton:Boolean
    デフォルト：false
    ヘッダーボタン(ok/cancelなど) を非表示(BuiltInButtonType.NONE)にしたとき、そのボタンの領域をなくすか、見えないがそこにあるものとしてレンダリングするか？　つまり、Goneにする(false:デフォルト)か、Invisibleにする(true)か。
var bodyContainerMargin
     ボディ部分だけのメッセージ的ダイアログを作る場合に、noHeader=true と組み合わせて使うことを想定
     上下左右を個別にカスタマイズするときは、onViewCreated()で、bodyContainerのマージンを直接操作する。
    -1: デフォルト（8dp)
    >=0: カスタムマージン(dp)
var noDialogMargin:Boolean by bundle.booleanFalse
     UtDialogConfig.dialogMarginOnPortrait / dialogMarginOnLandscape によるマージン設定を無効化する場合は true をセットする。
var hideStatusBarOnDialogMode:Boolean by bundle.booleanFalse
var widthOption: WidthOption
    COMPACT
var heightOption: HeightOption
    COMPACT
var gravityOption: GravityOption
    GravityOption.CENTER
var customPositionX: Float? by bundle.floatNullable
var customPositionY: Float? by bundle.floatNullable
var guardColor:Int by bundle.intNonnull(GuardColor.INVALID.color)
var bodyGuardColor:Int by bundle.intNonnull(defaultBodyGuardColor)
var parentVisibilityOption by bundle.enum(ParentVisibilityOption.HIDE_AND_SHOW)
var visible:Boolean


val rootDialog : UtDialog?
val parentDialog : UtDialog?

var title:String?
var leftButtonType:ButtonType
var rightButtonType:ButtonType

val orientation
val isLandscape
val isPortrait
val isPhone
val isTablet

lateinit var titleView:TextView
    private set
lateinit var leftButton: Button
    private set
lateinit var rightButton: Button
    private set
lateinit var progressRingOnTitleBar: ProgressBar
    private set

lateinit var rootView: ViewGroup              // 全画面を覆う透過の背景となるダイアログのルート：
    protected set
lateinit var dialogView:ViewGroup        // ダイアログ画面としてユーザーに見えるビュー。rootView上で位置、サイズを調整する。
    protected set
lateinit var bodyContainer:ViewGroup          // bodyViewの入れ物
    private set
lateinit var bodyView:View                      /* UtDialogを継承するサブクラス毎に作成されるダイアログの中身  (createBodyView()で構築されたビュー） */ private set
lateinit var refContainerView:View              // コンテナ領域（ダイアログ領域ーヘッダー領域）にフィットするダミービュー
    private set
lateinit var bodyGuardView:FrameLayout           // dialogContentへの操作をブロックするためのガードビュー
    private set
lateinit var centerProgressRing:ProgressBar     // 中央に表示するプログレスリング：デフォルトでは非表示。bodyGuardView とともに visible にすることで表示される。
    private set




## UtDialogConfig


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
