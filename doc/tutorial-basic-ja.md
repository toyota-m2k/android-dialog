# チュートリアル（基本編）- カスタムダイアログの作成と表示

<div align="right">
<a href="./tutorial-basic.md">EN</a> | JA
</div>

このチュートリアルでは、ユーザーに文字列を入力させる簡単なダイアログ (`CompactDialog`) を作成し、MainActivity から表示して、入力された文字列を受け取るまでの手順を説明します。

コード例は、[sampleモジュール](../sample) の [CompactDialog.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/dialog/CompactDialog.kt) / [MainActivity.kt](../sample/src/main/java/io/github/toyota32k/dialog/sample/MainActivity.kt) から抜粋しています。

尚、コード例では、Android 標準の ViewBinding（layout-xml の定義から View インスタンスの参照を自動生成する仕掛け）と、[android-binding](https://github.com/toyota-m2k/android-binding)（View-ViewModel バインディングライブラリ）を使用しています。どちらも必須ではありませんが、ソースがコンパクトに書けるのでお勧めです。

## 全体の流れ

1. [ダイアログのレイアウトを作成](#1-ダイアログのレイアウトを作成)
2. [ダイアログの ViewModel を作成](#2-viewmodel-の作成)
3. [ダイアログクラスを作成](#3-ダイアログクラスの作成)
4. [Activity のレイアウトを用意](#4-activity-のレイアウト)
5. [Activity の ViewModel を作成](#5-mainactivityviewmodel-の作成)
6. [UtImmortalTask でダイアログを表示](#6-utdialog-を表示するための実装)
7. [MainActivity を実装](#7-mainactivity-の実装)

## (1) ダイアログのレイアウトを作成

この例で使用するのは、ラベルと入力欄を１つ持つ簡単なレイアウトです。

**dialog_compact.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <TextView
        android:id="@+id/name_label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="What's your name?"
        />
    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:imeOptions="actionDone"/>
</LinearLayout>
```

## (2) ViewModel の作成

入力項目は１つしかありませんが、デバイス回転時の入力内容保全や、呼び出し元（この例では Activity）との確実なデータ受け渡しのために、ViewModel を使います。UtDialog で使う ViewModel は、`UtDialogViewModel` から派生します。

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
}
```

普通の ViewModel と違うのは、このインスタンスが **Activity ではなく UtImmortalTask のライフサイクルに紐づく**、という点です。だから、ダイアログ表示中に Activity が破棄・再生成されても入力内容は失われず、タスクが終了するまで、呼び出し元とダイアログの双方から同じインスタンスが参照できます。

## (3) ダイアログクラスの作成

`UtDialog`（android-binding を使う場合は `UtDialogEx`）を派生してダイアログクラスを作成します。

```kotlin
class CompactDialog : UtDialogEx() {
    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.LIMIT(400)
        gravityOption = GravityOption.CENTER
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        cancellable = false
        draggable = true
        enableFocusManagement()
            .autoRegister()
            .setInitialFocus(R.id.name_input)
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        return controls.root
    }
}
```

以下、一つずつ説明します。

### ViewModel の取得

ViewModel のインスタンスは、呼び出し元 (UtImmortalTask) で作成したもの（後述）を、`getViewModel()` で取得して利用します。**ViewModel を呼び出し元のスコープで作成する**のが、データ受け渡しのための重要なポイントです。

```kotlin
private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }
```

### ダイアログのプロパティ設定

ダイアログのプロパティは、`preCreateBodyView()` をオーバーライドして設定します。

|プロパティ|説明|
|---|---|
|title|ダイアログのタイトルバーに表示する文字列。|
|heightOption|[ダイアログ高さの指定](./sizing-option-ja.md)。COMPACT は、WRAP_CONTENT に相当します。|
|widthOption|[ダイアログ幅の指定](./sizing-option-ja.md)。LIMIT(400) は、画面幅が 400dp 以下の場合は FULL（MATCH_PARENT）として動作し、それ以上の場合は最大幅を 400dp に制限します。|
|gravityOption|ダイアログの配置方法。CENTER を指定すると画面中央に配置します。|
|leftButtonType|左ボタンに CANCEL ボタンを割り当てます。デフォルトは NONE（表示しない）です。|
|rightButtonType|右ボタンに DONE ボタンを割り当てます。デフォルトは NONE（表示しない）です。|
|cancellable|false を指定すると、ダイアログ外をタップしてもダイアログを閉じません。|
|draggable|true を指定すると、タイトルバーをドラッグしてダイアログを移動できます。|
|enableFocusManagement()|[フォーカス管理](./focus-manager-ja.md)を有効化します。この例では、フォーカス可能なビューを自動登録 (autoRegister) し、名前入力欄に初期フォーカスをセットしています。|

このほかのプロパティについては、[リファレンス](./reference-ja.md) をご参照ください。

### ボディビューの作成

`createBodyView()` をオーバーライドして、ダイアログのボディとなるビューを作成し、必要なイベントリスナーを登録します。

この例では、ビューの作成に ViewBinding.inflate() を使い、イベントリスナーの登録は、`UtDialogEx` の `binder`（[android-binding](https://github.com/toyota-m2k/android-binding)）に隠蔽されています。具体的には、`editTextBinding` で ViewModel の `yourName:MutableStateFlow<String>` と EditText を双方向バインドし、`enableBinding` で、`yourName` が空のときは DONE ボタンを無効化するように構成しています。

ViewBinding を使わない場合は、引数の inflater を使って layout-xml を inflate() してください。ダイアログテーマを正しく反映するため、必ずこの inflater を使う必要があります。また、savedInstanceState は DialogFragment から渡される再構築用の Bundle ですが、UtDialog では状態を ViewModel に持たせるので、ほとんど使いません。

## (4) Activity のレイアウト

ここからは、Activity 側の実装です。次の例では、ダイアログ表示のトリガーとなる Button と、ダイアログの結果を表示するデモ用の TextView を配置しました。

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity"
    android:orientation="vertical"
    >

    <Button
        android:id="@+id/btn_compact_dialog"
        android:text="@string/compact_dialog"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        style="?attr/materialButtonOutlinedStyle"
        />
    <TextView
        android:id="@+id/output_text"
        android:layout_width="match_parent"
        android:layout_height="30dp"
        android:paddingHorizontal="10dp"
        android:paddingVertical="2dp"
        />
</LinearLayout>
```

## (5) MainActivityViewModel の作成

標準の ViewModel を継承して、MainActivityViewModel を作成します。まず、ダイアログの結果の文字列を保持する `MutableStateFlow<String>` 型の outputString を用意します。

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
}
```

Activity 側の ViewModel は、UtImmortalTask とは無関係なので、`UtDialogViewModel` ではなく普通の ViewModel で構いません。

## (6) UtDialog を表示するための実装

いよいよ本題、`CompactDialog` を表示する実装です。`UtImmortalTask.launchTask()` でタスクのスコープを作成し、その中で、

1. `createViewModel()` でダイアログの ViewModel を作成し、
2. `showDialog()` でダイアログを表示します。

showDialog() は、ダイアログが閉じられるまで待機（サスペンド）し、ダイアログのインスタンスを返します。ダイアログがどのように閉じられたかは `IUtDialog#status` で確認できます。

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
    val commandCompactDialog = LiteUnitCommand {
        UtImmortalTask.launchTask {
            val vm = createViewModel<CompactDialogViewModel>()
            if(showDialog(CompactDialog()).status.ok) {
                outputString.value = "Your name is ${vm.yourName.value}."
            } else {
                outputString.value = "Canceled."
            }
        }
    }
}
```

この例では、[android-binding](https://github.com/toyota-m2k/android-binding) の `LiteUnitCommand` を使って、ボタン押下ハンドラごと ViewModel に実装していますが、Activity のどこ（ボタンの OnClickListener の中など）に書いても構いません。

ViewModel の作成とダイアログの表示だけなら、次のように１行で書くこともできます（ViewModel の初期化やダイアログのコンストラクタ引数が不要な場合）。

```kotlin
UtImmortalTask.launchTask {
    showDialog<CompactDialogViewModel, CompactDialog>()
}
```

タスクの起動方法のバリエーション（結果を待つ・戻り値を受け取るなど）については、[UtImmortalTask 詳説](./immortal-task-ja.md) をご参照ください。

## (7) MainActivity の実装

MainActivity は、`UtMortalActivity` を派生して実装します。とはいえ、必要な処理はほとんど MainActivityViewModel に実装済みなので、あとは ViewModel とビューをバインドするだけです。

```kotlin
class MainActivity : UtMortalActivity() {
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainActivityViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        binder
            .owner(this)
            .bindCommand(viewModel.commandCompactDialog, controls.btnCompactDialog)
            .textBinding(controls.outputText, viewModel.outputString)
    }
}
```

以上で、CompactDialog と、それを表示する MainActivity の実装ができました。ダイアログ表示中にデバイスを回転したり、他のアプリに切り替えたりしても、入力内容は保持され、結果は確実に受け取れます。

## 改良：ダイアログを閉じる前に入力値を検証する

ここから、少し改良してみましょう。

現在の実装では、名前が入っていないとき DONE ボタンをグレーアウトしていました。しかし「なぜボタンが押せないのか」がわからないので、状況によってはグレーアウトがユーザーエクスペリエンスを低下させることもあります。そこで、グレーアウトをやめて、名前が空のままボタンが押されたら "Input your name." というメッセージボックスを表示するようにしてみます。

### CompactDialogViewModel の修正

CompactDialogViewModel に、メッセージボックスを表示する showErrorMessage() メソッドを追加します。`UtDialogViewModel.launchSubTask()` でサブタスクを開始して、showConfirmMessageBox() を呼ぶだけです。

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
    fun showErrorMessage() {
        launchSubTask {
            showConfirmMessageBox(null, "Input your name.")
        }
    }
}
```

`launchSubTask()` は、この ViewModel を作成したタスクの上にサブタスクを作成して実行します。`UtImmortalTask.launchTask("sub") {...}` のように独立したタスクを起動しても動作しますが、ダイアログ表示中のタスクと名前が衝突しないよう別名を付ける必要があります。ViewModel の中からタスクを起動するときは launchSubTask() を使うのが簡単で確実です（詳細は [UtImmortalTask 詳説](./immortal-task-ja.md#サブタスク)）。

### CompactDialog の修正

まず、createBodyView() から enableBinding() の呼び出しを削除します。

次に、OKボタン押下時にダイアログを閉じてよいかどうかのチェックを追加します。`confirmToCompletePositive()` をオーバーライドし、viewModel.yourName が空なら、上で実装した showErrorMessage() を呼びます。confirmToCompletePositive() が false を返すと、ダイアログは閉じません。

```kotlin
class CompactDialog : UtDialogEx() {
    ...
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)  // enter key on the name input --> onPositive
        return controls.root
    }
    override fun confirmToCompletePositive(): Boolean {
        return if(viewModel.yourName.value.isNotEmpty()) {
            true
        } else {
            viewModel.showErrorMessage()
            false
        }
    }
}
```

このように、UtImmortalTask を使えば、ダイアログの中からメッセージボックスを表示することも簡単に実現できます。

## 次のステップ

- [チュートリアル（応用編）- サブダイアログと外部Activity連携](./tutorial-subdialog-ja.md)
- [UtImmortalTask 詳説](./immortal-task-ja.md)
- [UtDialog リファレンス](./reference-ja.md)
- [メッセージボックス / 選択ボックス](./messagebox-ja.md)
