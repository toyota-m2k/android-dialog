@file:Suppress("unused")

package io.github.toyota32k.dialog.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.task.UtViewModel.Companion.create

open class  UtViewModel : ViewModel(), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext
    companion object {
        /**
         * タスク開始時の初期化用
         * タスクのexecute()処理の中から実行する
         */
        suspend fun <T> createAsync(clazz: Class<T>, task: IUtImmortalTask, initialize:(suspend (T)->Unit)) : T where T: UtViewModel {
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
                .apply {
                    immortalTaskContext = task.immortalTaskContext
                    initialize.invoke(this)
                }
        }
        fun <T> create(clazz: Class<T>, task: IUtImmortalTask, initialize:((T)->Unit)?=null) : T where T: UtViewModel {
            return ViewModelProvider(task.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz]
                .apply {
                    immortalTaskContext = task.immortalTaskContext
                    initialize?.invoke(this)
                }
        }
        inline fun <reified T> createOnTask(task: IUtImmortalTask, noinline initialize:((T)->Unit)?=null) : T where T: UtViewModel {
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
            val task = UtImmortalTaskManager.taskOf(taskName)?.task ?: throw IllegalStateException("no task")
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
}

/**
 * ImmortalTask 内で、createViewModel<ViewModelType>() によってViewModelインスタンスを作成してから、showDialogを呼び出すと、
 * UtDialog派生クラスからは getViewModel<ViewModelType>() を使用して、そのViewModelインスタンスが取得できます。
 * ```
 *     class MyViewModel : UtViewModel() {
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
inline fun <reified T> IUtImmortalTask.createViewModel(noinline initialize:(T.()->Unit)?=null) : T where T: UtViewModel {
    return UtViewModel.create(T::class.java, this) { it->
        if (initialize!=null) {
            it.initialize()
        }
    }
}
suspend inline fun <reified T> IUtImmortalTask.withViewModel(noinline block:suspend IUtImmortalTask.(T)->Unit) : T where T: UtViewModel {
    return UtViewModel.createAsync(T::class.java, this) { it->
        block(it)
    }
}

inline fun <reified VM:ViewModel> IUtDialog.getViewModel():VM  {
    return UtViewModel.instanceFor(this)
}
