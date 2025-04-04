# Android用 ダイアログライブラリ
<div align="right">
<a href="./README.md">EN</a> | JA
</div>

## Introduction

## はじめに

このライブラリは、
1. Activityの生存期間に影響されないユーザー操作スコープの導入による実装の簡潔化を目的として開発しました。さらに、
1. ActivityやFragmentのライフサイクルを正しく扱い、ダイアログでのユーザー操作の結果を確実に受け取るためのフレームワーク。
1. 扱いにくい DialogFragment や AlertDialog をラップし、コンテンツ(layout)を定義するだけで適切に表示できる汎用的なダイアログレンダリングシステム。

などを目標に開発しました。

Androidアプリの開発においては、
Application, Activity, Fragment など、
ライフサイクル（生存期間）が異なるアプリケーションコンポーネントの存在が、実装の難易度・複雑さを上げ、ソースの可読性を低下させる最大の要因ではないかと思います。例えば、Windowsアプリ（WPF/UWP/WinUI...）なら、
```kotlin
// if it were windows ...
val dlg = WhatsYourNameDialog()
val result = dlg.show()
if(result!=null) {
    output.value = result.yourName
}
```
のように、直感的な実装が可能ですが、Androidではそうはいきません。
同じような書き方が Android でもできたら、便利だと思いませんか？ この `UtDialog` ライブラリを使えば、 `UtImmortalTask` ブロック（コルーチンスコープ）内で、次のように書けます。
```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<WhatsYourNameViewModel>()
    if(showDialog<WhatsYourNameDialog>().status.ok) {
        output.value = vm.yourName.value
    }
}
```

## 基本コンセプト

Activity はデバイスを回転したり、他のアプリに切り替えるたびに、インスタンスが生まれ変わる（破棄されて再生する）ライフサイクルを持っています。一方、ダイアログやメッセージボックスを使う処理は、画面に表示してから、ユーザーが操作して決定を下すまでの間が、意味的に１つのライフサイクル（生存期間）なのですが、これが Activity のライフサイクルと一致しないことが、Android 開発の難易度を上げる１つの要因となっています。

UtDialog ライブラリでは、上記のようなライフサイクルの違いを前提に、ユーザーが操作を開始してから完了するまで死ぬことのないタスク (UtImmortalTask)と、OSに生殺与奪の権利を握られている死すべき定めのActivity (UtMortalActivity) と定義し、それらを協調的に動作するシステムを構築しました。

## インストール (Gradle)

settings.gradle.kts で、mavenリポジトリ https://jitpack.io への参照を定義します。  

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}
```

モジュールの build.gradle で、dependencies を追加します。
```kotlin
dependencies {
    implementation("com.github.toyota-m2k:android-dialog:Tag")
}
```

## UtDialog と Activity の連携準備

UtImmortalTask, UtDialog と Activity は、`IUtDialogHost` インターフェースを介して通信します。

`AppCompatActivity` の代わりに、`UtMortalActivity` から Activity クラスを派生すれば、必要な実装はすべて用意されています。既存の実装（派生元クラス）を変更できない場合は、 `UtMortalActivity` の実装を参考に、Activityクラスに必要な処理（主に、UtMortalTaskKeeperのイベントハンドラ呼び出し）を追加してください。

## チュートリアル：ダイアログを実装する

ここでは、ユーザーに文字列を入力させる簡単なダイアログ(`UtDialog`)を作って、MainActivityから表示し、入力された文字列をMainActivityに表示する実装例を使って、UtDialog の使い方を説明します。

### (1) ダイアログのレイアウトを作成

この例で使用するのは、ラベルと入力欄を１つ持つ簡単なレイアウトです。

**dialog-compact.xml**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
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

### (2) ViewModel の作成

入力項目は１つしかありませんが、
デバイス回転時の入力内容保全や、呼び出し元（この例では Activity）との確実なデータ受け渡しのために、ViewModel を使います。UtDialog を使う場合は、UtDialogViewModelから派生するのが便利です。

尚、UtDialogViewModel は、v5 で導入しました。v4 以前は、ViewModel から派生して、IUtImmortalTaskMutableContextSource を実装し、構築時に、immortalTaskContext をセットする必要がありました。これが、かなり煩わしく、ミスも多かったので、v5 で改善しました。

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
}
```

### (3) ダイアログクラスの作成

次に、UtDialog を派生してダイアログクラスを作成します。サンプルでは、Android 標準の ViewBinding （layout-xml の定義から、Viewインスタンスの参照を自動生成する仕掛け）に加えて、[android-binding](https://github.com/toyota-m2k/android-binding)（View-ViewModel Binding ライブラリ）を利用しています。`UtDialogEx` は、`UtDialog` に対して、android-binding を使用するための小さな仕掛けを提供します。具体的には、Binderインスタンスをメンバーに持ち、titleやleftButtin, rightButton などのダイアログが持っているとViewModelとをバインドするための拡張関数を定義しています。ViewBindingも、android-binding も利用は必須ではありませんが、ソースがコンパクトに書けて便利なのでお勧めです。

```kotlin
class CompactDialog : UtDialogEx() {
    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption=HeightOption.COMPACT
        setLimitWidth(400)
        gravityOption = UtDialog.GravityOption.CENTER
        leftButtonType = UtDialog.ButtonType.CANCEL
        rightButtonType = UtDialog.ButtonType.DONE
        cancellable = false
        draggable = true
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

まず、Android 標準の ViewBinding のためのフィールドを用意します。ここでは lateinit で定義しておいて、続くonCreateView() で初期化します。

```kotlin
lateinit var controls: DialogCompactBinding
```

次に、UtDialogViewModel のインスタンスは、Activity側で作成（後述）したものを `IUtDialog.getViewModel()`で取得して利用します。ViewModelを呼び出し元のスコープで作成するのが、データ受け渡しのための重要なポイントです。

```kotlin
private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }
```

ダイアログのプロパティは、`UtDialog.precreateBodyView()` をオーバーライドして設定します。

```kotlin
override fun preCreateBodyView() {
    title = "Compact Dialog"
    heightOption = UtDialog.HeightOption.COMPACT
    widthOption = UtDialog.WidthOption.LIMIT(400)
    gravityOption = UtDialog.GravityOption.CENTER
    leftButtonType = UtDialog.ButtonType.CANCEL
    rightButtonType = UtDialog.ButtonType.DONE
    cancellable = false
    draggable = true
    enableFocusManagement()
        .autoRegister()
        .setInitialFocus(R.id.name_input)
}
```

個々のプロパティと設定内容は次の通りです。
|プロパティ|説明|
|---|---|
|title|ダイアログのタイトルバーに表示する文字列。|
|hightOption|[ダイアログ高さの指定](./doc/sizing-option-ja.md)。COMPACT は、WRAP_CONTENT に相当します。|
|widthOption|[ダイアログの幅の指定](./doc/sizing-option-ja.md)。LIMIT(400) は、画面幅が 400 以下の場合は、FULL（MATCH_PARENT）として動作し、それ以上の場合は最大幅 400dp に制限します。|
|gravityOption|ダイアログの配置方法。CENTER を指定すると画面中央に配置します。|
|leftButtonType|左ボタンに Cancel ボタンを割り当てます。デフォルトは NONE (表示しない) です。|
|rightButtonType|右ボタンに Done ボタンを割り当てます。デフォルトは NONE (表示しない) です。|
|cancellable|false を指定すると、ダイアログ外をタップしてもダイアログを閉じません。|
|draggable|true を指定すると、タイトルバーをドラッグしてダイアログの移動ができます。|
|enableFocusManagement()<br>  .autoRegister()<br>  .setInitialFocus(R.id.name_input)|[フォーカス管理](./doc/focus-manager-ja.md)を有効化し、フォーカス可能なビューを自動登録、名前入力欄に初期フォーカスをセットします。|

ダイアログのプロパティについては、[リファレンス](./doc/reference-ja.md) をご参照ください。


最後に、UtDialog.createBodyView をオーバーライドして、ダイアログのボディとなるビューを作成し、必要なイベントリスナーの登録を行います。

この例では、ビューの作成には、ViewBinding.inflate() を使い、イベントリスナーの登録は、`binder` ([android-binding](https://github.com/toyota-m2k/android-binding)) によって隠蔽されています。具体的には、`editTextBinding` で、ViewModel の `yourName:MutableStateFlow<String>` と、TextView を双方向バインドし、`enableBinding` で、`yourName` に文字列がセットされていないときは、OKボタンを無効化するように構成しています。さらに、bindCommandを使って、TextView 上でのリターンキー押下を、OKボタンevent（onPositive）にバインドします。

```kotlin
override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
    controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
    binder
        .editTextBinding(controls.nameInput, viewModel.yourName)
        .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)     // enter key on the name input --> onPositive
    return controls.root
}
```

尚、ViewBinding を使わない場合は、inflater 引数を使って、layout-xml を inflate()して下さい。また、savedInstanceState は、FragmentDialog.onCreateDialog() または、Fragment.onCreateView() が受け取った ダイアログ再構築用の Bundle型のデータですが、UtDialogは必ず ViewModel を使うので、ほとんど使いません。もちろん `binder` を使わずに、controls.nameInput に addTextChangedListener() でリスナーを登録して、viewModel.yourName を更新し、viewModel.yourName.onEach() でビューモデルの変更をビューにセットするコードを書いても構いません。

### (4) Activity のレイアウト

ここからは、Activity側の実装を行います。次の例では、ダイアログを表示するトリガーとなる Button と、ダイアログの結果を表示するデモ用の TextView を配置しました。

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
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
        android:id="@id/output_text"
        android:layout_width="match_parent"
        android:layout_height="30dp"
        android:background="@color/secondaryTextColor"
        android:paddingHorizontal="10dp"
        android:paddingVertical="2dp"
        android:textColor="@color/secondaryColor"
        />
</LinearLayout>
```

### (5) MainActivityViewModelの作成

標準の ViewModel を継承して、MainActivityViewModel を作成します。
まず、ダイアログの結果の文字列を保持する、MutableStateFlow<String> 型の outputString を用意します。

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
}
```

### (6) UtDialog を表示するための実装

`CompactDialog` を表示するための実装を行います。Activityのどこに実装しても構いませんが、このサンプルでは、[android-binding](https://github.com/toyota-m2k/android-binding) の `LiteUnitCommand` を使って、MainActivityViewModel に実装します。ビューモデルのプロパティを更新するコマンドハンドラを、ViewModel 内にまとめることで、ソースコードが整理され、見通しがよくなります。

`UtImmortalTask.launchTask()` 関数を利用して、UtImmortalTaskのスコープを作成して、UtDialog を表示します。UtImmortalTask 内では、ビューモデル作成関数 `createViewModel()` や、ダイアログ表示関数 `showDialog()` が使え、必ず、ダイアログのビューモデルを作成してから、ダイアログを表示します。showDialog() は、UtDialog が閉じられるまで待機（サスペンド）し、UtDialogインスタンスを返します。ダイアログがどのように閉じられたかは、`IUtDialog#status` で確認します。

```kotlin
class MainActivityViewModel : ViewModel() {
    val outputString = MutableStateFlow("")
    val commandCompactDialog = LiteUnitCommand {
        UtImmortalTask.launchTask {
            outputString.value = "Compact Dialog opening"
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

### (7) MainActivityの実装

MainActivity は、UtMortalDialog を派生して実装します。
とはいえ、必要な処理は、ほとんど MainActivityViewModel に実装済みなので、[android-binding](https://github.com/toyota-m2k/android-binding) を使って ViewModel とビューをバインドしているだけです。


```kotlin
class MainActivity : UtMortalActivity() {
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainActivityViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        // use default button labels
        binder
        .owner(this)
        .bindCommand(viewModel.commandCompactDialog, controls.btnCompactDialog)
        .textBinding(controls.outputText, viewModel.outputString)
    }
}
```

以上で CompactDialog と、それを表示するMainActivityの実装ができましたが、ここから、少し改良してみましょう。

現在の実装では、名前が入っていないときに、DONEボタンをグレーアウトしていました。しかし「なぜボタンが押せないのか」がわからないので、状況によってグレーアウトはユーザーエクスペリエンスを低下させることもあります。そこで、グレーアウトをやめて、ボタンを押したときに、名前が空なら、"Input your name." というメッセージボックスを表示してみます。

### CompactDialogViewModel の修正

CompactDialogViewModel にメッセージボックスを表示するための、showErrorMessage() メソッドを追加しました。`UtImmortalTask.launch()` でタスクを開始して、showConfirmMessageBox()を呼ぶだけです。MainActivityViewModel からダイアログを開くのとまったく同じです。ただし、ダイアログを表示したときのタスクと区別するために、launchTaskに"sub"というタグをつけています。

```kotlin
class CompactDialogViewModel : UtDialogViewModel() {
    val yourName = MutableStateFlow("")
    fun showErrorMessage() {
        UtImmortalTask.launchTask("sub") {
            showConfirmMessageBox(null, "Input your name.")
        }
    }
}
```

もちろんこれでも正しく動作しますが、UtDialogViewModel の中からサブタスクを、より効率よく開始するために、ViewModelを作成したタスク上に新しいタスクスコープを作成する `UtDialogViewModel.launchSubTask()` 拡張関数を用意しています。これを使えば、次のように書き直すことができます。

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

### CompactDialog の修正

まず、createBodyView()で enableBinding() の呼び出しを削除（コメントアウト）します。

次に、OKボタン押下時にダイアログを閉じるかどうかのチェックを追加するので、`confirmToCompletePositive()` をオーバーライドし、viewModel.yourName が空なら、上で実装した showErrorMessage() を呼びます。confirmToCompletePositive()が false を返すと、ダイアログは閉じません。

```kotlin
class CompactDialog : UtDialogEx() {
    ...
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
        //  .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
            .bindCommand(LiteUnitCommand(this::onPositive), controls.nameInput)     // enter key on the name input --> onPositive
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

このように、UtImmortalTask を使えば、ダイアログの中からメッセージボックスを表示することも簡単に実現できました。


##  チュートリアル２：サブダイアログを表示する

チュートリアル１では、ダイアログから、メッセージボックスを表示するところまで示しましたが、同様に、他のダイアログ（サブダイアログ）を開くことも可能ですし、ファイルピッカーなど、外部アプリを呼び出して結果をダイアログに反映することさえできます。

このチュートリアル２では、サブダイアログで入力したテキストや、ファイルピッカーで選択されたファイルの名前を、リスト(RecyclerView) に追加していくダイアログを作成します。

### (1) レイアウトの作成

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

### (2) ViewModel の作成

ViewModelには、リストに表示する文字列を保持する ObservableList と、テキスト追加コマンド、ファイル選択コマンドを実装しました。`UtDialogViewModel.launchSubTask()`関数で ImmortalTask ブロックを作成していること以外は、ActivityのViewModelから`UtImmortalTask.launchTask()` を使ってダイアログを開いたのと何もかわらないことがわかると思います。

`UtDialogViewModel.launchSubTask()` は、ダイアログを表示した（正確には ViewModel を構築した）ImmortalTask上に、同じコンテキストでImmortalTaskブロックを作成するメソッドです。`UtImmortalTask.launchTask()`を使っても同様に動作しますが、新たなタスクを生成しないぶん launchSubTask() の方が少し効率的です。

```kotlin
class NestedDialogViewModel : UtDialogViewModel() {
    val observableList = ObservableList<String>()
    val commandAddText = LiteUnitCommand {
        launchSubTask {
            val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
            if(showDialog(CompactDialog()).status.ok) {
                observableList.add(vm.yourName.value)
            }
        }
    }
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

### (3) UtDialogクラスの作成

サブダイアログだからといって特別な実装はありません。必要なことは、正しく、コマンドをViewにバインドするだけです。チュートリアルの本題から逸れますが、この例では、[android-binding](https://github.com/toyota-m2k/android-binding) の `OvservableList` と `RecyclerViewBinding` を利用したので、RecyclerView の実装がとてもコンパクトになっていることに驚くかもしれません。`recyclerViewGestureBinding()`拡張関数を呼ぶだけで、リストデータとのバインディングはもちろん、横スワイプして削除や、ドラッグして並び替えまで実現できます。

```kotlin
class NestedDialog : UtDialogEx() {
    override fun preCreateBodyView() {
        title="Fill Height"
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
            .recyclerViewGestureBinding(controls.recyclerView, viewModel.observableList, R.layout.item_string_list, dragToMove = true, swipeToDelete=true, deletionHandler = null) {
                listBinder, view, text->
                val textView = view.findViewById<TextView>(R.id.text_view)
                listBinder.textBinding(this@NestedDialog, textView, text.asConstantLiveData())
            }
        return controls.root
    }
}
```
### (4) Activity の準備

ViewModelでファイルピッカーを使うために、`UtActivityBrokerStore` を使っています。
[ドキュメント](./doc/activity-broker-ja.md) の説明に従ってファイルピッカーを用意してます。

- 呼び出し元の Activity に、`IUtActivityBrokerStoreProvider` インターフェースを追加
- Activity内に、activityBrokers プロパティをオーバーライド

```kotlin
class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    ...
    override val activityBrokers = UtActivityBrokerStore(this, UtOpenReadOnlyFilePicker())
    ...
}
```



## リファレンス

- [UtDialog リファレンスマニュアル](./doc/dialog-options-ja.md)
- [メッセージボックスを表示する](./doc/messagebox-ja.md)
- [高度なダイアログ--HeightOptionの使い方](./doc/sizing-option-ja.md)
- [ファイルピッカー/Permission/...](./doc/activity-broker-ja.md)
- [フォーカスマネージャ](./doc/focus-manager-ja.md)
