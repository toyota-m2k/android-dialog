# Lifecycle の苦悩

もう10年くらいAndroidの開発に関わっているが、Android って、iOS や Windows に比べて、「異質（<-全力で控えめにした表現）」だと思う。はじめのころ、Activityを、iOSのViewController, Windows の WindowかPage(UWP)くらいに考えていた。そしてActivityやView がコロコロ入れ替わることに気づく。まるで、大地が消えてなくなるような感じ。コールバックして帰ってきたら、呼び出したActivityが死んでいる。会社から帰宅したら、家がなくなっているような感じ。

とりわけ、ダイアログやメッセージボックスを表示し、ユーザーの判断によって処理を分岐する、というありふれたフローの実装がやたらと難しいし手間がかかる。そのためか、いたるところに不適切な実装が見られる。Kotlinとかコルーチン、あるいは、ViewModel,LiveData,　Flow など、新しい仕掛けが次々に登場したが、ActivityやFragmentの本質的な気持ち悪さは一貫して変わっていない。

では、どうすればよいのか、少しでもマシな方法は何なのか。
というわけで、Dialogを（ほんの少し）作りやすくし、（少しでも）間違った実装をしなくて済むようなライブラリを作ることにした。

このドキュメントでは、今回実装した、UtDialogとその関連クラスの背景を理解できるよう、この設計に至るまでの過程を少し遺しておこうと思う。
## 【１】 始めてAndroidアプリを作る人やってしまいがちな失敗

MainActivity上のSubmitボタン（R.id.submit）が押されたら、「Are you ready? (ok/cancel)」というメッセージボックスを表示し、okが押されたら、次の処理（goAhead()）を実行したい、というとき、
無邪気なAndroid開発初心者は、AlertDialogを使って、次のように実装してしまうかもしれない。というか、Android生まれのAndroid育ちの開発者以外、iOSとかWindowsとかの開発経験者なら、普通、こんな感じに書いてしまうんじゃないかな？

```Kotlin
class MessageBox(val message:String, val callback:(Boolean)->Unit): DialogFragment(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setMessage(message)
            .setNegativeButton(R.string.cancel, this)
            .setPositiveButton(R.string.ok, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        callback(which==DialogInterface.BUTTON_POSITIVE)
    }
}


class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.submit).setOnClickListener {
            MessageBox("Are you ready?") { result->
                if(result) { goAhead() }
            }.show(supportFragmentManager, null)
        }
    }

    private fun goAhead() {
        ... 次の処理
    }
    ...
}

```

当然、このコードだと、メッセージを表示してから、デバイスを回転したり、他のアプリに切り替えて戻ってきたら死んでしまう。Androidあるあるだね。
デバイスの回転、アプリの切り替えに際しては、ActivityやFragment が作り直される。。。つまり、その前後で、別のActivity や Fragmentに変わってしまう、ということを念頭に、上のコードの
何がダメかというと、大きく２つ、

1. onResultに渡したラムダの中から、this@MainActivityを参照している（goAhead()を呼んでいる）こと。
2. MessageBoxクラスのコンストラクタに引数を渡していること。

まず、MessageBoxを表示したときの this@MainActivity は、onResult()が呼ばれたときに生きているとは限らない。そのため、すでに onDestroy()されたMainActivity の goAhead()が呼ばれることになり、「なぜかビューが更新されない」とか、最悪の場合、 IllegalStateException などがスローされて、強制終了したりする。

つぎに、DialogFragmentとかFragment を継承するクラスのコンストラクタに引数を渡すのは、やめた方がいい。Fragmentインスタンス自体が OS によって再構築されるとき、（少なくとも見かけ上）引数無しのコンストラクタが実行される。
例えば、上のMessageBoxでは、messageもonResultも未初期化(==null)となり、それにアクセスしようとした時（＝okボタンかキャンセルボタンをタップしたとき）に即死する。
ちなみに、Fragmentに外部からパラメータを渡したければ、arguments(Bundle)か 、ViewModel を使うのが定石。

ややこしいことに、回転とかアプリ切り替えなどの操作で、

- ActivityやFragmentのインスタンスが再作成される場合（onDestroy/onCreate）
- Fragmentのビューだけ再作成される場合(onDestroyView/onCreateView)
- 再利用される場合（onStop/onStart) 

など、いろいろなパターンがある。これは、Activityの起動オプション（Intent#flagなど）やActivityスタックの状態、操作方法などによって変化する。つまり、状態遷移のパターンが一通りではないので、ちょっと動いたからと言って安心してはいけない。特に、一番シビアな、インスタンスが再作成されるケースでちゃんと動くかどうかの確認を忘れずに。


#### 寄り道：インスタンスが再作成されるケースの確認方法

Android界では常識だから、書かなくてもみんな知ってるよね？

- システム設定 - 「開発者向けオプション」で、「アクティビティを保持しない」のチェックをONにする。
- ターゲットアプリを起動し、確認したい画面を表示する。

ここから、次のような操作を行う。
- 他のアプリに切り替えて、タスクマネージャから、ターゲットアプリに戻る
- ホーム画面に戻って、ターゲットアプリのアイコンをタップして、ターゲットアプリに戻る

これで、確認したい画面が正しく復元され、その画面上での操作が正常に行われたら OK 

え？この操作で、ダイアログやメッセージボックスが消えた？
正しく実装できていれば、ダイアログ（Fragment）は OSが復元してくれるはず。
だから、それは消えたんじゃなくて、異常終了して、OSが親切に再起動してくれたのかもしれないよ。

-----
## 【２】 では、どうするのが正しいの？

[公式のドキュメント](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) で示されている方法だと、こんな感じ。

```Kotlin
class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    interface MessageBoxListener {
        fun onResult(result:Boolean)
    }

    var listener:MessageBoxListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setMessage("Are you ready?")
            .setNegativeButton(R.string.cancel, this)
            .setPositiveButton(R.string.ok, this)
            .create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? MessageBoxListener
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        listener?.invoke(which==DialogInterface.BUTTON_POSITIVE)
    }
}


class MainActivity:AppCompatActivity(), MessageBox.MessageBoxListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.submit).setOnClickListener {
            MessageBox().show(supportFragmentManager, null)
        }
    }

    override fun onResult(result:Boolean) {
        if(result) {
            goAhead()
        }
    }

    private fun goAhead() {
        ... 次の処理
    }
    ...
}

```
ポイントは、
- 結果を受け取るためのi/fを定義し、Activityで実装しておく。
- onAttach()で、そのActivityをメンバに覚えておく
- これで、ボタンクリックのタイミングで、必ず「結果を受け取るためのi/f」が使用可能となる。
  
うーん、めんどい。
いくつものダイアログを使う場合はどうする？ onResultで分岐できるようにする？
結局、それって、onActivityResult() と同じく、（ダイアログの）表示と、結果の処理が泣き別れるパターンで、書きにくいし読みにくい、イケてない実装になるよね。

この ダイアログ（DialogFragment） から Activity（やFragment) に結果を返す実装のキモは、Activityから、ダイアログにコールバックポイントを渡すのではなく、「**ダイアログ側から何らかの方法で、コールバックポイントを見つけられるようにする**」という点。これを押さえつつ、別の方法を考えてみよう。

-----
## 【３】 ViewModel は救世主たりえるか？

ViewModelは、ActivityやFragmentなどの lifecycleOwner に関連付けることができるデータオブジェクトで、ライフサイクルを越えて存在することができ、且つ、同じライフサイクルに属するActivityとFragment間でViewModelインスタンスを共有できる。ダイアログ側から見つけられる。昔、この仕掛けが初めて登場したとき、すべての問題を解決する救世主に見えたものだ。。。

そこで、上の実装を ViewModel を使って、次のように書き換えてみる。

```Kotlin
class MessageBoxViewModel: ViewModel() {
    var message:String = ""
    var callback:((Boolean)->Unit)? = null
}

class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    lateinit var viewModel:MessageBoxViewModel
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(this,ViewModelProvider.NewInstanceFactory()).get(MessageBoxViewModel::class.java)
        return AlertDialog.Builder(activity)
            .setMessage(viewModel.message)
            .setNegativeButton(R.string.cancel, this)
            .setPositiveButton(R.string.ok, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        viewModel.callback?.invoke(which==DialogInterface.BUTTON_POSITIVE)
    }
}

class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.submit).setOnClickListener {
            ViewModelProvider(this,ViewModelProvider.NewInstanceFactory())
            .get(MessageBoxViewModel::class.java)
            .callback = {if(it){ goAhead() }}
            MessageBox("Are you ready?") { result->
                if(result) { goAhead() }
            }.show(supportFragmentManager, null)
        }
    }

    private fun goAhead() {
        ... 次の処理
    }
    ...
}

```

ダイアログからViewModelを見つけて、callback取り出せるので、一見、うまくいきそうだろ？。。。わざと間違えてみた。
ダメなのは、ボタンクリックのイベントハンドラ内で、callbackを設定している点。
Activityが再作成されたとき、callbackが更新されないので、破棄済みのActivityにアクセスしてしまう。

```Kotlin
class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        ViewModelProvider(this, SavedStateViewModelFactory(this.requireActivity().application, this))
        .get(MessageBoxViewModel::class.java)
        .callback = {if(it){ goAhead() }}
        findViewById<Button>(R.id.submit).setOnClickListener {
            MessageBox("Are you ready?") { result->
                if(result) { goAhead() }
            }.show(supportFragmentManager, null)
        }
    }
```
のように、ViewModelへのcallback設定を外に出し、onCreate()で必ず実行されるようにすれば、期待通り動作する。
つまり、このMessageBoxを使うかどうかに関わらず、常に、このコールバックをViewModelに登録しておく必要があるわけだ。
他にメッセージボックスやダイアログが複数あるなら、
やはり、onCreate()で、それらのコールバックを（使うかどうかに関わらず）すべて登録しておく必要がある。うーん、[公式](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) の推奨する Activityにinterfaceを仕込む方法より面倒になってないか？ メッセージボックスみたいなしょぼいダイアログにまでViewModelを持たせるのも、なんだかなぁ。

-----
## 【４】 Coroutine、真打登場!?

Coroutineが登場したとき、

```Kotlin
class SomeDialog {
    suspend fun showDialog():Result
}

class MainActivity {
    suspend fun someAction() {
        val result:Result = SomeDialog().showDialog()
        when(result) {
            ok-> {}
            else-> {}
        }
    }
}
```

みたいなコードが書けるんじゃないか、という期待を持った人も多いんじゃないかな。実際、Windows+C# なら async/await で普通に書いてるし。
例えば、次のようなコード。

```Kotlin
class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setMessage("Are you ready?")
            .setNegativeButton(R.string.cancel, this)
            .setPositiveButton(R.string.ok, this)
            .create()
    }

    lateinit var continuation: Continuation<Boolean>
    suspend fun showMessage(fm:FragmentManager) {
        suspendCoroutine<Boolean> {
            continuation = it
            show(fm,null)
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        continuation.resume(which==DialogInterface.BUTTON_POSITIVE)
    }
}


class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.submit).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                var result = MessageBox().showMessage(supportFragmentManager)
                if(result) {
                    goAhead()
                }
            }
        }
    }

    private fun goAhead() {
        ... 次の処理
    }
    ...
}

```

これは素敵だ。だいたい動く、だいたい。。。
DialogFragmentインスタンスが再作成されるケース（＝onDestroy/onCreateが呼ばれるケース）で、continuationフィールドがnullになって、NREで死ぬ。残念。continuationフィールドに相当するオブジェクトをダイアログ側から見つける方法がないと、コルーチンではダメなのだよ。

それでも諦めきれず、いろいろ調べてみると、
[Coroutine時代のDialogFragment](https://qiita.com/idaisuke/items/b4f3c2e0a872544b97d0) という記事で、「Rxとコルーチンでダイアログの待ち合わせに成功した」というのを見つけた。これだ！
で、この記事のアイデアは、

- （continuation ではなく）Rxの SingleSubject を使って、完了を通知する。
- そのままだと、デバイス回転で、SingleSubjectがクリアされる。
- 「幸い SingleSubject は Serializable なフィールドしか持っていない」ので、Serializable にできる。
- だから、onSaveInstanceState で Bundleに保存し、onCreateDialog　で復元できて、うまくいった。
  
というもの。

。。。ん？ なんかオカシくね？ SingleSubject が 「Serializableなフィールドしか持っていない」 と書いているけど、Observerたちは？
半信半疑（というか、一信九疑くらい）で試してみた。確かに「回転」はイケる。だが、DialogFragmentインスタンスが再作成されるケースで例外が出て死ぬ。まぁ予想通り。

なにが起こっているかというと、まず「回転」の場合は、Fragmentインスタンスの再作成までは不要と判断したOSは、ビュー（DialogFragmentならDialogインスタンス）だけ再作成する。つまり、onDestroyView/onCreateView のライフサイクルをたどるパターンだ。このとき、
onSaveInstanceState()でBundleに put された SingleSubject は、実際にはシリアライズ(=Parcelへの書き込み）されず、オンメモリのまま、onCreate() や onCreateDialog() に渡ってくる。
だから、onCreateDialog()で、元のSingleSubjectがそのまま取り出せて、ちゃんと元のObserverを呼び出せる。

一方、DialogFragmentインスタンスが再作成されるケースでは、onDestroy()の内部処理あたりで、メモリ上に保持できなくなるBundleの内容をParcelに書き込む処理が行われ、このときはじめてSingleSubjectがシリアライズできないことに気づき、例外を投げて強制終了する。だから、冒頭に書いた「ライフサイクル、状態遷移には、いくつかのパターンがあって、ちょっと動いたからといって安心してはいけない」、というのはこういうことだ。

-----
## 【５】ContinuationをViewModelに覚えておけばイケんじゃね？

ViewModel の生存期間だけコルーチンを実行可能な viewModelScope というのがある。
【４】では、コルーチンの生存期間を意識せず、Activityが直接メッセージボックスを表示＆結果を受け取ろうとして、そのライフサイクルに足元をすくわれたが、これらをすべて、ViewModelに閉じ込めて、viewModelScopeで実行すれば問題は解決するのではないか？というのが、次のテーマ。
ちょっとややこしいけど、こんな実装。


```Kotlin
class MessageBoxViewModel: ViewModel() {
    lateinit var continuation: Continuation<Boolean>
    
    suspend fun showMessage(fm:FragmentManager, dlg:MessageBox):Boolean {
        return suspendCoroutine<Boolean> {
            continuation = it
            dlg.show(fm,null)
        }
    }
    
    fun onSubmit(fm:FragmentManager) {
        viewModelScope.launch { 
            val result = showMessage(fm, MessageBox())
            if(result) {
               goAhead() 
            }
        }
    }
    
    fun goAhead() {
        ... 次の処理
    }
}

class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    lateinit var viewModel:MessageBoxViewModel
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(this,ViewModelProvider.NewInstanceFactory()).get(MessageBoxViewModel::class.java)
        return AlertDialog.Builder(activity)
            .setMessage("Are you ready?")
            .setNegativeButton(R.string.cancel, this)
            .setPositiveButton(R.string.ok, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        viewModel.continuation.resume(which==DialogInterface.BUTTON_POSITIVE)
    }
}

class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        findViewById<Button>(R.id.submit).setOnClickListener {
            val viewModel = ViewModelProvider(this,ViewModelProvider.NewInstanceFactory()).get(MessageBoxViewModel::class.java)
            viewModel.showMessage(supportFragmentManager)
        }
    }
    ...
}

```

メッセージボックスの表示～その後の処理分岐をすべて、ViewModelに持たせ、viewModelScope でダイアログの表示、結果の取得、その後の処理を行う。こうすれば、ViewModelが生きている限り、continuationが失われることはなく、ViewModelが死ぬときに、viewModelScopeも死んで、このコルーチンがキャンセルされるから、不正な continuation にアクセスすることもあり得ない。完璧！！

ああああ！！！！

「ViewModelが死ぬとコルーチンがキャンセルされる」。。。よさそうに聞こえるけど、
だめなんだよ、これでは。Activityが死ぬと、そのライフサイクルに属しているViewModelも死ぬ。そして、Activityは復活する。そうすると、ViewModelや、DialogFragmentも復活する。けれども、キャンセルされたコルーチンは復活しない！！　その結果、だれも終了を待っていないダイアログが取り残されるのだ。そして、MessageBoxインスタンスのcontinuationフィールドは未初期化の状態となり、OKボタンを押すと、resume()の行で IlleagalStateException が出て死ぬ。無念。

Coroutineとか、ViewModelとか、周辺事情は日々進化しているけど、Androidの土台が追いつけていけない、てことなのか。。。

-----
## 【６】 結局どうしたか

思いつくこと、できそうなことは、すべて試した（と思う）。
その結果、誠に遺憾ながら、公式ドキュメントに書いている方法が一番マシ、という結論しか導けなかった。
ならば、これを、できるだけ抽象化・一般化して、汎用性・拡張性のある仕掛けを用意しようじゃないか、というのが最終的なアプローチ。
できれば、少しでも書きやすく、読みやすいように。

- IUtDialogHost
  
[IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./dialog_management.md)

-----
## 【７】 Coroutine 再び

以上は、DialogFragment から、呼び出し元のActivity/Fragment にどうやって結果を返すか？　という観点だったが、もう一つ、重要な処理パターンとして、
「バックグラウンド処理（コルーチン）の途中で、ダイアログ（確認メッセージなど）を表示し、その後の処理を分岐する」というのがある。
例えば、サーバーへのログインを行うため、
「ログインボタンが押されたら、バックグラウンドでサーバーに認証情報を送り、認証が成功すれば、次の画面へ遷移、失敗すればエラーメッセージを表示」
というシナリオを考える。これを、上記のフロー（DialogFragmentの結果は、かならずActivity/Fragmentで受け取る）では、かなり複雑なステートマシンを作らないといけない。現実的ではない。困った。

ここで問題となるのは、バックグラウンド処理として想定するコルーチンの生存期間と、DialogFragment などのライフサイクルが一致しないこと。この例だと、認証が終わったときに、
- 元のActivityと違うActivityに入れ替わっているかもしれない。
- そもそも、Activityが生きていない（他のアプリがアクティブになっている）かもしれない。
 
対策として、まず最初に思いつくのは、ViewModelScope でログインを実行する方法。
これなら、Activityが入れ替わってもViewModelが生きていれば、正しく動く。Activityが死ぬときは、ViewModelも死に、ログイン処理のコルーチンがキャンセルされるので、辻褄は合う。【５】の場合と違って、宙ぶらりんで残ってしまうビューもない。
だが、「ログインボタンを押した」というユーザー操作は、まるでなかったことにされてしまうが、それでよいか？
ログイン程度の通信なら、やり直してください、で済むかもしれないが、大きいファイルのDLとか、もっと時間のかかる処理だったら？

こういう場合どうやるのが正解なのか？　と思って調べていると、[キャンセルしちゃダメ絶対なコルーチン＆パターン](https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142a) にたどり着いた。超絶要約すると、
- アプリ（プロセス）の生存に関わらず、キャンセルしちゃダメな処理は、[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)を使いましょう。
- アプリが生きている間だけ動作すればいい（アプリ終了時にキャンセルされてもいい、or キャンセルされるべき）処理は、アプリケーションスコープの CoroutineScope(SupervisorJob()) で、コルーチンを使いましょう。
ということ。

先ほどのログインの例などは、このアプリケーションスコープがぴったりくるな。ログインに時間がかかっているとき、他のアプリに切り替えて、しばらくして戻ったらログイン完了している。アプリを終了されたら、ログイン処理はキャンセル。うん、いい感じ。
ちょっと待て。「コルーチンの生存期間と、DialogFragment などのライフサイクルが一致しない」問題が復活しているではないか！

状況を整理しよう。まず、生存期間に関して、２種類のオブジェクトが存在する。
- アプリケーションスコープ上のコルーチンは、ライフサイクルを持たず、死なない。
- DialogFragmentやActivityは、コロコロ死んで、輪廻転生する。

前者を `不死身のオブジェクト` **(ImmortalObject)**、後者を `死すべき定めのオブジェクト` **(MortalObject)** と名付けよう。
これらのオブジェクトの間で、実現したいのは、次の動作。

- ImmortalObject（コルーチン）
  - ダイアログを作る、表示する
  - このとき親となる Activity/Fragment（MoratlObject）を取得する必要がある（※１）。
  - ダイアログの完了を待つ（コルーチンのContinuation）

- MortalObject（ダイアログ）
  - 完了を呼び出し元のImmortalに通知する。
  - このとき、自分を待っているImmortalオブジェクト（のContinuation）を探す必要がある（※２）。

したがって、新たに必要となるのは、※１、※２の仕掛け。

1. アプリケーションスコープのコルーチンから、現在アクティブなMortalObject（LifecycleOwner)を取得する（もし存在しなければ、生成されるのを待つ）仕組み
    
    - UtDialogOwnerStack

2. DialogFragmentから、自分の完了を待っている ImmortalObjectを探して、待機を解除する。

   - UtImmortalTaskManager

 
[IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法](task.md)

