# フォーカス管理クラス (UtFocusManager)
<div align="right">
<a href="./focus-manager.md">EN</a> | JA
</div>


普段あまり意識しないのですが、外付けキーボードをつないでみたり、Chromebookで実行したりすると、いろいろ問題が発生します。

- EditTextでタブキーやエンターキーを押したときの動作（変換確定、フォーカス移動、EditorAction）が、端末やIMEによって異なる。
- 日本語入力時、HWキーボードのEnterで確定すると、（imeOptions == actionDone でも）次のコントロール (nextFocusDown) にフォーカスが移動してしまう。
- ダイアログ内でフォーカス移動しているつもりが、Activity本体側のコントロールにフォーカスが入ってしまう。

`UtFocusManager` を使うことで、これらの問題の大部分が回避できます。
尚、UtFocusManager は、UtDialog の標準機能として利用していますが、任意のActivityやFragment でも使えるように設計しています。

## 使い方
### UtDialog での利用

- UtDialog派生クラスのコンストラクタ、または、preCreateBodyView() で、rootFocusManager を初期化します。
    ```
    enableFocusManagement(true)             // rootFocusManagerを有効化する。Boolean型引数を false にすると、ヘッダー上のボタン(Done/Cancelなど)を管理対象から外す。
        .autoRegister()                     // この例ではフォーカス対象を自動登録。個別に登録する場合は、register()に、R.id.xxxx を渡す。
        .setCustomEditorAction()            // Enterキーによる自力フォーカス移動を有効化
        .setInitialFocus(R.id.input_1)      // 初期状態でフォーカスをセットするコントロールを指定（任意）
    ```
### 一般的なActivityやFragmentでの利用
- ActivityやFragmentのメンバーとして、UtFocusManagerインスタンスを作って初期化、管理対象ビューを登録します。
- `Activity#onCreate()`または、`Fragment#onCreateView()` で、`UtFocusManager#attach()` を呼び出して、ルートなるビュー(IdRes --> View解決に利用できるルートビュー) をアタッチします。
- `Activity#onKeyDown()` をオーバーライドして、`UtFocusManager#handleTabEvent()`を呼び出します。

## 複雑なコンテナの構成
- リストビューのコンテントなど、IDが重複するような複雑なコンテナのフォーカスを管理したい場合は、UtFocusManagerを階層化することができます。
- フォーカスマネージャの階層構造を構築するには、`appendChild()`, `insertChildAfter()`, `removeChild()` メソッドを使います。
- 実際に、UtDialogでは、ダイアログのビルトインボタンを含むrootViewと、サブクラスが作成する bodyView とを階層構造で保持しています。

