package io.github.toyota32k.dialog.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.IUtDialog

open class UtAndroidViewModel(application: Application) : AndroidViewModel(application), IUtImmortalTaskMutableContextSource {
    override lateinit var immortalTaskContext: IUtImmortalTaskContext

    companion object {
        fun <T> create(clazz: Class<T>, application: Application, task: IUtImmortalTask, initialize:((T)->Unit)?=null) : T where T: UtAndroidViewModel {
            return ViewModelProvider(
                task.immortalTaskContext,
                ViewModelProvider.AndroidViewModelFactory.getInstance(task.application))[clazz]
                .apply {
                    immortalTaskContext = task.immortalTaskContext
                    initialize?.invoke(this)
                }
        }

        /**
         * タスクを引数に UtAndroidViewModel を作成する
         */
        inline fun <reified T> createOnTask(application:Application, task: IUtImmortalTask, noinline initialize:((T)->Unit)?=null) : T where T: UtAndroidViewModel {
            return create(T::class.java, application, task, initialize)
        }

        /**
         * IUtImmortalTask のexecuteブロック内で、そのタスクスコープな UtAndroidViewModel を作成する
         */
        inline fun <reified T> IUtImmortalTask.createAndroidViewModel(noinline initialize:((T)->Unit)?=null) : T where T: UtAndroidViewModel {
            return create(T::class.java, application, this, initialize)
        }

        /**
         * UtMortalActivity#onResume より前に使用する場合は、application が uninitialized なので、こちらを使う。
         */
        inline fun <reified T> IUtImmortalTask.createAndroidViewModel(application: Application, noinline initialize:((T)->Unit)?=null) : T where T: UtAndroidViewModel {
            return create(T::class.java, application, this, initialize)
        }
    }
}
