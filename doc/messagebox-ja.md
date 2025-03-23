# メッセージボックスを表示する
<div align="right">
<a href="./messagebox.md">EN</a> | JA
</div>

メッセージボックスは、
タイトル（文字列）、メッセージ（文字列）と、OKボタン、Cancelボタン （または Yes/Noボタン）を表示してユーザーの判断を促す簡単なダイアログです。内部的には、AlertDialog を利用し、UtDialog の作法に従って、どこからでも簡単に利用できます。


## 事前準備

ダイアログを表示するActivityを、`UtMortalActivity` から派生します。既存の実装（派生元クラス）を変更できない場合は、 `UtMortalActivity` の実装を参考に、Activityクラスに必要な処理（主に、UtMortalTaskKeeperのイベントハンドラ呼び出し）を追加してください。

## メッセージボックスの表示

UtMessageBox も IUtDialog の実装クラスであり、通常の UtDialog と同様に、
UtImmortalTask スコープ内で UtMessageBox インスタンスを構築して表示できます。

```kotlin
UtImmortalTask.launchTask {
    showDialog("confirm") { 
        UtMessageBox.createForConfirm("Download File", "Completed.") 
    }
}
```

また、UtImmortalTask は、メッセージボックス表示に特化したいくつかの拡張関数を持っています。それを使うと、上記のコードは、次のように書けます。

```kotlin
UtImmortalTask.launchTask {
    showConfirmMessageBox("Download File", "Completed.") 
}
```

## メッセージボックス表示用拡張関数

### (1) 確認メッセージ
```kotlin
suspend fun UtImmortalTaskBase.showConfirmMessageBox(
    title:String?, 
    message:String?, 
    okLabel:String= UtStandardString.OK.text)
```
確認(OK)ボタンを１つだけ持つメッセージボックスを表示します。
ユーザーがOKボタンを押下するまでサスペンドします。戻り値はありません。

### (2) Ok/Cancel メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showOkCancelMessageBox(
    title:String?, 
    message:String?, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String= UtStandardString.CANCEL.text) : Boolean
```
OKボタンとキャンセルボタンを持つメッセージボックスを表示します。
ユーザーがOKボタンまたはキャンセルボタンを押下するまでサスペンドし、OKボタンが押下されると trueを、キャンセルボタンを押下すると、false を返します。

### (3) Yes/No メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showYesNoMessageBox(
    title:String?, 
    message:String?, 
    yesLabel:String= UtStandardString.YES.text, 
    noLabel:String= UtStandardString.NO.text) : Boolean
```
OKボタンが Yes, Cancelボタンが No と表記される以外は、OK/Cancelメッセージボックスとまったく同じです。

### (4) ３択メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showThreeChoicesMessageBox(
    title:String?, 
    message:String?, 
    positiveLabel:String, 
    neutralLabel:String, 
    negativeLabel:String) : IUtDialog.Status
```

Positive/Neutral/Negative の３つのボタンを持つメッセージボックスです。例えば、エラー発生時に、\[Retry\] /\[Skip\] / \[Abort\] の３つの選択肢を提示するような場合に使用します。ユーザーの選択結果は、IUtDialog.Status 型の戻り値 (POSITIVE/NEUTRAL/NEGATIVE) として受け取ります。

### (5) リストからの単一選択メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showSingleSelectionBox(
    title:String?, 
    items:Array<String>) : Int
```
リスト項目を文字列の配列として渡します。ユーザーがリスト項目をタップすると、戻り値として、、その項目の配列上のインデックスを返します。メッセージボックス外をタップするなどして、選択がキャンセルされた場合は、-1 を返します。

### (6) ラジオボタン型リストからの単一選択メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showRadioSelectionBox(
    title:String?, 
    items:Array<String>, 
    initialSelection:Int, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String?=UtStandardString.CANCEL.text) : Int
```

`showSingleSelectionBox()` と類似していますが、こちらはリスト上での選択状態をラジオボックスとして表示し、ユーザーがリスト項目をタップしてもメッセージボックスは閉じず、選択状態が変化します。ユーザーがOKボタンを押下すると、最後に選択されていた項目のインデックスが戻り値として返ります。`showSingleSelectionBox()` は、リストからの単純な選択に利用するのに対して、`showRadioSelectionBox()` は、現在の選択値を表示した上で、それを変更させる場合に利用します。

### (7) リストからの複数選択メッセージボックス

```kotlin
suspend fun UtImmortalTaskBase.showMultiSelectionBox(
    title:String?, 
    items:Array<String>, 
    initialSelections:BooleanArray?, 
    okLabel:String= UtStandardString.OK.text, 
    cancelLabel:String?=UtStandardString.CANCEL.text) : BooleanArray
```

`showRadioSelectionBox()` が単一選択用ラジオボタンなのに対して、`showMultiSelectionBox()` は複数選択可能なチェックボックスリストになります。ユーザーが選択した項目は、BooleanArray 型の戻り値として返されます。また、initialSelection を渡すことで、メッセージボックスを表示した直後の選択状態を指定できます。




