# Android用 ダイアログライブラリ

## はじめに

Androidアプリの開発においては、
Application, Activity, Fragment など、
ライフサイクル（生存期間と表現した方が感覚に近いかも）が異なるアプリケーションコンポーネントの存在が、実装の難易度・複雑さを上げ、ソースの可読性を低下させる最大の要因ではないかと思います。Androidアプリでも、例えば、Windowsアプリ（WPF/UWP/WinUI...)では当たり前の、
```
val dlg = SomeDialog()
val result = dlg.show()
if(result) {
    ...
}
```
のような直感的な実装ができたら、便利だと思いませんか？

## このライブラリの目的

このライブラリは、主に次の２つの目的で作成しました。

1. ActivityやFragmentのライフサイクルを正しく扱い、ダイアログでのユーザー操作の結果を確実に受け取るためのフレームワーク。
1. 扱いにくい DialogFragment や AlertDialog をラップし、コンテンツ(layout)を定義するだけで適切に表示できる汎用的なダイアログレンダリングシステム。
1. Activityの生存期間に影響されないユーザー操作スコープの導入による実装の簡潔化。

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

## コンフィギュレーション

ダイアログの動作は、`UtDialogConfig` で設定します。
ApplicationまたはActivity派生クラスの onCreate() で、setup()を呼び出します。
```
    UtDialogConfig.setup(this)
```

その他の設定については、[コンフィギュレーション](./doc/configulation-ja.md) をご参照ください。

## UtDialog と Activity の連携準備

UtDialog と Activity は、`IUtDialogHost` インターフェースを介して通信します。
`AppCompatActivity` の代わりに、`UtMortalActivity` から Activity クラスを派生すれば、必要な実装はすべて用意されています。既存の実装（派生元クラス）を変更できない場合は、 `UtMortalActivity` の実装を参考に、Activityクラスに必要な処理（主に、UtMortalTaskKeeperのイベントハンドラ呼び出し）を追加してください。

## ダイアログを作る

ここでは、ユーザーに文字列を入力させるための、簡単なダイアログ(`UtDialog`)の作り方を説明します。

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
        android:inputType="text" />
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

次に、UtDialog を派生してダイアログクラスを作成します。サンプルでは、Android 標準の ViewBinding （layout-xml の定義から、Viewインスタンスの参照を自動生成する仕掛け）に加えて、[android-binding] (https://github.com/toyota-m2k/android-binding)（View-ViewModel Binding ライブラリ）を利用しています。`UtDialogEx` は、`UtDialog` に対して、android-binding を使用するための小さな仕掛けを提供します。具体的には、Binderインスタンスをメンバーに持ち、titleやleftButtin, rightButton などのダイアログが持っているとViewModelとをバインドするための拡張関数を定義しています。ViewBindingも、android-binding も利用は必須ではありませんが、ソースがコンパクトに書けて便利なのでお勧めです。

```kotlin
class CompactDialog : UtDialogEx() {
    private lateinit var controls: DialogCompactBinding
    private val viewModel by lazy { getViewModel<CompactDialogViewModel>() }

    override fun preCreateBodyView() {
        title = "Compact Dialog"
        heightOption=HeightOption.COMPACT
        setLimitWidth(400)
        gravityOption = UtDialog.GravityOption.CENTER
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
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
    setLeftButton(UtDialog.BuiltInButtonType.CANCEL)
    setRightButton(UtDialog.BuiltInButtonType.DONE)
    cancellable = false
    draggable = true
}
```

個々のプロパティと設定内容は次の通りです。
|プロパティ|説明|
|---|---|
|title|ダイアログのタイトルバーに表示する文字列。|
|hightOption|ダイアログ高さの指定。COMPACT は、WRAP_CONTENT に相当します。|
|widthOption|ダイアログの幅の指定。LIMIT(400) は、画面幅が 400 以下の場合は、FULL（MATCH_PARENT）として動作し、それ以上の場合は最大幅 400dp に制限します。|
|gravityOption|ダイアログの配置方法。CENTER を指定すると画面中央に配置します。|
|setLeftButton|左ボタンに Cancel ボタンを割り当てます。デフォルトは NONE (表示しない) です。|
|setRightButton|右ボタンに Done ボタンを割り当てます。デフォルトは NONE (表示しない) です。|
|cancellable|false を指定すると、ダイアログ外をタップしてもダイアログを閉じません。|
|draggable|true を指定すると、タイトルバーをドラッグしてダイアログの移動ができます。|

ダイアログのプロパティについては、[リファレンス](./doc/reference-ja.md) をご参照ください。


最後に、UtDialog.createBodyView をオーバーライドして、ダイアログのボディとなるビューを作成し、必要なイベントリスナーの登録を行います。

この例では、ビューの作成には、ViewBinding.inflate() を使い、イベントリスナーの登録は、`binder` ([android-binding] (https://github.com/toyota-m2k/android-binding))によって隠蔽されています。具体的には、ViewModel の `yourName:MutableStateFlow<String>` と、TextView を双方向バインドし、`yourName` に文字列がセットされていないときは、OKボタンを無効化するように構成しています。

ViewBinding を使わない場合は、inflater 引数を使って、layout-xml を inflate()して下さい。尚、savedInstanceState は、FragmentDialog.onCreateDialog() または、Fragment.onCreateView() が受け取った ダイアログ再構築用の Bundle型のデータですが、UtDialogは必ず ViewModel を使うので、ほとんど使いません。もちろん `binder` を使わずに、controls.nameInput に addTextChangedListener() でリスナーを登録して、viewModel.yourName を更新し、viewModel.yourName.onEach() でビューモデルの変更をビューにセットするコードを書いても構いません。

```kotlin
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogCompactBinding.inflate(inflater.layoutInflater, null, false)
        binder
            .editTextBinding(controls.nameInput, viewModel.yourName)
            .enableBinding(rightButton, viewModel.yourName.map { it.isNotEmpty() }) // ensure the name is not empty
        return controls.root
    }
```

