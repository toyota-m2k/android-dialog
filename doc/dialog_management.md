
# IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには

## 【１】基本形

Activityからダイアログを表示して、Activity で結果を受け取るだけの、最小限の実装。


1. Activity または Fragment で IUtDialogHost と IUtDialogResultReceptor を継承する。
    ```Kotlin
    class MainActivity : AppCompatActivity(), IUtDialogHost,IUtDialogResultReceptor {

    ```

2. ダイアログを特定するタグ（文字列定数）を決める。
    ```Kotlin
    companion object {
        const val YES_NO_DIALOG_TAG = "YesNoDialog",
    }
    ```


3. IUtDialogHost.queryDialogResultReceptor()をオーバーライドする。

    ```Kotlin
    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return when(tag) {
            YES_NO_DIALOG_TAG -> this   // 自分が処理するタグなら this (IUtDialogResultReceptor)を返す
            else -> null                // 知らないタグならnullを返す。
        }
    }
    ```

4. onDialogResult.onDialogResult()をオーバーライドする。

    ```Kotlin
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
    ```

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

ダイアログの数が増え、それぞれの機能や目的毎に、結果の受け取りを FragmentやDialog,　ViewModelに分散させたい場合、「基本形」の方法では実装が困難になる。
このよう場合を想定し、
UtDialogHostManager は、複数の IUtDialogHost を階層化し、タグの生成と、それによるIUtDialogResultReceptor の選択・ルーティング機能を提供する。

UtDialogHostManagerは、ダイアログから結果を受け取る、Activity, Fragment, ViewModel のどれに持たせてもよいし、それぞれに持たせてもよい。以下の例では、ViewModelにUtDialogHostManagerを持たせる場合について説明する。


1. ViewModelを作成して、UtDialogHostManagerのフィールドを追加する。

    ```Kotlin
    class MainViewModel : ViewModel() {
        val dialogHostManager = UtDialogHostManager()
    }
    ```

2. Activity または Fragment で IUtDialogHost を継承する。

    ```Kotlin
    class MainActivity : AppCompatActivity(), IUtDialogHost {
    ```

3. IUtDialogHost.queryDialogResultReceptor()をオーバーライドする。

    ```Kotlin
    lateinit viewModel:MainViewModel    // onCreate()で初期化する
    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        // viewModel.dialogHostManager に丸投げ
        return viewModel.dialogHostManager.queryDialogResultReceptor(tag)
    }
    ```

4. ViewModelにダイアログの結果を受け取る Receptorを作成する。
   
    ```Kotlin
    val yesNoReceptor = dialogHostManager.register<UtMessageBox>("yesNoReceptor") {
        if(it.dialog.status.ok) {
            doAction()
        } else {
            dontAction()
        }
    }
    ```

    注意：
    receptorは必ず、コンストラクタで生成されるように記述する。当面不要だからといって、by lazy を使うと、ViewModelが再構築されたあと、一度もそのプロパティを参照していなければ、UtDialogHostManagerへの登録が行われないため、ダイアログからReceptorに辿り着けず、エラーになる。また、プロパティ名をタグ名にする、というあたりから、委譲プロパティが使えそう！を思うだろう。私自身、そう思って実装してみたのだが、やはり参照された時点で


5. ダイアログを表示する。
   
    ```Kotlin
    // なんかのボタンがクリックされた
    private fun onSomeButtonClick(view:View?) {
        viewModel.yesNoReceptor.showDialog(this) {
            UtMessageBox.createForYesNo("ボタンが押されました", "続けますか？")
        }
    }
    ```
## 【３】サブダイアログの使用

ダイアログ（１）から、さらにサブダイアログ（２）を開いたとき、サブダイアログ（２）の結果は、ダイアログ（１）で受け取りたい場合が多いと思う。この場合、サブダイアログにUtDialogHostManagerを、親のそれにaddChildHost()してDialogHostをチェインするのが基本だが、単に、サブダイアログの結果を親ダイアログで受け取りたいだけなら、もっと簡単に実現できる。

IUtDialog は、ダイアログの結果通知先を、次の優先順序で検索・選択する。
1. 起動元の [ImmortalTask](./task.md)
2. 親Fragment (IUtDialogHostの場合)
3. AttachされているActivity (IUtDialogHostの場合)

したがって、ダイアログ（１）が IUtDialogHost を実装するだけで、Activityや、他のFragmentよりも優先して、これが呼ばれるので、UtDialogHostManagerのチェーンを構成しなくても、子ダイアログの結果は取得できる。


