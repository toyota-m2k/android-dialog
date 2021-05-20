# UtMessageBox の使い方

UtMessageBox を使えば、Android標準のAlertDialogを利用したダイアログ（メッセージボックス、リストからの選択など）が利用できる。
UtMessageBoxから結果を受けとるは、[IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./dialog_management.md), または、[IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法](./task.md)に従い、IUtDialogResultReceptorまたは、Continuationから、IUtDialog を取り出し、IUtDialog.status (IUtDialog.Status型)をチェックする。ok/yes などは POSITIVE, cancel/no などは NEGATIVE, それ以外は、NEUTRAL を値として持つ。

## 確認メッセージ

OK（確認）ボタンだけを持ったメッセージを表示する場合は、次のようにする。

```Kotlin
UtMessageBox.createForConfirm("タイトル", "メッセージ").show(activity, "utmessage")
```

### UtMessageBox.createForOkCancel()メソッド

|引数|型||説明|
|:---|:---|:---|:---|
|title|String?||タイトル（不要ならnull）|
|message|String?||メッセージ|
|okLabel|String?|optional|default OK|

##
## OK/Cancel

メッセージを表示し、OK か、Cancel かの二択を迫るやつ。

```Kotlin
UtMessageBox.createForOkCancel("タイトル", "メッセージ").show(activity, "utmessage")
```

### UtMessageBox.createForOkCancel()メソッド

|引数|型||説明|
|:---|:---|:---|:---|
|title|String?||タイトル（不要ならnull）|
|message|String?||メッセージ|
|okLabel|String?|optional|default OK|
|cancelLabel|String?|optional|default CANCEL|

## Yes/No

Ok/Cancel の亜種。okLabel と cancelLabel を YES/NO に変えるのと同じ。実際、そういう実装になっている。

```Kotlin
UtMessageBox.createForYesNo("タイトル", "メッセージ").show(activity, "utmessage")
```

### UtMessageBox.createForYesNo()メソッド
|:---|:---|:---|:---|
|title|String?||タイトル（不要ならnull）|
|message|String?||メッセージ|
|yesLabel|String?|optional|default YES|
|noLabel|String?|optional|default NO|

### ３択メッセージボックス

AlertDialog 的には、Ok/Cancel/Abort など、ボタンを３つまで表示できるが、createForOkCancel 的なヘルパメソッドは用意していない。これは、単に３つ目の標準ラベル（引数省略時のラベル）を思いつかなかったからだ。
この３つ目のボタンは、AlertDialog では、NEUTRAL_BUTTON と呼ばれ、これが選択されたとき IUtDialog は、IUtDialog.Status.NEUTRAL を返す。また、UtMessageBoxのラベル名は、otherLabel である。
例えば、Ok/Cancel/Abort というラベルを付けるには、次のようにする。

```Kotlin
UtMessageBox().apply {
    title = "タイトル"
    message = "メッセージ"
    okLabel = "Ok"
    cancelLabel = "Cancel"
    otherLabel = "Abort"        // Neutralボタンのラベル
}.show(activity, "utmessage")
```

### ４択以上はどうする？

あまり知られていないが(※）、AlertDialog は、ボタン以外に、リストからの選択もサポートしている。
[UtSelectionBox](./selection_box.md) を使うことで、IUtDialogの様式によるリストからの選択操作が可能となる。

※個人の感想です。
