# Android用 ダイアログライブラリ

## このライブラリの目的

このライブラリは、おもに次の２つの目的で作成しました。

1. 扱いにくい DialogFragment, AlertDialog をラップし、コンテンツ(layout)を定義するだけで適切に表示できる汎用的なダイアログレンダリングシステム。
2. ActivityやFragmentのライフサイクルを正しく扱いつつ、ダイアログを表示し、ユーザー操作の結果を確実に受け取るためのフレームワーク。


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


## ダイアログを作る

ここでは、簡単なカスタムダイアログの作り方を説明します。
メッセージボックスの利用に関しては、

- [メッセージボックス](./doc/message_box-ja.md)
- [リストからのアイテム選択](./doc/selection_box-ja.md)

をご参照ください。

### １）ダイアログのレイアウトを作成

OK/Cancelなどのボタンは、親クラスが自動的に作成します。
ダイアログのレイアウトでは、「中身」だけを定義してください。
次の例は、名前入力欄だけを持つ、小さなダイアログです。ここではルートのGroupViewには、LinearLayout を使っていますが、
任意のGroupView（ConstraintLayoutなど）を使用できます。また、ルートGroupViewの layout_width / layout_height は、
ダイアログクラス側の widthOption/heightOption によって適当に調整されますが、
できるだけ、これらのオプションと矛盾しない指定にしておくことをお勧めします。
（すべてのケースを検証できていないので。。。）

`sample_compact_dialog.xml`
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
        android:text="Name"
        />
    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />
</LinearLayout>
```
### ２）UtDialog を継承して、カスタムなダイアログクラスを作成

init で、ダイアログのサイズ(widthOption, heightOption)やボタンのタイプなどを指定し、createBodyView() で、１）で作成したレイアウトをinflate しています。
端末の向きによってレイアウトを変える場合などは、preCreateBodyView()をオーバーライドして、
isLandscape などのプロパティをチェックして設定を変えるようにします。
この例では、HeightOption.COMPACT の設定により、WRAP_CONTENT 的なレイアウトになりますが、画面いっぱいに表示したり、
中身に合わせて伸縮し、必要に応じてスクロールする設定も可能です。詳しくは、[カスタムダイアログ](./doc/custom_dialog.md) や、
[UtDialog リファレンス](./doc/dialog_reference.md) を参照してください。

`CompactDialog.kt`
```Kotlin
class CompactDialog : UtDialog() {
    init {
        title="小さいダイアログ"
        setLimitWidth(400)
        heightOption=HeightOption.COMPACT
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
    }

    // 呼び出し元から、結果（このダイアログだと入力された名前）を取り出せるようにするためのプロパティ
    var name:String? = null

    /**
     * ダイアログの中身 (bodyView)を作成して返す。
     */
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_compact_dialog)
    }

    /**
     * Doneボタンがタップされたときの処理
     * このサンプルではｍ呼び出し元から結果が参照できるように、入力された内容を name プロパティにセットしています。
     */
    override fun onPositive() {
        name = dialog?.findViewById<EditText>(R.id.name_input)?.text?.toString() ?: ""
        super.onPositive()
    }
}
```

## ダイアログを表示して、結果を取り出す

ダイアログの表示と結果の取得には、[コルーチンを使用し（Activityのライフサイクルの外側で）同期的に実装する方法](./doc/task.md) と、
[Activity（やFragment）から表示して、Activity/Fragmentで結果を受け取る方法](./doc/dialog_management.md) の２つがあります。
ここでは、より簡単で推奨されるコルーチンを使う例を示します。

```kotlin
class CompactDialog : UtDialog() {
    companion object {
        suspend fun show():String? {
            UtImmortalSimpleTask.executeAsync(tag) {
                val dlg = showDialog(tag) { CompactDialog() }
                dlg.name    // executeAsync の戻り値
            }
        }        
    }
    // ...
}
```
`showDialog()` は、UtImmortalTaskBase のメソッドであり、
呼び出された時点でアクティブな（onResume～onPausedの）Activity/Fragmentを見つけ（もし、なければ、利用可能になるまsuspend）、
確実にダイアログを表示し、ユーザー操作などによってダイアログが閉じらるまでsuspendします。ダイアログがOKで閉じたかCancelで閉じたか、などは、
dlg.status で確認できます。

## ViewModel の使用

上の例では、ダイアログのプロパティ(name) を使ってデータを受け取りましたが、より複雑なモデルが必要となるダイアログでは ViewModel が使用されると思います。
このとき、ViewModel の生存期間がダイアログの生存期間と同じかそれ以上でないと、動作不正を起こします。これを避けるために、ViewModelStoreOwnerとして、
IUtImmortalTaskContext（IUtImmortalTask.immortalTaskContext）を使用します。

`UtImmortalViewModelHelper.createBy()` を使うことで、IUtImmortalTaskContext によるViewModel管理と、ViewModel内からの、ImmortalTask スコープの利用が可能になります。
具体的には次のように実装します。

```kotlin
class SomeViewModel : ViewModel(), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext
    
    companion object {
        // 作成・・・タスク開始時（ダイアログからinstanceOf()が呼ばれる前）によぶ。
        fun createBy(task: IUtImmortalTask) : SomeViewModel
                = UtImmortalViewModelHelper.createBy(SomeViewModel::class.java, task)

        // 取得：ダイアログの init, createBodyView()などのタイミングで（作成済みの）ViewModelを取り出す。
        fun instanceOf(taskName:String):SomeViewModel
                = UtImmortalViewModelHelper.instanceOf(SomeViewModel::class.java, taskName)
    }
}
```
## おまけ・・・アクティビティ呼び出し

ダイアログを表示して結果を受け取る仕組みを拡張し、他のアクティビティを起動して結果を受け取る `registerForActivityResult()` も、コルーチン内で同期的に記述できます。
`io.github.toyota32k.dialog.broker.pickers` には、ファイル選択系のアクティビティ呼び出しが実装されています。

```kotlin
class SomeActivity : UtMortalActivity(), IUtFilePickerStoreProvider {
    override val filePickers: UtFilePickerStore by lazy { UtFilePickerStore() }
    
    suspend fun openFile() {
        UtImmortalSimpleTask.executeAsync(tag) {
            val uri = filePickers.openFilePicker.selectFile() ?: return@executeAsync
            val activity = getActivity()
            assert(activity!==this@SomeActivity)    // 元のActivityインスタンスは死んでる。
            // uri を使ってなんかやる
        }
    }
}
```
ただし、openFile()が呼ばれ、filePickers.openFilePicker.selectFile() を実行すると、ファイル選択用Activityが起動するので、
SomeActivityは一旦 destroy され、場合によってはインスタンスも破棄されます。その場合、selectFile()から返ってきたときには、新しいActivityが起動しているので、
this@SomeActivity は使わず、UtImmortalTaskBase.getActivity() などの関数を使って、Activityを取得しなおす必要があります。

このことは、「ダイアログを表示して、結果を取り出す」で示した、showDialog() に関しても同様です。ImmortalTask内でsuspend関数を呼び出すと、
その前後で、ビューやActivityなどの mortal なインスタンスが同一である保証がないことに注意してください。
