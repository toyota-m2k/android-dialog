package io.github.toyota32k.task

import androidx.annotation.MainThread
import androidx.lifecycle.*
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.setAndGet
import kotlinx.coroutines.*
import java.io.Closeable
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * 世界の終り（アプリ終了）まで生きることができる不死身のタスククラス
 */
object UtImmortalTaskManager : Closeable  {
    interface ITaskInfo {
        val name:String
        val ownerName:String
        val state:LiveData<UtImmortalTaskState>
        val task:IUtImmortalTask?
        val result:Any?
    }
    data class TaskEntry(override val name:String, override val ownerName:String):ITaskInfo {
        override val state = MutableLiveData<UtImmortalTaskState>(UtImmortalTaskState.INITIAL)
        override var task:IUtImmortalTask?=null
        override var result:Any?=null
    }

    val logger = UtLog("UtTask")
    private val taskTable = mutableMapOf<String,TaskEntry>()
    private val dialogOwnerStack = UtDialogOwnerStack()

    val immortalTaskScope:CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val mortalInstanceSource:IUiMortalInstanceSource = dialogOwnerStack

    fun taskOf(name:String):ITaskInfo? {
        return taskTable[name]
    }

    private fun createTask(name:String, ownerName: String):TaskEntry {
        return TaskEntry(name,ownerName).apply { taskTable[name]=this }
    }

    fun onOwnerResumed(name:String, ownerName: String, owner:UtDialogOwner) : ITaskInfo {
        dialogOwnerStack.push(owner)
        return taskTable[name] ?: createTask(name, ownerName)
    }
    fun onOwnerPaused(name:String, ownerName:String, owner:UtDialogOwner) {
        dialogOwnerStack.remove(owner)
        taskTable[name]?.state?.removeObservers(owner.lifecycleOwner)
    }

    fun disposeTask(name:String, ownerName: String, owner:UtDialogOwner) {
        val entry = taskTable[name] ?: return
        if(entry.ownerName == ownerName) {
            entry.state.removeObservers(owner.lifecycleOwner)
            entry.task?.close()
            taskTable.remove(name)
        }
    }

    fun attachTask(task:IUtImmortalTask) {
        val entry = taskTable[task.taskName] ?: throw IllegalStateException("no such task: ${task.taskName}")
        if(entry.task!=null) throw IllegalStateException("task already running: ${task.taskName}")
        entry.state.value = UtImmortalTaskState.RUNNING
        entry.task = task
    }

    fun detachTask(task:IUtImmortalTask, succeeded:Boolean) {
        val entry = taskTable[task.taskName] ?: return
        entry.result = task.taskResult
        entry.state.value = if(succeeded) UtImmortalTaskState.COMPLETED else UtImmortalTaskState.ERROR
        entry.task = null
    }

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