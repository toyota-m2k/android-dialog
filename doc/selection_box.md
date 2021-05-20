# UtSelectionBox の使い方

UtSelectionBoxは、AlertDialogの標準機能を利用して、複数の選択肢をリスト表示し、アイテムを選択させるUIを提供する。
UiSelectionBoxから結果を受けとるは、[IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./dialog_management.md), または、[IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法](./task.md)に従い、IUtDialogResultReceptorまたは、Continuationから、IUtDialog を取り出し、それぞれのダイアログクラスの型にキャストする。


-----

## ■ UtSingleSelectionBox：単一選択（項目タップでCompleteするタイプ）

UtSingleSelectionBox は、与えられた選択肢をリストとして表示し、リストの項目がタップされると有無を言わさずダイアログが閉じる。
このため、OKボタンは不要（表示できるかもしれないが意味がない）。画面外タップでキャンセル。

```Kotlin
UtSingleSelectionBox.create("タイトル", arrayOf("Chrome", "Safari", "Edge", "Firefox"))
```


### UtSingleSelectionBox.create()

|引数|型|説明|
|:---|:---|:---|
|title|String|タイトル文字列|
|items|Array<String>|選択肢となる文字列配列|



### 値の取得(UtSingleSelectionBoxクラスのフィールド)

|フィールド名|型|説明|
|:---|:---|:---|
|selectedIndex|Int|選択アイテムの、create()に渡したitemsの中のインデックス。キャンセルなら -1|
|selectedItem|String?|選択されたアイテム(文字列)、キャンセルなら null|




-----

## ■ UtRadioSelectionBox：単一選択（ラジオボタンタイプ）

UtRadioSelectionBoxは、与えられた選択肢をラジオボタン付きリストとして表示する。アイテムをタップしたとき、ラジオボタンの選択が移動するだけで、ダイアログは閉じない。
選択後、OKボタンをタップすることにより、選択を完了(Complete)し、ダイアログを閉じる。OKボタンは必須だが、画面外タップでキャンセルするので、キャンセルボタンはオプショナル。

```Kotlin
UtRadioSelectionBox.create("タイトル", arrayOf("Chrome", "Safari", "Edge", "Firefox"), initialSelection=-1)
```

### UtRadioSelectionBox.create()

|引数|型||説明|
|:---|:---|:---|:---|
|title|String||タイトル文字列|
|items|Array<String>|選択肢となる文字列配列|
|initialSelection|Int|Optional|初期選択アイテムのインデックス（-1なら初期選択無し）|
|okLabel|String|Optional|OKボタンのラベル(default:OK)|
|cancelLabel|String?|Optional|Cancelボタンのラベル。nullなら非表示。(default:null)|

### 値の取得(UtRadioSelectionBoxクラスのフィールド)

|フィールド名|型|説明|
|:---|:---|:---|
|selectedIndex|Int|選択アイテムの、create()に渡したitemsの中のインデックス。非選択/キャンセルなら -1|
|selectedItem|String?|選択されたアイテム(文字列)、非選択/キャンセルなら null|

-----

## ■ UtMultiSelectionBox：複数選択（チェックボックスタイプ）

UtMultiSelectionBoxは、UtRadioSelectionBoxに似ているが、ラジオボタンではなく、チェックボックスになっていて、複数アイテムの選択が可能。
選択後、OKボタンをタップすることにより、選択を完了(Complete)し、ダイアログを閉じる。OKボタンは必須だが、画面外タップでキャンセルするので、キャンセルボタンはオプショナル。

|引数|型||説明|
|:---|:---|:---|:---|
|title|String||タイトル文字列|
|items|Array<String>|選択肢となる文字列配列|
|initialSelection|Int|Optional|初期選択アイテムのインデックス。-1なら初期選択無し。(default:-1)|
|okLabel|String|Optional|OKボタンのラベル(default:OK)|
|cancelLabel|String?|Optional|Cancelボタンのラベル。nullなら非表示。(default:null)|

```Kotlin
UtMultiSelectionBox.create("タイトル", arrayOf("Chrome", "Safari", "Edge", "Firefox"), initialSelections=booleanArrayOf(true,false,true,false))
```
### UtMultiSelectionBox.create()

|引数|型||説明|
|:---|:---|:---|:---|
|title|String||タイトル文字列|
|items|Array<String>|選択肢となる文字列配列|
|initialSelections|BooleanArray|Optional|初期選択状態を示すBoolean配列。nullなら初期選択なし。(default:null)|
|okLabel|String|Optional|OKボタンのラベル(default:OK)|
|cancelLabel|String?|Optional|Cancelボタンのラベル。nullなら非表示。(default:null)|

### 値の取得(UtMultiSelectionBoxクラスのフィールド)

|フィールド名|型|説明|
|:---|:---|:---|
|selectionFlags|BooleanArray|選択状態を示すBoolean配列|
|selectedItems|Array<String>|選択されたアイテム(文字列)の配列|
