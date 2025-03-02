# DialogFragment - Lifecycle の苦悩

十数年前に、はじめて Android アプリの開発に携わった当時、Activityを、iOSのViewController, Windows の Window(WFP)かPage(UWP)くらいに考えてた。そして「普通に」作っていたら、デバイスを回転するだけで、「いろいろな不具合」が起きた。ActivityやView のインスタンスがコロコロ入れ替わる。コールバックして帰ってきたら、呼び出し元のActivityがもういない。仕事して帰宅したら、自分の家（だと思っている家）に知らない人が住んでいるような感じだ。

なかでも、ダイアログやメッセージボックスを表示して、ユーザーの判断によって処理を分岐する、というありふれたフローの実装がやたらと難しく、いたるところに不適切な実装が見られる。Kotlinとかコルーチンとか、あるいは、ViewModel, LiveData といった、新しい仕掛けが次々に登場したが、ActivityやFragmentの本質的な気持ち悪さは一貫して変わっていない。

では、ダイアログの結果を待ち受けるにはどうすればよいのか、何が正解なのか？

というわけで、AndroidでDialogをなんとかうまい具合に実装できないモノかと、さまざま模索、試行錯誤した結果、[公式のドキュメント](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) の、[ダイアログのホストにイベントを渡す](https://developer.android.com/develop/ui/views/components/dialogs?hl=ja#PassingEvents) に記載された方法しかない、という結論に達したわけだが、この結論に至るまでの苦悩の過程を遺しておこうと思う。

尚、ダイアログの結果を外部（Activityなど）から受け取ろうとするから苦労するのであって、ダイアログ内でユーザー判断後の処理が閉じている場合、すなわち、onDone()などの中で、ViewModel や、SharedPreferences を書き換える、といった処理なら、何も難しくない。逆に、それで済むように、ロジックを考えることが、一番の解決策かもしれない。

## 【１】 最初の失敗・・・試行錯誤以前に、これはみんな知ってる前提だけど念のため

MainActivity上のSubmitボタン（R.id.submit）が押されたら、「Are you ready? (ok/cancel)」というメッセージボックスを表示し、okが押されたら、次の処理（goAhead()）を実行したい、というとき、
無邪気なAndroid初心者が、AlertDialogを使って、次のように実装してしまった。

```Kotlin
class MessageBox(val message:String, val callback:(Boolean)->Unit): 
    DialogFragment(), DialogInterface.OnClickListener {
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
            // MainActivity上のボタンを押されたらメッセージボックスを表示
            MessageBox("Are you ready?") { result->
                // Okなら、次の処理へ進む
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
当然のことながら、このコードでは、メッセージを表示してから、デバイスを回転したり、他のアプリに切り替えて戻ってきたら死んでしまう。
デバイスの回転、アプリの切り替えに際しては、ActivityやFragment が作り直される。。。つまり、その前後で、別のインスタンスに変わってしまう。だから、callback を呼び出したMainActivityインスタンスは、すでに死んでいる。死んだインスタンスのデータを操作しようとしたら例外が出る

もう一つ、本題から逸れるが、class MessageBox のコンストラクタで、messageやcallbackを渡している点もNG（たぶん、コンパイルエラーかワーニングがでるかと）。一般的に、Activity/Fragmentのコンストラクタに、引数を追加してはいけない。これらのインスタンスが OS によって再構築されるとき、カスタムな引数は無視されるからだ。つまり、この例なら、messageやcallbackが未初期化となってしまう。外部からデータを渡したければ、arguments(Bundle)か 、ViewModel を使うのが定石。

さらにややこしいことに、回転とかアプリ切り替えなどの操作で、

- ActivityやFragmentのインスタンスが再作成される場合（onDestroy/onCreate）
- Fragmentのビューだけ再作成される場合(onDestroyView/onCreateView)
- 同じインスタンスが再利用される場合 (onStop/onStart) 

など、いろいろなパターンがある。これは、Activityの起動オプション（Intent#flagなど）やActivityスタックの状態、操作方法などによっても変化する。つまり、状態遷移のパターンが一通りではないので、ちょっと動いたからと言って安心してはいけない。特に、一番シビアな、インスタンスが再作成されるケースでちゃんと動くかどうかの確認を忘れずに。

#### お節介ついでに：インスタンスが再作成されるケースの確認方法

- システム設定 - 「開発者向けオプション」で、「アクティビティを保持しない」のチェックをONにする。
- ターゲットアプリを起動し、確認したい画面を表示する。

ここから、次のような操作を行う。
- 他のアプリに切り替えて、タスクマネージャから、ターゲットアプリに戻る
- ホーム画面に戻って、ターゲットアプリのアイコンをタップして、ターゲットアプリに戻る

これで、確認したい画面が正しく復元され、その画面上での操作が正常に行われたら OK 

え？この操作で、ダイアログやメッセージボックスが何事もなく閉じたって？
正しく実装できていれば、ダイアログ（Fragment）は OSが復元してくれるはず。
だから、それは閉じたんじゃなくて、異常終了して、OSが親切に再起動してくれたのかもしれないよ。

-----
## 【２】 では、どうするのが正しいの？

[公式のドキュメント](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) で示されている方法だと、こんな感じ。

```Kotlin
class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    // 呼び出し元に結果を返すためのi/f定義
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

    // onAttach で context引数にActivity (implements MessageBoxListener) が渡ってくるので、
    // これをメンバーに覚えておく。
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
            // MainActivity上のボタンを押されたらメッセージボックスを表示
            MessageBox().show(supportFragmentManager, null)
        }
    }

    /**
     * MessageBoxの結果を受け取る (MessageBox.MessageBoxListenerの実装)
     */
    override fun onResult(result:Boolean) {
        if(result) {
            // Okなら嗣の処理へ進む
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
- 結果を受け取るためのi/fを定義し、MainActivityで実装しておいて、
- onAttach()で、MainActivityをメンバに覚えるところ。onAttach()は、Activityが作成されるたびに、新しくAttachされるアクティビティを渡してくれるので、古いActivityを参照し続けることがない。
  
しかし。。。めんどい。
いくつものダイアログを使う場合はどうする？ ダイアログ毎にi/f定義して、すべてを継承する？ または、１つのi/fだけ定義して、onResultで分岐できるようにする？ それって、ほぼ onActivityResult() だよね。（ダイアログの）表示と、結果の処理が泣き別れるパターンになって、書きにくいし読みにくいやつだよね。

この ダイアログ（DialogFragment） から Activity（やFragment) に結果を返す実装のキモは、ダイアログを表示するときにコールバックポイントを渡すのではなく、新しいActivityが生成されるたびに、ダイアログに結果を返す方法を設定すること。もう少し一般化するなら、「**ダイアログが結果を返すタイミングで、正しいコールバックポイントを見つけられるようにする方法**」ということになる。

-----
## 【３】 ViewModel は救世主たりえるか？

ViewModelは、ActivityやFragmentなどの lifecycleOwner に関連付けることができるデータオブジェクトで、ライフサイクルを越えて存在することができ、且つ、同じライフサイクルに属するActivityとFragment間でViewModelインスタンスを共有できる。ダイアログ側から見つけられる。昔、この仕掛けが初めて登場したとき、すべての問題を解決する救世主に見えたものだ。。。

そこで、上の実装例を ViewModel を使って、次のように書き換えてみる。

```Kotlin
class MessageBoxViewModel: ViewModel() {
    var message:String = ""
    var callback:((Boolean)->Unit)? = null
}

class MessageBox(): DialogFragment(), DialogInterface.OnClickListener {
    lateinit var viewModel:MessageBoxViewModel
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProvider(this,ViewModelProvider.NewInstanceFactory())
                    　.get(MessageBoxViewModel::class.java)
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
            // ボタンが押された
            // ViewModelに callbackを設定
            ViewModelProvider(this,ViewModelProvider.NewInstanceFactory())
                .get(MessageBoxViewModel::class.java)
                .callback = {if(it){ goAhead() }}

            // メッセージボックスを表示
            MessageBox("Are you ready?") { result->
                // Okなら次へ進む
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

ダイアログからViewModelを見つけて、callback取り出せるので、うまくいきそうだろ？。。。はい、ダメ。
ダメなのは、ボタンクリックのイベントハンドラ内で、callbackを設定している点。
Activityが再作成されたとき、callbackが更新されないので、破棄済みのActivityにアクセスしてしまう。

そこで、
```Kotlin
class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        // 将来メッセージボックスを表示するかどうかには関係なく、
        // あらかじめ ViewModelに callbackを設定
        ViewModelProvider(this, SavedStateViewModelFactory(this.requireActivity().application, this))
            .get(MessageBoxViewModel::class.java)
            .callback = {if(it){ goAhead() }}
        
        findViewById<Button>(R.id.submit).setOnClickListener {
            // ボタンが押されたら、メッセージボックスを表示
            MessageBox("Are you ready?") { result->
                // Okなら次へ進む
                if(result) { goAhead() }
            }.show(supportFragmentManager, null)
        }
    }
```
のように、ViewModelへのcallback設定を外に出し、onCreate()で必ず実行されるようにすれば、期待通り動作する。[公式](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) と同様に、「新しいActivityが生成されるたびに、ダイアログに結果を返す方法を設定する」ことになるからだ。

うーん、[公式](https://developer.android.com/guide/topics/ui/dialogs?hl=ja) の方法と比べてみると、結果を受け取るコールバックを、DialogFragment::onAttachで設定する代わりに、Activity::onCreateで設定するように変わっただけじゃないか。それどころか、ViewModelが間に挟まった分、逆に面倒になってしまった。

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

    // ダイアログの終了まで待機するためのcontinuationインスタンス
    lateinit var continuation: Continuation<Boolean>

    // メッセージボックスを表示して、ok/cancelで閉じられるまで待機する
    suspend fun showMessage(fm:FragmentManager) {
        suspendCoroutine<Boolean> {
            continuation = it
            show(fm,null)
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        // 結果を返す
        continuation.resume(which==DialogInterface.BUTTON_POSITIVE)
    }
}


class MainActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        findViewById<Button>(R.id.submit).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                // ボタンがクリックされたら メッセージボックスを表示し、結果を返すまで待機(suspend)
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

これは素敵だ。そして、だいたい動く、うん、だいたい。
DialogFragmentインスタンスが再作成されるケース（＝onDestroy/onCreateが呼ばれるケース）で、continuationフィールドがnullになって、NREで死ぬ。残念。だから、待ち受けるcontinuationフィールドはActivity側に持たせて、これをダイアログ側から見つける方法がないと、コルーチンではダメなのだよ。

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
だから、onCreateDialog()で、元のSingleSubjectがそのまま取り出せて、ちゃんと元のObserverを呼び出せる。上記の記事で、「うまくいった」ように見えたのは、この動作。

一方、DialogFragmentインスタンスが再作成されるケースでは、onDestroy()の内部処理あたりで、メモリ上に保持できなくなるBundleの内容をParcelに書き込む処理が行われ、このときはじめてSingleSubjectがシリアライズできないことに気づき、例外を投げて強制終了する。だから、冒頭に書いた「ライフサイクル、状態遷移には、いくつかのパターンがあって、ちょっと動いたからといって安心してはいけない」、というのはこういうことだ。

ちなみに、Androidアプリは強制終了すると、親切にもOSが再起動してくれる。そのため、この実装だと、他のアプリに切り替えて戻ってきたとき、実際にはアプリが１度死んで、再起動されているのだが、見かけ上は「なんかダイアログが閉じた」くらいにしか見えないので、この記事を書いた人は「うまくいっていない」ことに気づかなかったのだと思う。

-----
## 【５】ContinuationをViewModelに覚えておけばイケんじゃね？

ViewModel の生存期間だけコルーチンを実行可能な viewModelScope というのがある。
【４】では、コルーチンの生存期間を意識せず、Activityが直接メッセージボックスを表示＆結果を受け取ろうとして、そのライフサイクルに足元をすくわれたが、これらをすべて、ViewModelに閉じ込めて、viewModelScopeで実行すれば問題は解決するのではないか？というのが、次のテーマ。
ちょっとややこしいけど、こんな実装。


```Kotlin
class MessageBoxViewModel: ViewModel() {
    // ダイアログの終了まで待機するためのcontinuationインスタンス
    lateinit var continuation: Continuation<Boolean>

    // メッセージボックスを表示して、ok/cancelで閉じられるまで待機する
    private suspend fun showMessage(fm:FragmentManager, dlg:MessageBox):Boolean {
        return suspendCoroutine<Boolean> {
            continuation = it
            dlg.show(fm,null)
        }
    }
    
    fun showMessage(fm:FragmentManager) {
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
結局、ActivityやFragmentでダイアログの結果を受け取るには、公式ドキュメントに書いている方法がよい(マシ？)、というのが結論だ。

で、これを、できるだけ抽象化・一般化して、汎用性・拡張性のある仕掛けをつくってみたのが、このライブラリ。

このライブラリは、次の３つのインターフェースを定義するとともに、それぞれのコンクリートクラスを実装している。

- IUtDialog
    ダイアログ、メッセージボックスの i/f
- IUtDialogResultReceptor
    ダイアログから結果を受け取るi/f
- IUtDialogHost
    適切な IUtDialogResultReceptor を探すためのi/f

すなわち、IUtDialogから、（公式ドキュメントの通り）IUtDialogHost を実装したActivity/Fragmentを見つけて、そのi/fから、IUtDialogResultReceptor を取得して、呼び出せるようにした。IUtDialogResultReceptorを実装する（＝データを受け取る）のは、ActivityでもFragmentでもViewModelでも、その他のオブジェクトでも構わない。

ちなみに、ViewModelによるActivity/FragmentとDialog間のデータ共有、という単純な方法も考えられた。しかし、数ある（かもしれない）ViewModelの中から、共有に使う ViewModel を選び出すあたりが、どうしてもダイアログ毎の実装になってしまうし、そもそも、MessageBoxごときにもViewModelが必須というのはいかがなものか、ということで、より柔軟で共通実装をまとめやすい i/f構成とした。

参照：  
[IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./dialog_management.md)


-----
## 【７】 Coroutine 再び

以上は、DialogFragment から、呼び出し元のActivity/Fragment にどうやって結果を返すか？　という観点だったが、もう一つ、重要な処理パターンとして、
「バックグラウンド処理（コルーチン）の途中で、ダイアログ（確認メッセージなど）を表示し、その後の処理を分岐する」というのがある。
例えば、サーバーへのログインを行うため、
「ログインボタンが押されたら、バックグラウンドでサーバーに認証情報を送り、認証が成功すれば、次の画面へ遷移、失敗すればエラーメッセージを表示」
というシナリオを考える。これを、上記のフロー（DialogFragmentの結果は、かならずActivity/Fragmentで受け取る）では、Activityとバックグランドタスクの間を行き来する、かなり複雑なステートマシンを作らないといけない。しかも、ActivityはSuspendしてるかもしれない。。。これはめんどい、しんどい、いやだ。。。

本能が全力で拒絶するので、他の方法を真剣に考えよう。

ここで問題となるのは、バックグラウンド処理として想定するコルーチンの生存期間と、DialogFragment などのライフサイクルが一致しないこと。この例だと、認証が終わったときに、
- 元のActivityと違うActivityに入れ替わっているかもしれない。
- そもそも、Activityが生きていない（他のアプリがアクティブになっている）かもしれない。
 
対策として、まず最初に思いつくのは、ViewModelScope でログインを実行する方法。
これなら、Activityが入れ替わってもViewModelが生きていれば、正しく動く。Activityが死ぬときは、ViewModelも死に、ログイン処理のコルーチンがキャンセルされるので、辻褄は合う。バックグランドタスクなので、【５】の場合と違って、宙ぶらりんで残ってしまうビューはないから大丈夫。
だが、「ログインボタンを押した」というユーザー操作は、まるでなかったことにされてしまうが、それでよいか？
ログイン程度の操作なら、やり直してください、で済むかもしれないが、ウィザードのようなUIで、いろいろ入力した挙句、やり直してください、と言われたら、このクソアプリが、と悪態の一つもつきたくなる。

こういう場合どうやるのが正解なのか？　と思って調べていると、[キャンセルしちゃダメ絶対なコルーチン＆パターン](https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142a) にたどり着いた。超絶要約すると、
- アプリ（プロセス）の生存に関わらず、キャンセルしちゃダメな処理は、[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)を使いましょう。
- アプリが生きている間だけ動作すればいい（アプリ終了時にキャンセルされてもいい、or キャンセルされるべき）処理は、アプリケーションスコープの CoroutineScope(SupervisorJob()) で、コルーチンを使いましょう。
ということ。

先ほどのログインの例などは、２つ目の「アプリケーションスコープ」がぴったりくる。ログインに時間がかかっているとき、他のアプリに切り替えて、しばらくして戻ったらログイン完了している。アプリを終了されたら、ログイン処理はキャンセル。うん、いい感じ。

だが、ちょっと待て。「コルーチンの生存期間と、DialogFragment などのライフサイクルが一致しない」問題が復活しているではないか！

状況を整理しよう。まず、生存期間に関して、２種類のオブジェクトが存在する。
- アプリケーションスコープ上のコルーチンは、ライフサイクルを持たず、死なない。
- DialogFragmentやActivityは、コロコロ死んで、輪廻転生する。

前者を `不死身のオブジェクト` **(ImmortalObject)**、後者を `死すべき定めのオブジェクト` **(MortalObject)** と名付けよう。
これらのオブジェクトの間で、実現したいのは、次の動作。

- ImmortalObject（コルーチン）
  - ダイアログを作る、表示する・・・親となる Activity/Fragment（MoratlObject）を取得する必要がある（※１）。
  - ダイアログの完了を待つ（suspendCoroutine）

- MortalObject（ダイアログ）
  - 呼び出し元のImmortalObjectに完了通知・・・自分を待っているImmortalObject（のsuspendCoroutine）を取得する必要がある（※２）。

したがって、新たに必要となるのは、※１、※２の仕掛け。

1. アプリケーションスコープのコルーチンから、現在アクティブなMortalObject（LifecycleOwner)を取得する（もし存在しなければ、生成されるのを待つ）仕組み
    
    - UtDialogOwnerStack

2. DialogFragmentから、自分の完了を待っている ImmortalObjectを探して、待機を解除する。

   - UtImmortalTaskManager

 
参照：  
[IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法](task.md)

