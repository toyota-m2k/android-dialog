# フォーカス管理クラス (UtFocusManager)

## 作成の動機
EditTextでエンターキーを押したときの動作（変換確定、フォーカス移動、EditorAction）、タブによるフォーカス移動が端末やIMEによってマチマチで困った。多くは望まないけど、一般民間人に説明できる程度にはすっきりさせたい。

## 解決したい課題　＝ EditTextとフォーカス移動に関する不可解な動作
課題(A) 

ダイアログを開いて、(HWキーボードの）Tabでフォーカスを移動すると、ダイアログ外（Activity上の）のコントロールにフォーカスが移動できてしまう。

    --> モーダルダイアログのつもりなので、
        ダイアログ外のボタンが押せてしまうと何が起こるか考えるだけでも恐ろしいことです。

課題(B) 

日本語入力時、HWキーボードのEnterで確定すると、（imeOptions == actionDone でも）次のコントロール (nextFocusDown) にフォーカスが移動してしまう。

    --> 途中までの入力を確定して、続きを入力、という操作の妨げとなる
## 課題(A)の対策
1. `UtFocusManager#register()`で、フォーカス移動順序、移動範囲を事前登録する。

2. Tabキー押下時（DialogやActivityの OnKeyDown）に、自力でフォーカスを移動する。

    ※制限事項※
    
    端末やIMEの種類によって、Tabキー押下イベントがアプリ側にわたって来ないものがあり、その場合はタブによるフォーカス移動には対応できない。
    
    例）Lenovoのタブレットで文字入力を開始すると、ソフトウェアキーボードが最小化したようなビューが表示されるが、これが表示された状態ではタブイベントは送られて来ない。

## 課題(B)の対策
1. nextFocusDown に自分自身を設定することで課題(A)を回避できた（他の回避方法は見つからなかった）が、確定後、次の Enter でもフォーカスが移動しなくなった。。。（まぁ、あたりまえ）

2. そこで、nextFocusForwardを無効化して、EditText の OnEditorAction で、自力でフォーカス移動するようにした。

    ※注意※

    EditText の OnEditorActionListener は１つしか設定できないので、EditTextでのEnter押下検出などのために、このリスナーを利用していると競合してしまう。そのため、この動作はオプショナルとし、`setCustomEditorAction()` で有効化することとした。

## 使い方
### UtDialog での利用
- UtDialog派生クラスのコンストラクタ、または、preCreateBodyView() で、rootFocusManager を初期化する
    ```
    enableFocusManagement(true)             // rootFocusManagerを有効化する。Boolean型引数を false にすると、ヘッダー上のボタン(Done/Cancelなど）を管理対象から外す。
        .autoRegister()                     // この例ではフォーカス対象を自動登録。個別に登録する場合は、register()に、R.id.xxxx を渡す。
        .setCustomEditorAction()            // Enterキーによる自力フォーカス移動を有効化
        .setInitialFocus(R.id.input_1)      // 初期状態でフォーカスをセットするコントロールを指定（任意）
    ```
### 一般的なActivityやFragmentでの利用
- ActivityやFragmentのメンバーとして、UtFocusManagerインスタンスを作って初期化、管理対象ビューの登録を行う。
- `Activity#onCreate()`または、`Fragment#onCreateView()` で、`UtFocusManager#attach()` を呼び出して、適切なビュー(IdRes --> View解決に利用されるルートビュー）をアタッチする。
- `Activity#onKeyDown()` （`UtMortalActivity`の場合は、`handleKeyDown()`）をオーバーライドして、`UtFocusManager#handleTabEvent()`を呼び出す。

## 複雑なコンテナの構成
- リストビューのコンテントなど、IDが重複するような複雑なコンテナのフォーカスを管理したい場合は、UtFocusManagerを階層化することで対応する。
- フォーカスマネージャの階層構造を構築するには、`appendChild()`, `insertChildAfter()`, `removeChild()` メソッドを利用する。
