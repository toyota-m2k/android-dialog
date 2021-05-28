# IUtDialogをバックグラウンドスレッド（Coroutine）から利用する方法

Activity （あるいは、Fragment, ViewModel）からダイアログを開いて、Activity（あるいは、Fragment, ViewModel）で、その結果を受取る、というフローについては、[IUtDialog の結果を Activity/Fragment/ViewModel で受け取るには](./dialog_management.md) で説明した。

一方、バックグラウンドでログインやファイルをダウンロードなどの通信を行い、エラー通知や、ユーザー確認などのために、そのバックグラウンドタスクから、ダイアログやメッセージを表示したい、というケースがある。UtImmortalTaskManager は、このように、ライフサイクルに依存しないタスクから、ダイアログやActivityといったライフサイクルを持つオブジェクトを操作するための仕組みを提供する。

尚、本ライブラリでは、これらを以下のように名付けて区別する。

- **不死身のオブジェクト** (ImmortalObject)
    
    ライフサイクルを持たず、プロセス実行中は、処理が終了するまで死なない。

**死すべき定めのオブジェクト** (MortalObject)

    ActivityやFragmentなど、コロコロ死んでは、輪廻転生するヤツ。

通常、ImmortalObject と MortalObject間のプロトコルを実装済みの、Activityクラス（UtMortalActivity）と、タスククラス（UtMortalTaskBase)を使うので、UtImmortalTaskManager を直接操作する必要はない。

## 実装方法

1. バックグラウンド処理を行うための、UtImmortalTaskBase を派生するタスククラスを準備する。

   ```Kotlin
   class SampleTask : UtImmortalTaskBase(TASK_NAME) {
    companion object {
        // タスク名（タスクをグローバルに識別するとともに、
        // このタスク内からダイアログを開くときのタグ名としても利用される。
        val TASK_NAME = SampleTask::class.java.name
    }

    // バックグラウンドタスクの中身
    // UIスレッドから呼ばれるので、ブロッキングするなら適宜coroutineContextを切り替えること。
    override suspend fun execute(): Boolean {
        logger.info("waiting")
        delay(3*1000)
        logger.info("dialog")
        val r = showDialog(TASK_NAME) {
            UtMessageBox
            .createForOkCancel("Suspend Dialog", "Are you ready?")
        }
        return r?.status?.ok ?: false
    }
   }
   ```
    この例では、タスク開始後、３秒間待機したのち、ok/cancel メッセージボックスを表示し、ok されたら true, それ以外なら　false を返す。


    ```Kotlin
    fun showDialog(tag:String, dialogSource:(UtDialogOwner)-> IUtDialog):IUtDialog?
    ```
    は、UtImmortalTaskBase のメソッドで、ダイアログを表示してから、ダイアログが閉じる(completeする)まで待機し、そのダイアログインスタンスを返す。

2. Activity を　UtMortalActivity の派生クラスとし、immortalTaskNameListをオーバーライドする。
    ```Kotlin
    class MainActivity : UtMortalActivity() {
        override val immortalTaskNameList: Array<String> = arrayOf(SampleTask.TASK_NAME)

        // Activityがタスクの完了を知る必要があるなら、notifyImmortalTaskResult()もオーバーライド。
        // 例えば、タスクが完了したらアクティビティを終了するなら、こんな感じ。
        override fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {
            if(taskInfo.state.value==UtImmortalTaskState.COMPLETED) {
                finish()
            }
        }
        
    ```


## プロトコルの説明

UtImmortalTaskBase, UtMortalActivity は、UtImmortalTaskManager を使った immortal/mortal 間の典型的な処理を実装するが、以下の規約に従うことで、そのフローをカスタマイズしたり、新たに書き起こすことができる。

1. Immortal Task は ユニークに識別可能なキー（名前）を持つ

    companion object で定義した、TASK_NAME がこれに相当。

2. Mortal Object は、連携する Immortal Task を UtImmortalTaskManager に登録して監視する。

    UtImmortalTaskManager.registerTask()を呼び出すと、タスクテーブルに（なければ）タスクエントリーを追加し、ITaskInfo を返す。Mortal Object は、ITaskInfo.state (LiveData) を observe()することで、タスクの完了を検出する。タスクの結果は、ITaskInfo を介して取得できる。

    UtMortalActivity では、immortalTaskNameList をオーバーライドして、連携する Immortal Task のキーを列挙できるようにしておけば、onResume()で、これらのタスク登録処理を行い、state監視を開始する。タスクの完了は、オーバーライドした notifyImmortalTaskResult()で受け取ることができる。

3. Immortal Task は、開始時にタスクエントリにアタッチし、終了時にデタッチする。

    上記のタスクの登録(registerTask)は、エントリの予約であり、タスクそのものの情報は、Immortal Taskの開始時に attachTask() し、タスク終了時に detachTask()する。

    UtImmortalTaskBaseクラスを使うと、fire()メソッドで、attachTask/detachTaskを自動化できる。
    
    タスクエントリが作成されていないと、attachTask()は失敗する。Mortal Object の onResume()などのタイミングでreserveTask()することにより、エントリが登録されることを想定しているが、Mortal Object の開始前からタスクを実行しておくような場合は、ImmortalTaskBase継承クラスのコンストラクタなどで、UtImmortalTaskManager.createTask() を呼んでおく。


4. Immortal Task 内からダイアログを表示するときは、suspend fun で同期的に呼び出せる。
   
   UtDialogBaseのimmortalTaskName プロパティにタスク名をセットしてshow()すると、ダイアログのcomplete時に、この名前のタスクに対して、IUtImmortalTask.resumeTask(IUtDialog)を呼び出す規約としている。

   ImmortalTaskBaseでは、suspendCoroutine で待機し、ダイアログを呼び出し、resumeTaskで、待機を解除する一連の処理を、showDialog()メソッドで実装しており、同期的にダイアログの結果を受け取ることができる。
   
5. Immortal Task の登録解除

    Immortal Task 側からの detachTask()は、attachTask()前の状態（エントリが予約された状態）に戻すだけであり、タスクエントリは削除されない。
 
   タスクエントリは、Mortal Object側から、UtImmortalTaskManager.disposeTask() を呼び出すことで完全に削除されるが、アプリ実行中に複数回実行されるタスクや、Mortal Object 終了後も生き残るタスクの場合は、登録解除しない、という選択肢もある。 タスクエントリ（ITaskInfo）は、非常に小さいデータクラスであり、（resultに妙な参照をもったデータをセットしない限り）アプリ終了まで残っても支障はない。

   デフォルトで、UtMortalActivity は、finish されるとき（回転などによるonDestroyではない）に、disposeTask()する。アクティビティのfinish時にタスクエントリを残す必要がある場合は、queryDisposeTaskOnFinishActivity()をオーバーライドして、false を返すようにする。






    
