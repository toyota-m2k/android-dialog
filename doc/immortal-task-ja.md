# UtImmortalTask 詳説

<div align="right">
<a href="./immortal-task.md">EN</a> | JA
</div>

`UtImmortalTask` は、UtDialog ライブラリの中核となる仕組みです。「ダイアログを表示してから、ユーザーが操作を完了するまで」のような、Activity のライフサイクルとは無関係に生き続けるべき一連の処理を、**名前付きのタスク**（コルーチンスコープ）として実行します。

- タスクは、Activity の破棄・再生成に影響されません（だから "Immortal"＝不死身）。
- タスク内から表示したダイアログは、Activity が再生成されると、自動的に新しい Activity 上に復元され、タスクとの接続も回復します。
- タスクのライフサイクルに紐づく ViewModel ([UtDialogViewModel](#viewmodel-の作成と取得)) によって、呼び出し元とダイアログの間で安全にデータを受け渡しできます。

この仕組みを支えるのが、`UtMortalActivity`（またはそれと同等の実装を持つ Activity）です。Activity は、フォアグラウンドに出るたびに自分自身をライブラリに登録し（内部的には `UtImmortalTaskManager` が管理するスタックに積まれ）、タスクは「今生きている Activity」をそこから取得してダイアログを表示します。

## タスクの起動

タスクの起動には、`UtImmortalTask` オブジェクトの関数を使います。用途に応じて５種類あります。

|関数|戻り値|説明|
|---|---|---|
|`launchTask(callback)`|Job|タスクを起動して、すぐに制御を返します（やりっぱなし）。タスク内で例外が発生した場合はログに出力され、外にはスローされません。|
|`awaitTask(callback)`|Unit|タスクの終了を待ちます。タスク内で例外が発生すると、その例外をスローします。|
|`awaitTaskCatching(callback)`|Unit|タスクの終了を待ちます。タスク内で例外が発生しても、スローしません。|
|`awaitTaskResult(callback)`|T|callback の戻り値（T型）を待ちます。タスク内で例外が発生すると、その例外をスローします。|
|`awaitTaskResultCatching(default, callback)`|T|callback の戻り値（T型）を待ちます。タスク内で例外が発生した場合は、default を返します。|

```kotlin
// やりっぱなし
UtImmortalTask.launchTask {
    showConfirmMessageBox("Info", "Completed.")
}

// 結果（戻り値）を待つ
suspend fun inputName(): String? {
    return UtImmortalTask.awaitTaskResult {
        val vm = createViewModel<CompactDialogViewModel>()
        if (showDialog(CompactDialog()).status.ok) vm.yourName.value else null
    }
}
```

callback は、`IUtImmortalTask` をレシーバとする suspend 関数です。このスコープ内で使える API を[後述](#タスクスコープ内で使える-api)します。

### タスク名と多重起動の制御

各関数には、タスク名 (`taskName`) と、多重起動時の動作 (`allowSequential`) を指定するオーバーロードがあります。

```kotlin
fun launchTask(taskName:String, allowSequential:Boolean, callback: suspend IUtImmortalTask.() -> Unit):Job
```

- **taskName**<br>
  タスクを一意に識別する名前です。省略すると既定の名前 (`"UtImmortalTask.Default"`) が使われます。ダイアログや ViewModel は、この名前をキーにタスクと関連付けられます。
- **allowSequential**<br>
  同じ名前のタスクが実行中だった場合の動作を指定します。
  - `false`（デフォルト）: エラー（多重起動の抑止）。ボタン連打で同じダイアログが多重に開くことを防げます。
  - `true`: 実行中のタスクが終わるのを待ってから実行します。

## タスクスコープ内で使える API

### ダイアログの表示

```kotlin
suspend fun <D:IUtDialog> showDialog(dlg: D): D
suspend fun <D:IUtDialog> showDialog(tag: String, dialogSource: (UtDialogOwner) -> D): D
```

タスク内からダイアログを表示し、ダイアログが閉じられるまでサスペンドします。戻り値として、閉じられたダイアログのインスタンスを返すので、`status` プロパティ（どのボタンで閉じられたか）や、ダイアログクラスのプロパティを参照できます。

`tag` は、タスク内でダイアログを識別する名前です（省略形ではダイアログのクラス名が使われます）。同じタスク内で、同じ tag のダイアログを同時に複数表示することはできません。

ViewModel の作成とダイアログ表示をまとめて行う簡略版もあります。

```kotlin
// createViewModel<VM>() + showDialog(D()) と同等
suspend inline fun <reified VM: UtDialogViewModel, reified D:IUtDialog> showDialog():D
```

このほか、特定の Activity が表示されるのを待ってからダイアログを表示するオーバーロードも用意しています。複数の Activity を持つアプリで、特定画面の上でだけダイアログを開きたい場合に使用します。

```kotlin
suspend fun <D:IUtDialog> showDialog(tag: String, ownerClass: Class<*>, dialogSource: (UtDialogOwner) -> D): D
suspend fun <D:IUtDialog> showDialog(tag: String, ownerChooser: (LifecycleOwner) -> Boolean, dialogSource: (UtDialogOwner) -> D): D
```

### メッセージボックス・選択ボックス

`showConfirmMessageBox()`, `showOkCancelMessageBox()`, `showYesNoMessageBox()` など、定型のメッセージボックスを１行で表示する拡張関数を用意しています。詳細は [メッセージボックス](./messagebox-ja.md) をご参照ください。

### ViewModel の作成と取得

```kotlin
inline fun <reified T: UtDialogViewModel> IUtImmortalTask.createViewModel(noinline initialize:(T.()->Unit)?=null) : T
```

タスクのライフサイクルに紐づく `UtDialogViewModel` を作成します。**必ずダイアログを表示する前に作成してください。** initialize ラムダで、ViewModel の初期化（引数の受け渡しなど）ができます。

```kotlin
UtImmortalTask.launchTask {
    val vm = createViewModel<SomeDialogViewModel> { someParam = 123 }
    if (showDialog(SomeDialog()).status.ok) {
        // vm から結果を取り出す
    }
}
```

ダイアログクラス側では、`IUtDialog.getViewModel()` で、タスクが作成した ViewModel を取得します。

```kotlin
class SomeDialog : UtDialogEx() {
    private val viewModel by lazy { getViewModel<SomeDialogViewModel>() }
    ...
}
```

タスクが終了すると、そのタスク上に作られた ViewModel も破棄（onCleared）されます。

### Activity（オーナー）の取得

タスク内から、現在フォアグラウンドにある Activity を利用するときは、`withOwner()` を使います。Activity が存在しない（アプリがバックグラウンドにいる）間は、Activity が再開されるまでサスペンドして待つので、null チェックやライフサイクルの心配は不要です。

```kotlin
suspend fun <T> withOwner(fn: suspend (UtDialogOwner) -> T): T                 // 現在のオーナーを取得
suspend fun <T> withOwner(clazz: Class<*>, fn: suspend (UtDialogOwner) -> T): T  // 指定クラスのActivityを待って取得
```

`UtDialogOwner` は Activity（または Fragment）のラッパーで、`asContext()`, `asActivity()`, `lifecycleOwner` などが利用できます。[Activity Broker](./activity-broker-ja.md) と組み合わせると、ファイルピッカーの起動なども ViewModel 内に直列に記述できます。

```kotlin
launchSubTask {
    withOwner { owner ->
        val uri = owner.asActivityBrokerStore().openReadOnlyFilePicker.selectFile()
        ...
    }
}
```

型を指定して Activity を直接受け取る拡張関数もあります。

```kotlin
suspend inline fun <reified T:FragmentActivity, R> IUtImmortalTask.withActivity(fn: (T)->R):R
suspend fun IUtImmortalTask.getActivity(): FragmentActivity?
```

### Application・リソース文字列の取得

タスクスコープ（および UtDialogViewModel）からは、いつでも Application と文字列リソースを取得できます。

```kotlin
val IUtImmortalTask.application : Application
fun IUtImmortalTask.getStringOrNull(@StringRes id:Int):String?
fun IUtImmortalTask.getStringOrDefault(@StringRes id:Int, default:String):String
```

## サブタスク

実行中のタスクからさらにタスクを起動したい場合（ダイアログの中からサブダイアログやメッセージボックスを開く場合など）のために、`UtDialogViewModel` に次のメソッドを用意しています。

|メソッド|説明|
|---|---|
|`launchSubTask(callback): Job`|サブタスクを起動して、すぐに制御を返します。|
|`awaitSubTask(callback)`|サブタスクの終了を待ちます。例外はスローされます。|
|`awaitSubTaskCatching(callback)`|サブタスクの終了を待ちます。例外はスローされません。|
|`awaitSubTaskResult(callback): T`|サブタスクの結果（T型）を待ちます。例外はスローされます。|
|`awaitSubTaskResultCatching(default, callback): T`|サブタスクの結果（T型）を待ち、例外発生時は default を返します。|

タスク起動用の各関数（launchTask / awaitTask / awaitTaskCatching / awaitTaskResult / awaitTaskResultCatching）と対応しています。

サブタスクは、親タスクの名前から派生した一意な名前（`親タスク名#連番`）で実行されるため、タスク名の衝突を気にする必要がありません。

```kotlin
class SomeDialogViewModel : UtDialogViewModel() {
    fun showSubDialog() {
        launchSubTask {
            val vm = createViewModel<SubDialogViewModel>()
            if (showDialog(SubDialog()).status.ok) {
                ...
            }
        }
    }
}
```

`UtImmortalTask.launchTask("another-name") {...}` のように、明示的に別名を付けて独立したタスクを起動しても同様に動作しますが、実行中のタスクとの名前の衝突を自分で管理する必要があるため、ViewModel 内からは launchSubTask() の利用をお勧めします。

## Activity との連携の仕組み

タスクと Activity の連携は、次のように実現されています。

- `UtMortalActivity` は、onResume で自分自身を `UtImmortalTaskManager` に登録し、onPause で登録解除します（実装の実体は `UtMortalTaskKeeper`）。
- タスク内の `showDialog()` や `withOwner()` は、登録されている最新の Activity を取得して動作します。登録された Activity がなければ（アプリがバックグラウンドにいる間は）、Activity が登録されるまでサスペンドします。
- ダイアログ (DialogFragment) は、Activity 再生成時に FragmentManager によって復元され、タスク名をキーにタスク・ViewModel との接続を回復します。

したがって、`AppCompatActivity` 派生の既存 Activity を変更できない場合でも、`UtMortalTaskKeeper` のイベントハンドラ（onResume/onPause など）を呼び出す実装を追加すれば、UtMortalActivity と同様にライブラリと連携できます。詳しくは [UtMortalActivity.kt](../dialog/src/main/java/io/github/toyota32k/dialog/mortal/UtMortalActivity.kt) の実装をご参照ください。

## 関連ドキュメント

- [チュートリアル（基本編）](./tutorial-basic-ja.md)
- [チュートリアル（応用編）](./tutorial-subdialog-ja.md)
- [メッセージボックス / 選択ボックス](./messagebox-ja.md)
- [Activity Broker](./activity-broker-ja.md)
- [v6 から v7 への移行ガイド](./migration-v7-ja.md)
