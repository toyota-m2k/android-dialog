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