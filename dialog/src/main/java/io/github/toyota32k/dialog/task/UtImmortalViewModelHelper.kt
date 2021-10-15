package io.github.toyota32k.dialog.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog

object UtImmortalViewModelHelper {
    /**
     * タスク開始時の初期化用
     */
    fun <T> createBy(clazz: Class<T>, task: IUtImmortalTask, initialize:((T)->Unit)?=null) : T where T: ViewModel,T:IUtImmortralTaskMutableContextSource {
        return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory()).get(clazz)
            .apply {
                immortalTaskContext = task.immortalTaskContext
                initialize?.invoke(this)
            }
    }

    /**
     * ダイアログから取得する用
     */
    fun <T> instanceOf(clazz: Class<T>, taskName:String):T where T:ViewModel {
        val task = UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no task")
        return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory()).get(clazz)
    }
    /**
     * ダイアログから取得する用
     */
    fun <T> instanceFor(clazz: Class<T>, dialog: IUtDialog):T where T:ViewModel {
        return instanceOf(clazz, dialog.immortalTaskName?:throw java.lang.IllegalStateException("no task name in the dialog."))
    }

}
