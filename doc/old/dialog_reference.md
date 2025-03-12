# UtDialog リファレンス

## 初期設定用プロパティ

初期設定用プロパティは、コンストラクタ、または、preCreateBodyView()で設定することにより、その後のダイアログの構成や動作を決定する。
一部、動的な変更を可能にするメソッドが用意されている場合を除き、createDialogBody() 以降、これらを変更することはできない。

### title :String
    タイトルバーの中央に表示される、ダイアログタイトル。
    動的にタイトルを変更するときは、replaceTitle()メソッドを使用する。

### scrollable :Boolean
    bodyView をスクロール可能にする場合は trueを, スクロールしない場合、または、bodyView内にスクロール可能なコントロールを含む場合（自力でスクロールする場合）は falseを指定する。
    ただし、heightOption の指定によって、内部的に適切な値に変更されることがある。
    デフォルト： false

### cancellable :Boolean
    画面外タップでキャンセル可能にする場合は true, キャンセル不可とする場合は false を指定する。
    デフォルト： true

### widthOption :[WidthOption](./width_option.md)
    ダイアログの幅を決定するためのフラグを指定
    FIXED, LIMITの場合は、widthHintと合わせてセットする必要があるので、それぞれ専用の設定メソッド、setFixedWidth(), setLimitWidth()の使用を推奨。
    デフォルト：COMPACT

### widthHint :Int
    widthOptionとの組み合わせで使用される。widthOptionが、FIXED, LIMIT以外の場合は無視される。
    デフォルト：0

### heightOption :[HeightOption](./height_option.md)
    ダイアログの高さを決定するためのフラグを指定。
    FIXEDの場合は、heightHintと合わせてセットする必要があるので、専用の設定メソッド、setFixedHeight()の使用を推奨。
    デフォルト：COMPACT

### heightHint :Int
    heightOptionとの組み合わせで使用される。heightOptionが、FIXEDの場合以外は無視される。

### gravityOption: GravityOption
    デバイスの画面（スクリーン）全体に対する、ダイアログの表示位置を指定する。
    次の３つをサポート。他にもいろいろ考えられるが、使いそうにないのでとりあえず。。。
      - RIGHT_TOP: 右上に表示
      - CENTER:　中央に表示
      - LEFT_TOP: 左上に表示
    デフォルト：RIGHT_TOP 

### guardColor: Int @ColorInt
    ダイアログの画面外の表示色を指定。
    デフォルトでは、cancellable==true の場合は透明(GuardColor.TRANSPARENT)、cancellable==falseの場合は、黒っぽい半透明色となる。

-----
## ボタン(leftButton/rightButton)の設定

ボタンのラベル、色、種別(Positive/Negative) は、専用の設定メソッド (setLeftButton / setRightButton) を使って指定する。これらのメソッドは初期設定用に使うほか、ダイアログ表示後の動的な設定にも使える。

### デザインガイドライン
    leftButtonはNEGATIVE、rightButtonはPOSITIVEボタンとする。
    POSITIVEボタンは青い背景、NEGATIVEボタンは白い背景とする。
### setLeftButton(@StringRes id:Int, positive: Boolean=false, blue:Boolean=positive)
    タイトルバーの左側に表示するボタンの設定。
    id
        ラベルのリソースID。ゼロなら、左側のボタンを表示しない（デフォルト）
    positive
        タップ時に、Status.POSITIVE を返すか、Status.NEGATIVE を返すかを指定。
    blue
        true:青い背景のボタン / false:白い背景のボタン

### fun setLeftButton(type: BuiltInButtonType)
    作り付けボタンを指定。
      - Positiveボタン（青い背景）
        - OK
        - DONE
        - CLOSE
      - Negativeボタン（白い背景）
        - CANCEL
        - BACK
        - CLOSE_LEFT

### setRightButton(@StringRes id:Int, positive: Boolean=false, blue:Boolean=positive)
### fun setRightButton(type: BuiltInButtonType)
    タイトルバーの右側に表示するのボタンの設定。
    パラメータは、setLeftButton()と同じ。

-----
## 必ずオーバーライドするメソッド

### fun createBodyView(savedInstanceState:Bundle?, inflater: IViewInflater): View
    サブクラスで、bodyViewを生成するためのメソッドで、必ずオーバーライドする。

    savedInstanceState
        復元の場合はBundleが渡される。
        最近はViewModelを使うので、重要性は低下しているが、これを使う場合は、onSaveInstanceState()もオーバーライドして、やるべきことをやること。
    inflater
        layout xml からビューを作成するとき、このinflatorを使ってInflateすること。View.inflatorとか、テキトーなものを使ってはならない。
    
    上述の「初期設定用プロパティ」を、このメソッド内で変更しても有効にならないので注意。
    デバイスのorientationやサイズによって設定を動的に指定したい場合は、preCreateBodyView()メソッドをオーバーライドする。

-----
## 必要に応じてオーバーライドするメソッド

### fun preCreateBodyView()
    createBodyView()の直前に呼び出される。
    UtDialogの初期設定用プロパティを変更するラストチャンス。
    デバイスのorientationや、サイズ(phone/tablet)によって設定を動的に変える場合は、このメソッドをオーバーライドする。
    もちろん、設定を変更しない場合も、このタイミングで指定してもよい。

### fun calcCustomContainerHeight(currentBodyHeight:Int, currentContainerHeight:Int, maxContainerHeight:Int):Int
    heightOption に CUSTOM を指定した場合は、必ずオーバーライドする。

    currentBodyHeight
        現在の bodyViewの高さ (px)
    currentContainerHeight
        現在の bodyContainer (bodyViewの親のViewGroup)の高さ(px)
        bodyViewでマージンを設定していなければ、currentBodyHeightと一致する。
    maxContainerHeight
        bodyContainer の高さとして設定可能な最大サイズ
    
    @return
        新たに設定するbodyContainer の高さ

-----
## サブクラスから利用可能なプロパティ

### screenSize :MutableSize
    スクリーン（デバイス画面）のサイズ
    calcCustomContainerHeight()のヒントとして使用されることを想定し、HeightOption.CUSTOM で、createBodyView()以降にのみ、正しい値を返すことを保証する。
    
### orientation:Int
    preCreateBodyView()から動的な設定変更のために参照することを想定。
    Configuration.ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE, などの値を返す。
### isLandscape:Boolean
    preCreateBodyView()から動的な設定変更のために参照することを想定。
    orientation == Configuration.ORIENTATION_LANDSCAPE

### isPortrait:Boolean
    preCreateBodyView()から動的な設定変更のために参照することを想定。
    orientation == Configuration.ORIENTATION_PORTRAIT
    
### isPhone:Boolean
    preCreateBodyView()から動的な設定変更のために参照することを想定。
    sw600dp以下の場合にtrueを返す。
### isTablet:Boolean
    preCreateBodyView()から動的な設定変更のために参照することを想定。
    !isPhone

### bodyView:View
    createBodyView()が返したView
    当然、createBodyView()がreturnした後でのみ有効。

そのほか、以下のviewもサブクラスから参照可能だが、これらを直接操作することはないと思う。 

- lateinit var titleView:TextView
- lateinit var leftButton: Button
- lateinit var rightButton: Button
- lateinit var rootView: FrameLayout              // 全画面を覆う透過の背景となるダイアログのルート：
- lateinit var dialogView:ConstraintLayout        // ダイアログ画面としてユーザーに見えるビュー。rootView上で位置、サイズを調整する。
- lateinit var bodyContainer:FrameLayout          // bodyViewの入れ物

-----
## UtDialogBase から継承するプロパティ

### status: IUtDialog.Status
    ダイアログの状態を保持し、呼び出し元に渡すためのプロパティ
    complete()メソッドで設定され、通常、UtDialogサブクラスから直接操作、参照することはない。

### visible:Boolean
    ダイアログの表示・非表示を操作、取得するプロパティ。
    通常は、子ダイアログを表示するときに、親ダイアログを隠す、などの動作のためにUtDialogBase内で管理される。
    使ってもよいが、直接操作する状況は少ないと思う。

### parentVisibilityOption
    子ダイアログを表示するときに、親ダイアログを隠すかどうかを指定するフラグ。
    NONE 
        何もしない
    HIDE_AND_SHOW 
        子ダイアログを表示するときに親ダイアログを隠し、子ダイアログ閉じるときに、親ダイアログを表示する。
    HIDE_AND_LEAVE_IT
        子ダイアログを表示するときに親ダイアログを隠すが、子ダイアログを表示するときには放置する。
        親ダイアログは非表示のまま残り、キャンセルされるわけではない。

    UtMessageBox, UtSelectionBox のデフォルトは NONE, UtDialog のデフォルトは HIDE_AND_SHOW。
