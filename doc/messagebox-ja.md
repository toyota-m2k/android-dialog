# メッセージボックス / 選択ボックス

<div align="right">
<a href="./messagebox.md">EN</a> | JA
</div>

メッセージボックスは、タイトル（文字列）、メッセージ（文字列）と、OKボタン、Cancelボタン（または Yes/Noボタン）を表示してユーザーの判断を促す簡単なダイアログです。内部的には AlertDialog を利用し、UtDialog の作法に従って、どこからでも簡単に利用できます。

## 事前準備

ダイアログを表示する Activity を `UtMortalActivity` から派生します。既存の実装（派生元クラス）を変更できない場合は、`UtMortalActivity` の実装を参考に、Activity クラスに必要な処理（主に `UtMortalTaskKeeper` のイベントハンドラ呼び出し）を追加してください。詳細は [UtImmortalTask 詳説](./immortal-task-ja.md#activity-との連携の仕組み) をご参照ください。

## メッセージボックスの表示

`UtMessageBox` も IUtDialog の実装クラスであり、通常の UtDialog と同様に、UtImmortalTask のスコープ内で UtMessageBox インスタンスを構築して表示できます。

```kotlin
UtImmortalTask.launchTask {
    showDialog("confirm") {
        UtMessageBox.createForConfirm("Download File", "Completed.")
    }
}
```

さらに、メッセージボックス表示に特化した拡張関数を使うと、上記のコードは次のように書けます。

```kotlin
UtImmortalTask.launchTask {
    showConfirmMessageBox("Download File", "Completed.")
}
```

いずれの拡張関数も、ユーザーがボタンを押下する（またはキャンセルする）までサスペンドします。title, message などの文字列引数の代わりに、文字列リソースID (`@StringRes Int`) を渡すオーバーロードも用意しています。

## メッセージボックス表示用拡張関数

### (1) 確認メッセージ

```kotlin
suspend fun IUtImmortalTask.showConfirmMessageBox(
    title:String?,
    message:String?,
    okLabel:String = UtStandardString.OK.text,
    cancellable:Boolean = true)
```

確認(OK)ボタンを１つだけ持つメッセージボックスを表示します。
ユーザーがOKボタンを押下するまでサスペンドします。戻り値はありません。

### (2) OK/Cancel メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showOkCancelMessageBox(
    title:String?,
    message:String?,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : Boolean
```

OKボタンとキャンセルボタンを持つメッセージボックスを表示します。
ユーザーがボタンを押下するまでサスペンドし、OKボタンが押下されると true を、キャンセルボタンが押下されると false を返します。

### (3) Yes/No メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showYesNoMessageBox(
    title:String?,
    message:String?,
    yesLabel:String = UtStandardString.YES.text,
    noLabel:String = UtStandardString.NO.text,
    cancellable:Boolean = false) : Boolean
```

OKボタンが Yes、Cancelボタンが No と表記される以外は、OK/Cancel メッセージボックスとまったく同じです。

### (4) ３択メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showThreeChoicesMessageBox(
    title:String?,
    message:String?,
    positiveLabel:String,
    neutralLabel:String,
    negativeLabel:String,
    cancellable:Boolean = false) : IUtDialog.Status
```

Positive/Neutral/Negative の３つのボタンを持つメッセージボックスです。例えば、エラー発生時に \[Retry\] / \[Skip\] / \[Abort\] の３つの選択肢を提示するような場合に使用します。ユーザーの選択結果は、IUtDialog.Status 型の戻り値 (POSITIVE/NEUTRAL/NEGATIVE) として受け取ります。

### (5) リストからの単一選択メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showSingleSelectionBox(
    title:String?,
    items:Array<String>,
    cancellable:Boolean = false) : Int
```

リスト項目を文字列の配列として渡します。ユーザーがリスト項目をタップすると、戻り値として、その項目の配列上のインデックスを返します。メッセージボックス外をタップするなどして選択がキャンセルされた場合は、-1 を返します。

### (6) ラジオボタン型リストからの単一選択メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showRadioSelectionBox(
    title:String?,
    items:Array<String>,
    initialSelection:Int,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String? = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : Int
```

`showSingleSelectionBox()` と類似していますが、こちらはリスト上での選択状態をラジオボタンとして表示し、ユーザーがリスト項目をタップしてもメッセージボックスは閉じず、選択状態が変化します。ユーザーがOKボタンを押下すると、最後に選択されていた項目のインデックスが戻り値として返ります。キャンセルされた場合は -1 を返します。`showSingleSelectionBox()` がリストからの単純な選択に利用するのに対して、`showRadioSelectionBox()` は、現在の選択値を表示した上で、それを変更させる場合に利用します。

### (7) リストからの複数選択メッセージボックス

```kotlin
suspend fun IUtImmortalTask.showMultiSelectionBox(
    title:String?,
    items:Array<String>,
    initialSelections:BooleanArray?,
    okLabel:String = UtStandardString.OK.text,
    cancelLabel:String? = UtStandardString.CANCEL.text,
    cancellable:Boolean = true) : BooleanArray
```

`showRadioSelectionBox()` が単一選択用ラジオボタンなのに対して、`showMultiSelectionBox()` は複数選択可能なチェックボックスリストになります。ユーザーが選択した項目は、BooleanArray 型の戻り値として返されます（キャンセルされた場合は空の BooleanArray）。また、initialSelections を渡すことで、メッセージボックスを表示した直後の選択状態を指定できます。

## 関連ドキュメント

- [UtImmortalTask 詳説](./immortal-task-ja.md)
- [チュートリアル（基本編）](./tutorial-basic-ja.md)
- [UtDialog リファレンス](./reference-ja.md)
