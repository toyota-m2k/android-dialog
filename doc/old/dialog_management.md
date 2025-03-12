
# IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには

## はじめに

当初、このライブラリは、Windowsアプリのボタンクリックイベントで、例えば、
void OnButton




onCompletedなどのイベントハンドラで処理を完結するように構成できるなら、ここで説明する複雑な実装は不要です。実際、１つのActivityでのみ使用されるダイアログの場合は、ViewModelを介して状態変化をやり取りできる場合が多く、ダイアログの onCompletedで、ViewModelの Observableな(MutableFlowやMutableLiveDataの)フィールドに値を書き込む、あるいは、[android-binding](https://github.com/toyota-m2k/android-binding) の [ICommand](https://github.com/toyota-m2k/android-binding#command-classes) を使ってActivity にコールバックすることにより、大部分の機能は実現できるはずです。

このドキュメントでは、ダイアログの結果（＝ユーザーの判断）よって、処理フローを分岐するために、IUtDialogの結果を外部(Activity/Fragment/ViewModel)で受け取る方法を説明します。

尚、ここで説明する実装は、すべて UtMortalActivity クラスに実装されており、Activity を、AppCompatActivityの代わりに、UtMortalActivity から派生すれば、一切の処理を

尚、onCompletedなどのイベントハンドラで処理を完結するように構成できるなら、ここで説明する複雑な実装は不要です。実際、１つのActivityでのみ使用されるダイアログの場合は、ViewModelを介して状態変化をやり取りできる場合が多く、ダイアログの onCompletedで、ViewModelの Observableな(MutableFlowやMutableLiveDataの)フィールドに値を書き込む、あるいは、[android-binding](https://github.com/toyota-m2k/android-binding) の [ICommand](https://github.com/toyota-m2k/android-binding#command-classes) を使ってActivity にコールバックすることにより、大部分の機能は実現できるはずです。

## インターフェース

IUtDialog の結果を Activity/Fragment で受け取るために、２つの i/f を実装する。

- IUtDialogResultReceptor

  ダイアログの結果を受け取る i/f

    ```Kotlin
    interface IUtDialogResultReceptor {
        fun onDialogResult(caller: IUtDialog)
    }
    ```

- IUtDialogHost
  
  ダイアログから、その結果を受け取る IUtDialogResultReceptor を探すための i/f。

    ```Kotlin
    interface IUtDialogHost {
        fun queryDialogResultReceptor(tag:String): IUtDialogResultReceptor?
    }
    ```
  ダイアログは、IUtDialogHost　を継承する ActivityまたはFragment を起点として、ダイアログのタグ(Fragment::tag)をキーに、それを処理する IUtDialogResultReceptor を探して、onDialogResult()を呼び出す。



## 【１】基本形

Activityからダイアログを表示して、Activity で結果を受け取るだけの、最小限の実装。
サポートクラス（UtDialogHostManager）を使わず、必要な処理を自力で実装する。


1. ダイアログを一意に識別するタグ（文字列定数）を決める。
    ```Kotlin
    companion object {
        const val YES_NO_DIALOG_TAG = "YesNoDialog",
    }
    ```
    ダイアログのタグは、UtDialogが IUtDialogResultReceptor を探すときのユニークキーとなる。


2. Activityで IUtDialogHost を継承する。
    ```Kotlin
    class MainActivity : AppCompatActivity(), IUtDialogHost {
        ...

    ```

3. IUtDialogHost.queryDialogResultReceptor()をオーバーライドする。

    ```Kotlin
    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return when(tag) {
            YES_NO_DIALOG_TAG -> yesNoReceptor  // タグに対応する receptorを返す
            else -> null                        // 知らないタグならnullを返す。
        }
    }
    ```

4. ダイアログからの結果を受け取る、IUtDialogResultReceptor を実装する。

    ```Kotlin
    private val yesNoReceptor = object : IUtDialogResultReceptor {
        override fun onDialogResult(caller: IUtDialog) {
            // caller から必要な情報を取得する
            // 例えば、Yes/No で分岐するなら、こう。
            if(caller.status.yes) {
                // YESボタンでダイアログが閉じた
                doAction()
            } else {
                // NOまたはキャンセルされた
                dontAction()
            }
        }
    }
    ```
    この例では、Activityのメンバー変数として実装しているが、Activity自体を

    onDialogResultの引数として渡される caller は 表示したダイアログのインスタンスであり、これをそれぞれのダイアログ型にキャストすることで、必要な情報を取り出すことができる。



5. ダイアログを表示する。
   
    ```Kotlin
    // なんかのボタンがクリックされた
    private fun onSomeButtonClick(view:View?) {
        UtMessageBox
        .createForYesNo("ボタンが押されました", "続けますか？")
        .show(this, YES_NO_DIALOG_TAG)
    }
    ```

## 【２】UtDialogHostManager を利用する場合

上記「基本形」は、動作原理を説明するため、IUtDialogHost と IUtDialogResultReceptor を直接実装したが、
サポートクラスのUtDialogHostManager を使用することにより、複数のダイアログを扱ったり、その実装を、Dialog や ViewModel に分散したり、ということが容易になる。

UtDialogHostManager は、複数の IUtDialogHost を階層化し、タグによる、IUtDialogResultReceptor の選択・ルーティング機能を提供する。UtDialogHostManager を使うと、上の基本形は、次のように書ける。

1. Activity を UtDialogHostManagerにクラス委譲してIUtDialogHostを継承する。

    ```Kotlin
    class MainActivity private constractor(private val dialogHostManager:UtDialogHostManager) 
        : AppCompatActivity(), IUtDialogHos by dialogHostManager {
        constructor() : this(UtDialogHostManager())
        ...

    ```
    カッコいいから委譲してみた。dialogHostManagerを普通のメンバーにして、素直にIUtDialogHostを実装してもよい。


2. Activityのメンバーとして、ダイアログからの結果を受け取る、Receptor を作成

    ```Kotlin
    class MainActivity ... {
        ...
        private val yesNoReceptor = dialogHostManager.register<UtMessageBox>("yesNoReceptor") {
            if(it.dialog.status.ok) {
                doAction()
            } else {
                dontAction()
            }
        }
    ```
    このReceptorは、dialogHostManager内に保持され、dialogHostManagerによってルーティングされる。
    register()の引数 "yesNoReceptor" は、このダイアログのタグ。

    #### 注意：
    receptorは必ず、コンストラクタで生成されるように記述する。当面不要だからといって、by lazy を使うと、ViewModelが再構築されたあと、一度もそのプロパティを参照していなければ、UtDialogHostManagerへの登録が行われないため、ダイアログからReceptorに辿り着けず、エラーになる。また、プロパティ名をタグ名にする、というあたりから、委譲プロパティが使えそう！を思うだろう。私自身、そう思って実装してみたのだが、lazy以外の委譲プロパティも、実際の参照があるまで初期化されないため、うまくいかなかった。

    

3. ダイアログを表示する。
   
    ```Kotlin
    // なんかのボタンがクリックされた
    private fun onSomeButtonClick(view:View?) {
        yesNoReceptor.showDialog(this) {
            // ここにダイアログインスタンスを生成するコードを書く
            UtMessageBox.createForYesNo("ボタンが押されました", "続けますか？")
        }
    }
    ```
    ダイアログは、Receptorの showDialog()メソッドを使って表示する。showDialog()の第１引数には、ダイアログの親となるActivityまたはFragmentを渡し、第２引数　(lambda)で、表示するダイアログを構築して返す。

## 【２】UtDialogHostManager を階層化し、ダイアログの結果をViewModelで受け取る

UtDialogHostManagerは、Activity （またはFragment)を起点とするが、その処理の一部、または、すべてを、他のオブジェクトに持たせてもよい。以下の例では、ActivityとViewModelに処理を分散する。

1. ViewModelを作成して、IUtDialogHostをUtDialogHostManagerに委譲する

    ```Kotlin
    class MainViewModel private constructor(val dialogHostManager:UtDialogHostManager) 
    : ViewModel(), IUtDialogHost by dialogHostManager {
        constructor():this(UtDialogHostManager())
    }
    ```
2. Activity を UtDialogHostManagerにクラス委譲してIUtDialogHostを継承する。

    ```Kotlin
    class MainActivity private constractor(private val dialogHostManager:UtDialogHostManager) 
        : AppCompatActivity(), IUtDialogHos by dialogHostManager {
        constructor() : this(UtDialogHostManager())
        ...

    ```

3. Activity の onCreate/onDestroy で、ActivityとViewModelのdialogHostManagerのチェインを構築する。

    ```Kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        dialogHostManager.addChildHost(viewModel)
        ...
    }
    override fun onDestroy() {
        ...
        dialogHostManager.removeChildHost(viewModel)
        ...
    }

    ```

4. Activity, ViewModelにそれぞれのダイアログの結果を受け取る Receptorを作成する。
   
    `MainActivity.kt`
    ```Kotlin
    class MainActivity ... {
        ...
        val yesNoReceptor = dialogHostManager.register<UtMessageBox>("yesNoReceptor") {
            if(it.dialog.status.ok) {
                doAction()
            } else {
                dontAction()
            }
        }
    ```
    `MainViewModel.kt`
    ```Kotlin
    class MainViewModel ... {
        ...
        val anotherDialogReceptor = dialogHostManager.register<UtMessageBox>("anotherDialogReceptor") {
            if(it.dialog.status.ok) {
                ...
            }
        }
        
    ```

5. ダイアログを表示する。
   
    ```Kotlin
    // なんかのボタンがクリックされた
    private fun onSomeButtonClick(view:View?) {
        yesNoReceptor.showDialog(this) {
            UtMessageBox.createForYesNo("ボタンが押されました", "続けますか？")
        }
    }
    // なんか別のボタンがクリックされた
    private fun onAnotherButtonClick(view:View?) {
        viewModel.anotherDialogReceptor.showDialog(this) {
            AnotherDialog()
        }
    }
    ```
## 【３】サブダイアログの使用

ダイアログ（１）から、さらにサブダイアログ（２）を開いたとき、サブダイアログ（２）の結果は、ダイアログ（１）で受け取りたい場合が多いと思う。この場合、サブダイアログにUtDialogHostManagerフィールドを持たせて、親(Activityなど)のUtDialogHostManagerにaddChildHost()してDialogHostをチェインするのが基本だが、単に、サブダイアログの結果を親ダイアログで受け取りたいだけなら、簡単に実現できる。

IUtDialog は、ダイアログの結果通知先を、次の優先順序で検索・選択する。
1. 起動元の [ImmortalTask](./task.md)
2. 親Fragment (IUtDialogHostの場合)
3. AttachされているActivity (IUtDialogHostの場合)

したがって、ダイアログ（１）が IUtDialogHost を実装するだけで、Activityや、他のFragmentよりも優先して、これが呼ばれるので、UtDialogHostManagerのチェーンを構成しなくても、子ダイアログの結果が取得できる。


