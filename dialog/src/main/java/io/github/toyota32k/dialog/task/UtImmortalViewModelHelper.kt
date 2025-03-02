package io.github.toyota32k.dialog.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog

/**
 * タスクのライフサイクルに依存する ViewModel の作成、取得のためのヘルパークラス
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Deprecated("Use UtViewModel instead.")
object UtImmortalViewModelHelper {
    /**
     * タスク開始時の初期化用
     * タスクのexecute()処理の中から実行する
     */
    @Deprecated("Use UtViewModel.createBy instead.")
    fun <T> createBy(clazz: Class<T>, task: IUtImmortalTask, initialize:((T)->Unit)?=null) : T where T: ViewModel,T:IUtImmortalTaskMutableContextSource {
        return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
            .apply {
                immortalTaskContext = task.immortalTaskContext
                initialize?.invoke(this)
            }
    }

    /**
     * ダイアログから、タスク名をキーに作成済み ViewModelを取得する。
     */
    @Deprecated("Use UtViewModel.instanceOf instead.")
    fun <T> instanceOf(clazz: Class<T>, taskName:String):T where T:ViewModel {
        val task = UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no task")
        return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
    }
    /**
     * ダイアログにセットされているタスク名から、作成済みViewModelを取得する。
     */
    @Deprecated("Use UtViewModel.instanceFor instead.")
    fun <T> instanceFor(clazz: Class<T>, dialog: IUtDialog):T where T:ViewModel {
        return UtViewModel.instanceOf(clazz, dialog.immortalTaskName?:throw java.lang.IllegalStateException("no task name in the dialog."))
    }

}
