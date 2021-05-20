# Android用 ダイアログライブラリ

Androidアプリを開発していて、Windowsアプリのようなダイアログボックスを表示したいと思ったことはないだろうか。
善良なAndroid市民はそんなこと考えないのかもしれないが、確認メッセージ(Yes/No)を表示したい、というシーンくらいは珍しくないだろう。
ところが、Activity上のボタンがタップされたとき、確認メッセージを表示して、Yes/No で処理を分岐する、という単純なフローを作るのが、どえらい面倒くさい。

```Kotlin
    fun onButtonTap(v:View) {
        val result = MessageBox("yes or no").show().result()
        if(result) {
            // Yesが選択された
            ...
        }
    }
```

と書きたいところだが、このようなことは **できない**。

詳しくは、[Lifecycle の苦悩](./doc/tribulation.md) を参照いただきたいが、Activityなどのライフサイクルオブジェクトがダイアログ(DialogFragment)から結果を受け取るための、唯一、正しい方法は、
- Activity (or Fragment or ViewModel) に結果を受け取るinterfaceを実装する
- ダイアログ側に、このinterfaceを見つけ出す手段を実装する
- ダイアログ上のボタンタップなどのイベントで、このinterfaceを呼び出す
  
だけだと思われる。
つまり、ダイアログ側だけでなく、その結果を受け取る側（Activityなど）にも、事前の準備（受け取る仕掛けの実装）が必要なので厄介なのだ。


本ライブラリでは、このようなダイアログの実装に対する負担を少しでも軽減するため、以下のフレームワークを構築した。
- IUtDialog（ダイアログ）と、IUtDialogHost（その結果を受取る側）のプロトコルとその基本実装
- バックグラウンドタスクの途中でダイアログやメッセージを表示し、結果を受け取るまで待機するための仕組み

----
## 詳細情報
### ダイアログクラスの作り方、使い方
- [メッセージボックス](./doc/message_box.md)
- [リストからのアイテム選択](./doc/selection_box.md)
- [カスタムダイアログ](./doc/custom_dialog.md)
- [UtDialog リファレンス](./doc/dialog_reference.md)

### ダイアログの結果を受け取る方法
- [IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./doc/dialog_management.md)
- [IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法](./doc/task.md)
