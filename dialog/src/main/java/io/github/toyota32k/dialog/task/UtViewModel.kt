@file:Suppress("unused")

package io.github.toyota32k.dialog.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog

open class  UtViewModel : ViewModel(), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext
    companion object {
        /**
         * タスク開始時の初期化用
         * タスクのexecute()処理の中から実行する
         */
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
        fun <T> instanceOf(clazz: Class<T>, taskName:String):T where T:ViewModel {
            val task = UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no task")
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
        }
        /**
         * ダイアログにセットされているタスク名から、作成済みViewModelを取得する。
         */
        fun <T> instanceFor(clazz: Class<T>, dialog: IUtDialog):T where T:ViewModel {
            return instanceOf(clazz, dialog.immortalTaskName?:throw java.lang.IllegalStateException("no task name in the dialog."))
        }
    }
}

open class  UtAndroidViewModel(application: Application) : AndroidViewModel(application), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext
}
