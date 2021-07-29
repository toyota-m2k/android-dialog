# Android用 ダイアログライブラリ

Androidアプリを開発していて、Windowsアプリのようなダイアログボックスを表示したいと思ったことはないだろうか。
生粋のAndroid市民なら、そんなこと考えもしなのかもしれないが、確認メッセージ(Yes/No)を表示して、Yesの場合だけ処理を継続する、というシーンくらいは珍しくないだろう。
ところが、Activity上のボタンがタップされたとき、確認メッセージを表示して、Yes/No で処理を分岐する、という単純なフローを作るのが、どえらい面倒くさい。

```Kotlin
    class SomeActivity : AppCompatActivity() {
        ...
        fun onButtonTap(v:View) {
            val result = MessageBox("yes or no").show().result()
            if(result) {
                // Yesが選択された
                ...
            }
        }
        ...
    }
```

のように書きたいと思うのだが、残念ながら **できない**。

Activity（やFragment）は、iOSの UIViewController, WindowsのPage(UWP)やWindow(WPF)に相当する位置づけなんだと思うが、
「デバイスの回転やアプリ切り替えのたびに、ActivityやFragmentが、死んだり生き返ったりする」ところが大きく異なっており、
Windows 気分でダイアログを表示すると、思わぬ問題が起きる。

- ダイアログ(DialogFragment)を表示したときと、閉じるときとで、呼び出し元のActivity/Fragmentが、同じインスタンスとは限らない。
- そもそもダイアログ(DialogFragment)自体も、開いたときと閉じるときで同じインスタンスとは限らない。

だから、単純なダイアログ呼び出し/結果の待ち合わせが不可能なのだ。
簡単なケースでは、「結果は待たない、受け取らない」、つまり、OKボタンのClickイベントハンドラなどで、後処理（設定値の保存、反映など）をやってしまう、という解決策が使えるだろう。また、ダイアログからViewModelを更新し、Activityからこれをobserveする、というのが、今風かもしれない。だが、どちらの方法も、処理内容やダイアログ毎に、それぞれの実装が必要で、汎用化するのが難しい。

Ok/Cancelメッセージボックスの入力結果によって分岐する処理フローを実装する場合でも、無理をすれば、メッセージボックスのonOkハンドラ内に、後続処理を書いてしまえば、

詳しくは、[Lifecycle の苦悩](./doc/tribulation.md) を参照いただきたいが、試行錯誤の結果、
Activityなどのライフサイクルオブジェクトが、異なるライフサイクルを持つダイアログ(DialogFragment)から結果を受け取るには、
[公式のドキュメント](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) のに記載されている方法が、思いつく限りの他の方法よりマシだろう、という結論に達した。

この方法の要点は、

- Activity (or Fragment) に結果を受け取るinterfaceを実装する
- ダイアログ側のonAttachで、このinterfaceを実装したActivity/Fragmentを覚えておく
- ダイアログ上のボタンタップなどのイベントで、このinterfaceを呼び出す

つまり、冒頭に掲げたような、一連のフローの中にダイアログを挟むことはできず、ダイアログを開く処理と、ダイアログの結果を受け取った後の処理が別の関数に泣き別れてしまう辛みは、甘んじて受け入れる必要がある。

本ライブラリでは、このような制約を受け入れた上で、ダイアログの結果をActivityやFragmentで受け取る仕掛けを定型化し、ちょっとだけ実装しやすくしたつもりだ。

- IUtDialog（ダイアログ）と、IUtDialogHost（その結果を受取る側）のプロトコルとその基本実装
- バックグラウンドタスクの途中でダイアログやメッセージを表示し、結果を受け取るまで待機するための仕組み

さらに、類似のテーマとして、「他のActivityを呼び出して結果を受け取る」処理についても、小さな仕掛けを追加してみた。

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
- (おまけ)　[Activity を呼び出して結果を受け取る](./doc/activity_call.md)