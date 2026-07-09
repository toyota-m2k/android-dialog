# UtDialog リファレンス

<div align="right">
<a href="./reference.md">EN</a> | JA
</div>

UtDialog 派生クラスで利用できるプロパティ・メソッドと、グローバル設定 (UtDialogConfig) のリファレンスです。
基本的な使い方は [チュートリアル（基本編）](./tutorial-basic-ja.md) をご参照ください。

特に断りのない限り、各プロパティは `preCreateBodyView()` で設定します。

## ダイアログの結果

### val status : IUtDialog.Status

ダイアログの結果（どのように閉じられたか）を保持するプロパティです。

- UNKNOWN<br>無効値（ダイアログはまだ閉じられていない）
- POSITIVE<br>positiveボタン（OKなど）の押下によってダイアログが閉じた。
- NEGATIVE<br>negativeボタン（Cancelなど）の押下によってダイアログが閉じた。
- NEUTRAL<br>neutralボタンの押下によってダイアログが閉じた（３ボタンメッセージボックスの場合のみ）。

`ok` (== positive), `cancel` (== negative), `yes`, `no` などの判定用プロパティを持っており、通常は、`IUtImmortalTask.showDialog()` の戻り値 (IUtDialog) から `status.ok` のようにチェックします。

## 表示モード

### var isDialog : Boolean

デフォルト：`UtDialogConfig.showInDialogModeAsDefault`（初期値: true）

- true（ダイアログモード）: DialogFragment#show() によって表示されます。Activity の Window ではなく、新しい Window が作成され、その上にダイアログが表示されます。
- false（フラグメントモード）: FragmentManager のトランザクションにより、Activity の Window 上にダイアログが表示されます。

このフラグをダイアログ毎に変更する場合は、preCreateBodyView() ではなく、コンストラクタで設定してください。

### システムゾーン（システムバー・カットアウト等）の扱い

ダイアログを配置するとき、ステータスバーやナビゲーションバー、ディスプレイの切り欠き（カットアウト）などのシステム領域をどのように除けるかを、`systemZoneOption` で指定します。

- SystemZoneOption.NONE<br>何もしません（全画面に表示）。
- SystemZoneOption.FIT_TO_ACTIVITY（デフォルト）<br>Activity の Window（コンテント領域）に合わせます。
- SystemZoneOption.HIDE_ACTION_BAR<br>ActionBar を非表示にして、できるだけ全画面に表示します。
- SystemZoneOption.CUSTOM_INSETS<br>`systemZoneFlags` に指定されたシステム領域（SystemZone.SYSTEM_BARS / IME / CUTOUT の組み合わせ）を除けます。

CUSTOM_INSETS を使う場合は、`setCustomSystemZone()` でフラグとまとめて設定できます。

```kotlin
setCustomSystemZone(UtDialogConfig.SystemZone.SYSTEM_BARS, UtDialogConfig.SystemZone.CUTOUT)
```

デフォルト値は、`UtDialogConfig.systemZoneOption` / `UtDialogConfig.systemZoneFlags` で変更できます。ダイアログ毎に変更する場合は、コンストラクタで設定してください。

## 動作・操作モード

### var cancellable:Boolean

デフォルト: true

ダイアログ（またはメッセージボックス）の画面外をタップしたときキャンセルしてダイアログを閉じるかどうかを指定します。画面外をタップしてもダイアログを閉じないようにする場合は、false にします。
任意のタイミングで変更できます。

UtDialogEx を使用する場合は、`Binder.dialogCancellable()` 拡張関数によってビューモデルにバインドできます。

### var positiveCancellable:Boolean (protected)

デフォルト: false

デフォルトでは、ダイアログの画面外をタップしたときは、キャンセル扱い（negative()を呼び出してダイアログを閉じる）ですが、positiveCancellable を true にすると、画面外をタップしたとき、positive() を呼び出してダイアログを閉じます。

### var scrollable:Boolean

デフォルト: false

コンテナビューでスクロールを有効にするかどうかを指定します。true にする場合は、heightOption を COMPACT 以外（AUTO_SCROLL を推奨）にしてください。heightOption=AUTO_SCROLL の場合は常に scrollable=true として動作します。

### var draggable:Boolean

デフォルト: `UtDialogConfig.draggable`（初期値: false）

true にすると、ダイアログのタイトルバーをドラッグして、ダイアログを移動することができるようになります。

### var clipVerticalOnDrag:Boolean

デフォルト: true

true の場合、ダイアログをドラッグするとき、デバイス画面の上端・下端より外側に移動できないよう制限します。

### var clipHorizontalOnDrag:Boolean

デフォルト: true

true の場合、ダイアログをドラッグするとき、デバイス画面の左端・右端より外側に移動できないよう制限します（false でも、操作不能にならない程度にはクリップされます）。

### var animationEffect:Boolean

デフォルト: `UtDialogConfig.animationEffect`（初期値: true）

false にすると、ダイアログを表示するときのフェードイン/アウトアニメーションを無効化します。

### var noHeader:Boolean

デフォルト: false

true にすると、標準のタイトルバー（legacy ui の場合は ok/cancel ボタンを含む）を表示しません。

### var noFooter:Boolean

デフォルト：false

true にすると、ボタンバー（ok/cancelボタンを表示するエリア）を表示しません。legacy ui に対しては効果はありません。

### var invisibleBuiltInButton:Boolean

デフォルト：true

ダイアログボタン (leftButton/rightButton) を非表示 (ButtonType.NONE) にしたとき、そのボタンを View.INVISIBLE にするか、View.GONE にするかを指定します。デフォルト (true) では、View.INVISIBLE となります。GONE にすると、ボタンの片方だけ表示した場合にタイトルが左右に偏って表示されるので注意してください。

### var bodyContainerMargin: Int

デフォルト: -1

bodyContainer の上下左右のマージンをDP単位で指定します。-1 を指定すると、デフォルト値（dialog_frame.xml で定義された 8dp）が使用されます。上下左右を個別にカスタマイズするときは、onViewCreated() で、bodyContainer のマージンを直接設定してください。

### var noDialogMargin:Boolean

デフォルト: false

デバイス画面に対するダイアログのマージンは、UtDialogConfig.dialogMarginOnPortrait（縦置きの場合）および、UtDialogConfig.dialogMarginOnLandscape（横置きの場合）で設定します。noDialogMargin = true にすると、このマージン設定を無効化して、デバイス画面全体にダイアログを表示します。

## サイズ・表示位置

### var widthOption: WidthOption

デフォルト: WidthOption.COMPACT

ダイアログの幅を設定します。詳細は [WidthOption/HeightOption の使い方](./sizing-option-ja.md) をご参照ください。

### var heightOption: HeightOption

デフォルト: HeightOption.COMPACT

ダイアログの高さを設定します。詳細は [WidthOption/HeightOption の使い方](./sizing-option-ja.md) をご参照ください。

### var gravityOption: GravityOption

デフォルト：GravityOption.CENTER

ダイアログを配置する位置を指定します。次の４つの値が設定できます。

- GravityOption.CENTER<br>画面中央に配置（デフォルト）
- GravityOption.RIGHT_TOP<br>画面右上に配置
- GravityOption.LEFT_TOP<br>画面左上に配置
- GravityOption.CUSTOM<br>customPositionX, customPositionY プロパティで位置を指定

### var customPositionX: Float?
### var customPositionY: Float?

デフォルト: null

GravityOption.CUSTOM と組み合わせて、ダイアログの表示位置（rootView に対するローカル座標）を指定します。
また、draggable = true の場合、現在のダイアログの表示位置の取得/設定にも使用できます。

## ガードビュー（ダイアログの「画面外」）

### var guardColor: GuardColor

デフォルト： GuardColor.INVALID（未指定）

ダイアログの外側（rootView）の色を指定します。次の値が使用できます。

- GuardColor.TRANSPARENT<br>透明
- GuardColor.DIM<br>黒っぽい透過色
- GuardColor.SEE_THROUGH<br>白っぽい透過色
- GuardColor.SOLID_GRAY<br>不透過の灰色
- GuardColor.THEME_DIM<br>テーマの文字色をベースにした透過色（黒っぽい/白っぽいがテーマによって動的に変わる）
- GuardColor.THEME_SEE_THROUGH<br>テーマの背景色をベースにした透過色（黒っぽい/白っぽいがテーマによって動的に変わる）
- GuardColor.CUSTOM(color:Int)<br>任意の色を指定

無指定（デフォルト）の場合は、`cancellable=true` なら `UtDialogConfig.defaultGuardColorOfCancellableDialog`（初期値：TRANSPARENT）、それ以外の場合は `UtDialogConfig.defaultGuardColor`（初期値：THEME_DIM）が使われます。ただし、isPhone == true 且つ、UtDialogConfig.solidBackgroundOnPhone == true の場合は、この設定を無視して、常に `GuardColor.SOLID_GRAY` が使われます。

### var bodyGuardColor: GuardColor

デフォルト: `UtDialogConfig.defaultBodyGuardColor`（初期値: THEME_SEE_THROUGH）

bodyGuardView の背景色を指定します。bodyGuardView は、ビジーの場合などに、ダイアログ (bodyView) に対するタッチ操作をブロックするためのビューです。ダイアログボタン (leftButton, rightButton) はブロックされません。必要に応じて、これらのボタンを無効化、非表示化してください。
設定可能な値は、`guardColor` の説明を参照願います。

## ソフトウェアキーボード（IME）への対応

### var adjustContentForKeyboard: KeyboardAdjustMode

デフォルト: `UtDialogConfig.adjustContentForKeyboard`（初期値: NONE）

ソフトウェアキーボードが表示されたとき、フォーカスのある EditText が隠れないようにコンテンツを自動調整するかどうかを指定します。

- KeyboardAdjustMode.NONE<br>何もしません。
- KeyboardAdjustMode.AUTO<br>isDialog==true なら BY_GLOBAL_LAYOUT, false なら BY_WINDOW_INSETS を使用します。
- KeyboardAdjustMode.BY_WINDOW_INSETS<br>WindowInsets のリスナーを利用して IME のサイズを取得します。
- KeyboardAdjustMode.BY_GLOBAL_LAYOUT<br>GlobalLayout のリスナーを利用して、IME らしきビューの出現を監視します（WindowInsets のリスナーが呼ばれないケースへの対策）。

### var adjustContentsStrategy: KeyboardAdjustStrategy

デフォルト: `UtDialogConfig.adjustContentsStrategy`（初期値: PAN）

KeyboardAdjustMode.NONE 以外の場合に、どうやってコンテンツを調整するかを指定します。

- KeyboardAdjustStrategy.PAN<br>translationY を調整してダイアログをスライドさせます。
- KeyboardAdjustStrategy.RESIZE<br>paddingBottom でダイアログの高さを変えます（HeightOption.FULL/AUTO_SCROLL など、高さを調整可能なビューを含む場合に最適）。

## タイトル・ビルトインボタン

### var title:String?

デフォルト: null

タイトルバーに表示する文字列です。任意のタイミングで設定できます。

UtDialogEx を使う場合は、`Binder.dialogTitle()` 拡張関数により、ViewModel にバインドできます。

### var leftButtonType:ButtonType

デフォルト：ButtonType.NONE

左側のビルトインボタンのタイプを指定します。以下の値が指定可能です。

- NONE（デフォルト）<br>ボタンを表示しません。非表示になったボタンの配置方法は、invisibleBuiltInButton に従います。
- OK<br>OKボタン（positive）を表示します。
- DONE<br>DONEボタン（positive）を表示します。
- CLOSE<br>CLOSEボタン（positive）を表示します。
- CANCEL<br>CANCELボタン（negative）を表示します。
- BACK<br>BACKボタン（negative）を表示します。
- NEGATIVE_CLOSE<br>CLOSEボタン（negative）を表示します。
- POSITIVE_BACK<br>BACKボタン（positive）を表示します。
- CUSTOM(string:String, positive:Boolean)<br>任意の文字列を指定してボタンを表示します。文字列リソースID を渡すオーバーロードもあります。

### var rightButtonType:ButtonType

デフォルト：ButtonType.NONE

右側のビルトインボタンのタイプを指定します。指定可能な値は、leftButtonType の説明を参照願います。

### var optionButtonType:ButtonType

デフォルト：ButtonType.NONE

タイトルバー上に表示するオプションボタンのタイプを指定します。指定可能な値は、leftButtonType の説明を参照願います。ボタン押下時の動作は、UtDialogEx の `Binder.dialogOptionButtonCommand()` などでバインドします（positive/negative としてダイアログを閉じる動作にはなりません）。

## 親子ダイアログ

### var parentVisibilityOption: ParentVisibilityOption

デフォルト: ParentVisibilityOption.HIDE_AND_SHOW

サブダイアログを開くとき、親ダイアログを非表示にするかどうかを指定します。

- NONE<br>何もしません（親を表示しっぱなし）。
- HIDE_AND_SHOW（デフォルト）<br>このダイアログを開くときに親を非表示にして、閉じるときに再表示します。
- HIDE_AND_SHOW_ON_NEGATIVE<br>negative で閉じるときだけ、親を再表示します。
- HIDE_AND_SHOW_ON_POSITIVE<br>positive で閉じるときだけ、親を再表示します。
- HIDE_AND_LEAVE_IT<br>開くときに親を非表示にして、あとは何もしません。

### val rootDialog : UtDialog?

ルートダイアログ（ダイアログチェーンの先頭）を取得します。

### val parentDialog : UtDialog?

親ダイアログを取得します。

## 状態取得用のプロパティ

### val orientation:Int

resources.configuration.orientation の値（@Orientation）を返します。

### val isLandscape :Boolean

デバイスが横置き（ランドスケープ）の場合は true、それ以外の場合は false を返します。

### val isPortrait :Boolean

デバイスが縦置き（ポートレート）の場合は true、それ以外の場合は false を返します。

### val isPhone :Boolean

デバイスが Phone の場合は true、それ以外の場合は false を返します。
デバイス画面の短辺が 600dp 未満なら phone、600dp以上なら tablet と判断しています。

### val isTablet :Boolean

!isPhone を返します。

## ビルトインビューを取得するためのプロパティ

### val titleView:TextView

タイトルを表示する TextView です。

### val leftButton: Button

左側のビルトインボタンです。leftButtonType によって表示内容を設定します。

UtDialogEx を使用する場合には、`Binder.dialogLeftButtonVisibility()`, `Binder.dialogLeftButtonEnable()`, `Binder.dialogLeftButtonString()`, `Binder.dialogLeftButtonCommand()` 拡張関数によって、表示/非表示, 有効/無効, ボタンキャプション, ボタン押下時のコマンドをビューモデルにバインドできます。

### val rightButton: Button

右側のビルトインボタンです。rightButtonType によって表示内容を設定します。

UtDialogEx を使用する場合には、`Binder.dialogRightButtonVisibility()`, `Binder.dialogRightButtonEnable()`, `Binder.dialogRightButtonString()`, `Binder.dialogRightButtonCommand()` 拡張関数が利用できます。

### val optionButton: Button?

タイトルバー上のオプションボタンです。optionButtonType を設定した場合のみ有効です。

### val progressRingOnTitleBar: ProgressBar

タイトルバー上に表示する Progress Ring です。
デフォルトでは非表示 (INVISIBLE) ですが、例えば、ダイアログのコンテントをサーバーからダウンロードする場合など、初期化に時間がかかる場合には、progressRingOnTitleBar を VISIBLE にし、初期化が終わったら GONE に戻します。

UtDialogEx を利用する場合は、`Binder.dialogProgressRingOnTitleBarVisibility()` 拡張関数で、Progress Ring の表示・非表示をビューモデルにバインドできます。

### val rootView: ViewGroup

ダイアログの背景となるデバイス画面全体を覆うビューです。`guardColor` によって指定された背景色で描画されます。

### val dialogView:ViewGroup

ダイアログ画面としてユーザーに見える最上位のビューです。rootView 上に表示され、widthOption, heightOption, gravityOption, customPositionX, customPositionY などによって、サイズや位置が調整されます。

### val bodyContainer:ViewGroup

bodyView のコンテナです。scrollable == true の場合は ScrollView、それ以外の場合は、FrameLayout になります。

### val bodyView:View

UtDialog のサブクラスでオーバーライドされる createBodyView() によって作成されたビューです。

### val refContainerView:View

コンテナ領域（ダイアログ領域から、ヘッダー/フッターの領域、マージンを除いた領域）を取得するための invisible なビューです。HeightOption.AUTO_SCROLL や、HeightOption.CUSTOM でサイズ計算を行うために UtDialog 内部で使われます。通常、サブクラスなどから直接利用することはありません。

### val bodyGuardView:FrameLayout

bodyGuardView は、ダイアログに対するタッチ操作をブロックするためのビューです。
デフォルトでは非表示 (GONE) ですが、例えば、OKボタンを押したあと、処理が完了するまで待機するような場合に、VISIBLE にします。ただし、ダイアログボタン (leftButton, rightButton) は bodyView に含まれないのでブロックされません。必要に応じて、これらのボタンを無効化、非表示化してください。bodyGuardView の背景色は `bodyGuardColor` によってカスタマイズできます。

UtDialogEx を利用する場合は、`Binder.dialogBodyGuardViewVisibility()` 拡張関数で、bodyGuardView の表示・非表示をビューモデルにバインドできます。

### val centerProgressRing:ProgressBar

bodyGuardView の中央に表示するプログレスリングです。デフォルトでは非表示ですが、bodyGuardView とともに VISIBLE にすることで表示されます。

UtDialogEx を利用する場合は、`Binder.dialogBodyGuardViewVisibility()` 拡張関数で、bodyGuardView を表示したときに、centerProgressRing も表示するかどうかを指定できます。

## UtDialogサブクラスから利用可能なメソッド

### fun show(activity: FragmentActivity, tag:String?)

ダイアログを表示します。
通常は、`IUtImmortalTask.showDialog()` を使ってタスク内からダイアログを表示するので、直接 show() を呼び出すことはありません。

### fun complete(status: Status)

ダイアログを指定されたステータスで閉じます。
通常、UtDialog 派生クラスでは、onPositive() / onNegative() メソッドを使ってダイアログを閉じます。

### fun cancel()

ダイアログを Status.NEGATIVE ステータスで閉じます。`complete(Status.NEGATIVE)` と同義です。

### fun forceDismiss()

強制的にダイアログを閉じます。
通常は使いません。アクティビティを終了するときに開いているダイアログをすべて閉じる、UtDialogHelper.forceCloseAllDialogs() から例外的に呼び出されます。

### fun enableFocusManagement(withDialogButtons:Boolean = true, useKey:UseKey? = null): UtFocusManager

[フォーカス管理](./focus-manager-ja.md)を有効化して、UtFocusManager を返します。

### fun updateCustomHeight()

heightOption = CUSTOM の場合、bodyView の高さが変化したときにこのメソッドを呼び出して、ダイアログサイズを再計算・更新します。

## UtDialogサブクラスでオーバーライドが必要なメソッド

### fun preCreateBodyView()

UtDialog は、一部のプロパティ (title, cancellable など) を除いて、大部分のプロパティを createBodyView() が呼ばれる前に設定しておく必要があります。preCreateBodyView() は、これらのプロパティを設定する最適なタイミングです。

ただし、

- isDialog
- systemZoneOption / systemZoneFlags

をダイアログ毎に変更する場合は、preCreateBodyView() ではなく、コンストラクタで設定してください。これらをダイアログ毎に設定する必然性は薄いので、UtDialogConfig でデフォルト値を設定することを検討してください。

### fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View

ダイアログの bodyView を作成するためにオーバーライドします。layout.xml からビューを構築する場合は、ダイアログテーマを正しく反映するため、必ず、引数で渡される inflater を使用してください。

### fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int

heightOption に CUSTOM を指定した場合は、必ず、このメソッドをオーバーライドしてください。
引数として、以下の値が渡されます。

- currentBodyHeight<br>現在の bodyView（createBodyView が返したビュー）の高さです。
- currentContainerHeight<br>現在の containerView（bodyView の親）の高さです。通常は currentBodyHeight と一致します。
- maxContainerHeight<br>コンテナの高さの最大値です。このサイズを超えないよう、bodyView の高さを調整してください。

戻り値として、bodyView の高さを調整したあとの、containerView の高さを返してください。

## UtDialogサブクラスでオーバーライド可能なメソッド

### fun confirmToCompletePositive():Boolean

このメソッドをオーバーライドして、false を返すと、positive ボタン押下時にダイアログを閉じません。
ダイアログで必要な設定が揃っていない場合に、OKでダイアログを閉じないようにする場合に利用できます。使用例は[チュートリアル（基本編）](./tutorial-basic-ja.md#改良ダイアログを閉じる前に入力値を検証する)をご参照ください。

### fun confirmToCompleteNegative():Boolean

このメソッドをオーバーライドして、false を返すと、negative ボタン押下時にダイアログを閉じません。
何かの処理が終わるまでダイアログを閉じないようにする場合などに利用できます。

## グローバルオプション（UtDialogConfig）

アプリ内で共通のダイアログ動作に関する設定は、`UtDialogConfig` オブジェクトにまとめられています。Application#onCreate() などで設定してください。

### fun setup(context: Context, table:IUtStringTable? = null)

ライブラリを初期化します。OK/Cancel などの標準文字列を利用するために、アプリ起動時に呼び出してください。

### var showInDialogModeAsDefault = true

`UtDialog#isDialog` のデフォルト値を設定します。

### var animationEffect = true

`UtDialog#animationEffect` のデフォルト値を設定します。

### var draggable = false

`UtDialog#draggable` のデフォルト値を設定します。

### var systemZoneOption = SystemZoneOption.FIT_TO_ACTIVITY

システム領域（システムバー・カットアウト）の除け方のデフォルト値を設定します。詳細は [システムゾーンの扱い](#システムゾーンシステムバーカットアウト等の扱い) を参照してください。

### var systemZoneFlags = SystemZone.NORMAL

systemZoneOption = CUSTOM_INSETS の場合に除けるシステム領域を、SystemZone.SYSTEM_BARS / IME / CUTOUT の組み合わせで指定します。

### var adjustContentForKeyboard = KeyboardAdjustMode.NONE

`UtDialog#adjustContentForKeyboard` のデフォルト値を設定します。

### var adjustContentsStrategy = KeyboardAdjustStrategy.PAN

`UtDialog#adjustContentsStrategy` のデフォルト値を設定します。

### var showDialogImmediately = ShowDialogMode.Immediately

フラグメントモード (isDialog=false) でダイアログを表示する方法を指定します。

- ShowDialogMode.Immediately（デフォルト）<br>FragmentTransaction#commit() を呼んだあとすぐに、FragmentManager#executePendingTransactions() を実行します。
- ShowDialogMode.Commit<br>FragmentTransaction#commit() を呼びます。
- ShowDialogMode.CommitNow<br>FragmentTransaction#commitNow() を呼びます。

### var solidBackgroundOnPhone = false

isPhone==true の場合に、背景を灰色 (SOLID_GRAY) で塗りつぶす場合は true を指定します。

デザインにもよりますが、小さい画面では、本体の画面の上にダイアログの画面が重なると、ごちゃごちゃして見づらくなることがあります。さらに、ダイアログからサブダイアログに遷移するときに、一瞬、本体の画面が透けて見えるのが気持ち悪い、という意見もあって用意したのが、この「Phoneの場合は背景を見せない」という設定です。

### var defaultGuardColor = UtDialog.GuardColor.THEME_DIM

cancellable == false の場合の、`UtDialog#guardColor` のデフォルト値です。

### var defaultGuardColorOfCancellableDialog = UtDialog.GuardColor.TRANSPARENT

cancellable == true の場合の、`UtDialog#guardColor` のデフォルト値です。

### var defaultBodyGuardColor = UtDialog.GuardColor.THEME_SEE_THROUGH

`UtDialog#bodyGuardColor` のデフォルト値です。

### var dialogTheme: Int = R.style.UtDialogTheme

ダイアログのスタイルを指定します。
デフォルト（`R.style.UtDialogTheme`）は、Material3 の colorPrimary 系をベースにした配色になっています。このほか、colorSecondary 系をベースとした `R.style.UtDialogThemeSecondary`、colorTertiary 系をベースとした `R.style.UtDialogThemeTertiary` も選べます。

### var dialogFrameId: Int = R.layout.dialog_frame

ダイアログフレーム（UtDialog の土台となるビュー）のレイアウトをリソースIDで指定します。
デフォルト (R.layout.dialog_frame) は、Material3 ベースのデザインです。Material2 (Theme.MaterialComponents) を使用する場合は、`useLegacyTheme()` メソッドを呼ぶことで、`R.layout.dialog_frame_legacy` が設定されます。

### var fadeInDuration = 300L

フェードインアニメーションの遷移時間をミリ秒単位で指定します。

### var fadeOutDuration = 400L

フェードアウトアニメーションの遷移時間をミリ秒単位で指定します。

### var dialogMarginOnPortrait: Rect? = Rect(20, 40, 20, 40)

デバイスを縦置きにしたときの、rootView に対する dialogView のマージンを指定します。
Width/HeightOption FULL/LIMIT/AUTO_SCROLL/CUSTOM を指定したときの最大サイズ決定に使用されます。null を設定するとマージンはゼロになります。UtDialog#noDialogMargin = true にすることによって、ダイアログ毎にマージンをゼロにすることもできます。

### var dialogMarginOnLandscape: Rect? = Rect(40, 20, 40, 20)

デバイスを横置きにしたときの、rootView に対する dialogView のマージンを指定します。
仕様は dialogMarginOnPortrait に準じます。

## 関連ドキュメント

- [チュートリアル（基本編）](./tutorial-basic-ja.md)
- [WidthOption/HeightOption の使い方](./sizing-option-ja.md)
- [メッセージボックス / 選択ボックス](./messagebox-ja.md)
- [フォーカスマネージャ](./focus-manager-ja.md)
- [UtImmortalTask 詳説](./immortal-task-ja.md)
