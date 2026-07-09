# チュートリアル（応用編）- サブダイアログと外部Activity連携

<div align="right">
<a href="./tutorial-subdialog.md">EN</a> | JA
</div>

[基本編](./tutorial-basic-ja.md) では、ダイアログの中からメッセージボックスを表示するところまで説明しました。同様の方法で、ダイアログから別のダイアログ（サブダイアログ）を開くことも、ファイルピッカーなどの外部アプリを呼び出して結果をダイアログに反映することもできます。

このチュートリアルでは、次の２つの操作で文字列をリスト (RecyclerView) に追加していくダイアログ (`NestedDialog`) を作成します。

- サブダイアログ（基本編で作った CompactDialog）を開いてテキストを入力する
- ファイルピッカーを開いて、選択されたファイルの名前を取得する

コード例は、[sampleモジュール](../sample) の [NestedDialog.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/NestedDialog.kt) から抜粋しています。

## (1) レイアウトの作成

テキスト入力用サブダイアログを開くボタンと、ファイルピッカーを開くボタン、および、リスト表示用の RecyclerView を持つレイアウトを作成します。

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <Button
        android:id="@+id/add_text_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        android:text="@string/button_add_text"
        />
    <Button
        android:id="@+id/add_file_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="10dp"
        app:layout_constraintStart_toEndOf="@+id/add_text_button"
        app:layout_constraintTop_toTopOf="parent"
        android:text="@string/button_add_file"
        />
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@+id/add_text_button"
        app:layout_constraintBottom_toBottomOf="parent"
        android:background="?attr/colorSurface"
        />
</androidx.constraintlayout.widget.ConstraintLayout>
```

## (2) ViewModel の作成

ViewModel には、リストに表示する文字列を保持する `ObservableList`（[android-binding](https://github.com/toyota-m2k/android-binding)）と、テキスト追加コマンド、ファイル選択コマンドを実装します。

```kotlin
class NestedDialogViewModel : UtDialogViewModel() {
    val observableList = ObservableList<String>()

    // サブダイアログ（CompactDialog）を開いてテキストを追加
    val commandAddText = LiteUnitCommand {
        launchSubTask {
            val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
            if (showDialog(CompactDialog()).status.ok) {
                observableList.add(vm.yourName.value)
            }
        }
    }

    // ファイルピッカーを開いてファイル名を追加
    val commandAddFile = LiteUnitCommand {
        launchSubTask {
            withOwner { owner->
                val activityBrokers = owner.asActivityBrokerStore()
                val uri = activityBrokers.openReadOnlyFilePicker.selectFile()
                if (uri != null) {
                    observableList.add(getFileName(owner.asContext(), uri))
                }
            }
        }
    }

    private fun getFileName(context:Context, uri:Uri):String {
        return when(uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).name }
            ContentResolver.SCHEME_CONTENT -> context.contentResolver.query(uri,null,null,null,null,null)?.use { cursor ->
                cursor.moveToFirst().letOnTrue {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        cursor.getString(idx)
                    } else null
                }
            }
            else -> null
        } ?: "unknown file"
    }
}
```

### サブダイアログを開く (commandAddText)

`launchSubTask {...}` の中身に注目してください。`createViewModel()` で ViewModel を作って `showDialog()` で表示して結果を受け取る、という流れは、[基本編](./tutorial-basic-ja.md) で Activity の ViewModel から CompactDialog を開いたコードとまったく同じです。呼び出し元が Activity かダイアログかによる違いはありません。

`launchSubTask()` は、この ViewModel を構築した UtImmortalTask の上にサブタスクのスコープを作成するメソッドです。`UtImmortalTask.launchTask()` で独立したタスクを起動しても同様に動作しますが、実行中のタスクと名前が衝突しないように管理する必要があります。ViewModel 内からタスクを起動するときは launchSubTask() を使ってください（詳細は [UtImmortalTask 詳説](./immortal-task-ja.md#サブタスク)）。

### ファイルピッカーを開く (commandAddFile)

ファイルピッカーのような「外部Activityを起動して結果を受け取る」処理には、[Activity Broker](./activity-broker-ja.md) を使います。

- `withOwner {...}` で、現在フォアグラウンドにある Activity（の UtDialogOwner ラッパー）を取得します。
- `owner.asActivityBrokerStore()` で、Activity が保持している `UtActivityBrokerStore`（後述）を取得します。
- `openReadOnlyFilePicker.selectFile()` は、ファイルピッカーを起動して、ユーザーがファイルを選択するまでサスペンドし、選択されたファイルの Uri を返します（キャンセルされたら null）。

ファイルピッカーの起動という Activity 依存の処理が、ViewModel の中に、コルーチンの直列なフローとして記述できていることに注目してください。

## (3) ダイアログクラスの作成

サブダイアログを開くからといって、ダイアログクラス側に特別な実装は不要です。コマンドを View にバインドするだけです。

```kotlin
class NestedDialog : UtDialogEx() {
    override fun preCreateBodyView() {
        title = "Nested Dialog"
        heightOption = HeightOption.FULL
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
    }

    lateinit var controls: DialogNestedBinding
    val viewModel by lazy { getViewModel<NestedDialogViewModel>() }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogNestedBinding.inflate(inflater.layoutInflater)
        binder
            .bindCommand(viewModel.commandAddText, controls.addTextButton)
            .bindCommand(viewModel.commandAddFile, controls.addFileButton)
            .recyclerViewBindingEx(controls.recyclerView) {
                list(viewModel.observableList)
                inflate { parent-> ItemStringListBinding.inflate(inflater.layoutInflater, parent, false) }
                bindView { itemControls, itemBinder, _, text->
                    itemBinder.textBinding(this@NestedDialog, itemControls.textView, text.asConstantLiveData())
                }
            }
        return controls.root
    }
}
```

チュートリアルの本題から逸れますが、[android-binding](https://github.com/toyota-m2k/android-binding) の `ObservableList` と `recyclerViewBindingEx` により、RecyclerView（Adapter や ViewHolder）の実装がとてもコンパクトになっています。

## (4) Activity の準備

ViewModel からファイルピッカーを使えるようにするには、呼び出し元の Activity に `UtActivityBrokerStore` を実装しておく必要があります（ActivityResultContract の仕組み上、ピッカーの launcher は Activity の onCreate までに登録される必要があるためです）。

- 呼び出し元の Activity に `IUtActivityBrokerStoreProvider` インターフェースを追加し、
- `activityBrokers` プロパティをオーバーライドして、使用するブローカー（この例では `UtOpenReadOnlyFilePicker`）を登録します。

```kotlin
class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    override val activityBrokers = UtActivityBrokerStore(this, UtOpenReadOnlyFilePicker())
    ...
}
```

これで、`withOwner { owner-> owner.asActivityBrokerStore().openReadOnlyFilePicker }` という形で、どこからでもファイルピッカーが使えるようになります。

用意されているピッカー・パーミッション要求などのブローカー、および、カスタムブローカーの作り方については、[Activity Broker](./activity-broker-ja.md) をご参照ください。

## 関連ドキュメント

- [チュートリアル（基本編）](./tutorial-basic-ja.md)
- [UtImmortalTask 詳説](./immortal-task-ja.md)
- [Activity Broker - ファイルピッカー/パーミッション](./activity-broker-ja.md)
- [UtDialog リファレンス](./reference-ja.md)
