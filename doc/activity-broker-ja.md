# Activity Broker

## Activity呼び出しの問題点と UtDialogライブラリによる解決

Android では、FilePicker や ランタイムでの Permission の要求画面などは、外部アプリの Activity として提供されます。写真や動画を撮影する場合に、自前のカメラ機能を用意することなく、外部のActivity（アプリ）に処理を委託することもあります。

しかし、Activity を呼び出して結果を受け取る処理は、どうやっても面倒な実装が必要です。
例えば、FilePickerを使って、画像ファイルを１つ選択させる実装は、次のようになります。

```kotlin
class MainActivity : AppCompatActivity() {
    private val launcher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        findViewById<ImageView>(R.id.image_view).setImageURI(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            launcher.launch("image/*")
        }
    }
}
```

問題点は大きく２つあります。
- ファイルピッカーを起動する箇所（`launcher.launch("image/*")`）と、ファイルピッカーからファイル（Uri）を受け取って処理する箇所が泣き別れてしまっています。特に、launcher を ViewModelなど、Activityの外部から呼び出しても、その結果は、Activity でしか処理できず、ビジネスロジックとビューを分離できません。

- 受け取ったファイルを処理するコードが、launcher 内に実装されているため、同じピッカーを使う場合でも、処理内容毎に launcher を用意するか、launcher内で分岐させる必要があり、ますますコードが汚くなります。

こんなとき、UtDialogライブラリの `UtActivityBroker` を使えば、上のコードは、次のように書くことができます。

```kotlin
class MainActivity : UtMoralActivity() {
    val filePicker = UtOpenReadOnlyFilePicker().apply { register(this@MainActivity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            UtImmortalTask.launchTask {
                val uri = filePicker.selectFile()
                findViewById<ImageView>(R.id.image_view).setImageURI(uri)
                true
            }
        }
    }
}
```

`UtImmortalTask` のスコープから呼び出す限り、ViewModel のコマンドハンドラや、UtDialog からも filePicker が使えます。

例：ViewModel から呼び出す

```kotlin
class MainActivityViewModel : ViewModel() {
    val imageUrl = MutableStateFlow<Uri?>(null)
    val commandSelectFile = LiteUnitCommand {
        launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as MainActivity
                imageUri.value = activity.filePicker.selectFile("image/*")
            }
        }
    }
}
```

## ビルトイン UtActivityBroker

### (1) UtOpenReadOnlyFilePicker

読み出し用にファイルを１つ選択します。
```kotlin
suspend fun selectFile(mimeType:String = defaultMimeType): Uri?
```
|   |説明|
|---|---|
|引数|mimeType（デフォルト: `"*/*"`）|
|戻り値| 選択されたファイルのUri, キャンセルされたら null|

### (2) UtOpenReadOnlyMultiFile

読み出し用にファイルを複数選択します。
```kotlin
suspend fun selectFiles(mimeType:String = defaultMimeType): List<Uri>
```
|   |説明|
|---|---|
|引数|mimeType（デフォルト: `"*/*"`）|
|戻り値|選択されたファイルのUriのリスト, キャンセルされたら emptyList|

### (3) UtOpenFilePicker

読み書き用にファイルを１つ選択します。

```kotlin
suspend fun selectFile(mimeTypes:Array<String> = defaultMimeTypes):Uri?
```
|   |説明|
|---|---|
|引数|mimeTypeの配列（デフォルト: `arrayOf("*/*")`）|
|戻り値|選択されたファイルのUri, キャンセルされたら null|

### (4) UtOpenMultiFilePicker

読み書き用にファイルを複数選択します。

```kotlin
    suspend fun selectFiles(mimeTypes:Array<String> = defaultMimeTypes): List<Uri>
```
|   |説明|
|---|---|
|引数|mimeTypeの配列（デフォルト: `arrayOf("*/*")`）|
|戻り値|選択されたファイルのUriのリスト, キャンセルされたら emptyList|

### (5) UtCreateFilePicker
```kotlin
suspend fun selectFile(initialFileName:String, mimeType:String? = null):Uri?
```

作成するファイルを選択します。「名前を付けて保存」に相当します。

|   |説明|
|---|---|
|引数|initialFileName　初期ファイル名|
||mimeType（デフォルト: null）|
|戻り値|選択されたファイルのUri, キャンセルされたら null|


### (6) UtDirectoryPicker

```kotlin
suspend fun selectDirectory(initialPath:Uri?=null):Uri?
```

ディレクトリを選択します。

|   |説明|
|---|---|
|引数|initialPath 初期選択するパス名（デフォルト：null）|
|戻り値|ディレクトリのUri。このUriを使い、`DocumentFile.fromTreeUri(context, uri)` によって、ディレクトリの DocumentFile インスタンスが取得できる。|

### (7) UtPermissionBroker

```kotlin
fun isPermitted(permission: String):Boolean
```

指定された permission は許可されている(PERMISSION_GRANTED)かどうかを確認します。

|   |説明|
|---|---|
|引数|permission パーミッションの名前（android.Manifest.permission.CAMERA など）|
|戻り値|true: 許可されている（PERMISSION_GRANTED）/ false: 許可されていない|

```kotlin
suspend fun requestPermission(permission:String):Boolean {
```

指定された permission を要求します。

|   |説明|
|---|---|
|引数|permission パーミッションの名前（android.Manifest.permission.CAMERA など）|
|戻り値|true: 許可された（PERMISSION_GRANTED）/ false: 許可されなかった|

### (7) UtMultiPermissionsBroker

複数のパーミッションを一括要求します。
permissionsBroker.Request() でリクエストビルダーを取得し、要求するパーミッションを add() して、execute() を呼び出します。addIf() は、条件付きでパーミッションを要求します。次の例では、CAMERA, RECORD_AUDIO パーミッションを要求するとともに、Android 10 以前なら、WRITE_EXTERNAL_STORAGE も要求します。

```kotlin
if (permissionsBroker.Request()
        .add(Manifest.permission.CAMERA)
        .add(Manifest.permission.RECORD_AUDIO)
        .addIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .execute()) {
    // granted all
}
```

## カスタム ActivityBroker

上記以外のActivity呼び出しも、UtActivityBroker を派生したブローカークラスを作り、ActivityResultContract を派生したコントラクトを実装すれば、ビルトインブローカーと同様に利用できます。

[CameraBroker](../sample/src/main/java/io/github/toyota32k/dialog/sample/broker/CameraBroker.kt) は、暗黙的Intentを使ってカメラアプリのActivityを起動し、写真や動画を取得する完全なActivityBroker の実装例です。尚、その内部では（次章で説明する） `UtActivityBrokerStore` を経由して、UtPermissionBroker インスタンスを取得してカメラやマイクのパーミッションを要求しています。ActivityBroker を使うことで、Activity呼び出しを含む完全なフローが直感的に記述できることがわかると思います。

## UtActivityBrokerStore と IUtActivityBrokerStoreProvider

`UtActivityBrokerStore` は、ビルトインブローカーをはじめ、任意の `UtActivityBroker` を登録・保持しておくためのコンテナです。 `IUtActivityBrokerStoreProvider` は、オブジェクト（主にActivity）が、 `UtActivityBrokerStore` を持っていることを示すためのインターフェースです。

これまで述べた通り、UtActivityBroker は、ViewModel や UtDialog など、どこからでも呼び出すことができますが、UtActivityBroker インスタンス自体は、Activity に実装しなければなりません。複数のActivityで、これらを使う場合、各Activityに UtActivityBroker のインスタンス作成やメンバーとして公開するコードを実装する必要があります。この煩わしい作業を一般化したのが、UtActivityBrokerStore です。例えば、UtOpenFilePicker と、UtCreateFilePicker を使うなら、Activity で次のようにフィールドを定義します。

```kotlin
class SomeActivity : UtMortalActivity() {
    val activityBrokers = UtActivityBrokerStore(this, 
                            UtOpenFilePicker(), 
                            UtCreateFilePicker())
}
```
これで、`activityBrokers.openFilePicker.selectFile()` や、`activityBrokers.createFilePicker.selectFile()` が使えるようになります。
ただ、このままでは、Acticity外のモジュールから、activityBroker を使いたいとき、SomeActivity が activityBroker フィールドを持っていることを知っていて、SomeActivity にキャストして使う必要があります。

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activity = owner.asActivity() as? SomeActivity
                if(activity!=null) {
                    val url = activity.activityBrokers.openFilePicker.selectFile()
                    if(url!=null) {
                        ...
                    }
                }
            }
        }
    }
}
```
このコードでも問題なく動作しますが、
せっかく分離したビューモデルが SomeActivity に依存してしまい、エレガントではありません。

そこで、SomeActivity に、IUtActivityBrokerStoreProvider インターフェースを追加し、activityBroker を持っていることを抽象化します。

```kotlin
class SomeActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    override val activityBrokers = UtActivityBrokerStore(this, 
                            UtOpenFilePicker(), 
                            UtCreateFilePicker())
}
```

これで、OtherViewModel は次のように書け、SomeActivityへの依存を解消できました。

```kotlin
class OtherViewModel : ViewModel() {
    val command = LiteUnitCommand {
        UtImmortalTask.launchTask {
            withOwner { owner->
                val activityBrokers = owner.asActivityBrokerStoreOrNull()
                if(activityBrokers!=null) {
                    val url = activityBrokers.openFilePicker.selectFile()
                    if(url!=null) {
                        ...
                    }
                }
            }
        }
    }
}
```

