# v6 から v7 への移行ガイド

<div align="right">
<a href="./migration-v7.md">EN</a> | JA
</div>

v7 では、ImmortalTask まわりの実装を整理・簡素化しました。タスクの実装クラスを隠蔽し、`UtImmortalTask` オブジェクトの関数（ラムダを渡すスタイル）に一本化しています。v6 で非推奨 (Deprecated) となっていたクラスも削除しました。

`UtImmortalTask.launchTask {...}` と `createViewModel()`/`showDialog()` を使う標準的な書き方（[チュートリアル](./tutorial-basic-ja.md)のスタイル）で実装している場合、修正は不要です。

## 削除されたクラス・API

### UtImmortalTaskBase

タスクごとに派生クラスを作成する方式は廃止しました。`UtImmortalTask` の関数にラムダを渡す方式に移行してください。

```kotlin
// v6
class MyTask : UtImmortalTaskBase(TASK_NAME) {
    override suspend fun execute(): Boolean {
        val vm = createViewModel<MyViewModel>()
        return showDialog(MyDialog()).status.ok
    }
}
MyTask().fire()

// v7
UtImmortalTask.launchTask {
    val vm = createViewModel<MyViewModel>()
    showDialog(MyDialog()).status.ok
}
```

- `execute()` が Boolean を返す規約は廃止されました。戻り値が必要な場合は `awaitTaskResult()` を使います（型は任意）。
- `fire(coroutineScope)` の coroutineScope 引数は廃止されました。タスクは常にライブラリ内部のスコープ（メインスレッド）で実行されます。

### UtImmortalSimpleTask（v6で非推奨）

`UtImmortalTask.launchTask()` / `awaitTask()` / `awaitTaskResult()` に置き換えてください。コールバックが Boolean を返す規約は廃止されています。

```kotlin
// v6 (deprecated)
UtImmortalSimpleTask.run("myTask") {
    showConfirmMessageBox(null, "hello")
    true    // 意味のない true を返す必要があった
}

// v7
UtImmortalTask.launchTask("myTask") {
    showConfirmMessageBox(null, "hello")
}
```

### UtImmortalViewModelHelper（v6で非推奨）

`UtDialogViewModel` のコンパニオン関数、または拡張関数に置き換えてください。

|v6|v7|
|---|---|
|`UtImmortalViewModelHelper.createBy(clazz, task)`|`IUtImmortalTask.createViewModel<VM>()` または `UtDialogViewModel.create(clazz, task)`|
|`UtImmortalViewModelHelper.instanceOf(clazz, taskName)`|`UtDialogViewModel.instanceOf(clazz, taskName)`|
|`UtImmortalViewModelHelper.instanceFor(clazz, dialog)`|`IUtDialog.getViewModel<VM>()` または `UtDialogViewModel.instanceFor(clazz, dialog)`|

### IUtImmortalTask#taskResult

タスクの結果を `taskResult` プロパティに残す仕組みは廃止しました。タスクの結果は、`awaitTaskResult()` の戻り値として直接受け取ってください。

## 変更されたAPI

### UtDialogViewModel のサブタスク関数

シグネチャと実装が変わりました。コールバックのレシーバは `UtImmortalTaskBase` から `IUtImmortalTask` になります。

- `launchSubTask(callback): Job` … Job を返すようになりました。
- `awaitSubTaskResult(callback): T` … 結果 (T) を返すようになりました（v6 では結果が返らない不具合がありました）。
- `awaitSubTaskCatching(callback)` … 追加。例外をスローせずに、サブタスクの終了を待ちます。
- `awaitSubTaskResultCatching(default, callback): T` … 追加。例外発生時に default を返します。

また、内部動作として、サブタスクは親タスクのコンテキストを共有するのではなく、親タスク名から派生した一意な名前 (`親タスク名#連番`) を持つ独立したタスクとして実行されるようになりました。通常の使い方で影響はありませんが、タスク名に依存した処理（`UtImmortalTaskManager.taskOf()` など）を行っている場合はご注意ください。

### 同一タスク内での複数ダイアログの並列表示

v6 では、同一タスク内でダイアログの表示待ち（continuation）をスタックで管理していたため、複数のダイアログを並列に表示すると、閉じる順序によっては誤動作する可能性がありました。v7 では tag をキーにした管理に変更し、親ダイアログが先に閉じるようなケースでも正しく動作します。

ただし、**同じ tag のダイアログを同一タスク内で並列表示することはできません**（`IllegalStateException` がスローされます）。`showDialog(dlg)` の省略形はクラス名を tag として使うので、同じダイアログクラスを並列表示する場合は、`showDialog(tag) {...}` で明示的に異なる tag を指定してください。

## v5 以前からの移行

v4 以前のダイアログ用 ViewModel（`IUtImmortalTaskMutableContextSource` を自前で実装する方式）を使っている場合は、まず `UtDialogViewModel` 派生（v5 で導入）への移行が必要です。[チュートリアル（基本編）](./tutorial-basic-ja.md) の実装スタイルを参考にしてください。

## 関連ドキュメント

- [UtImmortalTask 詳説](./immortal-task-ja.md)
- [チュートリアル（基本編）](./tutorial-basic-ja.md)
