@file:Suppress("unused")

package io.github.toyota32k.dialog.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog
import kotlinx.coroutines.Job

/**
 * IUtImmortalTaskMutableContextSource を実装し、
 * UtImmortalTask / UtDialog と協調的に動作する ViewModel の基底クラス
 */
abstract class  UtDialogViewModel : ViewModel(), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext
    companion object {
        /**
         * タスク開始時の初期化用
         * タスクのexecute()処理の中から実行する
         */
        suspend fun <T> createAsync(clazz: Class<T>, task: IUtImmortalTask, initialize:(suspend (T)->Unit)) : T where T: UtDialogViewModel {
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
                .apply {
                    immortalTaskContext = task.immortalTaskContext
                    initialize.invoke(this)
                }
        }
        fun <T> create(clazz: Class<T>, task: IUtImmortalTask, initialize:((T)->Unit)?=null) : T where T: UtDialogViewModel {
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
                .apply {
                    immortalTaskContext = task.immortalTaskContext
                    initialize?.invoke(this)
                }
        }
        inline fun <reified T> createOnTask(task: IUtImmortalTask, noinline initialize:((T)->Unit)?=null) : T where T: UtDialogViewModel {
            return create(T::class.java, task, initialize)
        }
        /**
         * ダイアログから、タスク名をキーに作成済み ViewModelを取得する。
         */
        fun <T> instanceOf(clazz: Class<T>, taskName:String):T where T:ViewModel {
            val task = UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no task")
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
        }
        inline fun <reified T> instanceOf(taskName:String):T where T:ViewModel {
            return instanceOf(T::class.java, taskName)
        }
        /**
         * ダイアログにセットされているタスク名から、作成済みViewModelを取得する。
         */
        fun <T> instanceFor(clazz: Class<T>, dialog: IUtDialog):T where T:ViewModel {
            return instanceOf(clazz, dialog.immortalTaskName?:throw java.lang.IllegalStateException("no task name in the dialog."))
        }
        inline fun <reified T> instanceFor(dialog: IUtDialog):T where T:ViewModel {
            return instanceFor(T::class.java, dialog)
        }
    }

    /**
     * ViewModel内から、実行中のImmortalTask上で、サブタスクを開始する。
     * （やりっぱなし）
     */
    fun launchSubTask(callback:suspend IUtImmortalTask.()->Unit): Job {
        return (this.immortalTaskContext.task as UtImmortalTaskImpl).subTask().launchTask(callback)
    }
    /**
     * ViewModel内から、実行中のImmortalTask上で、サブタスクを開始する。
     * （終了を待つ）
     */
    suspend fun awaitSubTask(callback:suspend IUtImmortalTask.()->Unit) {
        return (this.immortalTaskContext.task as UtImmortalTaskImpl).subTask().awaitTask(callback)
    }
    /**
     * ViewModel内から、実行中のImmortalTask上で、サブタスクを開始する。
     * （結果を待つ）
     */
    suspend fun <T> awaitSubTaskResult(callback:suspend IUtImmortalTask.()->T):T {
        return (this.immortalTaskContext.task as UtImmortalTaskImpl).subTask().awaitTaskResult(callback)
    }

    suspend fun <T> safeAwaitSubTaskResult(default:T, callback:suspend IUtImmortalTask.()->T):T {
        return (this.immortalTaskContext.task as UtImmortalTaskImpl).subTask().awaitTaskResultCatching(default, callback)
    }
}



/**
 * ImmortalTask 内で、createViewModel<ViewModelType>() によってViewModelインスタンスを作成してから、showDialogを呼び出すと、
 * UtDialog派生クラスからは getViewModel<ViewModelType>() を使用して、そのViewModelインスタンスが取得できます。
 * ```
 *     class MyViewModel : UtDialogViewModel() {
 *         // ...
 *     }
 *     class MyDialog : UtDialogEx() {
 *         val viewModel = getViewModel<MyViewModel>()
 *         override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
 *             // ...
 *         }
 *     }
 *     fun showMyDialogAsync() {
 *         UtImmortalTask.launch {
 *             val p = createViewModel<MyViewModel>()
 *             val dlg = showDialog<MyDialog>("test-dialog")
 *             if (dlg.status.ok) {
 *                 // ダイアログがDoneボタンで閉じた
 *             }
 *         }
 *     }
 * ```
 */
inline fun <reified T> IUtImmortalTask.createViewModel(noinline initialize:(T.()->Unit)?=null) : T where T: UtDialogViewModel {
    return UtDialogViewModel.create(T::class.java, this) {
        if (initialize!=null) {
            it.initialize()
        }
    }
}

inline fun <reified VM:ViewModel> IUtDialog.getViewModel():VM  {
    return UtDialogViewModel.instanceFor(this)
}

