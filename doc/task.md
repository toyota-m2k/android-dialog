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



