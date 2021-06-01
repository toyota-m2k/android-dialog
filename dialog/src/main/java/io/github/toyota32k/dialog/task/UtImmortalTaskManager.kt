package io.github.toyota32k.dialog.task

import androidx.lifecycle.*
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.*
import java.io.Closeable
import java.lang.IllegalStateException

/**
 * 世界の終り（アプリ終了）まで生きることができる不死身のタスククラス
 */
object UtImmortalTaskManager : Closeable  {
    val logger = UtLog("UtTask")

    /**
     * タスク情報i/f
     */
    interface ITaskInfo {
        val name:String
        val state:LiveData<UtImmortalTaskState>
        val task:IUtImmortalTask?
        val result:Any?
    }

    /**
     * タスク情報i/f の実装クラス
     */
    private data class TaskEntry(override val name:String):ITaskInfo {
        override val state = MutableLiveData(UtImmortalTaskState.INITIAL)
        override var task:IUtImmortalTask?=null
        override var result:Any?=null
    }

    // タスクテーブル
    private val taskTable = mutableMapOf<String,TaskEntry>()
    // Activity/Fragment のキャッシュ的なやつ
    private val dialogOwnerStack = UtDialogOwnerStack()

    /**
     * ImmortalTask用コルーチンスコープ
     */
    val immortalTaskScope:CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Activity/Fragment を取得するための仕掛け i/f
     */
    val mortalInstanceSource:IUiMortalInstanceSource = dialogOwnerStack

    /**
     * 名前をキーにタスクを取得
     */
    fun taskOf(name:String):ITaskInfo? {
        return taskTable[name]
    }

    /**
     * タスクを構築してテーブルに登録する
     */
    fun createTask(name:String):ITaskInfo {
        return TaskEntry(name).apply { taskTable[name]=this }
    }

    fun registerOwner(owner:UtDialogOwner) {
        dialogOwnerStack.push(owner)
    }

    /**
     * タスクテーブルへのエントリ作成
     *
     * タスクテーブルに登録済みなら、そのタスクを返す。未登録なら作成して登録して返す。
     * attachTask()の前に実行しておく。Activity/Fragmentと協調する場合は、onResumed()から呼び出す。
     */
    fun reserveTask(name:String) : ITaskInfo {
        return taskTable[name] ?: createTask(name)
    }

    /**
     * Activity/Fragmentの切り離し
     *
     * Activity/Fragment #onPaused()から呼び出したらどうや。
     * dialogOwnerStackから削除、オブザーバーの登録解除のみ。
     * タスクテーブルからの削除は行わない。
     * タスクテーブルから削除する場合は disposeTask() を呼ぶこと。
     */
//    fun onOwnerPaused(name:String, owner:UtDialogOwner) {
//        dialogOwnerStack.remove(owner)
//        taskTable[name]?.state?.removeObservers(owner.lifecycleOwner)
//    }

    /**
     * 実行中タスクのタスクテーブルへの登録
     *
     * タスクを起動するときに呼び出す。
     * 実行中タスクをタスクテーブルに登録しておくことで、ダイアログなど、どこからでも、そのタスクを取り出せるようにする。
     *
     * @throw IllegalStateException
     *  - これ以前に、reserveTask()されていない。
     *  - 同一タスクの多重起動しようとした。
     */
    fun attachTask(task:IUtImmortalTask) {
        logger.debug(task.taskName)
        val entry = taskTable[task.taskName] ?: throw IllegalStateException("no such task: ${task.taskName}")
        if(entry.task!=null) throw IllegalStateException("task already running: ${task.taskName}")
        entry.state.value = UtImmortalTaskState.RUNNING
        entry.task = task
    }

    /**
     * 実行中タスクをタスクテーブルから切り離す。
     *
     * タスク終了後に呼び出す。
     * タスク終了後もタスクの結果(state/result)はtaskOfで取り出せる。
     * ある意味、意図的にリークさせているので、task.taskResultには、最小限の情報だけを残すようにする。
     * 本当に不要になれば、disposeTask()を呼び出す。
     */
    fun detachTask(task:IUtImmortalTask, succeeded:Boolean) {
        logger.debug()
        val entry = taskTable[task.taskName] ?: return
        entry.result = task.taskResult
        entry.state.value = if(succeeded) UtImmortalTaskState.COMPLETED else UtImmortalTaskState.ERROR
        entry.task = null
    }

    /**
     * タスクテーブルからエントリを削除する。
     */
    fun disposeTask(name:String, owner:UtDialogOwner?) {
        val entry = taskTable[name] ?: return
        owner?.lifecycleOwner?.let {
            entry.state.removeObservers(it)
        }
        entry.task?.close()
        entry.task = null
        taskTable.remove(name)
    }

    /**
     * 全クリア ... 普通は呼ぶ必要はないとおもう。
     */
    override fun close() {
        for(entry in taskTable.values) {
            entry.task?.close()
        }
        taskTable.clear()
    }

//    data class TaskEntry(var task:IUtImmortalTask?=null, val state:MutableLiveData<UtImmortalTaskState>) {
//        private val retainBy = mutableSetOf<String>()
//        fun retain(key:String) {
//            retainBy.add(key)
//        }
//        fun release(key:String):Boolean {
//            if(retainBy.contains(key)) {
//                retainBy.remove(key)
//                return retainBy.isEmpty()
//            }
//            return false
//        }
//        val isRetained:Boolean
//            get() = retainBy.isNotEmpty()
//    }
//
//    private val taskMap = mutableMapOf<String,TaskEntry>()
//    private val dialogOwnerStack = UtDialogOwnerStack()
//
//    val immortalTaskScope:CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
//    val mortalInstanceSource:IUiMortalInstanceSource = dialogOwnerStack
//
//    override fun close() {
//        immortalTaskScope.cancel()
//        taskMap.forEach { key, entry ->
//            entry.task?.close()
//        }
//        taskMap.clear()
//    }
//
//    private fun ensureTaskEntry(name:String):TaskEntry {
//        return taskMap[name] ?: taskMap.setAndGet(name, TaskEntry(null, MutableLiveData(UtImmortalTaskState.INITIAL)))
//    }
//
//    /**
//     * Activity, Fragment(mortalなクラスのインスタンス)からImmortalTaskに接続する。
//     * ImmortalTaskの状態(UtImmortalTaskState)を監視するActivity/Fragmentの、Activity.onResume()やonCreateView から呼び出す。
//     * retainKeyに監視者の名前を渡すことにより、close()を呼び出すまで、タスクは解放されない。
//     * connectは何回呼び出してもよい（Activity.onCreateViewから呼んでよい）が、その場合、同じretainKeyを渡すこと。
//     * retainKeyにnotNullな名前を渡した場合は、必ず１回、closeすること。
//     * 通常は、UtImmortalTaskSTateを監視して、COMPLETED|ERROR を検出してclose()する。
//     *
//     * @param name  接続先Immortalタスクの名前
//     * @param owner 接続するMortalインスタンス（Activity or Fragment)
//     * @param retainKey タスクを自動解放しないようretainする場合は、ユニークなキーを渡す。nullならretainしない。
//     * @return 状態監視用LiveData
//     */
//    @MainThread
//    fun connect(name:String, owner:UtDialogOwner, retainKey:String?):LiveData<UtImmortalTaskState> {
//        dialogOwnerStack.push(owner)
//        val entry = ensureTaskEntry(name)
//        if(!retainKey.isNullOrEmpty()) {
//            entry.retain(retainKey)
//        }
//        return entry.state
//    }
//
//    /**
//     * Activity/Fragmentからの監視を終了する
//     */
//    @MainThread
//    fun close(name:String, owner:UtDialogOwner, retainKey: String) {
//        dialogOwnerStack.remove(owner)
//        val entry = taskMap[name] ?: return
//        if(entry.release(retainKey)) {
//            entry.state.removeObservers(owner.lifecycleOwner)
//            taskMap.remove(name)
//        }
//    }
//
//    /**
//     * タスクの実行を開始する。
//     */
//    @MainThread
//    fun startTask(name:String, taskSource: ()->IUtImmortalTask) {
//        val entry = ensureTaskEntry(name)
//        if(entry.task!=null) {
//            throw IllegalStateException("task is already running.")
//        }
//        entry.state.value = UtImmortalTaskState.RUNNING
//        val task = taskSource()
//        if(task.taskName!=name) {
//            throw IllegalArgumentException("task name is invalid.")
//        }
//        entry.task = task
//    }
//
//    /**
//     * タスクを完了する
//     */
//    @MainThread
//    fun endTask(task:IUtImmortalTask, succeeded:Boolean) {
//        val entry = taskMap[task.taskName] ?: return
//        entry.state.value = if(succeeded) UtImmortalTaskState.COMPLETED else UtImmortalTaskState.ERROR
//        if(!entry.isRetained) {
//            taskMap.remove(task.taskName)
//        }
//    }
//
//    /**
//     * タスクを取得
//     */
//    @MainThread
//    fun taskOf(name:String?):IUtImmortalTask? {
//        return if(name!=null) taskMap[name]?.task else null
//    }
}