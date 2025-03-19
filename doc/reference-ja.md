# UtDialog Reference Manual

### ダイアログの動作を指定するプロパティ

### val status : IUtDialog.Status

ダイアログの結果（どのように閉じられたか）を保持するプロパティです。
- UNKNOWN<br>無効値（ダイアログはまだ実行されていない）
- POSITIVE<br>positiveボタン(OKなど) の押下によってダイアログが閉じた。
- NEGATIVE<br>negativeボタン(Cancelなど)の押下よってダイアログが閉じた。
- NEUTRAL<br>neutralボタン押下にｙほってダイアログが閉じた（３ボタンメッセージボックスの場合のみ）。

通常は、IUtImmortalTask.showDialog() の戻り値(IUtDialog)の status プロパティをチェックします。

### var isDialog : Boolean

デフォルト：false<br>
UtDialogConfig.showInDialogModeAsDefault を設定することにより、デフォルト値は変更できます。

false（デフォルト）の場合は、フラグメントモードとして動作し、FragmentManager のトランザクションにより、ActivityのWindow上にダイアログが表示されます。

true にすると、ダイアログモードで動作し、DialogFragment#show() によって表示されます。この場合は、（ActivityのWindowではなく）新しいWindowが作成され、その上にダイアログが表示されます。Activityの上に、独立したwindow を重ねる構成となるため、動作が安定していましたが、edge-to-edge が標準になると、Activityの状態（NoActionBar + statusBar非表示の場合など）と整合をとるのが難しくなってきたので、v4以降、デフォルトは false にしています。

### var edgeToEdgeEnabled : Boolean

デフォルト: true<br>
UtDialogConfig.edgeToEdgeEnabled を設定することにより、デフォルト値は変更できます。

Activityで、edge-to-edge を有効にしない場合は、false にします。true にすると、
isDialog=false (フラグメントモード) の場合に、setOnApplyWindowInsetsListenerを呼び出して、insets の調整を行います。

### var cancellable:Boolean

デフォルト: true<br>

ダイアログ（またはメッセージボックス）の画面外をタップしたときキャンセルしてダイアログを閉じるかどうかを指定します。画面外をタップしてもダイアログを閉じないようにする場合は、false にします。
任意のタイミングで変更できます。

UtDialogEx を使用する場合は、`Binder.dialogCancellable()` 拡張関数によってビューモデルにバインドできます。

### var scrollable:Boolean 

デフォルト: false<br>

コンテナビューでスクロールを有効にするかどうかを指定します。ただし、heightOption=COMPACT の場合は、scrollable=trueを設定しても効果はありません。また、heightOption=AUTO_SCROLL の場合は常に scrollable=true として動作します。

### var positiveCancellable:Boolean

デフォルト: false<br>

デフォルトでは、ダイアログの画面外をタップしたときは、キャンセル扱い（negative()を呼び出してダイアログを閉じる）ですが、positiveCancellable を trueにすると、画面外をタップしたとき、positive() を呼び出してダイアログを閉じます。

### var draggable:Boolean
デフォルト: false<br>

true にすると、ダイアログのタイトルバーをドラッグして、ダイアログを移動することができるようになります。

### var clipVerticalOnDrag:Boolean
デフォルト: false<br>

true にすると、ダイアログをドラッグするとき、縦方向への移動を禁止し、横方向にのみ平行移動するようになります。

var clipHorizontalOnDrag:Boolean
デフォルト: false<br>

true にすると、ダイアログをドラッグするとき、横方向への移動を禁止し、縦方向にのみ平行移動するようになります。

### var animationEffect:Boolean
デフォルト: true<br>

false にすると、ダイアログを表示するときのフェードイン/アウトアニメーションを無効化します。

### var noHeader:Boolean
デフォルト: false<br>

true にすると、標準のタイトルバー（legacy ui の場合は ok/cancel ボタンを含む）を表示しません。

### var noFooter:Boolean
デフォルト：false<br>

true にすると、ボタンバー（ok/cancelボタンを表示するエリア）を表示しません。legacy ui に対しては効果はありません。

### var invisibleBuiltInButton:Boolean
デフォルト：true<br>

ダイアログボタン(leftButton/rightButton) を非表示(BuiltInButton.NONE)にしたとき、そのボタンを View.INVISIBLE にするか、View.GONE にするかを指定します。デフォルト (true) では、View.INVISIBLE となります。

### var bodyContainerMargin
デフォルト: -1<br>

bodyContainer の上下左右のマージンをDP単位で指定します。-1 を指定すると、デフォルト値（dialog-flame.xml で定義された8dp）が使用されます。上下左右を個別にカスタマイズするときは、onViewCreated()で、bodyContainerのマージンを直接設定してください。

### var noDialogMargin:Boolean
デフォルト: false<br>

デバイス画面に対するダイアログのマージンは、UtDialogConfig.dialogMarginOnPortrait（横置きの場合）および、UtDialogConfig.dialogMarginOnLandscape（縦置きの場合）で設定します。noDialogMargin = true にすると、このマージン設定を無効化して、デバイス画面全体にダイアログを表示します。

## var hideStatusBarOnDialogMode:Boolean
デフォルト: false<br>

ダイアログを表示するとき、StatusBar を非表示するかどうかを指定します。
このプロパティは、ダイアログモード（isDialog = true）の場合にのみ有効です。hideStatusBarOnDialogMode = true にすると、以下の設定が行われます。
- StatusBar を非表示
- FLAG_LAYOUT_NO_LIMITS を設定して、rootView を全画面に表示
- LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES をセット

NoActionBar系のスタイルを適用し、（プログラム的に）StatusBarを非表示にしたとき、
ダイアログモードの場合、Activityとは独立した window が作成されて、StatusBar は表示された状態となり、切り欠き部分を避けた領域に rootViewが配置されてしまいます。背景が透明（GuardColor.TRANSPARENT）なダイアログではあまり気になりませんが、背景を隠すダイアログでは、StatusBar だけが露出したような表示になってしまします。hideStatusBarOnDialogMode = true は、この現象を回避するために使用します。

### var widthOption: WidthOption
デフォルト: WidthOption.COMPACT<br>

ダイアログの幅を設定します。詳細は [WidthOption/HeightOption の使い方](sizing-option-ja.md)をご参照ください。

### var heightOption: HeightOption
デフォルト: HeightOption.COMPACT<br>

ダイアログの高さを設定します。詳細は [WidthOption/HeightOption の使い方](sizing-option-ja.md)をご参照ください。

### var gravityOption: GravityOption
デフォルト：GravityOption.CENTER<br>

ダイアログを配置する位置を指定します。次の４つの値が設定できます。

- GravitiyOption.CENTER<br>画面中央に配置（デフォルト）
- GravityOption.RIGHT_TOP<br>画面右上に配置
- GravitiyOption.LEFT_TOP<br>画面左上に配置
- GravitiyOption.CUSTOM<br>customPositionX, customPositionY プロパティで位置を指定

### var customPositionX: Float?
デフォルト: null <br>

GravityOption.CUSTOM と組み合わせて使用します。
また、draggable = true の場合、現在のダイアログの表示位置を取得/設定することができます。

### var customPositionY: Float?
デフォルト: null <br>

GravityOption.CUSTOM と組み合わせて使用します。
また、draggable = true の場合、現在のダイアログの表示位置を取得/設定することができます。

### var guardColor :GuardColor
デフォルト： GuardColor.INVALID<br>

ダイアログの外側の色を指定します。
次の値が使用できます。

- GuardColor.TRANSPARENT<br>透明
- DIM<br>黒っぽい透過色
- GuardColor.SEE_THROUGH<br>白っぽい透過色
- GuardColor.SOLID_GRAY<br>不透過の灰色
- GuardColor.THEME_DIM<br>文字色をベースした透過色（黒っぽい/白っぽいがテーマによって動的に変る）
- THEME_SEE_THROUGH<br>背景色をベースした透過色（黒っぽい/白っぽいがテーマによって動的に変る）
- CUSTOM(color:Int)<br>任意の色を指定

無指定（デフォルト）の場合は、`cancellable=true` なら、`UtDialogConfig.defaultGuardColorOfCancellableDialog`（デフォルト：UtDialog.GuardColor.TRANSPARENT）,それ以外の場合は、`UtDialogConfig.defaultGuardColor`（UtDialog.GuardColor.THEME_SEE_THROUGH）が使われます。ただし、isPhone == true 且つ、UtDialogConfig.solidBackgroundOnPhone == true の場合は、この設定を無視して、常に、`GuardColor.SOLID_GRAY` が使われます。

### var bodyGuardColor :GuardColor
デフォルト: UtDialogOption.defaultBodyGuardColor (UtDialog.GuardColor.THEME_SEE_THROUGH)

bodyGuardView の背景色を指定します。bodyGuardViewは、
ビジーの場合などに、ダイアログに対するタッチ操作をブロックするためのビューです。
設定可能な値は、`guardColor` の説明を参照願います。

### var title:String?
デフォルト: null

タイトルバーに表示する文字列です。
任意のタイミングで設定できます。

UtDialogEx を使う場合は、`Binder.dialogTitle()` 拡張関数により、ViewModelにバインドできます。

### var leftButtonType:ButtonType
デフォルト：ButtonType.NONE

左側のビルトインボタンのタイプを指定します。以下の値が指定可能です。

- NONE（デフォルト）<br>ボタンは表示しません。非表示になったボタンの配置方法は、invisibleBuiltInButton に従います。
- OK<br>OKボタン（positive）を表示します。
- DONE<br>DONEボタン（positive）を表示します。
- CLOSE<br>CLOSEボタン（positive）を表示します。
- CANCEL<br>CANCELボタン（negative）を表示します。
- BACK<br>BACKボタン（negative）を表示します。
- NEGATIVE_CLOSE<br>CLOSEボタン（negative）を表示します。
- POSITIVE_BACK<br>BACKボタン（positive）を表示します。
- CUSTOM(string:String, positive:Boolean)<br>任意の文字列を指定してボタンを表示します。

### var rightButtonType:ButtonType
デフォルト：ButtonType.NONE<br>

右側のビルトインボタンのタイプを指定します。指定可能な値は、leftButtonType の説明を参照願います。


## 状態取得用のプロパティ

### val orientation:Int

resources.configuration.orientation の値（@Orientation）を返します。

### val isLandscape :Boolean

デバイスが縦置きの場合は true それ以外の場合は false を返します。

### val isPortrait :Boolean

デバイスが横置きの場合は true それ以外の場合は false を返します。

### val isPhone :Boolean

デバイスが Phone の場合は true それ以外の場合は false を返します。
デバイス画面の短辺が 600dp 未満なら phone、600dp以上なら tablet と判断しています。

### val isTablet

!isPhone を返します。

## ダイアログチェーンを参照するためのプロパティ

### val rootDialog : UtDialog?

ルートダイアログ（ダイアログチェーンの先頭）を取得します。

### val parentDialog : UtDialog?

親ダイアログを取得します。

## ビルトインビューを取得するためのプロパティ

### val titleView:TextView

タイトルを表示する TextView です。

### val leftButton: Button

左側のビルトインボタンです。
leftButtonType によって表示内容を設定します。

UtDialogEx を使用する場合には、`Binder.dialogLeftButtonVisibility()`, `Binder.dialogLeftButtonEnable()`, `Binder.dialogLeftButtonString()`, `Binder.dialogLeftButtonCommand` 拡張関数によって、表示/非表示, 有効/無効, ボタンキャプション, ボタン押下時のコマンドをビューモデルにバインドできます。

### val rightButton: Button

右側のビルトインボタンです。
rightButtonType によって表示内容を設定します。

UtDialogEx を使用する場合には、`Binder.dialogRightButtonVisibility()`, `Binder.dialogRightButtonEnable()`, `Binder.dialogRightButtonString()`, `Binder.dialogRightButtonCommand` 拡張関数によって、表示/非表示, 有効/無効, ボタンキャプション, ボタン押下時のコマンドをビューモデルにバインドできます。

### val progressRingOnTitleBar: ProgressBar

タイトルバー上に表示する Progress Ring です。
デフォルトでは非表示(INVISIBLE) ですが、例えば、ダイアログのコンテントをサーバーからダウンロードする場合など、初期化に時間がかかる場合には、progressRingOnTitleBarを VISIBLE にし、初期化が終わったら GONE に戻します。

UtDialogEx を利用する場合は、`Binder.dialogProgressRingOnTitleTitleBarVisibility()` 拡張関数で、Progress Ring の表示・非表示をビューモデルにバインドできます。


### val rootView: ViewGroup

ダイアログの背景となるデバイス画面全体を覆うビューです。`guardColor` によって指定された背景色で描画されます。

### val dialogView:ViewGroup

ダイアログ画面としてユーザーに見える最上位のビューです。rootView上に表示され、widthOption, heightOption, gravityOption, customPositionX, customPositionY などによって、サイズや位置が調整されます。

### val bodyContainer:ViewGroup

bodyView のコンテナです。scrollable == true の場合は ScrollView、それ以外の場合は、FrameLayout になります。

### val bodyView:View

UtDialogのサブクラスでオーバーライドされる createBodyView() によって作成されたビューです。

### val refContainerView:View

コンテナ領域（ダイアログ領域から、ヘッダー/フッターの領域、マージンを除いた領域）を取得するためのの invisible なビューです。HeihtOption.AUTO_SCROLL や、HeightOption.CUSTOM でサイズ計算を行うためにUtDialog内部で使われます。通常、サブクラスなどから直接利用することはありません。

### val bodyGuardView:FrameLayout

bodyGuardViewは、ダイアログに対するタッチ操作をブロックするためのビューです。
デフォルトでは非表示(GONE)ですが、例えば、OKボタンを押したあと、処理が完了するまで待機するような場合に、VISIBLE にします。bodyGuardViewの背景色は `bodyGuardColor` によってカスタマイズできます。

UtDialogEx を利用する場合は、`Binder.dialogGuardViewVisibility()` 拡張関数で、bodyGuardView の表示・非表示をビューモデルにバインドできます。

### val centerProgressRing:ProgressBar     

bodyGuardView の中央に表示するプログレスリング、デフォルトでは非表示です。bodyGuardView とともに VISIBLE にすることで表示されます。

UtDialogEx を利用する場合は、`Binder.dialogGuardViewVisibility()` 拡張関数で、bodyGuardView を表示したときに、centerProgressRingも表示するかどうかも指定できます。

## UtDialogサブクラスから利用可能なメソッド


### fun show(activity: FragmentActivity, tag:String?)

ダイアログを表示します。
通常は、`IUtImmortalTask.showDialog()`関数を使って、UtImmortalTask内からダイアログを表示します。直接、show()関数を呼び出すことはありませｓん。

### fun complete(status: Status)

ダイアログを指定されたステータスで閉じます。
通常、UtDialog 派生クラスでは、onPositive() / onNegative() メソッドを使ってダイアログを閉じます。

### fun cancel()

ダイアログをStatus.NEGATIVEステータスで閉じます。
`complete(Status.NEGATIVE)` と同義です。

### fun forceDismiss()

強制的にダイアログを閉じます。
通常は使いません。アクティビティを終了するときに開いているダイアログをすべて閉じる、UtDialogHelper.forceCloseAllDialogs() から例外的に呼び出されます。

## UtDialogサブクラスでオーバーライドが必要なメソッド


### fun preCreateBodyView()

UtDialogは、一部のプロパティ(title, cancellable)を除いて、大部分のプロパティは、createBodyView() が呼ばれる前に設定しておく必要があります。preCreateBodyView()は、これらのプロパティを設定する最適なタイミングです。

### fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View

ダイアログのbodyView を作成するためにオーバーライドします。layout.xml からビューを構築する場合は、ダイアログテーマを正しく反映するため、必ず、引数で渡される inflater を使用してください。

### fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int

heightOption に、CUSTOM を指定した場合は、必ず、このメソッドをオーバーライドしてください。
引数として、以下の値が渡されます。
- currentBodyHeight<br>現在のbodyView（createBodyViewが返したビュー）の高さです。
- currentContainerHeight<br>現在のcontainerView（bodyViewの親）の高さです。通常は currentBodyHeightと一致します。
- maxContainerHeight<br>コンテナの高さの最大値です。このサイズを超えないよう、bodyViewの高さを調整してください。

戻り値として、bodyViewの高さを調整したあとの、containerViewの高さを返してください。

## UtDialogサブクラスでオーバーライド可能なメソッド

### fun confirmToCompletePositive():Boolean

このメソッドをオーバーライドして、false を返すと、positiveボタン押下時にダイアログを閉じません。
ダイアログで必要な設定が揃っていない場合に、OKでダイアログを閉じないようにする場合に利用できます。

### fun confirmToCompleteNegative():Boolean

このメソッドをオーバーライドして、false を返すと、negativeボタン押下時にダイアログを閉じません。
何かの処理が終わるまでダイアログを閉じないようにする場合などに利用できます。


## グローバルオプション（UtDialogConfig）

### var showInDialogModeAsDefault = false

`UtDialog#isDialog` のデフォルト値を設定します。

### var edgeToEdgeEnabled = true

`UtDialog#edgeToEdgeEnabled` のデフォルト値を設定します。

### var showDialogImmediately:ShowDialogMode

フラグメントモード(isDialog=false) でダイアログを表示する方法を指定します。
- ShowDialogMode.Immediately（デフォルト）<br>
FragmentTransaction#commit()を呼んだあとすぐに、FragmentManager#executePendingTransactions()を実行します。
- ShowDialogMode.Commit<br>FragmentTransaction#commit()を呼びます。
- ShowDialogMode.CommitNow<br>FragmentTransaction#commitNow()を呼びます。

###    var solidBackgroundOnPhone:Boolean = false

isPhone==true の場合に、背景を灰色(SOLID_GRAY)で塗りつぶす場合は true を指定します。

デザインにもよりますが、小さい画面では、本体の画面の上にダイアログの画面が重なると、ごちゃごちゃして見づらくな
ることがありました。さらに、ダイアログからサブダイアログに遷移するときに、一瞬、本体の画面が透けて見えるのが気持ち悪い、という意見もあって用意したのが、この「Phoneの場合は背景を見せない」という設定です。


### var defaultGuardColor:UtDialog.GuardColor = UtDialog.GuardColor.THEME_DIM

cancellable == false の場合の、`UtDialog#guardColor` のデフォルト値です。

### var defaultGuardColorOfCancellableDialog:Int = UtDialog.GuardColor.TRANSPARENT

cancellable == true の場合の、`UtDialog#guardColor` のデフォルト値です。

### var defaultBodyGuardColor:Int = UtDialog.GuardColor.THEME_SEE_THROUGH

`UtDialog#bodyGuardColor` のデフォルト値です。

### var dialogTheme: Int = R.style UtDialogTheme

ダイアログのスタイルを指定します。
デフォルト（R.style.UtDialogTheme）は、Material3 のテーマカラーを使った配色になっています。

### var dialogFrameId: Int = R.layout.dialog_frame

ダイアログフレーム（UtDialogの土台となるビュー）のレイアウトをリソースIDで指定します。
デフォルト (R.layout.dialog_frame) は、Material3 ベースのデザインです。Material2 (Theme.MaterialComponents) を使用する場合は、`useLegacyTheme()` メソッドを呼ぶことで、`R.layout.dialog_frame_legacy` が設定されます。

### var fadeInDuration:Long = 300L

フェードインアニメーションの遷移時間をミリ秒単位で指定します。

### var fadeOutDuraton:Long = 400L

フェードアウトアニメーションの遷移時間をミリ秒単位で指定します。

### var dialogMarginOnPortrait: Rect? = Rect(20, 40, 20, 40)

デバイス横置きにしたときの、rootView に対する dialogView のマージンを指定します。
Width/HeightOption FULL/LIMIT/AUTO_SCROLL/CUSTOM を指定したときの最大サイズ決定に使用されます。null を設定するとマージンはゼロになります。UtDialog#noDialogMargin = true にすることによって、ダイアログ毎にマージンをゼロにすることもできます。

### var dialogMarginOnLandscape: Rect? = Rect(40, 20, 40, 20)

デバイス横置きにしたときの、rootView に対する dialogView のマージンを指定します。
仕様は dialogMarginOnPortrait に準じます。
