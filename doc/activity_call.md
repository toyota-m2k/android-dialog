# Activity を呼び出して結果を受け取る

「UIを表示して（ユーザーの操作を待って）結果を受け取る」という目的において、ダイアログとAcitivity呼び出し（startActivityForResut/onActivityResult）は共通しており、それぞれがライフサイクルを持つため、BundleやIntentなどシリアライズ可能なオブジェクトを介してデータを受け渡す点も同じである。決定的に異なるのは、ダイアログは、自身のアプリ内、つまり同じメモリ空間に属するオブジェクトなのに対して、Activityは、他のアプリ、他のメモリ空間に属している可能性がある点である。[IUtDialog](./dialog_management.md) の実装では、ダイアログにBundleでデータを渡すが、その結果は（メモリ上の）IUtDialogインスタンスから直接取り出せるので、呼び出したActivity/Fragmentはもちろん、[ImmortalTask](./task.md)でも結果を受け取れた。

これに対して、Activity呼び出しの場合は、**結果を受け取れるのは呼び出したActivity(or Fragment)に限られる**。このため、Activity/Fragmentで結果を受け取る方法は、IUtDialogの場合と、ほとんど同じ形式にできる（というより、IUtDialogの仕掛けをstartActivcityForResultに寄せた、というべきか）が、ImmortalTask から呼び出す場合は、Activity経由で結果を受け取る必要があり、そのため、Activity、ImmortalTask双方に準備・仕掛けが必要となる。

UtActivityConnector は、AndroidX のActivity/Fragment で導入された、registerForActivityResult() の単純なラッパーだが、これを UtActivityConnectorStore などと組み合わせることで、ImmortalTask から Activity を呼び出して結果を受け取るまでの一連のフローを実現する。
また、UtActivityConnector は、呼び出すActivity（Intent.action）毎に継承クラスを実装し、処理内容(callback)毎にインスタンス化するので、あらかじめ利用するActivity用のUtActivityConnector 継承クラスを実装しておけば、再利用が容易になる。

以下、Activity を呼び出して結果を受け取る方法について、Activity(/Fragment)から実行する場合と、ImmortalTask から実行する場合に分けて説明する。

## Activity(/Fragment)から別のActivityを呼び出して結果を受け取る

前述の通り、この場合は、AndroidX のActivity/Fragment で導入された、registerForActivityResult() を使う実装そのものなので、あえて UtActivityConnector を使う必要はないが、Activity呼び出しを含む機能をAPI化する場合は、この手順に従い、UtActivityConnectorを実装する。

尚、本ライブラリでは、ファイルピッカーとして、以下のような UtActivityConnector を実装していおり、これらを使うだけなら、手順２へ進む。

- **UtFileOpenPicker**

    ACTION_OPEN_DOCUMENT<br>
    編集（読み書き）用にファイルを選択する。

- **UtMultiFileOpenPicker**

    ACTION_OPEN_DOCUMENT / EXTRA_ALLOW_MULTIPLE=true<br>
    編集（読み書き）用にファイルを複数選択する。

- **UtContentPicker**
    
    ACTION_GET_CONTENT<br>
    インポート（読み取り）用にファイルを選択する。
    
- **UtMultiContentPicker**

    ACTION_GET_CONTENT / EXTRA_ALLOW_MULTIPLE=true<br>
    インポート（読み取り）用にファイルを複数選択する。

- **UtFileCreatePicker**

    ACTION_CREATE_DOCUMENT<br>
    作成するファイルを指定する。既存ファイルを選択すると、「上書きするか」どうか確認される。

- **UtDirectoryPicker**

    ACTION_OPEN_DOCUMENT_TREE<br>
    読み書き用にディレクトリのURLを取得する。取得したURIは、DocumentFile.fromTreeUri()でDocumentFileを作成することにより、そのディレクトリ下のファイルを好きなようにできる。


### 1. UtActivityConnectorを継承するConnectorクラスを作成


以下、UtFileOpenPicker を例にして説明（ただし、説明用にミニマム構成となるよう改変）。

```Kotlin
class UtFileOpenPicker(
    owner: UtDialogOwner,                   // activity|fragment
    mimeTypes: Array<String>,               // launch()で引数省略時のデフォルトパラメータ
    callback: ActivityResultCallback<Uri>)  // 結果を受け取ったときの処理
    : UtActivityConnector<Array<String>, Uri>(
        owner.registerForActivityResult(ActivityResultContracts.OpenDocument(), callback),
        mimeTypes) {

    // ファクトリの実装 (ImmortalTaskからの呼び出しでのみ使用)
    class Factory(immortalTaskName: String, connectorName:String, defArg:Array<String>)
        : UtActivityConnectorFactoryBank.ActivityConnectorFactory<Array<String>, Uri>(
        UtActivityConnectorKey(immortalTaskName,connectorName), defArg) {
        override fun createActivityConnector(owner: UtDialogOwner): UtActivityConnector<Array<String>, Uri> {
            return UtFileOpenPicker(owner, defArg, ImmortalResultCallback(key.immortalTaskName))
        }
    }
}
```
UtActivityConnector 仮想クラスのコンストラクタの第１引数は、ActivityResultContract型であり、UtActivityConnector&lt;I,O> generics の I, O は、ActivityResultContract&lt;I,O> の I,O の型と一致する。第２引数には、すなわち、ActivityResultLauncher&lt;I>.launch() に渡すI型のパラメータを渡す。これにより、共通する launch()の引数を省略可能としているが、呼び出し時にパラメータを指定することで、このパラメータを上書きできる。

実装した UtActivityConnector をImmortalTask から利用できるようにするには、ActivityConnectorFactory を継承したファクトリクラスを作成する必要がある。ActivityConnectorFactoryを実装する場合は、createActivityConnector()メソッドをオーバーライドし、UtActivityConnector継承クラスのインスタンスを返すようにするが、このとき、callback引数には、ImmortalTask専用の UtActivityConnector.ImmortalResultCallback インスタンスを渡すこと。

### 2. Activity / Fragment のコンストラクタで Connectorクラスをインスタンス化

        
