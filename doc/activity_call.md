# Activity を呼び出して結果を受け取る

「UIを表示して（ユーザーの操作を待って）結果を受け取る」という目的において、ダイアログとAcitivity呼び出し（startActivityForResut/onActivityResult）は共通しており、それぞれがライフサイクルを持つため、BundleやIntentなどシリアライズ可能なオブジェクトを介してデータを受け渡す点も同じである。決定的に異なるのは、ダイアログは、自身のアプリ内、つまり同じメモリ空間に属するオブジェクトなのに対して、Activityは、他のアプリ、他のメモリ空間に属している可能性がある点である。[IUtDialog](./dialog_management.md) の実装では、ダイアログにBundleでデータを渡すが、その結果は（メモリ上の）IUtDialogインスタンスから直接取り出せるので、呼び出したActivity/Fragmentはもちろん、[ImmortalTask](./task.md)でも結果を受け取れた。

これに対して、Activity呼び出しの場合は、**結果を受け取れるのは呼び出したActivity(or Fragment)に限られる**。このため、Activity/Fragmentで結果を受け取る方法は、IUtDialogの場合と、ほとんど同じ（というより、IUtDialogの仕掛けをstartActivcityForResultに寄せた、というべきか）だが、ImmortalTask から呼び出す場合は、Activity経由で結果を受け取る必要があり、そのため、Activity、ImmortalTask双方に準備・仕掛けが必要となる。

UtActivityConnector は、AndroidX のActivity/Fragment で導入された、registerForActivityResult() の単純なラッパーだが、これを UtActivityConnectorStore などと組み合わせることで、ImmortalTask から Activity を呼び出して結果を受け取るまでの一連のフローを実現する。
また、UtActivityConnector は、呼び出すActivity（Intent.action）毎に継承クラスを実装し、処理内容(callback)毎にインスタンス化するので、あらかじめ利用するActivity用のUtActivityConnector 継承クラスを実装しておけば、再利用が容易になる。

以下、Activity を呼び出して結果を受け取る方法について、Activity(/Fragment)から実行する場合と、ImmortalTask から実行する場合に分けて説明する。

## ■Activity(/Fragment)から別のActivityを呼び出して結果を受け取る

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


## 1. UtActivityConnectorを継承するConnectorクラスを作成


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

## 2. Activity / Fragment のメンバとして ActivityConnectorをインスタンス化

    通常は、コンストラクタでConnectorインスタンスを作成する。
    onStart, onCreate/onCreateView でもよいが、onResume では遅すぎる。

```Kotlin
class SomeActivity : FragmentActivity {
    val fileOpenPicker = FileOpenPicker(this.toDialogOwner(), "application/pdf") { uri->
        if(uri!=null) {
            // uriを使ってファイルにアクセス
        }
    }
    ...
}
```

## 3. Connectorを呼び出す

上記 2. で作成したConnector は任意のメソッドから呼び出せる。例えば、R.id.open_file ボタンクリックでこれを呼び出す場合は次の通り。

```Kotlin
override fun onCreate() {
    ...
    findViewById<Button>(R.id.open_file).setOnClickListener {
        fileOpenPicker.launch()
    }
}

```

ここで、次のコードの用に、fileOpenPickerを呼び出すタイミングで作成した方が、美しいと思うかもしれないがこれは間違い！

```Kotlin

lateinit var fileOpenPicker:FileOpenPicker
override fun onCreate() {
    ...
    findViewById<Button>(R.id.open_file).setOnClickListener {
        // この書き方だと、ボタンクリックされたときにしかfileOpenPickerが登録されず、
        // FilePickerのActivityから戻ってきたときのActivityでは未登録となり、
        // 結果を受け取ることができない。
        fileOpenPicker = FileOpenPicker(this.toDialogOwner(), "application/pdf").apply {
            launch()
        }
    }
}

```

------

## ■バックグラウンドスレッド（Coroutine）から別のActivityを呼び出して結果を受け取る

UtImmortalTask + UtMortalActivity で、この機能を実現する。
考え方は、[IUtDialog の場合](./task.md) と同じだが、UtActivityConnector (IUtDialogのIUtDialogResultReceptorに相当)を必ず、Activity/Fragment 側で用意しておく必要がある点が異なる。そのため、ImmortalTask から (Activity/Fragment内の) ActivityConnector を見つけ出すための仕掛けを追加している。

- IUtActivityConnectorStore
- UtActivityConnectorStore
- UtActivityConnectorFactoryBank

## 1. UtActivityConnectorImmortalTaskBase を派生するタスククラスを準備する。

UtActivityConnectorImmortalTaskBase を継承したクラスを作成し、execute()をオーバーライドし、UtActivityConnectorImmortalTaskBaseが提供する　launchActivityConnector メソッド、

```Kotlin
/**
 * ImmortalTask内で、名前で指定したコネクタのlaunch（引数なし）を実行する。
 * @param connectorName ActivityConnectorの名前
 * @return 外部アクティビティからの戻り値
 */
protected suspend inline fun <reified O> launchActivityConnector(connectorName: String) : O?
/**
 * ImmortalTask内で、名前で指定したコネクタのlaunch（引数あり）を実行する。
 * @param connectorName ActivityConnectorの名前
 * @param arg launchに渡す引数
 * @return 外部アクティビティからの戻り値
 */
protected suspend inline fun <I, reified O> launchActivityConnector(connectorName: String, arg:I) : O?
```

を使って、Activity を起動し、結果を受け取る。

```Kotlin
class SampleTask : UtActivityConnectorImmortalTaskBase(TASK_NAME) {
    companion object {
        const val TASK_NAME = "FileTestTask"
        const val OPEN_FILE_CONNECTOR_NAME = "OpenFileConnector"
    }

    override suspend fun execute(): Boolean {
        val uri = launchActivityConnector<Uri>(OPEN_FILE_CONNECTOR) ?: return false
        withOwner { owner->
            owner.asContext().contentResolver.openInputStream(uri)?.use { stream->
                ...
            }
        }
        return true
    }
}
```
このように、ImmortalTaskからは、Activityを呼び出して結果を受け取る、という一連のフローを「同期的」に実装できるのだ！！

尚、UtActivityConnectorImmortalTaskBaseは、[UtImmortalTaskBase](./task.md) を継承しているので、IUtDialog との強調動作も可能。

## 2. UtActivityConnector を使用するActivityで IUtActivityConnectorStore を継承する

ImmortalTask との強調動作に必要な処理を隠蔽するため、Activityは、UtMortalActivity の派生クラスとすることを推奨。

```Kotlin
class SomeActivity : UtMortalActivity,  IUtActivityConnectorStore {
    ...
}
```

## 2. UtActivityConnectorFactoryBank の準備

UtActivityConnectorFactoryBank は、アクティビティが再作成される場合に、そのアクティビティが使用する ActivityConnector を再生成するための情報を保持しておくクラスである。このため、クラスインスタンスではなく、companion object などでインスタンス化しておく。

```Kotlin
class SomeActivity : UtMortalActivity, IUtActivityConnectorStore {
    companion object {
        val activityConnectorFactoryBank = UtActivityConnectorFactoryBank(
            arrayOf(
                UtFileOpenPicker.Factory(SomeTask.TASK_NAME, SomeTask.OPEN_FILE_CONNECTOR_NAME, arrayOf("text/*")),
                UtDirectoryPicker.Factory(OtherTask.TASK_NAME, OtherTask.OPEN_DIRECTORY_CONNECTOR_NANE, null),
            ))
    }
    ...
}
```

## 3. UtActivityConnectorStore のインスタンス化

Activityのコンストラクタで、UtActivcityConnectorFactoryBank.createConnectorStore()を呼び出して、UtActivityConnectorStore インスタンスを作成し、IUtActivityConnectorStore #getActivityConnector()を実装する。

```Kotlin
class SomeActivity : UtMortalActivity, IUtActivityConnectorStore {
    ...
    private val activityConnectorStore = activityConnectorFactoryBank.createConnectorStore(this.toDialogOwner())

    override fun getActivityConnector(immortalTaskName: String,connectorName: String): UtActivityConnector<*, *>? {
        return activityConnectorStore.getActivityConnector(immortalTaskName,connectorName)
    }
    ...
}
```

## 4. Activityのコンストラクタで、ActivityConnector をインスタンス化

```Kotlin
class SomeActivity : UtMortalActivity, IUtActivityConnectorStore {
    ...
    val fileOpenPicker = FileOpenPicker(this.toDialogOwner(), "application/pdf") { uri->
        if(uri!=null) {
            // uriを使ってファイルにアクセス
        }
    }
    ...
}
```
