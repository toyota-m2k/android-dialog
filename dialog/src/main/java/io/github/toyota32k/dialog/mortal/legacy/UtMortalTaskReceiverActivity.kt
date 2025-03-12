package io.github.toyota32k.dialog.mortal.legacy

import io.github.toyota32k.dialog.mortal.UtMortalActivity
import io.github.toyota32k.dialog.task.UtImmortalTaskManager

/**
 * UtDialog設計当初は、生存期間の長いImmortalTask の結果を、短命な MortalActivity で受け取ることを目指し、
 * 任意のタイミングで開始したタスクの結果をIUtImmortalTaskResultReceiver（を継承するActivity）が受け取る仕組みを実装しました。
 * この仕組みを使うには、次の UtMortalTaskReceiverActivity を継承した Activityを実装して、
 * reservedImmortalTaskNames と、onImmortalTaskResult をオーバーライドします。
 * reservedImmortalTaskNames に登録された名前のタスクを実行すると、そのタスクが終了するときに、onImmortalTaskResultが呼び出されます。
 *
 * しかし、実際に使ってみると、Taskの結果はTask内で完結した方が処理が１か所にまとめられ、保守性・可視性がよいし、
 * 必要ならViewModel 経由で、Flow/LiveData の形でActivityに渡すこともできるので、
 * この仕掛けを利用する機会はほとんどありませんでしたし、今後も必要になる見込みもないので、
 * v5以降は、コードの見通しをよくするため、これに関わる実装を分離して、legacy namespace に塩漬けにします。
 */
abstract class UtMortalTaskReceiverActivity(val mortalStaticTaskKeeper: UtMortalStaticTaskKeeper = UtMortalStaticTaskKeeper())
    : UtMortalActivity(mortalStaticTaskKeeper), IUtImmortalTaskResultReceiver {

    /**
     * 予約するタスクの名前を登録します。
     * 少なくとも onResume() が呼ばれる前に登録するようにします。
     * この例では、コンストラクタで登録しています。
     */
    override val reservedImmortalTaskNames: Array<String>
            = arrayOf("sample-task-1", "sample-task-2")
    /**
     * ImmortalTask の結果を受け取るハンドラ
     * task.name でタスクを識別して、タスクの結果(task.result) を処理します。
     * このメソッドは、Activity が活動している（onResume ... onPause）状態で呼び出されることが保証されます。
     */
    override fun onImmortalTaskResult(task: UtImmortalTaskManager.ITaskInfo) {
        when(task.name) {
            "sample-task-1"-> { /*do something with task.result. */}
            "sample-task-2"-> { /*do something with task.result. */}
        }
    }
}